package com.example.tmdt.ui.tutor;

import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;
import com.example.tmdt.R;
import com.example.tmdt.ui.common.ScreenFragment;
import com.example.tmdt.ui.common.Ui;
import com.example.tmdt.GroupClass;
import com.example.tmdt.GroupMatch;
import com.example.tmdt.Store;
import com.example.tmdt.Tutor;

/** Gợi ý ghép nhóm: students who posted tutor requests for the same subject and level, so the tutor opens one class for them. */
public final class GroupMatchFragment extends ScreenFragment {

    @Override protected View build() {
        LinearLayout root = ui.column(); ui.header(root,"Gợi ý ghép nhóm",classroom::back,null);
        LinearLayout body = ui.page(root);
        ui.add(body,ui.note(R.drawable.ic_users,"Học viên đăng tin tìm gia sư cùng môn và cấp học.\nMở một lớp nhóm để các bạn học cùng nhau.",Ui.PALE,Ui.BLUE,Ui.INK));
        ui.space(body,14);
        Store store = classroom.store;
        List<GroupMatch.Group> groups = GroupMatch.groups(store.openRequests,classroom.me());
        if (!store.requestsError.isEmpty()) {
            ui.add(body,ui.note(R.drawable.ic_info,"Chưa tải được tin tìm gia sư.\n"+store.requestsError,0xFFFFE9EC,Ui.RED,Ui.INK)); ui.space(body,12);
            ui.addAction(body,ui.action("Thử lại",0,Ui.OUTLINE,classroom::retryRequests),44);
        } else if (!store.openRequestsLoaded) {
            ui.add(body,ui.note(R.drawable.ic_clock,"Đang tải tin tìm gia sư…",Ui.PALE,Ui.BLUE,Ui.MUTED));
        } else if (groups.isEmpty()) {
            LinearLayout empty = ui.column(); empty.setGravity(Gravity.CENTER); empty.setPadding(ui.dp(18),ui.dp(28),ui.dp(18),ui.dp(26)); ui.cardSurface(empty,Ui.PALE,0);
            empty.addView(ui.icon(R.drawable.ic_users,40,Ui.BLUE)); ui.space(empty,14);
            TextView title = ui.text("Chưa có nhóm phù hợp",18,Ui.INK,true); title.setGravity(Gravity.CENTER); ui.add(empty,title); ui.space(empty,8);
            TextView hint = ui.text("Khi có từ 2 học viên đăng tin cùng môn\nvà cùng cấp học, nhóm sẽ hiện ở đây.",15,Ui.MUTED,false);
            hint.setGravity(Gravity.CENTER); ui.add(empty,hint);
            ui.add(body,empty);
        } else {
            for (GroupMatch.Group group : groups) { ui.add(body,card(group)); ui.space(body,13); }
        }
        return root;
    }

    private View card(GroupMatch.Group group) {
        LinearLayout card = ui.bordered(); card.setPadding(ui.dp(12),ui.dp(13),ui.dp(12),ui.dp(12));
        LinearLayout top = ui.row(); top.setGravity(Gravity.CENTER_VERTICAL);
        GroupClass preview = new GroupClass("","",""); preview.subject = group.subject;
        top.addView(ui.photo(preview.art(),null,64,64,12)); ui.gap(top,12);
        LinearLayout info = ui.column();
        ui.add(info,ui.text(group.size()+" học viên cần "+group.subject,19,Ui.INK,true)); ui.space(info,6);
        LinearLayout tags = ui.row();
        tags.addView(ui.pill(group.levelLabel(),13,Ui.BLUE,Ui.PALE),ui.lp(-2,-2));
        info.addView(tags); ui.weight(top,info); ui.add(card,top); ui.space(card,12);

        String budget = group.budgetLabel(), schedule = group.scheduleLabel();
        if (!budget.isEmpty()) ui.add(card,ui.fact(R.drawable.ic_coin,budget,18,15,Ui.INK,false));
        ui.add(card,ui.fact(R.drawable.ic_calendar,schedule.isEmpty()?"Lịch mỗi bạn một khác, xem bên dưới":"Hay rảnh: "+schedule,18,15,Ui.INK,false));
        ui.space(card,10); ui.line(card); ui.space(card,10);

        for (int i = 0; i < group.requests.size(); i++) {
            if (i > 0) ui.space(card,12);
            ui.add(card,student(group.requests.get(i)));
        }
        ui.space(card,14);
        ui.addAction(card,ui.action("Mở lớp nhóm",0,Ui.PRIMARY,()->classroom.openDraft(group)),44);
        return card;
    }

    private View student(GroupMatch.Request request) {
        LinearLayout row = ui.row(); row.setGravity(Gravity.TOP);
        String name = request.studentName.isEmpty() ? "Học viên" : request.studentName;
        row.addView(ui.avatar(request.studentId,name,38)); ui.gap(row,10);
        LinearLayout text = ui.column();
        TextView title = ui.text(name,15,Ui.INK,true); title.setSingleLine(true); title.setEllipsize(TextUtils.TruncateAt.END); ui.add(text,title);
        ui.space(text,2);
        TextView when = ui.text("Lịch: "+(request.schedule.isEmpty()?"chưa ghi":request.schedule),13,Ui.MUTED,false);
        when.setMaxLines(2); when.setEllipsize(TextUtils.TruncateAt.END); ui.add(text,when);
        if (request.budget > 0) { ui.space(text,1); ui.add(text,ui.text("Ngân sách: "+Tutor.money((int) request.budget)+" / buổi",13,Ui.MUTED,false)); }
        ui.weight(row,text);
        return row;
    }
}
