package com.example.tmdt;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * UC29 – Xử lý báo cáo vi phạm (màn 50), chỉ dành cho quản trị viên (users/{uid}.vai_tro = ADMIN).
 * Mở báo cáo mới tự chuyển "Đang xem xét". Hướng xử lý: yêu cầu bổ sung minh chứng, cảnh cáo, khóa tài khoản,
 * đề xuất hoàn tiền (≤ tổng hợp đồng), bác bỏ. Khóa tài khoản ghi users/{id}.trang_thai = BANNED kèm một bản ghi
 * admin_audit_logs (BAN_USER) trong cùng batch, giống chức năng khóa tài khoản sẵn có của quản trị.
 */
public final class ReportAdminState extends MemberState {
    public static final int MIN_CONCLUSION = 5, MAX_CONCLUSION = 500;

    private final List<Report> reports = new ArrayList<>();
    private ListenerRegistration listener;
    private String listUid;
    private boolean loading;
    private String loadError = "";
    /** Hợp đồng gắn với báo cáo (để giới hạn số tiền hoàn). */
    private final Map<String, Contract> contracts = new HashMap<>();
    private final Set<String> loadingContracts = new HashSet<>();
    /** Trạng thái tài khoản người bị báo cáo: ACTIVE / BANNED, vai trò ADMIN. */
    private final Map<String, String> targetStatus = new HashMap<>(), targetRole = new HashMap<>();
    private final Set<String> loadingTargets = new HashSet<>();
    private final Set<String> markedReviewing = new HashSet<>();

    public void start() {
        final String me = uid();
        if (me == null) { stop(); return; }
        if (me.equals(listUid) && listener != null) return;
        stop(); listUid = me; loading = true; notifyUi();
        listener = db.collection("reports").limit(500).addSnapshotListener((snapshot, error) -> {
            if (!me.equals(listUid)) return;
            loading = false;
            if (error != null || snapshot == null) { loadError = Docs.message(error); listener = null; notifyUi(); return; }
            reports.clear();
            for (DocumentSnapshot doc : snapshot.getDocuments()) reports.add(Report.from(doc));
            Collections.sort(reports, (a, b) -> Long.compare(b.createdAt, a.createdAt));
            loadError = ""; notifyUi();
        });
    }
    public void stop() {
        if (listener != null) listener.remove();
        listener = null; listUid = null; reports.clear(); loading = false; loadError = "";
        contracts.clear(); targetStatus.clear(); targetRole.clear(); markedReviewing.clear();
    }
    public void retry() { stop(); start(); }
    @Override protected void onCleared() { stop(); }

    public boolean loading() { return loading; }
    public String loadError() { return loadError; }
    public List<Report> reports(boolean finished) {
        List<Report> result = new ArrayList<>();
        for (Report r : reports) if (r.finished() == finished) result.add(r);
        return result;
    }
    /** Số báo cáo đang chờ xử lý (cho ô số liệu ở trang quản trị). */
    public int openCount() { int n = 0; for (Report r : reports) if (!r.finished()) n++; return n; }
    public Report report(String id) { if (id == null) return null; for (Report r : reports) if (r.id.equals(id)) return r; return null; }

    // Dữ liệu phụ ------------------------------------------------------------------------------------

