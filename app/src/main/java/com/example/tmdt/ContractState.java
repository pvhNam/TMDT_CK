package com.example.tmdt;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * UC23 – Chốt hợp đồng dạy. Dữ liệu contracts/{id}; chỉ hai bên của hợp đồng (và quản trị viên) đọc được.
 * Gia sư gửi hợp đồng cho buổi học đã chấp nhận (tự xác nhận phần của mình); học viên xác nhận hoặc từ chối;
 * gia sư có thể hủy khi học viên chưa xác nhận.
 */
public final class ContractState extends MemberState {
    private final List<Contract> contracts = new ArrayList<>();
    private ListenerRegistration listener;
    private String listUid;
    private boolean loading;
    private String loadError = "";
    /** Mã hợp đồng vừa tạo, để màn 40 chuyển từ chế độ soạn sang chế độ xem. */
    private String created;
    /** Hợp đồng đang soạn (chưa gửi). Giữ trong ViewModel để xoay màn hình không mất bản nháp. */
    Contract draft;

    public void start() {
        final String me = uid();
        if (me == null) { stop(); return; }
        if (me.equals(listUid) && listener != null) return;
        stop(); listUid = me; loading = true; notifyUi();
        listener = db.collection("contracts").whereArrayContains("member_ids", me).addSnapshotListener((snapshot, error) -> {
            if (!me.equals(listUid)) return;
            loading = false;
            if (error != null || snapshot == null) { loadError = Docs.message(error); listener = null; notifyUi(); return; }
            contracts.clear();
            for (DocumentSnapshot doc : snapshot.getDocuments()) contracts.add(Contract.from(doc));
            Collections.sort(contracts, (a, b) -> Long.compare(b.createdAt, a.createdAt));
            loadError = ""; notifyUi();
        });
    }
    public void stop() {
        if (listener != null) listener.remove();
        listener = null; listUid = null; contracts.clear(); loading = false; loadError = "";
    }
    public void retry() { stop(); start(); }
    @Override protected void onCleared() { stop(); }

    public boolean loading() { return loading; }
    public String loadError() { return loadError; }
    public List<Contract> contracts() { return Collections.unmodifiableList(contracts); }
    public Contract contract(String id) { for (Contract c : contracts) if (c.id.equals(id)) return c; return null; }
    /** Hợp đồng mới nhất của một buổi học, ưu tiên hợp đồng còn hiệu lực hoặc đang chờ. */
    public Contract forLesson(String lessonId) {
        Contract fallback = null;
        for (Contract c : contracts) {
            if (!c.lessonId.equals(lessonId)) continue;
            if (!c.closed()) return c;
            if (fallback == null) fallback = c;
        }
        return fallback;
    }
    /** Hợp đồng đã chốt mà người dùng là một bên, dùng cho đánh giá và báo cáo. */
    public List<Contract> agreed() {
        List<Contract> result = new ArrayList<>();
        for (Contract c : contracts) if (c.agreed()) result.add(c);
        return result;
    }
    /** Số hợp đồng đang chờ người dùng xác nhận (huy hiệu ở mục Cá nhân). */
    public int awaitingMe() {
        String me = uid(); int count = 0;
        for (Contract c : contracts) if (c.awaits(me)) count++;
        return count;
    }
    public String takeCreated() { String value = created; created = null; return value; }

    /** Gia sư gửi hợp đồng cho học viên. Gia sư được tính là đã xác nhận. */
    public void propose(Contract draft) {
        final String me = uid();
        if (busy || me == null || !me.equals(draft.tutorId)) return;
        busy = true; notifyUi();
        DocumentReference ref = db.collection("contracts").document();
        final String id = ref.getId();
        Map<String, Object> data = new HashMap<>();
        data.put("code", Docs.code("HĐ", id));
        data.put("lesson_id", draft.lessonId);
        data.put("student_id", draft.studentId); data.put("tutor_id", draft.tutorId);
        data.put("member_ids", Arrays.asList(draft.studentId, draft.tutorId));
        data.put("student_name", draft.studentName); data.put("tutor_name", draft.tutorName);
        data.put("subject", draft.subject); data.put("sessions", draft.sessions); data.put("minutes", draft.minutes);
        data.put("schedule", draft.schedule); data.put("start_date", draft.startDate);
        data.put("start_at", new Timestamp(new Date(draft.startAt)));
        data.put("mode", draft.mode); data.put("fee", draft.fee); data.put("total", draft.total()); data.put("terms", draft.terms);
        data.put("base_terms", draft.baseTerms); data.put("template_version", draft.templateVersion);
        data.put("proposed_by", me);
        data.put("student_confirmed", me.equals(draft.studentId)); data.put("tutor_confirmed", me.equals(draft.tutorId));
        data.put("status", Contract.PENDING); data.put("reject_reason", ""); data.put("confirmed_at", null);
        data.put("created_at", FieldValue.serverTimestamp()); data.put("updated_at", FieldValue.serverTimestamp());
        ref.set(data).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) { failed(task.getException()); return; }
            created = id;
            done("Đã gửi hợp đồng. Chờ " + draft.otherName(me) + " xác nhận.");
        });
    }

    /** Học viên xác nhận: hợp đồng được chốt và chuyển sang chờ ký quỹ. */
    public void confirm(Contract contract) {
        String me = uid();
        if (busy || me == null || !contract.pending() || me.equals(contract.proposedBy)) return;
        Map<String, Object> update = new HashMap<>();
        update.put(me.equals(contract.studentId) ? "student_confirmed" : "tutor_confirmed", true);
        update.put("status", Contract.AWAITING_ESCROW);
        update.put("confirmed_at", FieldValue.serverTimestamp()); update.put("updated_at", FieldValue.serverTimestamp());
        write(contract, update, "Đã chốt hợp đồng " + contract.code + ". Bước tiếp theo là ký quỹ học phí.");
    }
    public void reject(Contract contract, String reason) {
        String me = uid();
        if (busy || me == null || !contract.pending() || me.equals(contract.proposedBy)) return;
        Map<String, Object> update = new HashMap<>();
        update.put("status", Contract.REJECTED); update.put("reject_reason", reason.trim());
        update.put("updated_at", FieldValue.serverTimestamp());
        write(contract, update, "Đã từ chối hợp đồng.");
    }
    public void cancel(Contract contract) {
        String me = uid();
        if (busy || me == null || !contract.pending() || !me.equals(contract.proposedBy)) return;
        Map<String, Object> update = new HashMap<>();
        update.put("status", Contract.CANCELLED); update.put("updated_at", FieldValue.serverTimestamp());
        write(contract, update, "Đã hủy hợp đồng đã gửi.");
    }
    private void write(Contract contract, Map<String, Object> update, String success) {
        busy = true; notifyUi();
        db.collection("contracts").document(contract.id).update(update).addOnCompleteListener(task -> {
            if (task.isSuccessful()) done(success); else failed(task.getException());
        });
    }
}
