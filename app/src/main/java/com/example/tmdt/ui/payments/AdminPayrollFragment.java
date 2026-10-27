package com.example.tmdt.ui.payments;

import android.view.View;
import android.widget.LinearLayout;
import com.example.tmdt.PaymentState;
import com.example.tmdt.ui.common.Ui;

public final class AdminPayrollFragment extends PaymentScreen {
    @Override protected View build(){
        LinearLayout root=root("Chi trả gia sư theo tháng"),body=ui.page(root);feedback(body);
        action(body,"Kỳ thanh toán · "+state().payrollPeriod,Ui.OUTLINE,()->prompt("Chọn tháng đã kết thúc","YYYY-MM",value->{try{state().payrollPeriod=java.time.YearMonth.parse(value).toString();state().changed();}catch(Exception e){state().error="Nhập kỳ theo dạng YYYY-MM.";state().changed();}}));
        action(body,"Lập bảng chi trả tháng "+state().payrollPeriod,Ui.PRIMARY,()->ui.dialog().setTitle("Lập bảng đối soát")
                .setMessage("Chỉ gom các lượt học đã được học sinh xác nhận và chưa đưa vào bảng khác. Thao tác này chưa chuyển tiền ngân hàng.")
                .setNegativeButton("Đóng",null).setPositiveButton("Lập bảng",(d,w)->state().preparePayroll()).show());
        long total=0;int count=0;
        for(PaymentState.Row payout:state().payouts.values())if(payout.text("period").equals(state().payrollPeriod)&&!"CANCELLED".equals(payout.text("status"))){total+=payout.number("amount");count++;}
        summary(body,count+" bảng chi trả","Kỳ "+state().payrollPeriod+" · Đến hạn ngày 5 tháng kế tiếp",PaymentState.money(total));
        for(PaymentState.Row payout:newest(state().payouts.values()))if(payout.text("period").equals(state().payrollPeriod)){
            LinearLayout card=ui.card();ui.add(card,ui.text(payout.text("tutor_name"),17,Ui.INK,true));ui.space(card,6);ui.add(card,ui.text(PaymentState.money(payout.number("amount")),22,Ui.BLUE,true));ui.space(card,6);
            ui.add(card,ui.text(payout.number("session_count")+" lượt học · "+PaymentState.payoutStatus(payout.text("status")),14,Ui.MUTED,false));
            ui.clickable(card,()->{state().selectedPayout=payout.id;state().origin="admin_payroll";host().show("payout_detail");});ui.add(body,card);ui.space(body,12);
        }
        return root;
    }
}
