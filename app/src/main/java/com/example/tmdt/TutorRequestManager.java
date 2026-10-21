package com.example.tmdt;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** UC12: owner-only live lists and transactional mutations, retained across rotation. */
public final class TutorRequestManager extends ViewModel {
    private static final String PREFIX = "uc12.";
    private static final String[] DRAFT_KEYS = {
            "title", "subject_id", "subject_name", "level", "budget", "schedule", "description"
    };
    private final SavedStateHandle saved;
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final MutableLiveData<Integer> changes = new MutableLiveData<>(0);
    private final List<TutorRequestRecord> records = new ArrayList<>();
    private final List<TutorApplication> applications = new ArrayList<>();
    private final FirebaseAuth.AuthStateListener authListener = ignored -> bindOwner();
    private ListenerRegistration requestsListener, applicationsListener;
    private String boundOwner, watchedId = "", listError = "", actionError = "", notice = "", applicationsError = "";
    private int generation, listGeneration, applicationsGeneration;
    private boolean loading, busy, offline, applicationsLoading, applicationsOffline, awaitingApplicantRequest;

    public TutorRequestManager(SavedStateHandle saved) {
        this.saved = saved;
        bindOwner();
        auth.addAuthStateListener(authListener);
    }

    public LiveData<Integer> changes() { return changes; }
    public boolean loading() { return loading; }
    public boolean busy() { return busy; }
    public boolean offline() { return offline; }
    public String error() { return actionError.isEmpty() ? listError : actionError; }
    public String notice() { return notice; }
    public boolean applicantsLoading() { return applicationsLoading; }
    public boolean applicantsOffline() { return applicationsOffline; }
    public boolean closedTab() { return Boolean.TRUE.equals(saved.get(PREFIX + "closed_tab")); }
    public void closedTab(boolean closed) { saved.set(PREFIX + "closed_tab", closed); }
    public String applicantsError() { return applicationsError; }
    public List<TutorApplication> applicants() { return Collections.unmodifiableList(new ArrayList<>(applications)); }

    public List<TutorRequestRecord> requests(boolean closed) {
        List<TutorRequestRecord> result = new ArrayList<>();
        for (TutorRequestRecord record : records) {
            if (closed ? record.isClosed() : record.isOpen()) result.add(record);
        }
        return Collections.unmodifiableList(result);
    }

    public TutorRequestRecord request(String id) {
        for (TutorRequestRecord record : records) if (record.id.equals(id)) return record;
        return null;
    }

    public void retry() {
        bindOwner();
        if (busy) return;
        actionError = ""; notice = "";
        listenRequests();
        if (!watchedId.isEmpty()) {
            String id = watchedId;
            if (applicationsListener != null) applicationsListener.remove();
            applicationsListener = null;
            applicationsLoading = false;
            watchApplicants(id);
        }
    }

    private String owner() { return auth.getCurrentUser() == null ? "" : auth.getCurrentUser().getUid(); }
    private String savedText(String key) {
        Object value = saved.get(PREFIX + key);
        return value instanceof String ? (String) value : "";
    }
    private void changed() {
        Integer value = changes.getValue();
        changes.setValue(value == null ? 1 : value + 1);
    }

    private void bindOwner() {
        String uid = owner();
        if (uid.equals(boundOwner)) return;
        generation++; listGeneration++; applicationsGeneration++;
        if (requestsListener != null) requestsListener.remove();
        if (applicationsListener != null) applicationsListener.remove();
        requestsListener = null; applicationsListener = null;
        records.clear(); applications.clear(); watchedId = "";
        busy = false; loading = false; offline = false; applicationsLoading = false; applicationsOffline = false; awaitingApplicantRequest = false;
        listError = ""; actionError = ""; notice = ""; applicationsError = "";
        if (!uid.equals(savedText("owner"))) {
            for (String key : new ArrayList<>(saved.keys())) if (key.startsWith(PREFIX)) saved.remove(key);
            saved.set(PREFIX + "owner", uid);
        }
        boundOwner = uid;
        listenRequests();
    }

