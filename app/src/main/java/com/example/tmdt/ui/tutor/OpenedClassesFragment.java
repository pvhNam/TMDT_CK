package com.example.tmdt.ui.tutor;

import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import java.util.ArrayList;
import java.util.List;
import com.example.tmdt.R;
import com.example.tmdt.MainActivity;
import com.example.tmdt.ui.common.ScreenFragment;
import com.example.tmdt.ui.common.Ui;
import com.example.tmdt.GroupClass;
import com.example.tmdt.Store;
import com.example.tmdt.Tutor;

/** Screen 35 · Lớp đã mở (UC17): the tutor's classes by status, with review, edit and close actions. */
public final class OpenedClassesFragment extends ScreenFragment {
    private static final String[] STATUSES = {GroupClass.OPEN, GroupClass.RUNNING, GroupClass.ENDED};

    @Override protected View build() {
        LinearLayout root = ui.column();
        View open = ui.action("＋ Mở lớp",0,Ui.PRIMARY,()->activity.openClass(null,"openClass"));
        open.setLayoutParams(ui.lp(104,37));
        ui.title(root,"Lớp đã mở",open);
        String[] labels = {"Đang tuyển","Đang học","Đã kết thúc"};
        for (int i = 0; i < 3; i++) labels[i] += " ("+mine(STATUSES[i]).size()+")";
        ui.tabs(root,labels,activity.classTab,index->{activity.classTab=index;activity.show("classes");});
        LinearLayout body = ui.page(root); ui.space(body,4);
        List<GroupClass> list = mine(STATUSES[activity.classTab]);
        for (GroupClass item : list) { ui.add(body,card(item)); ui.space(body,11); }
        if (list.isEmpty()) {
            ui.add(body,ui.note(R.drawable.ic_info,activity.classTab==0?"Chưa có lớp đang tuyển sinh. Chọn \"Mở lớp\" để tạo lớp mới."
                    :"Chưa có lớp nào trong mục này.",Ui.PALE,Ui.BLUE,Ui.INK));
        }
        return root;
    }

    private List<GroupClass> mine(String status) {
        List<GroupClass> list = new ArrayList<>();
        for (GroupClass item : activity.store.classes) if (item.tutorId == Store.TUTOR && item.status.equals(status)) list.add(item);
        return list;
    }

    private View card(GroupClass item) {
        LinearLayout card = ui.bordered(11); card.setPadding(ui.dp(10),ui.dp(13),ui.dp(10),ui.dp(10));
        LinearLayout top = ui.row(); top.setGravity(Gravity.TOP);
        top.addView(ui.art(item.smallArt(),77,83)); ui.gap(top,11);
        LinearLayout info = ui.column(); LinearLayout titleRow = ui.row(); titleRow.setGravity(Gravity.TOP);
        ui.weight(titleRow,ui.text(item.title,item.title.length()>16?16:21,Ui.INK,true)); ui.gap(titleRow,6);
        titleRow.addView(status(item)); ui.add(info,titleRow); ui.space(info,4);
        ui.add(info,ui.fact(R.drawable.ic_users,item.members.size()+"/"+item.capacity+" học viên",18,16,Ui.INK,false));
        if (item.pendingCount() > 0) ui.add(info,ui.fact(R.drawable.ic_clock,item.pendingCount()+" đăng ký chờ duyệt",18,16,Ui.ORANGE,false));
        ui.add(info,ui.fact(item.mode.equals("Tại nhà")?R.drawable.ic_home:R.drawable.ic_video,item.sessions+" buổi · "+item.mode,18,16,Ui.INK,false));
        ui.add(info,ui.fact(R.drawable.ic_coin,Tutor.money(item.price)+(item.perSession?" / buổi":" / học viên"),18,16,Ui.INK,false));
        ui.add(info,ui.fact(R.drawable.ic_calendar,item.scheduleLabel(),18,16,Ui.INK,false));
        ui.weight(top,info); ui.add(card,top); ui.space(card,8);

        if (GroupClass.OPEN.equals(item.status) && item.pendingCount() > 0) {
            ui.addAction(card,ui.action("Duyệt đăng ký",0,Ui.PRIMARY,()->activity.openClass(item,"review")),40); ui.space(card,10);
            LinearLayout row = ui.row();
            ui.weightAction(row,ui.action("Thành viên",R.drawable.ic_users,Ui.OUTLINE,()->members(activity,item)),40); ui.gap(row,10);
            ui.weightAction(row,ui.action("Chỉnh sửa",R.drawable.ic_edit,Ui.OUTLINE,()->activity.openClass(item,"openClass")),40);
            ui.add(card,row); ui.space(card,9);
            ui.addAction(card,ui.action("⊘  Đóng tuyển sinh",0,Ui.DANGER,()->close(activity,item)),38);
        } else {
            ui.addAction(card,ui.action("Xem lớp",0,Ui.OUTLINE,()->activity.openClass(item,"review")),38);
        }
        return card;
    }

    private TextView status(GroupClass item) {
        if (GroupClass.OPEN.equals(item.status)) return ui.pill("Đang tuyển",12,Ui.ORANGE,Ui.ORANGE_BG);
        if (GroupClass.RUNNING.equals(item.status)) return ui.pill("Đang học",12,Ui.GREEN,Ui.GREEN_BG);
        return ui.pill("Đã kết thúc",12,Ui.MUTED,0xFFEEF2F7);
    }

    public static void members(MainActivity activity, GroupClass item) {
        StringBuilder list = new StringBuilder();
        for (String member : item.members) list.append("• ").append(member).append('\n');
        activity.dialog("Thành viên ("+item.members.size()+"/"+item.capacity+")",list.length()==0?"Lớp chưa có học viên.":list.toString().trim());
    }

    public static void close(MainActivity activity, GroupClass item) {
        new AlertDialog.Builder(activity).setTitle("Đóng tuyển sinh?")
                .setMessage("Lớp \""+item.title+"\" sẽ chuyển sang mục Đang học với "+item.members.size()+" học viên. Các đăng ký đang chờ sẽ không được duyệt thêm.")
                .setNegativeButton("Hủy",null)
                .setPositiveButton("Đóng tuyển sinh",(dialog,which)->{
                    activity.store.closeRecruiting(item); activity.classTab=1; activity.show("classes"); activity.notice("Đã đóng tuyển sinh lớp \""+item.title+"\".");
                }).show();
    }
}
