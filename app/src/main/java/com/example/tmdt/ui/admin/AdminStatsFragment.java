package com.example.tmdt.ui.admin;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.tmdt.ManageState;
import com.example.tmdt.AdminStats;
import com.example.tmdt.R;
import com.example.tmdt.ui.common.Ui;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Thống kê lớp học và đăng ký (Thành viên 4).
 * - Biểu đồ cột theo tuỳ chọn thời gian (từ ngày – đến ngày): lượt đăng ký học, người dùng mới hoặc lớp nhóm mới;
 *   tự gom theo ngày / tuần / tháng tuỳ độ dài khoảng.
 * - Biểu đồ tròn: cơ cấu đăng ký hoặc lớp nhóm theo môn học hoặc trạng thái, kèm tỷ lệ % và chú giải.
 * Mọi số liệu tính theo ngày tạo (created_at) nằm trong khoảng đã chọn.
 */
public final class AdminStatsFragment extends ManageScreen {
    private static final String[] METRICS = {"Lượt đăng ký học", "Người dùng mới", "Lớp nhóm mới"};
    private LocalDate from = LocalDate.now().minusDays(29), to = LocalDate.now();
    private int metric, pieTarget, pieBy;

    @Override public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        if (state != null) {
            from = LocalDate.parse(state.getString("from", from.toString())); to = LocalDate.parse(state.getString("to", to.toString()));
            metric = state.getInt("metric"); pieTarget = state.getInt("pieTarget"); pieBy = state.getInt("pieBy");
        }
    }
    @Override public void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        out.putString("from", from.toString()); out.putString("to", to.toString());
        out.putInt("metric", metric); out.putInt("pieTarget", pieTarget); out.putInt("pieBy", pieBy);
    }

    @Override protected View build() {
        ManageState admin = manage();
        LinearLayout root = root("Thống kê"); LinearLayout body = ui.page(root);
        if (!feedback(body)) return root;

        // Khoảng thời gian -------------------------------------------------------------------------
        ui.section(body, "Khoảng thời gian", 18); ui.space(body, 8);
        LinearLayout range = ui.row();
        ui.weight(range, datePicker("Từ ngày", from, picked -> { from = picked; if (from.isAfter(to)) to = from; refresh(); })); ui.gap(range, 10);
        ui.weight(range, datePicker("Đến ngày", to, picked -> { to = picked; if (to.isBefore(from)) from = to; refresh(); }));
        ui.add(body, range); ui.space(body, 8);
        LinearLayout quick = ui.row(); LocalDate today = LocalDate.now();
        quick(quick, "7 ngày", today.minusDays(6), today); ui.gap(quick, 6);
        quick(quick, "30 ngày", today.minusDays(29), today); ui.gap(quick, 6);
        quick(quick, "Tháng này", today.withDayOfMonth(1), today); ui.gap(quick, 6);
        quick(quick, "Năm nay", today.withDayOfYear(1), today);
        ui.add(body, quick); ui.space(body, 16);

        if (waiting(body, ManageState.USERS, ManageState.CLASSES, ManageState.REGISTRATIONS, ManageState.LESSONS)) return root;
        ZoneId zone = ZoneId.systemDefault();

        // Biểu đồ cột --------------------------------------------------------------------------------
        ui.section(body, "Biểu đồ cột", 18); ui.space(body, 8);
        LinearLayout metrics = ui.row();
        for (int i = 0; i < METRICS.length; i++) {
            final int index = i; if (i > 0) ui.gap(metrics, 6);
            TextView chip = ui.choice(METRICS[i], metric == i, () -> { metric = index; refresh(); }); chip.setTextSize(13);
            ui.weight(metrics, chip);
        }
        ui.add(body, metrics); ui.space(body, 10);
        List<AdminStats.Bucket> bars = AdminStats.bars(times(admin, metric), from, to, zone);
        String unit = AdminStats.unitFor(from, to);
        LinearLayout barCard = ui.card();
        ui.add(barCard, ui.text(METRICS[metric] + " · " + ManageScreen.date(epoch(from, zone)) + " – " + ManageScreen.date(epoch(to, zone)), 15, Ui.INK, true));
        ui.space(barCard, 4);
        ui.add(barCard, ui.text("Tổng: " + AdminStats.total(bars) + " · mỗi cột là 1 " + (AdminStats.DAY.equals(unit) ? "ngày" : AdminStats.WEEK.equals(unit) ? "tuần" : "tháng"), 13, Ui.MUTED, false));
        ui.space(barCard, 8);
        BarChartView barChart = new BarChartView(requireContext()); barChart.setData(bars);
        ui.add(barCard, barChart);
        ui.add(body, barCard); ui.space(body, 18);

        // Biểu đồ tròn -------------------------------------------------------------------------------
        ui.section(body, "Biểu đồ tròn – cơ cấu", 18); ui.space(body, 8);
        LinearLayout target = ui.row();
        ui.weight(target, ui.choice("Đăng ký học", pieTarget == 0, () -> { pieTarget = 0; refresh(); })); ui.gap(target, 8);
        ui.weight(target, ui.choice("Lớp nhóm", pieTarget == 1, () -> { pieTarget = 1; refresh(); }));
        ui.add(body, target); ui.space(body, 6);
        LinearLayout by = ui.row();
        ui.weight(by, ui.choice("Theo môn học", pieBy == 0, () -> { pieBy = 0; refresh(); })); ui.gap(by, 8);
        ui.weight(by, ui.choice("Theo trạng thái", pieBy == 1, () -> { pieBy = 1; refresh(); }));
        ui.add(body, by); ui.space(body, 10);

        List<AdminStats.Slice> slices = AdminStats.slices(keys(admin, zone));
        LinearLayout pieCard = ui.card();
        ui.add(pieCard, ui.text((pieTarget == 0 ? "Đăng ký học" : "Lớp nhóm") + (pieBy == 0 ? " theo môn học" : " theo trạng thái"), 15, Ui.INK, true));
        ui.space(pieCard, 6);
        PieChartView pie = new PieChartView(requireContext()); pie.setData(slices, pieTarget == 0 ? "đăng ký" : "lớp");
        ui.add(pieCard, pie); ui.space(pieCard, 8);
        if (slices.isEmpty()) ui.add(pieCard, ui.text("Không có dữ liệu trong khoảng đã chọn.", 14, Ui.MUTED, false));
        for (int i = 0; i < slices.size(); i++) legend(pieCard, PieChartView.color(i), slices.get(i));
        ui.add(body, pieCard); ui.space(body, 10);
        ui.add(body, ui.text("Số liệu tính theo ngày tạo trong khoảng đã chọn. Đăng ký học gồm lịch học 1-1 (cả học thử) và đăng ký lớp nhóm.", 13, Ui.MUTED, false));
        return root;
    }

    /** Ô chọn ngày dạng picker của nhóm. */
    private View datePicker(String label, LocalDate value, Consumer<LocalDate> picked) {
        LinearLayout box = ui.column();
        ui.label(box, label);
        TextView text = ui.value(value.format(ManageScreen.DATE));
        box.addView(ui.picker(R.drawable.ic_calendar, text, () -> new DatePickerDialog(requireContext(),
                R.style.ThemeOverlay_TMDT_Classroom_DatePicker, (v, y, m, d) -> picked.accept(LocalDate.of(y, m + 1, d)),
                value.getYear(), value.getMonthValue() - 1, value.getDayOfMonth()).show()), ui.lp(-1, -2));
        return box;
    }
    private void quick(LinearLayout row, String label, LocalDate start, LocalDate end) {
        TextView chip = ui.choice(label, from.equals(start) && to.equals(end), () -> { from = start; to = end; refresh(); });
        chip.setTextSize(13); ui.weight(row, chip);
    }
    private void legend(LinearLayout card, int color, AdminStats.Slice slice) {
        LinearLayout row = ui.row(); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(0, ui.dp(5), 0, ui.dp(5));
        View dot = new View(requireContext()); ui.surface(dot, color, 6, 0); row.addView(dot, ui.lp(12, 12)); ui.gap(row, 10);
        TextView name = ui.text(slice.label, 14, Ui.INK, false); name.setMaxLines(1); ui.weight(row, name); ui.gap(row, 8);
        row.addView(ui.text(slice.count + "", 14, Ui.MUTED, false)); ui.gap(row, 12);
        TextView percent = ui.text(slice.percentLabel(), 14, Ui.INK, true); percent.setMinWidth(ui.dp(52)); percent.setGravity(Gravity.END);
        row.addView(percent);
        row.setContentDescription(slice.label + ": " + slice.count + ", " + slice.percentLabel());
        ui.add(card, row);
    }

    private static long epoch(LocalDate day, ZoneId zone) { return day.atStartOfDay(zone).toInstant().toEpochMilli(); }

    /** Mốc thời gian của chỉ số đang chọn cho biểu đồ cột. */
    private static List<Long> times(ManageState admin, int metric) {
        List<Long> times = new ArrayList<>();
        if (metric == 0) for (ManageState.Registration r : admin.allRegistrations()) times.add(r.createdAt);
        else if (metric == 1) for (ManageState.User u : admin.users()) times.add(u.createdAt);
        else for (ManageState.GroupRow c : admin.classes()) times.add(c.createdAt);
        return times;
    }
    /** Nhãn của từng mục trong khoảng đã chọn cho biểu đồ tròn. */
    private List<String> keys(ManageState admin, ZoneId zone) {
        long start = epoch(from, zone), end = epoch(to.plusDays(1), zone);
        List<String> keys = new ArrayList<>();
        if (pieTarget == 0) {
            for (ManageState.Registration r : admin.allRegistrations()) if (r.createdAt >= start && r.createdAt < end)
                keys.add(pieBy == 0 ? (r.subject.isEmpty() ? AdminStats.subjectOf(r.title) : r.subject) : AdminStats.statusLabel(r.statusKey()));
        } else {
            for (ManageState.GroupRow c : admin.classes()) if (c.createdAt >= start && c.createdAt < end)
                keys.add(pieBy == 0 ? c.subject : AdminStats.statusLabel(c.status));
        }
        return keys;
    }
}
