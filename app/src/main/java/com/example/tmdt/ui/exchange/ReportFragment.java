package com.example.tmdt.ui.exchange;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.example.tmdt.ChatState;
import com.example.tmdt.Contract;
import com.example.tmdt.Conversation;
import com.example.tmdt.Exchange;
import com.example.tmdt.MainActivity;
import com.example.tmdt.R;
import com.example.tmdt.Report;
import com.example.tmdt.ReportState;
import com.example.tmdt.Review;
import com.example.tmdt.databinding.FragmentReportBinding;
import com.example.tmdt.databinding.ItemEvidenceFileBinding;
import java.util.ArrayList;
import java.util.List;

/**
 * Màn 47 – Gửi báo cáo vi phạm (UC27). Đối tượng là một hợp đồng hoặc cuộc trò chuyện của chính người dùng,
 * nên người liên quan luôn là bên kia (Firestore Rules kiểm tra lại). Minh chứng là ảnh, tối đa 5 ảnh.
 */
public final class ReportFragment extends Fragment {
    private final EvidencePicker picker = new EvidencePicker(this);
    private FragmentReportBinding binding;
    private Exchange exchange;
    private ReportState reports;

    @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        binding = FragmentReportBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        MainActivity host = (MainActivity) requireActivity();
        exchange = host.exchange;
        reports = exchange.reports;
        ExUi.header(binding.header, getString(R.string.ex_report_title), null, exchange::back);
        if (reports.draftSubject == null && exchange.reportType() != null && exchange.reportSubject() != null)
            reports.draftSubject = exchange.reportType() + ":" + exchange.reportSubject();

        binding.description.setText(reports.draftDescription);
        ExUi.counter(binding.description, binding.counter, Report.MAX_DESCRIPTION);
        ExUi.afterChange(binding.description, value -> reports.draftDescription = value);
        binding.confirm.setChecked(reports.draftConfirmed);
        binding.confirm.setOnCheckedChangeListener((b, checked) -> { reports.draftConfirmed = checked; render(); });
        binding.subject.setOnClickListener(v -> chooseSubject());
        binding.reason.setOnClickListener(v -> new AlertDialog.Builder(requireContext()).setTitle(R.string.ex_report_reason)
                .setItems(Report.REASONS, (d, which) -> { reports.draftReason = Report.REASONS[which]; render(); }).show());
        binding.addEvidence.setOnClickListener(v -> {
            if (reports.draftFiles.size() >= Report.MAX_EVIDENCE) { ExUi.snack(binding.getRoot(), "Tối đa " + Report.MAX_EVIDENCE + " ảnh minh chứng."); return; }
            picker.pick();
        });
        picker.onResult((files, failed) -> {
            int room = Report.MAX_EVIDENCE - reports.draftFiles.size();
            for (int i = 0; i < files.size() && i < room; i++) reports.draftFiles.add(files.get(i));
            if (failed > 0 || files.size() > room) ExUi.snack(binding.getRoot(), failed > 0 ? "Có " + failed + " ảnh không đọc được hoặc quá lớn." : "Chỉ giữ " + Report.MAX_EVIDENCE + " ảnh đầu tiên.");
            render();
        }, working -> { if (binding != null) binding.processing.setVisibility(working ? View.VISIBLE : View.GONE); });
        binding.submit.setOnClickListener(v -> submit());

