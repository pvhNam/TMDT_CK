package com.example.tmdt.ui.payments;

import android.view.View;
import android.widget.LinearLayout;
import com.example.tmdt.PaymentState;
import com.example.tmdt.ui.common.Ui;
import java.util.Map;

public final class PayoutDetailFragment extends PaymentScreen {
    @Override protected View build(){
        LinearLayout root=root("Chi tiết chi trả"),body=ui.page(root);feedback(body);
        PaymentState.Row payout=state().payouts.get(state().selectedPayout);
        if(payout==null){summary(body,"Đang tải bảng chi trả","Hãy mở lại từ danh sách nếu dữ liệu chưa cập nhật.","");return root;}
        summary(body,payout.text("tutor_name"),"Kỳ "+payout.text("period")+" · "+payout.number("session_count")+" lượt học\n"+PaymentState.payoutStatus(payout.text("status")),PaymentState.money(payout.number("amount")));
        Map<String,Object> bank=payout.map("bank");summary(body,"Tài khoản nhận tiền",String.valueOf(bank.getOrDefault("bank_name",""))+"\n"+bank.getOrDefault("account_number","")+"\n"+bank.getOrDefault("account_name",""),"");
        ui.add(body,ui.text("Đến hạn: "+date(payout.time("due_at")),15,Ui.INK,true));ui.space(body,10);
        if("PAID".equals(payout.text("status"))){ui.add(body,ui.text("Đã chi: "+date(payout.time("paid_at"))+"\nMã ngân hàng: "+payout.text("bank_reference"),14,Ui.GREEN,false));}
        else if(state().admin&&"READY".equals(payout.text("status"))){
            action(body,"Xác nhận đã chuyển tiền cho gia sư",Ui.PRIMARY,()->prompt("Chỉ xác nhận sau khi chuyển khoản thành công","Mã giao dịch ngân hàng thực tế",state()::markPayout));
            action(body,"Hủy bảng chưa chi để điều chỉnh",Ui.OUTLINE,()->prompt("Chỉ hủy khi chưa chuyển tiền","Lý do hủy bảng và lập lại",state()::cancelPayout));
        }
        ui.space(body,14);ui.add(body,ui.text("Chi tiết các lượt học",18,Ui.INK,true));ui.space(body,10);
        for(PaymentState.Row earning:newest(state().earnings.values()))if(earning.text("payout_id").equals(payout.id))summary(body,earning.text("title"),"Buổi "+earning.number("session_number")+" · "+date(earning.time("created_at")),PaymentState.money(earning.number("amount")));
        return root;
    }
}
