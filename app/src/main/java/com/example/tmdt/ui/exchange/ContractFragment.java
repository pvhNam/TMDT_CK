package com.example.tmdt.ui.exchange;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.example.tmdt.Contract;
import com.example.tmdt.ContractState;
import com.example.tmdt.Exchange;
import com.example.tmdt.MainActivity;
import com.example.tmdt.R;
import com.example.tmdt.databinding.FragmentContractBinding;

/**
 * Màn 40 – Chốt hợp đồng dạy (UC23).
 * Gia sư soạn hợp đồng cho buổi học đã chấp nhận: lịch, thời lượng, hình thức lấy theo buổi học; gia sư điền môn,
 * số buổi, học phí và thỏa thuận thêm; điều khoản chung là mẫu của quản trị viên. Học viên xem rồi
 * "Xác nhận hợp đồng" (→ chờ thanh toán) hoặc từ chối; gia sư được hủy khi học viên chưa xác nhận.
 * Hợp đồng đã chốt: học viên thanh toán toàn khóa bằng VNPay (luồng thanh toán của nhóm).

 */
public final class ContractFragment extends Fragment {
    private FragmentContractBinding binding;
    private Exchange exchange;
    private ContractState contracts;
    /** Bản nháp đang hiển thị trong các ô nhập (tránh ghi đè khi người dùng đang gõ). */
    private Contract filled;

