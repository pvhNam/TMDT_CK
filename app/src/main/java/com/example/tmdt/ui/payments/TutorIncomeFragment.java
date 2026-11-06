package com.example.tmdt.ui.payments;

import android.view.View;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.tmdt.PaymentState;
import com.example.tmdt.R;
import com.example.tmdt.TutorWallet;
import com.example.tmdt.ui.common.Ui;
import com.example.tmdt.ui.common.StableViews;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

public final class TutorIncomeFragment extends PaymentScreen {
    private LinearLayout body;
    @Override protected View build(){
        LinearLayout root=root("Thu nhập gia sư");body=ui.page(root);render();return root;
    }
    @Override protected boolean updateContent(){if(body==null)return false;render();return true;}
    @Override public void onDestroyView(){super.onDestroyView();body=null;}
    private void render(){
        StableViews rows=StableViews.at(body);rows.begin();
        if(!state().error.isEmpty()||!state().notice.isEmpty()||state().busy)
            rows.add("feedback",java.util.Arrays.asList(state().error,state().notice,state().busy),()->{LinearLayout box=ui.column();feedback(box);return box;},0);
        rows.add("period",state().incomePeriod,this::monthSelector,ui.dp(14));
        long total=0,paid=0;int count=0;
        for(PaymentState.Row earning:state().earnings.values())if(earning.text("tutor_id").equals(state().owner)&&earning.text("period").equals(state().incomePeriod)){
            total+=earning.number("amount");count++;PaymentState.Row payout=state().payouts.get(earning.text("payout_id"));if(payout!=null&&TutorWallet.salaryPaid(payout.text("status")))paid+=earning.number("amount");
        }
        final long earned=total,received=paid;final int sessions=count;
        rows.add("totals",Arrays.asList(state().incomePeriod,total,paid,count),()->{
            LinearLayout card=ui.card(),heading=ui.row();ui.cardSurface(card,Ui.PALE,Ui.BORDER);
            ui.weight(heading,ui.text("Tổng thu nhập",14,Ui.MUTED,false));
            heading.addView(ui.pill(sessions+" buổi",12,Ui.BLUE,Ui.WHITE));ui.add(card,heading);ui.space(card,12);
            ui.add(card,ui.text(PaymentState.money(earned),26,Ui.INK,true));ui.space(card,18);ui.line(card);ui.space(card,16);
            LinearLayout stats=ui.row();
            ui.weight(stats,metric("Đã nhận",received,Ui.GREEN));ui.gap(stats,16);
            ui.weight(stats,metric("Chờ chi trả",earned-received,Ui.INK));ui.add(card,stats);return card;
        },ui.dp(12));
        rows.add("bank",true,()->{
            LinearLayout card=ui.row();ui.pad(card,14,8);ui.surface(card,Ui.WHITE,16,Ui.BORDER);
            card.addView(ui.icon(R.drawable.ic_wallet,22,Ui.BLUE));ui.gap(card,12);
            TextView label=ui.text("Ví nhận lương",14,Ui.INK,true);label.setMinHeight(ui.dp(40));label.setGravity(Gravity.CENTER_VERTICAL);
            ui.weight(card,label);card.addView(ui.icon(R.drawable.ic_right,18,Ui.MUTED));
            ui.clickable(card,()->host().show("tutor_wallet"));return card;
        },ui.dp(20));
        List<PaymentState.Row> payouts=new ArrayList<>();
        for(PaymentState.Row payout:newest(state().payouts.values()))
            if(payout.text("tutor_id").equals(state().owner)&&payout.text("period").equals(state().incomePeriod))payouts.add(payout);
        if(!payouts.isEmpty())rows.add("payout-heading",payouts.size(),()->{
            LinearLayout heading=ui.row();ui.weight(heading,ui.text("Chi trả kỳ này",16,Ui.INK,true));
            heading.addView(ui.pill(String.valueOf(payouts.size()),12,Ui.BLUE,Ui.PALE));return heading;
        },ui.dp(12));
        for(PaymentState.Row payout:payouts){
            rows.add("payout/"+payout.id,payout,()->{
            LinearLayout card=ui.card(),top=ui.row();boolean completed=TutorWallet.salaryPaid(payout.text("status"));
            ui.weight(top,ui.text(payout.number("session_count")+" buổi học",13,Ui.MUTED,false));
            top.addView(ui.pill(PaymentState.payoutStatus(payout.text("status")),11,completed?Ui.GREEN:Ui.ORANGE,completed?Ui.GREEN_BG:Ui.ORANGE_BG));
            ui.add(card,top);ui.space(card,12);
            LinearLayout amount=ui.row();ui.weight(amount,ui.text(PaymentState.money(payout.number("amount")),20,Ui.INK,true));
            amount.addView(ui.icon(R.drawable.ic_right,18,Ui.BLUE));ui.add(card,amount);
            card.setContentDescription("Xem khoản chi trả "+payout.text("period")+", "+PaymentState.money(payout.number("amount")));
            ui.clickable(card,()->{state().selectedPayout=payout.id;state().origin="tutor_income";host().show("payout_detail");});return card;
            },ui.dp(12));
        }
        rows.commit();
    }

