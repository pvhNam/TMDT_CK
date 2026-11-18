package com.example.tmdt;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Một tài khoản đăng ký cho nhiều học viên (Thành viên 4): danh sách người học của tài khoản
 * (users/{uid}/learners/{id}), ví dụ phụ huynh thêm các con. Khi đặt lịch 1-1 hoặc đăng ký lớp nhóm,
 * tài khoản chọn học cho bản thân hay cho một người học trong danh sách.
 */
public final class LearnerState extends MemberState {
    public static final int MAX_LEARNERS = 10, MAX_NAME = 100, MAX_SHORT = 40, MAX_NOTE = 200;

    /** Một người học thuộc tài khoản. */
    public static final class Learner {
        public String id = "", name = "", grade = "", level = "", relation = "", note = "";
        public long createdAt;
        public String summary() {
            StringBuilder s = new StringBuilder();
            for (String part : new String[]{relation, grade, level}) if (!part.isEmpty()) { if (s.length() > 0) s.append(" · "); s.append(part); }
            return s.toString();
        }
    }

    private final List<Learner> learners = new ArrayList<>();
    private ListenerRegistration listener;
    private String listUid;
    private boolean loading;
    private String loadError = "";

    public void start() {
        final String me = uid();
        if (me == null) { stop(); return; }
        if (me.equals(listUid) && listener != null) return;
        stop(); listUid = me; loading = true; notifyUi();
        listener = collection(me).addSnapshotListener((snapshot, error) -> {
            if (!me.equals(listUid)) return;
            loading = false;
            if (error != null || snapshot == null) { loadError = Docs.message(error); listener = null; notifyUi(); return; }
            learners.clear();
            for (DocumentSnapshot d : snapshot.getDocuments()) {
                Learner l = new Learner(); l.id = d.getId(); l.name = Docs.text(d, "ho_ten"); l.grade = Docs.text(d, "lop");
                l.level = Docs.text(d, "cap_hoc"); l.relation = Docs.text(d, "quan_he"); l.note = Docs.text(d, "ghi_chu");
                try { l.createdAt = Docs.millis(d, "created_at"); } catch (RuntimeException ignored) { l.createdAt = 0; }
                learners.add(l);
            }
            Collections.sort(learners, (a, b) -> Long.compare(a.createdAt, b.createdAt));
            loadError = ""; notifyUi();
        });
    }
    public void stop() {
        if (listener != null) listener.remove();
        listener = null; listUid = null; learners.clear(); loading = false; loadError = "";
    }
    public void retry() { stop(); start(); }
    @Override protected void onCleared() { stop(); }

    public boolean loading() { return loading; }
    public String loadError() { return loadError; }
    public List<Learner> learners() { return Collections.unmodifiableList(learners); }
    public Learner learner(String id) { if (id == null) return null; for (Learner l : learners) if (l.id.equals(id)) return l; return null; }

    private CollectionReference collection(String me) { return db.collection("users").document(me).collection("learners"); }

    /** Thêm (id rỗng) hoặc sửa người học. Trả về lỗi kiểm tra, hoặc null khi đã gửi yêu cầu. */
    public String save(String id, String name, String grade, String level, String relation, String note) {
        String me = uid();
        if (me == null) return "Bạn cần đăng nhập.";
        if (busy) return "Đang lưu, vui lòng đợi.";
        String n = clean(name), g = clean(grade), lv = clean(level), r = clean(relation), t = clean(note);
        if (n.length() < 2 || n.length() > MAX_NAME) return "Họ tên người học từ 2 đến " + MAX_NAME + " ký tự.";
        if (g.length() > MAX_SHORT || lv.length() > MAX_SHORT || r.length() > MAX_SHORT) return "Lớp, cấp học, quan hệ tối đa " + MAX_SHORT + " ký tự.";
        if (t.length() > MAX_NOTE) return "Ghi chú tối đa " + MAX_NOTE + " ký tự.";
        boolean create = id == null || id.isEmpty();
        if (create && learners.size() >= MAX_LEARNERS) return "Mỗi tài khoản có tối đa " + MAX_LEARNERS + " người học.";
        for (Learner l : learners) if (!l.id.equals(id) && l.name.equalsIgnoreCase(n)) return "Đã có người học tên " + n + ".";
        Map<String, Object> data = new HashMap<>();
        data.put("ho_ten", n); data.put("lop", g); data.put("cap_hoc", lv); data.put("quan_he", r); data.put("ghi_chu", t);
        data.put("updated_at", FieldValue.serverTimestamp());
        busy = true; notifyUi();
        if (create) {
            data.put("created_at", FieldValue.serverTimestamp());
            collection(me).document().set(data).addOnCompleteListener(task -> {
                if (task.isSuccessful()) done("Đã thêm " + n + "."); else failed(task.getException());
            });
        } else {
            collection(me).document(id).update(data).addOnCompleteListener(task -> {
                if (task.isSuccessful()) done("Đã cập nhật " + n + "."); else failed(task.getException());
            });
        }
        return null;
    }

    /** Xóa người học khỏi danh sách. Lịch học và đăng ký đã gửi vẫn giữ tên người học. */
    public String delete(Learner learner) {
        String me = uid();
        if (me == null) return "Bạn cần đăng nhập.";
        if (busy) return "Đang lưu, vui lòng đợi.";
        busy = true; notifyUi();
        collection(me).document(learner.id).delete().addOnCompleteListener(task -> {
            if (task.isSuccessful()) done("Đã xóa " + learner.name + " khỏi danh sách."); else failed(task.getException());
        });
        return null;
    }

    private static String clean(String value) { return value == null ? "" : value.trim().replaceAll("\\s+", " "); }
}
