package com.example.tmdt.ui.payments;

import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.LinearLayout;
import com.example.tmdt.PaymentState;
import com.example.tmdt.R;
import com.example.tmdt.ui.common.Ui;

public final class PaymentDetailFragment extends PaymentScreen {
    private boolean allSessions;
    @Override public void onCreate(android.os.Bundle saved){super.onCreate(saved);if(saved!=null)allSessions=saved.getBoolean("all_sessions");}
    @Override public void onSaveInstanceState(android.os.Bundle out){super.onSaveInstanceState(out);out.putBoolean("all_sessions",allSessions);}
    private final androidx.activity.result.ActivityResultLauncher<Intent> vnpay=registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),result->{
        Intent data=result.getData();
        if(result.getResultCode()==android.app.Activity.RESULT_OK&&data!=null){
            String id=data.getStringExtra(VnpayPaymentActivity.PAYMENT),url=data.getStringExtra(VnpayPaymentActivity.CALLBACK);
            if(id!=null&&url!=null)state().returned(id,url);
        }else if(data!=null&&data.hasExtra(VnpayPaymentActivity.ERROR)){state().error=data.getStringExtra(VnpayPaymentActivity.ERROR);state().changed();}
        else{state().notice="Đã đóng VNPay. Chưa có kết quả xác nhận thanh toán.";state().changed();}
    });
    @Override protected View build(){
        LinearLayout root=root("Chi tiết thanh toán"),body=ui.page(root);feedback(body);
        PaymentState.Row payment=state().current();
        if(payment==null){
            summary(body,"Thanh toán học phí","VNPay","");
            if(!state().requestedType.isEmpty()&&state().requestedStudent.equals(state().owner))action(body,"Tiếp tục với VNPay",Ui.PRIMARY,()->state().checkout(this::openCheckout));
            else if(!state().requestedStudent.equals(state().owner))ui.add(body,ui.text("Chưa có giao dịch.",14,Ui.MUTED,false));
            else ui.add(body,ui.text("Đang tải giao dịch…",15,Ui.MUTED,false));
            return root;
        }
        String status=payment.text("status");boolean student=payment.text("student_id").equals(state().owner),teacher=payment.text("tutor_id").equals(state().owner);
        LinearLayout receipt=ui.card();
        ui.add(receipt,statusPill(status));ui.space(receipt,14);
        ui.add(receipt,ui.text(payment.text("title").isEmpty()?"Học phí":payment.text("title"),18,Ui.INK,true));ui.space(receipt,16);
        ui.add(receipt,ui.text("Tổng học phí",13,Ui.MUTED,false));ui.space(receipt,6);
        ui.add(receipt,ui.text(PaymentState.money(payment.number("amount")),30,Ui.BLUE,true));ui.space(receipt,16);ui.line(receipt);ui.space(receipt,8);
        fact(receipt,"Mã đơn",String.valueOf(payment.number("order_code")));
        fact(receipt,"Phương thức","VNPay");
        if(!payment.text("tutor_name").isEmpty())fact(receipt,"Gia sư",payment.text("tutor_name"));
        fact(receipt,"Số buổi",payment.number("session_count")+" buổi");
        if(payment.number("received_amount")>0&&payment.number("received_amount")!=payment.number("amount"))fact(receipt,"Đã nhận",PaymentState.money(payment.number("received_amount")));
        if(payment.time("paid_at")>0)fact(receipt,"Thanh toán lúc",date(payment.time("paid_at")));
        if(payment.number("refund_total")>0)fact(receipt,"Đã hoàn",PaymentState.money(payment.number("refund_total")));
        ui.add(body,receipt);ui.space(body,16);
        if(payment.flag("needs_review")){ui.add(body,ui.note(R.drawable.ic_info,payment.text("review_reason"),Ui.ORANGE_BG,Ui.ORANGE,Ui.INK));ui.space(body,12);}
        if(student&&java.util.Arrays.asList("CREATING","WAITING","ERROR","EXPIRED","CANCELLED","FAILED").contains(status))action(body,"Thanh toán qua VNPay",Ui.PRIMARY,()->state().checkout(this::openCheckout));
        action(body,"Kiểm tra thanh toán",Ui.OUTLINE,()->state().refresh());
        ui.space(body,10);LinearLayout heading=ui.row();ui.weight(heading,ui.text("Các buổi trong khóa",18,Ui.INK,true));
        int completed=0;for(PaymentState.Row row:state().sessions.values())if("COMPLETED".equals(row.text("status")))completed++;
        heading.addView(ui.pill(completed+" / "+payment.number("session_count"),12,Ui.BLUE,Ui.PALE));ui.add(body,heading);ui.space(body,12);
        long shown=allSessions?payment.number("session_count"):Math.min(4,payment.number("session_count"));
        for(int n=1;n<=shown;n++){
            final int number=n;PaymentState.Row session=state().sessions.get(String.valueOf(n));String sessionStatus=session==null?"":session.text("status");
            LinearLayout card=ui.card(),row=ui.row();ui.weight(row,ui.text("Buổi "+n,16,Ui.INK,true));
            row.addView(session==null?ui.pill("Chưa hoàn thành",12,Ui.MUTED,Ui.PALE):statusPill(sessionStatus));ui.add(card,row);
            if("PAID".equals(status)){
                if(teacher&&session==null){ui.space(card,12);action(card,"Báo đã hoàn thành buổi "+n,Ui.OUTLINE,()->ui.dialog().setTitle("Xác nhận đã dạy")
                        .setMessage("Xác nhận đã hoàn thành buổi "+number+"?")
                        .setNegativeButton("Đóng",null).setPositiveButton("Đã dạy",(d,w)->state().session(number,"propose","")).show());
                }
                if(student&&"PROPOSED".equals(sessionStatus)){
                    ui.space(card,12);
                    action(card,"Xác nhận đã học buổi "+n,Ui.PRIMARY,()->ui.dialog().setTitle("Xác nhận hoàn thành")
                            .setMessage("Xác nhận đã học buổi "+number+"?")
                            .setNegativeButton("Đóng",null).setPositiveButton("Xác nhận",(d,w)->state().session(number,"confirm","")).show());
                    action(card,"Báo vấn đề",Ui.OUTLINE,()->prompt("Phản ánh buổi "+number,"Mô tả vấn đề",reason->state().session(number,"dispute",reason)));
                }
            }
            if(session!=null&&"DISPUTED".equals(sessionStatus)){
                ui.space(card,10);ui.add(card,ui.text(session.text("reason"),14,Ui.RED,false));
                if(state().admin)action(card,"Mở lại xác nhận sau đối soát",Ui.OUTLINE,()->prompt("Ghi chú xử lý phản ánh","Kết quả đã trao đổi với hai bên",reason->state().session(number,"reopen",reason)));
            }
            ui.add(body,card);ui.space(body,10);
        }
        if(payment.number("session_count")>4)action(body,allSessions?"Thu gọn danh sách buổi":"Xem tất cả "+payment.number("session_count")+" buổi",Ui.OUTLINE,()->{allSessions=!allSessions;refresh();});
        if((student||state().admin)&&java.util.Arrays.asList("PAID","PAID_REVIEW","CLOSED_REFUNDED").contains(status)){
            ui.space(body,8);action(body,"Yêu cầu hoàn học phí chưa sử dụng",Ui.OUTLINE,()->prompt("Yêu cầu hoàn tiền","Lý do dừng khóa hoặc cần hoàn tiền",state()::refund));
        }
        return root;
    }
    private void openCheckout(String value){
        if(state().sandbox()){
            vnpay.launch(new Intent(requireContext(),VnpayPaymentActivity.class).putExtra(VnpayPaymentActivity.PAYMENT,state().selected).putExtra(VnpayPaymentActivity.URL,value));return;
        }
        try{Uri uri=Uri.parse(value);boolean valid=("sandbox.vnpayment.vn".equals(uri.getHost())&&"/paymentv2/vpcpay.html".equals(uri.getPath()))||("pay.vnpay.vn".equals(uri.getHost())&&"/vpcpay.html".equals(uri.getPath()));
            if(!"https".equals(uri.getScheme())||!valid||uri.getUserInfo()!=null||(uri.getPort()!=-1&&uri.getPort()!=443)||uri.getFragment()!=null)throw new IllegalArgumentException();startActivity(new Intent(Intent.ACTION_VIEW,uri));}
        catch(Exception e){state().error="Chưa mở được trang VNPay. Vui lòng thử lại.";state().changed();}
    }
}
