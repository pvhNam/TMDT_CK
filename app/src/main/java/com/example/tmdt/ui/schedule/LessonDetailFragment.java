package com.example.tmdt.ui.schedule;

import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.tmdt.R;
import com.example.tmdt.ui.common.ScreenFragment;
import com.example.tmdt.ui.common.Ui;
import com.example.tmdt.Lesson;
import com.example.tmdt.Store;
import com.example.tmdt.Tutor;

/** Screen 12 · Chi tiết buổi học (UC24): lesson facts, the room button and links to confirm, cancel or reschedule. */
public final class LessonDetailFragment extends ScreenFragment {
    private Lesson lesson;

    @Override protected View build() {
        lesson = activity.store.lesson(activity.lessonId);
        Tutor tutor = Tutor.get(lesson.tutorId);
        LinearLayout root = ui.column(); ui.header(root,"Chi tiết buổi học",activity::back,null);
        LinearLayout body = ui.page(root);

        LinearLayout summary = ui.row(); summary.setPadding(ui.dp(14),ui.dp(18),ui.dp(12),ui.dp(18)); ui.surface(summary,Ui.PALE,10,0);
        LinearLayout heading = ui.column(), titleRow = ui.row();
        TextView title = ui.text(lesson.title,24,Ui.INK,true); title.setMaxLines(2); titleRow.addView(title); ui.gap(titleRow,10);
        titleRow.addView(ui.lessonStatus(lesson)); ui.add(heading,titleRow); ui.space(heading,10);
        ui.add(heading,ui.text(lesson.goal,16,Ui.MUTED,false));
        ui.weight(summary,heading); ui.gap(summary,8); summary.addView(ui.icon(R.drawable.ic_cap,48,Ui.BLUE));
        ui.add(body,summary); ui.space(body,11);

        LinearLayout profile = ui.row(); profile.setPadding(ui.dp(9),ui.dp(9),ui.dp(9),ui.dp(9)); ui.surface(profile,Ui.WHITE,10,Ui.BORDER);
        profile.addView(ui.photo(Store.tutorPhoto(tutor.id),"Ảnh gia sư "+tutor.name,73,79,9)); ui.gap(profile,13);
        LinearLayout info = ui.column(); ui.add(info,ui.text(tutor.name,18,Ui.INK,true)); ui.space(info,6);
        ui.add(info,ui.text(tutor.subject+" · "+tutor.level,15,Ui.MUTED,false));
        info.addView(ui.link("Xem hồ sơ",16,()->activity.openTutor(tutor)),ui.lp(-2,-2));
        ui.weight(profile,info); ui.add(body,profile);

        LinearLayout table = ui.table();
        ui.tableRow(table,R.drawable.ic_calendar,"Ngày học",lesson.dateLabel());
        ui.tableRow(table,R.drawable.ic_clock,"Thời gian",lesson.timeLabel());
        ui.tableRow(table,lesson.mode.equals("Tại nhà")?R.drawable.ic_home:R.drawable.ic_video,"Hình thức",lesson.mode+(lesson.trial?" · Học thử":""));
        if (!lesson.address.isEmpty()) ui.tableRow(table,R.drawable.ic_pin,"Địa chỉ",lesson.address);
        ui.tableRow(table,R.drawable.ic_hourglass,"Thời lượng",lesson.minutes+" phút");
        ui.add(body,table); ui.space(body,12);

        ui.label(body,"Mục tiêu buổi học");
        TextView goal = ui.text(lesson.goal,16,Ui.INK,false); goal.setPadding(ui.dp(12),ui.dp(12),ui.dp(12),ui.dp(12));
        goal.setMinHeight(ui.dp(46)); ui.surface(goal,Ui.PALE,10,Ui.BORDER); ui.add(body,goal); ui.space(body,12);

        LinearLayout price = ui.row(); price.setPadding(ui.dp(13),ui.dp(11),ui.dp(16),ui.dp(11)); ui.surface(price,Ui.PALE,10,0);
        price.addView(ui.icon(R.drawable.ic_wallet,22,Ui.INK)); ui.gap(price,12);
        ui.weight(price,ui.text("Học phí",17,Ui.INK,false)); price.addView(ui.text(Tutor.money(lesson.total()),20,Ui.INK,true));
        ui.add(body,price); ui.space(body,10);

        if (lesson.proposal() && lesson.confirmed()) {
            LinearLayout proposal = ui.note(R.drawable.ic_clock,"Gia sư đề nghị đổi lịch sang "+lesson.proposalLabel()+". Chạm để phản hồi.",Ui.ORANGE_BG,Ui.ORANGE,Ui.ORANGE);
            ui.clickable(proposal,()->activity.show("reschedule")); ui.add(body,proposal); ui.space(body,8);
        }
        ui.add(body,ui.note(R.drawable.ic_info,hint(),Ui.WHITE,Ui.BLUE,Ui.MUTED)); ui.space(body,8);
        ui.addAction(body,mainAction(),49); ui.space(body,12);
        ui.addAction(body,ui.action("Nhắn tin gia sư",R.drawable.ic_chat,Ui.OUTLINE,()->activity.message(tutor)),48);
        if (lesson.active() && !lesson.started()) {
            TextView cancel = ui.link("Hủy buổi học",16,()->activity.show("cancel")); cancel.setTextColor(Ui.RED);
            cancel.setGravity(Gravity.CENTER); ui.space(body,6); ui.add(body,cancel);
        }
        return root;
    }

    private String hint() {
        if (Lesson.CANCELLED.equals(lesson.status)) return "Buổi học đã hủy"+(lesson.cancelReason.isEmpty()?".":" · Lý do: "+lesson.cancelReason+".");
        if (Lesson.REJECTED.equals(lesson.status)) return "Gia sư đã từ chối yêu cầu này. Bạn có thể chọn thời gian khác.";
        if (lesson.pending()) return "Gia sư sẽ xác nhận yêu cầu của bạn.";
        if (lesson.needsConfirmation()) return "Buổi học đã kết thúc. Hãy xác nhận hoàn thành để giải ngân học phí.";
        if (lesson.finished) return "Bạn đã xác nhận buổi học này hoàn thành.";
        return lesson.mode.equals("Tại nhà") ? "Gia sư sẽ đến địa chỉ học đúng giờ đã hẹn." : "Phòng học mở trước giờ bắt đầu 10 phút.";
    }

    /** The button of the design changes with the lesson's state instead of always reading "Chưa đến giờ vào học". */
    private View mainAction() {
        if (Lesson.CANCELLED.equals(lesson.status)) return ui.action("Buổi học đã hủy",0,Ui.DISABLED,null);
        if (Lesson.REJECTED.equals(lesson.status)) return ui.action("Yêu cầu bị từ chối",0,Ui.DISABLED,null);
        if (lesson.pending()) return ui.action("Đang chờ gia sư xác nhận",0,Ui.DISABLED,null);
        if (lesson.finished) return ui.action("Đã xác nhận hoàn thành",0,Ui.DISABLED,null);
        if (lesson.needsConfirmation()) return ui.action("Xác nhận hoàn thành",0,Ui.PRIMARY,()->activity.show("confirm"));
        if (lesson.roomOpen()) return lesson.mode.equals("Tại nhà") ? ui.action("Buổi học đang diễn ra",0,Ui.DISABLED,null)
                : ui.action("Vào phòng học",0,Ui.PRIMARY,()->activity.dialog("Phòng học trực tuyến","Phòng học sẽ mở khi ứng dụng được kết nối máy chủ."));
        return ui.action("Chưa đến giờ vào học",0,Ui.DISABLED,null);
    }
}
