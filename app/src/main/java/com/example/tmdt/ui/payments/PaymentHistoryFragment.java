package com.example.tmdt.ui.payments;

import android.view.View;
import android.widget.LinearLayout;
import com.example.tmdt.PaymentState;
import com.example.tmdt.ui.common.Ui;

public final class PaymentHistoryFragment extends PaymentScreen {
    @Override protected View build(){
        LinearLayout root=root(state().admin?"Quản lý thanh toán":"Thanh toán của tôi"),body=ui.page(root);feedback(body);
        if(state().admin)action(body,"Chi trả gia sư theo tháng",Ui.PRIMARY,()->host().show("admin_payroll"));
        if(state().tutor)action(body,"Thu nhập và tài khoản nhận tiền",Ui.OUTLINE,()->host().show("tutor_income"));
        ui.add(body,ui.text("Học phí chuyển về hệ thống. Gia sư được đối soát theo các buổi đã hoàn thành, đến hạn ngày 5 tháng sau.",14,Ui.MUTED,false));ui.space(body,18);
        if(state().payments.isEmpty())summary(body,"Chưa có giao dịch","Mở buổi học, hợp đồng hoặc đăng ký lớp đã được chấp nhận để thanh toán.","");
        for(PaymentState.Row payment:newest(state().payments.values())){
            LinearLayout card=ui.card();ui.add(card,ui.text(payment.text("title"),17,Ui.INK,true));ui.space(card,6);
            ui.add(card,ui.text(payment.text("tutor_name")+" · "+payment.number("session_count")+" buổi",14,Ui.MUTED,false));ui.space(card,8);
            ui.add(card,ui.text(PaymentState.money(payment.number("amount")),21,Ui.BLUE,true));ui.space(card,6);
            ui.add(card,ui.text(PaymentState.status(payment.text("status")),14,Ui.INK,true));
            ui.clickable(card,()->state().openExisting(payment.id,"payments"));ui.add(body,card);ui.space(body,12);
        }
        if(state().admin){
            ui.space(body,10);ui.add(body,ui.text("Yêu cầu hoàn tiền",19,Ui.INK,true));ui.space(body,10);
            for(PaymentState.Row refund:newest(state().refunds.values())){
                summary(body,refund.text("title"),refund.text("reason")+"\n"+PaymentState.status(refund.text("status")),PaymentState.money(refund.number("amount")));
                if("PENDING".equals(refund.text("status"))){
                    ui.add(body,ui.text("Đối soát số tiền và tài khoản hoàn với học sinh trước khi chuyển.",13,Ui.MUTED,false));ui.space(body,8);
                    action(body,"Xác nhận đã chuyển hoàn tiền",Ui.OUTLINE,()->{
                    state().selectedRefund=refund.id;prompt("Chỉ xác nhận sau khi đã chuyển tiền","Mã giao dịch ngân hàng thực tế",state()::finishRefund);
                    });
                    action(body,"Từ chối yêu cầu hoàn tiền",Ui.OUTLINE,()->{state().selectedRefund=refund.id;prompt("Kết quả đối soát yêu cầu hoàn","Lý do từ chối sau khi trao đổi với học sinh",state()::rejectRefund);});
                }
            }
        }
        return root;
    }
}
