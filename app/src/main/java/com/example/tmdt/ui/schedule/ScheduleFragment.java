package com.example.tmdt.ui.schedule;

import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import com.example.tmdt.R;
import com.example.tmdt.ui.common.ScreenFragment;
import com.example.tmdt.ui.common.Ui;
import com.example.tmdt.GroupClass;
import com.example.tmdt.Lesson;
import com.example.tmdt.Store;
import com.example.tmdt.Tutor;

/** Screen 04 · Lịch học (UC24): the student's upcoming, pending and past lessons. */
public final class ScheduleFragment extends ScreenFragment {

    @Override protected View build() {
        LinearLayout root=ui.column();ui.title(root,"Lịch học",ui.link("Chế độ gia sư",15,this::tutorMode));
        ui.tabs(root,new String[]{"Sắp tới","Chờ xác nhận","Đã học"},classroom.scheduleTab,index->{classroom.scheduleTab=index;classroom.show("schedule");});
        LinearLayout content=ui.page(root);ui.space(content,6);
        int tab=classroom.scheduleTab;
        List<Entry> visible=new ArrayList<>();
        for(Lesson lesson:classroom.lessons) {
            if(!lesson.student.equals(Store.STUDENT))continue;
            boolean include=tab==1?lesson.pending()||Lesson.REJECTED.equals(lesson.status):
                    tab==2?(lesson.confirmed()&&lesson.ended())||Lesson.CANCELLED.equals(lesson.status):
                    lesson.confirmed()&&!lesson.ended();
            if(include)visible.add(new Entry(lesson.start(),lessonCard(lesson)));
        }
        // Group classes: registrations wait under "Chờ xác nhận"; members see every session under "Sắp tới" or "Đã học".
        for(GroupClass item:classroom.store.classes) {
            if(tab==1) {
                for(GroupClass.Registration registration:item.registrations) {
                    if(!registration.student.equals(Store.STUDENT)||!(registration.pending()||Lesson.REJECTED.equals(registration.status)))continue;
                    boolean waiting=registration.pending();
                    visible.add(new Entry(LocalDate.parse(item.startDate).atTime(item.hour,0),classCard(item,"Khai giảng "+item.startLabel()+" · "+item.scheduleLabel(),
                            waiting?ui.pill("Chờ duyệt",14,Ui.ORANGE,Ui.ORANGE_BG):ui.pill("Bị từ chối",14,Ui.MUTED,0xFFEEF2F7),
                            (waiting?"Đăng ký gửi ngày ":"Gia sư đã từ chối đăng ký gửi ngày ")+LocalDate.parse(registration.date).format(Lesson.DATE)+".")));
                }
            } else if(item.members.contains(Store.STUDENT)) {
                for(GroupClass.Session session:item.sessionList()) {
                    if(tab==2?!session.ended():session.ended()||!item.active())continue;
                    visible.add(new Entry(session.start(),classCard(item,session.date.format(Lesson.DATE)+" · "+Lesson.range(item.hour,item.minutes),
                            ui.pill("Buổi "+session.number+"/"+item.sessions,14,tab==2?Ui.MUTED:Ui.GREEN,tab==2?0xFFEEF2F7:Ui.GREEN_BG),
                            "Buổi "+session.number+" trên tổng số "+item.sessions+" buổi.")));
                }
            }
        }
        Comparator<Entry> order=Comparator.comparing(entry->entry.start);
        visible.sort(tab==2?order.reversed():order);
        for(Entry entry:visible){ui.add(content,entry.view);ui.space(content,11);}
        if(visible.isEmpty()) {
            LinearLayout empty=ui.column();ui.pad(empty,18,32);empty.setGravity(Gravity.CENTER);ui.surface(empty,0xFFF3F8FE,12,0);
            empty.addView(ui.icon(R.drawable.ic_calendar,42,Ui.BLUE));ui.space(empty,16);
            TextView title=ui.text(classroom.scheduleTab==1?"Chưa có yêu cầu đặt học":classroom.scheduleTab==2?"Chưa có buổi học đã hoàn thành":"Chưa có lịch học sắp tới",17,Ui.INK,true);
            title.setGravity(Gravity.CENTER);ui.add(empty,title);ui.space(empty,10);
            TextView description=ui.text("Tìm gia sư phù hợp và bắt đầu hành trình học tập của bạn.",14,Ui.MUTED,false);description.setGravity(Gravity.CENTER);
            ui.add(empty,description);ui.space(empty,18);ui.addAction(empty,ui.action("Tìm gia sư",R.drawable.ic_search,Ui.PRIMARY,()->classroom.show("home")),48);
            ui.add(content,empty);ui.space(content,16);
        }
        ui.space(content,7);ui.add(content,banner());
        return root;
    }

