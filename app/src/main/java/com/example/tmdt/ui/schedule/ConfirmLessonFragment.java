package com.example.tmdt.ui.schedule;

import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.tmdt.R;
import com.example.tmdt.ui.common.ScreenFragment;
import com.example.tmdt.ui.common.Ui;
import com.example.tmdt.Lesson;
import com.example.tmdt.Store;
import com.example.tmdt.Tutor;

/** Screen 42 · Xác nhận hoàn thành (UC25). Only the lesson's student can confirm, once, after the lesson ended. */
public final class ConfirmLessonFragment extends ScreenFragment {
    private Lesson lesson;
    private CheckBox agree;

    @Override protected View build() {
        lesson = classroom.store.lesson(classroom.lessonId);
        Tutor tutor = Tutor.get(lesson.tutorId);
        LinearLayout root = ui.column(); ui.header(root,"Xác nhận hoàn thành",classroom::back,null);
        LinearLayout body = ui.page(root);

        LinearLayout card = ui.row(); card.setGravity(android.view.Gravity.TOP); card.setPadding(ui.dp(9),ui.dp(9),ui.dp(9),ui.dp(12));
        ui.surface(card,Ui.WHITE,10,Ui.BORDER);
        card.addView(ui.photo(Store.tutorPhoto(tutor.id),"Ảnh gia sư "+tutor.name,100,107,9)); ui.gap(card,13);
        LinearLayout info = ui.column(); ui.space(info,3); ui.add(info,ui.text(tutor.name,18,Ui.INK,true)); ui.space(info,6);
        ui.add(info,ui.text(tutor.subject+" · "+tutor.level,15,Ui.MUTED,false)); ui.space(info,14);
        ui.add(info,ui.text("▦ "+lesson.dateLabel()+" · "+lesson.timeLabel(),15,Ui.INK,false)); ui.space(info,10);
        info.addView(lesson.finished ? ui.pill("✓ Đã xác nhận hoàn thành",14,Ui.GREEN,Ui.GREEN_BG)
                : ui.pill("◷ Chờ học viên xác nhận",14,Ui.ORANGE,Ui.ORANGE_BG), ui.lp(-1,33));
        ui.weight(card,info); ui.add(body,card); ui.space(body,18);

        ui.label(body,"Nội dung đã học");
        TextView content = ui.text(lesson.content.isEmpty()?lesson.goal:lesson.content,16,Ui.INK,false);
        content.setPadding(ui.dp(12),ui.dp(12),ui.dp(12),ui.dp(12)); content.setMinHeight(ui.dp(61)); ui.surface(content,Ui.PALE,10,Ui.BORDER);
        ui.add(body,content); ui.space(body,12);

        LinearLayout tiles = ui.row();
        ui.weight(tiles,tile(R.drawable.ic_clock,"Thời lượng",lesson.minutes+" phút")); ui.gap(tiles,9);
        ui.weight(tiles,tile(R.drawable.ic_coin,"Học phí buổi học",Tutor.money(lesson.total())));
        ui.add(body,tiles); ui.space(body,16);

        ui.add(body,ui.note(R.drawable.ic_info,"Xác nhận hoàn thành sẽ cho phép giải ngân học phí buổi này cho gia sư.",Ui.PALE,Ui.BLUE,Ui.MUTED));
        ui.space(body,8);
        if (lesson.finished) {
            ui.addAction(body,ui.action("Đã xác nhận hoàn thành",0,Ui.DISABLED,null),47);
        } else if (!lesson.ended()) {
            ui.addAction(body,ui.action("Buổi học chưa kết thúc",0,Ui.DISABLED,null),47);
        } else {
            agree = ui.check("Tôi xác nhận buổi học đã diễn ra.",false,null); ui.add(body,agree); ui.space(body,4);
            ui.addAction(body,ui.action("Xác nhận hoàn thành",0,Ui.PRIMARY,this::confirm),47);
        }
        ui.space(body,12);
        ui.addAction(body,ui.action("Báo cáo vấn đề",0,Ui.OUTLINE,()->classroom.dialog("Báo cáo vấn đề",
                "Màn hình Báo cáo vi phạm (47) do Thành viên 4 phụ trách. Học phí của buổi này được giữ lại cho đến khi báo cáo được xử lý.")),46);
        return root;
    }

    private View tile(int icon, String label, String value) {
        LinearLayout tile = ui.row(); tile.setPadding(ui.dp(14),ui.dp(10),ui.dp(8),ui.dp(10)); tile.setMinimumHeight(ui.dp(70));
        ui.surface(tile,Ui.PALE,10,Ui.BORDER); tile.addView(ui.icon(icon,26,Ui.INK)); ui.gap(tile,13);
        LinearLayout text = ui.column(); ui.add(text,ui.text(label,14,Ui.MUTED,false)); ui.space(text,4);
        ui.add(text,ui.text(value,20,Ui.INK,true)); ui.weight(tile,text); return tile;
    }

    private void confirm() {
        if (!agree.isChecked()) { classroom.dialog("Chưa xác nhận","Hãy đánh dấu \"Tôi xác nhận buổi học đã diễn ra\" trước khi tiếp tục."); return; }
        String error = classroom.store.confirmFinished(lesson, Store.STUDENT);
        if (error != null) { classroom.dialog("Không thể xác nhận",error); return; }
        classroom.show("lesson");
        classroom.dialog("Đã xác nhận hoàn thành","Học phí "+Tutor.money(lesson.total())+" của buổi này sẽ được giải ngân cho gia sư (màn hình 43 do Thành viên 4 phụ trách). Bạn có thể đánh giá gia sư sau buổi học.");
    }
}
