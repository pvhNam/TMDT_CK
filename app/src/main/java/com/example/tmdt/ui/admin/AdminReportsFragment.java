package com.example.tmdt.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.tmdt.AdminState;
import com.example.tmdt.Exchange;
import com.example.tmdt.MainActivity;
import com.example.tmdt.R;
import com.example.tmdt.Report;
import com.example.tmdt.databinding.FragmentListScreenBinding;
import com.example.tmdt.databinding.ItemAdminReportBinding;
import com.example.tmdt.ui.exchange.ExUi;
import com.example.tmdt.ui.exchange.ListScreen;
import com.example.tmdt.ui.exchange.SimpleAdapter;
import java.util.List;

/** Danh sách báo cáo vi phạm cho quản trị viên (UC29): chờ xử lý / đã xử lý; chọn một báo cáo để mở màn 50. */
public final class AdminReportsFragment extends Fragment {
    private FragmentListScreenBinding binding;
    private AdminState admin;
    private SimpleAdapter<Report, ItemAdminReportBinding> adapter;
    private int tab;

    @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        binding = FragmentListScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        Exchange exchange = ((MainActivity) requireActivity()).exchange;
        admin = exchange.admin;
        if (state != null) tab = state.getInt("tab");
        ExUi.header(binding.header, getString(R.string.ex_admin_reports_title), getString(R.string.ex_admin_subtitle), exchange::back);
        adapter = new SimpleAdapter<>(ItemAdminReportBinding::inflate, (b, r) -> {
            b.code.setText(r.code);
            b.reason.setText(r.reason);
            b.parties.setText(r.reporterName + " → " + r.targetName);
            b.date.setText("Ngày báo cáo: " + ExUi.dateTime(r.createdAt));
            ExUi.pill(b.status, r.statusLabel(), ExUi.kind(r));
            b.getRoot().setOnClickListener(v -> exchange.openHandleReport(r));
        });
        ListScreen.setup(binding, adapter);
        ListScreen.tabs(binding, tab, position -> { tab = position; render(); }, getString(R.string.ex_tab_pending), getString(R.string.ex_tab_done));
        admin.changes().observe(getViewLifecycleOwner(), ignored -> render());
        admin.startReports();
        render();
    }

    private void render() {
        if (binding == null) return;
        List<Report> items = admin.reports(tab == 1);
        if (admin.reportsLoading() && items.isEmpty()) { ListScreen.state(binding, "Đang tải báo cáo…", null, null); return; }
        if (!admin.reportsError().isEmpty()) { ListScreen.state(binding, admin.reportsError(), getString(R.string.chat_retry), admin::retryReports); return; }
        if (items.isEmpty()) { ListScreen.state(binding, tab == 0 ? "Không có báo cáo nào chờ xử lý." : "Chưa có báo cáo đã xử lý.", null, null); return; }
        ListScreen.state(binding, null, null, null);
        adapter.submit(items);
    }

    @Override public void onSaveInstanceState(@NonNull Bundle out) { super.onSaveInstanceState(out); out.putInt("tab", tab); }
    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}
