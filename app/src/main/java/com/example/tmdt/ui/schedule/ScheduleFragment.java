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

/** Screen 04 · Lịch học (UC24): the student's upcoming, pending and past lessons. */
public final class ScheduleFragment extends ScreenFragment {

    @Override protected View build() {
        LinearLayout root=ui.column();ui.title(root,"Lịch học",classroom.isTutor()?ui.link("Chế độ gia sư",15,this::tutorMode):null);
        ui.tabs(root,new String[]{"Sắp tới","Chờ xác nhận","Đã học"},classroom.scheduleTab,index->{classroom.scheduleTab=index;classroom.show("schedule");});
        LinearLayout content=ui.page(root);ui.space(content,6);
        int tab=classroom.scheduleTab;
        if(tab==1){
            TextView history=ui.link("Xem lịch sử đăng ký lớp học  ›",15,classroom::openRegistrations);
            history.setGravity(android.view.Gravity.END|android.view.Gravity.CENTER_VERTICAL);ui.add(content,history);ui.space(content,2);
        }
        List<Entry> visible=new ArrayList<>();
        for(Lesson lesson:classroom.lessons) {
            if(!lesson.studentId.equals(classroom.me()))continue;
            boolean include=tab==1?lesson.pending()||Lesson.REJECTED.equals(lesson.status):
                    tab==2?(lesson.confirmed()&&lesson.ended())||Lesson.CANCELLED.equals(lesson.status):
                    lesson.confirmed()&&!lesson.ended();
            if(include)visible.add(new Entry(lesson.start(),lessonCard(lesson)));
        }
        // Group classes: registrations wait under "Chờ xác nhận"; members see every session under "Sắp tới" or "Đã học".
        for(GroupClass item:classroom.store.classes) {
            if(tab==1) {
                for(GroupClass.Registration registration:item.registrations) {
                    if(!registration.studentId.equals(classroom.me())||!(registration.pending()||Lesson.REJECTED.equals(registration.status)))continue;
                    boolean waiting=registration.pending();
                    String sent=LocalDate.parse(registration.date).format(Lesson.DATE);
                    // Registrations still waiting when the class stopped recruiting are closed without the tutor's answer.
                    String note=waiting?"Đăng ký gửi ngày "+sent+".":item.cancelled()?"Lớp đã bị gia sư hủy nên đăng ký gửi ngày "+sent+" không được duyệt.":GroupClass.OPEN.equals(item.status)?"Gia sư đã từ chối đăng ký gửi ngày "+sent+"."
                            :"Lớp đã đóng tuyển sinh nên đăng ký gửi ngày "+sent+" không được duyệt.";
                    visible.add(new Entry(LocalDate.parse(item.startDate).atTime(item.hour,item.minute),classCard(item,"Khai giảng "+item.startLabel()+" · "+item.scheduleLabel(),
                            waiting?ui.pill("Chờ duyệt",14,Ui.ORANGE,Ui.ORANGE_BG):ui.pill("Bị từ chối",14,Ui.MUTED,0xFFEEF2F7),note)));
                }
            } else if(item.members.contains(classroom.me())) {
                if(item.cancelled()) {
                    if(tab==0&&!item.over())visible.add(new Entry(LocalDate.parse(item.startDate).atTime(item.hour,item.minute),classCard(item,item.scheduleLabel(),
                            ui.pill("Đã hủy",14,Ui.RED,0xFFFFE9EC),"Gia sư đã hủy lớp. Lý do: "+item.cancelReason+".")));
                    continue;
                }
                for(GroupClass.Session session:item.sessionList()) {
                    if(tab==2?!session.ended():session.ended()||!item.active())continue;
                    visible.add(new Entry(session.start(),classCard(item,session.label(),
                            ui.pill("Buổi "+session.number+"/"+item.sessions,14,tab==2?Ui.MUTED:Ui.GREEN,tab==2?0xFFEEF2F7:Ui.GREEN_BG),
                            "Buổi "+session.number+" trên tổng số "+item.sessions+" buổi.",item.online()&&session.roomOpen(),tab==0?moveNote(item,session):null)));
                }
            }
        }
        Comparator<Entry> order=Comparator.comparing(entry->entry.start);
        visible.sort(tab==2?order.reversed():order);
        for(Entry entry:visible){ui.add(content,entry.view);ui.space(content,11);}
        if(visible.isEmpty()) {
            LinearLayout empty=ui.column();ui.pad(empty,18,32);empty.setGravity(Gravity.CENTER);ui.cardSurface(empty,0xFFF3F8FE,0);
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
        LinearLayout card=ui.bordered();card.setPadding(ui.dp(10),ui.dp(10),ui.dp(10),ui.dp(10));
        LinearLayout row=ui.row();row.setGravity(Gravity.TOP);
        row.addView(ui.avatar(lesson.tutorId,lesson.tutorName,73));ui.gap(row,14);
        LinearLayout details=ui.column();ui.space(details,4);
        LinearLayout titleRow=ui.row();titleRow.setGravity(Gravity.TOP);
        TextView title=ui.text(lesson.title,lesson.title.length()>14?14:17,Ui.INK,true);ui.weight(titleRow,title);ui.gap(titleRow,4);
        titleRow.addView(ui.lessonStatus(lesson));ui.add(details,titleRow);ui.space(details,2);
        TextView teacher=ui.text(lesson.tutorName,15,Ui.MUTED,false);ui.clickable(teacher,()->classroom.openTutor(lesson.tutorId,lesson.tutorName));ui.add(details,teacher);ui.space(details,8);
        ui.add(details,ui.fact(R.drawable.ic_calendar,lesson.dateLabel()+" · "+lesson.timeLabel(),18,14,Ui.MUTED,false));
        ui.add(details,ui.fact(lesson.mode.equals("Tại nhà")?R.drawable.ic_home_soft:R.drawable.ic_video,lesson.mode+(lesson.trial?" · Học thử":""),18,15,Ui.MUTED,false));
        ui.weight(row,details);ui.add(card,row);
        if(lesson.proposal()&&lesson.confirmed()) {
            ui.space(card,8);LinearLayout proposal=ui.note(R.drawable.ic_clock,"Gia sư đề nghị đổi lịch. Chạm để xem.",Ui.ORANGE_BG,Ui.BLUE,Ui.ORANGE);
            ui.clickable(proposal,()->{classroom.lessonId=lesson.id;classroom.show("reschedule");});ui.add(card,proposal);
        }
        ui.space(card,9);
        LinearLayout actions=ui.row();
        ui.weightAction(actions,ui.action("Nhắn tin",0,Ui.OUTLINE,()->classroom.message(lesson.tutorName)),38);ui.gap(actions,9);
        ui.weightAction(actions,ui.action(lesson.needsConfirmation()?"Xác nhận":"Chi tiết",0,Ui.PRIMARY,()->classroom.openLesson(lesson)),38);
        ui.add(card,actions);return card;
    }

    /** A tutor account also books lessons as a student; this switches to its teaching side. */
    private void tutorMode() {
        classroom.ui.dialog().setTitle("Chuyển sang chế độ gia sư?")
                .setMessage("Bạn sẽ chuyển sang giao diện gia sư để duyệt yêu cầu, xem lịch dạy và quản lý lớp nhóm.")
                .setNegativeButton("Hủy",null).setPositiveButton("Chuyển",(d,w)->classroom.setTutorMode(true)).show();
    }

    /** A group class session or registration, laid out like a lesson card; "Chi tiết" summarises the class. */
    private View classCard(GroupClass item, String when, TextView status, String note) { return classCard(item,when,status,note,false,null); }
    private View classCard(GroupClass item, String when, TextView status, String note, boolean live, View extra) {
        LinearLayout card=ui.bordered();card.setPadding(ui.dp(10),ui.dp(10),ui.dp(10),ui.dp(10));
        LinearLayout row=ui.row();row.setGravity(Gravity.TOP);
        row.addView(ui.photo(item.art(),null,73,77,12));ui.gap(row,14);
        LinearLayout details=ui.column();ui.space(details,4);
        LinearLayout titleRow=ui.row();titleRow.setGravity(Gravity.TOP);
        TextView title=ui.text(item.title,item.title.length()>14?14:17,Ui.INK,true);ui.weight(titleRow,title);ui.gap(titleRow,4);
        titleRow.addView(status);ui.add(details,titleRow);ui.space(details,2);
        TextView teacher=ui.text(item.tutorName,15,Ui.MUTED,false);ui.clickable(teacher,()->classroom.openTutor(item.tutorId,item.tutorName));ui.add(details,teacher);ui.space(details,8);
        ui.add(details,ui.fact(R.drawable.ic_calendar,when,18,14,Ui.MUTED,false));
        ui.add(details,ui.fact(R.drawable.ic_users,item.mode+" · Lớp nhóm",18,15,Ui.MUTED,false));
        ui.weight(row,details);ui.add(card,row);
        if(extra!=null){ui.space(card,8);ui.add(card,extra);}
        ui.space(card,9);
        String summary=note+"\n\n"+item.subject+" · "+item.level+"\nLịch học: "+item.scheduleLabel()+"\nKhai giảng: "+item.startLabel()+" · "+item.sessions+" buổi\n"
                +item.mode+(item.address.isEmpty()?"":" · "+item.address)+(item.online()?"\nPhòng học: "+(item.roomLink.isEmpty()?"phòng Jitsi của app":item.roomHost()):"")+"\nSĩ số: "+item.members.size()+"/"+item.capacity+"\nHọc phí: "+item.priceLabel();
        LinearLayout actions=ui.row();
        ui.weightAction(actions,ui.action("Nhắn tin",0,Ui.OUTLINE,()->classroom.message(item.tutorName)),38);ui.gap(actions,9);
        ui.weightAction(actions,live?ui.action("Vào phòng học",R.drawable.ic_video,Ui.PRIMARY,()->classroom.openClassRoom(item))
                :ui.action("Chi tiết",0,Ui.PRIMARY,item.cancelled()?()->classroom.openRegistration(item):()->classroom.dialog(item.title,summary)),38);
        ui.add(card,actions);return card;
    }

    /** The tutor's proposal for this session and the student's answer, or where a moved session came from. */
    private View moveNote(GroupClass item,GroupClass.Session session) {
        if(item.proposalOpen()&&item.proposal.number==session.number) {
            String answer=item.proposal.answers.get(classroom.me()),target=item.proposal.move.label(item.minutes);
            LinearLayout note=GroupClass.Proposal.JOIN.equals(answer)
                    ?ui.note(R.drawable.ic_calendar,"Bạn tham gia được giờ mới "+target+".\nChạm để xem hoặc đổi câu trả lời.",Ui.PALE,Ui.BLUE,Ui.INK)
                    :GroupClass.Proposal.BUSY.equals(answer)
                    ?ui.note(R.drawable.ic_info,"Bạn đã báo bận giờ mới "+target+".\nBuổi học vẫn giữ giờ cũ. Chạm để đổi câu trả lời.",0xFFFFE9EC,Ui.RED,Ui.INK)
                    :ui.note(R.drawable.ic_clock,"Gia sư đề nghị đổi buổi này sang "+target+".\nChạm để trả lời.",Ui.ORANGE_BG,Ui.BLUE,Ui.ORANGE);
            ui.clickable(note,()->answerMove(item));return note;
        }
        if(!session.moved())return null;
        return ui.note(R.drawable.ic_calendar,"Đã đổi lịch từ "+session.plannedLabel()+".\nLý do: "+session.move.reason,Ui.GREEN_BG,Ui.GREEN,Ui.INK);
    }

    /** Both slots, the reason and the deadline; the student answers whether they can come at the new time. */
    private void answerMove(GroupClass item) {
        if(!item.proposalOpen()){classroom.show("schedule");return;}
        GroupClass.Proposal proposal=item.proposal;GroupClass.Session session=item.sessionList().get(proposal.number-1);
        String answer=proposal.answers.get(classroom.me()),clash=classroom.store.moveConflict(item);
        LinearLayout view=ui.column();view.setPadding(ui.dp(20),ui.dp(20),ui.dp(20),ui.dp(18));
        LinearLayout top=ui.row();top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(ui.photo(item.art(),null,52,52,12));ui.gap(top,14);
        LinearLayout heading=ui.column();
        TextView title=ui.text(item.title,19,Ui.INK,true);title.setSingleLine(true);title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        ui.add(heading,title);ui.space(heading,4);ui.add(heading,ui.text("Buổi "+proposal.number+"/"+item.sessions+" · "+item.tutorName,14,Ui.MUTED,false));
        ui.weight(top,heading);ui.add(view,top);ui.space(view,16);
        ui.add(view,RescheduleFragment.slot(ui,"Lịch hiện tại",session.label(),Ui.PALE,Ui.INK));ui.space(view,10);
        ui.add(view,RescheduleFragment.slot(ui,"Lịch đề xuất",proposal.move.label(item.minutes),Ui.GREEN_BG,Ui.GREEN));ui.space(view,14);
        ui.label(view,"Lý do đổi lịch");
        TextView reason=ui.text(proposal.move.reason,15,Ui.INK,false);reason.setPadding(ui.dp(12),ui.dp(10),ui.dp(12),ui.dp(10));
        ui.fieldSurface(reason,Ui.PALE,Ui.BORDER);ui.add(view,reason);ui.space(view,12);
        if(clash!=null){ui.add(view,ui.note(R.drawable.ic_info,"Giờ đề xuất trùng với "+clash+" của bạn.",0xFFFFE9EC,Ui.RED,Ui.INK));ui.space(view,10);}
        ui.add(view,ui.note(R.drawable.ic_clock,"Trả lời trước "+proposal.deadlineLabel()+".\nKhông ai báo bận thì buổi học chuyển sang giờ mới.",Ui.PALE,Ui.BLUE,Ui.MUTED));
        if(answer!=null){
            ui.space(view,10);
            ui.add(view,ui.fact(GroupClass.Proposal.JOIN.equals(answer)?R.drawable.ic_calendar:R.drawable.ic_info,
                    "Bạn đã trả lời: "+(GroupClass.Proposal.JOIN.equals(answer)?"Tham gia được":"Bận"),18,14,Ui.MUTED,false));
        }
        ui.space(view,16);
        AlertDialog dialog=ui.dialog().create();
        LinearLayout actions=ui.row();
        ui.weightAction(actions,ui.action("Báo bận",0,Ui.OUTLINE,()->{dialog.dismiss();answer(item,false);}),46);ui.gap(actions,10);
        ui.weightAction(actions,ui.action("Tham gia được",0,Ui.PRIMARY,()->{dialog.dismiss();answer(item,true);}),46);
        ui.add(view,actions);
        TextView close=ui.link("Đóng",15,dialog::dismiss);close.setTextColor(Ui.MUTED);close.setGravity(Gravity.CENTER);
        ui.space(view,6);ui.add(view,close);
        android.widget.ScrollView scroll=new android.widget.ScrollView(classroom.context());scroll.addView(view);
        dialog.setView(scroll);dialog.show();
    }
    private void answer(GroupClass item,boolean join) {
        String error=classroom.store.answerMove(item,join);
        if(error!=null){classroom.dialog("Chưa gửi được câu trả lời",error);return;}
        classroom.show("schedule");
        classroom.notice(join?"Đã báo gia sư: bạn tham gia được giờ mới.":"Đã báo bận. Gia sư sẽ giữ giờ cũ hoặc đề nghị giờ khác.");
    }

    /** A card with the time it is sorted by, so lessons and class sessions share one list. */
    private static final class Entry {
        final LocalDateTime start; final View view;
        Entry(LocalDateTime start,View view){this.start=start;this.view=view;}
    }

    /** "Sẵn sàng cho buổi học tiếp theo" banner with the calendar illustration of the design. */
    private View banner() {
        LinearLayout banner=ui.row();banner.setPadding(ui.dp(16),ui.dp(16),ui.dp(7),ui.dp(10));banner.setMinimumHeight(ui.dp(160));ui.cardSurface(banner,Ui.PALE,0);
        LinearLayout text=ui.column();ui.add(text,ui.text("Sẵn sàng cho buổi học tiếp theo",21,Ui.INK,true));ui.space(text,8);
        ui.add(text,ui.text("Xem lại mục tiêu trước khi bắt đầu.",16,Ui.MUTED,false));ui.weight(banner,text);
        banner.addView(ui.art(R.drawable.anh_lich_hoc,126,126));
        return banner;
    }
}