    private View lessonCard(Lesson lesson) {
        Tutor tutor=Tutor.get(lesson.tutorId);
        LinearLayout card=ui.bordered(12);card.setPadding(ui.dp(10),ui.dp(10),ui.dp(10),ui.dp(10));
        LinearLayout row=ui.row();row.setGravity(Gravity.TOP);
        row.addView(ui.photo(Store.tutorPhoto(tutor.id),"Ảnh gia sư "+tutor.name,73,77,9));ui.gap(row,14);
        LinearLayout details=ui.column();ui.space(details,4);
        LinearLayout titleRow=ui.row();titleRow.setGravity(Gravity.TOP);
        TextView title=ui.text(lesson.title,lesson.title.length()>14?14:17,Ui.INK,true);ui.weight(titleRow,title);ui.gap(titleRow,4);
        titleRow.addView(ui.lessonStatus(lesson));ui.add(details,titleRow);ui.space(details,2);
        TextView teacher=ui.text(tutor.name,15,Ui.MUTED,false);ui.clickable(teacher,()->classroom.openTutor(tutor));ui.add(details,teacher);ui.space(details,8);
        ui.add(details,ui.fact(R.drawable.ic_calendar,lesson.dateLabel()+" · "+lesson.timeLabel(),18,14,Ui.MUTED,false));
        ui.add(details,ui.fact(lesson.mode.equals("Tại nhà")?R.drawable.ic_home:R.drawable.ic_video,lesson.mode+(lesson.trial?" · Học thử":""),18,15,Ui.MUTED,false));
        ui.weight(row,details);ui.add(card,row);
        if(lesson.proposal()&&lesson.confirmed()) {
            ui.space(card,8);LinearLayout proposal=ui.note(R.drawable.ic_clock,"Gia sư đề nghị đổi lịch. Chạm để xem.",Ui.ORANGE_BG,Ui.BLUE,Ui.ORANGE);
            ui.clickable(proposal,()->{classroom.lessonId=lesson.id;classroom.show("reschedule");});ui.add(card,proposal);
        }
        ui.space(card,9);
        LinearLayout actions=ui.row();
        ui.weightAction(actions,ui.action("Nhắn tin",0,Ui.OUTLINE,()->classroom.message(tutor)),38);ui.gap(actions,9);
        ui.weightAction(actions,ui.action(lesson.needsConfirmation()?"Xác nhận":"Chi tiết",0,Ui.PRIMARY,()->classroom.openLesson(lesson)),38);
        ui.add(card,actions);return card;
    }

    /** Demo switch to the sample tutor's side, where requests and class registrations are answered. */
    private void tutorMode() {
        new AlertDialog.Builder(classroom.context()).setTitle("Chuyển sang chế độ gia sư?")
                .setMessage("Bạn sẽ xem ứng dụng như cô Minh Anh (tài khoản gia sư mẫu) để duyệt yêu cầu và quản lý lớp nhóm.")
                .setNegativeButton("Hủy",null).setPositiveButton("Chuyển",(d,w)->classroom.setTutorMode(true)).show();
    }

    /** A group class session or registration, laid out like a lesson card; "Chi tiết" summarises the class. */
    private View classCard(GroupClass item, String when, TextView status, String note) {
        Tutor tutor=Tutor.get(item.tutorId);
        LinearLayout card=ui.bordered(12);card.setPadding(ui.dp(10),ui.dp(10),ui.dp(10),ui.dp(10));
        LinearLayout row=ui.row();row.setGravity(Gravity.TOP);
        row.addView(ui.art(item.smallArt(),73,77));ui.gap(row,14);
        LinearLayout details=ui.column();ui.space(details,4);
        LinearLayout titleRow=ui.row();titleRow.setGravity(Gravity.TOP);
        TextView title=ui.text(item.title,item.title.length()>14?14:17,Ui.INK,true);ui.weight(titleRow,title);ui.gap(titleRow,4);
        titleRow.addView(status);ui.add(details,titleRow);ui.space(details,2);
        TextView teacher=ui.text(tutor.name,15,Ui.MUTED,false);ui.clickable(teacher,()->classroom.openTutor(tutor));ui.add(details,teacher);ui.space(details,8);
        ui.add(details,ui.fact(R.drawable.ic_calendar,when,18,14,Ui.MUTED,false));
        ui.add(details,ui.fact(R.drawable.ic_users,item.mode+" · Lớp nhóm",18,15,Ui.MUTED,false));
        ui.weight(row,details);ui.add(card,row);ui.space(card,9);
        String summary=note+"\n\n"+item.subject+" · "+item.level+"\nLịch học: "+item.scheduleLabel()+"\nKhai giảng: "+item.startLabel()+" · "+item.sessions+" buổi\n"
                +item.mode+(item.address.isEmpty()?"":" · "+item.address)+"\nSĩ số: "+item.members.size()+"/"+item.capacity+"\nHọc phí: "+item.priceLabel();
        LinearLayout actions=ui.row();
        ui.weightAction(actions,ui.action("Nhắn tin",0,Ui.OUTLINE,()->classroom.message(tutor)),38);ui.gap(actions,9);
        ui.weightAction(actions,ui.action("Chi tiết",0,Ui.PRIMARY,()->classroom.dialog(item.title,summary)),38);
        ui.add(card,actions);return card;
    }

    /** A card with the time it is sorted by, so lessons and class sessions share one list. */
    private static final class Entry {
        final LocalDateTime start; final View view;
        Entry(LocalDateTime start,View view){this.start=start;this.view=view;}
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
