package com.example.tmdt.ui.exchange;

import android.os.Bundle;
import android.text.InputFilter;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import com.example.tmdt.LearnerState;
import com.example.tmdt.MainActivity;
import com.example.tmdt.R;
import com.example.tmdt.ui.common.ScreenFragment;
import com.example.tmdt.ui.common.Ui;

/**
 * Người học của tôi: một tài khoản (ví dụ phụ huynh) thêm nhiều người học rồi chọn người học khi đặt lịch 1-1
 * hoặc đăng ký lớp nhóm. Thêm, sửa, xóa người học.
 */
public final class LearnersFragment extends ScreenFragment {
    private LearnerState learners() { return ((MainActivity) requireActivity()).exchange.learners; }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        LearnerState learners = learners();
        learners.changes().observe(getViewLifecycleOwner(), ignored -> {
            String notice = learners.takeNotice();
            if (!notice.isEmpty()) classroom.notice(notice);
            refresh();
        });
        learners.start();
    }

    @Override protected View build() {
        LearnerState state = learners();
        LinearLayout root = ui.column(); ui.header(root, "Người học của tôi", classroom::back, null);
        LinearLayout body = ui.page(root);
        ui.add(body, ui.note(R.drawable.ic_info, "Thêm con hoặc người thân để đặt lịch học và đăng ký lớp nhóm cho từng người bằng tài khoản này. "
                + "Khi đặt lịch, chọn người học ở mục \"Người học\".", Ui.PALE, Ui.BLUE, Ui.INK));
        ui.space(body, 14);
        if (state.loading() && state.learners().isEmpty()) {
            ui.add(body, ui.text("Đang tải…", 15, Ui.MUTED, false));
        } else if (!state.loadError().isEmpty()) {
            LinearLayout note = ui.note(R.drawable.ic_info, state.loadError() + " Chạm để thử lại.", Ui.ORANGE_BG, Ui.ORANGE, Ui.INK);
            ui.clickable(note, state::retry); ui.add(body, note);
        } else if (state.learners().isEmpty()) {
            ui.add(body, ui.text("Chưa có người học nào. Khi đặt lịch, mặc định học cho chính bạn.", 15, Ui.MUTED, false));
        }
        for (LearnerState.Learner l : state.learners()) {
            LinearLayout card = ui.card(); LinearLayout row = ui.row(); row.setGravity(Gravity.CENTER_VERTICAL);
            row.addView(ui.avatar(l.id, l.name, 44)); ui.gap(row, 12);
            LinearLayout info = ui.column(); ui.add(info, ui.text(l.name, 17, Ui.INK, true));
            String summary = l.summary();
            if (!summary.isEmpty()) { ui.space(info, 3); ui.add(info, ui.text(summary, 13, Ui.MUTED, false)); }
            if (!l.note.isEmpty()) { ui.space(info, 3); ui.add(info, ui.text(l.note, 13, Ui.MUTED, false)); }
            ui.weight(row, info); ui.gap(row, 6); row.addView(ui.text("›", 22, Ui.MUTED, false));
            ui.add(card, row); card.setContentDescription("Sửa " + l.name); ui.clickable(card, () -> edit(l));
            ui.add(body, card); ui.space(body, 10);
        }
        ui.space(body, 6);
        boolean full = state.learners().size() >= LearnerState.MAX_LEARNERS;
        ui.addAction(body, ui.action(full ? "Đã đủ " + LearnerState.MAX_LEARNERS + " người học" : "Thêm người học", 0,
                full || state.busy() ? Ui.DISABLED : Ui.PRIMARY, full ? null : () -> edit(null)), 48);
        return root;
    }

    private EditText field(LinearLayout form, String label, String value, int max) {
        ui.add(form, ui.text(label, 13, Ui.MUTED, true)); ui.space(form, 4);
        EditText input = ui.input(label); input.setText(value);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(max)});
        ui.add(form, input); ui.space(form, 10);
        return input;
    }

    /** Thêm (learner == null) hoặc sửa / xóa một người học. */
    private void edit(@Nullable LearnerState.Learner learner) {
        LinearLayout form = ui.column(); form.setPadding(ui.dp(22), ui.dp(6), ui.dp(22), 0);
        EditText name = field(form, "Họ và tên *", learner == null ? "" : learner.name, LearnerState.MAX_NAME);
        EditText relation = field(form, "Quan hệ (con, em, cháu…)", learner == null ? "" : learner.relation, LearnerState.MAX_SHORT);
        EditText grade = field(form, "Lớp (ví dụ: Lớp 5)", learner == null ? "" : learner.grade, LearnerState.MAX_SHORT);
        EditText level = field(form, "Cấp học (Tiểu học, THCS, THPT…)", learner == null ? "" : learner.level, LearnerState.MAX_SHORT);
        EditText note = field(form, "Ghi chú cho gia sư", learner == null ? "" : learner.note, LearnerState.MAX_NOTE);
        android.widget.ScrollView scroll = new android.widget.ScrollView(requireContext()); scroll.addView(form);
        AlertDialog.Builder builder = ui.dialog().setTitle(learner == null ? "Thêm người học" : "Sửa người học").setView(scroll)
                .setNegativeButton("Hủy", null).setPositiveButton("Lưu", null);
        if (learner != null) builder.setNeutralButton("Xóa", null);
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(shown -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String error = learners().save(learner == null ? "" : learner.id, text(name), text(grade), text(level), text(relation), text(note));
                if (error != null) { name.setError(error); return; }
                dialog.dismiss(); classroom.hideKeyboard();
            });
            if (learner != null) {
                TextView delete = dialog.getButton(AlertDialog.BUTTON_NEUTRAL); delete.setTextColor(Ui.RED);
                delete.setOnClickListener(v -> {
                    dialog.dismiss();
                    ui.dialog().setTitle("Xóa " + learner.name + "?")
                            .setMessage("Lịch học và đăng ký đã gửi vẫn giữ tên người học này.")
                            .setNegativeButton("Hủy", null)
                            .setPositiveButton("Xóa", (d, w) -> report(learners().delete(learner))).show();
                });
            }
        });
        dialog.show();
    }
    private static String text(EditText input) { return input.getText() == null ? "" : input.getText().toString(); }
    private void report(String error) { if (error != null) classroom.dialog("Chưa thực hiện được", error); }
}
