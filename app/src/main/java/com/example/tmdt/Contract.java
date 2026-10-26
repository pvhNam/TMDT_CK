package com.example.tmdt;

import com.google.firebase.firestore.DocumentSnapshot;

/**
 * Hợp đồng dạy (UC23, màn 40) giữa một học viên và một gia sư đã trao đổi qua tin nhắn.
 * Một bên đề nghị, bên kia xác nhận; sau khi hai bên xác nhận hợp đồng chuyển sang chờ ký quỹ (màn 41).
 */
public final class Contract {
    public static final String PENDING = "CHO_XAC_NHAN", AWAITING_ESCROW = "CHO_KY_QUY", ACTIVE = "HIEU_LUC",
            COMPLETED = "HOAN_THANH", REJECTED = "TU_CHOI", CANCELLED = "DA_HUY";
    public static final int MIN_FEE = 50000, MAX_FEE = 2000000, MAX_SESSIONS = 60;

    public String id = "", code = "", conversationId = "", studentId = "", tutorId = "", studentName = "", tutorName = "";
    public String subject = "", schedule = "", startDate = "", mode = "Trực tuyến", terms = "", proposedBy = "", status = PENDING, rejectReason = "";
    public int sessions = 4, minutes = 60;
    public long fee, startAt, createdAt, confirmedAt;
    public boolean studentConfirmed, tutorConfirmed;

    public long total() { return fee * sessions; }
    public boolean pending() { return PENDING.equals(status); }
    /** Đã được hai bên chốt: được đánh giá và báo cáo theo hợp đồng này. */
    public boolean agreed() { return AWAITING_ESCROW.equals(status) || ACTIVE.equals(status) || COMPLETED.equals(status); }
    public boolean closed() { return REJECTED.equals(status) || CANCELLED.equals(status); }
    public boolean member(String uid) { return uid != null && (uid.equals(studentId) || uid.equals(tutorId)); }
    public String other(String uid) { return uid != null && uid.equals(studentId) ? tutorId : studentId; }
    public String otherName(String uid) { return uid != null && uid.equals(studentId) ? tutorName : studentName; }
    public String sessionsLabel() { return sessions + " buổi × " + minutes + " phút"; }

    public String statusLabel() {
        switch (status) {
            case PENDING: return "Chờ xác nhận";
            case AWAITING_ESCROW: return "Chờ thanh toán học phí";
            case ACTIVE: return "Đang hiệu lực";
            case COMPLETED: return "Đã hoàn thành";
            case REJECTED: return "Đã từ chối";
            case CANCELLED: return "Đã hủy";
            default: return status;
        }
    }

    static Contract from(DocumentSnapshot doc) {
        Contract c = new Contract();
        c.id = doc.getId(); c.code = Docs.text(doc, "code"); c.conversationId = Docs.text(doc, "conversation_id");
        c.studentId = Docs.text(doc, "student_id"); c.tutorId = Docs.text(doc, "tutor_id");
        c.studentName = Docs.text(doc, "student_name"); c.tutorName = Docs.text(doc, "tutor_name");
        c.subject = Docs.text(doc, "subject"); c.schedule = Docs.text(doc, "schedule"); c.startDate = Docs.text(doc, "start_date");
        c.mode = Docs.text(doc, "mode"); c.terms = Docs.text(doc, "terms"); c.proposedBy = Docs.text(doc, "proposed_by");
        c.status = Docs.text(doc, "status"); c.rejectReason = Docs.text(doc, "reject_reason");
        c.sessions = (int) Docs.number(doc, "sessions"); c.minutes = (int) Docs.number(doc, "minutes"); c.fee = Docs.number(doc, "fee");
        c.startAt = Docs.millis(doc, "start_at"); c.createdAt = Docs.millis(doc, "created_at"); c.confirmedAt = Docs.millis(doc, "confirmed_at");
        c.studentConfirmed = Docs.flag(doc, "student_confirmed"); c.tutorConfirmed = Docs.flag(doc, "tutor_confirmed");
        if (c.code.isEmpty()) c.code = Docs.code("HĐ", c.id);
        return c;
    }
}
