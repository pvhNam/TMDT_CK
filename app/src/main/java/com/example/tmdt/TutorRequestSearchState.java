package com.example.tmdt;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** UC13: verified tutors search open requests, with a server-side subject filter. */
public final class TutorRequestSearchState extends ViewModel {
    private static final String PREFIX = "uc13.";
    private final SavedStateHandle saved;
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final MutableLiveData<Integer> changes = new MutableLiveData<>(0);
    private final List<TutorRequestRecord> records = new ArrayList<>();
    private final FirebaseAuth.AuthStateListener authListener = ignored -> bindOwner();
    private ListenerRegistration accountListener, tutorListener, requestsListener;
    private String boundOwner, accountError = "", tutorError = "", queryError = "";
    private int generation, queryGeneration;
    private boolean accountLoaded, tutorLoaded, accountActive, tutorVerified;
    private boolean accountCached, tutorCached, queryCached, loading, queryFailed, readDenied;

    public TutorRequestSearchState(SavedStateHandle saved) {
        this.saved = saved;
        bindOwner();
        auth.addAuthStateListener(authListener);
    }

    public LiveData<Integer> changes() { return changes; }
    public boolean loading() { return sameOwner() && loading; }
    public boolean checkingAccess() { return sameOwner() && (!accountLoaded || !tutorLoaded); }
    public boolean authorized() {
        return sameOwner() && accountLoaded && tutorLoaded && accountActive && tutorVerified
                && accountError.isEmpty() && tutorError.isEmpty() && !readDenied;
    }
    public boolean offline() { return sameOwner() && (accountCached || tutorCached || queryCached); }
    public String error() {
        if (!sameOwner()) return "Vui lòng đăng nhập để tìm tin cần gia sư.";
        if (!accountError.isEmpty()) return accountError;
        if (!tutorError.isEmpty()) return tutorError;
        if (!checkingAccess()) {
            if (!accountActive) return "Tài khoản chưa hoạt động. Vui lòng kiểm tra hồ sơ tài khoản.";
            if (!tutorVerified) return "Chỉ gia sư đã được duyệt mới có thể tìm tin cần gia sư.";
        }
        return queryError;
    }
    public List<TutorRequestRecord> requests() {
        return authorized() ? Collections.unmodifiableList(new ArrayList<>(records)) : Collections.emptyList();
    }
    public TutorRequestRecord request(String id) {
        if (!authorized()) return null;
        for (TutorRequestRecord record : records) if (record.id.equals(id)) return record;
        return null;
    }
    public String selectedSubjectId() { return sameOwner() ? savedText("subject_id") : ""; }
    public String selectedSubjectName() { return sameOwner() ? savedText("subject_name") : ""; }

    /** Empty subject means all subjects; changing a filter immediately removes the previous results. */
    public void selectSubject(String id, String name) {
        bindOwner();
        String nextId = id == null ? "" : id.trim();
        String nextName = nextId.isEmpty() || name == null ? "" : name.trim();
        if (nextId.equals(selectedSubjectId())) {
            saved.set(PREFIX + "subject_name", nextName);
            changed(); return;
        }
        saved.set(PREFIX + "subject_id", nextId);
        saved.set(PREFIX + "subject_name", nextName);
        stopRequests(); queryError = ""; queryFailed = false;
        if (authorized()) listenRequests();
        else changed();
    }

    public void retry() {
        bindOwner();
        if (!sameOwner()) { changed(); return; }
        if (checkingAccess() || !authorized() || !accountError.isEmpty() || !tutorError.isEmpty()) {
            listenAccess();
        } else listenRequests();
    }

