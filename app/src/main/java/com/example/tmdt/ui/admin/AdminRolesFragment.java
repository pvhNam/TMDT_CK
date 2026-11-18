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
import java.util.List;

/**
 * Phân quyền quản trị (Thành viên 4): danh sách quản trị viên hiện tại, tìm tài khoản để cấp quyền quản trị
 * hoặc thu hồi. Bắt buộc nhập lý do; mỗi lần đổi quyền ghi admin_audit_logs (GRANT_ADMIN / REVOKE_ADMIN).
 * Không tự đổi quyền của chính mình, không cấp quyền cho tài khoản đang bị khóa.
 */
public final class AdminRolesFragment extends ManageScreen {
    private static final String[] TABS = {"Quản trị viên", "Cấp quyền mới"};
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
        LinearLayout root = root("Phân quyền quản trị");
        ui.tabs(root, TABS, tab, index -> { tab = index; refresh(); });
        LinearLayout body = ui.page(root);
        if (!feedback(body)) return root;
        if (waiting(body, ManageState.USERS)) return root;
        ui.add(body, ui.note(R.drawable.ic_info, tab == 0
                ? "Quản trị viên xem và quản lý được toàn bộ tài khoản, lớp học, hợp đồng, tài chính. Thu hồi quyền khi không còn cần."
                : "Tìm tài khoản đang hoạt động để cấp quyền quản trị. Tài khoản bị khóa phải được mở khóa trước.",
                Ui.PALE, Ui.BLUE, Ui.INK));
        ui.space(body, 12);
        if (tab == 1) search(body, "Tìm theo tên, email, số điện thoại", query, text -> { query = text; fill(); });
        list = ui.column(); ui.add(body, list);
        fill();
        return root;
    }

    private void fill() {
        if (list == null) return;
        list.removeAllViews();
        String me = classroom.me();
        List<ManageState.User> shown = new ArrayList<>();
        for (ManageState.User u : manage().users()) {
            if (tab == 0 ? u.admin() : !u.admin() && matches(query, u.name, u.email, u.phone)) shown.add(u);
        }
        ui.add(list, ui.text(shown.size() + (tab == 0 ? " quản trị viên" : " tài khoản"), 13, Ui.MUTED, false)); ui.space(list, 8);
        if (shown.isEmpty()) { ui.add(list, ui.text(tab == 0 ? "Chưa có quản trị viên." : "Không có tài khoản phù hợp.", 14, Ui.MUTED, false)); return; }
        for (ManageState.User u : shown) {
            List<TextView> pills = new ArrayList<>();
            pills.add(pill(u.roleLabel(), u.admin() ? "ADMIN" : ""));
            if (u.banned()) pills.add(pill("Đã khóa", "BANNED"));
            if (u.id.equals(me)) pills.add(pill("Bạn", "ADMIN"));
            ui.add(list, row(u.id, u.name, u.email, () -> open(u), pills.toArray(new TextView[0])));
            ui.space(list, 10);
        }
    }

    private void open(ManageState.User u) {
        ManageState admin = manage();
        boolean self = u.id.equals(classroom.me());
        LinearLayout content = ui.column();
        LinearLayout table = ui.table();
        ui.tableRow(table, R.drawable.ic_field_email, "Email", u.email.isEmpty() ? "—" : u.email);
        ui.tableRow(table, R.drawable.ic_field_phone, "Điện thoại", u.phone.isEmpty() ? "—" : u.phone);
        ui.tableRow(table, R.drawable.ic_field_shield, "Vai trò", u.roleLabel());
        ui.tableRow(table, R.drawable.ic_info, "Trạng thái", u.banned() ? "Đã khóa" : "Hoạt động");
        ui.tableRow(table, R.drawable.ic_calendar, "Ngày tạo", date(u.createdAt));
        ui.add(content, table);
        AlertDialog[] holder = new AlertDialog[1];
        if (self) {
            ui.space(content, 10);
            ui.add(content, ui.text("Đây là tài khoản của bạn: không thể tự thu hồi quyền.", 14, Ui.MUTED, false));
        } else if (u.admin()) {
            dialogAction(content, holder, "Thu hồi quyền quản trị", Ui.DANGER, () ->
                    askReason("Thu hồi quyền của " + label(u), "Tài khoản trở về vai trò người dùng thường.", "Thu hồi", true,
                            reason -> report(admin.setAdmin(u, false, reason))));
        } else if (u.banned()) {
            dialogAction(content, holder, "Cấp quyền quản trị (mở khóa trước)", Ui.DISABLED, null);
        } else {
            dialogAction(content, holder, "Cấp quyền quản trị", Ui.PRIMARY, () ->
                    askReason("Cấp quyền cho " + label(u), "Tài khoản sẽ vào trang quản trị và quản lý được toàn hệ thống.", "Cấp quyền", false,
                            reason -> report(admin.setAdmin(u, true, reason))));
        }
        dialogAction(content, holder, "Xem tài khoản", Ui.OUTLINE, () -> user(u.id));
        holder[0] = detail(label(u), content);
    }
    private static String label(ManageState.User u) { return u.name.isEmpty() ? u.email : u.name; }
}
