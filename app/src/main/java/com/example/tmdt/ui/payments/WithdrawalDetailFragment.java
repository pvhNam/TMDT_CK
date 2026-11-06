package com.example.tmdt.ui.payments;

import android.view.View;
import android.widget.LinearLayout;
import com.example.tmdt.PaymentState;
import com.example.tmdt.TutorWallet;
import com.example.tmdt.ui.common.StableViews;
import com.example.tmdt.ui.common.Ui;
import java.util.Arrays;
import java.util.Map;

public final class WithdrawalDetailFragment extends PaymentScreen {
    private LinearLayout body;
    @Override protected View build(){LinearLayout root=root("Chi tiết rút tiền");body=ui.page(root);render();return root;}
    @Override protected boolean updateContent(){if(body==null)return false;render();return true;}
    private void render(){
        StableViews rows=StableViews.at(body);rows.begin();
        if(!state().error.isEmpty()||!state().notice.isEmpty()||state().busy)rows.add("feedback",Arrays.asList(state().error,state().notice,state().busy),()->{LinearLayout box=ui.column();feedback(box);return box;},0);
        PaymentState.Row row=state().withdrawals.get(state().selectedWithdrawal);
        if(row==null){rows.add("loading",true,()->ui.text("Đang tải giao dịch rút tiền…",14,Ui.MUTED,false),0);rows.commit();return;}
        boolean pending="PENDING".equals(row.text("status")),processing="PROCESSING".equals(row.text("status")),paid="PAID".equals(row.text("status"));
        rows.add("summary",row,()->{
            LinearLayout card=ui.card();ui.cardSurface(card,Ui.PALE,Ui.BORDER);ui.add(card,ui.text(row.text("tutor_name"),17,Ui.INK,true));ui.space(card,12);
            ui.add(card,ui.text(PaymentState.money(row.number("amount")),27,Ui.INK,true));ui.space(card,10);
            ui.add(card,ui.text(TutorWallet.withdrawalStatus(row.text("status")),13,paid?Ui.GREEN:Ui.MUTED,true));ui.space(card,14);
            fact(card,"Ngày tạo",date(row.time("created_at")));return card;
        },ui.dp(14));
        Map<String,Object> bank=row.map("bank");rows.add("bank",bank,()->{
            LinearLayout card=ui.card();ui.add(card,ui.text("Ngân hàng nhận tiền",15,Ui.INK,true));ui.space(card,10);
            fact(card,"Ngân hàng",value(bank,"bank_name"));fact(card,"Số tài khoản",value(bank,"account_number"));fact(card,"Chủ tài khoản",value(bank,"account_name"));return card;
        },ui.dp(14));
        if(!pending&&!processing)rows.add("result",row,()->{
            LinearLayout card=ui.card();fact(card,"Ngày xử lý",date(row.time("processed_at")));
            if(paid)fact(card,"Mã giao dịch",row.text("bank_reference"));
            else{ui.space(card,6);ui.add(card,ui.text(row.text("review_note"),13,Ui.MUTED,false));ui.space(card,8);ui.add(card,ui.text("Đã hoàn "+PaymentState.money(row.number("amount"))+" về số dư khả dụng.",13,Ui.GREEN,true));}return card;
        },ui.dp(14));
        if((pending||processing)&&!state().admin&&state().owner.equals(row.text("tutor_id"))){
            rows.add("actions",Arrays.asList(row.id,row.text("status"),state().busy),()->{
                LinearLayout actions=ui.column();
                ui.addAction(actions,ui.action("Tiếp tục rút tiền",0,state().busy?Ui.DISABLED:Ui.PRIMARY,state().busy?null:()->ui.dialog().setTitle("Xác nhận rút tiền")
                        .setMessage(PaymentState.money(row.number("amount"))+"\n"+value(bank,"bank_name")+"\n"+value(bank,"account_number"))
                        .setNegativeButton("Đóng",null).setPositiveButton("Rút tiền",(dialog,which)->state().completeWithdrawal(row.id)).show()),48);ui.space(actions,10);
                ui.addAction(actions,ui.action("Hủy yêu cầu rút tiền",0,state().busy?Ui.DISABLED:Ui.OUTLINE,state().busy?null:()->ui.dialog().setTitle("Hủy yêu cầu rút tiền?")
                        .setMessage(PaymentState.money(row.number("amount"))).setNegativeButton("Đóng",null).setPositiveButton("Hủy yêu cầu",(dialog,which)->state().cancelWithdrawal(row.id)).show()),48);
                return actions;
            },0);
        }rows.commit();
    }
    private String value(Map<String,Object> bank,String key){Object value=bank.get(key);return value instanceof String?(String)value:"";}
    @Override public void onDestroyView(){super.onDestroyView();body=null;}
}
