package com.example.tmdt;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dữ liệu cho màn thống kê của quản trị viên: người dùng, gia sư, lớp nhóm, đăng ký lớp và lịch học 1-1.
 */
public final class ManageState extends MemberState {
    public static final String USERS = "users", TUTORS = "tutors", CLASSES = "classes", REGISTRATIONS = "registrations", LESSONS = "lessons";

    /** Tài khoản người dùng (users/{uid}). */
    public static final class User {
        public String id = "", name = "", email = "", phone = "", region = "", role = "STUDENT", status = "ACTIVE";
        public boolean tutor; public long createdAt;
        public boolean admin() { return "ADMIN".equals(role); }
        public boolean banned() { return "BANNED".equals(status); }
        public String roleLabel() { return admin() ? "Quản trị viên" : tutor ? "Gia sư" : "Học viên"; }
    }
    /** Hồ sơ gia sư (tutor_profiles/{uid}). */
    public static final class Tutor {
        public String id = "", name = "", region = "", school = "", major = "", mode = "", status = "";
        public boolean visible; public long experience, createdAt;
        public String statusLabel() {
            switch (status) {
                case "VERIFIED": return "Đã duyệt";
                case "PENDING": return "Chờ duyệt";
                case "IN_REVIEW": return "Đang xét";
                case "NEEDS_REVISION": return "Cần bổ sung";
                case "REJECTED": return "Từ chối";
                default: return status.isEmpty() ? "Chưa rõ" : status;
            }
        }
    }
    /** Lớp nhóm (group_classes/{id}). */
    public static final class GroupRow {
        public String id = "", title = "", subject = "", level = "", tutorId = "", tutorName = "", mode = "", status = "", startDate = "";
        public int members, capacity, sessions; public long price, createdAt;
    }
    /** Một đăng ký học: đăng ký lớp nhóm (class_registrations) hoặc lịch học 1-1 (lessons). */
    public static final class Registration {
        public String id = "", kind = "", subject = "", title = "", tutorId = "", tutorName = "", studentId = "", studentName = "", status = "", classId = "";
        public boolean trial, finished; public long createdAt, startAt, price;
        public boolean group() { return REGISTRATIONS.equals(kind); }
        /** Trạng thái hiển thị: buổi 1-1 đã học xong tính là "Đã hoàn thành". */
        public String statusKey() { return !group() && finished ? "FINISHED" : status; }
    }

    private final Map<String, ListenerRegistration> listeners = new HashMap<>();
    private final Map<String, Boolean> loaded = new HashMap<>();
    private final Map<String, String> errors = new HashMap<>();
    private final List<User> users = new ArrayList<>();
    private final List<Tutor> tutors = new ArrayList<>();
    private final List<GroupRow> classes = new ArrayList<>();
    private final List<Registration> groupRegistrations = new ArrayList<>(), lessons = new ArrayList<>();
    private String listUid;

    /** Bắt đầu nghe dữ liệu (chỉ gọi khi người dùng là quản trị viên). */
    public void start() {
        String me = uid();
        if (me == null) { stop(); return; }
        if (me.equals(listUid)) return;
        stop(); listUid = me;
        listen(USERS, "users");
        listen(TUTORS, "tutor_profiles");
        listen(CLASSES, "group_classes");
        listen(REGISTRATIONS, "class_registrations");
        listen(LESSONS, "lessons");
    }
    private void listen(String key, String collection) {
        final String me = listUid;
        listeners.put(key, db.collection(collection).addSnapshotListener((snapshot, error) -> {
            if (me == null || !me.equals(listUid)) return;
            loaded.put(key, true);
            if (error != null || snapshot == null) { errors.put(key, Docs.message(error)); notifyUi(); return; }
            errors.remove(key);
            parse(key, snapshot);
            notifyUi();
        }));
    }
    public void stop() {
        for (ListenerRegistration l : listeners.values()) if (l != null) l.remove();
        listeners.clear(); loaded.clear(); errors.clear(); listUid = null;
        users.clear(); tutors.clear(); classes.clear(); groupRegistrations.clear(); lessons.clear();
    }
    public void retry() { stop(); start(); }
    /** Tăng mỗi khi dữ liệu đổi, để màn hình biết cần vẽ lại. */
    public int version() { Integer v = changes().getValue(); return v == null ? 0 : v; }
    @Override protected void onCleared() { stop(); }

