package com.example.tmdt.ui.groupclass;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.time.LocalDate;
import java.util.Arrays;
import com.example.tmdt.GroupClass;
import com.example.tmdt.Lesson;
import com.example.tmdt.MainActivity;
import com.example.tmdt.PaymentState;
import com.example.tmdt.R;
import com.example.tmdt.Tutor;
import com.example.tmdt.ui.common.ScreenFragment;
import com.example.tmdt.ui.common.Ui;

/** Chi tiết một đăng ký lớp: its progress, the course fee, the goal that was sent, the class it belongs to and what the student can do next. */
public final class RegistrationDetailFragment extends ScreenFragment {
    private GroupClass item;
    private GroupClass.Registration registration;
    private PaymentState payments;
    private PaymentState.Row payment;
    /** A refund request sent from this screen is still waiting for its answer. */
    private boolean sending;

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        payments = ((MainActivity) requireActivity()).payments;
        super.onViewCreated(view,state);
        payments.changes.observe(getViewLifecycleOwner(),ignored->{
            if (sending && !payments.busy) {
                sending = false;
                if (payments.error.isEmpty()) classroom.notice("Đã gửi yêu cầu hoàn học phí.");
                else classroom.dialog("Chưa gửi được yêu cầu",payments.error);
            }
            refresh();
        });
    }

    @Override protected View build() {
        item = classroom.store.groupClass(classroom.classId); registration = null;
        for (GroupClass.Registration own : item.registrations) if (own.studentId.equals(classroom.me())) registration = own;
        LinearLayout root = ui.column(); ui.header(root,"Chi tiết đăng ký",classroom::back,null);
        LinearLayout body = ui.page(root);
        if (registration == null) {
            ui.add(body,ui.note(R.drawable.ic_info,"Không tìm thấy đăng ký của bạn ở lớp này.",Ui.PALE,Ui.BLUE,Ui.INK));
            return root;
        }
        payment = payments.paymentFor("GROUP",registration.id());
        int group = RegistrationHistoryFragment.group(registration);

        LinearLayout status = ui.column(); ui.pad(status,16,16); ui.cardSurface(status,Ui.PALE,0);
        status.addView(RegistrationHistoryFragment.pill(ui,item,registration,14),ui.lp(-2,-2)); ui.space(status,10);
        ui.add(status,ui.text(headline(group),20,Ui.INK,true)); ui.space(status,6);
        ui.add(status,ui.text(explanation(group),15,Ui.MUTED,false)); ui.space(status,16);
        ui.add(status,steps(group));
        ui.add(body,status); ui.space(body,14);
        if (group == RegistrationHistoryFragment.ACCEPTED && (!item.cancelled() || settled())) { ui.add(body,fee()); ui.space(body,14); }

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

        ClassQuestionEntry.add(this,ui,body,item.id);
        actions(body,group);
        return root;
    }

    private String status() { return payment == null ? "" : payment.text("status"); }
    private boolean paid() { return Arrays.asList("PAID","PAID_REVIEW").contains(status()); }
    private boolean refunding() { return "REFUND_REQUESTED".equals(status()); }
    private boolean refunded() { return Arrays.asList("CLOSED_REFUNDED","REFUNDED").contains(status()); }
    /** The money has reached VNPay, whatever happened to it afterwards. */
    private boolean settled() { return paid() || refunding() || refunded() || "GATEWAY_REVIEW".equals(status()); }

    private String headline(int group) {
        if (item.cancelled()) return "Lớp đã bị hủy";
        if (group == RegistrationHistoryFragment.WAITING) return "Đang chờ gia sư duyệt";
        if (group == RegistrationHistoryFragment.ACCEPTED) return paid() ? "Đã thanh toán khóa học" : refunding() ? "Đang chờ hoàn học phí"
                : refunded() ? "Đã hoàn học phí" : settled() ? "Đang đối soát thanh toán" : "Đã được duyệt · Chờ thanh toán";
        return GroupClass.OPEN.equals(item.status) ? "Gia sư chưa nhận đăng ký này" : "Đăng ký không được duyệt";
    }
    private String explanation(int group) {
        boolean accepted = group == RegistrationHistoryFragment.ACCEPTED;
        if (item.cancelled()) return "Gia sư đã hủy lớp. Lý do: "+item.cancelReason+"."+(accepted?" "+feeNote():"");
        if (group == RegistrationHistoryFragment.WAITING) return "Chưa thu học phí. Gia sư sẽ xem xét đăng ký của bạn trước.";
        if (accepted) return paid() ? "Xác nhận từng buổi trong mục Thanh toán để ghi nhận tiền dạy." : settled() ? feeNote() : "Hãy thanh toán đủ học phí cả khóa qua VNPay.";
        return GroupClass.OPEN.equals(item.status) ? "Lớp vẫn đang tuyển sinh, bạn có thể gửi lại đăng ký với mục tiêu khác."
                : "Lớp đã đóng tuyển sinh trước khi đăng ký được duyệt. Hãy tìm lớp tương tự.";
    }
    private String feeNote() {
        if (paid()) return "Bạn có thể yêu cầu hoàn phần học phí chưa học ngay bên dưới.";
        if (refunding()) return "Yêu cầu hoàn học phí đang chờ admin xử lý.";
        if (refunded()) return "Học phí đã được hoàn lại cho bạn.";
        if (settled()) return "VNPay đang đối soát giao dịch của bạn.";
        return "Bạn chưa thanh toán nên không mất khoản phí nào.";
    }
    private String classStatus() {
        if (item.cancelled()) return "Đã hủy";
        if (GroupClass.OPEN.equals(item.status)) return item.full() ? "Đang tuyển sinh · đã đủ chỗ" : "Đang tuyển sinh";
        return GroupClass.RUNNING.equals(item.status) ? "Đang học" : "Đã kết thúc";
    }

    /** Gửi đăng ký → Gia sư duyệt → Thanh toán, or Hoàn tiền once a paid class is cancelled. */
    private View steps(int group) {
        LinearLayout row = ui.row(); row.setGravity(Gravity.TOP);
        boolean accepted = group == RegistrationHistoryFragment.ACCEPTED, declined = group == RegistrationHistoryFragment.DECLINED;
        boolean back = item.cancelled() && settled();
        row.addView(step("1","Gửi đăng ký",true,false),new LinearLayout.LayoutParams(0,-2,1));
        row.addView(step("2","Gia sư duyệt",accepted,declined),new LinearLayout.LayoutParams(0,-2,1));
        row.addView(step("3",back?"Hoàn tiền":"Thanh toán",back?refunded():settled(),false),new LinearLayout.LayoutParams(0,-2,1));
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

    /** Học phí of an accepted student: the whole course is paid through VNPay and asked back when the tutor cancels the class. */
    private View fee() {
        boolean refund = item.cancelled();
        LinearLayout card = ui.bordered(); ui.pad(card,14,14);
        LinearLayout head = ui.row();
        FrameLayout badge = new FrameLayout(ui.context); ui.fieldSurface(badge,refund?0xFFFFE9EC:Ui.PALE,0);
        badge.addView(ui.icon(R.drawable.ic_wallet,22,refund?Ui.RED:Ui.BLUE),new FrameLayout.LayoutParams(ui.dp(22),ui.dp(22),Gravity.CENTER));
        head.addView(badge,ui.lp(44,44)); ui.gap(head,12);
        LinearLayout names = ui.column();
        ui.add(names,single(refund?"Hoàn học phí":"Học phí khóa học",16,Ui.INK,true)); ui.space(names,2);
        ui.add(names,single(refund?"Trả lại phần học phí chưa học":item.sessions+" buổi · Thanh toán qua VNPay",14,Ui.MUTED,false));
        ui.weight(head,names); ui.add(card,head); ui.space(card,12); ui.line(card); ui.space(card,4);
        if (refund) {
            line(card,"Học phí đã đóng",single(Tutor.money((int) payment.number("received_amount")),17,Ui.BLUE,true));
            if (payment.number("refund_total") > 0) line(card,"Đã hoàn",single(Tutor.money((int) payment.number("refund_total")),17,Ui.GREEN,true));
        } else {
            long amount = payment == null ? (long) item.price*(item.perSession?item.sessions:1) : payment.number("amount");
            line(card,"Số tiền",single(Tutor.money((int) amount),17,Ui.BLUE,true));
        }
        line(card,"Trạng thái",feePill()); ui.space(card,10);
        ui.addAction(card,feeAction(refund),46);
        return card;
    }
    /** One row of the fee card: a muted label on the left and its value on the right. */
    private void line(LinearLayout card, String label, View value) {
        LinearLayout row = ui.row(); row.setMinimumHeight(ui.dp(38));
        ui.weight(row,single(label,15,Ui.MUTED,false)); ui.gap(row,10); row.addView(value,ui.lp(-2,-2)); ui.add(card,row);
    }
    private TextView single(String value, int size, int color, boolean bold) {
        TextView view = ui.text(value,size,color,bold); view.setSingleLine(true); view.setEllipsize(TextUtils.TruncateAt.END); return view;
    }
    private TextView feePill() {
        if (paid()) return ui.pill("Đã thanh toán",13,Ui.GREEN,Ui.GREEN_BG);
        if (refunded()) return ui.pill("Đã hoàn tiền",13,Ui.GREEN,Ui.GREEN_BG);
        if (refunding()) return ui.pill("Chờ hoàn tiền",13,Ui.ORANGE,Ui.ORANGE_BG);
        if (settled()) return ui.pill("Đang đối soát",13,Ui.ORANGE,Ui.ORANGE_BG);
        return ui.pill("Chưa thanh toán",13,Ui.ORANGE,Ui.ORANGE_BG);
    }
    private View feeAction(boolean refund) {
        Runnable open = ()->((MainActivity) requireActivity()).openPayment("GROUP",registration.id());
        if (refund && paid()) return payments.busy ? ui.action("Đang gửi yêu cầu…",0,Ui.DISABLED,null)
                : ui.action("Yêu cầu hoàn tiền",R.drawable.ic_wallet,Ui.PRIMARY,this::askRefund);
        if (!settled()) return ui.action("Thanh toán qua VNPay",R.drawable.ic_wallet,Ui.PRIMARY,open);
        if (paid()) return ui.action("Xem thanh toán và các buổi",R.drawable.ic_calendar,Ui.OUTLINE,open);
        return ui.action("Xem chi tiết thanh toán",R.drawable.ic_file,Ui.OUTLINE,open);
    }
    /** Only the unused part comes back: sessions the student already confirmed stay paid to the tutor. */
    private void askRefund() {
        long left = payment.number("received_amount")-payment.number("refund_total")-payment.number("earned_amount");
        ui.dialog().setTitle("Yêu cầu hoàn học phí")
                .setMessage("Lớp \""+item.title+"\" đã bị hủy. Admin sẽ đối soát và hoàn lại"+(left>0?" "+Tutor.money((int) left):"")
                        +" học phí chưa học cho bạn.")
                .setNegativeButton("Đóng",null)
                .setPositiveButton("Gửi yêu cầu",(dialog,which)->{
                    sending = true; payments.selected = payment.id;
                    payments.refund("Gia sư đã hủy lớp. Lý do: "+item.cancelReason);
                }).show();
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
