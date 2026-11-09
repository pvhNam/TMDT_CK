package com.example.tmdt.ui.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.tmdt.ManageState;
import com.example.tmdt.AdminStats;
import com.example.tmdt.R;
import com.example.tmdt.ui.common.Ui;
import java.util.ArrayList;
import java.util.List;

/**
 * Quản lý lớp học và đăng ký lớp học toàn hệ thống (Thành viên 4).
 * Ba thẻ: lớp nhóm, đăng ký lớp nhóm, lịch học 1-1 (đăng ký học với gia sư). Lọc theo trạng thái, tìm kiếm,
 * xem chi tiết. Chỉ xem — huỷ lớp, duyệt đăng ký vẫn do gia sư/học viên thực hiện để không lệch luồng thanh toán, hoàn tiền.
 */
public final class AdminClassesFragment extends ManageScreen {
    private static final String[] TABS = {"Lớp nhóm", "Đăng ký lớp", "Lịch học 1-1"};
    private static final String[][] FILTERS = {
            {"", "OPEN", "RUNNING", "ENDED", "CANCELLED"},
            {"", "PENDING", "CONFIRMED", "REJECTED", "CANCELLED"},
            {"", "PENDING", "CONFIRMED", "FINISHED", "REJECTED", "CANCELLED"}};
    private int tab;
    private String filter = "", query = "";
    private LinearLayout list;

