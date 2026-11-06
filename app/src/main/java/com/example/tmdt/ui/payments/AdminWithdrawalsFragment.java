package com.example.tmdt.ui.payments;

import android.view.View;
import android.widget.LinearLayout;
import com.example.tmdt.PaymentState;
import com.example.tmdt.R;
import com.example.tmdt.TutorWallet;
import com.example.tmdt.ui.common.StableViews;
import com.example.tmdt.ui.common.Ui;
import java.util.Arrays;

public final class AdminWithdrawalsFragment extends PaymentScreen {
    private LinearLayout body;
    @Override protected View build(){LinearLayout root=root("Lịch sử rút tiền");body=ui.page(root);render();return root;}
    @Override protected boolean updateContent(){if(body==null)return false;render();return true;}
    private void render(){
        StableViews rows=StableViews.at(body);rows.begin();if(!state().admin){rows.commit();return;}
        if(!state().error.isEmpty()||!state().notice.isEmpty()||state().busy)rows.add("feedback",Arrays.asList(state().error,state().notice,state().busy),()->{LinearLayout box=ui.column();feedback(box);return box;},0);
        long total=0;int count=0;for(PaymentState.Row row:state().withdrawals.values())if("PAID".equals(row.text("status"))){total+=row.number("amount");count++;}
        final long withdrawn=total;final int completed=count;
        rows.add("summary",Arrays.asList(total,count),()->{
            LinearLayout card=ui.card();ui.cardSurface(card,Ui.PALE,Ui.BORDER);ui.add(card,ui.text("Tổng đã rút",13,Ui.MUTED,false));ui.space(card,10);
            ui.add(card,ui.text(PaymentState.money(withdrawn),26,Ui.INK,true));ui.space(card,8);ui.add(card,ui.text(completed+" giao dịch hoàn tất",13,Ui.MUTED,false));return card;
        },ui.dp(16));
        rows.add("filters",state().withdrawalFilter,()->{
            LinearLayout box=ui.column();ui.tabs(box,new String[]{"Tất cả","Đã rút","Đã đóng"},state().withdrawalFilter,index->{state().withdrawalFilter=index;state().changed();});return box;
        },ui.dp(14));
        int shown=0;
        for(PaymentState.Row row:newest(state().withdrawals.values())){
            String status=row.text("status");boolean show=state().withdrawalFilter==0|| (state().withdrawalFilter==1?"PAID".equals(status):Arrays.asList("REJECTED","CANCELLED").contains(status));
            if(!show)continue;shown++;
            rows.add("withdrawal/"+row.id,row,()->{
                LinearLayout card=ui.card(),top=ui.row(),copy=ui.column();top.addView(ui.avatar(row.text("tutor_id"),row.text("tutor_name"),44));ui.gap(top,12);
                ui.add(copy,ui.text(row.text("tutor_name"),16,Ui.INK,true));ui.space(copy,5);ui.add(copy,ui.text(date(row.time("created_at")),12,Ui.MUTED,false));ui.weight(top,copy);top.addView(ui.icon(R.drawable.ic_right,18,Ui.MUTED));ui.add(card,top);ui.space(card,14);
                ui.add(card,ui.text(PaymentState.money(row.number("amount")),21,Ui.INK,true));ui.space(card,8);
                ui.add(card,ui.text(TutorWallet.withdrawalStatus(status),12,"PAID".equals(status)?Ui.GREEN:Ui.MUTED,false));
                ui.clickable(card,()->state().openWithdrawal(row.id,"admin_withdrawals"));return card;
            },ui.dp(12));
        }
        if(shown==0)rows.add("empty",state().withdrawalFilter,()->ui.text("Chưa có giao dịch trong mục này.",13,Ui.MUTED,false),0);
        rows.commit();
    }
    @Override public void onDestroyView(){super.onDestroyView();body=null;}
}