    private LinearLayout metric(String label,long amount,int color){
        LinearLayout box=ui.column();ui.add(box,ui.text(label,12,Ui.MUTED,false));ui.space(box,6);
        ui.add(box,ui.text(PaymentState.money(amount),16,color,true));return box;
    }
    private YearMonth selectedMonth(){
        try{return YearMonth.parse(state().incomePeriod);}catch(java.time.format.DateTimeParseException e){return YearMonth.now(ZoneId.of("Asia/Ho_Chi_Minh"));}
    }
    private View monthSelector(){
        LinearLayout bar=ui.row();ui.surface(bar,Ui.WHITE,16,Ui.BORDER);
        bar.addView(ui.iconButton(R.drawable.ic_back,Ui.BLUE,"Tháng trước",()->selectMonth(selectedMonth().minusMonths(1))));
        TextView month=ui.link("Tháng "+selectedMonth().format(DateTimeFormatter.ofPattern("MM/yyyy")),15,this::chooseMonth);
        month.setGravity(Gravity.CENTER);month.setMinHeight(ui.dp(48));ui.weight(bar,month);
        bar.addView(ui.iconButton(R.drawable.ic_right,Ui.BLUE,"Tháng sau",()->selectMonth(selectedMonth().plusMonths(1))));return bar;
    }
    private void selectMonth(YearMonth month){state().incomePeriod=month.toString();state().changed();}
    private void chooseMonth(){
        TreeSet<YearMonth> months=new TreeSet<>(Collections.reverseOrder());months.add(selectedMonth());
        YearMonth current=YearMonth.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        for(int i=0;i<12;i++)months.add(current.minusMonths(i));
        for(PaymentState.Row earning:state().earnings.values())addMonth(months,earning);
        for(PaymentState.Row payout:state().payouts.values())addMonth(months,payout);
        List<YearMonth> choices=new ArrayList<>(months);String[] labels=new String[choices.size()];
        for(int i=0;i<choices.size();i++)labels[i]="Tháng "+choices.get(i).format(DateTimeFormatter.ofPattern("MM/yyyy"));
        ui.dialog().setTitle("Chọn tháng").setSingleChoiceItems(labels,choices.indexOf(selectedMonth()),(dialog,which)->{
            selectMonth(choices.get(which));dialog.dismiss();
        }).setNegativeButton("Đóng",null).show();
    }
    private void addMonth(TreeSet<YearMonth> months,PaymentState.Row row){
        if(!row.text("tutor_id").equals(state().owner))return;
        try{months.add(YearMonth.parse(row.text("period")));}catch(java.time.format.DateTimeParseException ignored){}
    }
}
