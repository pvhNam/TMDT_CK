package com.example.tmdt;

import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.tmdt.ui.admin.AdminClassesFragment;
import com.example.tmdt.ui.admin.AdminStatsFragment;
import com.example.tmdt.ui.exchange.AgreementFragment;
import com.example.tmdt.ui.exchange.ContractFragment;
import com.example.tmdt.ui.exchange.ContractTemplatesFragment;
import com.example.tmdt.ui.exchange.ContractsFragment;
import com.example.tmdt.ui.exchange.HandleReportFragment;
import com.example.tmdt.ui.exchange.MyReportsFragment;
import com.example.tmdt.ui.exchange.ReportFragment;
import com.example.tmdt.ui.exchange.ReportsAdminFragment;
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
 * Luồng: học viên đặt lịch → gia sư chấp nhận → gia sư gửi hợp đồng (theo mẫu của quản trị viên) → học viên xác nhận.
 */
public final class Exchange {
    public static final String CONTRACT = "contract", CONTRACTS = "contracts", AGREEMENT = "agreement", TEMPLATES = "contract_templates";
    public static final String REVIEW = "ex_review", REVIEWS = "ex_reviews", REPORT = "ex_report", REPORTS = "ex_reports",
            REPORTS_ADMIN = "report_admin", HANDLE_REPORT = "report_handle";
    /** Thống kê cho quản trị viên */
    public static final String ADMIN_STATS = "admin_stats";
    public static final String ADMIN_CLASSES = "admin_classes";
    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] WEEKDAYS = {"Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy", "Chủ nhật"};

    private final MainActivity host;
    public final ContractState contracts;
    public final AgreementState agreements;
    public final ReviewState reviews;
    public final ReportState reports;
    public final ReportAdminState reportAdmin;

    // Lựa chọn đang mở (giữ khi xoay máy).
    private String contractId;
    private String reviewContractId, reviewsTarget, reviewsName, reviewsRole, reportType, reportSubject, adminReportId;
    private final Map<String, String> origins = new HashMap<>();

    Exchange(MainActivity host, Bundle state) {
        this.host = host;
        ViewModelProvider provider = new ViewModelProvider(host);
        contracts = provider.get(ContractState.class);
        agreements = provider.get(AgreementState.class);
        reviews = provider.get(ReviewState.class);
        reports = provider.get(ReportState.class);
        reportAdmin = provider.get(ReportAdminState.class);
        if (state != null) {
            contractId = state.getString("ex.contract");
            reviewContractId = state.getString("ex.review"); adminReportId = state.getString("ex.adminReport");
            reviewsTarget = state.getString("ex.reviewsTarget"); reviewsName = state.getString("ex.reviewsName");
            reviewsRole = state.getString("ex.reviewsRole");
            reportType = state.getString("ex.reportType"); reportSubject = state.getString("ex.reportSubject");
            Bundle saved = state.getBundle("ex.origins");
            if (saved != null) for (String key : saved.keySet()) origins.put(key, saved.getString(key));
        }
    }
    void save(Bundle out) {
        out.putString("ex.contract", contractId);
        out.putString("ex.review", reviewContractId); out.putString("ex.adminReport", adminReportId);
        out.putString("ex.reviewsTarget", reviewsTarget); out.putString("ex.reviewsName", reviewsName);
        out.putString("ex.reviewsRole", reviewsRole);
        out.putString("ex.reportType", reportType); out.putString("ex.reportSubject", reportSubject);
        Bundle saved = new Bundle(); for (Map.Entry<String, String> e : origins.entrySet()) saved.putString(e.getKey(), e.getValue());
        out.putBundle("ex.origins", saved);
    }

    // Điều hướng dùng bởi MainActivity ---------------------------------------------------------------

    /** Đăng nhập/đăng xuất: bật hoặc tắt các listener của tài khoản. */
    void accountChanged(boolean signedIn) {
        if (signedIn) { contracts.start(); agreements.start(); }
        else {
            contracts.stop(); agreements.stop(); contracts.draft = null; contractId = null;
            reviews.stopWatch(); new androidx.lifecycle.ViewModelProvider(host).get(ManageState.class).stop(); reports.stop(); reports.clearDraft(); reportAdmin.stop(); adminReportId = null;
        }
    }
    static boolean owns(String destination) {
        return CONTRACT.equals(destination) || CONTRACTS.equals(destination) || AGREEMENT.equals(destination) || TEMPLATES.equals(destination)
                || REVIEW.equals(destination) || REVIEWS.equals(destination) || ADMIN_STATS.equals(destination) || ADMIN_CLASSES.equals(destination) || REPORT.equals(destination) || REPORTS.equals(destination)
                || REPORTS_ADMIN.equals(destination) || HANDLE_REPORT.equals(destination);
    }
    /** Khách cần đăng nhập; mẫu hợp đồng chỉ dành cho quản trị viên; thiếu hợp đồng thì về danh sách. */
    String redirect(String destination) {
        if (!owns(destination)) return destination;
        if (REVIEWS.equals(destination) && reviewsTarget != null && Review.TUTOR.equals(reviewsRole)) return destination; // Khách xem được đánh giá gia sư.
        if (!host.account.signedIn()) return "login";
        if ((ADMIN_STATS.equals(destination) || ADMIN_CLASSES.equals(destination)) && !isAdmin()) return "account";
        if (REVIEWS.equals(destination) && reviewsTarget == null) return "account";
        if ((REPORTS_ADMIN.equals(destination) || HANDLE_REPORT.equals(destination)) && !isAdmin()) return "account";
        if (REVIEW.equals(destination) && reviewContractId == null) return CONTRACTS;
        if (HANDLE_REPORT.equals(destination) && adminReportId == null) return REPORTS_ADMIN;
        if (TEMPLATES.equals(destination) && !isAdmin()) return "account";
        if (CONTRACT.equals(destination) && contractId == null && contracts.draft == null) return CONTRACTS;
        return destination;
    }
    Fragment fragment(String destination) {
        switch (destination) {
            case CONTRACT: return new ContractFragment();
            case CONTRACTS: return isTutor()?new com.example.tmdt.ui.exchange.ContractsHubFragment():new ContractsFragment();
            case AGREEMENT: return new AgreementFragment();
            case TEMPLATES: return new ContractTemplatesFragment();
            case REVIEW: return new ReviewFormFragment();
            case REVIEWS: return new ReviewsFragment();
            case ADMIN_STATS: return new AdminStatsFragment();
            case ADMIN_CLASSES: return new AdminClassesFragment();
            case REPORT: return new ReportFragment();
            case REPORTS: return new MyReportsFragment();
            case REPORTS_ADMIN: return new ReportsAdminFragment();
            case HANDLE_REPORT: return new HandleReportFragment();
            default: return null;
        }
    }
    String backTarget(String screen) {
        if (!owns(screen)) return null;
        if (CONTRACT.equals(screen)) contracts.draft = null;
        String origin = origins.get(screen);
        if (origin != null && !origin.equals(screen)) return origin;
        if (HANDLE_REPORT.equals(screen)) return REPORTS_ADMIN;
        if (REPORTS_ADMIN.equals(screen) && isAdmin()) return "admin_dashboard";
        return "account";
    }
    private void open(String destination) {
        String current = host.screen;
        if (!destination.equals(current) && !(owns(current) && destination.equals(origins.get(current)))) origins.put(destination, current);
        host.hideKeyboard(); host.show(destination);
    }

    public boolean isAdmin() { UserProfile p = host.account.profile(); return p != null && "ADMIN".equals(p.role); }
    /** Gia sư đã được duyệt (VERIFIED), giống điều kiện của chế độ gia sư. */
    public boolean isTutor() { return host.classroom.isTutor(); }
    public String myId() { return contracts.myId(); }
    public String myName() { UserProfile p = host.account.profile(); return p != null && !p.name.isEmpty() ? p.name : "Người dùng"; }
    public void back() { host.back(); }
    public void show(String destination) { host.show(destination); }
    public void dialog(String title, String message) {
        new AlertDialog.Builder(host).setTitle(title).setMessage(message).setPositiveButton(R.string.understood, null).show();
    }

    // Lối vào ----------------------------------------------------------------------------------------

    public String contractId() { return contractId; }
    public Contract draft() { return contracts.draft; }
    public void openContracts() { open(CONTRACTS); }
    public void openAgreement() { open(AGREEMENT); }
    public void openTemplates() { open(TEMPLATES); }
    /** Thống kê biểu đồ cho quản trị viên. */
    public void openStats() { open(ADMIN_STATS); }
    public void openClassesAdmin() { open(ADMIN_CLASSES); }

    // UC26 – Đánh giá hai chiều ----------------------------------------------------------------------

    public String myRole() { return isTutor() ? Review.TUTOR : Review.STUDENT; }
    public String reviewContractId() { return reviewContractId; }
    /** Màn 09 (học viên đánh giá gia sư) hoặc 46 (gia sư đánh giá học viên) cho một hợp đồng đã chốt. */
    public void openReview(Contract contract) { reviewContractId = contract.id; open(REVIEW); }
    public String reviewsTarget() { return reviewsTarget; }
    public String reviewsName() { return reviewsName; }
    public String reviewsRole() { return reviewsRole; }
    /** Màn 10: đánh giá về một người (role = TUTOR: đánh giá gia sư, công khai; STUDENT: đánh giá học viên). */
    public void openReviews(String targetId, String name, String role) {
        reviewsTarget = targetId; reviewsName = name; reviewsRole = role; open(REVIEWS);
    }
    /** "Đánh giá của tôi": gia sư đang ở chế độ gia sư xem đánh giá về mình với vai trò gia sư, còn lại là học viên. */
    public void openMyReviews() {
        boolean tutor = isTutor() && host.classroom.tutorMode();
        openReviews(myId(), myName(), tutor ? Review.TUTOR : Review.STUDENT);
    }
    /** Đánh giá có thể viết cho hợp đồng này chưa: đã chốt và buổi học đầu tiên đã kết thúc. */
    public static boolean canReview(Contract c) {
        return c != null && c.agreed() && System.currentTimeMillis() >= c.startAt + c.minutes * 60_000L;
    }

    // UC27 – Báo cáo vi phạm -------------------------------------------------------------------------

    public String reportType() { return reportType; }
    public String reportSubject() { return reportSubject; }
    /** Màn 47; type/subjectId (CONTRACT hoặc CONVERSATION) chọn sẵn đối tượng, null để người dùng tự chọn. */
    public void openReport(String type, String subjectId) {
        reportType = type; reportSubject = subjectId;
        if (type != null && subjectId != null) { reports.clearDraft(); reports.draftSubject = type + ":" + subjectId; }
        open(REPORT);
    }
    public void openReports() { open(REPORTS); }
    /** Thay màn hiện tại (ví dụ sau khi gửi báo cáo): quay lại sẽ về nơi đã mở màn hiện tại. */
    public void replaceWith(String destination) {
        String origin = origins.get(host.screen);
        if (origin != null) origins.put(destination, origin); else origins.remove(destination);
        host.hideKeyboard(); host.show(destination);
    }

    // UC29 – Xử lý báo cáo vi phạm (quản trị viên) ---------------------------------------------------

    public String adminReportId() { return adminReportId; }
    /** Danh sách báo cáo cho quản trị viên. Trang quản trị gọi exchange.openReportsAdmin(). */
    public void openReportsAdmin() { open(REPORTS_ADMIN); }
    public void openHandleReport(Report report) { adminReportId = report.id; open(HANDLE_REPORT); }
    public void openContract(Contract contract) { contractId = contract.id; contracts.draft = null; open(CONTRACT); }
    /** Sau khi gửi: màn 40 chuyển sang xem hợp đồng vừa tạo. */
    public void showCreated(String id) { contractId = id; contracts.draft = null; }

    /** Hợp đồng của buổi học (null nếu chưa có). Học thử không có hợp đồng. */
    public Contract contractFor(Lesson lesson) { return lesson == null ? null : contracts.forLesson(lesson.id); }
    /** Buổi học cần hợp đồng: đặt lịch (không phải học thử) đã được gia sư chấp nhận. */
    public static boolean needsContract(Lesson lesson) { return lesson != null && !lesson.trial && lesson.confirmed(); }

    /**
     * Gọi ngay sau khi gia sư bấm "Chấp nhận" một yêu cầu đặt lịch: gợi ý gửi hợp đồng cho học viên.
     * Học thử không cần hợp đồng.
     */
    public void afterAccepted(Lesson lesson) {
        if (!needsContract(lesson)) return;
        new AlertDialog.Builder(host).setTitle("Gửi hợp đồng dạy?")
                .setMessage("Đã chấp nhận lịch học với " + lesson.studentName + ". Gửi hợp đồng dạy để học viên xem và xác nhận số buổi, học phí và điều khoản.")
                .setNegativeButton("Để sau", null)
                .setPositiveButton("Soạn hợp đồng", (d, w) -> openLessonContract(lesson)).show();
    }

    /**
     * Mở hợp đồng của buổi học: có hợp đồng đang chờ/đã chốt thì mở nó; gia sư chưa có thì soạn mới
     * (cần đã ký hợp đồng cộng tác và quản trị viên đã soạn mẫu); học viên chưa có thì báo chờ gia sư.
     */
    public void openLessonContract(Lesson lesson) {
        if (lesson == null) return;
        Contract existing = contracts.forLesson(lesson.id);
        if (existing != null && !existing.closed()) { openContract(existing); return; }
        String me = myId();
        if (!me.equals(lesson.tutorId)) {
            if (existing != null) { openContract(existing); return; }
            dialog("Chưa có hợp đồng", "Gia sư " + lesson.tutorName + " sẽ gửi hợp đồng dạy sau khi chấp nhận lịch học. Bạn sẽ thấy hợp đồng ở đây và ở mục Cá nhân → Hợp đồng của tôi.");
            return;
        }
        if (!needsContract(lesson)) { dialog("Chưa thể gửi hợp đồng", "Chỉ gửi hợp đồng cho lịch học đã chấp nhận (không áp dụng cho học thử)."); return; }
        if (!agreements.signed()) {
            new AlertDialog.Builder(host).setTitle("Ký hợp đồng cộng tác trước")
                    .setMessage("Gia sư cần ký hợp đồng cộng tác với hệ thống trước khi gửi hợp đồng dạy cho học viên.")
                    .setNegativeButton(R.string.cancel, null).setPositiveButton("Ký ngay", (d, w) -> openAgreement()).show();
            return;
        }
        AgreementState.Template template = agreements.template(AgreementState.STUDENT_TUTOR);
        if (template == null) {
            dialog("Chưa có mẫu hợp đồng", "Quản trị viên chưa soạn mẫu hợp đồng học viên – gia sư. Vui lòng thử lại sau.");
            return;
        }
        Contract d = fromLesson(lesson);
        if (existing != null) keepTerms(d, existing);
        d.baseTerms = template.content; d.templateVersion = template.version;
        contracts.draft = d; contractId = null; open(CONTRACT);
    }
    /** "Tạo hợp đồng mới" từ hợp đồng đã bị từ chối/hủy: giữ số buổi, học phí, thỏa thuận cũ; điều khoản lấy mẫu mới nhất. */
    public void redraft(Contract old) {
        AgreementState.Template template = agreements.template(AgreementState.STUDENT_TUTOR);
        if (template == null) { dialog("Chưa có mẫu hợp đồng", "Quản trị viên chưa soạn mẫu hợp đồng học viên – gia sư."); return; }
        Lesson lesson = host.classroom.store.lesson(old.lessonId);
        if (lesson == null || !needsContract(lesson)) { dialog("Chưa thể tạo hợp đồng mới", "Buổi học của hợp đồng này không còn ở trạng thái đã chấp nhận."); return; }
        Contract value = fromLesson(lesson);
        keepTerms(value, old);
        value.baseTerms = template.content; value.templateVersion = template.version;
        contracts.draft = value; contractId = null; host.show(CONTRACT);
    }

    /** Bản nháp điền sẵn từ buổi học đã chấp nhận: môn, giờ bắt đầu, thời lượng, hình thức, học phí một buổi. */
    static Contract fromLesson(Lesson lesson) {
        Contract d = new Contract();
        d.lessonId = lesson.id; d.studentId = lesson.studentId; d.tutorId = lesson.tutorId;
        d.studentName = lesson.studentName; d.tutorName = lesson.tutorName;
        d.subject = lesson.title.length() > 120 ? lesson.title.substring(0, 120) : lesson.title;
        d.minutes = lesson.minutes; d.mode = lesson.mode; d.fee = lesson.price; d.sessions = 4;
        schedule(d, LocalDate.parse(lesson.date), LocalTime.of(lesson.hour, lesson.minute));
        return d;
    }
    /** Ngày bắt đầu và giờ học: lịch hằng tuần lấy theo thứ của ngày bắt đầu. */
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
    /** Giữ phần gia sư đã điền ở hợp đồng cũ; lịch, thời lượng, hình thức luôn theo buổi học hiện tại. */
    static void keepTerms(Contract into, Contract old) {
        into.subject = old.subject; into.sessions = old.sessions; into.fee = old.fee; into.terms = old.terms;
    }
}