    private void parse(String key, QuerySnapshot snapshot) {
        switch (key) {
            case USERS:
                users.clear();
                for (DocumentSnapshot d : snapshot.getDocuments()) {
                    User u = new User(); u.id = d.getId(); u.name = Docs.text(d, "ho_ten"); u.email = Docs.text(d, "email");
                    u.phone = Docs.text(d, "so_dien_thoai"); u.region = Docs.text(d, "khu_vuc");
                    String role = Docs.text(d, "vai_tro"); if (!role.isEmpty()) u.role = role;
                    String status = Docs.text(d, "trang_thai"); if (!status.isEmpty()) u.status = status;
                    u.tutor = Docs.flag(d, "la_gia_su"); u.createdAt = time(d, "created_at");
                    users.add(u);
                }
                Collections.sort(users, (a, b) -> Long.compare(b.createdAt, a.createdAt));
                break;
            case TUTORS:
                tutors.clear();
                for (DocumentSnapshot d : snapshot.getDocuments()) {
                    Tutor t = new Tutor(); t.id = d.getId(); t.name = Docs.text(d, "ho_ten"); t.region = Docs.text(d, "khu_vuc");
                    t.school = Docs.text(d, "truong"); t.major = Docs.text(d, "chuyen_nganh"); t.mode = Docs.text(d, "hinh_thuc_day");
                    t.status = Docs.text(d, "trang_thai_xac_minh"); t.visible = Docs.flag(d, "nhan_lop");
                    t.experience = Docs.number(d, "so_nam_kinh_nghiem"); t.createdAt = time(d, "created_at");
                    tutors.add(t);
                }
                Collections.sort(tutors, (a, b) -> Long.compare(b.createdAt, a.createdAt));
                break;
            case CLASSES:
                classes.clear();
                for (DocumentSnapshot d : snapshot.getDocuments()) {
                    GroupRow c = new GroupRow(); c.id = d.getId(); c.title = Docs.text(d, "ten_lop"); c.subject = Docs.text(d, "mon_hoc");
                    c.level = Docs.text(d, "cap_hoc"); c.tutorId = Docs.text(d, "tutor_id"); c.tutorName = Docs.text(d, "ten_gia_su");
                    c.mode = Docs.text(d, "hinh_thuc"); c.status = Docs.text(d, "trang_thai"); c.startDate = Docs.text(d, "ngay_bat_dau");
                    c.members = Docs.strings(d.get("thanh_vien")).size(); c.capacity = (int) Docs.number(d, "suc_chua");
                    c.sessions = (int) Docs.number(d, "so_buoi"); c.price = Docs.number(d, "hoc_phi"); c.createdAt = time(d, "created_at");
                    classes.add(c);
                }
                Collections.sort(classes, (a, b) -> Long.compare(b.createdAt, a.createdAt));
                break;
            case REGISTRATIONS:
                groupRegistrations.clear();
                for (DocumentSnapshot d : snapshot.getDocuments()) {
                    Registration r = new Registration(); r.id = d.getId(); r.kind = REGISTRATIONS; r.classId = Docs.text(d, "class_id");
                    r.tutorId = Docs.text(d, "tutor_id"); r.studentId = Docs.text(d, "student_id"); r.studentName = Docs.text(d, "ten_hoc_vien");
                    r.status = Docs.text(d, "trang_thai"); r.createdAt = time(d, "created_at");
                    groupRegistrations.add(r);
                }
                Collections.sort(groupRegistrations, (a, b) -> Long.compare(b.createdAt, a.createdAt));
                break;
            case LESSONS:
                lessons.clear();
                for (DocumentSnapshot d : snapshot.getDocuments()) {
                    Registration r = new Registration(); r.id = d.getId(); r.kind = LESSONS; r.title = Docs.text(d, "tieu_de");
                    r.subject = AdminStats.subjectOf(r.title); r.tutorId = Docs.text(d, "tutor_id"); r.tutorName = Docs.text(d, "ten_gia_su");
                    r.studentId = Docs.text(d, "student_id"); r.studentName = Docs.text(d, "ten_hoc_vien"); r.status = Docs.text(d, "trang_thai");
                    r.trial = Docs.flag(d, "hoc_thu"); r.finished = Docs.flag(d, "da_hoan_thanh"); r.price = Docs.number(d, "hoc_phi");
                    r.createdAt = time(d, "created_at"); r.startAt = time(d, "bat_dau");
                    lessons.add(r);
                }
                Collections.sort(lessons, (a, b) -> Long.compare(b.createdAt, a.createdAt));
                break;
            default:
        }
    }
    /** Đọc thời gian an toàn: trường thiếu hoặc sai kiểu trả 0 thay vì làm hỏng cả danh sách. */
    private static long time(DocumentSnapshot d, String field) {
        try { return Docs.millis(d, field); } catch (RuntimeException wrongType) { return 0; }
    }

