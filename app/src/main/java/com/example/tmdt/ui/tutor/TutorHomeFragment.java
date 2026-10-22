package com.example.tmdt.ui.tutor;

import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.example.tmdt.R;
import com.example.tmdt.ui.common.ScreenFragment;
import com.example.tmdt.ui.common.Ui;
import com.example.tmdt.GroupClass;
import com.example.tmdt.Lesson;
import com.example.tmdt.Tutor;

/** Screen 19 · Trang chủ gia sư: teaching overview, new requests and today's lessons. */
public final class TutorHomeFragment extends ScreenFragment {
    private boolean allRequests;
    private LinearLayout requests;
    private TextView requestsToggle;
    @Override public void onCreate(android.os.Bundle state){
        super.onCreate(state);allRequests=state!=null&&state.getBoolean("all_requests",false);
    }
    @Override public void onSaveInstanceState(android.os.Bundle out){super.onSaveInstanceState(out);out.putBoolean("all_requests",allRequests);}

    @Override protected View build() {
        LinearLayout root = ui.column(); LinearLayout body = ui.page(root);
        LinearLayout top = ui.row(); top.setPadding(0,ui.dp(8),0,ui.dp(8));
        View avatar=ui.accountAvatar(63); avatar.setOnClickListener(v->classroom.show("account"));
        com.example.tmdt.ui.common.Motion.press(avatar);
        top.addView(avatar); ui.gap(top,15);
        LinearLayout hello = ui.column(); ui.add(hello,ui.text("Chào "+givenName(classroom.tutorName())+"!",21,Ui.INK,true)); ui.space(hello,6);
        TextView mode = ui.pill("Chế độ gia sư",14,Ui.BLUE,Ui.PALE);
        ui.clickable(mode,()->classroom.ui.dialog().setTitle("Chuyển về chế độ học viên?")
                .setNegativeButton("Hủy",null).setPositiveButton("Chuyển",(d,w)->classroom.setTutorMode(false)).show());
        mode.setContentDescription("Chế độ gia sư. Chạm để chuyển về chế độ học viên"); hello.addView(mode,ui.lp(-2,27));
        ui.weight(top,hello);
        top.addView(ui.iconButton(R.drawable.ic_bell,Ui.INK,"Thông báo",()->classroom.dialog("Thông báo","Chức năng này hiện chưa khả dụng.")));
        ui.add(body,top); ui.space(body,10);

        List<Lesson> pending = new ArrayList<>(), today = new ArrayList<>();
        Set<String> students = new HashSet<>();
        for (Lesson lesson : classroom.lessons) {
            if (!lesson.tutorId.equals(classroom.me())) continue;
            if (lesson.pending() && !lesson.started()) pending.add(lesson);
            if (lesson.confirmed() && lesson.date.equals(LocalDate.now().toString())) today.add(lesson);
            if (lesson.confirmed()) students.add(lesson.studentId);
        }
        int registrations = 0;
        List<GroupClass.Session> classesToday = new ArrayList<>();
        for (GroupClass item : classroom.store.classes) if (item.tutorId.equals(classroom.me())) {
            registrations += item.pendingCount();
            if (!item.active()) continue;
            students.addAll(item.members);
            for (GroupClass.Session session : item.sessionList()) if (session.date.equals(LocalDate.now())) classesToday.add(session);
        }
        today.sort((a,b)->a.hour-b.hour);

        ui.section(body,"Tổng quan giảng dạy",19); ui.space(body,8);
        LinearLayout tiles = ui.row();
        ui.weight(tiles,tile(R.drawable.ic_mail,String.valueOf(pending.size()+registrations),"Yêu cầu mới",()->{allRequests=true;animateRequests(pending);})); ui.gap(tiles,7);
        ui.weight(tiles,tile(R.drawable.ic_calendar,String.valueOf(today.size()+classesToday.size()),"Buổi hôm nay",()->{classroom.teachingDay=LocalDate.now();classroom.show("teaching");})); ui.gap(tiles,7);
        ui.weight(tiles,tile(R.drawable.ic_users,String.valueOf(students.size()),"Học viên",()->classroom.show("classes")));
        ui.add(body,tiles); ui.space(body,20);
        View searchRequests=ui.action(getString(R.string.request_search_title),R.drawable.ic_field_note,Ui.PRIMARY,
                ()->((com.example.tmdt.MainActivity)requireActivity()).openRequestSearch("tutorHome"));
        searchRequests.setId(R.id.search_tutor_requests);
        ui.addAction(body,searchRequests,50);ui.space(body,18);
        replies(body);

        requestsToggle=sectionTitle(body,"Yêu cầu mới",()->{allRequests=!allRequests;animateRequests(pending);});
        requests = ui.column(); ui.add(body,requests); renderRequests(pending); ui.space(body,10);

        sectionTitle(body,"Lịch dạy hôm nay",()->{classroom.teachingDay=LocalDate.now();classroom.show("teaching");});
        for (Lesson lesson : today) { ui.add(body,todayCard(lesson)); ui.space(body,10); }
        for (GroupClass.Session session : classesToday) { ui.add(body,classTodayCard(session.groupClass())); ui.space(body,10); }
        if (today.isEmpty() && classesToday.isEmpty()) ui.add(body,ui.note(R.drawable.ic_info,"Hôm nay chưa có buổi dạy nào.",Ui.PALE,Ui.BLUE,Ui.INK));
        return root;
    }

