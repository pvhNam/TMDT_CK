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
import com.example.tmdt.Contract;
import com.example.tmdt.Exchange;
import com.example.tmdt.MainActivity;
import com.example.tmdt.R;
import com.example.tmdt.Report;
import com.example.tmdt.ReportState;
import com.example.tmdt.databinding.FragmentHandleReportBinding;
import com.example.tmdt.databinding.ItemEvidenceFileBinding;
import com.example.tmdt.ui.exchange.ExUi;
import java.util.List;

/**
 * Màn 50 – Xử lý báo cáo (UC29). Xem phản ánh và minh chứng, chọn hướng xử lý (yêu cầu bổ sung, cảnh cáo,
 * khóa tài khoản, hoàn tiền, bác bỏ), ghi kết luận và xác nhận. Mọi quyết định được ghi vào admin_logs.
 */
public final class HandleReportFragment extends Fragment {
    private static final String[] OUTCOMES = {Report.OUTCOME_NEED_INFO, Report.OUTCOME_WARN, Report.OUTCOME_BAN, Report.OUTCOME_REFUND, Report.OUTCOME_DISMISS};
    private FragmentHandleReportBinding binding;
    private Exchange exchange;
    private AdminState admin;
    private ReportState evidence;
    private String outcome;
    private boolean markedReviewing;

