package com.example.tmdt.tutor.schedule;

import android.app.DatePickerDialog;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import com.example.tmdt.R;
import com.example.tmdt.MainActivity;
import com.example.tmdt.common.Ui;
import com.example.tmdt.data.GroupClass;
import com.example.tmdt.data.Lesson;
import com.example.tmdt.data.Store;
import com.example.tmdt.data.Tutor;
import com.example.tmdt.tutor.groupclass.OpenedClassesScreen;

/** Screen 45 · Lịch dạy gia sư (UC24): week or month view, lesson details and reschedule proposals. */
public final class TeachingScheduleScreen {
    private static final int[] HOURS = {17, 18, 19, 20, 21};
    private final MainActivity activity;
    private final Ui ui;
    private LinearLayout content;
    public TeachingScheduleScreen(MainActivity activity) { this.activity=activity; this.ui=activity.ui; }

    public View build() {
        LinearLayout root = ui.column();
        ui.title(root,"Lịch dạy",ui.iconButton(R.drawable.ic_bell,Ui.INK,"Thông báo",
                ()->activity.dialog("Thông báo","Màn hình Thông báo (11) do Thành viên 4 phụ trách.")));
        LinearLayout body = ui.page(root);
        content = ui.column(); ui.add(body,content); render();
        return root;
    }

    private void render() {
        content.removeAllViews();
        LocalDate day = activity.teachingDay; boolean month = activity.teachingMonth;
        LinearLayout toggle = ui.row();
        ui.weight(toggle,ui.choice("Tuần",!month,()->{activity.teachingMonth=false;render();})); ui.gap(toggle,8);
        ui.weight(toggle,ui.choice("Tháng",month,()->{activity.teachingMonth=true;render();}));
        ui.add(content,toggle); ui.space(content,12);

        LinearLayout period = ui.row();
        period.addView(ui.iconButton(R.drawable.ic_back,Ui.INK,month?"Tháng trước":"Tuần trước",()->{activity.teachingDay=month?day.minusMonths(1):day.minusWeeks(1);render();}));
        TextView label = ui.text("Tháng "+day.getMonthValue()+", "+day.getYear(),20,Ui.INK,true); label.setGravity(Gravity.CENTER); ui.weight(period,label);
        period.addView(ui.iconButton(R.drawable.ic_right,Ui.INK,month?"Tháng sau":"Tuần sau",()->{activity.teachingDay=month?day.plusMonths(1):day.plusWeeks(1);render();}));
        ui.add(content,period);

        List<Entry> shown = new ArrayList<>();
        if (!month) {
            LinearLayout strip = ui.row(); LocalDate monday = day.with(DayOfWeek.MONDAY);
            for (int i = 0; i < 7; i++) {
                LocalDate date = monday.plusDays(i); if (i > 0) ui.gap(strip,5);
                ui.weight(strip,ui.day(date,date.equals(day),()->{activity.teachingDay=date;render();}));
            }
            ui.add(content,strip); ui.space(content,6);
        }
        for (Lesson lesson : mine()) if (visible(LocalDate.parse(lesson.date),day,month)) shown.add(new Entry(lesson.start(),card(lesson,month)));
        for (GroupClass item : activity.store.classes)
            if (item.tutorId==Store.TUTOR && item.active())
                for (GroupClass.Session session : item.sessionList())
                    if (visible(session.date,day,month)) shown.add(new Entry(session.start(),classCard(session,month)));
        shown.sort(Comparator.comparing(entry->entry.start));
        for (Entry entry : shown) { ui.add(content,entry.view); ui.space(content,11); }
        if (shown.isEmpty()) { ui.add(content,ui.note(R.drawable.ic_info,month?"Không có buổi dạy trong tháng này.":"Không có buổi dạy trong ngày này.",Ui.PALE,Ui.BLUE,Ui.INK)); ui.space(content,11); }

        ui.space(content,4); ui.section(content,"Đề nghị thay đổi",19); ui.space(content,6);
        int proposals = 0;
        for (Lesson lesson : mine()) if (lesson.proposal()) {
            ui.add(content,ui.note(R.drawable.ic_clock,"Chờ học viên đồng ý · "+lesson.student+" · "+lesson.title+"\n"
                    +lesson.dateLabel()+" "+lesson.timeLabel()+" → "+lesson.proposalLabel(),Ui.ORANGE_BG,Ui.BLUE,Ui.ORANGE));
            ui.space(content,8); proposals++;
        }
        if (proposals == 0) ui.add(content,ui.note(R.drawable.ic_info,"Chưa có đề nghị thay đổi nào.",Ui.PALE,Ui.BLUE,Ui.MUTED));
    }

