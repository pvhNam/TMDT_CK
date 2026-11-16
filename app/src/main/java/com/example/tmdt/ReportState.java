package com.example.tmdt;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * UC27 – Gửi báo cáo vi phạm (màn 47) và theo dõi báo cáo (màn 48).
 * reports/{id} chỉ người gửi và quản trị viên đọc được; người bị báo cáo không xem được.
 * Minh chứng là ảnh JPEG đã nén, lưu base64 tại reports/{id}/evidence/e1..e5 (không cần Firebase Storage).
 */
public final class ReportState extends MemberState {
    /** Ảnh minh chứng đã chọn, chưa gửi. */
    public static final class Attachment {
        public final String name, data; public final long size;
        public Attachment(String name, String data, long size) { this.name = name; this.data = data; this.size = size; }
    }
    /** Đối tượng có thể báo cáo: một hợp đồng hoặc một cuộc trò chuyện của người dùng. */
    public static final class Subject {
        public final String type, id, label, targetId, targetName, targetRole;
        public Subject(String type, String id, String label, String targetId, String targetName, String targetRole) {
            this.type = type; this.id = id; this.label = label; this.targetId = targetId; this.targetName = targetName; this.targetRole = targetRole;
        }
    }
    public static final int MAX_DATA = 700_000;

    private final List<Report> reports = new ArrayList<>();
    private ListenerRegistration listener;
    private String listUid;
    private boolean loading;
    private String loadError = "";
    private final Map<String, List<Report.Evidence>> evidence = new HashMap<>();
    private final java.util.Set<String> loadingEvidence = new java.util.HashSet<>();
    /** Số minh chứng của báo cáo tại lần tải gần nhất. */
    private final Map<String, Integer> evidenceFor = new HashMap<>();
    private String sentId;
    // Bản nháp màn 47 (giữ khi xoay máy hoặc khi đi chọn ảnh).
    public String draftSubject, draftReason, draftDescription = "";
    public boolean draftConfirmed;
    public final List<Attachment> draftFiles = new ArrayList<>();
    public void clearDraft() { draftSubject = null; draftReason = null; draftDescription = ""; draftConfirmed = false; draftFiles.clear(); }

    public void start() {
        final String me = uid();
        if (me == null) { stop(); return; }
        if (me.equals(listUid) && listener != null) return;
        stop(); listUid = me; loading = true; notifyUi();
        listener = db.collection("reports").whereEqualTo("reporter_id", me).addSnapshotListener((snapshot, error) -> {
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
        listener = null; listUid = null; reports.clear(); evidence.clear(); evidenceFor.clear(); loading = false; loadError = "";
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
    public String takeSent() { String value = sentId; sentId = null; return value; }

    public void submit(Subject subject, String reporterName, String reporterRole, String reason, String description, List<Attachment> files) {
        final String me = uid();
        if (busy || me == null || subject == null || files.size() > Report.MAX_EVIDENCE) return;
        DocumentReference ref = db.collection("reports").document();
        Map<String, Object> data = new HashMap<>();
        data.put("code", Docs.code("BC", ref.getId()));
        data.put("reporter_id", me); data.put("reporter_name", reporterName); data.put("reporter_role", reporterRole);
        data.put("target_id", subject.targetId); data.put("target_name", subject.targetName); data.put("target_role", subject.targetRole);
        data.put("subject_type", subject.type); data.put("subject_id", subject.id); data.put("subject_label", subject.label);
        data.put("reason", reason); data.put("description", description.trim());
        data.put("evidence_count", files.size()); data.put("status", Report.RECEIVED);
        data.put("admin_note", ""); data.put("outcome", ""); data.put("conclusion", ""); data.put("refund_amount", 0);
        data.put("created_at", FieldValue.serverTimestamp()); data.put("updated_at", FieldValue.serverTimestamp());
        WriteBatch batch = db.batch();
        batch.set(ref, data);
        for (int i = 0; i < files.size(); i++) batch.set(ref.collection("evidence").document("e" + (i + 1)), evidenceData(me, files.get(i)));
        busy = true; notifyUi();
        final String id = ref.getId();
        batch.commit().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) { failed(task.getException()); return; }
            sentId = id; clearDraft(); done("Đã gửi báo cáo. Quản trị viên sẽ xem xét và phản hồi cho bạn.");
        });
    }

    /** Bổ sung minh chứng cho báo cáo chưa kết thúc. Báo cáo đang "Cần bổ sung" được chuyển lại "Đang xem xét". */
    public void addEvidence(Report report, List<Attachment> files) {
        final String me = uid();
        if (busy || me == null || report.finished() || files.isEmpty()) return;
        int total = report.evidenceCount + files.size();
        if (total > Report.MAX_EVIDENCE) { notice = "Mỗi báo cáo có tối đa " + Report.MAX_EVIDENCE + " minh chứng."; notifyUi(); return; }
        DocumentReference ref = db.collection("reports").document(report.id);
        WriteBatch batch = db.batch();
        for (int i = 0; i < files.size(); i++)
            batch.set(ref.collection("evidence").document("e" + (report.evidenceCount + i + 1)), evidenceData(me, files.get(i)));
        Map<String, Object> update = new HashMap<>();
        update.put("evidence_count", total); update.put("updated_at", FieldValue.serverTimestamp());
        if (Report.NEED_INFO.equals(report.status)) update.put("status", Report.REVIEWING);
        batch.update(ref, update);
        busy = true; notifyUi();
        batch.commit().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) { failed(task.getException()); return; }
            fetchEvidence(report.id);
            done("Đã bổ sung minh chứng.");
        });
    }
    private static Map<String, Object> evidenceData(String me, Attachment file) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", file.name); data.put("size", file.size); data.put("data", file.data);
        data.put("uploaded_by", me); data.put("created_at", FieldValue.serverTimestamp());
        return data;
    }

    /** Tải minh chứng của một báo cáo (người gửi hoặc quản trị viên). */
    public void loadEvidence(String reportId) {
        if (evidence.containsKey(reportId)) return;
        fetchEvidence(reportId);
    }
    private void fetchEvidence(String reportId) {
        if (!loadingEvidence.add(reportId)) return;
        db.collection("reports").document(reportId).collection("evidence").get().addOnCompleteListener(task -> {
            loadingEvidence.remove(reportId);
            List<Report.Evidence> items = new ArrayList<>();
            if (task.isSuccessful() && task.getResult() != null)
                for (DocumentSnapshot doc : task.getResult().getDocuments()) items.add(Report.Evidence.from(doc));
            Collections.sort(items, (a, b) -> a.id.compareTo(b.id));
            evidence.put(reportId, items); notifyUi();
        });
    }
    /**
     * Minh chứng đã tải của một báo cáo, null nếu chưa có. Tự tải lại khi số minh chứng trên báo cáo đã thay đổi
     * (ví dụ người gửi vừa bổ sung).
     */
    public List<Report.Evidence> evidence(Report report) {
        Integer known = evidenceFor.get(report.id);
        if ((known == null || known != report.evidenceCount) && !loadingEvidence.contains(report.id)) {
            evidenceFor.put(report.id, report.evidenceCount);
            fetchEvidence(report.id);
        }
        return evidence.get(report.id);
    }
}
