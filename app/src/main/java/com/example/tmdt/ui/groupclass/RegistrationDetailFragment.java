package com.example.tmdt.ui.groupclass;

import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.time.LocalDate;
import com.example.tmdt.GroupClass;
import com.example.tmdt.Lesson;
import com.example.tmdt.R;
import com.example.tmdt.ui.common.ScreenFragment;
import com.example.tmdt.ui.common.Ui;

/** Chi tiết một đăng ký lớp: its progress, the goal that was sent, the class it belongs to and what the student can do next. */
public final class RegistrationDetailFragment extends ScreenFragment {
    private GroupClass item;
    private GroupClass.Registration registration;

    @Override protected View build() {
        item = classroom.store.groupClass(classroom.classId);
        for (GroupClass.Registration own : item.registrations) if (own.studentId.equals(classroom.me())) registration = own;
        LinearLayout root = ui.column(); ui.header(root,"Chi tiết đăng ký",classroom::back,null);
        LinearLayout body = ui.page(root);
        if (registration == null) {
            ui.add(body,ui.note(R.drawable.ic_info,"Không tìm thấy đăng ký của bạn ở lớp này.",Ui.PALE,Ui.BLUE,Ui.INK));
            return root;
        }
        int group = RegistrationHistoryFragment.group(registration);
        if(Lesson.CONFIRMED.equals(registration.status)){
            com.example.tmdt.MainActivity host=(com.example.tmdt.MainActivity)requireActivity();
            boolean paid=host.payments.paid("GROUP",registration.id());
            ui.addAction(body,ui.action(paid?"Thanh toán và xác nhận các buổi học":"Thanh toán toàn bộ khóa qua VNPay",R.drawable.ic_wallet,Ui.PRIMARY,()->host.openPayment("GROUP",registration.id())),48);ui.space(body,12);
        }

        LinearLayout status = ui.column(); ui.pad(status,16,16); ui.cardSurface(status,Ui.PALE,0);
        status.addView(RegistrationHistoryFragment.pill(ui,item,registration,14),ui.lp(-2,-2)); ui.space(status,10);
        ui.add(status,ui.text(headline(group),20,Ui.INK,true)); ui.space(status,6);
        ui.add(status,ui.text(explanation(group),15,Ui.MUTED,false)); ui.space(status,16);
        ui.add(status,steps(group));
        ui.add(body,status); ui.space(body,14);

        LinearLayout summary = ui.row(); summary.setPadding(ui.dp(12),ui.dp(12),ui.dp(12),ui.dp(12)); ui.cardSurface(summary,Ui.WHITE,Ui.BORDER);
        summary.addView(ui.photo(item.art(),null,74,78,12)); ui.gap(summary,13);
        LinearLayout info = ui.column(); ui.add(info,ui.text(item.title,19,Ui.INK,true)); ui.space(info,3);
        ui.add(info,ui.text(item.subject+" · "+item.level,14,Ui.MUTED,false)); ui.space(info,8);
        LinearLayout teacher = ui.row(); teacher.setGravity(Gravity.CENTER_VERTICAL);
        teacher.addView(ui.avatar(item.tutorId,item.tutorName,32)); ui.gap(teacher,8); ui.weight(teacher,ui.text(item.tutorName,15,Ui.INK,false));
        ui.add(info,teacher); ui.weight(summary,info); ui.add(body,summary); ui.space(body,14);

        ui.section(body,"Thông tin đăng ký",17); ui.space(body,8);
        LinearLayout sent = ui.table();
        ui.tableRow(sent,R.drawable.ic_calendar,"Ngày gửi",LocalDate.parse(registration.date).format(Lesson.DATE));
        if (!registration.pending() && registration.updated != null)
            ui.tableRow(sent,R.drawable.ic_clock,"Cập nhật",registration.updated.format(Lesson.TIME)+" · "+registration.updated.format(Lesson.DATE));
        ui.tableRow(sent,R.drawable.ic_file,"Trạng thái",RegistrationHistoryFragment.label(item,registration));
        if (item.cancelled() && item.members.contains(classroom.me())) ui.tableRow(sent,R.drawable.ic_coin,"Hoàn tiền","Chờ hoàn tiền");
        ui.add(body,sent); ui.space(body,12);
        ui.label(body,"Mục tiêu đã gửi");
        TextView goal = ui.text(registration.goal.isEmpty()?"Không có":registration.goal,15,Ui.INK,false); goal.setPadding(ui.dp(12),ui.dp(10),ui.dp(12),ui.dp(10));
        ui.fieldSurface(goal,Ui.PALE,Ui.BORDER); ui.add(body,goal); ui.space(body,16);

        ui.section(body,"Thông tin lớp",17); ui.space(body,8);
        LinearLayout details = ui.table();
        ui.tableRow(details,R.drawable.ic_calendar,"Khai giảng",item.startLabel());
        ui.tableRow(details,R.drawable.ic_clock,"Lịch học",item.scheduleLabel());
        ui.tableRow(details,R.drawable.ic_file,"Số buổi",item.sessions+" buổi");
        ui.tableRow(details,item.mode.equals("Tại nhà")?R.drawable.ic_home_soft:R.drawable.ic_video,"Hình thức",item.mode);
        if (!item.address.isEmpty()) ui.tableRow(details,R.drawable.ic_pin,"Địa điểm",item.address);
        if (item.online()) ui.tableRow(details,R.drawable.ic_video,"Phòng học",item.roomLink.isEmpty()?"Phòng của app":item.roomHost());
        ui.tableRow(details,R.drawable.ic_coin,"Học phí",item.priceLabel());
        ui.tableRow(details,R.drawable.ic_users,"Sĩ số",item.members.size()+"/"+item.capacity+" học viên");
        ui.tableRow(details,R.drawable.ic_info,"Trạng thái lớp",classStatus());
        ui.add(body,details); ui.space(body,18);

        actions(body,group);
        return root;
    }

