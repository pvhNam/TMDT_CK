package com.example.tmdt.ui.payments;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.tmdt.PaymentState;
import com.example.tmdt.R;
import com.example.tmdt.ReportPeriod;
import com.example.tmdt.TutorWallet;
import com.example.tmdt.ui.common.RenderSnapshot;
import com.example.tmdt.ui.common.StableViews;
import com.example.tmdt.ui.common.Ui;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;

public final class PayoutDetailFragment extends PaymentScreen {
    private LinearLayout body;
    @Override public void onViewCreated(@NonNull View view,@Nullable Bundle saved){
        super.onViewCreated(view,saved);host().administration.changes.observe(getViewLifecycleOwner(),ignored->refreshData());
    }
    @Override public void onResume(){super.onResume();refreshData();}
    @Override protected Object dataSnapshot(){return RenderSnapshot.of(super.dataSnapshot(),host().administration.users,host().administration.tutors,LocalDate.now(ReportPeriod.ZONE));}
    @Override protected View build(){
        LinearLayout root=root("Chi tiết bảng lương");body=ui.page(root);render();return root;
    }
    @Override protected boolean updateContent(){if(body==null)return false;render();return true;}
    private void render(){
        StableViews rows=StableViews.at(body);rows.begin();
        if(!state().error.isEmpty()||!state().notice.isEmpty()||state().busy)
            rows.add("feedback",Arrays.asList(state().error,state().notice,state().busy),()->{LinearLayout box=ui.column();feedback(box);return box;},0);
        PaymentState.Row payout=state().payouts.get(state().selectedPayout);
        if(payout==null){rows.add("loading",true,()->ui.text("Đang tải bảng lương…",14,Ui.MUTED,false),0);rows.commit();return;}
        boolean credited="CREDITED".equals(payout.text("status")),paid=TutorWallet.salaryPaid(payout.text("status")),cancelled="CANCELLED".equals(payout.text("status"));
        PaymentState.Row user=host().administration.users.get(payout.text("tutor_id"));if(user==null)user=host().administration.tutors.get(payout.text("tutor_id"));
        boolean blocked=user!=null&&"BANNED".equals(user.text("trang_thai"));
        rows.add("summary",Arrays.asList(payout,blocked),()->{
            LinearLayout card=ui.card(),top=ui.row(),name=ui.column();ui.cardSurface(card,Ui.PALE,Ui.BORDER);
            top.addView(ui.avatar(payout.text("tutor_id"),payout.text("tutor_name"),48));ui.gap(top,12);
            ui.add(name,ui.text(payout.text("tutor_name"),17,Ui.INK,true));ui.space(name,5);
            ui.add(name,ui.text("Kỳ "+period(payout.text("period"))+" · "+payout.number("session_count")+" buổi học",12,Ui.MUTED,false));ui.weight(top,name);ui.add(card,top);ui.space(card,14);
            ui.add(card,ui.pill(blocked&&!paid&&!cancelled?"Tạm dừng chi trả":PaymentState.payoutStatus(payout.text("status")),11,
                    paid?Ui.GREEN:cancelled?Ui.MUTED:blocked?Ui.RED:Ui.ORANGE,paid?Ui.GREEN_BG:cancelled?Ui.WHITE:blocked?0xFFFFE9EC:Ui.ORANGE_BG));ui.space(card,16);
            ui.add(card,ui.text("Số tiền chi trả",12,Ui.MUTED,false));ui.space(card,6);
            android.widget.TextView amount=ui.text(PaymentState.money(payout.number("amount")),27,Ui.INK,true);amount.setMaxLines(1);amount.setAutoSizeTextTypeUniformWithConfiguration(18,27,1,android.util.TypedValue.COMPLEX_UNIT_SP);ui.add(card,amount);
            if(payout.time("due_at")>0){ui.space(card,12);ui.add(card,ui.fact(R.drawable.ic_calendar,"Ngày chi trả: "+date(payout.time("due_at")).substring(0,10),18,13,Ui.INK,false));}return card;
        },ui.dp(14));
        Map<String,Object> bank=payout.map("bank");
        if("PAID".equals(payout.text("status")))rows.add("bank",bank,()->{
            LinearLayout card=ui.card();ui.add(card,ui.text("Tài khoản nhận lương",15,Ui.INK,true));ui.space(card,12);
            fact(card,"Ngân hàng",bankValue(bank,"bank_name"));
            fact(card,"Số tài khoản",bankValue(bank,"account_number"));
            fact(card,"Chủ tài khoản",bankValue(bank,"account_name"));return card;
        },ui.dp(14));
        else rows.add("destination",state().wallets.get(payout.text("tutor_id")),()->{
            LinearLayout card=ui.card();fact(card,"Nơi nhận lương","Ví gia sư");
            PaymentState.Row wallet=state().wallets.get(payout.text("tutor_id"));
            if(wallet!=null)fact(card,"Số dư khả dụng",PaymentState.money(TutorWallet.available(wallet)));return card;
        },ui.dp(14));
        if(paid)rows.add("receipt",payout,()->{
            LinearLayout card=ui.card();ui.add(card,ui.text(credited?"Đã vào ví":"Đã chi trả",15,Ui.GREEN,true));ui.space(card,8);
            fact(card,credited?"Ngày vào ví":"Ngày chuyển",date(payout.time(credited?"credited_at":"paid_at")));
            if(!credited)fact(card,"Mã giao dịch",payout.text("bank_reference"));return card;
        },ui.dp(14));
        else if(cancelled&&!payout.text("cancel_reason").isEmpty())rows.add("cancelled",payout.text("cancel_reason"),()->{
            LinearLayout card=ui.card();ui.add(card,ui.text("Lý do hủy",14,Ui.INK,true));ui.space(card,6);ui.add(card,ui.text(payout.text("cancel_reason"),13,Ui.MUTED,false));return card;
        },ui.dp(14));
        else if(state().admin&&"READY".equals(payout.text("status"))){
            boolean due=payout.time("due_at")>0&&System.currentTimeMillis()>=payout.time("due_at");
            rows.add("actions",Arrays.asList(payout.id,blocked,due,state().busy),()->{
                LinearLayout box=ui.column();boolean enabled=!state().busy&&!blocked&&due;
                String title=blocked?"Gia sư đang bị khóa":!due?"Chưa đến ngày chi trả":"Trả lương vào ví";
                ui.addAction(box,ui.action(title,0,enabled?Ui.PRIMARY:Ui.DISABLED,enabled?()->ui.dialog().setTitle("Trả lương vào ví?")
                        .setMessage(payout.text("tutor_name")+"\n"+PaymentState.money(payout.number("amount"))+"\nKỳ "+period(payout.text("period")))
                        .setNegativeButton("Đóng",null).setPositiveButton("Trả lương",(dialog,which)->state().creditSalary(payout.id)).show():null),48);ui.space(box,10);
                ui.addAction(box,ui.action("Hủy bảng lương",0,state().busy?Ui.DISABLED:Ui.OUTLINE,state().busy?null:()->prompt("Hủy bảng lương","Lý do hủy",state()::cancelPayout)),48);return box;
            },ui.dp(18));
        }
        java.util.List<PaymentState.Row> earnings=new java.util.ArrayList<>();
        for(PaymentState.Row earning:newest(state().earnings.values()))if(earning.text("payout_id").equals(payout.id))earnings.add(earning);
        if(!earnings.isEmpty())rows.add("earning-heading",earnings.size(),()->ui.text("Buổi học trong kỳ ("+earnings.size()+")",16,Ui.INK,true),ui.dp(10));
        for(PaymentState.Row earning:earnings)rows.add("earning/"+earning.id,earning,()->{
            LinearLayout card=ui.card();ui.add(card,ui.text(earning.text("title"),14,Ui.INK,true));ui.space(card,6);
            ui.add(card,ui.text("Buổi "+earning.number("session_number")+" · "+date(earning.time("created_at")),12,Ui.MUTED,false));ui.space(card,10);
            ui.add(card,ui.text(PaymentState.money(earning.number("amount")),17,Ui.BLUE,true));return card;
        },ui.dp(10));
        rows.commit();
    }
    private String bankValue(Map<String,Object> bank,String key){Object value=bank.get(key);return value instanceof String?((String)value).trim():"";}
    private String period(String period){try{return java.time.YearMonth.parse(period).format(java.time.format.DateTimeFormatter.ofPattern("MM/yyyy"));}catch(java.time.format.DateTimeParseException failure){return period;}}
    @Override public void onDestroyView(){super.onDestroyView();body=null;}
}