    @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        binding = FragmentContractBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        exchange = ((MainActivity) requireActivity()).exchange;
        contracts = exchange.contracts;
        ExUi.header(binding.header, getString(R.string.ex_contract_title), null, exchange::back);
        bindEditor();
        binding.termsRow.setOnClickListener(v -> showTerms());
        binding.agree.setOnCheckedChangeListener((b, checked) -> render());
        contracts.changes().observe(getViewLifecycleOwner(), ignored -> render());
        exchange.agreements.changes().observe(getViewLifecycleOwner(), ignored -> render());
        contracts.start();
        render();
    }

    // ----------------------------------------------------------------------------------------------
    // Gia sư soạn hợp đồng
    // ----------------------------------------------------------------------------------------------

    private void bindEditor() {
        ExUi.afterChange(binding.subjectInput, value -> { Contract d = exchange.draft(); if (d != null) d.subject = value.trim(); });
        ExUi.afterChange(binding.feeInput, value -> {
            Contract d = exchange.draft(); if (d == null) return;
            try { d.fee = value.isEmpty() ? 0 : Long.parseLong(value); } catch (NumberFormatException e) { d.fee = 0; }
            renderCost(d);
        });
        binding.minus.setOnClickListener(v -> changeSessions(-1));
        binding.plus.setOnClickListener(v -> changeSessions(1));
    }
    private void changeSessions(int delta) {
        Contract d = exchange.draft(); if (d == null) return;
        d.sessions = Math.max(1, Math.min(Contract.MAX_SESSIONS, d.sessions + delta));
        binding.sessionsValue.setText(String.valueOf(d.sessions));
        renderCost(d);
    }

    // ----------------------------------------------------------------------------------------------
    // Hiển thị
    // ----------------------------------------------------------------------------------------------

    private void render() {
        if (binding == null) return;
        String created = contracts.takeCreated();
        if (created != null) exchange.showCreated(created);
        String notice = contracts.takeNotice();
        if (!notice.isEmpty()) ExUi.snack(binding.getRoot(), notice);

        Contract draft = exchange.draft();
        Contract contract = draft != null ? draft : contracts.contract(exchange.contractId());
        boolean editing = draft != null;
        if (contract == null) {
            binding.content.setVisibility(View.GONE); binding.actions.setVisibility(View.GONE);
            binding.state.setVisibility(View.VISIBLE);
            binding.state.setText(contracts.loading() ? "Đang tải hợp đồng…" : !contracts.loadError().isEmpty() ? contracts.loadError() : "Không tìm thấy hợp đồng.");
            return;
        }
        binding.state.setVisibility(View.GONE); binding.content.setVisibility(View.VISIBLE); binding.actions.setVisibility(View.VISIBLE);
        String me = contracts.myId();

        binding.code.setText(editing ? "Hợp đồng mới gửi " + contract.studentName : "Hợp đồng số " + contract.code);
        ExUi.pill(binding.status, editing ? "Bản nháp" : contract.statusLabel(), editing ? ExUi.NEUTRAL : ExUi.kind(contract));
        boolean showClosed = !editing && contract.closed();
        binding.closedNote.setVisibility(showClosed ? View.VISIBLE : View.GONE);
        if (showClosed) binding.closedNote.setText(Contract.REJECTED.equals(contract.status)
                ? "Học viên đã từ chối hợp đồng" + (contract.rejectReason.isEmpty() ? "." : ": " + contract.rejectReason)
                : "Gia sư đã hủy hợp đồng này.");

        // Các bên
        ExUi.avatar(binding.studentAvatar.avatarPhoto, binding.studentAvatar.avatarInitials, null, contract.studentName);
        ExUi.avatar(binding.tutorAvatar.avatarPhoto, binding.tutorAvatar.avatarInitials, null, contract.tutorName);
        binding.studentName.setText(contract.studentName);
        binding.tutorName.setText(contract.tutorName);

        // Lớp học: lịch, bắt đầu, hình thức luôn theo buổi học đã đặt
        binding.infoView.setVisibility(View.VISIBLE);
        binding.subjectView.setVisibility(editing ? View.GONE : View.VISIBLE);
        binding.sessionsView.setVisibility(editing ? View.GONE : View.VISIBLE);
        binding.subjectView.setText(contract.subject);
        binding.sessionsView.setText(contract.sessionsLabel());
        binding.scheduleView.setText("Lịch hằng tuần: " + contract.schedule);
        binding.startView.setText("Bắt đầu từ " + contract.startDate + (editing ? " (theo lịch học đã chấp nhận)" : ""));
        binding.modeView.setText(contract.mode + " · " + contract.minutes + " phút/buổi");
        binding.infoEdit.setVisibility(editing ? View.VISIBLE : View.GONE);
        if (editing) { if (filled != draft) fillEditor(draft); } else filled = null;
        renderCost(contract);
        renderActions(contract, editing, me);
    }

    private void fillEditor(Contract d) {
        filled = d;
        binding.subjectInput.setText(d.subject);
        binding.feeInput.setText(d.fee > 0 ? String.valueOf(d.fee) : "");
        binding.sessionsValue.setText(String.valueOf(d.sessions));
        renderCost(d);
    }
    private void renderCost(Contract c) {
        binding.feeValue.setText(ExUi.money(c.fee) + " " + getString(R.string.ex_per_session));
        binding.totalValue.setText(ExUi.money(c.total()));
    }

    private void renderActions(Contract c, boolean editing, String me) {
        boolean busy = contracts.busy();
        boolean tutor = me.equals(c.tutorId);
        boolean needsAgreement = editing || c.awaits(me);
        binding.agree.setVisibility(needsAgreement ? View.VISIBLE : View.GONE);
        boolean agreed = binding.agree.isChecked();
        binding.secondary.setVisibility(View.GONE); binding.tertiary.setVisibility(View.GONE);
        binding.primary.setIconResource(R.drawable.ic_ex_shield);

        if (editing) {
            binding.primary.setText("Gửi hợp đồng cho học viên");
            binding.primary.setEnabled(agreed && !busy);
            binding.primary.setOnClickListener(v -> propose(c));
        } else if (c.pending() && tutor) {
            binding.primary.setText("Chờ " + c.studentName + " xác nhận");
            binding.primary.setEnabled(false);
            binding.tertiary.setVisibility(View.VISIBLE); binding.tertiary.setText("Hủy hợp đồng");
            binding.tertiary.setEnabled(!busy);
            binding.tertiary.setOnClickListener(v -> new AlertDialog.Builder(requireContext()).setTitle("Hủy hợp đồng đã gửi?")
                    .setMessage("Học viên sẽ không xác nhận được hợp đồng này. Bạn có thể gửi hợp đồng mới sau khi hủy.").setNegativeButton(R.string.cancel, null)
                    .setPositiveButton("Hủy hợp đồng", (d, w) -> contracts.cancel(c)).show());
        } else if (c.pending()) {
            binding.primary.setText("Xác nhận hợp đồng");
            binding.primary.setEnabled(agreed && !busy);
            binding.primary.setOnClickListener(v -> contracts.confirm(c));
            binding.tertiary.setVisibility(View.VISIBLE); binding.tertiary.setText("Từ chối");
            binding.tertiary.setEnabled(!busy);
            binding.tertiary.setOnClickListener(v -> reject(c));
        } else if (c.agreed()) {
            boolean student = me.equals(c.studentId);
            binding.primary.setText(Contract.AWAITING_ESCROW.equals(c.status) ? (student ? "Thanh toán toàn bộ khóa qua VNPay" : "Xem thanh toán khóa học") : "Thanh toán và các buổi trong khóa");
            binding.primary.setEnabled(!busy);
            binding.primary.setOnClickListener(v -> ((MainActivity)requireActivity()).openPayment("CONTRACT",c.id,c.studentId));
            // Nút Đánh giá (UC26) và Báo cáo (UC27) sẽ thêm khi push các UC đó.
        } else if (tutor) {
            binding.primary.setText("Tạo hợp đồng mới");
            binding.primary.setIconResource(R.drawable.ic_ex_doc);
            binding.primary.setEnabled(!busy);
            binding.primary.setOnClickListener(v -> { binding.agree.setChecked(false); exchange.redraft(c); render(); });
        } else {
            binding.primary.setText(Contract.REJECTED.equals(c.status) ? "Bạn đã từ chối hợp đồng này" : "Hợp đồng đã bị hủy");
            binding.primary.setEnabled(false);
        }
    }

    private void propose(Contract d) {
        if (d.subject.length() < 2) { ExUi.snack(binding.getRoot(), "Nhập tên môn học."); binding.subjectInput.requestFocus(); return; }
        if (d.fee < Contract.MIN_FEE || d.fee > Contract.MAX_FEE) {
            ExUi.snack(binding.getRoot(), "Học phí mỗi buổi từ " + ExUi.money(Contract.MIN_FEE) + " đến " + ExUi.money(Contract.MAX_FEE) + ".");
            binding.feeInput.requestFocus(); return;
        }
        ((MainActivity) requireActivity()).hideKeyboard();
        contracts.propose(d);
    }

    private void reject(Contract c) {
        EditText input = new EditText(requireContext());
        input.setHint("Lý do (không bắt buộc)"); input.setSingleLine(false); input.setMaxLines(4);
        input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(300)});
        LinearLayout box = new LinearLayout(requireContext());
        int pad = ExUi.dp(requireContext(), 20); box.setPadding(pad, 0, pad, 0); box.addView(input, new LinearLayout.LayoutParams(-1, -2));
        new AlertDialog.Builder(requireContext()).setTitle("Từ chối hợp đồng").setView(box)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton("Từ chối", (d, w) -> contracts.reject(c, ExUi.text(input))).show();
    }

    /** Điều khoản chung (mẫu của quản trị viên, chỉ xem) và thỏa thuận thêm (gia sư nhập khi soạn). */
    private void showTerms() {
        Contract draft = exchange.draft();
        Contract c = draft != null ? draft : contracts.contract(exchange.contractId());
        if (c == null) return;
        String base = c.baseTerms.isEmpty() ? getString(R.string.ex_terms_base) : c.baseTerms;
        String version = c.templateVersion > 0 ? " (mẫu phiên bản " + c.templateVersion + ")" : "";
        if (draft == null) {
            new AlertDialog.Builder(requireContext()).setTitle(getString(R.string.ex_terms) + version)
                    .setMessage(base + (c.terms.isEmpty() ? "" : "\n\n" + getString(R.string.ex_terms_extra) + ":\n" + c.terms))
                    .setPositiveButton(R.string.close, null).show();
            return;
        }
        EditText input = new EditText(requireContext());
        input.setHint(R.string.ex_terms_extra_hint); input.setText(draft.terms); input.setMinLines(3);
        input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(1000)});
        LinearLayout box = new LinearLayout(requireContext()); box.setOrientation(LinearLayout.VERTICAL);
        int pad = ExUi.dp(requireContext(), 20); box.setPadding(pad, pad / 2, pad, 0);
        android.widget.TextView text = new android.widget.TextView(requireContext());
        text.setText(base + "\n\n" + getString(R.string.ex_terms_extra) + ":");
        text.setTextColor(requireContext().getColor(R.color.chat_ink));
        box.addView(text); box.addView(input, new LinearLayout.LayoutParams(-1, -2));
        android.widget.ScrollView scroll = new android.widget.ScrollView(requireContext()); scroll.addView(box);
        new AlertDialog.Builder(requireContext()).setTitle(getString(R.string.ex_terms) + version).setView(scroll)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.understood, (d, w) -> draft.terms = ExUi.text(input)).show();
    }

    @Override public void onDestroyView() { super.onDestroyView(); binding = null; filled = null; }
}