    private void listenRequests() {
        if (requestsListener != null) requestsListener.remove();
        requestsListener = null;
        final String uid = owner();
        final int token = generation, queryToken = ++listGeneration;
        listError = ""; loading = !uid.isEmpty();
        if (uid.isEmpty()) { changed(); return; }
        changed();
        // Only one equality filter: no composite index and no cross-account documents in cache results.
        requestsListener = db.collection("tutor_requests").whereEqualTo("student_id", uid)
                .addSnapshotListener(MetadataChanges.INCLUDE, (snapshot, failure) -> {
                    if (token != generation || queryToken != listGeneration || !uid.equals(owner())) return;
                    loading = false;
                    if (failure != null) {
                        listError = message(failure, "Chưa tải được tin đăng. Kiểm tra kết nối rồi thử lại.");
                        if (accessDenied(failure)) {
                            records.clear(); offline = false;
                            clearUnavailableApplicants();
                        }
                    } else if (snapshot != null) {
                        listError = ""; offline = snapshot.getMetadata().isFromCache();
                        records.clear();
                        for (DocumentSnapshot document : snapshot.getDocuments()) {
                            TutorRequestRecord record = TutorRequestRecord.from(document);
                            if (record != null && (record.isOpen() || record.isClosed())) records.add(record);
                        }
                        records.sort(Comparator.comparingLong((TutorRequestRecord record) -> record.createdAtMillis)
                                .reversed().thenComparing(record -> record.id));
                        if (!watchedId.isEmpty()) {
                            if (request(watchedId) == null) {
                                if (!awaitingApplicantRequest || !snapshot.getMetadata().isFromCache()) clearUnavailableApplicants();
                            } else if (awaitingApplicantRequest) {
                                awaitingApplicantRequest = false;
                                watchApplicants(watchedId);
                            }
                        }
                    }
                    changed();
                });
    }

    public void watchApplicants(String id) {
        bindOwner();
        String next = id == null ? "" : id;
        if (next.equals(watchedId) && (applicationsLoading || (applicationsListener != null && applicationsError.isEmpty()))) return;
        if (applicationsListener != null) applicationsListener.remove();
        applicationsListener = null;
        final int queryToken = ++applicationsGeneration, token = generation;
        final String uid = owner();
        watchedId = next; applications.clear(); applicationsError = ""; applicationsLoading = false; applicationsOffline = false; awaitingApplicantRequest = false;
        if (next.isEmpty()) { changed(); return; }
        if (uid.isEmpty()) {
            applicationsError = "Vui lòng đăng nhập để xem danh sách ứng tuyển.";
            changed(); return;
        }
        if (!validId(next) || request(next) == null) {
            awaitingApplicantRequest = validId(next) && (loading || offline);
            applicationsError = awaitingApplicantRequest ? "Đang tải thông tin tin đăng. Vui lòng đợi kết nối máy chủ." : "Tin đăng không còn khả dụng.";
            changed(); return;
        }
        applicationsLoading = true; changed();
        applicationsListener = db.collection("tutor_requests").document(next).collection("applications")
                .addSnapshotListener(MetadataChanges.INCLUDE, (snapshot, failure) -> {
                    if (token != generation || queryToken != applicationsGeneration || !uid.equals(owner())) return;
                    applicationsLoading = false;
                    if (failure != null) {
                        applicationsError = message(failure, "Chưa tải được danh sách ứng tuyển. Vui lòng thử lại.");
                        if (accessDenied(failure)) {
                            applications.clear(); applicationsOffline = false;
                        }
                    } else if (snapshot != null) {
                        applicationsError = ""; applicationsOffline = snapshot.getMetadata().isFromCache(); applications.clear();
                        for (DocumentSnapshot document : snapshot.getDocuments()) {
                            TutorApplication application = TutorApplication.from(document);
                            if (application != null) applications.add(application);
                        }
                        applications.sort(Comparator.comparingLong((TutorApplication item) -> item.createdAtMillis)
                                .reversed().thenComparing(item -> item.id));
                    }
                    changed();
                });
    }

    private void clearUnavailableApplicants() {
        applicationsGeneration++;
        if (applicationsListener != null) applicationsListener.remove();
        applicationsListener = null;
        applications.clear(); applicationsLoading = false; applicationsOffline = false; awaitingApplicantRequest = false;
        applicationsError = "Tin đăng đã bị xóa hoặc không còn khả dụng.";
    }