    private String owner() { return auth.getCurrentUser() == null ? "" : auth.getCurrentUser().getUid(); }
    private boolean sameOwner() { return boundOwner != null && !boundOwner.isEmpty() && boundOwner.equals(owner()); }
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
        if (!uid.equals(savedText("owner"))) {
            for (String key : new ArrayList<>(saved.keys())) if (key.startsWith(PREFIX)) saved.remove(key);
            saved.set(PREFIX + "owner", uid);
        }
        boundOwner = uid;
        listenAccess();
    }

    private void listenAccess() {
        generation++;
        if (accountListener != null) accountListener.remove();
        if (tutorListener != null) tutorListener.remove();
        accountListener = null; tutorListener = null;
        stopRequests();
        accountLoaded = false; tutorLoaded = false; accountActive = false; tutorVerified = false;
        accountCached = false; tutorCached = false;
        accountError = ""; tutorError = ""; queryError = ""; readDenied = false; queryFailed = false;
        final String uid = owner();
        final int token = generation;
        changed();
        if (uid.isEmpty()) return;
        accountListener = db.collection("users").document(uid)
                .addSnapshotListener(MetadataChanges.INCLUDE, (document, failure) -> {
                    if (!current(token, uid)) return;
                    boolean previousActive = accountActive;
                    if (failure != null) {
                        accountLoaded = true; accountActive = false; accountCached = false;
                        accountError = failureMessage(failure, "Chưa kiểm tra được tài khoản. Kiểm tra mạng rồi thử lại.");
                    } else {
                        accountError = "";
                        accountCached = document != null && document.getMetadata().isFromCache();
                        accountLoaded = document != null && (document.exists() || !accountCached);
                        accountActive = document != null && document.exists() && "ACTIVE".equals(document.get("trang_thai"));
                    }
                    if (previousActive != accountActive) readDenied = false;
                    accessChanged();
                });
        tutorListener = db.collection("tutor_profiles").document(uid)
                .addSnapshotListener(MetadataChanges.INCLUDE, (document, failure) -> {
                    if (!current(token, uid)) return;
                    boolean previousVerified = tutorVerified;
                    if (failure != null) {
                        tutorLoaded = true; tutorVerified = false; tutorCached = false;
                        tutorError = failureMessage(failure, "Chưa kiểm tra được hồ sơ gia sư. Kiểm tra mạng rồi thử lại.");
                    } else {
                        tutorError = "";
                        tutorCached = document != null && document.getMetadata().isFromCache();
                        tutorLoaded = document != null && (document.exists() || !tutorCached);
                        tutorVerified = document != null && document.exists()
                                && "VERIFIED".equals(document.get("trang_thai_xac_minh"));
                    }
                    if (previousVerified != tutorVerified) readDenied = false;
                    accessChanged();
                });
    }

    private boolean current(int token, String uid) { return token == generation && uid.equals(owner()) && uid.equals(boundOwner); }
    private void accessChanged() {
        if (!authorized()) stopRequests();
        else if (requestsListener == null && !queryFailed) { listenRequests(); return; }
        changed();
    }

    private void stopRequests() {
        queryGeneration++;
        if (requestsListener != null) requestsListener.remove();
        requestsListener = null;
        records.clear(); loading = false; queryCached = false;
    }

    private void listenRequests() {
        stopRequests(); queryError = ""; queryFailed = false;
        if (!authorized()) { changed(); return; }
        final String uid = owner(), subjectId = selectedSubjectId();
        final int token = generation, queryToken = queryGeneration;
        Query query = db.collection("tutor_requests").whereEqualTo("trang_thai", "OPEN");
        if (!subjectId.isEmpty()) query = query.whereEqualTo("subject_id", subjectId);
        loading = true; changed();
        // Equalities only; sort locally so this flow does not require a composite index.
        requestsListener = query.addSnapshotListener(MetadataChanges.INCLUDE, (snapshot, failure) -> {
            if (!current(token, uid) || queryToken != queryGeneration || !subjectId.equals(selectedSubjectId()) || !authorized()) return;
            loading = false;
            if (failure != null) {
                queryFailed = true;
                queryError = failureMessage(failure, "Chưa tải được tin cần gia sư. Vui lòng thử lại.");
                if (accessDenied(failure)) {
                    readDenied = true;
                    stopRequests();
                }
            } else if (snapshot != null) {
                queryError = ""; queryFailed = false;
                queryCached = snapshot.getMetadata().isFromCache();
                records.clear();
                for (DocumentSnapshot document : snapshot.getDocuments()) {
                    TutorRequestRecord record = TutorRequestRecord.from(document);
                    if (record != null && record.isOpen() && (subjectId.isEmpty() || subjectId.equals(record.subjectId))) records.add(record);
                }
                records.sort(Comparator.comparingLong((TutorRequestRecord record) -> record.createdAtMillis)
                        .reversed().thenComparing(record -> record.id));
            }
            changed();
        });
    }

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
    private static String failureMessage(Throwable failure, String fallback) {
        Throwable cause = failure;
        while (cause != null) {
            if (cause instanceof FirebaseFirestoreException) {
                FirebaseFirestoreException.Code code = ((FirebaseFirestoreException) cause).getCode();
                if (code == FirebaseFirestoreException.Code.PERMISSION_DENIED)
                    return "Tài khoản chưa có quyền tìm tin. Vui lòng kiểm tra trạng thái tài khoản và hồ sơ gia sư.";
                if (code == FirebaseFirestoreException.Code.UNAUTHENTICATED)
                    return "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.";
                if (code == FirebaseFirestoreException.Code.UNAVAILABLE || code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED)
                    return "Chưa kết nối được máy chủ. Kiểm tra mạng rồi thử lại.";
            }
            if (cause.getCause() == cause) break;
            cause = cause.getCause();
        }
        return fallback;
    }

    @Override protected void onCleared() {
        generation++;
        auth.removeAuthStateListener(authListener);
        if (accountListener != null) accountListener.remove();
        if (tutorListener != null) tutorListener.remove();
        stopRequests();
    }
}