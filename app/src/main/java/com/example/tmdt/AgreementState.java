package com.example.tmdt;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Mẫu hợp đồng do quản trị viên soạn (contract_templates/{HOC_VIEN_GIA_SU | GIA_SU_HE_THONG})
 * và hợp đồng cộng tác giữa gia sư với hệ thống (tutor_agreements/{uid}).
 * Hợp đồng cộng tác ký 1 hoặc 2 năm và giữ nguyên trong thời hạn (xem {@link AgreementTerm});
 * mỗi lần ký mới hoặc gia hạn, bản trước được lưu ở tutor_agreements/{uid}/lich_su.
 */
public final class AgreementState extends MemberState {
    public static final String STUDENT_TUTOR = "HOC_VIEN_GIA_SU", TUTOR_PLATFORM = "GIA_SU_HE_THONG";
    public static final int MAX_CONTENT = 8000;

    /** Một mẫu hợp đồng; {@code version} tăng 1 mỗi lần quản trị viên lưu. */
    public static final class Template {
        public final String kind, title, content; public final int version; public final long updatedAt;
        Template(String kind, String title, String content, int version, long updatedAt) {
            this.kind = kind; this.title = title; this.content = content; this.version = version; this.updatedAt = updatedAt;
        }
    }
    /** Hợp đồng cộng tác gia sư đã ký: bản sao nguyên văn của mẫu tại thời điểm ký, kèm thời hạn và điều khoản chung. */
    public static final class Agreement {
        public final String name, title, content; public final int version; public final long signedAt;
        public String status = AgreementTerm.SIGNED, clauses = "", terminateReason = "";
        public int years, renewals;
        /** 0 với hợp đồng ký trước khi có thời hạn. */
        public long start, end, terminatedAt;
        Agreement(String name, String title, String content, int version, long signedAt) {
            this.name = name; this.title = title; this.content = content; this.version = version; this.signedAt = signedAt;
        }
        public boolean terminated() { return AgreementTerm.TERMINATED.equals(status); }
    }

    private final Map<String, Template> templates = new HashMap<>();
    private final Map<String, Boolean> templateLoaded = new HashMap<>();
    private ListenerRegistration studentTutor, tutorPlatform, mine;
    private String listUid;
    private Agreement agreement;
    /** Dữ liệu gốc của hợp đồng hiện tại, chép nguyên vào lịch sử khi ký mới hoặc gia hạn. */
    private Map<String, Object> agreementRaw;
    private boolean agreementLoaded;
    private String loadError = "";

    public void start() {
        final String me = uid();
        if (me == null) { stop(); return; }
        if (me.equals(listUid)) return;
        stop(); listUid = me;
        studentTutor = watchTemplate(STUDENT_TUTOR, me);
        tutorPlatform = watchTemplate(TUTOR_PLATFORM, me);
        mine = db.collection("tutor_agreements").document(me).addSnapshotListener((doc, error) -> {
            if (!me.equals(listUid)) return;
            agreementLoaded = true;
            if (error != null) { loadError = Docs.message(error); notifyUi(); return; }
            agreement = doc == null || !doc.exists() ? null : parseAgreement(doc);
            agreementRaw = agreement == null ? null : doc.getData();
            notifyUi();
        });
    }
    private ListenerRegistration watchTemplate(String kind, String me) {
        return db.collection("contract_templates").document(kind).addSnapshotListener((doc, error) -> {
            if (!me.equals(listUid)) return;
            templateLoaded.put(kind, true);
            if (error != null) { loadError = Docs.message(error); notifyUi(); return; }
            if (doc == null || !doc.exists()) templates.remove(kind); else templates.put(kind, parse(kind, doc));
            notifyUi();
        });
    }
    private static Agreement parseAgreement(DocumentSnapshot doc) {
        Agreement a = new Agreement(Docs.text(doc, "ho_ten"), Docs.text(doc, "tieu_de"), Docs.text(doc, "noi_dung"),
                (int) Docs.number(doc, "phien_ban"), millis(doc, "signed_at"));
        String status = Docs.text(doc, "trang_thai"); if (!status.isEmpty()) a.status = status;
        a.years = (int) Docs.number(doc, "thoi_han_nam"); a.renewals = (int) Docs.number(doc, "lan_gia_han");
        a.start = millis(doc, "hieu_luc_tu"); a.end = millis(doc, "het_han"); a.clauses = Docs.text(doc, "dieu_khoan");
        a.terminatedAt = millis(doc, "cham_dut_luc"); a.terminateReason = Docs.text(doc, "ly_do_cham_dut");
        return a;
    }
    private static long millis(DocumentSnapshot doc, String field) {
        try { return Docs.millis(doc, field); } catch (RuntimeException wrongType) { return 0; }
    }
    private static Template parse(String kind, DocumentSnapshot doc) {
        return new Template(kind, Docs.text(doc, "tieu_de"), Docs.text(doc, "noi_dung"), (int) Docs.number(doc, "phien_ban"), Docs.millis(doc, "updated_at"));
    }
    public void stop() {
        for (ListenerRegistration l : new ListenerRegistration[]{studentTutor, tutorPlatform, mine}) if (l != null) l.remove();
        studentTutor = tutorPlatform = mine = null; listUid = null;
        templates.clear(); templateLoaded.clear(); agreement = null; agreementRaw = null; agreementLoaded = false; loadError = "";
    }
    @Override protected void onCleared() { stop(); }