    /** The selected day in week view, or any day of the selected month in month view. */
    private static boolean visible(LocalDate date, LocalDate day, boolean month) {
        return month ? date.getYear()==day.getYear() && date.getMonth()==day.getMonth() : date.equals(day);
    }

    /** A card with the time it is sorted by, so lessons and class sessions share one list. */
    private static final class Entry {
        final LocalDateTime start; final View view;
        Entry(LocalDateTime start, View view) { this.start=start; this.view=view; }
    }

    /** One session of a group class; it follows the class schedule, so it is moved by editing the class, not by a proposal. */
    private View classCard(GroupClass.Session session, boolean withDate) {
        GroupClass item = session.groupClass();
        LinearLayout card = ui.bordered(10); card.setPadding(ui.dp(11),ui.dp(11),ui.dp(9),ui.dp(7));
        LinearLayout top = ui.row();
        ui.weight(top,ui.text((withDate?session.date.format(Lesson.DATE).substring(0,5)+" · ":"")+Lesson.range(item.hour,item.minutes).replace("–"," – "),21,Ui.INK,true));
        top.addView(ui.pill("Lớp nhóm",14,Ui.BLUE,Ui.PALE)); ui.add(card,top); ui.space(card,4);
        ui.add(card,ui.text(item.title,21,Ui.INK,true)); ui.space(card,6);
        LinearLayout row = ui.row(); row.addView(ui.art(item.smallArt(),62,64)); ui.gap(row,12);
        LinearLayout info = ui.column(); ui.add(info,ui.text("Buổi "+session.number+"/"+item.sessions,14,Ui.MUTED,false)); ui.space(info,4);
        ui.add(info,ui.text(item.members.size()+"/"+item.capacity+" học viên",18,Ui.INK,true)); ui.space(info,4);
        ui.add(info,ui.text("▣ "+item.mode,15,Ui.INK,false)); ui.weight(row,info);
        ui.add(card,row); ui.space(card,8);
        LinearLayout actions = ui.row(); card.addView(actions,ui.lp(-1,-2));
        ui.weightAction(actions,ui.action("Thành viên",R.drawable.ic_users,Ui.OUTLINE,()->OpenedClassesScreen.members(activity,item)),40); ui.gap(actions,8);
        ui.weightAction(actions,ui.action("Xem lớp",0,Ui.PRIMARY,()->activity.openClass(item,"review")),40);
        return card;
    }

    private List<Lesson> mine() {
        List<Lesson> list = new ArrayList<>();
        for (Lesson lesson : activity.lessons) if (lesson.tutorId==Store.TUTOR && lesson.active()) list.add(lesson);
        list.sort(Comparator.comparing((Lesson lesson)->lesson.date).thenComparingInt(lesson->lesson.hour));
        return list;
    }

    private View card(Lesson lesson, boolean withDate) {
        LinearLayout card = ui.bordered(10); card.setPadding(ui.dp(11),ui.dp(11),ui.dp(9),ui.dp(7));
        LinearLayout top = ui.row(); ui.weight(top,ui.text((withDate?lesson.dateLabel().substring(0,5)+" · ":"")+lesson.timeLabel().replace("–"," – "),21,Ui.INK,true));
        top.addView(ui.lessonStatus(lesson)); ui.add(card,top); ui.space(card,4);
        ui.add(card,ui.text(lesson.title,21,Ui.INK,true)); ui.space(card,6);
        LinearLayout student = ui.row(); student.addView(ui.photo(Store.studentPhoto(lesson.student),"Ảnh "+lesson.student,62,64,9)); ui.gap(student,12);
        LinearLayout info = ui.column(); ui.add(info,ui.text("Học viên",14,Ui.MUTED,false)); ui.space(info,4);
        ui.add(info,ui.text(lesson.student,18,Ui.INK,true)); ui.space(info,4);
        ui.add(info,ui.text("▣ "+lesson.mode+(lesson.trial?" · Học thử":""),15,Ui.INK,false)); ui.weight(student,info);
        ui.add(card,student); ui.space(card,8);
        LinearLayout actions = ui.row(); card.addView(actions,ui.lp(-1,-2));
        if (lesson.pending()) {
            ui.weightAction(actions,ui.action("Từ chối",0,Ui.DANGER,()->answer(lesson,false)),40); ui.gap(actions,8);
            ui.weightAction(actions,ui.action("Chấp nhận",0,Ui.PRIMARY,()->answer(lesson,true)),40);
        } else {
            ui.weightAction(actions,ui.action("Chi tiết",R.drawable.ic_chat,Ui.OUTLINE,()->details(activity,lesson)),40); ui.gap(actions,8);
            View move = lesson.started() ? ui.action("Đã diễn ra",0,Ui.DISABLED,null)
                    : lesson.proposal() ? ui.action("Chờ học viên",0,Ui.DISABLED,null)
                    : ui.action("Đề nghị đổi lịch",0,Ui.PRIMARY,()->propose(lesson));
            ui.weightAction(actions,move,40);
        }
        return card;
    }