    @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        binding = FragmentHandleReportBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        exchange = ((MainActivity) requireActivity()).exchange;
        admin = exchange.admin; evidence = exchange.reports;
        if (state != null) outcome = state.getString("outcome");
        ExUi.header(binding.header, getString(R.string.ex_handle_title), getString(R.string.ex_admin_subtitle), exchange::back);
        ExUi.counter(binding.conclusion, binding.counter, 500);
        binding.outcome.setOnClickListener(v -> {
            String[] labels = new String[OUTCOMES.length];
            for (int i = 0; i < labels.length; i++) labels[i] = Report.outcomeLabel(OUTCOMES[i]);
            new AlertDialog.Builder(requireContext()).setTitle(R.string.ex_outcome)
                    .setItems(labels, (d, which) -> { outcome = OUTCOMES[which]; render(); }).show();
        });
        binding.submit.setOnClickListener(v -> confirm());
        admin.changes().observe(getViewLifecycleOwner(), ignored -> render());
        evidence.changes().observe(getViewLifecycleOwner(), ignored -> render());
        admin.startReports();
        render();
    }

    private Report report() { return admin.report(exchange.adminReportId()); }

    private void render() {
        if (binding == null) return;
        String notice = admin.takeNotice();
        if (!notice.isEmpty()) ExUi.snack(binding.getRoot(), notice);
        Report r = report();
        if (r == null) {
            binding.content.setVisibility(View.GONE); binding.submit.setVisibility(View.GONE);
            binding.state.setVisibility(View.VISIBLE);
            binding.state.setText(admin.reportsLoading() ? "Đang tải báo cáo…" : admin.reportsError().isEmpty() ? "Không tìm thấy báo cáo." : admin.reportsError());
            return;
        }
        binding.state.setVisibility(View.GONE); binding.content.setVisibility(View.VISIBLE);
        if (!markedReviewing) { markedReviewing = true; admin.startReviewing(r); }
        if (Report.SUBJECT_CONTRACT.equals(r.subjectType)) admin.loadContract(r.subjectId);

        binding.code.setText(r.code);
        binding.reason.setText(r.reason);
        binding.date.setText("Ngày báo cáo: " + ExUi.dateTime(r.createdAt));
        ExUi.pill(binding.status, r.statusLabel(), ExUi.kind(r));
        ExUi.avatar(binding.reporterAvatar.avatarPhoto, binding.reporterAvatar.avatarInitials, null, r.reporterName);
        binding.reporterName.setText(r.reporterName);
        binding.reporterRole.setText(Report.roleLabel(r.reporterRole));
        ExUi.avatar(binding.targetAvatar.avatarPhoto, binding.targetAvatar.avatarInitials, null, r.targetName);
        binding.targetName.setText(r.targetName);
        binding.targetRole.setText(Report.roleLabel(r.targetRole) + " · " + r.subjectLabel);
        binding.description.setText(r.description);

        // Minh chứng
        binding.evidence.removeAllViews();
        List<Report.Evidence> files = evidence.evidence(r);
        binding.evidenceEmpty.setVisibility(files == null || files.isEmpty() ? View.VISIBLE : View.GONE);
        binding.evidenceEmpty.setText(files == null ? "Đang tải minh chứng…" : "Không có minh chứng đính kèm.");
        if (files != null) for (Report.Evidence e : files) {
            ItemEvidenceFileBinding row = ItemEvidenceFileBinding.inflate(getLayoutInflater(), binding.evidence, false);
            android.graphics.Bitmap thumb = ExUi.decode(e.data);
            if (thumb != null) row.thumb.setImageBitmap(thumb);
            row.name.setText(e.name);
            row.size.setText(ExUi.size(e.size) + " · " + ExUi.dateTime(e.createdAt));
            row.view.setVisibility(View.VISIBLE);
            row.getRoot().setOnClickListener(v -> ExUi.showImage(requireContext(), e.name, e.data));
            binding.evidence.addView(row.getRoot());
        }

        // Đã xử lý: chỉ xem kết quả
        boolean finished = r.finished();
        binding.decision.setVisibility(finished ? View.VISIBLE : View.GONE);
        binding.form.setVisibility(finished ? View.GONE : View.VISIBLE);
        if (finished) {
            String text = Report.outcomeLabel(r.outcome) + "\n" + (r.conclusion.isEmpty() ? "" : r.conclusion + "\n")
                    + (r.refundAmount > 0 ? "Số tiền hoàn: " + ExUi.money(r.refundAmount) + "\n" : "")
                    + "Xác nhận lúc " + ExUi.dateTime(r.decidedAt);
            binding.decisionText.setText(text);
            binding.submit.setVisibility(View.GONE);
            return;
        }
        binding.submit.setVisibility(View.VISIBLE);
        binding.outcome.setText(outcome == null ? "" : Report.outcomeLabel(outcome));
        boolean refund = Report.OUTCOME_REFUND.equals(outcome);
        Contract contract = admin.contract(r.subjectId);
        binding.refundGroup.setVisibility(refund ? View.VISIBLE : View.GONE);
        binding.refundHint.setVisibility(refund ? View.VISIBLE : View.GONE);
        if (refund) {
            if (contract != null && binding.refund.length() == 0) binding.refund.setText(String.valueOf(contract.fee));
            binding.refundHint.setText(!Report.SUBJECT_CONTRACT.equals(r.subjectType) ? "Chỉ hoàn tiền được cho báo cáo gắn với hợp đồng."
                    : contract == null ? "Đang tải hợp đồng…" : "Tối đa " + ExUi.money(contract.total()) + " (tổng hợp đồng " + contract.code + ").");
        }
        boolean needInfo = Report.OUTCOME_NEED_INFO.equals(outcome);
        binding.conclusionTitle.setText(needInfo ? R.string.ex_request_info : R.string.ex_conclusion);
        binding.submit.setEnabled(outcome != null && !admin.busy());
    }

    private void confirm() {
        Report r = report();
        if (r == null || outcome == null) return;
        String conclusion = ExUi.text(binding.conclusion);
        if (conclusion.length() < 5) { ExUi.snack(binding.getRoot(), "Nhập kết luận hoặc nội dung cần bổ sung (ít nhất 5 ký tự)."); binding.conclusion.requestFocus(); return; }
        long refund = 0;
        if (Report.OUTCOME_REFUND.equals(outcome)) {
            Contract contract = admin.contract(r.subjectId);
            if (!Report.SUBJECT_CONTRACT.equals(r.subjectType) || contract == null) { ExUi.snack(binding.getRoot(), "Báo cáo này không gắn với hợp đồng nên không thể hoàn tiền."); return; }
            try { refund = Long.parseLong(ExUi.text(binding.refund)); } catch (NumberFormatException e) { refund = 0; }
            if (refund <= 0 || refund > contract.total()) { ExUi.snack(binding.getRoot(), "Số tiền hoàn từ 1đ đến " + ExUi.money(contract.total()) + "."); return; }
        }
        final long amount = refund;
        String message = Report.outcomeLabel(outcome) + (Report.OUTCOME_BAN.equals(outcome) ? "\n\nTài khoản " + r.targetName + " sẽ bị khóa ngay." : "")
                + (amount > 0 ? "\n\nSố tiền hoàn: " + ExUi.money(amount) : "");
        ((MainActivity) requireActivity()).hideKeyboard();
        new AlertDialog.Builder(requireContext()).setTitle("Xác nhận xử lý " + r.code + "?").setMessage(message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ex_confirm_handle, (d, w) -> admin.decide(r, outcome, conclusion, amount)).show();
    }

    @Override public void onSaveInstanceState(@NonNull Bundle out) { super.onSaveInstanceState(out); out.putString("outcome", outcome); }
    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}
