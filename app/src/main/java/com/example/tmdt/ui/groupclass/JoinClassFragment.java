package com.example.tmdt.ui.groupclass;

import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import java.time.LocalDate;
import com.example.tmdt.R;
import com.example.tmdt.ui.common.ScreenFragment;
import com.example.tmdt.ui.common.Ui;
import com.example.tmdt.GroupClass;
import com.example.tmdt.Lesson;

/** Screen 38 · Đăng ký tham gia lớp (UC20). Seats are checked before any registration or deposit is created. */
public final class JoinClassFragment extends ScreenFragment {
    private GroupClass item;
    private EditText goal;

    @Override protected View build() {
        item = classroom.store.groupClass(classroom.classId);
        LinearLayout root = ui.column(); ui.header(root,"Đăng ký tham gia lớp",classroom::back,null);
        LinearLayout body = ui.page(root);

        LinearLayout card = ui.row(); card.setGravity(Gravity.TOP); card.setPadding(ui.dp(6),ui.dp(11),ui.dp(8),ui.dp(11));
        ui.cardSurface(card,Ui.WHITE,Ui.BORDER);
        card.addView(ui.art(item.largeArt(),108,112)); ui.gap(card,14);
        LinearLayout info = ui.column(); ui.add(info,ui.text(item.title,23,Ui.INK,true)); ui.space(info,4);
        ui.add(info,ui.text(item.subject+" · "+item.level,16,Ui.MUTED,false)); ui.space(info,8);
        LinearLayout teacher = ui.row(); teacher.addView(ui.photo(0,"Ảnh gia sư "+item.tutorName,48,50,9)); ui.gap(teacher,11);
        LinearLayout name = ui.column(); ui.add(name,ui.text(item.tutorName,16,Ui.INK,false)); ui.space(name,6);
        ui.add(name,ui.text("Gia sư đã xác minh",15,Ui.MUTED,false)); ui.weight(teacher,name);
        ui.add(info,teacher); ui.weight(card,info); ui.add(body,card); ui.space(body,11);

        LinearLayout table = ui.table();
        ui.tableRow(table,R.drawable.ic_calendar,"Khai giảng",item.startLabel());
        ui.tableRow(table,R.drawable.ic_calendar,"Lịch học",item.scheduleLabel());
        ui.tableRow(table,R.drawable.ic_file,"Số buổi học",item.sessions+" buổi");
        ui.tableRow(table,R.drawable.ic_laptop,"Hình thức",item.mode);
        if (!item.address.isEmpty()) ui.tableRow(table,R.drawable.ic_pin,"Địa điểm",item.address);
        ui.add(body,table); ui.space(body,6);
        LinearLayout seats = ui.row(); seats.setPadding(ui.dp(15),0,0,0);
        seats.addView(ui.text("♟  Đã có "+item.members.size()+"/"+item.capacity+" học viên · "+(item.full()?"Đã đủ chỗ":"Còn "+item.seatsLeft()+" chỗ"),17,Ui.INK,false));
        ui.add(body,seats); ui.space(body,14);
        if (!item.description.isEmpty()) { ui.add(body,ui.text(item.description,15,Ui.MUTED,false)); ui.space(body,12); }

        GroupClass.Registration sent = item.registrationOf(classroom.me());
        boolean member = item.members.contains(classroom.me());
        if (!member && sent == null) {
            ui.label(body,"Mục tiêu học tập"); goal = ui.entry("Ví dụ: Ôn kiến thức và luyện bài tập",true);
            goal.setMinimumHeight(ui.dp(54)); goal.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(200)});
            ui.add(body,goal); ui.space(body,16);
        }
        ui.section(body,"Học phí",18); ui.space(body,8);
        LinearLayout price = ui.row(); price.setPadding(ui.dp(15),ui.dp(10),ui.dp(15),ui.dp(10)); ui.cardSurface(price,Ui.PALE,0);
        price.addView(ui.icon(R.drawable.ic_coin,22,Ui.INK)); ui.gap(price,17); price.addView(ui.text(item.priceLabel(),22,Ui.INK,true));
        ui.add(body,price); ui.space(body,20);

        if (member) {
            ui.add(body,ui.note(R.drawable.ic_info,"Bạn đã là thành viên của lớp này.",Ui.GREEN_BG,Ui.GREEN,Ui.INK)); ui.space(body,16);
            ui.addAction(body,ui.action("Đã tham gia lớp",0,Ui.DISABLED,null),52);
        } else if (sent != null) {
            ui.add(body,ui.note(R.drawable.ic_info,"Bạn đã gửi đăng ký ngày "+LocalDate.parse(sent.date).format(Lesson.DATE)+". Đăng ký đang chờ gia sư duyệt.",Ui.PALE,Ui.BLUE,Ui.INK));
            ui.space(body,16); ui.addAction(body,ui.action("Đã gửi đăng ký",0,Ui.DISABLED,null),52);
        } else {
            ui.add(body,ui.note(R.drawable.ic_info,"Bước tiếp theo: ký quỹ học phí và gửi đăng ký chờ gia sư duyệt.",Ui.PALE,Ui.BLUE,Ui.INK));
            ui.space(body,24); ui.addAction(body,ui.action("Tiếp tục đăng ký →",0,Ui.PRIMARY,this::submit),52);
        }
        return root;
    }

    private void submit() {
        String objective = goal.getText().toString().trim();
        if (objective.isEmpty()) { goal.setError("Vui lòng nhập mục tiêu học tập"); goal.requestFocus(); return; }
        classroom.hideKeyboard();
        if (item.full()) { classroom.show("full"); return; }
        // A clash is reported before the deposit step, so no money is held for a class the student cannot attend.
        String blocked = classroom.store.registrationError(item,classroom.me());
        if (blocked != null) { classroom.dialog("Chưa gửi được đăng ký",blocked); return; }
        classroom.ui.dialog().setTitle("Ký quỹ học phí")
                .setMessage(item.priceLabel()+" sẽ được giữ ký quỹ cho đến khi gia sư duyệt đăng ký.\n\nMàn hình Ví và nạp tiền (41) do Thành viên 4 phụ trách; bản mẫu chỉ ghi nhận đăng ký, chưa trừ tiền.")
                .setNegativeButton("Hủy",null)
                .setPositiveButton("Xác nhận ký quỹ",(dialog,which)->{
                    // Seats may have run out while the dialog was open.
                    if (item.full()) { classroom.show("full"); return; }
                    String error = classroom.store.register(item,classroom.me(),classroom.myName(),objective);
                    if (error != null) { classroom.dialog("Chưa gửi được đăng ký",error); return; }
                    classroom.show("groups"); classroom.notice("Đã gửi đăng ký lớp \""+item.title+"\". Chờ gia sư duyệt.");
                }).show();
    }
}