        reports.changes().observe(getViewLifecycleOwner(), ignored -> render());
        exchange.contracts.changes().observe(getViewLifecycleOwner(), ignored -> render());
        host.chat.changes().observe(getViewLifecycleOwner(), ignored -> render());
        exchange.contracts.start(); host.chat.start();
        render();
    }

    /** Hợp đồng (trừ đề nghị đã hủy) và cuộc trò chuyện của người dùng. */
    private List<ReportState.Subject> subjects() {
        String me = reports.myId();
        List<ReportState.Subject> list = new ArrayList<>();
        for (Contract c : exchange.contracts.contracts()) {
            if (Contract.CANCELLED.equals(c.status)) continue;
            String other = c.other(me);
            list.add(new ReportState.Subject(Report.SUBJECT_CONTRACT, c.id, "Hợp đồng " + c.code + " · " + c.subject,
                    other, c.otherName(me), other.equals(c.tutorId) ? Review.TUTOR : Review.STUDENT));
        }
        ChatState chat = ((MainActivity) requireActivity()).chat;
        for (Conversation c : chat.conversations())
            list.add(new ReportState.Subject(Report.SUBJECT_CONVERSATION, c.id, "Trò chuyện với " + c.peerName,
                    c.peerId, c.peerName, Review.TUTOR.equals(c.peerRole) ? Review.TUTOR : Review.STUDENT));
        return list;
    }
    private ReportState.Subject selected() {
        if (reports.draftSubject == null) return null;
        for (ReportState.Subject s : subjects()) if ((s.type + ":" + s.id).equals(reports.draftSubject)) return s;
        return null;
    }
    private void chooseSubject() {
        List<ReportState.Subject> list = subjects();
        if (list.isEmpty()) { exchange.dialog("Chưa có đối tượng", "Bạn chỉ có thể báo cáo người đã trao đổi tin nhắn hoặc lập hợp đồng với bạn."); return; }
        String[] labels = new String[list.size()];
        for (int i = 0; i < labels.length; i++) labels[i] = list.get(i).label;
        new AlertDialog.Builder(requireContext()).setTitle(R.string.ex_report_subject)
                .setItems(labels, (d, which) -> { ReportState.Subject s = list.get(which); reports.draftSubject = s.type + ":" + s.id; render(); }).show();
    }

    private void render() {
        if (binding == null) return;
        String notice = reports.takeNotice();
        if (!notice.isEmpty()) ExUi.snack(binding.getRoot(), notice);
        if (reports.takeSent() != null) { exchange.replaceWith(Exchange.REPORTS); return; }

        ReportState.Subject subject = selected();
        binding.subject.setText(subject == null ? "" : subject.label);
        binding.personTitle.setVisibility(subject == null ? View.GONE : View.VISIBLE);
        binding.person.setVisibility(subject == null ? View.GONE : View.VISIBLE);
        if (subject != null) {
            ExUi.avatar(binding.avatar.avatarPhoto, binding.avatar.avatarInitials, null, subject.targetName);
            binding.personName.setText(subject.targetName);
            binding.personInfo.setText(Report.roleLabel(subject.targetRole) + " · " + subject.label);
        }
        binding.reason.setText(reports.draftReason == null ? "" : reports.draftReason);

        binding.files.removeAllViews();
        for (ReportState.Attachment file : new ArrayList<>(reports.draftFiles)) {
            ItemEvidenceFileBinding row = ItemEvidenceFileBinding.inflate(getLayoutInflater(), binding.files, false);
            android.graphics.Bitmap thumb = ExUi.decode(file.data);
            if (thumb != null) row.thumb.setImageBitmap(thumb);
            row.name.setText(file.name); row.size.setText(ExUi.size(file.size));
            row.remove.setVisibility(View.VISIBLE);
            row.remove.setContentDescription(getString(R.string.ex_remove) + " " + file.name);
            row.remove.setOnClickListener(v -> { reports.draftFiles.remove(file); render(); });
            row.getRoot().setOnClickListener(v -> ExUi.showImage(requireContext(), file.name, file.data));
            binding.files.addView(row.getRoot());
        }
        binding.submit.setEnabled(subject != null && reports.draftReason != null && binding.confirm.isChecked() && !reports.busy());
        binding.submit.setText(reports.busy() ? "Đang gửi…" : getString(R.string.ex_send_report));
    }

    private void submit() {
        ReportState.Subject subject = selected();
        if (subject == null || reports.draftReason == null) return;
        if (ExUi.text(binding.description).length() < 10) {
            ExUi.snack(binding.getRoot(), "Mô tả chi tiết ít nhất 10 ký tự."); binding.description.requestFocus(); return;
        }
        ((MainActivity) requireActivity()).hideKeyboard();
        reports.submit(subject, exchange.myName(), exchange.myRole(), reports.draftReason, ExUi.text(binding.description),
                new ArrayList<>(reports.draftFiles));
    }

    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}