    private void answer(Lesson lesson, boolean accept) {
        String error = accept ? activity.store.acceptRequest(lesson) : activity.store.rejectRequest(lesson);
        if (error != null) { activity.dialog("Không thể xử lý yêu cầu",error); return; }
        render(); activity.notice(accept?"Đã chấp nhận buổi học với "+lesson.student+".":"Đã từ chối yêu cầu của "+lesson.student+".");
    }

    /** Tutor-side lesson summary with a shortcut to message the student. */
    public static void details(MainActivity activity, Lesson lesson) {
        new AlertDialog.Builder(activity).setTitle(lesson.title+" · "+lesson.student)
                .setMessage(lesson.dateLabel()+" · "+lesson.timeLabel()+"\n"+lesson.mode+(lesson.address.isEmpty()?"":"\nĐịa chỉ: "+lesson.address)
                        +"\n\nMục tiêu: "+lesson.goal+"\nHọc phí: "+Tutor.money(lesson.total())+"\nTrạng thái: "+lesson.statusLabel()
                        +(lesson.proposal()?"\n\nĐề nghị đổi sang "+lesson.proposalLabel()+" đang chờ học viên.":""))
                .setNeutralButton("Nhắn tin",(dialog,which)->activity.message(lesson.student))
                .setPositiveButton("Đóng",null).show();
    }

    private void propose(Lesson lesson) {
        LocalDate[] date = {LocalDate.parse(lesson.date).plusDays(1)}; int[] hour = {lesson.hour};
        LinearLayout form = ui.column(); form.setPadding(ui.dp(22),ui.dp(8),ui.dp(22),0);
        ui.label(form,"Ngày mới"); TextView dateValue = ui.value(date[0].format(Lesson.DATE));
        ui.add(form,ui.picker(R.drawable.ic_calendar,dateValue,()->{
            DatePickerDialog picker = new DatePickerDialog(activity,(view,year,month,day)->{date[0]=LocalDate.of(year,month+1,day);dateValue.setText(date[0].format(Lesson.DATE));},
                    date[0].getYear(),date[0].getMonthValue()-1,date[0].getDayOfMonth());
            picker.getDatePicker().setMinDate(System.currentTimeMillis()-1000); picker.show();
        }));
        ui.space(form,12); ui.label(form,"Giờ bắt đầu"); LinearLayout hours = ui.row(); ui.add(form,hours);
        Runnable[] renderHours = new Runnable[1];
        renderHours[0] = () -> {
            hours.removeAllViews();
            for (int i = 0; i < HOURS.length; i++) {
                int value = HOURS[i]; if (i > 0) ui.gap(hours,5);
                ui.weight(hours,ui.choice(value+"h",value==hour[0],()->{hour[0]=value;renderHours[0].run();}));
            }
        };
        renderHours[0].run();
        ui.space(form,12); ui.label(form,"Lý do đề xuất"); EditText reason = ui.entry("Ví dụ: Gia sư có lịch công tác.",false);
        reason.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(120)}); ui.add(form,reason);
        AlertDialog dialog = new AlertDialog.Builder(activity).setTitle("Đề nghị đổi lịch").setView(form)
                .setNegativeButton("Hủy",null).setPositiveButton("Gửi đề nghị",null).create();
        dialog.setOnShowListener(shown -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String text = reason.getText().toString().trim();
            if (text.isEmpty()) { reason.setError("Vui lòng nhập lý do"); return; }
            String error = activity.store.propose(lesson,date[0],hour[0],text);
            if (error != null) { activity.dialog("Chưa gửi được đề nghị",error); return; }
            dialog.dismiss(); activity.hideKeyboard(); render();
            activity.notice("Đã gửi đề nghị. Lịch cũ được giữ cho đến khi học viên đồng ý.");
        }));
        dialog.show();
    }
}