    /** Answers to reschedule proposals not dismissed yet; tapping one opens that day of the teaching schedule. */
    private void replies(LinearLayout body) {
        List<Lesson> answers = new ArrayList<>();
        for (Lesson lesson : classroom.lessons)
            if (lesson.tutorId.equals(classroom.me()) && lesson.replied() && !classroom.replySeen(lesson)) answers.add(lesson);
        if (answers.isEmpty()) return;
        ui.section(body,"Phản hồi đổi lịch",19); ui.space(body,6);
        for (Lesson lesson : answers) {
            LinearLayout note = ui.note(lesson.kept()?R.drawable.ic_clock:R.drawable.ic_calendar,lesson.replyText(),
                    lesson.kept()?0xFFFFE9EC:Ui.GREEN_BG,lesson.kept()?Ui.RED:Ui.GREEN,Ui.INK);
            ui.gap(note,8); note.addView(ui.link("Đã xem",14,()->{classroom.markReplySeen(lesson);classroom.show("tutorHome");}));
            ui.clickable(note,()->{classroom.teachingDay=LocalDate.parse(lesson.date);classroom.teachingMonth=false;classroom.show("teaching");});
            ui.add(body,note); ui.space(body,8);
        }
        ui.space(body,12);
    }

    private static String givenName(String name) {
        int space = name.trim().indexOf(' ');
        return space < 0 ? name.trim() : name.trim().substring(space+1);
    }

    private TextView sectionTitle(LinearLayout body, String title, Runnable all) {
        LinearLayout row = ui.row(); ui.weight(row,ui.text(title,19,Ui.INK,true));
        TextView link=ui.link("Xem tất cả",14,all);row.addView(link); ui.add(body,row); ui.space(body,4);return link;
    }

    private View tile(int icon, String value, String label, Runnable click) {
        LinearLayout tile = ui.column(); tile.setGravity(Gravity.CENTER_HORIZONTAL); tile.setPadding(0,ui.dp(14),0,ui.dp(10));
        tile.setMinimumHeight(ui.dp(109)); ui.cardSurface(tile,Ui.PALE,Ui.BORDER);
        tile.addView(ui.icon(icon,27,Ui.BLUE)); ui.space(tile,6); tile.addView(ui.text(value,24,Ui.INK,true)); ui.space(tile,6);
        TextView text = ui.text(label,15,Ui.INK,false); text.setGravity(Gravity.CENTER); tile.addView(text);
        tile.setContentDescription(value+" "+label); ui.clickable(tile,click); return tile;
    }

