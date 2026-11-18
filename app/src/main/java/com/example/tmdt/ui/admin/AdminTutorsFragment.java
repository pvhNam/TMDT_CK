package com.example.tmdt.ui.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import com.example.tmdt.ManageState;
import com.example.tmdt.R;
import com.example.tmdt.ui.common.Ui;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Các chức năng quản lý cơ bản của gia sư (Thành viên 4): danh sách gia sư theo trạng thái xét duyệt,
 * số lớp/buổi học/học viên của từng gia sư, ẩn hoặc hiện gia sư trong tìm kiếm (nhan_lop),
 * khóa tài khoản gia sư (dùng chức năng khóa của trang Quản lý tài khoản) và lối sang màn duyệt hồ sơ.
 */
public final class AdminTutorsFragment extends ManageScreen {
    private static final String[] TABS = {"Tất cả", "Đã duyệt", "Chờ duyệt", "Đang ẩn"};
    private int tab;
    private String query = "";
    private LinearLayout list;

    @Override public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        if (state != null) { tab = state.getInt("tab"); query = state.getString("query", ""); }
    }
    @Override public void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out); out.putInt("tab", tab); out.putString("query", query);
    }

    @Override protected View build() {
        LinearLayout root = root("Quản lý gia sư");
        ui.tabs(root, TABS, tab, index -> { tab = index; refresh(); });
        LinearLayout body = ui.page(root);
        if (!feedback(body)) return root;
        if (waiting(body, ManageState.TUTORS, ManageState.USERS, ManageState.CLASSES, ManageState.LESSONS, ManageState.REGISTRATIONS)) return root;
        search(body, "Tìm theo tên, khu vực, trường, chuyên ngành", query, text -> { query = text; fill(); });
        ui.space(body, 10);
        list = ui.column(); ui.add(body, list);
        fill();
        return root;
    }

    private boolean inTab(ManageState.Tutor t) {
        switch (tab) {
            case 1: return "VERIFIED".equals(t.status) && t.visible;
            case 2: return "PENDING".equals(t.status) || "IN_REVIEW".equals(t.status) || "NEEDS_REVISION".equals(t.status);
            case 3: return "VERIFIED".equals(t.status) && !t.visible;
            default: return true;
        }
    }

    private void fill() {
        if (list == null) return;
        list.removeAllViews();
        List<ManageState.Tutor> shown = new ArrayList<>();
        for (ManageState.Tutor t : manage().tutors()) if (inTab(t) && matches(query, t.name, t.region, t.school, t.major)) shown.add(t);
        ui.add(list, ui.text(shown.size() + " gia sư", 14, Ui.MUTED, false)); ui.space(list, 8);
        if (tab == 2 && !shown.isEmpty()) {
            LinearLayout note = ui.note(R.drawable.ic_field_shield, "Mở màn Duyệt hồ sơ gia sư để xét minh chứng.", Ui.ORANGE_BG, Ui.ORANGE, Ui.INK);
            ui.clickable(note, () -> open("admin_tutors")); ui.add(list, note); ui.space(list, 8);
        }
        if (shown.isEmpty()) { ui.add(list, ui.note(R.drawable.ic_info, "Không có gia sư phù hợp.", Ui.PALE, Ui.BLUE, Ui.INK)); return; }
        for (ManageState.Tutor t : shown) {
            ManageState.User account = manage().user(t.id);
            List<TextView> pills = new ArrayList<>();
            pills.add(pill(t.statusLabel(), t.status));
            if ("VERIFIED".equals(t.status)) pills.add(pill(t.visible ? "Đang hiển thị" : "Đang ẩn", t.visible ? "VISIBLE" : "HIDDEN"));
            if (account != null && account.banned()) pills.add(pill("Đã khóa", "BANNED"));
            String sub = join(t.region, t.major.isEmpty() ? t.school : t.major);
            ui.add(list, row(t.id, t.name, sub, () -> open(t), pills.toArray(new TextView[0])));
            ui.space(list, 8);
        }
    }

    private void open(ManageState.Tutor t) {
        ManageState admin = manage();
        ManageState.User account = admin.user(t.id);
        int classes = 0, lessons = 0, done = 0;
        Set<String> students = new HashSet<>();
        for (ManageState.GroupRow c : admin.classes()) if (t.id.equals(c.tutorId)) classes++;
        for (ManageState.Registration r : admin.allRegistrations()) {
            if (!t.id.equals(r.tutorId)) continue;
            if (!r.group()) { lessons++; if (r.finished) done++; }
            if ("CONFIRMED".equals(r.status) && !r.studentId.isEmpty()) students.add(r.studentId);
        }
        LinearLayout content = ui.column();
        LinearLayout table = ui.table();
        ui.tableRow(table, R.drawable.ic_field_shield, "Xét duyệt", t.statusLabel());
        ui.tableRow(table, R.drawable.ic_field_location, "Khu vực", t.region.isEmpty() ? "—" : t.region);
        ui.tableRow(table, R.drawable.ic_cap, "Trường", t.school.isEmpty() ? "—" : t.school);
        ui.tableRow(table, R.drawable.ic_ex_book, "Chuyên ngành", t.major.isEmpty() ? "—" : t.major);
        ui.tableRow(table, R.drawable.ic_star, "Kinh nghiệm", t.experience + " năm");
        ui.tableRow(table, R.drawable.ic_laptop, "Hình thức", mode(t.mode));
        ui.tableRow(table, R.drawable.ic_users, "Lớp nhóm đã mở", String.valueOf(classes));
        ui.tableRow(table, R.drawable.ic_calendar, "Lịch học 1-1", lessons + " (" + done + " đã dạy)");
        ui.tableRow(table, R.drawable.ic_person, "Học viên đã nhận", String.valueOf(students.size()));
        ui.tableRow(table, R.drawable.ic_field_lock, "Tài khoản", account == null ? "—" : account.banned() ? "Đã khóa" : "Hoạt động");
        ui.tableRow(table, R.drawable.ic_calendar, "Đăng ký gia sư", date(t.createdAt));
        ui.add(content, table);

        AlertDialog[] holder = new AlertDialog[1];
        ui.space(content, 6);
        if ("VERIFIED".equals(t.status)) {
            if (t.visible) dialogAction(content, holder, "Ẩn khỏi tìm gia sư", Ui.OUTLINE,
                    () -> confirmVisible(t, false));
            else dialogAction(content, holder, "Hiện lại trong tìm gia sư", Ui.OUTLINE,
                    () -> confirmVisible(t, true));
        } else dialogAction(content, holder, "Xem hồ sơ chờ duyệt", Ui.OUTLINE, () -> open("admin_tutors"));
        boolean self = t.id.equals(classroom.me());
        if (account != null) dialogAction(content, holder, "Xem tài khoản", Ui.OUTLINE, () -> user(account.id));
        if (account != null && !self && !account.admin()) {
            if (account.banned()) dialogAction(content, holder, "Mở khóa tài khoản", Ui.OUTLINE, () ->
                    askReason("Mở khóa " + t.name, "Gia sư đăng nhập và nhận lớp lại được.", "Mở khóa", false,
                            reason -> state().status(account.id, false, reason)));
            else dialogAction(content, holder, "Khóa tài khoản gia sư", Ui.DANGER, () ->
                    askReason("Khóa " + t.name, "Gia sư bị đăng xuất, không nhắn tin, nhận lớp hay dạy được cho tới khi mở khóa.", "Khóa", true,
                            reason -> state().status(account.id, true, reason)));
        }
        holder[0] = detail(t.name.isEmpty() ? "Gia sư" : t.name, content);
    }

    private void confirmVisible(ManageState.Tutor t, boolean visible) {
        ui.dialog().setTitle(visible ? "Hiện lại gia sư?" : "Ẩn gia sư?")
                .setMessage(visible ? t.name + " sẽ xuất hiện lại trong Tìm gia sư." :
                        t.name + " sẽ không xuất hiện trong Tìm gia sư; các lớp và lịch học hiện có vẫn giữ nguyên.")
                .setNegativeButton("Hủy", null)
                .setPositiveButton(visible ? "Hiện" : "Ẩn", (d, w) -> report(manage().setTutorVisible(t, visible)))
                .show();
    }

    private static String mode(String mode) {
        switch (mode) {
            case "TRUC_TUYEN": return "Trực tuyến";
            case "TAI_NHA": return "Tại nhà";
            case "CA_HAI": return "Cả hai";
            default: return mode.isEmpty() ? "—" : mode;
        }
    }
    private static String join(String a, String b) {
        if (a.isEmpty()) return b; if (b.isEmpty()) return a; return a + " · " + b;
    }
}
