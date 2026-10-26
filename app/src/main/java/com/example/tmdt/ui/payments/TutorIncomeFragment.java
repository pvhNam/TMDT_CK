package com.example.tmdt.ui.payments;

import android.view.View;
import android.widget.LinearLayout;
import com.example.tmdt.PaymentState;
import com.example.tmdt.ui.common.Ui;

public final class TutorIncomeFragment extends PaymentScreen {
    @Override protected View build(){
        LinearLayout root=root("Thu nhập gia sư"),body=ui.page(root);feedback(body);
        action(body,"Tài khoản ngân hàng nhận tiền",Ui.OUTLINE,()->host().show("payout_bank"));
        action(body,"Chọn tháng · "+state().incomePeriod,Ui.OUTLINE,()->prompt("Tháng ghi nhận thu nhập","YYYY-MM",value->{try{state().incomePeriod=java.time.YearMonth.parse(value).toString();state().changed();}catch(Exception e){state().error="Nhập tháng theo dạng YYYY-MM.";state().changed();}}));
        long total=0,paid=0;int count=0;
        for(PaymentState.Row earning:state().earnings.values())if(earning.text("tutor_id").equals(state().owner)&&earning.text("period").equals(state().incomePeriod)){
            total+=earning.number("amount");count++;PaymentState.Row payout=state().payouts.get(earning.text("payout_id"));if(payout!=null&&"PAID".equals(payout.text("status")))paid+=earning.number("amount");
        }
        summary(body,"Tháng "+state().incomePeriod,count+" lượt học đã được xác nhận · Đến hạn ngày 5 tháng sau",PaymentState.money(total));
        summary(body,"Chờ chi trả","Đã nhận trong kỳ: "+PaymentState.money(paid),PaymentState.money(total-paid));
        for(PaymentState.Row payout:newest(state().payouts.values()))if(payout.text("tutor_id").equals(state().owner)){
            LinearLayout card=ui.card();ui.add(card,ui.text("Kỳ "+payout.text("period"),17,Ui.INK,true));ui.space(card,6);
            ui.add(card,ui.text(PaymentState.money(payout.number("amount")),22,Ui.BLUE,true));ui.space(card,6);
            ui.add(card,ui.text(PaymentState.payoutStatus(payout.text("status"))+" · "+payout.number("session_count")+" lượt học",14,Ui.MUTED,false));
            ui.clickable(card,()->{state().selectedPayout=payout.id;state().origin="tutor_income";host().show("payout_detail");});ui.add(body,card);ui.space(body,12);
        }
        ui.add(body,ui.text("Các khoản chưa lập bảng vẫn được giữ trong thu nhập. Với lớp nhóm, tiền dạy được ghi nhận riêng theo buổi và học sinh đã thanh toán.",14,Ui.MUTED,false));
        return root;
    }
}