    // Đọc -------------------------------------------------------------------------------------------

    public boolean loaded(String key) { return Boolean.TRUE.equals(loaded.get(key)); }
    public boolean loadedAll() { for (String k : new String[]{USERS, TUTORS, CLASSES, REGISTRATIONS, LESSONS}) if (!loaded(k)) return false; return true; }
    public String error(String key) { String e = errors.get(key); return e == null ? "" : e; }
    /** Lỗi đầu tiên (nếu có) của các nguồn dữ liệu. */
    public String firstError() { for (String e : errors.values()) if (!e.isEmpty()) return e; return ""; }
    public List<User> users() { return Collections.unmodifiableList(users); }
    public List<Tutor> tutors() { return Collections.unmodifiableList(tutors); }
    public List<GroupRow> classes() { return Collections.unmodifiableList(classes); }
    public GroupRow groupClass(String id) { for (GroupRow c : classes) if (c.id.equals(id)) return c; return null; }
    public User user(String id) { for (User u : users) if (u.id.equals(id)) return u; return null; }
    public Tutor tutor(String id) { for (Tutor t : tutors) if (t.id.equals(id)) return t; return null; }
    /** Đăng ký lớp nhóm đã gắn tên lớp, môn, gia sư từ lớp tương ứng. */
    public List<Registration> groupRegistrations() {
        for (Registration r : groupRegistrations) {
            GroupRow c = groupClass(r.classId);
            if (c != null) { r.title = c.title; r.subject = c.subject; r.tutorName = c.tutorName; r.price = c.price; }
        }
        return Collections.unmodifiableList(groupRegistrations);
    }
    public List<Registration> lessons() { return Collections.unmodifiableList(lessons); }
    /** Mọi đăng ký học (lịch 1-1 và đăng ký lớp nhóm), mới nhất trước. */
    public List<Registration> allRegistrations() {
        List<Registration> all = new ArrayList<>(lessons);
        all.addAll(groupRegistrations());
        Collections.sort(all, (a, b) -> Long.compare(b.createdAt, a.createdAt));
        return all;
    }
    public List<Registration> registrationsOfClass(String classId) {
        List<Registration> result = new ArrayList<>();
        for (Registration r : groupRegistrations()) if (r.classId.equals(classId)) result.add(r);
        return result;
    }

    // Ghi -------------------------------------------------------------------------------------------

    /** Lý do tối thiểu khi khoá/mở khoá/đổi quyền (rules kiểm tra 5–500 ký tự). */
    public static final int MIN_REASON = 5, MAX_REASON = 500;

    /** Cấp hoặc thu hồi quyền quản trị. Không áp dụng cho chính mình và tài khoản đang bị khoá. */
    public String setAdmin(User user, boolean admin, String reason) {
        String me = uid();
        if (me == null || busy) return "Đang xử lý, vui lòng đợi.";
        if (user.id.equals(me)) return "Không thể tự thay đổi quyền của chính bạn.";
        if (user.banned()) return "Hãy mở khoá tài khoản trước khi đổi quyền.";
        if (user.admin() == admin) return admin ? "Tài khoản đã là quản trị viên." : "Tài khoản không có quyền quản trị.";
        String why = reason == null ? "" : reason.trim();
        if (why.length() < MIN_REASON) return "Nhập lý do ít nhất " + MIN_REASON + " ký tự.";
        write(user, "vai_tro", user.role, admin ? "ADMIN" : "STUDENT", admin ? "GRANT_ADMIN" : "REVOKE_ADMIN", why,
                admin ? "Đã cấp quyền quản trị cho " + user.name + "." : "Đã thu hồi quyền quản trị của " + user.name + ".");
        return null;
    }
    private void write(User user, String field, String before, String after, String action, String reason, String success) {
        busy = true; notifyUi();
        DocumentReference ref = db.collection("users").document(user.id);
        Map<String, Object> update = new HashMap<>();
        update.put(field, after); update.put("updated_at", FieldValue.serverTimestamp());
        Map<String, Object> log = new HashMap<>();
        log.put("actor_id", uid()); log.put("target_id", user.id); log.put("action", action);
        log.put("before", before); log.put("after", after); log.put("reason", reason);
        log.put("created_at", FieldValue.serverTimestamp());
        WriteBatch batch = db.batch();
        batch.update(ref, update);
        batch.set(db.collection("admin_audit_logs").document(), log);
        batch.commit().addOnCompleteListener(task -> { if (task.isSuccessful()) done(success); else failed(task.getException()); });
    }
}
