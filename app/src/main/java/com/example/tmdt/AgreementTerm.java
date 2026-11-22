package com.example.tmdt;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Thời hạn của hợp đồng cộng tác gia sư – hệ thống: ký 1 hoặc 2 năm, nội dung không đổi trong thời hạn,
 * gia hạn trong 30 ngày cuối, chấm dứt khi vi phạm (ký lại sau 30 ngày). Thuần Java để test được.
 */
public final class AgreementTerm {
    public static final long DAY = 86_400_000L;
    /** Một năm hợp đồng = 365 ngày (rules Firestore dùng cùng con số). */
    public static final int YEAR_DAYS = 365;
    /** Mở gia hạn khi còn ≤ 30 ngày. */
    public static final int RENEW_WINDOW_DAYS = 30;
    /** Bị chấm dứt do vi phạm thì 30 ngày sau mới được ký hợp đồng mới. */
    public static final int RESIGN_WAIT_DAYS = 30;
    public static final String SIGNED = "DA_KY", TERMINATED = "CHAM_DUT";

    public enum Phase {
        /** Chưa từng ký. */
        NONE,
        /** Ký theo cách cũ (không có thời hạn): cần ký lại theo thời hạn mới. */
        LEGACY,
        /** Đang hiệu lực, chưa tới 30 ngày cuối. */
        ACTIVE,
        /** Đang hiệu lực, còn ≤ 30 ngày: được gia hạn. */
        EXPIRING,
        /** Đã gia hạn trước: kỳ mới bắt đầu sau (kỳ cũ vẫn hiệu lực tới lúc đó). */
        RENEWED,
        /** Hết hạn, chưa gia hạn. */
        EXPIRED,
        /** Bị chấm dứt, chưa đủ 30 ngày. */
        TERMINATED_WAIT,
        /** Bị chấm dứt, đã quá 30 ngày: được ký mới. */
        TERMINATED_CAN_SIGN
    }

    private AgreementTerm() {}

    public static long end(long start, int years) { return start + (long) years * YEAR_DAYS * DAY; }

    /** {@code start} = 0 nghĩa là hợp đồng cũ không có thời hạn. */
    public static Phase phase(long now, boolean exists, String status, long start, long end, long terminatedAt) {
        if (!exists) return Phase.NONE;
        if (TERMINATED.equals(status)) return now >= resignAllowedAt(terminatedAt) ? Phase.TERMINATED_CAN_SIGN : Phase.TERMINATED_WAIT;
        if (end <= 0) return Phase.LEGACY;
        if (now >= end) return Phase.EXPIRED;
        if (start > now) return Phase.RENEWED;
        return now >= end - RENEW_WINDOW_DAYS * DAY ? Phase.EXPIRING : Phase.ACTIVE;
    }

    /** Được gửi hợp đồng dạy cho học viên (hợp đồng cộng tác còn hiệu lực). */
    public static boolean active(Phase p) { return p == Phase.ACTIVE || p == Phase.EXPIRING || p == Phase.RENEWED; }
    /** Được bấm ký (ký mới hoặc gia hạn) ở giai đoạn này. */
    public static boolean canSign(Phase p) {
        return p == Phase.NONE || p == Phase.LEGACY || p == Phase.EXPIRING || p == Phase.EXPIRED || p == Phase.TERMINATED_CAN_SIGN;
    }
    public static boolean renewal(Phase p) { return p == Phase.EXPIRING; }
    /** Gia hạn khi còn hiệu lực: kỳ mới nối tiếp ngay ngày hết hạn; còn lại bắt đầu từ lúc ký. */
    public static long newStart(Phase p, long now, long currentEnd) { return renewal(p) ? currentEnd : now; }
    public static long resignAllowedAt(long terminatedAt) { return terminatedAt + RESIGN_WAIT_DAYS * DAY; }
    /** Số ngày còn lại (làm tròn lên), 0 khi đã hết. */
    public static long daysLeft(long now, long until) { return until <= now ? 0 : (until - now + DAY - 1) / DAY; }

    public static String date(long millis, ZoneId zone) {
        return DateTimeFormatter.ofPattern("dd/MM/yyyy").format(Instant.ofEpochMilli(millis).atZone(zone));
    }

    /**
     * Điều khoản chung đi kèm mọi hợp đồng cộng tác, điền sẵn thời hạn. Lưu nguyên văn vào hợp đồng lúc ký,
     * nên đổi chữ ở đây không làm thay đổi hợp đồng đã ký.
     */
    public static String clauses(int years, long start, long end, ZoneId zone) {
        return "1. Thời hạn: " + years + " năm, có hiệu lực từ " + date(start, zone) + " đến hết " + date(end - 1, zone) + ".\n"
                + "2. Không thay đổi trong thời hạn: nội dung đã ký giữ nguyên tới hết thời hạn. Khi hệ thống ban hành mẫu mới, "
                + "mẫu mới chỉ áp dụng khi gia sư gia hạn hoặc ký hợp đồng mới.\n"
                + "3. Vi phạm hợp đồng: gia sư hủy buổi học không lý do nhiều lần, thu tiền học ngoài hệ thống, khai sai thông tin "
                + "hồ sơ hoặc có hành vi không phù hợp với học viên thì hệ thống được cảnh cáo, đơn phương chấm dứt hợp đồng "
                + "và khóa tài khoản tùy mức độ. Bị chấm dứt thì gia sư không gửi được hợp đồng dạy mới và chỉ được ký lại sau "
                + RESIGN_WAIT_DAYS + " ngày; các khóa đang dạy vẫn hoàn tất và thanh toán theo hợp đồng dạy đã ký.\n"
                + "4. Gia hạn: trong " + RENEW_WINDOW_DAYS + " ngày cuối, gia sư được gia hạn thêm 1 hoặc 2 năm theo mẫu hiện hành; "
                + "kỳ mới bắt đầu ngay khi kỳ cũ kết thúc. Hết hạn mà chưa gia hạn thì không gửi được hợp đồng dạy mới cho tới khi ký lại.\n"
                + "5. Chấm dứt trước hạn: gia sư muốn chấm dứt trước hạn thì liên hệ quản trị viên; hệ thống chấm dứt trước hạn "
                + "chỉ khi gia sư vi phạm điều 3 và phải ghi rõ lý do.";
    }
}
