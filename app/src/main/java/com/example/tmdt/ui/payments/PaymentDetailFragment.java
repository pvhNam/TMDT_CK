package com.example.tmdt.ui.payments;

import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.LinearLayout;
import com.example.tmdt.PaymentState;
import com.example.tmdt.R;
import com.example.tmdt.ui.common.Ui;

public final class PaymentDetailFragment extends PaymentScreen {
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
            summary(body,"Thanh toán học phí","Hệ thống sẽ lấy số tiền theo đăng ký đã được gia sư chấp nhận. Bạn kiểm tra lại số tiền trên trang VNPay trước khi chuyển khoản.","");
            if(!state().requestedType.isEmpty()&&state().requestedStudent.equals(state().owner))action(body,"Tiếp tục với VNPay",Ui.PRIMARY,()->state().checkout(this::openCheckout));
            else if(!state().requestedStudent.equals(state().owner))ui.add(body,ui.text("Học sinh chưa tạo giao dịch cho đăng ký này.",15,Ui.MUTED,false));
            else ui.add(body,ui.text("Đang tải giao dịch…",15,Ui.MUTED,false));
            return root;
        }
        String status=payment.text("status");boolean student=payment.text("student_id").equals(state().owner),teacher=payment.text("tutor_id").equals(state().owner);
        summary(body,payment.text("title"),payment.text("tutor_name")+" · "+payment.number("session_count")+" buổi\n"+PaymentState.status(status),PaymentState.money(payment.number("amount")));
        ui.add(body,ui.text("Mã đơn: "+payment.number("order_code"),13,Ui.MUTED,false));ui.space(body,10);
        if(payment.flag("needs_review")){ui.add(body,ui.note(R.drawable.ic_info,payment.text("review_reason"),Ui.ORANGE_BG,Ui.ORANGE,Ui.INK));ui.space(body,12);}
        if(student&&java.util.Arrays.asList("CREATING","WAITING","ERROR","EXPIRED","CANCELLED","FAILED").contains(status))action(body,"Thanh toán qua VNPay",Ui.PRIMARY,()->state().checkout(this::openCheckout));
        action(body,"Kiểm tra thanh toán",Ui.OUTLINE,()->state().refresh());
        if(payment.time("paid_at")>0)ui.add(body,ui.text("Đã nhận tiền: "+PaymentState.money(payment.number("received_amount"))+" · "+date(payment.time("paid_at")),13,Ui.GREEN,false));
        if(payment.number("refund_total")>0)ui.add(body,ui.text("Đã hoàn: "+PaymentState.money(payment.number("refund_total")),13,Ui.MUTED,false));
        ui.space(body,12);ui.add(body,ui.text("Các buổi trong khóa",19,Ui.INK,true));ui.space(body,6);
        ui.add(body,ui.text("Gia sư báo đã dạy, học sinh xác nhận rồi tiền buổi đó mới được cộng vào kỳ chi trả. Mỗi buổi chỉ được tính một lần.",14,Ui.MUTED,false));ui.space(body,12);
        for(int n=1;n<=payment.number("session_count");n++){
            final int number=n;PaymentState.Row session=state().sessions.get(String.valueOf(n));String sessionStatus=session==null?"":session.text("status");
            LinearLayout card=ui.card();ui.add(card,ui.text("Buổi "+n,16,Ui.INK,true));ui.space(card,6);
            ui.add(card,ui.text(session==null?"Chưa báo hoàn thành":PaymentState.status(sessionStatus),14,Ui.MUTED,false));ui.space(card,8);
            if("PAID".equals(status)){
                if(teacher&&session==null)action(card,"Báo đã hoàn thành buổi "+n,Ui.OUTLINE,()->ui.dialog().setTitle("Xác nhận đã dạy")
                        .setMessage("Buổi "+number+" đã thực tế diễn ra? Học sinh sẽ nhận yêu cầu xác nhận.")
                        .setNegativeButton("Đóng",null).setPositiveButton("Đã dạy",(d,w)->state().session(number,"propose","")).show());
                if(student&&"PROPOSED".equals(sessionStatus)){
                    action(card,"Xác nhận đã học buổi "+n,Ui.PRIMARY,()->ui.dialog().setTitle("Xác nhận hoàn thành")
                            .setMessage("Bạn xác nhận buổi "+number+" đã diễn ra? Tiền dạy sẽ được ghi nhận cho gia sư.")
                            .setNegativeButton("Đóng",null).setPositiveButton("Xác nhận",(d,w)->state().session(number,"confirm","")).show());
                    action(card,"Báo vấn đề",Ui.OUTLINE,()->prompt("Phản ánh buổi "+number,"Mô tả vấn đề",reason->state().session(number,"dispute",reason)));
                }
            }
            if(session!=null&&"DISPUTED".equals(sessionStatus)){
                ui.add(card,ui.text(session.text("reason"),14,Ui.RED,false));
                if(state().admin)action(card,"Mở lại xác nhận sau đối soát",Ui.OUTLINE,()->prompt("Ghi chú xử lý phản ánh","Kết quả đã trao đổi với hai bên",reason->state().session(number,"reopen",reason)));
            }
            ui.add(body,card);ui.space(body,10);
        }
        if((student||state().admin)&&java.util.Arrays.asList("PAID","PAID_REVIEW","CLOSED_REFUNDED").contains(status)){
            ui.space(body,8);action(body,"Yêu cầu hoàn học phí chưa sử dụng",Ui.OUTLINE,()->prompt("Yêu cầu hoàn tiền","Lý do dừng khóa hoặc cần hoàn tiền",state()::refund));
        }
        ui.space(body,10);ui.add(body,ui.note(R.drawable.ic_info,"Tiền dạy được ghi nhận theo tháng học sinh xác nhận hoàn thành. Đến ngày 5 tháng sau, admin đối soát và chi trả.",Ui.PALE,Ui.BLUE,Ui.MUTED));
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
