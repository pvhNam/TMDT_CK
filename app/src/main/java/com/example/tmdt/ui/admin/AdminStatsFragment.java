package com.example.tmdt.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.example.tmdt.AdminState;
import com.example.tmdt.Exchange;
import com.example.tmdt.MainActivity;
import com.example.tmdt.R;
import com.example.tmdt.databinding.FragmentAdminStatsBinding;
import com.example.tmdt.databinding.ViewExStatRowBinding;
import com.example.tmdt.databinding.ViewExStatTileBinding;
import com.example.tmdt.ui.exchange.ExUi;
import java.time.YearMonth;
import java.util.Locale;

/**
 * Màn 52 – Thống kê hệ thống (UC31): người dùng, gia sư, lớp đang hoạt động (hợp đồng đã chốt), hồ sơ chờ duyệt,
 * hoạt động trong tháng đã chọn và các việc chờ xử lý. Số liệu đếm trực tiếp trên Firestore.
 */
public final class AdminStatsFragment extends Fragment {
    private FragmentAdminStatsBinding binding;
    private Exchange exchange;
    private AdminState admin;

    @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        binding = FragmentAdminStatsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        exchange = ((MainActivity) requireActivity()).exchange;
        admin = exchange.admin;
        ExUi.header(binding.header, getString(R.string.ex_stats_title), getString(R.string.ex_admin_subtitle), exchange::back);
        tile(binding.tileUsers, R.drawable.ic_ex_users, R.string.ex_stat_users);
        tile(binding.tileTutors, R.drawable.ic_ex_cap, R.string.ex_stat_tutors);
        tile(binding.tileContracts, R.drawable.ic_ex_book_filled, R.string.ex_stat_contracts);
        tile(binding.tileProfiles, R.drawable.ic_ex_doc_filled, R.string.ex_stat_profiles);
        row(binding.rowContracts, R.drawable.ic_ex_check_circle, R.string.ex_month_contracts, null);
        row(binding.rowUsers, R.drawable.ic_ex_person_plus, R.string.ex_month_users, null);
        row(binding.rowReports, R.drawable.ic_ex_warning, R.string.ex_month_reports, null);
        row(binding.todoProfiles, R.drawable.ic_ex_doc, R.string.ex_todo_profiles, () -> exchange.dialog(getString(R.string.ex_todo_profiles),
                "Duyệt hồ sơ gia sư là UC28 (màn 49) do Thành viên 1 phụ trách. Số liệu ở đây đếm các hồ sơ đang ở trạng thái PENDING."));
        row(binding.todoReports, R.drawable.ic_ex_warning, R.string.ex_todo_reports, exchange::openAdminReports);
        row(binding.todoUsers, R.drawable.ic_ex_users, R.string.ex_menu_users, exchange::openAdminUsers);
        binding.month.setOnClickListener(v -> chooseMonth());
        binding.refresh.setOnClickListener(v -> admin.loadStats(admin.statsMonth()));
        admin.changes().observe(getViewLifecycleOwner(), ignored -> render());
        if (state == null || admin.stat(AdminState.USERS) < 0) admin.loadStats(admin.statsMonth());
        render();
    }

    private void tile(ViewExStatTileBinding t, int icon, int label) { t.icon.setImageResource(icon); t.label.setText(label); }
    private void row(ViewExStatRowBinding r, int icon, int label, @Nullable Runnable open) {
        r.icon.setImageResource(icon); r.label.setText(label);
        r.chevron.setVisibility(open == null ? View.GONE : View.VISIBLE);
        r.getRoot().setClickable(open != null);
        if (open != null) r.getRoot().setOnClickListener(v -> open.run());
    }

    private static String month(YearMonth m) { return "Tháng " + m.getMonthValue() + ", " + m.getYear(); }
    private void chooseMonth() {
        YearMonth now = YearMonth.now();
        String[] labels = new String[12]; YearMonth[] values = new YearMonth[12];
        for (int i = 0; i < 12; i++) { values[i] = now.minusMonths(i); labels[i] = month(values[i]); }
        new AlertDialog.Builder(requireContext()).setTitle("Chọn tháng")
                .setItems(labels, (d, which) -> admin.loadStats(values[which])).show();
    }

    private String number(String key) {
        long value = admin.stat(key);
        return value < 0 ? (admin.statsLoading() ? "…" : "—") : String.format(Locale.forLanguageTag("vi-VN"), "%,d", value);
    }

    private void render() {
        if (binding == null) return;
        binding.month.setText(month(admin.statsMonth()));
        binding.realtime.setText(admin.statsLoading() ? "Đang cập nhật…" : getString(R.string.ex_realtime));
        binding.error.setVisibility(admin.statsError().isEmpty() ? View.GONE : View.VISIBLE);
        binding.error.setText(admin.statsError());
        binding.tileUsers.value.setText(number(AdminState.USERS));
        binding.tileTutors.value.setText(number(AdminState.TUTORS));
        binding.tileContracts.value.setText(number(AdminState.ACTIVE_CONTRACTS));
        binding.tileProfiles.value.setText(number(AdminState.PENDING_PROFILES));
        binding.monthTitle.setText(getString(R.string.ex_month_activity) + " · " + month(admin.statsMonth()));
        binding.rowContracts.value.setText(number(AdminState.MONTH_CONTRACTS));
        binding.rowUsers.value.setText(number(AdminState.MONTH_USERS));
        binding.rowReports.value.setText(number(AdminState.MONTH_REPORTS));
        badge(binding.todoProfiles, AdminState.PENDING_PROFILES, ExUi.BAD);
        badge(binding.todoReports, AdminState.OPEN_REPORTS, ExUi.BAD);
        binding.todoUsers.value.setText("");
        binding.refresh.setEnabled(!admin.statsLoading());
    }
    private void badge(ViewExStatRowBinding r, String key, int kind) {
        r.value.setText("");
        long value = admin.stat(key);
        if (value < 0) { r.badge.setVisibility(View.GONE); r.value.setText(number(key)); return; }
        ExUi.pill(r.badge, number(key), value > 0 ? kind : ExUi.NEUTRAL);
    }

    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}
