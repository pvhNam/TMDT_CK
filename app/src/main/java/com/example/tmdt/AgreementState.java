package com.example.tmdt;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.HashMap;
import java.util.Map;

/**
 * Mẫu hợp đồng do quản trị viên soạn (contract_templates/{HOC_VIEN_GIA_SU | GIA_SU_HE_THONG})
 * và hợp đồng cộng tác giữa gia sư với hệ thống (tutor_agreements/{uid}).
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
    /** Hợp đồng cộng tác gia sư đã ký: bản sao nguyên văn của mẫu tại thời điểm ký. */
    public static final class Agreement {
        public final String name, title, content; public final int version; public final long signedAt;
        Agreement(String name, String title, String content, int version, long signedAt) {
            this.name = name; this.title = title; this.content = content; this.version = version; this.signedAt = signedAt;
        }
    }

    private final Map<String, Template> templates = new HashMap<>();
    private final Map<String, Boolean> templateLoaded = new HashMap<>();
    private ListenerRegistration studentTutor, tutorPlatform, mine;
    private String listUid;
    private Agreement agreement;
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
            agreement = doc == null || !doc.exists() ? null : new Agreement(Docs.text(doc, "ho_ten"), Docs.text(doc, "tieu_de"),
                    Docs.text(doc, "noi_dung"), (int) Docs.number(doc, "phien_ban"), Docs.millis(doc, "signed_at"));
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
    private static Template parse(String kind, DocumentSnapshot doc) {
        return new Template(kind, Docs.text(doc, "tieu_de"), Docs.text(doc, "noi_dung"), (int) Docs.number(doc, "phien_ban"), Docs.millis(doc, "updated_at"));
    }
    public void stop() {
        for (ListenerRegistration l : new ListenerRegistration[]{studentTutor, tutorPlatform, mine}) if (l != null) l.remove();
        studentTutor = tutorPlatform = mine = null; listUid = null;
        templates.clear(); templateLoaded.clear(); agreement = null; agreementLoaded = false; loadError = "";
    }
    @Override protected void onCleared() { stop(); }

    public String loadError() { return loadError; }
    public boolean templateLoaded(String kind) { return Boolean.TRUE.equals(templateLoaded.get(kind)); }
    /** null khi quản trị viên chưa soạn mẫu này. */
    public Template template(String kind) { return templates.get(kind); }
    public boolean agreementLoaded() { return agreementLoaded; }
    public Agreement agreement() { return agreement; }
    /** Gia sư đã ký hợp đồng cộng tác (bất kỳ phiên bản nào). */
    public boolean signed() { return agreement != null; }
    /** Đã ký nhưng quản trị viên đã cập nhật mẫu sang phiên bản mới hơn. */
    public boolean outdated() {
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

    /** Gia sư ký hợp đồng cộng tác theo mẫu hiện tại. */
    public void sign(String name) {
        final String me = uid();
        Template t = templates.get(TUTOR_PLATFORM);
        if (busy || me == null || t == null) return;
        Map<String, Object> data = new HashMap<>();
        data.put("user_id", me); data.put("ho_ten", name);
        data.put("tieu_de", t.title); data.put("noi_dung", t.content); data.put("phien_ban", t.version);
        data.put("trang_thai", "DA_KY"); data.put("signed_at", FieldValue.serverTimestamp());
        busy = true; notifyUi();
        db.collection("tutor_agreements").document(me).set(data).addOnCompleteListener(task -> {
            if (task.isSuccessful()) done("Đã ký hợp đồng cộng tác. Bạn có thể gửi hợp đồng dạy cho học viên.");
            else failed(task.getException());
        });
    }
}
