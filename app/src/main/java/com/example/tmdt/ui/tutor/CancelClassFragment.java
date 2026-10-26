package com.example.tmdt.ui.tutor;

import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.tmdt.GroupClass;
import com.example.tmdt.R;
import com.example.tmdt.ui.common.ScreenFragment;
import com.example.tmdt.ui.common.Ui;

/** Hủy lớp học (UC17): the tutor gives a reason, sees who is affected and confirms; the class cannot be reopened. */
public final class CancelClassFragment extends ScreenFragment {
    private static final String[] REASONS = {"Không đủ học viên","Gia sư có việc đột xuất","Thay đổi kế hoạch dạy","Lý do khác"};
    private GroupClass item;
    private int reason = 0;
    private LinearLayout reasons;
    private EditText other;
    private CheckBox agree;

    @Override protected View build() {
        item = classroom.store.groupClass(classroom.classId);
        LinearLayout root = ui.column(); ui.header(root,"Hủy lớp học",classroom::back,null);
        LinearLayout body = ui.page(root);

        LinearLayout card = ui.row(); card.setGravity(Gravity.CENTER_VERTICAL); card.setPadding(ui.dp(12),ui.dp(12),ui.dp(12),ui.dp(12));
        ui.cardSurface(card,Ui.WHITE,Ui.BORDER);
        card.addView(ui.photo(item.art(),null,70,74,12)); ui.gap(card,13);
        LinearLayout info = ui.column();
        TextView title = ui.text(item.title,17,Ui.INK,true); title.setSingleLine(true); title.setEllipsize(TextUtils.TruncateAt.END); ui.add(info,title); ui.space(info,3);
        ui.add(info,ui.text(item.subject+" · "+item.level,14,Ui.MUTED,false)); ui.space(info,6);
        ui.add(info,ui.fact(R.drawable.ic_calendar,item.scheduleLabel(),16,14,Ui.INK,false));
        ui.add(info,ui.fact(R.drawable.ic_users,item.members.size()+"/"+item.capacity+" học viên",16,14,Ui.INK,false));
        ui.weight(card,info); ui.add(body,card); ui.space(body,18);

        ui.section(body,"Lý do hủy lớp",18); ui.space(body,4);
        reasons = ui.column(); ui.add(body,reasons);
        other = ui.entry("Nhập lý do hủy lớp",false); other.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(150)});
        ui.add(body,other); renderReasons(); ui.space(body,16);

        ui.add(body,impact()); ui.space(body,10);
        agree = ui.check("Tôi hiểu lớp đã hủy không thể mở lại.",false,null); ui.add(body,agree); ui.space(body,12);
        LinearLayout actions = ui.row();
        ui.weightAction(actions,ui.action("Giữ lớp",0,Ui.OUTLINE,classroom::back),48); ui.gap(actions,10);
        ui.weightAction(actions,ui.action("Hủy lớp",R.drawable.ic_trash,Ui.DANGER,this::cancel),48);
        ui.add(body,actions);
        return root;
    }

    /** What cancelling changes for the students, so the tutor decides knowingly. */
    private View impact() {
        int waiting = item.pendingCount();
        LinearLayout box = ui.column(); ui.pad(box,16,14); ui.cardSurface(box,Ui.ORANGE_BG,0);
        LinearLayout head = ui.row(); head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(ui.icon(R.drawable.ic_info,20,Ui.ORANGE)); ui.gap(head,8); ui.weight(head,ui.text("Sau khi hủy lớp",16,Ui.INK,true));
        ui.add(box,head); ui.space(box,10);
        ui.add(box,ui.fact(R.drawable.ic_users,item.members.size()+" học viên được báo lớp đã hủy",18,14,Ui.INK,false));
        if (waiting > 0) ui.add(box,ui.fact(R.drawable.ic_clock,waiting+" đăng ký đang chờ sẽ tự đóng",18,14,Ui.INK,false));
        ui.add(box,ui.fact(R.drawable.ic_calendar,"Các buổi còn lại gỡ khỏi lịch học",18,14,Ui.INK,false));
        ui.add(box,ui.fact(R.drawable.ic_coin,"Học phí ký quỹ chờ hoàn lại học viên",18,14,Ui.INK,false));
        return box;
    }

    private void renderReasons() {
        reasons.removeAllViews();
        for (int i = 0; i < REASONS.length; i++) { int index = i; ui.add(reasons,ui.radio(REASONS[i],i==reason,()->{reason=index;renderReasons();})); }
        other.setVisibility(reason == REASONS.length-1 ? View.VISIBLE : View.GONE);
    }

    private void cancel() {
        String text = reason == REASONS.length-1 ? other.getText().toString().trim() : REASONS[reason];
        if (text.isEmpty()) { other.setError("Vui lòng nhập lý do hủy lớp"); other.requestFocus(); return; }
        if (!agree.isChecked()) { classroom.dialog("Chưa xác nhận","Hãy đánh dấu \"Tôi hiểu lớp đã hủy không thể mở lại.\" trước khi hủy."); return; }
        String error = classroom.store.cancelClass(item,text);
        if (error != null) { classroom.dialog("Không thể hủy lớp",error); return; }
        classroom.hideKeyboard(); classroom.classTab = 2; classroom.show("classes");
        classroom.notice("Đã hủy lớp \""+item.title+"\".");
    }
}