    private String headline(int group) {
        if (item.cancelled()) return "Lớp đã bị hủy";
        if (group == RegistrationHistoryFragment.WAITING) return "Đang chờ gia sư duyệt";
        if (group == RegistrationHistoryFragment.ACCEPTED) return ((com.example.tmdt.MainActivity)requireActivity()).payments.paid("GROUP",registration.id())?"Đã thanh toán khóa học":"Đã được duyệt · Chờ thanh toán";
        return GroupClass.OPEN.equals(item.status) ? "Gia sư chưa nhận đăng ký này" : "Đăng ký không được duyệt";
    }
    private String explanation(int group) {
        if (group == RegistrationHistoryFragment.WAITING) return "Chưa thu học phí. Gia sư sẽ xem xét đăng ký của bạn trước.";
        if (group == RegistrationHistoryFragment.ACCEPTED) return "Thanh toán đầy đủ học phí khóa qua VNPay. Xác nhận từng buổi trong mục Thanh toán để ghi nhận tiền dạy.";
        if (item.cancelled()) return "Gia sư đã hủy lớp. Lý do: "+item.cancelReason+"."
                +(item.members.contains(classroom.me())?" Học phí ký quỹ của bạn đang chờ hoàn tiền.":"");
        if (group == RegistrationHistoryFragment.WAITING) return "Chưa thu học phí. Gia sư sẽ xem xét đăng ký của bạn trước.";
        if (group == RegistrationHistoryFragment.ACCEPTED) return "Thanh toán đầy đủ học phí khóa qua VNPay. Xác nhận từng buổi trong mục Thanh toán để ghi nhận tiền dạy.";
        return GroupClass.OPEN.equals(item.status) ? "Lớp vẫn đang tuyển sinh, bạn có thể gửi lại đăng ký với mục tiêu khác."
                : "Lớp đã đóng tuyển sinh trước khi đăng ký được duyệt. Hãy tìm lớp tương tự.";
    }
    private String classStatus() {
        if (item.cancelled()) return "Đã hủy";
        if (GroupClass.OPEN.equals(item.status)) return item.full() ? "Đang tuyển sinh · đã đủ chỗ" : "Đang tuyển sinh";
        return GroupClass.RUNNING.equals(item.status) ? "Đang học" : "Đã kết thúc";
    }

    /** Gửi đăng ký → Gia sư duyệt → Thanh toán học phí. */
    private View steps(int group) {
        LinearLayout row = ui.row(); row.setGravity(Gravity.TOP);
        boolean accepted = group == RegistrationHistoryFragment.ACCEPTED, declined = group == RegistrationHistoryFragment.DECLINED;
        row.addView(step("1","Gửi đăng ký",true,false),new LinearLayout.LayoutParams(0,-2,1));
        row.addView(step("2","Gia sư duyệt",accepted,declined),new LinearLayout.LayoutParams(0,-2,1));
        boolean paid=((com.example.tmdt.MainActivity)requireActivity()).payments.paid("GROUP",registration.id());
        row.addView(step("3","Thanh toán",paid,false),new LinearLayout.LayoutParams(0,-2,1));
        return row;
    }
    private View step(String number, String title, boolean done, boolean refused) {
        LinearLayout column = ui.column(); column.setGravity(Gravity.CENTER_HORIZONTAL);
        int color = refused ? Ui.RED : done ? Ui.GREEN : Ui.GRAY;
        TextView dot = ui.text(refused?"✕":done?"✓":number,15,Ui.WHITE,true); dot.setGravity(Gravity.CENTER);
        ui.surface(dot,color,15,0); column.addView(dot,ui.lp(30,30)); ui.space(column,6);
        TextView label = ui.text(title,13,done||refused?Ui.INK:Ui.MUTED,done); label.setGravity(Gravity.CENTER); ui.add(column,label);
        return column;
    }

    private void actions(LinearLayout body, int group) {
        LinearLayout row = ui.row();
        ui.weightAction(row,ui.action("Nhắn tin",R.drawable.ic_chat,Ui.OUTLINE,()->classroom.message(item.tutorName)),48); ui.gap(row,10);
        if (item.cancelled())
            ui.weightAction(row,ui.action("Tìm lớp tương tự",0,Ui.PRIMARY,()->{classroom.similarSubject=item.subject;classroom.show("groups");}),48);
        else if (group == RegistrationHistoryFragment.ACCEPTED && item.roomOpen())
            ui.weightAction(row,ui.action("Vào phòng học",R.drawable.ic_video,Ui.PRIMARY,()->classroom.openClassRoom(item)),48);
        else if (group == RegistrationHistoryFragment.ACCEPTED)
            ui.weightAction(row,ui.action("Xem lịch học",0,Ui.PRIMARY,()->{classroom.scheduleTab=0;classroom.show("schedule");}),48);
        else if (group == RegistrationHistoryFragment.WAITING)
            ui.weightAction(row,ui.action("Xem lớp",0,Ui.PRIMARY,()->classroom.openClass(item,"join")),48);
        else if (item.recruiting())
            ui.weightAction(row,ui.action("Đăng ký lại",0,Ui.PRIMARY,()->classroom.openClass(item,"join")),48);
        else ui.weightAction(row,ui.action("Tìm lớp tương tự",0,Ui.PRIMARY,()->{classroom.similarSubject=item.subject;classroom.show("groups");}),48);
        ui.add(body,row);
    }
}
