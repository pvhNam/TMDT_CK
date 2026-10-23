package com.example.tmdt.ui.groupclass;

import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import com.example.tmdt.Classroom;
import com.example.tmdt.GroupClass;
import com.example.tmdt.Lesson;
import com.example.tmdt.R;
import com.example.tmdt.ui.common.ScreenFragment;
import com.example.tmdt.ui.common.Ui;

/** Lịch sử đăng ký lớp học: every class the student asked to join, the day it was sent and the tutor's answer. */
public final class RegistrationHistoryFragment extends ScreenFragment {
    static final int ALL = 0, WAITING = 1, ACCEPTED = 2, DECLINED = 3;
    private static final String[] FILTERS = {"Tất cả","Chờ duyệt","Đã duyệt","Từ chối"};

    /** One of the student's registrations together with its class. */
    static final class Entry {
        final GroupClass item; final GroupClass.Registration registration;
        Entry(GroupClass item, GroupClass.Registration registration) { this.item=item; this.registration=registration; }
    }

    static List<Entry> mine(Classroom classroom) {
        List<Entry> list = new ArrayList<>();
        for (GroupClass item : classroom.store.classes)
            for (GroupClass.Registration registration : item.registrations)
                if (registration.studentId.equals(classroom.me())) list.add(new Entry(item,registration));
        list.sort(Comparator.comparing((Entry entry)->entry.registration.date).reversed());
        return list;
    }

    static int group(GroupClass.Registration registration) {
        return registration.pending() ? WAITING : Lesson.CONFIRMED.equals(registration.status) ? ACCEPTED : DECLINED;
    }

    /** A registration still waiting when the class stopped recruiting is closed without an answer from the tutor. */
    static String label(GroupClass item, GroupClass.Registration registration) {
        if (item.cancelled()) return "Lớp đã hủy";
        switch (group(registration)) {
            case WAITING: return "Chờ duyệt";
            case ACCEPTED: return "Đã duyệt";
            default: return GroupClass.OPEN.equals(item.status) ? "Bị từ chối" : "Lớp đã đóng";
        }
    }

    static TextView pill(Ui ui, GroupClass item, GroupClass.Registration registration, int size) {
        String text = label(item,registration);
        if (item.cancelled()) return ui.pill(text,size,Ui.RED,0xFFFFE9EC);
        switch (group(registration)) {
            case WAITING: return ui.pill(text,size,Ui.ORANGE,Ui.ORANGE_BG);
            case ACCEPTED: return ui.pill(text,size,Ui.GREEN,Ui.GREEN_BG);
            default: return GroupClass.OPEN.equals(item.status) ? ui.pill(text,size,Ui.RED,0xFFFFE9EC) : ui.pill(text,size,Ui.MUTED,0xFFEEF2F7);
        }
    }

    @Override protected View build() {
        LinearLayout root = ui.column(); ui.header(root,"Lớp đã đăng ký",classroom::back,null);
        LinearLayout body = ui.page(root);
        List<Entry> all = mine(classroom);
        int[] counts = new int[FILTERS.length];
        for (Entry entry : all) { counts[ALL]++; counts[group(entry.registration)]++; }

        LinearLayout chips = ui.row();
        for (int i = 0; i < FILTERS.length; i++) {
            if (i > 0) ui.gap(chips,8);
            chips.addView(filter(i,counts[i]),new LinearLayout.LayoutParams(0,ui.dp(58),1));
        }
        ui.add(body,chips); ui.space(body,14);

        int shown = 0;
        for (Entry entry : all) {
            if (classroom.registrationFilter != ALL && group(entry.registration) != classroom.registrationFilter) continue;
            ui.add(body,card(entry)); ui.space(body,11); shown++;
        }
        if (shown == 0) {
            LinearLayout empty = ui.column(); empty.setGravity(Gravity.CENTER); empty.setPadding(ui.dp(18),ui.dp(28),ui.dp(18),ui.dp(22)); ui.cardSurface(empty,Ui.PALE,0);
            empty.addView(ui.icon(R.drawable.ic_users,40,Ui.BLUE)); ui.space(empty,14);
            TextView title = ui.text(all.isEmpty()?"Bạn chưa đăng ký lớp nào":"Không có đăng ký ở mục này",18,Ui.INK,true); title.setGravity(Gravity.CENTER); ui.add(empty,title); ui.space(empty,8);
            TextView hint = ui.text("Tìm lớp học nhóm phù hợp và gửi đăng ký để học cùng bạn bè.",15,Ui.MUTED,false); hint.setGravity(Gravity.CENTER); ui.add(empty,hint); ui.space(empty,16);
            ui.addAction(empty,ui.action("Tìm lớp học nhóm",R.drawable.ic_search,Ui.PRIMARY,()->classroom.show("groups")),46);
            ui.add(body,empty);
        }
        return root;
    }

    /** Equal-width filter with the count above its label, so all four fit on one line. */
    private View filter(int filter, int count) {
        boolean selected = filter == classroom.registrationFilter;
        LinearLayout chip = ui.column(); chip.setGravity(Gravity.CENTER);
        ui.fieldSurface(chip,selected?Ui.BLUE:Ui.WHITE,selected?Ui.BLUE:Ui.BORDER);
        TextView number = ui.text(String.valueOf(count),17,selected?Ui.WHITE:Ui.INK,true); number.setGravity(Gravity.CENTER); ui.add(chip,number);
        TextView label = ui.text(FILTERS[filter],12,selected?Ui.WHITE:Ui.MUTED,false); label.setGravity(Gravity.CENTER); label.setSingleLine(true); ui.add(chip,label);
        chip.setSelected(selected); chip.setContentDescription(FILTERS[filter]+" "+count+(selected?", đang chọn":""));
        ui.clickable(chip,()->{classroom.registrationFilter=filter;classroom.show("registrations");});
        return chip;
    }

    private View card(Entry entry) {
        GroupClass item = entry.item; GroupClass.Registration registration = entry.registration;
        LinearLayout card = ui.bordered(); card.setPadding(ui.dp(12),ui.dp(12),ui.dp(12),ui.dp(12));
        LinearLayout row = ui.row(); row.setGravity(Gravity.TOP);
        row.addView(ui.photo(item.art(),null,66,70,12)); ui.gap(row,12);
        LinearLayout info = ui.column();
        TextView title = ui.text(item.title,17,Ui.INK,true); title.setSingleLine(true); title.setEllipsize(TextUtils.TruncateAt.END); ui.add(info,title); ui.space(info,3);
        ui.add(info,ui.text(item.subject+" · "+item.tutorName,14,Ui.MUTED,false)); ui.space(info,6);
        ui.add(info,ui.fact(R.drawable.ic_calendar,"Gửi ngày "+LocalDate.parse(registration.date).format(Lesson.DATE),16,14,Ui.INK,false));
        ui.add(info,ui.fact(R.drawable.ic_clock,"Khai giảng "+item.startLabel()+" · "+item.scheduleLabel(),16,14,Ui.INK,false));
        ui.weight(row,info); ui.gap(row,8);
        row.addView(pill(ui,item,registration,13),ui.lp(-2,-2));
        ui.add(card,row);
        card.setContentDescription(item.title+", "+label(item,registration)+". Chạm để xem chi tiết");
        ui.clickable(card,()->classroom.openRegistration(item));
        return card;
    }
}
