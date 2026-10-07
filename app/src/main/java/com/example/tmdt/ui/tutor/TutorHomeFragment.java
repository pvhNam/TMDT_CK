package com.example.tmdt.ui.tutor;

import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
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

    @Override protected View build() {
        LinearLayout root = ui.column(); LinearLayout body = ui.page(root);
        LinearLayout top = ui.row(); top.setPadding(0,ui.dp(8),0,ui.dp(8));
        top.addView(ui.photo(0,"Ảnh gia sư "+classroom.tutorName(),63,63,32)); ui.gap(top,15);
        LinearLayout hello = ui.column(); ui.add(hello,ui.text("Chào "+classroom.tutorName()+"!",21,Ui.INK,true)); ui.space(hello,6);
        TextView mode = ui.pill("Chế độ gia sư",14,Ui.BLUE,Ui.PALE);
        ui.clickable(mode,()->new AlertDialog.Builder(classroom.context()).setTitle("Chuyển về chế độ học viên?")
                .setNegativeButton("Hủy",null).setPositiveButton("Chuyển",(d,w)->classroom.setTutorMode(false)).show());
        mode.setContentDescription("Chế độ gia sư. Chạm để chuyển về chế độ học viên"); hello.addView(mode,ui.lp(-2,27));
        ui.weight(top,hello);
        FrameLayout bell = new FrameLayout(classroom.context());
        bell.addView(ui.iconButton(R.drawable.ic_bell,Ui.INK,"Thông báo, có thông báo mới",()->classroom.dialog("Thông báo","Màn hình Thông báo (11) do Thành viên 4 phụ trách.")));
        View dot = new View(classroom.context()); ui.surface(dot,Ui.RED,6,0);
        FrameLayout.LayoutParams dotParams = new FrameLayout.LayoutParams(ui.dp(7),ui.dp(7),Gravity.TOP|Gravity.END); dotParams.setMargins(0,ui.dp(12),ui.dp(12),0);
        bell.addView(dot,dotParams); top.addView(bell); ui.add(body,top); ui.space(body,10);

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

        ui.section(body,"Tổng quan giảng dạy",24); ui.space(body,8);
        LinearLayout tiles = ui.row();
        ui.weight(tiles,tile(R.drawable.ic_mail,String.valueOf(pending.size()+registrations),"Yêu cầu mới",()->{allRequests=true;renderRequests(pending);})); ui.gap(tiles,7);
        ui.weight(tiles,tile(R.drawable.ic_calendar,String.valueOf(today.size()+classesToday.size()),"Buổi hôm nay",()->{classroom.teachingDay=LocalDate.now();classroom.show("teaching");})); ui.gap(tiles,7);
        ui.weight(tiles,tile(R.drawable.ic_users,String.valueOf(students.size()),"Học viên",()->classroom.show("classes")));
        ui.add(body,tiles); ui.space(body,20);

        sectionTitle(body,"Yêu cầu mới",()->{allRequests=!allRequests;renderRequests(pending);});
        requests = ui.column(); ui.add(body,requests); renderRequests(pending); ui.space(body,10);

        sectionTitle(body,"Lịch dạy hôm nay",()->{classroom.teachingDay=LocalDate.now();classroom.show("teaching");});
        for (Lesson lesson : today) { ui.add(body,todayCard(lesson)); ui.space(body,10); }
        for (GroupClass.Session session : classesToday) { ui.add(body,classTodayCard(session.groupClass())); ui.space(body,10); }
        if (today.isEmpty() && classesToday.isEmpty()) ui.add(body,ui.note(R.drawable.ic_info,"Hôm nay chưa có buổi dạy nào.",Ui.PALE,Ui.BLUE,Ui.INK));
        return root;
    }

    private void sectionTitle(LinearLayout body, String title, Runnable all) {
        LinearLayout row = ui.row(); ui.weight(row,ui.text(title,23,Ui.INK,true));
        row.addView(ui.link("Xem tất cả",17,all)); ui.add(body,row); ui.space(body,4);
    }

    private View tile(int icon, String value, String label, Runnable click) {
        LinearLayout tile = ui.column(); tile.setGravity(Gravity.CENTER_HORIZONTAL); tile.setPadding(0,ui.dp(14),0,ui.dp(10));
        tile.setMinimumHeight(ui.dp(109)); ui.surface(tile,Ui.PALE,10,Ui.BORDER);
        tile.addView(ui.icon(icon,27,Ui.BLUE)); ui.space(tile,6); tile.addView(ui.text(value,24,Ui.INK,true)); ui.space(tile,6);
        TextView text = ui.text(label,15,Ui.INK,false); text.setGravity(Gravity.CENTER); tile.addView(text);
        tile.setContentDescription(value+" "+label); ui.clickable(tile,click); return tile;
    }

    /** Lesson requests first, then class registrations; shows one card until "Xem tất cả" is chosen. */
    private void renderRequests(List<Lesson> pending) {
        requests.removeAllViews(); int shown = 0;
        for (Lesson lesson : pending) {
            if (!allRequests && shown == 1) break;
            ui.add(requests,requestCard(0,lesson.studentName,lesson.title,
                    lesson.dateLabel()+" · "+lesson.timeLabel(),"Chờ xác nhận","Xem yêu cầu",()->respond(lesson)));
            ui.space(requests,10); shown++;
        }
        for (GroupClass item : classroom.store.classes) {
            if (!item.tutorId.equals(classroom.me())) continue;
            for (GroupClass.Registration registration : item.registrations) {
                if (!registration.pending() || (!allRequests && shown == 1)) continue;
                ui.add(requests,requestCard(0,registration.studentName,"Đăng ký lớp "+item.title,
                        "Gửi ngày "+LocalDate.parse(registration.date).format(Lesson.DATE),"Chờ duyệt","Duyệt đăng ký",()->classroom.openClass(item,"review")));
                ui.space(requests,10); shown++;
            }
        }
        if (shown == 0) { ui.add(requests,ui.note(R.drawable.ic_info,"Chưa có yêu cầu mới.",Ui.PALE,Ui.BLUE,Ui.INK)); ui.space(requests,10); }
    }

    private View requestCard(int photo, String name, String title, String when, String status, String action, Runnable click) {
        LinearLayout card = ui.bordered(10); card.setPadding(ui.dp(9),ui.dp(9),ui.dp(9),ui.dp(11));
        LinearLayout top = ui.row(); top.setGravity(Gravity.TOP);
        top.addView(ui.photo(photo,"Ảnh "+name,70,74,9)); ui.gap(top,13);
        LinearLayout info = ui.column(); ui.add(info,ui.text(name,18,Ui.INK,true)); ui.space(info,5);
        ui.add(info,ui.text(title,15,Ui.MUTED,false)); ui.space(info,2); ui.add(info,ui.text(when,14,Ui.MUTED,false)); ui.space(info,5);
        info.addView(ui.pill(status,14,Ui.ORANGE,Ui.ORANGE_BG),ui.lp(-2,26)); ui.weight(top,info);
        ui.add(card,top); ui.space(card,12); ui.addAction(card,ui.action(action,0,Ui.OUTLINE,click),39);
        return card;
    }

    private View todayCard(Lesson lesson) {
        LinearLayout card = ui.row(); card.setGravity(Gravity.TOP); card.setPadding(ui.dp(9),ui.dp(9),ui.dp(9),ui.dp(12));
        ui.surface(card,Ui.WHITE,10,Ui.BORDER);
        card.addView(ui.photo(0,"Ảnh "+lesson.studentName,69,73,9)); ui.gap(card,13);
        LinearLayout info = ui.column(); ui.add(info,ui.text(lesson.studentName,18,Ui.INK,true)); ui.space(info,5);
        ui.add(info,ui.text(lesson.title,15,Ui.MUTED,false)); ui.space(info,2);
        ui.add(info,ui.text(lesson.dateLabel()+" · "+lesson.timeLabel(),14,Ui.MUTED,false)); ui.space(info,8);
        LinearLayout row = ui.row(); row.addView(ui.pill(lesson.mode,14,Ui.GREEN,Ui.GREEN_BG),ui.lp(-2,28)); ui.gap(row,10);
        View detail = ui.action("Chi tiết",0,Ui.PRIMARY,()->TeachingScheduleFragment.details(classroom,lesson)); ui.weightAction(row,detail,34);
        ui.add(info,row); ui.weight(card,info); return card;
    }

    private View classTodayCard(GroupClass item) {
        LinearLayout card = ui.row(); card.setGravity(Gravity.TOP); card.setPadding(ui.dp(9),ui.dp(9),ui.dp(9),ui.dp(12));
        ui.surface(card,Ui.WHITE,10,Ui.BORDER);
        card.addView(ui.art(item.smallArt(),69,73)); ui.gap(card,13);
        LinearLayout info = ui.column(); ui.add(info,ui.text(item.title,18,Ui.INK,true)); ui.space(info,5);
        ui.add(info,ui.text("Lớp nhóm · "+item.members.size()+"/"+item.capacity+" học viên",15,Ui.MUTED,false)); ui.space(info,2);
        ui.add(info,ui.text(LocalDate.now().format(Lesson.DATE)+" · "+Lesson.range(item.hour,item.minutes),14,Ui.MUTED,false)); ui.space(info,8);
        LinearLayout row = ui.row(); row.addView(ui.pill(item.mode,14,Ui.GREEN,Ui.GREEN_BG),ui.lp(-2,28)); ui.gap(row,10);
        View detail = ui.action("Xem lớp",0,Ui.PRIMARY,()->classroom.openClass(item,"review")); ui.weightAction(row,detail,34);
        ui.add(info,row); ui.weight(card,info); return card;
    }

    /** Stand-in for screen 20 · Yêu cầu học (member 2), so booking requests can be answered in the demo. */
    private void respond(Lesson lesson) {
        new AlertDialog.Builder(classroom.context()).setTitle("Yêu cầu của "+lesson.studentName)
                .setMessage(lesson.title+"\n"+lesson.dateLabel()+" · "+lesson.timeLabel()+"\n"+lesson.mode+"\n\nMục tiêu: "+lesson.goal+"\nHọc phí: "+Tutor.money(lesson.total()))
                .setNeutralButton("Đóng",null)
                .setNegativeButton("Từ chối",(d,w)->finish(classroom.store.rejectRequest(lesson),"Đã từ chối yêu cầu."))
                .setPositiveButton("Chấp nhận",(d,w)->finish(classroom.store.acceptRequest(lesson),"Đã chấp nhận. Buổi học được thêm vào lịch dạy."))
                .show();
    }
    private void finish(String error, String done) {
        if (error != null) { classroom.dialog("Không thể xử lý yêu cầu",error); return; }
        classroom.show("tutorHome"); classroom.notice(done);
    }
}
