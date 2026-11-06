package com.example.tmdt;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Thống kê cho quản trị viên (Thành viên 4): phần tính toán không phụ thuộc Android/Firebase để kiểm thử được.
 * Biểu đồ cột: đếm số mục theo khoảng thời gian từ ngày – đến ngày, tự gom theo ngày / tuần / tháng.
 */
public final class AdminStats {
    private AdminStats() {}

    public static final String DAY = "DAY", WEEK = "WEEK", MONTH = "MONTH";
    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MM/yyyy");

    /** Một cột của biểu đồ cột. */
    public static final class Bucket {
        public final LocalDate start, end; public final String label; public int count;
        Bucket(LocalDate start, LocalDate end, String label) { this.start = start; this.end = end; this.label = label; }
    }
    /** Khoảng ≤ 31 ngày gom theo ngày, ≤ 26 tuần gom theo tuần, dài hơn gom theo tháng. */
    public static String unitFor(LocalDate from, LocalDate to) {
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        return days <= 31 ? DAY : days <= 182 ? WEEK : MONTH;
    }

    /**
     * Đếm các mốc thời gian (epoch millis) rơi vào [from, to] theo từng cột. Mốc ngoài khoảng bị bỏ qua.
     * Cột tuần bắt đầu thứ Hai; cột đầu/cuối được cắt cho vừa khoảng đã chọn.
     */
    public static List<Bucket> bars(Collection<Long> times, LocalDate from, LocalDate to, ZoneId zone) {
        if (to.isBefore(from)) { LocalDate swap = from; from = to; to = swap; }
        String unit = unitFor(from, to);
        List<Bucket> buckets = new ArrayList<>();
        LocalDate cursor = from;
        while (!cursor.isAfter(to)) {
            LocalDate end;
            String label;
            if (DAY.equals(unit)) { end = cursor; label = cursor.format(DAY_LABEL); }
            else if (WEEK.equals(unit)) {
                end = cursor.plusDays(7 - cursor.getDayOfWeek().getValue());
                if (end.isAfter(to)) end = to;
                label = "T" + cursor.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) + " (" + cursor.format(DAY_LABEL) + ")";
            } else {
                end = cursor.withDayOfMonth(cursor.lengthOfMonth());
                if (end.isAfter(to)) end = to;
                label = cursor.format(MONTH_LABEL);
            }
            buckets.add(new Bucket(cursor, end, label));
            cursor = end.plusDays(1);
        }
        for (Long time : times) {
            if (time == null || time <= 0) continue;
            LocalDate day = Instant.ofEpochMilli(time).atZone(zone).toLocalDate();
            if (day.isBefore(from) || day.isAfter(to)) continue;
            for (Bucket b : buckets) if (!day.isBefore(b.start) && !day.isAfter(b.end)) { b.count++; break; }
        }
        return buckets;
    }

    public static int total(List<Bucket> buckets) { int sum = 0; for (Bucket b : buckets) sum += b.count; return sum; }
    public static int max(List<Bucket> buckets) { int max = 0; for (Bucket b : buckets) max = Math.max(max, b.count); return max; }

    /** Môn học từ tiêu đề buổi học, ví dụ "Toán THPT" → "Toán", "Tiếng Anh Luyện thi" → "Tiếng Anh". */
    public static String subjectOf(String title) {
        if (title == null) return "";
        String value = title.trim();
        for (String level : new String[]{"Tiểu học", "THCS", "THPT", "Luyện thi", "Khác"})
            if (value.endsWith(" " + level)) return value.substring(0, value.length() - level.length() - 1).trim();
        return value;
    }
}
