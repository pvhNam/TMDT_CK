package com.example.tmdt.ui.exchange;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.tmdt.Exchange;
import com.example.tmdt.MainActivity;
import com.example.tmdt.R;
import com.example.tmdt.Report;
import com.example.tmdt.ReportState;
import com.example.tmdt.databinding.FragmentListScreenBinding;
import com.example.tmdt.databinding.ItemEvidenceFileBinding;
import com.example.tmdt.databinding.ItemMyReportBinding;
import com.example.tmdt.databinding.ItemTimelineStepBinding;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Màn 48 – Báo cáo của tôi (hỗ trợ UC27): tab Đang xử lý / Đã giải quyết, tiến trình kiểm tra, bổ sung minh chứng. */
public final class MyReportsFragment extends Fragment {
    private final EvidencePicker picker = new EvidencePicker(this);
    private FragmentListScreenBinding binding;
    private Exchange exchange;
    private ReportState reports;
    private SimpleAdapter<Report, ItemMyReportBinding> adapter;
    private final Set<String> expanded = new HashSet<>();
    private int tab;
    /** Báo cáo đang được bổ sung minh chứng. */
    private String adding;

    @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        binding = FragmentListScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        exchange = ((MainActivity) requireActivity()).exchange;
        reports = exchange.reports;
        if (state != null) {
            tab = state.getInt("tab"); adding = state.getString("adding");
            ArrayList<String> open = state.getStringArrayList("expanded"); if (open != null) expanded.addAll(open);
        }
        ExUi.header(binding.header, getString(R.string.ex_my_reports_title), null, exchange::back);
        binding.header.action.setVisibility(View.VISIBLE);
        binding.header.action.setImageResource(R.drawable.ic_ex_plus);
        binding.header.action.setContentDescription(getString(R.string.ex_new_report));
        binding.header.action.setOnClickListener(v -> { reports.clearDraft(); exchange.openReport(null, null); });
        adapter = new SimpleAdapter<>(ItemMyReportBinding::inflate, this::bind);
        ListScreen.setup(binding, adapter);
        ListScreen.tabs(binding, tab, position -> { tab = position; render(); }, getString(R.string.ex_tab_processing), getString(R.string.ex_tab_resolved));
        picker.onResult((files, failed) -> {
            Report report = find(adding);
            if (report != null && !files.isEmpty()) reports.addEvidence(report, files);
            if (failed > 0) ExUi.snack(binding.getRoot(), "Có " + failed + " ảnh không đọc được hoặc quá lớn.");
        }, working -> {});
        reports.changes().observe(getViewLifecycleOwner(), ignored -> render());
        reports.start();
        render();
    }

    private Report find(String id) {
        if (id == null) return null;
        for (Report r : reports.reports(false)) if (r.id.equals(id)) return r;
        return null;
    }

    private void render() {
        if (binding == null) return;
        String notice = reports.takeNotice();
        if (!notice.isEmpty()) ExUi.snack(binding.getRoot(), notice);
        List<Report> items = reports.reports(tab == 1);
        if (reports.loading() && items.isEmpty()) { ListScreen.state(binding, "Đang tải báo cáo…", null, null); return; }
        if (!reports.loadError().isEmpty()) { ListScreen.state(binding, reports.loadError(), getString(R.string.chat_retry), reports::retry); return; }
        if (items.isEmpty()) {
            ListScreen.state(binding, tab == 0 ? "Bạn không có báo cáo nào đang xử lý." : "Chưa có báo cáo nào được giải quyết.",
                    getString(R.string.ex_menu_new_report), () -> { reports.clearDraft(); exchange.openReport(null, null); });
            return;
        }
        ListScreen.state(binding, null, null, null);
        adapter.submit(items);
    }

    private void bind(ItemMyReportBinding b, Report r) {
        boolean open = expanded.contains(r.id);
        b.code.setText(r.code);
        b.reason.setText(r.reason);
        b.date.setText(ExUi.date(r.createdAt));
        ExUi.pill(b.status, r.statusLabel(), ExUi.kind(r));
        b.chevron.setImageResource(open ? R.drawable.ic_ex_chevron_up : R.drawable.ic_ex_chevron_down);
        b.summary.setContentDescription(r.code + ", " + r.reason + ", " + r.statusLabel());
        b.summary.setOnClickListener(v -> { if (!expanded.remove(r.id)) expanded.add(r.id); render(); });
        b.details.setVisibility(open ? View.VISIBLE : View.GONE);
        if (!open) return;

        // Tiến trình xử lý
        b.timeline.removeAllViews();
        step(b.timeline, R.drawable.ic_ex_step_done, "Đã tiếp nhận", ExUi.dateTime(r.createdAt), "Báo cáo của bạn đã được tiếp nhận.", false);
        if (Report.NEED_INFO.equals(r.status))
            step(b.timeline, R.drawable.ic_ex_step_warn, "Cần bổ sung minh chứng", ExUi.dateTime(r.updatedAt),
                    r.adminNote.isEmpty() ? "Quản trị viên cần thêm thông tin. Hãy bổ sung minh chứng." : r.adminNote, false);
        else {
            boolean reviewing = r.reviewingAt > 0 || r.finished();
            step(b.timeline, r.finished() ? R.drawable.ic_ex_step_done : reviewing ? R.drawable.ic_ex_step_current : R.drawable.ic_ex_step_pending,
                    "Đang kiểm tra minh chứng", ExUi.dateTime(r.reviewingAt), reviewing ? "Quản trị viên đang kiểm tra thông tin và minh chứng." : "Chờ quản trị viên tiếp nhận.", false);
        }
        String result;
        if (r.finished()) {
            result = Report.outcomeLabel(r.outcome) + (r.conclusion.isEmpty() ? "" : ". " + r.conclusion);
            if (r.refundAmount > 0) result += "\nSố tiền hoàn: " + ExUi.money(r.refundAmount);
        } else result = "Chúng tôi sẽ thông báo cho bạn khi có kết quả.";
        step(b.timeline, r.finished() ? R.drawable.ic_ex_step_done : R.drawable.ic_ex_step_pending, "Kết quả xử lý", ExUi.dateTime(r.decidedAt), result, true);

        b.target.setText("Người bị báo cáo: " + r.targetName + " · " + r.subjectLabel);
        b.description.setText(r.description);

        // Minh chứng
        b.evidence.removeAllViews();
        List<Report.Evidence> files = reports.evidence(r);
        b.evidenceEmpty.setText(files == null ? "Đang tải minh chứng…" : files.isEmpty() ? "Chưa có minh chứng." : "");
        b.evidenceEmpty.setVisibility(files == null || files.isEmpty() ? View.VISIBLE : View.GONE);
        if (files != null) for (Report.Evidence e : files) {
            ItemEvidenceFileBinding row = ItemEvidenceFileBinding.inflate(getLayoutInflater(), b.evidence, false);
            android.graphics.Bitmap thumb = ExUi.decode(e.data);
            if (thumb != null) row.thumb.setImageBitmap(thumb);
            row.name.setText(e.name); row.size.setText(ExUi.size(e.size));
            row.getRoot().setOnClickListener(v -> ExUi.showImage(requireContext(), e.name, e.data));
            b.evidence.addView(row.getRoot());
        }
        boolean canAdd = !r.finished() && r.evidenceCount < Report.MAX_EVIDENCE;
        b.add.setVisibility(canAdd ? View.VISIBLE : View.GONE);
        b.add.setEnabled(!reports.busy());
        b.add.setOnClickListener(v -> { adding = r.id; picker.pick(); });
    }

    private void step(LinearLayout parent, int icon, String title, String time, String body, boolean last) {
        ItemTimelineStepBinding s = ItemTimelineStepBinding.inflate(getLayoutInflater(), parent, false);
        s.dot.setImageResource(icon);
        s.title.setText(title);
        s.time.setText(time); s.time.setVisibility(time.isEmpty() ? View.GONE : View.VISIBLE);
        s.body.setText(body);
        s.line.setVisibility(last ? View.INVISIBLE : View.VISIBLE);
        parent.addView(s.getRoot());
    }

    @Override public void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("tab", tab); out.putString("adding", adding); out.putStringArrayList("expanded", new ArrayList<>(expanded));
    }
    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}
