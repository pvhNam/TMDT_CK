package com.example.tmdt.ui.schedule;

import android.view.View;
import android.widget.LinearLayout;
import com.example.tmdt.R;
import com.example.tmdt.ui.common.ScreenFragment;
import com.example.tmdt.ui.common.Ui;
import com.example.tmdt.Lesson;

/** Screen 55 · Phản hồi đổi lịch (branch of UC24): the student compares both slots and answers the tutor. */
public final class RescheduleFragment extends ScreenFragment {
    private Lesson lesson;

    @Override protected View build() {
        lesson = classroom.store.lesson(classroom.lessonId);
        LinearLayout root = ui.column(); ui.header(root,"Yêu cầu đổi lịch",classroom::back,null);
        LinearLayout body = ui.page(root);
        if (!lesson.proposal()) {
            ui.add(body,ui.note(R.drawable.ic_info,"Không có đề nghị đổi lịch nào đang chờ. Lịch hiện tại: "+lesson.dateLabel()+" · "+lesson.timeLabel()+".",Ui.PALE,Ui.BLUE,Ui.INK));
            ui.space(body,16); ui.addAction(body,ui.action("Quay lại buổi học",0,Ui.OUTLINE,classroom::back),49);
            return root;
        }
        ui.space(body,6);
        ui.add(body,ui.note(R.drawable.ic_clock,"Chờ phản hồi\nGia sư đang chờ bạn xác nhận.",Ui.ORANGE_BG,Ui.BLUE,Ui.ORANGE)); ui.space(body,14);

        LinearLayout card = ui.bordered(11); card.setPadding(ui.dp(7),ui.dp(7),ui.dp(7),ui.dp(14));
        LinearLayout profile = ui.row(); profile.setGravity(android.view.Gravity.TOP);
        profile.addView(ui.photo(0,"Ảnh gia sư "+lesson.tutorName,91,94,9)); ui.gap(profile,14);
        LinearLayout info = ui.column(); ui.space(info,8); ui.add(info,ui.text(lesson.tutorName,23,Ui.INK,true)); ui.space(info,8);
        ui.add(info,ui.text(lesson.title,17,Ui.INK,false)); ui.weight(profile,info); ui.add(card,profile); ui.space(card,7);
        ui.add(card,slot("Lịch hiện tại",lesson.dateLabel()+" · "+lesson.timeLabel(),Ui.PALE,Ui.INK)); ui.space(card,12);
        ui.add(card,slot("Lịch đề xuất",lesson.proposalLabel(),Ui.GREEN_BG,Ui.GREEN)); ui.space(card,14);
        LinearLayout reasonTitle = ui.row(); reasonTitle.setPadding(ui.dp(7),0,0,0);
        reasonTitle.addView(ui.text("Lý do đề xuất",18,Ui.INK,true)); ui.add(card,reasonTitle); ui.space(card,10);
        LinearLayout reason = ui.row(); reason.setPadding(ui.dp(12),0,ui.dp(8),0);
        reason.addView(ui.icon(R.drawable.ic_chat,22,Ui.INK)); ui.gap(reason,16);
        ui.weight(reason,ui.text(lesson.proposalReason.isEmpty()?"Gia sư không ghi lý do.":lesson.proposalReason,17,Ui.INK,false));
        ui.add(card,reason); ui.add(body,card); ui.space(body,11);

        ui.addAction(body,ui.action("Đồng ý đổi lịch",0,Ui.PRIMARY,this::accept),50); ui.space(body,12);
        ui.addAction(body,ui.action("Giữ lịch hiện tại",0,Ui.OUTLINE,this::keep),49); ui.space(body,6);
        ui.add(body,ui.note(R.drawable.ic_info,"Lịch cũ được giữ khi chưa có xác nhận.",Ui.WHITE,Ui.BLUE,Ui.MUTED));
        return root;
    }

    private View slot(String title, String value, int fill, int accent) {
        LinearLayout box = ui.column(); box.setPadding(ui.dp(10),ui.dp(11),ui.dp(10),ui.dp(12)); ui.surface(box,fill,10,0);
        ui.add(box,ui.text(title,18,accent,true)); ui.space(box,9);
        LinearLayout row = ui.row(); row.setPadding(ui.dp(3),0,0,0); row.addView(ui.icon(R.drawable.ic_calendar,23,accent)); ui.gap(row,13);
        ui.weight(row,ui.text(value,18,Ui.INK,false)); ui.add(box,row); return box;
    }

    private void accept() {
        String error = classroom.store.acceptProposal(lesson);
        if (error != null) { classroom.dialog("Chưa đổi được lịch",error); return; }
        classroom.show("lesson"); classroom.notice("Đã đổi lịch sang "+lesson.dateLabel()+" · "+lesson.timeLabel()+".");
    }
    private void keep() {
        classroom.store.clearProposal(lesson);
        classroom.show("lesson"); classroom.notice("Đã giữ lịch hiện tại. Gia sư sẽ nhận được phản hồi của bạn.");
    }
}
