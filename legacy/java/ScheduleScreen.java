package com.example.tmdt;

import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class ScheduleScreen {
    private final MainActivity activity;
    private final Ui ui;
    private LinearLayout tabs, content;
    ScheduleScreen(MainActivity activity){this.activity=activity;this.ui=activity.ui;}

    View build() {
        LinearLayout root=ui.column();ui.header(root,"Lịch học",null,null);
        tabs=ui.row();ui.pad(tabs,12,0);ui.add(root,tabs);ui.line(root);
        content=ui.body(root);render();return root;
    }
    private void render() {
        tabs.removeAllViews();content.removeAllViews();String[] labels={"Sắp tới","Chờ xác nhận","Đã học"};
        for(int i=0;i<3;i++) {
            int index=i;boolean selected=i==activity.scheduleTab;
            LinearLayout tab=ui.column();TextView label=ui.text(labels[i],14,selected?Ui.BLUE:Ui.MUTED,selected);
            label.setGravity(Gravity.CENTER);label.setMinimumHeight(ui.dp(48));ui.add(tab,label);
            View underline=new View(activity);ui.surface(underline,selected?Ui.BLUE:Ui.WHITE,2,0);tab.addView(underline,ui.lp(-1,3));
            tab.setSelected(selected);tab.setContentDescription(labels[i]);ui.clickable(tab,()->{activity.scheduleTab=index;render();});ui.weight(tabs,tab);
        }
        List<Lesson> visible=new ArrayList<>();
        for(Lesson lesson:activity.lessons) {
            boolean include=activity.scheduleTab==1?lesson.pending:activity.scheduleTab==2?
                    !lesson.pending && lesson.completed():!lesson.pending && !lesson.completed();
            if(include)visible.add(lesson);
        }
        visible.sort(Comparator.comparing((Lesson lesson)->lesson.date).thenComparingInt(lesson->lesson.hour));
        for(Lesson lesson:visible){ui.add(content,lessonCard(lesson));ui.space(content,12);}
        if(visible.isEmpty()) {
            LinearLayout empty=ui.column();ui.pad(empty,18,32);empty.setGravity(Gravity.CENTER);ui.surface(empty,0xFFF3F8FE,12,0);
            empty.addView(new LineIcon(activity,"calendar",Ui.BLUE),ui.lp(42,42));ui.space(empty,16);
            TextView title=ui.text(activity.scheduleTab==1?"Chưa có yêu cầu đặt học":activity.scheduleTab==2?"Chưa có buổi học đã hoàn thành":"Chưa có lịch học sắp tới",17,Ui.INK,true);
            title.setGravity(Gravity.CENTER);ui.add(empty,title);ui.space(empty,10);
            TextView description=ui.text("Tìm gia sư phù hợp và bắt đầu hành trình học tập của bạn.",14,Ui.MUTED,false);description.setGravity(Gravity.CENTER);
            ui.add(empty,description);ui.space(empty,18);ui.add(empty,ui.button("Tìm gia sư","search",true,()->activity.show("home")));ui.add(content,empty);ui.space(content,16);
        }
        ui.space(content,3);ui.add(content,new DesignImage(activity,3));ui.space(content,10);
    }
    private View lessonCard(Lesson lesson) {
        Tutor tutor=Tutor.ALL[lesson.tutorId];LinearLayout card=ui.card(),row=ui.row();row.setGravity(Gravity.TOP);
        row.addView(ui.portrait(tutor,76,82));ui.gap(row,12);LinearLayout details=ui.column();
        // Keep the status on its own row so names remain readable on narrow devices.
        ui.add(details,ui.text(lesson.title(),16,Ui.INK,true));ui.space(details,5);
        TextView teacher=ui.text(tutor.name,14,Ui.MUTED,false);ui.clickable(teacher,()->activity.openTutor(tutor));ui.add(details,teacher);ui.space(details,7);
        TextView status=ui.badge(lesson.pending?"Chờ xác nhận":lesson.completed()?"Đã học":"Đã xác nhận");
        if(lesson.pending){status.setTextColor(0xFFA36A00);ui.surface(status,0xFFFFF2D8,8,0);}details.addView(status,ui.lp(-2,-2));
        ui.weight(row,details);ui.add(card,row);ui.space(card,13);
        LinearLayout date=ui.row();date.addView(new LineIcon(activity,"calendar",Ui.MUTED),ui.lp(18,18));ui.gap(date,9);
        ui.weight(date,ui.text(lesson.dateLabel()+"  ·  "+lesson.timeLabel(),13,Ui.MUTED,false));ui.add(card,date);ui.space(card,9);
        LinearLayout mode=ui.row();mode.addView(new LineIcon(activity,lesson.mode.equals("Trực tuyến")?"video":"home",Ui.MUTED),ui.lp(18,18));ui.gap(mode,9);
        ui.weight(mode,ui.text(lesson.mode,13,Ui.MUTED,false));ui.add(card,mode);ui.space(card,14);
        LinearLayout actions=ui.row();ui.weight(actions,ui.button("Nhắn tin","chat",false,()->activity.message(tutor)));ui.gap(actions,9);
        ui.weight(actions,ui.button("Chi tiết",null,true,()->activity.dialog("Chi tiết buổi học",
                lesson.title()+"\nGia sư: "+tutor.name+"\n\n"+lesson.dateLabel()+" · "+lesson.timeLabel()+"\n"+lesson.mode+
                        (lesson.address.isEmpty()?"":"\nĐịa chỉ: "+lesson.address)+"\n\nMục tiêu: "+lesson.goal+"\nHọc phí: "+Tutor.money(lesson.total())+
                        "\n\n"+(lesson.pending?"Yêu cầu đang chờ xác nhận. Đây là dữ liệu lưu trên thiết bị, chưa gửi đến gia sư.":"Buổi học mẫu để xem trước giao diện."))));
        ui.add(card,actions);return card;
    }
}