    public Contract contract(String id) {
        if (id == null || id.isEmpty()) return null;
        Contract c = contracts.get(id);
        if (c == null && loadingContracts.add(id)) {
            db.collection("contracts").document(id).get().addOnCompleteListener(task -> {
                loadingContracts.remove(id);
                if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) contracts.put(id, Contract.from(task.getResult()));
                notifyUi();
            });
        }
        return c;
    }
    private final Map<String, Long> lessonPrices = new HashMap<>();
    private final Set<String> loadingLessons = new HashSet<>();
    /** Học phí buổi học (lessons/{id}.hoc_phi), -1 khi chưa tải xong. */
    public long lessonPrice(String id) {
        if (id == null || id.isEmpty()) return -1;
        Long price = lessonPrices.get(id);
        if (price == null && loadingLessons.add(id)) {
            db.collection("lessons").document(id).get().addOnCompleteListener(task -> {
                loadingLessons.remove(id);
                DocumentSnapshot d = task.isSuccessful() ? task.getResult() : null;
                lessonPrices.put(id, d != null && d.exists() ? Docs.number(d, "hoc_phi") : 0L);
                notifyUi();
            });
        }
        return price == null ? -1 : price;
    }
    /** Số tiền hoàn tối đa: tổng hợp đồng hoặc học phí buổi học; 0 = không hoàn được; -1 = đang tải. */
    public long refundCap(Report report) {
        if (Report.SUBJECT_CONTRACT.equals(report.subjectType)) { Contract c = contract(report.subjectId); return c == null ? -1 : c.total(); }
        if (Report.SUBJECT_LESSON.equals(report.subjectType)) return lessonPrice(report.subjectId);
        return 0;
    }

    /** "ACTIVE", "BANNED" hoặc "" khi chưa tải xong. */
    public String targetStatus(String uid) { loadTarget(uid); String s = targetStatus.get(uid); return s == null ? "" : s; }
    public boolean targetIsAdmin(String uid) { loadTarget(uid); return "ADMIN".equals(targetRole.get(uid)); }
    private void loadTarget(String uid) {
        if (uid == null || uid.isEmpty() || targetStatus.containsKey(uid) || !loadingTargets.add(uid)) return;
        db.collection("users").document(uid).get().addOnCompleteListener(task -> {
            loadingTargets.remove(uid);
            DocumentSnapshot doc = task.isSuccessful() ? task.getResult() : null;
            String status = doc != null && doc.exists() ? Docs.text(doc, "trang_thai") : "";
            targetStatus.put(uid, status.isEmpty() ? "ACTIVE" : status);
            targetRole.put(uid, doc != null && doc.exists() ? Docs.text(doc, "vai_tro") : "");
            notifyUi();
        });
    }

    // Ghi --------------------------------------------------------------------------------------------

    /** Mở báo cáo mới: chuyển "Đã tiếp nhận" → "Đang xem xét" (một lần). */
    public void startReviewing(Report report) {
        String me = uid();
        if (me == null || report == null || !Report.RECEIVED.equals(report.status) || !markedReviewing.add(report.id)) return;
        Map<String, Object> update = new HashMap<>();
        update.put("status", Report.REVIEWING); update.put("handler_id", me);
        update.put("reviewing_at", FieldValue.serverTimestamp()); update.put("updated_at", FieldValue.serverTimestamp());
        db.collection("reports").document(report.id).update(update);
    }

    /**
     * Quyết định xử lý. Trả về thông báo lỗi kiểm tra phía app, hoặc null khi đã gửi yêu cầu.
     * @param refund số tiền đề xuất hoàn (chỉ với hướng HOAN_TIEN)
     */
    public String decide(Report report, String outcome, String conclusion, long refund) {
        String me = uid();
        if (me == null || busy) return "Đang xử lý, vui lòng đợi.";
        if (report == null || report.finished()) return "Báo cáo đã được xử lý.";
        String text = conclusion == null ? "" : conclusion.trim();
        if (text.length() < MIN_CONCLUSION) return "Nhập kết luận hoặc nội dung cần bổ sung (ít nhất " + MIN_CONCLUSION + " ký tự).";
        if (text.length() > MAX_CONCLUSION) text = text.substring(0, MAX_CONCLUSION);
        boolean needInfo = Report.OUTCOME_NEED_INFO.equals(outcome);
        boolean ban = Report.OUTCOME_BAN.equals(outcome);
        if (ban) {
            if (report.targetId.equals(me)) return "Không thể tự khóa tài khoản của bạn.";
            if (targetIsAdmin(report.targetId)) return "Không khóa được tài khoản quản trị viên.";
            if (targetStatus(report.targetId).isEmpty()) return "Đang tải thông tin tài khoản, thử lại sau giây lát.";
        }
        if (Report.OUTCOME_REFUND.equals(outcome)) {
            long cap = refundCap(report);
            if (cap == 0) return "Chỉ hoàn tiền được cho báo cáo gắn với hợp đồng hoặc buổi học có học phí.";
            if (cap < 0) return "Đang tải học phí, thử lại sau giây lát.";
            if (refund <= 0 || refund > cap) return "Số tiền hoàn từ 1đ đến " + Docs.money(cap) + ".";
        } else refund = 0;

        Map<String, Object> update = new HashMap<>();
        update.put("handler_id", me); update.put("updated_at", FieldValue.serverTimestamp());
        if (report.reviewingAt == 0) update.put("reviewing_at", FieldValue.serverTimestamp());
        if (needInfo) {
            update.put("status", Report.NEED_INFO); update.put("admin_note", text);
        } else {
            update.put("status", Report.OUTCOME_DISMISS.equals(outcome) ? Report.DISMISSED : Report.RESOLVED);
            update.put("outcome", outcome); update.put("conclusion", text);
            update.put("refund_amount", refund); update.put("decided_at", FieldValue.serverTimestamp());
        }
        WriteBatch batch = db.batch();
        batch.update(db.collection("reports").document(report.id), update);
        boolean alreadyBanned = "BANNED".equals(targetStatus(report.targetId));
        if (ban && !alreadyBanned) {
            Map<String, Object> user = new HashMap<>();
            user.put("trang_thai", "BANNED"); user.put("updated_at", FieldValue.serverTimestamp());
            batch.update(db.collection("users").document(report.targetId), user);
            Map<String, Object> log = new HashMap<>();
            log.put("actor_id", me); log.put("target_id", report.targetId); log.put("action", "BAN_USER");
            log.put("before", "ACTIVE"); log.put("after", "BANNED");
            String reason = "Báo cáo " + report.code + ": " + text;
            log.put("reason", reason.length() > 500 ? reason.substring(0, 500) : reason);
            log.put("created_at", FieldValue.serverTimestamp());
            batch.set(db.collection("admin_audit_logs").document(), log);
        }
        busy = true; notifyUi();
        final String code = report.code;
        final String target = report.targetId;
        batch.commit().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) { failed(task.getException()); return; }
            if (ban) targetStatus.put(target, "BANNED");
            done(needInfo ? "Đã gửi yêu cầu bổ sung minh chứng." : "Đã xử lý báo cáo " + code + ".");
        });
        return null;
    }
}
