package com.example.tmdt.ui.tutor;

import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import com.example.tmdt.R;
import com.example.tmdt.ui.common.ScreenFragment;
import com.example.tmdt.ui.common.Ui;
import com.example.tmdt.GroupClass;
import com.example.tmdt.Lesson;

/** Screen 36 · Duyệt đăng ký lớp (UC18): accept or reject students without exceeding the class limit. */
public final class ReviewRegistrationsFragment extends ScreenFragment {
    private GroupClass item;
    private boolean newestFirst = true;
    private LinearLayout list;
    private TextView heading, summary, sort;

    @Override protected View build() {
        item = classroom.store.groupClass(classroom.classId);
        LinearLayout root = ui.column(); ui.header(root,"Duyệt đăng ký lớp",classroom::back,null);
        LinearLayout body = ui.page(root); ui.space(body,4);

        LinearLayout card = ui.row(); card.setGravity(Gravity.TOP); card.setPadding(ui.dp(10),ui.dp(12),ui.dp(10),ui.dp(12));
        ui.cardSurface(card,Ui.PALE,0); card.addView(ui.art(item.smallArt(),77,86)); ui.gap(card,12);
        LinearLayout info = ui.column(); ui.add(info,ui.text(item.title,21,Ui.INK,true)); ui.space(info,10);
        summary = ui.text("",18,Ui.INK,false); ui.add(info,summary); ui.space(info,8);
        ui.add(info,ui.text("▣ "+item.sessions+" buổi · "+item.mode,16,Ui.INK,false)); ui.space(info,6);
        ui.add(info,ui.text("▦ "+item.scheduleLabel(),15,Ui.INK,false)); ui.weight(card,info); ui.add(body,card); ui.space(body,16);

        LinearLayout titleRow = ui.row(); heading = ui.text("",20,Ui.INK,true); ui.weight(titleRow,heading);
        sort = ui.link("",14,()->{newestFirst=!newestFirst;render();}); sort.setTextColor(Ui.MUTED); titleRow.addView(sort);
        ui.add(body,titleRow); ui.space(body,4);
        list = ui.column(); ui.add(body,list); ui.space(body,2);
        ui.add(body,ui.note(R.drawable.ic_info,"Số học viên được duyệt không vượt quá "+item.capacity+".",Ui.PALE,Ui.BLUE,Ui.MUTED)); ui.space(body,18);

        ui.section(body,"Thành viên ("+item.members.size()+"/"+item.capacity+")",20); ui.space(body,6);
        ui.add(body,ui.text(item.members.isEmpty()?"Lớp chưa có học viên.":String.join(" · ",item.memberNames()),15,Ui.MUTED,false));
        if (GroupClass.OPEN.equals(item.status)) {
            ui.space(body,16); LinearLayout actions = ui.row();
            ui.weightAction(actions,ui.action("Chỉnh sửa lớp",R.drawable.ic_edit,Ui.OUTLINE,()->classroom.openClass(item,"openClass")),42); ui.gap(actions,10);
            ui.weightAction(actions,ui.action("⊘  Đóng tuyển",0,Ui.DANGER,()->OpenedClassesFragment.close(classroom,item)),42);
            ui.add(body,actions);
        }
        render();
        return root;
    }

    private void render() {
        summary.setText("Đã duyệt "+item.members.size()+"/"+item.capacity+"  ·  "+(item.full()?"Đã đủ chỗ":"Còn "+item.seatsLeft()+" chỗ"));
        List<GroupClass.Registration> waiting = new ArrayList<>();
        for (GroupClass.Registration registration : item.registrations) if (registration.pending()) waiting.add(registration);
        waiting.sort((a,b)->newestFirst?b.date.compareTo(a.date):a.date.compareTo(b.date));
        heading.setText("Danh sách đăng ký ("+waiting.size()+")"); sort.setText(newestFirst?"Mới nhất ⌄":"Cũ nhất ⌄");
        sort.setContentDescription("Sắp xếp: "+(newestFirst?"mới nhất":"cũ nhất"));
        list.removeAllViews();
        for (GroupClass.Registration registration : waiting) { ui.add(list,card(registration)); ui.space(list,12); }
        if (waiting.isEmpty()) { ui.add(list,ui.text("Không có đăng ký nào đang chờ duyệt.",15,Ui.MUTED,false)); ui.space(list,12); }
    }

    private View card(GroupClass.Registration registration) {
        LinearLayout card = ui.bordered(); card.setPadding(ui.dp(9),ui.dp(10),ui.dp(10),ui.dp(10));
        LinearLayout top = ui.row(); top.setGravity(Gravity.TOP);
        top.addView(ui.photo(0,"Ảnh "+registration.studentName,71,78,9)); ui.gap(top,15);
        LinearLayout info = ui.column(); LinearLayout nameRow = ui.row(); ui.weight(nameRow,ui.text(registration.studentName,20,Ui.INK,true));
        nameRow.addView(ui.pill("Chờ duyệt",14,Ui.ORANGE,Ui.ORANGE_BG)); ui.add(info,nameRow); ui.space(info,4);
        ui.add(info,ui.text("Mục tiêu học",14,Ui.MUTED,false)); ui.space(info,5);
        ui.add(info,ui.text(registration.goal,16,Ui.INK,false)); ui.space(info,7);
        ui.add(info,ui.text("▦ Gửi ngày "+LocalDate.parse(registration.date).format(Lesson.DATE),15,Ui.INK,false));
        ui.weight(top,info); ui.add(card,top); ui.space(card,10);
        LinearLayout actions = ui.row();
        ui.weightAction(actions,ui.action("×  Từ chối",0,Ui.DANGER,()->reject(registration)),40); ui.gap(actions,10);
        ui.weightAction(actions,ui.action("✓  Chấp nhận",0,Ui.PRIMARY,()->accept(registration)),40);
        ui.add(card,actions); return card;
    }

    private void accept(GroupClass.Registration registration) {
        String error = classroom.store.accept(item,registration);
        if (error != null) { classroom.dialog("Không thể duyệt thêm",error); return; }
        classroom.show("review"); classroom.notice("Đã nhận "+registration.studentName+" vào lớp.");
    }
    private void reject(GroupClass.Registration registration) {
        classroom.ui.dialog().setTitle("Từ chối "+registration.studentName+"?")
                .setMessage("Tiền ký quỹ của học viên sẽ được hoàn theo quy định (Thành viên 4 xử lý).")
                .setNegativeButton("Hủy",null)
                .setPositiveButton("Từ chối",(dialog,which)->{
                    String error = classroom.store.reject(registration);
                    if (error != null) classroom.dialog("Không thể từ chối",error); else { classroom.show("review"); classroom.notice("Đã từ chối đăng ký."); }
                }).show();
    }
}
