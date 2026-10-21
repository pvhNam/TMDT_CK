package com.example.tmdt;

import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.tmdt.ui.exchange.ContractFragment;
import com.example.tmdt.ui.exchange.ContractsFragment;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Thành viên 4 – UC23 Chốt hợp đồng dạy: màn 40 và "Hợp đồng của tôi", lựa chọn đang mở và điều hướng quay lại.
 * MainActivity chỉ chuyển tiếp sang lớp này (giống {@link Classroom}).
 */
public final class Exchange {
    public static final String CONTRACT = "contract", CONTRACTS = "contracts";
    public static final String TUTOR = "TUTOR", STUDENT = "STUDENT";
    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] WEEKDAYS = {"Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy", "Chủ nhật"};

    private final MainActivity host;
    public final ContractState contracts;

    // Lựa chọn đang mở (giữ khi xoay máy).
    private String contractId;
    /** Màn đã mở từng màn của phần này, để nút quay lại trở về đúng chỗ. */
    private final Map<String, String> origins = new HashMap<>();

    Exchange(MainActivity host, Bundle state) {
        this.host = host;
        contracts = new ViewModelProvider(host).get(ContractState.class);
        if (state != null) {
            contractId = state.getString("ex.contract");
            Bundle saved = state.getBundle("ex.origins");
            if (saved != null) for (String key : saved.keySet()) origins.put(key, saved.getString(key));
        }
    }
    void save(Bundle out) {
        out.putString("ex.contract", contractId);
        Bundle saved = new Bundle(); for (Map.Entry<String, String> e : origins.entrySet()) saved.putString(e.getKey(), e.getValue());
        out.putBundle("ex.origins", saved);
    }

    // Điều hướng dùng bởi MainActivity ---------------------------------------------------------------

    /** Đăng nhập/đăng xuất: bật hoặc tắt listener hợp đồng của tài khoản. */
    void accountChanged(boolean signedIn) {
        if (signedIn) contracts.start(); else { contracts.stop(); contracts.draft = null; contractId = null; }
    }
    static boolean owns(String destination) { return CONTRACT.equals(destination) || CONTRACTS.equals(destination); }
    /** Khách cần đăng nhập; mở màn 40 mà không có hợp đồng/bản nháp thì về danh sách. */
    String redirect(String destination) {
        if (!owns(destination)) return destination;
        if (!host.account.signedIn()) return "login";
        if (CONTRACT.equals(destination) && contractId == null && contracts.draft == null) return CONTRACTS;
        return destination;
    }
    Fragment fragment(String destination) {
        switch (destination) {
            case CONTRACT: return new ContractFragment();
            case CONTRACTS: return new ContractsFragment();
            default: return null;
        }
    }
    String backTarget(String screen) {
        if (!owns(screen)) return null;
        if (CONTRACT.equals(screen)) contracts.draft = null;
        String origin = origins.get(screen);
        if (origin != null && !origin.equals(screen)) return origin;
        return "account";
    }
    private void open(String destination) {
        String current = host.screen;
        if (!destination.equals(current) && !(owns(current) && destination.equals(origins.get(current)))) origins.put(destination, current);
        host.hideKeyboard(); host.show(destination);
    }

    // Thông tin người đang đăng nhập -----------------------------------------------------------------

    public boolean isTutor() { UserProfile p = host.account.profile(); return p != null && (p.tutor || TUTOR.equals(p.role)); }
    public String myName() { UserProfile p = host.account.profile(); return p != null && !p.name.isEmpty() ? p.name : "Người dùng"; }
    public void back() { host.back(); }
    public void show(String destination) { host.show(destination); }
    public void dialog(String title, String message) {
        new AlertDialog.Builder(host).setTitle(title).setMessage(message).setPositiveButton(R.string.understood, null).show();
    }

    // UC23 – Hợp đồng --------------------------------------------------------------------------------

    public String contractId() { return contractId; }
    public Contract draft() { return contracts.draft; }
    public void openContracts() { open(CONTRACTS); }
    public void openContract(Contract contract) { contractId = contract.id; contracts.draft = null; open(CONTRACT); }
    /** Sau khi gửi đề nghị: màn 40 chuyển sang xem hợp đồng vừa tạo. */
    public void showCreated(String id) { contractId = id; contracts.draft = null; }

    /** Mục "Hợp đồng dạy" trong màn trò chuyện: mở hợp đồng đang có, hoặc soạn đề nghị mới từ buổi học đang trao đổi. */
    public void openContractFor(Conversation conversation) {
        Contract existing = contracts.latestFor(conversation.id);
        if (existing != null && !existing.closed()) { openContract(existing); return; }
        Contract value = newDraft(conversation, existing);
        if (value == null) {
            dialog("Chưa thể lập hợp đồng", "Hợp đồng dạy được lập giữa một học viên và một gia sư. Cuộc trò chuyện này chưa xác định được bên gia sư.");
            return;
        }
        contracts.draft = value; contractId = null; open(CONTRACT);
    }
    /** "Tạo đề nghị mới" từ hợp đồng đã bị từ chối/hủy: giữ nguyên các điều khoản cũ để sửa. */
    public void redraft(Contract old) {
        Contract value = copy(old);
        value.id = ""; value.code = ""; value.status = Contract.PENDING; value.rejectReason = "";
        contracts.draft = value; contractId = null; host.show(CONTRACT);
    }
    private Contract newDraft(Conversation c, Contract previous) {
        String me = host.account.signedIn() ? host.chat.myId() : "";
        Contract d = previous != null ? copy(previous) : new Contract();
        d.id = ""; d.code = ""; d.status = Contract.PENDING; d.rejectReason = ""; d.conversationId = c.id;
        if (TUTOR.equals(c.peerRole)) { d.studentId = me; d.tutorId = c.peerId; d.studentName = myName(); d.tutorName = c.peerName; }
        else if (TUTOR.equals(c.myRole) || isTutor()) { d.tutorId = me; d.studentId = c.peerId; d.tutorName = myName(); d.studentName = c.peerName; }
        else return null;
        if (previous != null) return d;
        LocalDate start = LocalDate.now().plusDays(1);
        LocalTime time = LocalTime.of(19, 0);
        if (c.hasLesson()) {
            d.subject = c.lessonTitle;
            // Ví dụ "15/09/2026 · 19:00–20:00"
            try {
                String[] parts = c.lessonTime.split("·");
                LocalDate day = LocalDate.parse(parts[0].trim(), DATE);
                if (day.isAfter(LocalDate.now())) start = day;
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
            String subject = host.catalog.subjects.get(offering.subjectId);
            if (d.subject.isEmpty() && subject != null) d.subject = (subject + " " + CatalogState.level(offering.level)).trim();
            if (!c.hasLesson()) d.minutes = 90;
        }
        if (d.fee == 0) d.fee = 150000;
        if (d.subject.isEmpty()) d.subject = "Môn học";
        if (d.subject.length() > 120) d.subject = d.subject.substring(0, 120);
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
}
