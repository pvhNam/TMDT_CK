package com.example.tmdt;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

/** Thống kê quản trị: chia cột theo khoảng ngày, cơ cấu biểu đồ tròn, tỷ lệ và nhãn. */
public class AdminStatsTest {
    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static long at(int y, int m, int d, int h) { return LocalDate.of(y, m, d).atTime(h, 0).atZone(VN).toInstant().toEpochMilli(); }

    @Test public void shortRangeGroupsByDay() {
        LocalDate from = LocalDate.of(2026, 10, 1), to = LocalDate.of(2026, 10, 7);
        List<AdminStats.Bucket> bars = AdminStats.bars(Arrays.asList(at(2026, 10, 1, 9), at(2026, 10, 1, 23), at(2026, 10, 7, 0),
                at(2026, 9, 30, 23), at(2026, 10, 8, 0)), from, to, VN);
        assertEquals(AdminStats.DAY, AdminStats.unitFor(from, to));
        assertEquals(7, bars.size());
        assertEquals("01/10", bars.get(0).label);
        assertEquals(2, bars.get(0).count);
        assertEquals(1, bars.get(6).count);
        assertEquals(3, AdminStats.total(bars));   // 2 mốc ngoài khoảng bị bỏ
        assertEquals(2, AdminStats.max(bars));
    }

    @Test public void mediumRangeGroupsByWeekStartingMonday() {
        LocalDate from = LocalDate.of(2026, 10, 1), to = LocalDate.of(2026, 11, 30);   // 61 ngày
        assertEquals(AdminStats.WEEK, AdminStats.unitFor(from, to));
        List<AdminStats.Bucket> bars = AdminStats.bars(Arrays.asList(at(2026, 10, 4, 10), at(2026, 10, 5, 10)), from, to, VN);
        assertEquals(LocalDate.of(2026, 10, 4), bars.get(0).end);    // 01/10 (thứ Năm) → Chủ nhật 04/10
        assertEquals(LocalDate.of(2026, 10, 5), bars.get(1).start);  // tuần sau bắt đầu thứ Hai
        assertEquals(1, bars.get(0).count);
        assertEquals(1, bars.get(1).count);
        assertEquals(to, bars.get(bars.size() - 1).end);
    }

    @Test public void longRangeGroupsByMonth() {
        LocalDate from = LocalDate.of(2026, 1, 15), to = LocalDate.of(2026, 12, 31);
        List<AdminStats.Bucket> bars = AdminStats.bars(new ArrayList<>(), from, to, VN);
        assertEquals(AdminStats.MONTH, AdminStats.unitFor(from, to));
        assertEquals(12, bars.size());
        assertEquals("01/2026", bars.get(0).label);
        assertEquals(LocalDate.of(2026, 1, 31), bars.get(0).end);
    }

    @Test public void reversedRangeIsSwapped() {
        List<AdminStats.Bucket> bars = AdminStats.bars(new ArrayList<>(), LocalDate.of(2026, 10, 3), LocalDate.of(2026, 10, 1), VN);
        assertEquals(3, bars.size());
    }
}
