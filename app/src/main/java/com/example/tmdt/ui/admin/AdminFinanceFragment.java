package com.example.tmdt.ui.admin;

import android.view.View;
import android.widget.LinearLayout;
import com.example.tmdt.*;
import com.example.tmdt.ui.common.StableViews;
import com.example.tmdt.ui.common.Ui;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/** One predictable entry point for collections, earnings and monthly payouts. */
public final class AdminFinanceFragment extends AdminScreen {
    private LinearLayout body;
    @Override protected View build(){
        LinearLayout root=root("Tài chính");body=ui.page(root);render();return root;
    }
    @Override protected boolean updateContent(){if(body==null)return false;render();return true;}
    private void render(){
        StableViews rows=StableViews.at(body);rows.begin();if(!feedback(rows)){rows.commit();return;}
        rows.add("shortcuts",true,()->{
            LinearLayout row=ui.row();ui.weight(row,shortcut(R.drawable.ic_users,"Thu nhập gia sư",()->{money().incomePeriod=money().payrollPeriod;open("admin_income");}));ui.gap(row,10);
            ui.weight(row,shortcut(R.drawable.ic_wallet,"Bảng lương",()->open("admin_payroll")));return row;
        },ui.dp(18));
        rows.add("revenue-heading",true,()->ui.text("Doanh thu học phí",19,Ui.INK,true),ui.dp(12));
        ReportPeriod period=state().revenuePeriod;
        rows.add("period",period,()->AdminPeriodFilter.create(ui,period,value->{state().revenuePeriod=value;state().changed();}),ui.dp(12));
        RevenueBreakdown receipts=new RevenueBreakdown(money().payments.values(),period);
        rows.add("revenue",Arrays.asList(period,receipts.signature()),()->AdminRevenueCard.create(ui,receipts,false,false),ui.dp(20));
        rows.add("payroll-heading",true,()->ui.text("Lương gia sư",18,Ui.INK,true),ui.dp(12));
        rows.add("payroll-period",money().payrollPeriod,()->AdminMonthPicker.create(ui,money().payrollPeriod,value->{money().payrollPeriod=value;money().changed();}),ui.dp(12));
        MonthlyPayroll payroll=new MonthlyPayroll(money().payrollPeriod,money().earnings,money().payouts);
        rows.add("payroll",Arrays.asList(payroll.period,payroll.total,payroll.waitingAmount,payroll.paidAmount,payroll.unpreparedAmount,payroll.tutors),()->{
            LinearLayout card=ui.card(),top=ui.row();ui.weight(top,ui.text("Quỹ lương trong tháng",14,Ui.MUTED,false));top.addView(ui.pill(payroll.tutors+" gia sư",11,Ui.BLUE,Ui.PALE));ui.add(card,top);ui.space(card,10);
            ui.add(card,ui.text(PaymentState.money(payroll.total),25,Ui.INK,true));ui.space(card,8);
            ui.add(card,ui.text("Ngày chi trả: "+Instant.ofEpochMilli(payroll.dueAt).atZone(ReportPeriod.ZONE).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),12,Ui.MUTED,false));ui.space(card,14);ui.line(card);ui.space(card,12);
            LinearLayout stats=ui.row();ui.weight(stats,metric("Chờ chi",payroll.waitingAmount,Ui.ORANGE));ui.gap(stats,10);
            ui.weight(stats,metric("Đã chi",payroll.paidAmount,Ui.GREEN));ui.add(card,stats);
            if(payroll.unpreparedAmount>0){ui.space(card,12);ui.add(card,ui.text("Chưa lập bảng: "+PaymentState.money(payroll.unpreparedAmount),13,Ui.MUTED,false));}
            ui.space(card,16);ui.addAction(card,ui.action("Xem bảng lương",0,Ui.PRIMARY,()->open("admin_payroll")),48);return card;
        },ui.dp(20));
        long available=0,withdrawn=0;
        for(PaymentState.Row wallet:money().wallets.values()){available+=TutorWallet.available(wallet);withdrawn+=wallet.number("total_withdrawn");}
        final long walletAvailable=available,walletWithdrawn=withdrawn;
        rows.add("wallet-heading",true,()->ui.text("Ví gia sư",18,Ui.INK,true),ui.dp(12));
        rows.add("wallets",Arrays.asList(available,withdrawn,money().walletLoaded),()->{
            LinearLayout card=ui.card(),stats=ui.row();ui.weight(stats,metric("Số dư khả dụng",walletAvailable,Ui.INK));ui.gap(stats,10);ui.weight(stats,metric("Đã rút",walletWithdrawn,Ui.GREEN));ui.add(card,stats);
            ui.space(card,14);ui.addAction(card,ui.action("Xem lịch sử rút tiền",0,Ui.OUTLINE,()->{money().withdrawalFilter=0;open("admin_withdrawals");}),48);return card;
        },ui.dp(20));
        int refunds=0;
        for(PaymentState.Row r:money().refunds.values())if("PENDING".equals(r.text("status")))refunds++;
        final int pendingRefunds=refunds;
        rows.add("management",refunds,()->{
            LinearLayout box=ui.column();ui.section(box,"Giao dịch",17);ui.space(box,10);
            destination(box,R.drawable.ic_coin,"Lịch sử học phí","",()->open("payments"));
            destination(box,R.drawable.ic_wallet,"Hoàn học phí",pendingRefunds>0?pendingRefunds+" chờ":"",this::openRefunds);
            destination(box,R.drawable.ic_users,"Thu nhập từng gia sư","",()->{money().incomePeriod=money().payrollPeriod;open("admin_income");});return box;
        },0);
        if(!money().error.isEmpty())rows.add("payment-error",money().error,()->ui.note(R.drawable.ic_info,money().error,0xFFFFF0F0,Ui.RED,Ui.RED),0);
        rows.commit();
    }
    @Override public void onDestroyView(){super.onDestroyView();body=null;}
}