    public String draft(String key) { return savedText("draft." + key); }
    public void draft(String key, String value) {
        if (!busy) saved.set(PREFIX + "draft." + key, value == null ? "" : value);
    }

    /** Repeated rendering of the same edit never replaces the user's draft. */
    public boolean startEdit(String id) {
        bindOwner();
        TutorRequestRecord record = request(id);
        if (record == null) {
            actionError = loading ? "Đang tải tin đăng. Vui lòng đợi một chút." : "Tin đăng không còn khả dụng.";
            changed(); return false;
        }
        if (id.equals(savedText("edit_id"))) return true;
        if (busy) return false;
        if (record.updatedAt == null) {
            actionError = "Tin đăng đang đồng bộ. Vui lòng tải lại rồi thử sửa.";
            changed(); return false;
        }
        clearEdit();
        saved.set(PREFIX + "edit_id", id);
        saved.set(PREFIX + "version_seconds", record.updatedAt.getSeconds());
        saved.set(PREFIX + "version_nanos", record.updatedAt.getNanoseconds());
        draft("title", record.title); draft("subject_id", record.subjectId);
        draft("subject_name", record.subjectName); draft("level", record.level);
        draft("budget", Long.toString(record.budget)); draft("schedule", record.schedule);
        draft("description", record.description);
        actionError = ""; notice = "";
        return true;
    }

    /** Explicitly discard a draft before reopening a fresh server version. */
    public void resetEdit() {
        if (busy) return;
        clearEdit(); actionError = ""; changed();
    }
    private void clearEdit() {
        for (String key : DRAFT_KEYS) saved.remove(PREFIX + "draft." + key);
        saved.remove(PREFIX + "edit_id");
        saved.remove(PREFIX + "version_seconds"); saved.remove(PREFIX + "version_nanos");
    }
    public String editSavedId() { return savedText("edit_saved_id"); }
    public void consumeEditSaved() { saved.remove(PREFIX + "edit_saved_id"); }

    public void close(String id) { mutate(id, "close", null, null); }
    public void delete(String id) { mutate(id, "delete", null, null); }

    public void saveEdit(String id, TutorRequest value) {
        bindOwner();
        if (busy) return;
        if (value == null || !value.invalidField().isEmpty()) {
            actionError = "Vui lòng kiểm tra đầy đủ thông tin tin đăng."; changed(); return;
        }
        Object seconds = saved.get(PREFIX + "version_seconds"), nanos = saved.get(PREFIX + "version_nanos");
        if (!savedText("edit_id").equals(id) || !(seconds instanceof Number) || !(nanos instanceof Number)) {
            actionError = "Vui lòng mở lại tin đăng trước khi sửa."; changed(); return;
        }
        Timestamp version = new Timestamp(((Number) seconds).longValue(), ((Number) nanos).intValue());
        mutate(id, "edit", value, version);
    }

