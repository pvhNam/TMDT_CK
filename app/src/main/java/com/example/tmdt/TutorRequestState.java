package com.example.tmdt;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import java.util.Map;
import java.util.UUID;

/** Keeps the draft and an in-flight publication across activity recreation. */
public final class TutorRequestState extends ViewModel {
    private final SavedStateHandle saved;
    private final MutableLiveData<Integer> changes = new MutableLiveData<>(0);
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth.AuthStateListener authListener = ignored -> bindOwner();
    private boolean busy;
    private String error = "";
    private int generation;

    public TutorRequestState(SavedStateHandle saved) {
        this.saved = saved;
        bindOwner(); auth.addAuthStateListener(authListener);
    }
    public LiveData<Integer> changes() { return changes; }
    public boolean busy() { return busy; }
    public String error() { return error; }
    public boolean published() { return Boolean.TRUE.equals(saved.get("published")); }
    public String draft(String key) { String value = saved.get(key); return value == null ? "" : value; }
    public void draft(String key, String value) { if (!busy) saved.set(key, value); }
    public String publishedText() { return draft("published_text"); }
    public void reportError(String message) { error = message; changed(); }
    private void changed() { changes.setValue(changes.getValue() + 1); }
    private String owner() { return auth.getCurrentUser() == null ? "" : auth.getCurrentUser().getUid(); }
    private void bindOwner() {
        String uid = owner();
        if (!uid.equals(draft("owner"))) {
            generation++; busy = false;
            reset(); saved.set("owner", uid);
        }
    }
    public void reset() {
        if (busy) return;
        for (String key : new String[]{"title", "subject_id", "subject_name", "level", "budget", "schedule", "description", "request_id", "published_text"}) saved.remove(key);
        saved.set("published", false); error = ""; changed();
    }
    public void publish(TutorRequest request) {
        if (busy || published()) return;
        String uid = owner();
        if (uid.isEmpty()) { reportError("Vui lòng đăng nhập để đăng tin."); return; }
        if (!request.invalidField().isEmpty()) { reportError("Vui lòng kiểm tra đầy đủ thông tin đăng tin."); return; }
        if (draft("request_id").isEmpty()) saved.set("request_id", uid + "_" + UUID.randomUUID());
        DocumentReference ref = db.collection("tutor_requests").document(draft("request_id"));
        int token = generation;
        busy = true; error = ""; changed();
        // A stable document ID lets a retry recover an acknowledged publication without creating another post.
        db.runTransaction(tx -> {
            DocumentSnapshot existing = tx.get(ref);
            if (existing.exists()) return existing.getData();
            DocumentSnapshot account = tx.get(db.collection("users").document(uid));
            DocumentSnapshot subject = tx.get(db.collection("subjects").document(request.subjectId));
            if (!account.exists() || !"ACTIVE".equals(account.getString("trang_thai")))
                throw new IllegalStateException("Hãy hoàn thiện hồ sơ tài khoản trước khi đăng tin.");
            if (!subject.exists() || !Boolean.TRUE.equals(subject.getBoolean("dang_su_dung"))
                    || !request.subjectName.equals(subject.getString("ten_mon")))
                throw new IllegalStateException("Môn học đã thay đổi. Vui lòng tải lại danh mục và chọn lại.");
            Map<String, Object> data = request.fields(uid, account.getString("ho_ten"));
            data.put("created_at", FieldValue.serverTimestamp()); data.put("updated_at", FieldValue.serverTimestamp());
            tx.set(ref, data); return data;
        }).addOnCompleteListener(task -> {
            if (token != generation || !uid.equals(owner())) return;
            busy = false;
            if (task.isSuccessful()) {
                Map<String, Object> data = task.getResult();
                saved.set("published_text", data.get("tieu_de") + "\n" + data.get("mon_hoc") + " · "
                        + CatalogState.level((String) data.get("cap_hoc")) + "\n"
                        + CatalogState.money(((Number) data.get("ngan_sach")).longValue()) + " / buổi\n"
                        + data.get("lich_hoc") + "\n\n" + data.get("mo_ta"));
                saved.set("published", true);
            } else {
                Throwable cause = task.getException();
                while (cause != null && cause.getCause() != null) cause = cause.getCause();
                if (cause instanceof IllegalStateException) error = cause.getMessage();
                else if (task.getException() instanceof FirebaseFirestoreException
                        && ((FirebaseFirestoreException) task.getException()).getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED)
                    error = "Không thể đăng tin với tài khoản này. Vui lòng kiểm tra hồ sơ hoặc liên hệ hỗ trợ.";
                else error = "Chưa đăng được tin. Kiểm tra kết nối rồi thử lại; thông tin đã nhập vẫn được giữ.";
            }
            changed();
        });
    }
    @Override protected void onCleared() { auth.removeAuthStateListener(authListener); }
}