    /** Lesson requests first, then class registrations; shows one card until "Xem tất cả" is chosen. */
    private void animateRequests(List<Lesson> pending) {
        com.example.tmdt.ui.common.Motion.change((android.view.ViewGroup)requests.getParent());
        renderRequests(pending);
    }
    private void renderRequests(List<Lesson> pending) {
        requestsToggle.setText(allRequests?"Thu gọn":"Xem tất cả");
        requestsToggle.setContentDescription(allRequests?"Thu gọn yêu cầu mới":"Xem tất cả yêu cầu mới");
        requests.removeAllViews(); int shown = 0;
        for (Lesson lesson : pending) {
            if (!allRequests && shown == 1) break;
            ui.add(requests,requestCard(lesson.studentId,lesson.studentName,lesson.title,
                    lesson.dateLabel()+" · "+lesson.timeLabel(),"Chờ xác nhận","Xem yêu cầu",()->respond(lesson)));
            ui.space(requests,10); shown++;
        }
        for (GroupClass item : classroom.store.classes) {
            if (!item.tutorId.equals(classroom.me())) continue;
            for (GroupClass.Registration registration : item.registrations) {
                if (!registration.pending() || (!allRequests && shown == 1)) continue;
                ui.add(requests,requestCard(registration.studentId,registration.studentName,"Đăng ký lớp "+item.title,
                        "Gửi ngày "+LocalDate.parse(registration.date).format(Lesson.DATE),"Chờ duyệt","Duyệt đăng ký",()->classroom.openClass(item,"review")));
                ui.space(requests,10); shown++;
            }
        }
        if (shown == 0) { ui.add(requests,ui.note(R.drawable.ic_info,"Chưa có yêu cầu mới.",Ui.PALE,Ui.BLUE,Ui.INK)); ui.space(requests,10); }
    }

    private View requestCard(String userId, String name, String title, String when, String status, String action, Runnable click) {
        LinearLayout card = ui.bordered(); card.setPadding(ui.dp(9),ui.dp(9),ui.dp(9),ui.dp(11));
        LinearLayout top = ui.row(); top.setGravity(Gravity.TOP);
        top.addView(ui.avatar(userId,name,70)); ui.gap(top,13);
        LinearLayout info = ui.column(); ui.add(info,ui.text(name,18,Ui.INK,true)); ui.space(info,5);
        ui.add(info,ui.text(title,15,Ui.MUTED,false)); ui.space(info,2); ui.add(info,ui.text(when,14,Ui.MUTED,false)); ui.space(info,5);
        info.addView(ui.pill(status,14,Ui.ORANGE,Ui.ORANGE_BG),ui.lp(-2,26)); ui.weight(top,info);
        ui.add(card,top); ui.space(card,12); ui.addAction(card,ui.action(action,0,Ui.OUTLINE,click),39);
        return card;
    }

    private View todayCard(Lesson lesson) {
        LinearLayout card = ui.row(); card.setGravity(Gravity.TOP); card.setPadding(ui.dp(9),ui.dp(9),ui.dp(9),ui.dp(12));
        ui.cardSurface(card,Ui.WHITE,Ui.BORDER);
        card.addView(ui.avatar(lesson.studentId,lesson.studentName,69)); ui.gap(card,13);
        LinearLayout info = ui.column(); ui.add(info,ui.text(lesson.studentName,18,Ui.INK,true)); ui.space(info,5);
        ui.add(info,ui.text(lesson.title,15,Ui.MUTED,false)); ui.space(info,2);
        ui.add(info,ui.text(lesson.dateLabel()+" · "+lesson.timeLabel(),14,Ui.MUTED,false)); ui.space(info,8);
        LinearLayout row = ui.row(); row.addView(ui.pill(lesson.mode,14,Ui.GREEN,Ui.GREEN_BG),ui.lp(-2,28)); ui.gap(row,10);
        row.addView(ui.action("Chi tiết",0,Ui.PRIMARY,()->TeachingScheduleFragment.details(classroom,lesson)),ui.lp(105,34));
        ui.add(info,row); ui.weight(card,info); return card;
    }

    private View classTodayCard(GroupClass item) {
        LinearLayout card = ui.row(); card.setGravity(Gravity.TOP); card.setPadding(ui.dp(9),ui.dp(9),ui.dp(9),ui.dp(12));
        ui.cardSurface(card,Ui.WHITE,Ui.BORDER);
        card.addView(ui.photo(item.art(),null,69,73,12)); ui.gap(card,13);
        LinearLayout info = ui.column(); ui.add(info,ui.text(item.title,18,Ui.INK,true)); ui.space(info,5);
        ui.add(info,ui.text("Lớp nhóm · "+item.members.size()+"/"+item.capacity+" học viên",15,Ui.MUTED,false)); ui.space(info,2);
        ui.add(info,ui.text(LocalDate.now().format(Lesson.DATE)+" · "+Lesson.range(item.hour,item.minutes),14,Ui.MUTED,false)); ui.space(info,8);
        LinearLayout row = ui.row(); row.addView(ui.pill(item.mode,14,Ui.GREEN,Ui.GREEN_BG),ui.lp(-2,28)); ui.gap(row,10);
        row.addView(ui.action("Xem lớp",0,Ui.PRIMARY,()->classroom.openClass(item,"review")),ui.lp(105,34));
        ui.add(info,row); ui.weight(card,info); return card;
    }

