package com.example.tmdt.ui.groupclass;

import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import com.example.tmdt.R;
import com.example.tmdt.ui.common.ScreenFragment;
import com.example.tmdt.ui.common.Ui;
import com.example.tmdt.GroupClass;
import com.example.tmdt.Tutor;

/** Screen 60 · Lớp đã đủ chỗ (branch of UC20): registration is disabled before any deposit is taken. */
public final class ClassFullFragment extends ScreenFragment {
    private GroupClass item;

    @Override protected View build() {
        item = activity.store.groupClass(activity.classId);
        Tutor tutor = Tutor.get(item.tutorId);
        LinearLayout root = ui.column(); ui.header(root,"Chi tiết lớp nhóm",activity::back,null);
        LinearLayout body = ui.page(root);

        LinearLayout card = ui.bordered(11); card.setPadding(ui.dp(13),ui.dp(14),ui.dp(12),ui.dp(12));
        LinearLayout top = ui.row();
        FrameLayout badge = new FrameLayout(activity); ui.surface(badge,Ui.PALE,9,0);
        badge.addView(ui.icon(R.drawable.ic_users_filled,35,Ui.BLUE),new FrameLayout.LayoutParams(ui.dp(35),ui.dp(35),Gravity.CENTER));
        top.addView(badge,ui.lp(62,59)); ui.gap(top,13);
        LinearLayout title = ui.column(); ui.add(title,ui.text(item.title,21,Ui.INK,true)); ui.space(title,8);
        ui.add(title,ui.text(tutor.name,18,Ui.INK,false)); ui.weight(top,title); ui.add(card,top); ui.space(card,14);
        ui.add(card,ui.fact(R.drawable.ic_calendar,item.scheduleLabel(),23,16,Ui.INK,false)); ui.space(card,11);
        ui.add(card,ui.fact(R.drawable.ic_coin,item.priceLabel(),23,16,Ui.INK,false)); ui.space(card,11);
        ui.add(card,ui.fact(R.drawable.ic_pin,item.address.isEmpty()?"Học trực tuyến":"Học trực tiếp tại "+item.address,23,14,Ui.INK,false));
        ui.add(body,card);

        LinearLayout seats = ui.bordered(10); seats.setGravity(Gravity.CENTER_HORIZONTAL); seats.setPadding(ui.dp(12),ui.dp(14),ui.dp(12),ui.dp(12));
        seats.addView(ui.pill("♟ Đã đủ "+item.members.size()+"/"+item.capacity+" học viên",14,Ui.ORANGE,Ui.ORANGE_BG),ui.lp(196,36)); ui.space(seats,20);
        LinearLayout people = ui.row();
        for (int i = 0; i < Math.min(item.capacity,10); i++) { people.addView(ui.icon(R.drawable.ic_user_filled,18,Ui.BLUE)); ui.gap(people,7); }
        people.addView(new View(activity),new LinearLayout.LayoutParams(0,1,1));
        people.addView(ui.text(item.members.size()+"/"+item.capacity,15,Ui.INK,false)); ui.add(seats,people);
        ui.add(body,seats); ui.space(body,16);

        ui.add(body,ui.note(R.drawable.ic_info,"Lớp vừa hết chỗ.\nChưa tạo đăng ký hoặc giữ tiền ký quỹ.",Ui.PALE,Ui.BLUE,Ui.INK)); ui.space(body,15);
        ui.addAction(body,ui.action("Lớp đã đủ chỗ",0,Ui.DISABLED,null),51); ui.space(body,12);
        ui.addAction(body,ui.action("Tìm lớp tương tự",0,Ui.PRIMARY,()->{activity.similarSubject=item.subject;activity.show("groups");}),49); ui.space(body,11);
        ui.addAction(body,ui.action("Nhắn tin cho gia sư",R.drawable.ic_chat,Ui.OUTLINE,()->activity.message(tutor)),45);
        return root;
    }
}
