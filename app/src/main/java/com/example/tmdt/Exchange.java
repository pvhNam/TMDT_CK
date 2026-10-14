package com.example.tmdt;

import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.tmdt.ui.admin.AdminReportsFragment;
import com.example.tmdt.ui.admin.AdminStatsFragment;
import com.example.tmdt.ui.admin.AdminUsersFragment;
import com.example.tmdt.ui.admin.HandleReportFragment;
import com.example.tmdt.ui.exchange.ContractFragment;
import com.example.tmdt.ui.exchange.ContractsFragment;
import com.example.tmdt.ui.exchange.MyReportsFragment;
import com.example.tmdt.ui.exchange.ReportFragment;
import com.example.tmdt.ui.exchange.ReviewFormFragment;
import com.example.tmdt.ui.exchange.ReviewsFragment;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Thành viên 4 – Trao đổi, thanh toán và quản trị: các màn hình, lựa chọn đang mở và điều hướng quay lại
 * của UC23, UC26, UC27, UC29, UC30, UC31. MainActivity chỉ chuyển tiếp sang lớp này (giống {@link Classroom}).
 */
public final class Exchange {
    public static final String CONTRACT = "contract", CONTRACTS = "contracts", REVIEW = "review", REVIEWS = "reviews",
            REPORT = "report", REPORTS = "reports", ADMIN_REPORTS = "admin_reports", HANDLE_REPORT = "handle_report",
            ADMIN_USERS = "admin_users", ADMIN_STATS = "admin_stats";
    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] WEEKDAYS = {"Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy", "Chủ nhật"};

    private final MainActivity host;
    public final ContractState contracts;
    public final ReviewState reviews;
    public final ReportState reports;
    public final AdminState admin;

    // Lựa chọn đang mở (giữ khi xoay máy).
    private String contractId, reviewContractId, adminReportId, reviewsTarget, reviewsName, reviewsRole;
    private String reportType, reportSubject;
    /** Hợp đồng đang soạn (chưa gửi). */
    private Contract draft;
    /** Màn đã mở từng màn của phần này, để nút quay lại trở về đúng chỗ. */
    private final Map<String, String> origins = new HashMap<>();

    Exchange(MainActivity host, Bundle state) {
        this.host = host;
        ViewModelProvider provider = new ViewModelProvider(host);
        contracts = provider.get(ContractState.class); reviews = provider.get(ReviewState.class);
        reports = provider.get(ReportState.class); admin = provider.get(AdminState.class);
        if (state != null) {
            contractId = state.getString("ex.contract"); reviewContractId = state.getString("ex.review");
            adminReportId = state.getString("ex.adminReport"); reviewsTarget = state.getString("ex.reviewsTarget");
            reviewsName = state.getString("ex.reviewsName"); reviewsRole = state.getString("ex.reviewsRole");
            reportType = state.getString("ex.reportType"); reportSubject = state.getString("ex.reportSubject");
            Bundle saved = state.getBundle("ex.origins");
            if (saved != null) for (String key : saved.keySet()) origins.put(key, saved.getString(key));
        }
    }
    void save(Bundle out) {
        out.putString("ex.contract", contractId); out.putString("ex.review", reviewContractId);
        out.putString("ex.adminReport", adminReportId); out.putString("ex.reviewsTarget", reviewsTarget);
        out.putString("ex.reviewsName", reviewsName); out.putString("ex.reviewsRole", reviewsRole);
        out.putString("ex.reportType", reportType); out.putString("ex.reportSubject", reportSubject);
        Bundle saved = new Bundle(); for (Map.Entry<String, String> e : origins.entrySet()) saved.putString(e.getKey(), e.getValue());
        out.putBundle("ex.origins", saved);
    }

    // Điều hướng dùng bởi MainActivity ---------------------------------------------------------------

    /** Đăng nhập/đăng xuất: bật hoặc tắt các listener của tài khoản. */
    void accountChanged(boolean signedIn) {
        if (signedIn) { contracts.start(); reports.start(); }
        else { contracts.stop(); reports.stop(); reviews.stopWatch(); admin.stopReports(); admin.stopUsers(); }
    }
    static boolean owns(String destination) {
        switch (destination) {
            case CONTRACT: case CONTRACTS: case REVIEW: case REVIEWS: case REPORT: case REPORTS:
            case ADMIN_REPORTS: case HANDLE_REPORT: case ADMIN_USERS: case ADMIN_STATS: return true;
            default: return false;
        }
    }
    private static boolean adminScreen(String destination) {
        return ADMIN_REPORTS.equals(destination) || HANDLE_REPORT.equals(destination) || ADMIN_USERS.equals(destination) || ADMIN_STATS.equals(destination);
    }
    /** Khách cần đăng nhập; màn quản trị chỉ dành cho ADMIN; thiếu lựa chọn thì về màn danh sách. */
    String redirect(String destination) {
        if (!owns(destination)) return destination;
        if (REVIEWS.equals(destination)) return reviewsTarget == null ? "home" : destination;  // Khách xem được đánh giá gia sư.
        if (!host.account.signedIn()) return "login";
        if (adminScreen(destination) && !isAdmin()) return "account";
        if (CONTRACT.equals(destination) && contractId == null && draft == null) return CONTRACTS;
        if (REVIEW.equals(destination) && reviewContractId == null) return CONTRACTS;
        if (HANDLE_REPORT.equals(destination) && adminReportId == null) return ADMIN_REPORTS;
        return destination;
    }
    Fragment fragment(String destination) {
        switch (destination) {
            case CONTRACT: return new ContractFragment();
            case CONTRACTS: return new ContractsFragment();
            case REVIEW: return new ReviewFormFragment();
            case REVIEWS: return new ReviewsFragment();
            case REPORT: return new ReportFragment();
            case REPORTS: return new MyReportsFragment();
            case ADMIN_REPORTS: return new AdminReportsFragment();
            case HANDLE_REPORT: return new HandleReportFragment();
            case ADMIN_USERS: return new AdminUsersFragment();
            case ADMIN_STATS: return new AdminStatsFragment();
            default: return null;
        }
    }
    String backTarget(String screen) {
        if (!owns(screen)) return null;
        if (CONTRACT.equals(screen)) draft = null;
        String origin = origins.get(screen);
        if (origin != null && !origin.equals(screen)) return origin;
        if (HANDLE_REPORT.equals(screen)) return ADMIN_REPORTS;
        if (ADMIN_REPORTS.equals(screen) || ADMIN_USERS.equals(screen)) return ADMIN_STATS.equals(origins.get(screen)) ? ADMIN_STATS : "account";
        return REVIEWS.equals(screen) ? "home" : "account";
    }
    private void open(String destination) {
        String current = host.screen;
        if (!destination.equals(current) && !(owns(current) && destination.equals(origins.get(current)))) origins.put(destination, current);
        host.hideKeyboard(); host.show(destination);
    }

    /** Thay màn hiện tại bằng màn khác (ví dụ sau khi gửi báo cáo): quay lại sẽ về nơi đã mở màn hiện tại. */
    public void replaceWith(String destination) {
        String origin = origins.get(host.screen);
        if (origin != null) origins.put(destination, origin); else origins.remove(destination);
        host.hideKeyboard(); host.show(destination);
    }

    // Thông tin người đang đăng nhập -----------------------------------------------------------------

    public boolean isAdmin() { UserProfile p = host.account.profile(); return p != null && "ADMIN".equals(p.role); }
    public boolean isTutor() { UserProfile p = host.account.profile(); return p != null && (p.tutor || "TUTOR".equals(p.role)); }
    public String myName() { UserProfile p = host.account.profile(); return p != null && !p.name.isEmpty() ? p.name : "Người dùng"; }
    public String myRole() { return isTutor() ? Review.TUTOR : Review.STUDENT; }
    public void back() { host.back(); }
    public void show(String destination) { host.show(destination); }
    public void dialog(String title, String message) {
        new AlertDialog.Builder(host).setTitle(title).setMessage(message).setPositiveButton(R.string.understood, null).show();
    }

    // UC23 – Hợp đồng --------------------------------------------------------------------------------

    public String contractId() { return contractId; }
    public Contract draft() { return draft; }
    public void openContracts() { open(CONTRACTS); }
    public void openContract(Contract contract) { contractId = contract.id; draft = null; open(CONTRACT); }
    /** Sau khi gửi đề nghị: màn 40 chuyển sang xem hợp đồng vừa tạo. */
    public void showCreated(String id) { contractId = id; draft = null; }

    /** Nút "Hợp đồng" trong màn trò chuyện: mở hợp đồng đang có, hoặc soạn đề nghị mới từ buổi học đang trao đổi. */
    public void openContractFor(Conversation conversation) {
        Contract existing = contracts.latestFor(conversation.id);
        if (existing != null && !existing.closed()) { openContract(existing); return; }
        Contract value = newDraft(conversation, existing);
        if (value == null) {
            dialog("Chưa thể lập hợp đồng", "Hợp đồng dạy được lập giữa một học viên và một gia sư. Cuộc trò chuyện này chưa xác định được bên gia sư.");
            return;
        }
        draft = value; contractId = null; open(CONTRACT);
    }
    /** "Tạo đề nghị mới" từ hợp đồng đã bị từ chối/hủy: giữ nguyên các điều khoản cũ để sửa. */
    public void redraft(Contract old) {
        Contract value = copy(old);
        value.id = ""; value.code = ""; value.status = Contract.PENDING; value.rejectReason = "";
        draft = value; contractId = null; host.show(CONTRACT);
    }
    private Contract newDraft(Conversation c, Contract previous) {
        String me = host.account.signedIn() ? host.chat.myId() : "";
        Contract d = previous != null ? copy(previous) : new Contract();
        d.id = ""; d.code = ""; d.status = Contract.PENDING; d.rejectReason = ""; d.conversationId = c.id;
        if (Review.TUTOR.equals(c.peerRole)) { d.studentId = me; d.tutorId = c.peerId; d.studentName = myName(); d.tutorName = c.peerName; }
        else if (Review.TUTOR.equals(c.myRole) || isTutor()) { d.tutorId = me; d.studentId = c.peerId; d.tutorName = myName(); d.studentName = c.peerName; }
        else return null;
        if (previous != null) return d;
        LocalDate start = LocalDate.now().plusDays(1);
        LocalTime time = LocalTime.of(19, 0);
        if (c.hasLesson()) {
            d.subject = c.lessonTitle;
            // "15/09/2026 · 19:00–20:00"
            try {
                String[] parts = c.lessonTime.split("·");
                start = LocalDate.parse(parts[0].trim(), DATE);
                String[] range = parts[1].trim().split("[–-]");
                time = LocalTime.parse(range[0].trim());
                LocalTime end = LocalTime.parse(range[1].trim());
                int minutes = (int) java.time.Duration.between(time, end).toMinutes();
                if (minutes == 60 || minutes == 90 || minutes == 120) d.minutes = minutes;
            } catch (RuntimeException ignored) { /* Không đọc được: dùng giá trị mặc định. */ }
        }
        CatalogState.Teacher teacher = null;
        for (CatalogState.Teacher t : host.catalog.teachers.values()) if (t.userId.equals(d.tutorId)) teacher = t;
        if (teacher != null && !teacher.offerings.isEmpty()) {
            CatalogState.Offering offering = teacher.offerings.get(0);
            d.fee = offering.price;
            if (d.subject.isEmpty()) d.subject = host.catalog.subjects.get(offering.subjectId) + " " + CatalogState.level(offering.level);
            if (!c.hasLesson()) d.minutes = 90;
        }
        if (d.fee == 0) d.fee = 150000;
        if (d.subject.isEmpty()) d.subject = "Môn học";
        schedule(d, start, time);
        return d;
    }
    /** Cập nhật ngày bắt đầu và giờ học của bản nháp: lịch hằng tuần lấy theo thứ của ngày bắt đầu. */
    public static void schedule(Contract d, LocalDate start, LocalTime time) {
        d.startDate = start.format(DATE);
        d.startAt = start.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        d.schedule = weekday(start.getDayOfWeek()) + " · " + time + "–" + time.plusMinutes(d.minutes);
    }
    public static String weekday(DayOfWeek day) { return WEEKDAYS[day.getValue() - 1]; }
    public static LocalDate startDay(Contract d) {
        return java.time.Instant.ofEpochMilli(d.startAt).atZone(ZoneId.systemDefault()).toLocalDate();
    }
    public static LocalTime startTime(Contract d) {
        return java.time.Instant.ofEpochMilli(d.startAt).atZone(ZoneId.systemDefault()).toLocalTime().withSecond(0).withNano(0);
    }
    private static Contract copy(Contract o) {
        Contract c = new Contract();
        c.conversationId = o.conversationId; c.studentId = o.studentId; c.tutorId = o.tutorId; c.studentName = o.studentName; c.tutorName = o.tutorName;
        c.subject = o.subject; c.schedule = o.schedule; c.startDate = o.startDate; c.mode = o.mode; c.terms = o.terms;
        c.sessions = o.sessions; c.minutes = o.minutes; c.fee = o.fee; c.startAt = o.startAt;
        LocalDate start = startDay(o);
        if (start.isBefore(LocalDate.now().plusDays(1))) schedule(c, LocalDate.now().plusDays(1), startTime(o));
        return c;
    }

    // UC26 – Đánh giá --------------------------------------------------------------------------------

    public String reviewContractId() { return reviewContractId; }
    public void openReview(Contract contract) { reviewContractId = contract.id; open(REVIEW); }
    public String reviewsTarget() { return reviewsTarget; }
    public String reviewsName() { return reviewsName; }
    public String reviewsRole() { return reviewsRole; }
    /** Màn 10: đánh giá về một gia sư (công khai) hoặc về chính mình. */
    public void openReviews(String targetId, String name, String role) {
        reviewsTarget = targetId; reviewsName = name; reviewsRole = role; open(REVIEWS);
    }
    public void openMyReviews() { openReviews(host.chat.myId(), myName(), myRole()); }

    // UC27 – Báo cáo vi phạm -------------------------------------------------------------------------

    public String reportType() { return reportType; }
    public String reportSubject() { return reportSubject; }
    /** Màn 47, có thể chọn sẵn đối tượng (hợp đồng hoặc cuộc trò chuyện). */
    public void openReport(String type, String subjectId) {
        reportType = type; reportSubject = subjectId;
        if (type != null && subjectId != null) reports.draftSubject = type + ":" + subjectId;
        open(REPORT);
    }
    public void openReports() { open(REPORTS); }

    // UC29–31 – Quản trị -----------------------------------------------------------------------------

    public String adminReportId() { return adminReportId; }
    public void openAdminReports() { open(ADMIN_REPORTS); }
    public void openHandleReport(Report report) { adminReportId = report.id; open(HANDLE_REPORT); }
    public void openAdminUsers() { open(ADMIN_USERS); }
    public void openAdminStats() { open(ADMIN_STATS); }
}