    /** Stand-in for screen 20 · Yêu cầu học (member 2), so booking requests can be answered in the demo. */
    private void respond(Lesson lesson) {
        LinearLayout view = ui.column(); view.setPadding(ui.dp(20),ui.dp(20),ui.dp(20),ui.dp(18));
        LinearLayout top = ui.row();
        top.addView(ui.avatar(lesson.studentId,lesson.studentName,56)); ui.gap(top,14);
        LinearLayout who = ui.column(); ui.add(who,ui.text(lesson.studentName,20,Ui.INK,true)); ui.space(who,6);
        who.addView(ui.pill(lesson.trial?"Yêu cầu học thử":"Yêu cầu đặt lịch",13,Ui.ORANGE,Ui.ORANGE_BG),ui.lp(-2,-2));
        ui.weight(top,who); ui.add(view,top); ui.space(view,16);

        LinearLayout table = ui.table();
        ui.tableRow(table,R.drawable.ic_file,"Môn học",lesson.title);
        ui.tableRow(table,R.drawable.ic_calendar,"Ngày học",lesson.dateLabel());
        ui.tableRow(table,R.drawable.ic_clock,"Thời gian",lesson.timeLabel());
        ui.tableRow(table,lesson.online()?R.drawable.ic_video:R.drawable.ic_home_soft,"Hình thức",lesson.mode);
        if (!lesson.address.isEmpty()) ui.tableRow(table,R.drawable.ic_pin,"Địa chỉ",lesson.address);
        ui.add(view,table); ui.space(view,14);

        ui.label(view,"Mục tiêu buổi học");
        TextView goal = ui.text(lesson.goal,15,Ui.INK,false); goal.setPadding(ui.dp(12),ui.dp(10),ui.dp(12),ui.dp(10));
        ui.fieldSurface(goal,Ui.PALE,Ui.BORDER); ui.add(view,goal); ui.space(view,12);

        LinearLayout price = ui.row(); price.setPadding(ui.dp(14),ui.dp(10),ui.dp(16),ui.dp(10)); ui.fieldSurface(price,Ui.PALE,0);
        price.addView(ui.icon(R.drawable.ic_wallet,22,Ui.INK)); ui.gap(price,12);
        ui.weight(price,ui.text("Học phí",16,Ui.INK,false)); price.addView(ui.text(Tutor.money(lesson.total()),19,Ui.INK,true));
        ui.add(view,price); ui.space(view,18);

        ScrollView scroll = new ScrollView(classroom.context()); scroll.addView(view);
        AlertDialog dialog = ui.dialog().setView(scroll).create();
        LinearLayout actions = ui.row();
        ui.weightAction(actions,ui.action("Từ chối",0,Ui.DANGER,()->{dialog.dismiss();finish(classroom.store.rejectRequest(lesson),"Đã từ chối yêu cầu.");}),46);
        ui.gap(actions,10);
        ui.weightAction(actions,ui.action("Chấp nhận",0,Ui.PRIMARY,()->{dialog.dismiss();finish(classroom.store.acceptRequest(lesson),"Đã chấp nhận. Buổi học được thêm vào lịch dạy.");}),46);
        ui.add(view,actions); ui.space(view,6);
        TextView later = ui.link("Để sau",15,dialog::dismiss); later.setTextColor(Ui.MUTED); later.setGravity(Gravity.CENTER); ui.add(view,later);
        dialog.show();
    }
    private void finish(String error, String done) {
        if (error != null) { classroom.dialog("Không thể xử lý yêu cầu",error); return; }
        classroom.show("tutorHome"); classroom.notice(done);
    }
}
