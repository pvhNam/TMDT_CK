package com.example.tmdt.ui.admin;

import android.view.View;
import android.widget.LinearLayout;
import com.example.tmdt.*;
import com.example.tmdt.ui.common.Ui;

/** One predictable entry point for collections, earnings and monthly payouts. */
public final class AdminFinanceFragment extends AdminScreen {
    @Override protected View build(){
        LinearLayout root=root("Tài chính"),body=ui.page(root);if(!feedback(body))return root;
        ui.add(body,ui.text("Học phí và chi trả",23,Ui.INK,true));ui.space(body,6);
        ui.space(body,16);
        RevenueBreakdown receipts=new RevenueBreakdown(money().payments.values());long paid=0,ready=0;int waiting=0,refunds=0;
        for(PaymentState.Row p:money().payouts.values()){
            if("PAID".equals(p.text("status")))paid+=p.number("amount");
            if("READY".equals(p.text("status"))){ready+=p.number("amount");waiting++;}
        }
        for(PaymentState.Row r:money().refunds.values())if("PENDING".equals(r.text("status")))refunds++;
        card(body,"Học phí đã nhận",PaymentState.money(receipts.received),"Toàn thời gian · Đã hoàn: "+PaymentState.money(receipts.refunded),()->open("payments"));
        LinearLayout stats=ui.row();ui.weight(stats,statistic("Đã trả gia sư",PaymentState.money(paid),null));ui.gap(stats,10);
        ui.weight(stats,statistic("Bảng lương chờ chi",PaymentState.money(ready),null));ui.add(body,stats);ui.space(body,20);
        ui.section(body,"Quản lý",17);ui.space(body,10);
        destination(body,R.drawable.ic_coin,"Học phí đã nhận","",()->open("payments"));
        destination(body,R.drawable.ic_wallet,"Hoàn học phí",refunds>0?refunds+" chờ":"",this::openRefunds);
        destination(body,R.drawable.ic_users,"Thu nhập gia sư","",()->open("admin_income"));
        destination(body,R.drawable.ic_wallet,"Bảng lương & chi trả",waiting>0?waiting+" chờ":"",this::openPayroll);
        if(!money().error.isEmpty()){ui.space(body,12);ui.add(body,ui.note(R.drawable.ic_info,money().error,0xFFFFF0F0,Ui.RED,Ui.RED));}return root;
    }
}
