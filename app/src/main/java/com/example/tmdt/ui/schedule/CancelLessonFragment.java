package com.example.tmdt.ui.schedule;

import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import com.example.tmdt.R;
import com.example.tmdt.ui.common.ScreenFragment;
import com.example.tmdt.ui.common.Ui;
import com.example.tmdt.Lesson;
import com.example.tmdt.Store;
import com.example.tmdt.Tutor;

/** Screen 54 · Hủy buổi học (branch of UC24). Refunds are only reviewed, never promised in full. */
public final class CancelLessonFragment extends ScreenFragment {
    private static final String[] REASONS = {"Có việc đột xuất", "Muốn đổi lịch", "Lý do khác"};
    private Lesson lesson;
    private int reason = 0;
    private LinearLayout reasons;
    private EditText other;
    private CheckBox agree;

    @Override protected View build() {
        lesson = classroom.store.lesson(classroom.lessonId);
        Tutor tutor = Tutor.get(lesson.tutorId);
        LinearLayout root = ui.column(); ui.header(root,"Hủy buổi học",classroom::back,null);
        LinearLayout body = ui.page(root);

        LinearLayout card = ui.row(); card.setGravity(android.view.Gravity.TOP); card.setPadding(ui.dp(9),ui.dp(9),ui.dp(9),ui.dp(9));
        ui.surface(card,Ui.WHITE,10,Ui.BORDER);
        card.addView(ui.photo(Store.tutorPhoto(tutor.id),"Ảnh gia sư "+tutor.name,98,106,9)); ui.gap(card,13);
        LinearLayout info = ui.column(); ui.space(info,3); ui.add(info,ui.text(tutor.name,18,Ui.INK,true)); ui.space(info,6);
        ui.add(info,ui.text(lesson.title,15,Ui.MUTED,false)); ui.space(info,34);
        ui.add(info,ui.text("▦ "+lesson.dateLabel()+" · "+lesson.timeLabel(),15,Ui.INK,false));
        ui.weight(card,info); ui.add(body,card); ui.space(body,16);

        ui.section(body,"Lý do hủy",22); ui.space(body,4);
        reasons = ui.column(); ui.add(body,reasons);
        other = ui.entry("Nhập lý do hủy",false); other.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(150)});
        ui.add(body,other); renderReasons(); ui.space(body,12);

        LinearLayout deposit = ui.column(); deposit.setPadding(ui.dp(20),ui.dp(12),ui.dp(16),ui.dp(14)); ui.surface(deposit,Ui.PALE,11,Ui.BORDER);
        LinearLayout amount = ui.row(); amount.addView(ui.icon(R.drawable.ic_coin,26,Ui.INK)); ui.gap(amount,16);
        LinearLayout amountText = ui.column(); ui.add(amountText,ui.text("Học phí ký quỹ:",18,Ui.MUTED,false)); ui.space(amountText,4);
        ui.add(amountText,ui.text(Tutor.money(lesson.total()),26,Ui.INK,true)); ui.weight(amount,amountText); ui.add(deposit,amount); ui.space(deposit,8);
        LinearLayout refund = ui.row(); refund.addView(ui.icon(R.drawable.ic_info,24,Ui.INK)); ui.gap(refund,18);
        ui.weight(refund,ui.text("Quyền hoàn tiền được xem xét theo điều kiện buổi học.",17,Ui.MUTED,false)); ui.add(deposit,refund);
        ui.add(body,deposit); ui.space(body,6);

        agree = ui.check("Tôi đã đọc điều kiện hủy.",false,null); ui.add(body,agree); ui.space(body,4);
        ui.addAction(body,ui.action("Xác nhận hủy",R.drawable.ic_trash,Ui.DANGER,this::cancel),49); ui.space(body,12);
        ui.addAction(body,ui.action("Giữ buổi học",0,Ui.PRIMARY,classroom::back),50);
        return root;
    }

    private void renderReasons() {
        reasons.removeAllViews();
        for (int i = 0; i < REASONS.length; i++) { int index = i; ui.add(reasons,ui.radio(REASONS[i],i==reason,()->{reason=index;renderReasons();})); }
        other.setVisibility(reason == 2 ? View.VISIBLE : View.GONE);
    }

    private void cancel() {
        String text = reason == 2 ? other.getText().toString().trim() : REASONS[reason];
        if (text.isEmpty()) { other.setError("Vui lòng nhập lý do hủy"); other.requestFocus(); return; }
        if (!agree.isChecked()) { classroom.dialog("Chưa đọc điều kiện hủy","Hãy đánh dấu \"Tôi đã đọc điều kiện hủy\" trước khi xác nhận."); return; }
        String error = classroom.store.cancel(lesson, text);
        if (error != null) { classroom.dialog("Không thể hủy buổi học",error); return; }
        classroom.hideKeyboard(); classroom.show("lesson");
        classroom.notice("Đã hủy buổi học. Yêu cầu hoàn tiền sẽ được xem xét theo điều kiện.");
    }
}