    public String loadError() { return loadError; }
    public boolean templateLoaded(String kind) { return Boolean.TRUE.equals(templateLoaded.get(kind)); }
    /** null khi quản trị viên chưa soạn mẫu này. */
    public Template template(String kind) { return templates.get(kind); }
    public boolean agreementLoaded() { return agreementLoaded; }
    public Agreement agreement() { return agreement; }
    /** Gia sư đã từng ký hợp đồng cộng tác (kể cả đã hết hạn hoặc bị chấm dứt). */
    public boolean signed() { return agreement != null; }
    public AgreementTerm.Phase phase() { return phase(System.currentTimeMillis()); }
    public AgreementTerm.Phase phase(long now) {
        Agreement a = agreement;
        return a == null ? AgreementTerm.Phase.NONE : AgreementTerm.phase(now, true, a.status, a.start, a.end, a.terminatedAt);
    }
    /** Hợp đồng cộng tác còn hiệu lực: được gửi hợp đồng dạy cho học viên. */
    public boolean active() { return AgreementTerm.active(phase()); }
    /** Mẫu đã có phiên bản mới hơn bản đã ký. Chỉ để thông báo: bản đã ký giữ nguyên tới hết thời hạn. */
    public boolean newerTemplate() {
        Template t = templates.get(TUTOR_PLATFORM);
        return agreement != null && t != null && agreement.version < t.version;
    }

    /** Quản trị viên lưu mẫu: mỗi lần lưu là một phiên bản mới. */
    public void saveTemplate(String kind, String title, String content) {
        final String me = uid();
        if (busy || me == null) return;
        Template current = templates.get(kind);
        Map<String, Object> data = new HashMap<>();
        data.put("tieu_de", title.trim()); data.put("noi_dung", content.trim());
        data.put("phien_ban", current == null ? 1 : current.version + 1);
        data.put("updated_by", me); data.put("updated_at", FieldValue.serverTimestamp());
        busy = true; notifyUi();
        db.collection("contract_templates").document(kind).set(data).addOnCompleteListener(task -> {
            if (task.isSuccessful()) done("Đã lưu mẫu hợp đồng (phiên bản " + data.get("phien_ban") + ").");
            else failed(task.getException());
        });
    }

    /**
     * Gia sư ký hợp đồng cộng tác {@code years} năm theo mẫu hiện tại: ký lần đầu, ký lại sau khi hết hạn/bị chấm dứt,
     * hoặc gia hạn trong 30 ngày cuối (kỳ mới nối tiếp ngày hết hạn). Bản đang có được chép vào lịch sử trong cùng lần ghi.
     */
    public void sign(String name, int years) {
        final String me = uid();
        Template t = templates.get(TUTOR_PLATFORM);
        long now = System.currentTimeMillis();
        AgreementTerm.Phase phase = phase(now);
        if (busy || me == null || t == null || (years != 1 && years != 2) || !AgreementTerm.canSign(phase)) return;
        boolean renewal = AgreementTerm.renewal(phase);
        long start = AgreementTerm.newStart(phase, now, agreement == null ? 0 : agreement.end);
        long end = AgreementTerm.end(start, years);
        Map<String, Object> data = new HashMap<>();
        data.put("user_id", me); data.put("ho_ten", name);
        data.put("tieu_de", t.title); data.put("noi_dung", t.content); data.put("phien_ban", t.version);
        data.put("trang_thai", AgreementTerm.SIGNED); data.put("signed_at", FieldValue.serverTimestamp());
        data.put("thoi_han_nam", years);
        data.put("hieu_luc_tu", new Timestamp(new Date(start))); data.put("het_han", new Timestamp(new Date(end)));
        data.put("dieu_khoan", AgreementTerm.clauses(years, start, end, ZoneId.systemDefault()));
        data.put("lan_gia_han", renewal ? agreement.renewals + 1 : 0);
        DocumentReference ref = db.collection("tutor_agreements").document(me);
        WriteBatch batch = db.batch();
        if (agreementRaw != null && agreement != null)
            batch.set(ref.collection("lich_su").document(String.valueOf(agreement.signedAt)), new HashMap<>(agreementRaw));
        batch.set(ref, data);
        busy = true; notifyUi();
        batch.commit().addOnCompleteListener(task -> {
            if (task.isSuccessful()) done(renewal ? "Đã gia hạn hợp đồng cộng tác thêm " + years + " năm, từ "
                    + AgreementTerm.date(start, ZoneId.systemDefault()) + "."
                    : "Đã ký hợp đồng cộng tác " + years + " năm. Bạn có thể gửi hợp đồng dạy cho học viên.");
            else failed(task.getException());
        });
    }
}
