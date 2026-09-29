package com.example.tmdt.student.schedule;

import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import com.example.tmdt.R;
import com.example.tmdt.MainActivity;
import com.example.tmdt.common.Ui;
import com.example.tmdt.data.Lesson;
import com.example.tmdt.data.Store;
import com.example.tmdt.data.Tutor;

/** Screen 04 · Lịch học (UC24): the student's upcoming, pending and past lessons. */
public final class ScheduleScreen {
    private final MainActivity activity;
    private final Ui ui;
    public ScheduleScreen(MainActivity activity){this.activity=activity;this.ui=activity.ui;}

    public View build() {
        LinearLayout root=ui.column();ui.title(root,"Lịch học",null);
        ui.tabs(root,new String[]{"Sắp tới","Chờ xác nhận","Đã học"},activity.scheduleTab,index->{activity.scheduleTab=index;activity.show("schedule");});
        LinearLayout content=ui.page(root);ui.space(content,6);
        List<Lesson> visible=new ArrayList<>();
        for(Lesson lesson:activity.lessons) {
            if(!lesson.student.equals(Store.STUDENT))continue;
            boolean include=activity.scheduleTab==1?lesson.pending()||Lesson.REJECTED.equals(lesson.status):
                    activity.scheduleTab==2?(lesson.confirmed()&&lesson.ended())||Lesson.CANCELLED.equals(lesson.status):
                    lesson.confirmed()&&!lesson.ended();
            if(include)visible.add(lesson);
        }
        Comparator<Lesson> order=Comparator.comparing((Lesson lesson)->lesson.date).thenComparingInt(lesson->lesson.hour);
        visible.sort(activity.scheduleTab==2?order.reversed():order);
        for(Lesson lesson:visible){ui.add(content,lessonCard(lesson));ui.space(content,11);}
        if(visible.isEmpty()) {
            LinearLayout empty=ui.column();ui.pad(empty,18,32);empty.setGravity(Gravity.CENTER);ui.surface(empty,0xFFF3F8FE,12,0);
            empty.addView(ui.icon(R.drawable.ic_calendar,42,Ui.BLUE));ui.space(empty,16);
            TextView title=ui.text(activity.scheduleTab==1?"Chưa có yêu cầu đặt học":activity.scheduleTab==2?"Chưa có buổi học đã hoàn thành":"Chưa có lịch học sắp tới",17,Ui.INK,true);
            title.setGravity(Gravity.CENTER);ui.add(empty,title);ui.space(empty,10);
            TextView description=ui.text("Tìm gia sư phù hợp và bắt đầu hành trình học tập của bạn.",14,Ui.MUTED,false);description.setGravity(Gravity.CENTER);
            ui.add(empty,description);ui.space(empty,18);ui.addAction(empty,ui.action("Tìm gia sư",R.drawable.ic_search,Ui.PRIMARY,()->activity.show("home")),48);
            ui.add(content,empty);ui.space(content,16);
        }
        ui.space(content,7);ui.add(content,banner());
        return root;
    }

    private View lessonCard(Lesson lesson) {
        Tutor tutor=Tutor.ALL[lesson.tutorId];
        LinearLayout card=ui.bordered(12);card.setPadding(ui.dp(10),ui.dp(10),ui.dp(10),ui.dp(10));
        LinearLayout row=ui.row();row.setGravity(Gravity.TOP);
        row.addView(ui.photo(Store.tutorPhoto(tutor.id),"Ảnh gia sư "+tutor.name,73,77,9));ui.gap(row,14);
        LinearLayout details=ui.column();ui.space(details,4);
        LinearLayout titleRow=ui.row();titleRow.setGravity(Gravity.TOP);
        TextView title=ui.text(lesson.title,lesson.title.length()>14?14:17,Ui.INK,true);ui.weight(titleRow,title);ui.gap(titleRow,4);
        titleRow.addView(ui.lessonStatus(lesson));ui.add(details,titleRow);ui.space(details,2);
        TextView teacher=ui.text(tutor.name,15,Ui.MUTED,false);ui.clickable(teacher,()->activity.openTutor(tutor));ui.add(details,teacher);ui.space(details,8);
        ui.add(details,ui.fact(R.drawable.ic_calendar,lesson.dateLabel()+" · "+lesson.timeLabel(),18,14,Ui.MUTED,false));
        ui.add(details,ui.fact(lesson.mode.equals("Tại nhà")?R.drawable.ic_home:R.drawable.ic_video,lesson.mode+(lesson.trial?" · Học thử":""),18,15,Ui.MUTED,false));
        ui.weight(row,details);ui.add(card,row);
        if(lesson.proposal()&&lesson.confirmed()) {
            ui.space(card,8);LinearLayout proposal=ui.note(R.drawable.ic_clock,"Gia sư đề nghị đổi lịch. Chạm để xem.",Ui.ORANGE_BG,Ui.BLUE,Ui.ORANGE);
            ui.clickable(proposal,()->{activity.lessonId=lesson.id;activity.show("reschedule");});ui.add(card,proposal);
        }
        ui.space(card,9);
        LinearLayout actions=ui.row();
        ui.weightAction(actions,ui.action("Nhắn tin",0,Ui.OUTLINE,()->activity.message(tutor)),38);ui.gap(actions,9);
        ui.weightAction(actions,ui.action(lesson.needsConfirmation()?"Xác nhận":"Chi tiết",0,Ui.PRIMARY,()->activity.openLesson(lesson)),38);
        ui.add(card,actions);return card;
    }

    /** "Sẵn sàng cho buổi học tiếp theo" banner with the calendar illustration of the design. */
    private View banner() {
        LinearLayout banner=ui.row();banner.setPadding(ui.dp(16),ui.dp(16),ui.dp(7),ui.dp(10));banner.setMinimumHeight(ui.dp(160));ui.surface(banner,Ui.PALE,11,0);
        LinearLayout text=ui.column();ui.add(text,ui.text("Sẵn sàng cho buổi học tiếp theo",21,Ui.INK,true));ui.space(text,8);
        ui.add(text,ui.text("Xem lại mục tiêu trước khi bắt đầu.",16,Ui.MUTED,false));ui.weight(banner,text);
        banner.addView(ui.art(R.drawable.illus_calendar,126,126));
        return banner;
    }
}
