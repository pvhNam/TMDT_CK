package com.example.tmdt.ui.tutor;

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
import com.example.tmdt.Classroom;
import com.example.tmdt.ui.common.ScreenFragment;
import com.example.tmdt.ui.common.TimeRange;
import com.example.tmdt.ui.common.Ui;
import com.example.tmdt.GroupClass;
import com.example.tmdt.Lesson;
import com.example.tmdt.Tutor;

/** Screen 45 · Lịch dạy gia sư (UC24): week or month view, lesson details and reschedule proposals. */
public final class TeachingScheduleFragment extends ScreenFragment {
    private LinearLayout content;

    @Override protected View build() {
        LinearLayout root = ui.column();
        ui.title(root,"Lịch dạy",ui.iconButton(R.drawable.ic_bell,Ui.INK,"Thông báo",
                ()->classroom.dialog("Thông báo","Chức năng này hiện chưa khả dụng.")));
        LinearLayout body = ui.page(root);
        content = ui.column(); ui.add(body,content); render();
        return root;
    }

    private void animateCalendar(){
        render();com.example.tmdt.ui.common.Motion.reveal(content);
    }
    private void render() {
        content.removeAllViews();
        LocalDate day = classroom.teachingDay; boolean month = classroom.teachingMonth;
        LinearLayout toggle = ui.row();
        ui.weight(toggle,ui.choice("Tuần",!month,()->{if(classroom.teachingMonth){classroom.teachingMonth=false;animateCalendar();}})); ui.gap(toggle,8);
        ui.weight(toggle,ui.choice("Tháng",month,()->{if(!classroom.teachingMonth){classroom.teachingMonth=true;animateCalendar();}}));
        ui.add(content,toggle); ui.space(content,12);

        LinearLayout period = ui.row();
        period.addView(ui.iconButton(R.drawable.ic_back,Ui.INK,month?"Tháng trước":"Tuần trước",()->{classroom.teachingDay=month?day.minusMonths(1):day.minusWeeks(1);animateCalendar();}),ui.lp(40,36));
        TextView label = ui.text("Tháng "+day.getMonthValue()+", "+day.getYear(),20,Ui.INK,true); label.setGravity(Gravity.CENTER); ui.weight(period,label);
        period.addView(ui.iconButton(R.drawable.ic_right,Ui.INK,month?"Tháng sau":"Tuần sau",()->{classroom.teachingDay=month?day.plusMonths(1):day.plusWeeks(1);animateCalendar();}),ui.lp(40,36));
        ui.add(content,period);

        List<Entry> shown = new ArrayList<>();
        if (!month) {
            LinearLayout strip = ui.row(); LocalDate monday = day.with(DayOfWeek.MONDAY);
            for (int i = 0; i < 7; i++) {
                LocalDate date = monday.plusDays(i); if (i > 0) ui.gap(strip,5);
                ui.weight(strip,ui.day(date,date.equals(day),()->{if(!date.equals(classroom.teachingDay)){classroom.teachingDay=date;animateCalendar();}}));
            }
            ui.add(content,strip); ui.space(content,6);
        }
        for (Lesson lesson : mine()) if (visible(LocalDate.parse(lesson.date),day,month)) shown.add(new Entry(lesson.start(),card(lesson,month)));
        for (GroupClass item : classroom.store.classes)
            if (item.tutorId.equals(classroom.me()) && item.active())
                for (GroupClass.Session session : item.sessionList())
                    if (visible(session.date,day,month)) shown.add(new Entry(session.start(),classCard(session,month)));
        shown.sort(Comparator.comparing(entry->entry.start));
        for (Entry entry : shown) { ui.add(content,entry.view); ui.space(content,11); }
        if (shown.isEmpty()) { ui.add(content,ui.note(R.drawable.ic_info,month?"Không có buổi dạy trong tháng này.":"Không có buổi dạy trong ngày này.",Ui.PALE,Ui.BLUE,Ui.INK)); ui.space(content,11); }

        ui.space(content,4); ui.section(content,"Đề nghị thay đổi",19); ui.space(content,6);
        int proposals = 0;
        for (Lesson lesson : mine()) if (lesson.proposal()) {
            ui.add(content,ui.note(R.drawable.ic_clock,"Chờ học viên đồng ý · "+lesson.studentName+" · "+lesson.title+"\n"
                    +lesson.dateLabel()+" "+lesson.timeLabel()+" → "+lesson.proposalLabel(),Ui.ORANGE_BG,Ui.BLUE,Ui.ORANGE));
            ui.space(content,8); proposals++;
        }
        for (Lesson lesson : mine()) if (lesson.replied()) {
            ui.add(content,ui.note(lesson.kept()?R.drawable.ic_clock:R.drawable.ic_calendar,lesson.replyText(),
                    lesson.kept()?0xFFFFE9EC:Ui.GREEN_BG,lesson.kept()?Ui.RED:Ui.GREEN,Ui.INK));
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
        LinearLayout card = ui.bordered(); card.setPadding(ui.dp(11),ui.dp(11),ui.dp(9),ui.dp(7));
        LinearLayout top = ui.row();
        ui.weight(top,ui.text((withDate?session.date.format(Lesson.DATE).substring(0,5)+" · ":"")+Lesson.range(item.hour,item.minutes).replace("–"," – "),21,Ui.INK,true));
        top.addView(ui.pill("Lớp nhóm",14,Ui.BLUE,Ui.PALE)); ui.add(card,top); ui.space(card,4);
        ui.add(card,ui.text(item.title,21,Ui.INK,true)); ui.space(card,6);
        LinearLayout row = ui.row(); row.addView(ui.photo(item.art(),null,62,64,12)); ui.gap(row,12);
        LinearLayout info = ui.column(); ui.add(info,ui.text("Buổi "+session.number+"/"+item.sessions,14,Ui.MUTED,false)); ui.space(info,4);
        ui.add(info,ui.text(item.members.size()+"/"+item.capacity+" học viên",18,Ui.INK,true)); ui.space(info,4);
        ui.add(info,ui.text("▣ "+item.mode,15,Ui.INK,false)); ui.weight(row,info);
        ui.add(card,row); ui.space(card,8);
        LinearLayout actions = ui.row(); card.addView(actions,ui.lp(-1,-2));
        ui.weightAction(actions,ui.action("Thành viên",R.drawable.ic_users,Ui.OUTLINE,()->OpenedClassesFragment.members(classroom,item)),40); ui.gap(actions,8);
        ui.weightAction(actions,ui.action("Xem lớp",0,Ui.PRIMARY,()->classroom.openClass(item,"review")),40);
        return card;
    }

    private List<Lesson> mine() {
        List<Lesson> list = new ArrayList<>();
        for (Lesson lesson : classroom.lessons) if (lesson.tutorId.equals(classroom.me()) && lesson.active()) list.add(lesson);
        list.sort(Comparator.comparing((Lesson lesson)->lesson.date).thenComparingInt(lesson->lesson.hour));
        return list;
    }

    private View card(Lesson lesson, boolean withDate) {
        LinearLayout card = ui.bordered(); card.setPadding(ui.dp(14),ui.dp(12),ui.dp(14),ui.dp(14));
        LinearLayout top = ui.row(); top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(ui.icon(R.drawable.ic_clock,18,Ui.BLUE)); ui.gap(top,6);
        ui.weight(top,ui.text((withDate?lesson.dateLabel().substring(0,5)+" · ":"")+lesson.timeLabel().replace("–"," – "),17,Ui.INK,true));
        top.addView(ui.lessonStatus(lesson)); ui.add(card,top); ui.space(card,12);

        LinearLayout student = ui.row(); student.setGravity(Gravity.CENTER_VERTICAL);
        student.addView(ui.avatar(lesson.studentId,lesson.studentName,48)); ui.gap(student,12);
        LinearLayout info = ui.column(); ui.add(info,ui.text(lesson.title,17,Ui.INK,true)); ui.space(info,3);
        ui.add(info,ui.text(lesson.studentName,14,Ui.MUTED,false)); ui.weight(student,info); ui.gap(student,8);
        student.addView(ui.pill(lesson.mode+(lesson.trial?" · Thử":""),13,lesson.online()?Ui.BLUE:Ui.GREEN,lesson.online()?Ui.PALE:Ui.GREEN_BG),ui.lp(-2,26));
        ui.add(card,student); ui.space(card,12);

        if (lesson.confirmed() && lesson.online() && !lesson.ended()) {
            LinearLayout link = ui.row(); link.setGravity(Gravity.CENTER_VERTICAL);
            link.setPadding(ui.dp(12),ui.dp(9),ui.dp(12),ui.dp(9)); ui.fieldSurface(link,Ui.PALE,0);
            link.addView(ui.icon(R.drawable.ic_video,18,Ui.BLUE)); ui.gap(link,8);
            TextView host = ui.text(lesson.roomLink.isEmpty()?"Chưa có link phòng học":lesson.roomHost(),14,lesson.roomLink.isEmpty()?Ui.MUTED:Ui.INK,false);
            host.setSingleLine(true); host.setEllipsize(android.text.TextUtils.TruncateAt.END); ui.weight(link,host); ui.gap(link,8);
            link.addView(ui.text(lesson.roomLink.isEmpty()?"Dán link":"Sửa",14,Ui.BLUE,true));
            link.setContentDescription("Link phòng học. Chạm để "+(lesson.roomLink.isEmpty()?"dán link":"sửa"));
            ui.clickable(link,()->editRoomLink(lesson)); ui.add(card,link); ui.space(card,12);
        }
        LinearLayout actions = ui.row(); card.addView(actions,ui.lp(-1,-2));
        if (lesson.pending()) {
            ui.weightAction(actions,ui.action("Từ chối",0,Ui.DANGER,()->answer(lesson,false)),42); ui.gap(actions,10);
            ui.weightAction(actions,ui.action("Chấp nhận",0,Ui.PRIMARY,()->answer(lesson,true)),42);
        } else {
            ui.weightAction(actions,ui.action("Chi tiết",0,Ui.OUTLINE,()->details(classroom,lesson,this::render)),42); ui.gap(actions,10);
            View move = lesson.roomOpen() && lesson.online() ? ui.action("Vào phòng học",R.drawable.ic_video,Ui.PRIMARY,()->classroom.openRoom(lesson))
                    : lesson.started() ? ui.action("Đã diễn ra",0,Ui.DISABLED,null)
                    : lesson.proposal() ? ui.action("Chờ học viên",0,Ui.DISABLED,null)
                    : ui.action("Đề nghị đổi lịch",0,Ui.PRIMARY,()->propose(lesson));
            ui.weightAction(actions,move,42);
        }
        return card;
    }

    private void editRoomLink(Lesson lesson) { editRoomLink(classroom,lesson,this::render); }

    private static void editRoomLink(Classroom classroom, Lesson lesson, Runnable saved) {
        Ui ui = classroom.ui;
        LinearLayout form = ui.column(); form.setPadding(ui.dp(22),ui.dp(4),ui.dp(22),0);
        ui.add(form,ui.text("Dán link Google Meet, Google Classroom hoặc Zoom. Để trống thì dùng phòng Jitsi của app.",14,Ui.MUTED,false)); ui.space(form,12);
        EditText input = ui.entry("https://meet.google.com/...",false); input.setText(lesson.roomLink);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI); ui.add(form,input);
        AlertDialog dialog = ui.dialog().setTitle("Link phòng học").setView(form).setNegativeButton("Hủy",null).setPositiveButton("Lưu",null).create();
        dialog.setOnShowListener(shown -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String error = classroom.store.setRoomLink(lesson,input.getText().toString());
            if (error != null) { input.setError(error); return; }
            dialog.dismiss(); classroom.hideKeyboard(); saved.run();
            classroom.notice(lesson.roomLink.isEmpty()?"Buổi học sẽ dùng phòng Jitsi của app.":"Đã lưu link phòng học.");
        }));
        dialog.show();
    }

    private void answer(Lesson lesson, boolean accept) {
        String error = accept ? classroom.store.acceptRequest(lesson) : classroom.store.rejectRequest(lesson);
        if (error != null) { classroom.dialog("Không thể xử lý yêu cầu",error); return; }
        render(); classroom.notice(accept?"Đã chấp nhận buổi học với "+lesson.studentName+".":"Đã từ chối yêu cầu của "+lesson.studentName+".");
    }

    /** Tutor-side lesson summary with the room link, a shortcut to message the student and the room button. */
    public static void details(Classroom classroom, Lesson lesson) { details(classroom,lesson,()->{}); }

    private static void details(Classroom classroom, Lesson lesson, Runnable changed) {
        Ui ui = classroom.ui;
        LinearLayout view = ui.column(); view.setPadding(ui.dp(20),ui.dp(20),ui.dp(20),ui.dp(18));
        LinearLayout top = ui.row();
        top.addView(ui.avatar(lesson.studentId,lesson.studentName,56)); ui.gap(top,14);
        LinearLayout who = ui.column(); ui.add(who,ui.text(lesson.studentName,20,Ui.INK,true)); ui.space(who,6);
        who.addView(ui.lessonStatus(lesson),ui.lp(-2,-2));
        ui.weight(top,who); ui.add(view,top); ui.space(view,16);

        LinearLayout table = ui.table();
        ui.tableRow(table,R.drawable.ic_file,"Môn học",lesson.title);
        ui.tableRow(table,R.drawable.ic_calendar,"Ngày học",lesson.dateLabel());
        ui.tableRow(table,R.drawable.ic_clock,"Thời gian",lesson.timeLabel());
        ui.tableRow(table,lesson.online()?R.drawable.ic_video:R.drawable.ic_home_soft,"Hình thức",lesson.mode+(lesson.trial?" · Học thử":""));
        if (!lesson.address.isEmpty()) ui.tableRow(table,R.drawable.ic_pin,"Địa chỉ",lesson.address);
        ui.add(view,table); ui.space(view,12);

        AlertDialog dialog = ui.dialog().create();
        if (lesson.confirmed() && lesson.online() && !lesson.ended()) {
            LinearLayout link = ui.note(R.drawable.ic_video,lesson.roomLink.isEmpty()?"Chưa gắn link phòng học. Chạm để dán link Google Meet, Classroom...":"Phòng học: "+lesson.roomHost()+". Chạm để sửa link.",Ui.PALE,Ui.BLUE,Ui.INK);
            ui.clickable(link,()->{dialog.dismiss();editRoomLink(classroom,lesson,()->{changed.run();details(classroom,lesson,changed);});});
            ui.add(view,link); ui.space(view,12);
        }
        if (lesson.proposal()) {
            ui.add(view,ui.note(R.drawable.ic_clock,"Đề nghị đổi sang "+lesson.proposalLabel()+" đang chờ học viên.",Ui.ORANGE_BG,Ui.ORANGE,Ui.ORANGE)); ui.space(view,12);
        }

        ui.label(view,"Mục tiêu buổi học");
        TextView goal = ui.text(lesson.goal,15,Ui.INK,false); goal.setPadding(ui.dp(12),ui.dp(10),ui.dp(12),ui.dp(10));
        ui.fieldSurface(goal,Ui.PALE,Ui.BORDER); ui.add(view,goal); ui.space(view,12);

        LinearLayout price = ui.row(); price.setPadding(ui.dp(14),ui.dp(10),ui.dp(16),ui.dp(10)); ui.fieldSurface(price,Ui.PALE,0);
        price.addView(ui.icon(R.drawable.ic_wallet,22,Ui.INK)); ui.gap(price,12);
        ui.weight(price,ui.text("Học phí",16,Ui.INK,false)); price.addView(ui.text(Tutor.money(lesson.total()),19,Ui.INK,true));
        ui.add(view,price); ui.space(view,18);

        boolean room = lesson.roomOpen() && lesson.online();
        LinearLayout actions = ui.row();
        ui.weightAction(actions,ui.action("Nhắn tin",R.drawable.ic_chat,Ui.OUTLINE,()->{dialog.dismiss();classroom.message(lesson.studentName);}),46);
        ui.gap(actions,10);
        ui.weightAction(actions,room ? ui.action("Vào phòng học",R.drawable.ic_video,Ui.PRIMARY,()->{dialog.dismiss();classroom.openRoom(lesson);})
                : ui.action("Đóng",0,Ui.PRIMARY,dialog::dismiss),46);
        ui.add(view,actions);
        if (room) {
            TextView close = ui.link("Đóng",15,dialog::dismiss); close.setTextColor(Ui.MUTED); close.setGravity(Gravity.CENTER);
            ui.space(view,6); ui.add(view,close);
        }
        android.widget.ScrollView scroll = new android.widget.ScrollView(classroom.context()); scroll.addView(view);
        dialog.setView(scroll); dialog.show();
    }

    private void propose(Lesson lesson) {
        LocalDate[] date = {LocalDate.parse(lesson.date).plusDays(1)}; int[] hour = {lesson.hour};
        LinearLayout form = ui.column(); form.setPadding(ui.dp(22),ui.dp(8),ui.dp(22),0);
        ui.label(form,"Ngày mới"); TextView dateValue = ui.value(date[0].format(Lesson.DATE));
        ui.add(form,ui.picker(R.drawable.ic_calendar,dateValue,()->{
            DatePickerDialog picker = new DatePickerDialog(classroom.context(),R.style.ThemeOverlay_TMDT_Classroom_DatePicker,(view,year,month,day)->{date[0]=LocalDate.of(year,month+1,day);dateValue.setText(date[0].format(Lesson.DATE));},
                    date[0].getYear(),date[0].getMonthValue()-1,date[0].getDayOfMonth());
            picker.getDatePicker().setMinDate(System.currentTimeMillis()-1000); picker.show();
        }));
        ui.space(form,14);
        TimeRange time = new TimeRange(ui,7,23,lesson.minutes,lesson.minutes,lesson.hour,lesson.minutes,(h,m)->hour[0]=h);
        hour[0] = time.hour(); ui.add(form,time.view("Giờ học mới"));
        ui.space(form,12); ui.label(form,"Lý do đề xuất"); EditText reason = ui.entry("Ví dụ: Gia sư có lịch công tác.",false);
        reason.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(120)}); ui.add(form,reason);
        android.widget.ScrollView scroll = new android.widget.ScrollView(classroom.context()); scroll.addView(form);
        AlertDialog dialog = classroom.ui.dialog().setTitle("Đề nghị đổi lịch").setView(scroll)
                .setNegativeButton("Hủy",null).setPositiveButton("Gửi đề nghị",null).create();
        dialog.setOnShowListener(shown -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String text = reason.getText().toString().trim();
            if (text.isEmpty()) { reason.setError("Vui lòng nhập lý do"); return; }
            String error = classroom.store.propose(lesson,date[0],hour[0],text);
            if (error != null) { classroom.dialog("Chưa gửi được đề nghị",error); return; }
            dialog.dismiss(); classroom.hideKeyboard(); render();
            classroom.notice("Đã gửi đề nghị. Lịch cũ được giữ cho đến khi học viên đồng ý.");
        }));
        dialog.show();
    }
}