    private void mutate(String id, String operation, TutorRequest value, Timestamp version) {
        bindOwner();
        if (busy) return;
        final String uid = owner();
        if (uid.isEmpty()) { actionError = "Vui lòng đăng nhập để quản lý tin đăng."; changed(); return; }
        if (!validId(id)) { actionError = "Tin đăng không hợp lệ. Vui lòng tải lại danh sách."; changed(); return; }
        final int token = generation;
        DocumentReference reference = db.collection("tutor_requests").document(id);
        busy = true; actionError = ""; notice = ""; changed();
        db.runTransaction(transaction -> {
            DocumentSnapshot current = transaction.get(reference);
            DocumentSnapshot profile = transaction.get(db.collection("users").document(uid));
            requireOwner(current, profile, uid);
            Object status = current.get("trang_thai");
            if ("delete".equals(operation) && "DELETED".equals(status)) return null;
            if (!"OPEN".equals(status) && !"CLOSED".equals(status))
                throw new RequestFailure("Tin đăng đã bị xóa hoặc không còn khả dụng.");
            Map<String, Object> update = new HashMap<>();
            if ("edit".equals(operation)) {
                if (!Objects.equals(version, current.get("updated_at")))
                    throw new RequestFailure("Tin đã thay đổi trên thiết bị khác. Bản nháp vẫn được giữ; hãy tải lại tin trước khi sửa tiếp.");
                boolean subjectChanged = !value.subjectId.equals(current.get("subject_id"))
                        || !value.subjectName.equals(current.get("mon_hoc"));
                if (subjectChanged) {
                    DocumentSnapshot subject = transaction.get(db.collection("subjects").document(value.subjectId));
                    if (!subject.exists() || !Boolean.TRUE.equals(subject.get("dang_su_dung"))
                            || !value.subjectName.equals(subject.get("ten_mon")))
                        throw new RequestFailure("Môn học đã thay đổi. Vui lòng chọn lại môn học đang sử dụng.");
                }
                update.put("tieu_de", value.title); update.put("subject_id", value.subjectId);
                update.put("mon_hoc", value.subjectName); update.put("cap_hoc", value.level);
                update.put("ngan_sach", value.budget); update.put("lich_hoc", value.schedule);
                update.put("mo_ta", value.description);
            } else if ("close".equals(operation)) {
                if ("CLOSED".equals(status)) return null;
                update.put("trang_thai", "CLOSED");
            } else {
                update.put("trang_thai", "DELETED");
                update.put("deleted_at", FieldValue.serverTimestamp());
            }
            update.put("updated_at", FieldValue.serverTimestamp());
            transaction.update(reference, update);
            return null;
        }).addOnCompleteListener(task -> {
            if (token != generation || !uid.equals(owner())) return;
            busy = false;
            if (task.isSuccessful()) {
                if ("edit".equals(operation)) {
                    clearEdit(); saved.set(PREFIX + "edit_saved_id", id);
                    notice = "Đã cập nhật tin đăng.";
                } else if ("close".equals(operation)) notice = "Đã đóng tin đăng.";
                else notice = "Đã xóa tin đăng.";
            } else actionError = message(task.getException(), "Chưa lưu được thay đổi. Kiểm tra kết nối rồi thử lại; bản nháp vẫn được giữ.");
            changed();
        });
    }

    private static void requireOwner(DocumentSnapshot request, DocumentSnapshot profile, String uid) {
        if (!request.exists() || !uid.equals(request.get("student_id")))
            throw new RequestFailure("Bạn chỉ có thể quản lý tin do mình đăng.");
        if (!profile.exists() || !"ACTIVE".equals(profile.get("trang_thai")))
            throw new RequestFailure("Tài khoản chưa được phép quản lý tin. Vui lòng kiểm tra hồ sơ tài khoản.");
    }
    private static boolean validId(String id) { return id != null && !id.isEmpty() && !id.contains("/"); }
    private static boolean accessDenied(Throwable failure) {
        Throwable cause = failure;
        while (cause != null) {
            if (cause instanceof FirebaseFirestoreException) {
                FirebaseFirestoreException.Code code = ((FirebaseFirestoreException) cause).getCode();
                if (code == FirebaseFirestoreException.Code.PERMISSION_DENIED
                        || code == FirebaseFirestoreException.Code.UNAUTHENTICATED) return true;
            }
            if (cause.getCause() == cause) break;
            cause = cause.getCause();
        }
        return false;
    }
    private static String message(Throwable failure, String fallback) {
        Throwable cause = failure;
        while (cause != null) {
            if (cause instanceof RequestFailure) return cause.getMessage();
            if (cause instanceof FirebaseFirestoreException) {
                FirebaseFirestoreException.Code code = ((FirebaseFirestoreException) cause).getCode();
                if (code == FirebaseFirestoreException.Code.PERMISSION_DENIED)
                    return "Không thể thực hiện với tài khoản này. Vui lòng kiểm tra hồ sơ và quyền truy cập tin đăng.";
                if (code == FirebaseFirestoreException.Code.UNAVAILABLE || code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED)
                    return "Chưa kết nối được máy chủ. Kiểm tra mạng rồi thử lại; bản nháp vẫn được giữ.";
            }
            if (cause.getCause() == cause) break;
            cause = cause.getCause();
        }
        return fallback;
    }
    private static final class RequestFailure extends RuntimeException {
        RequestFailure(String message) { super(message); }
    }

    @Override protected void onCleared() {
        generation++; listGeneration++; applicationsGeneration++;
        auth.removeAuthStateListener(authListener);
        if (requestsListener != null) requestsListener.remove();
        if (applicationsListener != null) applicationsListener.remove();
    }
}