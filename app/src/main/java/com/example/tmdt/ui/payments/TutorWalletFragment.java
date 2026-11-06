package com.example.tmdt.ui.payments;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.tmdt.PaymentState;
import com.example.tmdt.R;
import com.example.tmdt.TutorWallet;
import com.example.tmdt.ui.common.StableViews;
import com.example.tmdt.ui.common.Ui;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public final class TutorWalletFragment extends PaymentScreen {
    private LinearLayout body;
    @Override protected View build(){LinearLayout root=root("Ví gia sư");body=ui.page(root);render();return root;}
    @Override protected boolean updateContent(){if(body==null)return false;render();return true;}
    private void render(){
        StableViews rows=StableViews.at(body);rows.begin();
        if(!state().tutor||state().admin){rows.add("access",true,()->ui.text("Ví dành cho tài khoản gia sư.",14,Ui.MUTED,false),0);rows.commit();return;}
        if(!state().error.isEmpty()||!state().notice.isEmpty()||state().busy)rows.add("feedback",Arrays.asList(state().error,state().notice,state().busy),()->{LinearLayout box=ui.column();feedback(box);return box;},0);
        PaymentState.Row wallet=state().wallet();long earned=0,paid=0;
        for(PaymentState.Row earning:state().earnings.values())if(state().owner.equals(earning.text("tutor_id")))earned+=earning.number("amount");
        for(PaymentState.Row payout:state().payouts.values())if(state().owner.equals(payout.text("tutor_id"))&&TutorWallet.salaryPaid(payout.text("status")))paid+=payout.number("amount");
        final long pendingSalary=Math.max(0,earned-paid),available=TutorWallet.available(wallet),reserved=TutorWallet.reserved(wallet);
        rows.add("balance",Arrays.asList(wallet,pendingSalary,state().walletLoaded,state().busy),()->{
            LinearLayout card=ui.card();ui.cardSurface(card,Ui.PALE,Ui.BORDER);LinearLayout top=ui.row();
            top.addView(ui.icon(R.drawable.ic_wallet,24,Ui.BLUE));ui.gap(top,10);ui.weight(top,ui.text("Số dư khả dụng",14,Ui.MUTED,false));ui.add(card,top);ui.space(card,16);
            TextView amount=ui.text(state().walletLoaded?PaymentState.money(available):"Đang tải số dư…",26,Ui.INK,true);amount.setMaxLines(1);amount.setAutoSizeTextTypeUniformWithConfiguration(16,26,1,android.util.TypedValue.COMPLEX_UNIT_SP);ui.add(card,amount);ui.space(card,18);
            boolean enabled=state().walletLoaded&&available>=TutorWallet.MIN_WITHDRAWAL&&!state().busy;
            ui.addAction(card,ui.action("Rút về ngân hàng",0,enabled?Ui.PRIMARY:Ui.DISABLED,enabled?()->state().beginWithdrawal():null),48);ui.space(card,16);ui.line(card);ui.space(card,14);
            LinearLayout stats=ui.row();ui.weight(stats,metric("Đang rút",reserved,Ui.INK));ui.gap(stats,12);ui.weight(stats,metric("Lương chờ vào ví",pendingSalary,Ui.MUTED));ui.add(card,stats);return card;
        },ui.dp(12));
        rows.add("bank",state().bank,()->{
            LinearLayout card=ui.row();ui.pad(card,14,10);ui.surface(card,Ui.WHITE,16,Ui.BORDER);card.addView(ui.icon(R.drawable.ic_wallet,22,Ui.BLUE));ui.gap(card,12);
            LinearLayout copy=ui.column();ui.add(copy,ui.text("Ngân hàng nhận tiền",14,Ui.INK,true));
            String number=String.valueOf(state().bank.getOrDefault("account_number",""));String label=number.isEmpty()?"Thêm tài khoản ngân hàng":state().bank.getOrDefault("bank_name","")+" · •••• "+number.substring(Math.max(0,number.length()-4));
            ui.space(copy,5);ui.add(copy,ui.text(label,12,Ui.MUTED,false));ui.weight(card,copy);card.addView(ui.icon(R.drawable.ic_right,18,Ui.MUTED));
            ui.clickable(card,()->{state().bankOrigin="tutor_wallet";host().show("payout_bank");});return card;
        },ui.dp(20));
        rows.add("history-title",true,()->ui.text("Lịch sử ví",17,Ui.INK,true),ui.dp(10));
        rows.add("filters",state().walletFilter,()->{
            LinearLayout box=ui.column();ui.tabs(box,new String[]{"Tất cả","Nhận lương","Rút tiền"},state().walletFilter,index->{state().walletFilter=index;state().changed();});return box;
        },ui.dp(14));
        List<Entry> entries=new ArrayList<>();
        if(state().walletFilter!=2)for(PaymentState.Row payout:state().payouts.values())if(state().owner.equals(payout.text("tutor_id"))&&"CREDITED".equals(payout.text("status")))entries.add(new Entry(payout,true));
        if(state().walletFilter!=1)for(PaymentState.Row withdrawal:state().withdrawals.values())if(state().owner.equals(withdrawal.text("tutor_id")))entries.add(new Entry(withdrawal,false));
        entries.sort(Comparator.comparingLong((Entry entry)->entry.salary?entry.row.time("credited_at"):entry.row.time("created_at")).reversed());
        for(Entry entry:entries)rows.add((entry.salary?"salary/":"withdrawal/")+entry.row.id,entry.row,()->entry(entry),ui.dp(10));
        if(entries.isEmpty())rows.add("empty",state().walletFilter,()->ui.text("Chưa có giao dịch ví trong mục này.",13,Ui.MUTED,false),0);
        rows.commit();
    }
    private View metric(String label,long value,int color){LinearLayout box=ui.column();ui.add(box,ui.text(label,12,Ui.MUTED,false));ui.space(box,6);TextView number=ui.text(PaymentState.money(value),16,color,true);number.setMaxLines(1);number.setAutoSizeTextTypeUniformWithConfiguration(11,16,1,android.util.TypedValue.COMPLEX_UNIT_SP);ui.add(box,number);return box;}
    private View entry(Entry entry){
        PaymentState.Row row=entry.row;LinearLayout card=ui.card(),top=ui.row();
        ui.weight(top,ui.text(entry.salary?"Lương kỳ "+row.text("period"):"Rút về ngân hàng",14,Ui.INK,true));top.addView(ui.icon(R.drawable.ic_right,17,Ui.MUTED));ui.add(card,top);ui.space(card,8);
        String status=entry.salary?"Đã vào ví":TutorWallet.withdrawalStatus(row.text("status"));
        ui.add(card,ui.text(status,12,entry.salary||"PAID".equals(row.text("status"))?Ui.GREEN:Ui.MUTED,false));ui.space(card,10);
        boolean closed=!entry.salary&&Arrays.asList("REJECTED","CANCELLED").contains(row.text("status"));
        ui.add(card,ui.text((entry.salary?"+ ":closed?"":"− ")+PaymentState.money(row.number("amount")),19,entry.salary?Ui.GREEN:closed?Ui.MUTED:Ui.INK,true));ui.space(card,5);
        ui.add(card,ui.text(date(row.time(entry.salary?"credited_at":"created_at")),12,Ui.MUTED,false));
        ui.clickable(card,()->{
            if(entry.salary){state().selectedPayout=row.id;state().origin="tutor_wallet";host().show("payout_detail");}
            else state().openWithdrawal(row.id,"tutor_wallet");
        });return card;
    }
    private static final class Entry {final PaymentState.Row row;final boolean salary;Entry(PaymentState.Row row,boolean salary){this.row=row;this.salary=salary;}}
    @Override public void onDestroyView(){super.onDestroyView();body=null;}
}