    @Override public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        if (state != null) { tab = state.getInt("tab"); filter = state.getString("filter", ""); query = state.getString("query", ""); }
    }
    @Override public void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out); out.putInt("tab", tab); out.putString("filter", filter); out.putString("query", query);
    }

    @Override protected View build() {
        LinearLayout root = root("Lớp học và đăng ký");
        ui.tabs(root, TABS, tab, index -> { tab = index; filter = ""; refresh(); });
        LinearLayout body = ui.page(root);
        if (!feedback(body)) return root;
        if (waiting(body, ManageState.CLASSES, ManageState.REGISTRATIONS, ManageState.LESSONS)) return root;

        // Bộ lọc trạng thái (cuộn ngang) kèm số lượng từng trạng thái.
        String[] keys = FILTERS[tab];
        String[] labels = new String[keys.length];
        int selected = 0;
        for (int i = 0; i < keys.length; i++) {
            labels[i] = (keys[i].isEmpty() ? "Tất cả" : AdminStats.statusLabel(keys[i])) + " (" + count(keys[i]) + ")";
            if (keys[i].equals(filter)) selected = i;
        }
        chips(body, labels, selected, index -> { filter = keys[index]; refresh(); });
        search(body, tab == 0 ? "Tìm theo tên lớp, môn, gia sư" : "Tìm theo học viên, gia sư, môn học", query, text -> { query = text; fill(); });
        ui.space(body, 10);
        list = ui.column(); ui.add(body, list);
        fill();
        return root;
    }

    private int count(String key) {
        int n = 0;
        if (tab == 0) { for (ManageState.GroupRow c : manage().classes()) if (key.isEmpty() || key.equals(c.status)) n++; }
        else for (ManageState.Registration r : rows()) if (key.isEmpty() || key.equals(r.statusKey())) n++;
        return n;
    }
    private List<ManageState.Registration> rows() { return tab == 1 ? manage().groupRegistrations() : manage().lessons(); }

    private void fill() {
        if (list == null) return;
        list.removeAllViews();
        int shown = 0;
        if (tab == 0) {
            for (ManageState.GroupRow c : manage().classes()) {
                if (!filter.isEmpty() && !filter.equals(c.status)) continue;
                if (!matches(query, c.title, c.subject, c.tutorName)) continue;
                String sub = c.tutorName + " · " + c.members + "/" + c.capacity + " học viên" + (c.startDate.isEmpty() ? "" : " · bắt đầu " + c.startDate);
                ui.add(list, row(c.tutorId, c.title, sub, () -> openClass(c), pill(AdminStats.statusLabel(c.status), c.status)));
                ui.space(list, 8); shown++;
            }
        } else {
            for (ManageState.Registration r : rows()) {
                if (!filter.isEmpty() && !filter.equals(r.statusKey())) continue;
                if (!matches(query, r.studentName, r.tutorName, r.title, r.subject)) continue;
                String title = (r.studentName.isEmpty() ? "Học viên" : r.studentName) + " → " + (r.tutorName.isEmpty() ? "gia sư" : r.tutorName);
                String sub = (r.title.isEmpty() ? r.subject : r.title) + " · gửi " + date(r.createdAt);
                List<TextView> pills = new ArrayList<>();
                pills.add(pill(AdminStats.statusLabel(r.statusKey()), r.statusKey()));
                if (r.trial) pills.add(pill("Học thử", ""));
                ui.add(list, row(r.studentId, title, sub, () -> openRegistration(r), pills.toArray(new TextView[0])));
                ui.space(list, 8); shown++;
            }
        }
        if (shown == 0) ui.add(list, ui.note(R.drawable.ic_info, "Không có mục phù hợp.", Ui.PALE, Ui.BLUE, Ui.INK));
    }

    private void openClass(ManageState.GroupRow c) {
        LinearLayout content = ui.column();
        LinearLayout table = ui.table();
        ui.tableRow(table, R.drawable.ic_ex_book, "Môn học", c.subject + (c.level.isEmpty() ? "" : " · " + c.level));
        ui.tableRow(table, R.drawable.ic_cap, "Gia sư", c.tutorName.isEmpty() ? "—" : c.tutorName);
        ui.tableRow(table, R.drawable.ic_laptop, "Hình thức", c.mode.isEmpty() ? "—" : c.mode);
        ui.tableRow(table, R.drawable.ic_calendar, "Bắt đầu", c.startDate.isEmpty() ? "—" : c.startDate);
        ui.tableRow(table, R.drawable.ic_clock, "Số buổi", String.valueOf(c.sessions));
        ui.tableRow(table, R.drawable.ic_users, "Sĩ số", c.members + "/" + c.capacity);
        ui.tableRow(table, R.drawable.ic_coin, "Học phí", vnd(c.price));
        ui.tableRow(table, R.drawable.ic_info, "Trạng thái", AdminStats.statusLabel(c.status));
        ui.tableRow(table, R.drawable.ic_calendar, "Ngày tạo", date(c.createdAt));
        ui.add(content, table); ui.space(content, 14);
        List<ManageState.Registration> regs = manage().registrationsOfClass(c.id);
        ui.add(content, ui.text("Đăng ký vào lớp (" + regs.size() + ")", 16, Ui.INK, true)); ui.space(content, 6);
        if (regs.isEmpty()) ui.add(content, ui.text("Chưa có học viên đăng ký.", 14, Ui.MUTED, false));
        for (ManageState.Registration r : regs) {
            LinearLayout line = ui.row(); line.setPadding(0, ui.dp(4), 0, ui.dp(4));
            TextView name = ui.text(r.studentName.isEmpty() ? "(Học viên)" : r.studentName, 14, Ui.INK, false); ui.weight(line, name);
            line.addView(pill(AdminStats.statusLabel(r.status), r.status));
            ui.add(content, line);
        }
        detail(c.title.isEmpty() ? "Lớp nhóm" : c.title, content);
    }

    private void openRegistration(ManageState.Registration r) {
        LinearLayout content = ui.column();
        LinearLayout table = ui.table();
        ui.tableRow(table, R.drawable.ic_field_user, "Học viên", r.studentName.isEmpty() ? "—" : r.studentName);
        ui.tableRow(table, R.drawable.ic_cap, "Gia sư", r.tutorName.isEmpty() ? "—" : r.tutorName);
        ui.tableRow(table, R.drawable.ic_ex_book, r.group() ? "Lớp" : "Nội dung", r.title.isEmpty() ? r.subject : r.title);
        if (!r.group()) {
            ui.tableRow(table, R.drawable.ic_clock, "Lịch học", r.startAt <= 0 ? "—" : date(r.startAt) + " " +
                    java.time.Instant.ofEpochMilli(r.startAt).atZone(ZONE).toLocalTime().withSecond(0).withNano(0));
            ui.tableRow(table, R.drawable.ic_coin, "Học phí", r.trial ? "Học thử" : vnd(r.price));
        } else ui.tableRow(table, R.drawable.ic_coin, "Học phí lớp", vnd(r.price));
        ui.tableRow(table, R.drawable.ic_info, "Trạng thái", AdminStats.statusLabel(r.statusKey()));
        ui.tableRow(table, R.drawable.ic_calendar, "Ngày gửi", date(r.createdAt));
        ui.add(content, table);
        ui.space(content, 10);
        ui.add(content, ui.note(R.drawable.ic_info, "Duyệt, huỷ hoặc hoàn tiền do gia sư và học viên thực hiện trong luồng của họ; quản trị viên theo dõi tại đây.", Ui.PALE, Ui.BLUE, Ui.INK));
        detail(r.group() ? "Đăng ký lớp nhóm" : "Lịch học 1-1", content);
    }
}
