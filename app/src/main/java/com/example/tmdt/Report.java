package com.example.tmdt;

import com.google.firebase.firestore.DocumentSnapshot;

/** Báo cáo vi phạm (UC27 gửi, màn 47–48; UC29 xử lý, màn 50). */
public final class Report {
    public static final String RECEIVED = "DA_TIEP_NHAN", REVIEWING = "DANG_KIEM_TRA", NEED_INFO = "CAN_BO_SUNG",
            RESOLVED = "DA_XU_LY", DISMISSED = "BAC_BO";
    /** Hướng xử lý của quản trị viên. */
    public static final String OUTCOME_WARN = "CANH_CAO", OUTCOME_BAN = "KHOA_TAI_KHOAN", OUTCOME_REFUND = "HOAN_TIEN",
            OUTCOME_DISMISS = "BAC_BO", OUTCOME_NEED_INFO = "YEU_CAU_BO_SUNG";
    public static final String SUBJECT_CONTRACT = "CONTRACT", SUBJECT_CONVERSATION = "CONVERSATION", SUBJECT_LESSON = "LESSON";
    public static final int MAX_DESCRIPTION = 500, MAX_EVIDENCE = 5;
    public static final String[] REASONS = {
            "Buổi học không diễn ra", "Gia sư/học viên đến muộn hoặc vắng mặt", "Đề nghị giao dịch ngoài nền tảng",
            "Thái độ, ngôn từ không phù hợp", "Nội dung giảng dạy không đúng thỏa thuận", "Gian lận học phí, thanh toán", "Khác"};

    public String id = "", code = "", reporterId = "", reporterName = "", reporterRole = "", targetId = "", targetName = "", targetRole = "";
    public String subjectType = "", subjectId = "", subjectLabel = "", reason = "", description = "", status = RECEIVED;
    public String adminNote = "", outcome = "", conclusion = "", handlerId = "";
    public int evidenceCount;
    public long refundAmount, createdAt, reviewingAt, decidedAt, updatedAt;

    public boolean finished() { return RESOLVED.equals(status) || DISMISSED.equals(status); }
    public String statusLabel() {
        switch (status) {
            case RECEIVED: return "Đã tiếp nhận";
            case REVIEWING: return "Đang xem xét";
            case NEED_INFO: return "Cần bổ sung";
            case RESOLVED: return "Đã xử lý";
            case DISMISSED: return "Không vi phạm";
            default: return status;
        }
    }
    public static String outcomeLabel(String outcome) {
        switch (outcome) {
            case OUTCOME_WARN: return "Cảnh cáo người bị báo cáo";
            case OUTCOME_BAN: return "Khóa tài khoản người bị báo cáo";
            case OUTCOME_REFUND: return "Đề xuất hoàn tiền cho học viên";
            case OUTCOME_DISMISS: return "Bác bỏ báo cáo (không vi phạm)";
            case OUTCOME_NEED_INFO: return "Yêu cầu bổ sung minh chứng";
            default: return "";
        }
    }
    public static String roleLabel(String role) { return Review.TUTOR.equals(role) ? "Gia sư" : "Học viên"; }

    static Report from(DocumentSnapshot doc) {
        Report r = new Report();
        r.id = doc.getId(); r.code = Docs.text(doc, "code");
        r.reporterId = Docs.text(doc, "reporter_id"); r.reporterName = Docs.text(doc, "reporter_name"); r.reporterRole = Docs.text(doc, "reporter_role");
        r.targetId = Docs.text(doc, "target_id"); r.targetName = Docs.text(doc, "target_name"); r.targetRole = Docs.text(doc, "target_role");
        r.subjectType = Docs.text(doc, "subject_type"); r.subjectId = Docs.text(doc, "subject_id"); r.subjectLabel = Docs.text(doc, "subject_label");
        r.reason = Docs.text(doc, "reason"); r.description = Docs.text(doc, "description"); r.status = Docs.text(doc, "status");
        r.adminNote = Docs.text(doc, "admin_note"); r.outcome = Docs.text(doc, "outcome"); r.conclusion = Docs.text(doc, "conclusion");
        r.handlerId = Docs.text(doc, "handler_id"); r.evidenceCount = (int) Docs.number(doc, "evidence_count");
        r.refundAmount = Docs.number(doc, "refund_amount");
        r.createdAt = Docs.millis(doc, "created_at"); r.reviewingAt = Docs.millis(doc, "reviewing_at");
        r.decidedAt = Docs.millis(doc, "decided_at"); r.updatedAt = Docs.millis(doc, "updated_at");
        if (r.code.isEmpty()) r.code = Docs.code("BC", r.id);
        return r;
    }

    /** Một ảnh minh chứng (JPEG nén, lưu base64 trong reports/{id}/evidence/e{n}). */
    public static final class Evidence {
        public String id = "", name = "", data = "";
        public long size, createdAt;
        static Evidence from(DocumentSnapshot doc) {
            Evidence e = new Evidence();
            e.id = doc.getId(); e.name = Docs.text(doc, "name"); e.data = Docs.text(doc, "data");
            e.size = Docs.number(doc, "size"); e.createdAt = Docs.millis(doc, "created_at");
            return e;
        }
    }
}
