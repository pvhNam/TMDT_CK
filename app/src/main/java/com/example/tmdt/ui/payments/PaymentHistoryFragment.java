package com.example.tmdt.ui.payments;

import android.view.View;
import android.widget.LinearLayout;
import com.example.tmdt.PaymentState;
import com.example.tmdt.ui.common.Ui;

public final class PaymentHistoryFragment extends PaymentScreen {
    @Override protected View build(){
        LinearLayout root=root(state().admin?"Quản lý thanh toán":"Thanh toán của tôi"),body=ui.page(root);feedback(body);
        if(state().admin){ui.tabs(body,new String[]{"Học phí","Hoàn tiền"},state().adminPaymentTab,i->{state().adminPaymentTab=i;scrollToTop();refresh();});ui.space(body,16);}
        if(!state().admin||state().adminPaymentTab==0){
        if(state().tutor)action(body,"Thu nhập và tài khoản nhận tiền",Ui.OUTLINE,()->host().show("tutor_income"));
        LinearLayout heading=ui.row();ui.weight(heading,ui.text("Lịch sử giao dịch",19,Ui.INK,true));heading.addView(ui.pill(String.valueOf(state().payments.size()),12,Ui.BLUE,Ui.PALE));ui.add(body,heading);ui.space(body,14);
        for(PaymentState.Row payment:newest(state().payments.values())){
            LinearLayout card=ui.card(),top=ui.row();
            ui.weight(top,ui.text(payment.text("title").isEmpty()?"Thanh toán học phí":payment.text("title"),17,Ui.INK,true));ui.gap(top,8);top.addView(ui.text("›",25,Ui.BLUE,false));ui.add(card,top);ui.space(card,8);
            ui.add(card,ui.text(payment.text("tutor_name")+" · "+payment.number("session_count")+" buổi",13,Ui.MUTED,false));ui.space(card,14);
            ui.add(card,ui.text(PaymentState.money(payment.number("amount")),24,Ui.BLUE,true));ui.space(card,10);
            ui.add(card,statusPill(payment.text("status")));
            if(payment.time("created_at")>0){ui.space(card,10);ui.add(card,ui.text(date(payment.time("created_at")),12,Ui.MUTED,false));}
            card.setContentDescription("Xem giao dịch "+(payment.text("title").isEmpty()?payment.id:payment.text("title")));
            ui.clickable(card,()->state().openExisting(payment.id,"payments"));ui.add(body,card);ui.space(body,12);
        }
        }
        if(state().admin&&state().adminPaymentTab==1){
            ui.space(body,10);ui.add(body,ui.text("Yêu cầu hoàn tiền",19,Ui.INK,true));ui.space(body,10);
            for(PaymentState.Row refund:newest(state().refunds.values())){
                summary(body,refund.text("title"),refund.text("reason")+"\n"+PaymentState.status(refund.text("status")),PaymentState.money(refund.number("amount")));
                if("PENDING".equals(refund.text("status"))){
                    action(body,"Xác nhận đã chuyển hoàn tiền",Ui.OUTLINE,()->{
                    state().selectedRefund=refund.id;prompt("Xác nhận đã hoàn tiền","Mã giao dịch ngân hàng",state()::finishRefund);
                    });
                    action(body,"Từ chối yêu cầu hoàn tiền",Ui.OUTLINE,()->{state().selectedRefund=refund.id;prompt("Từ chối hoàn tiền","Lý do từ chối",state()::rejectRefund);});
                }
            }
        }
        return root;
    }
}
