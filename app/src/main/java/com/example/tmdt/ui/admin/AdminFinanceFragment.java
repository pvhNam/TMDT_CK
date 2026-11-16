package com.example.tmdt.ui.admin;

import android.view.View;
import android.widget.LinearLayout;
import com.example.tmdt.*;
import com.example.tmdt.ui.common.StableViews;
import com.example.tmdt.ui.common.Ui;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/** Tuition, monthly salary and wallets each have a short, focused tab. */
public final class AdminFinanceFragment extends AdminScreen {
    private LinearLayout body,tabs;
    @Override protected View build(){
        LinearLayout root=root("Tài chính");tabs=ui.column();ui.add(root,tabs);body=ui.page(root);render();return root;
    }
    @Override protected boolean updateContent(){if(body==null)return false;render();return true;}
    private void render(){
        int tab=Math.max(0,Math.min(2,state().financeTab));
        StableViews navigation=StableViews.at(tabs);navigation.begin();
        navigation.add("tabs",tab,()->{
            LinearLayout box=ui.column();ui.tabs(box,new String[]{"Học phí","Lương","Ví"},tab,index->{
                if(state().financeTab==index)return;state().financeTab=index;scrollToTop();refreshData();
            });return box;
        },ui.dp(12));navigation.commit();
        StableViews rows=StableViews.at(body);rows.begin();if(!feedback(rows)){rows.commit();return;}
        if(tab==0)tuition(rows);else if(tab==1)payroll(rows);else wallets(rows);
        if(!money().error.isEmpty())rows.add("payment-error",money().error,()->ui.note(R.drawable.ic_info,money().error,0xFFFFF0F0,Ui.RED,Ui.RED),0);
        rows.commit();
    }
    private void tuition(StableViews rows){
        int pending=0;for(PaymentState.Row refund:money().refunds.values())if("PENDING".equals(refund.text("status")))pending++;
        final int refunds=pending;
        rows.add("transactions",refunds,()->{
            LinearLayout box=ui.column(),card=menuGroup(box,"Quản lý học phí");
            menuItem(card,R.drawable.ic_coin,"Lịch sử giao dịch","",()->open("payments"));
            menuItem(card,R.drawable.ic_users,"Thu nhập từng gia sư","",()->open("admin_income"));
            menuItem(card,R.drawable.ic_wallet,"Hoàn học phí",refunds>0?refunds+" chờ":"",this::openRefunds);return box;
        },ui.dp(22));
        rows.add("revenue-heading",true,()->ui.text("Doanh thu học phí",17,Ui.INK,true),ui.dp(12));
        ReportPeriod period=state().revenuePeriod;
        rows.add("period",period,()->AdminPeriodFilter.create(ui,period,value->{state().revenuePeriod=value;state().changed();}),ui.dp(12));
        RevenueBreakdown receipts=new RevenueBreakdown(money().payments.values(),period);
        rows.add("revenue",Arrays.asList(period,receipts.signature()),()->AdminRevenueCard.create(ui,receipts,false,false),0);
    }
    private void payroll(StableViews rows){
        incomeFeedback(rows);
        rows.add("payroll-heading",true,()->ui.text("Kỳ lương",17,Ui.INK,true),ui.dp(12));
        rows.add("payroll-period",money().payrollPeriod,()->AdminMonthPicker.create(ui,money().payrollPeriod,value->{money().payrollPeriod=value;money().changed();}),ui.dp(12));
        rows.add("current-month",money().payrollPeriod,()->ui.link("Xem tháng này",13,()->{money().payrollPeriod=ReportPeriod.currentMonth().toString();money().changed();}),ui.dp(12));
        rows.add("income-heading",true,()->{
            LinearLayout row=ui.row();ui.weight(row,ui.text("Lương từng gia sư",17,Ui.INK,true));
            row.addView(ui.link("Xem tất cả",13,()->{money().incomePeriod=money().payrollPeriod;open("admin_income");}));return row;
        },ui.dp(12));
        List<TutorIncomeReport> reports=TutorIncomeReport.month(money().payrollPeriod,money().earnings,money().payouts,state().tutorDirectory());
        for(int i=0;i<Math.min(5,reports.size());i++){
            TutorIncomeReport report=reports.get(i);
            rows.add("teacher/"+report.tutorId,Arrays.asList(report,person(report.tutorId),money().incomeLoaded,money().incomeError()),()->incomeCard(report),ui.dp(12));
        }
        if(reports.isEmpty()&&money().incomeError().isEmpty())rows.add("empty-teachers",Arrays.asList(money().incomeLoaded,state().loadingTutors,state().loadingTutorProfiles),()->
            ui.text(!money().incomeLoaded||state().loadingTutors||state().loadingTutorProfiles?"Đang tải danh sách gia sư…":"Chưa có gia sư trong danh sách.",14,Ui.MUTED,false),ui.dp(14));
        MonthlyPayroll payroll=new MonthlyPayroll(money().payrollPeriod,money().earnings,money().payouts);
        rows.add("payroll",Arrays.asList(payroll.period,payroll.total,payroll.waitingAmount,payroll.paidAmount,payroll.unpreparedAmount,payroll.tutors,money().incomeLoaded,money().incomeError()),()->{
            LinearLayout card=ui.card(),top=ui.row();ui.weight(top,ui.text("Tổng lương trong tháng",14,Ui.MUTED,false));top.addView(ui.pill(payroll.tutors+" gia sư",11,Ui.BLUE,Ui.PALE));ui.add(card,top);ui.space(card,10);
            android.widget.TextView total=ui.text(money().incomeLoaded?PaymentState.money(payroll.total):"Chưa tải xong dữ liệu",25,Ui.INK,true);total.setMaxLines(1);
            total.setAutoSizeTextTypeUniformWithConfiguration(16,25,1,android.util.TypedValue.COMPLEX_UNIT_SP);ui.add(card,total);ui.space(card,8);
            ui.add(card,ui.text("Ngày chi trả: "+Instant.ofEpochMilli(payroll.dueAt).atZone(ReportPeriod.ZONE).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),12,Ui.MUTED,false));ui.space(card,14);ui.line(card);ui.space(card,12);
            LinearLayout stats=ui.row();ui.weight(stats,metric("Chờ chi",payroll.waitingAmount,Ui.ORANGE));ui.gap(stats,10);
            ui.weight(stats,metric("Đã chi",payroll.paidAmount,Ui.GREEN));ui.add(card,stats);
            if(payroll.unpreparedAmount>0){ui.space(card,12);ui.add(card,ui.text("Chưa lập bảng: "+PaymentState.money(payroll.unpreparedAmount),13,Ui.MUTED,false));}
            ui.space(card,16);ui.addAction(card,ui.action("Xem bảng lương",0,Ui.PRIMARY,()->open("admin_payroll")),48);return card;
        },ui.dp(22));
        rows.add("income",true,()->{
            LinearLayout box=ui.column(),card=menuGroup(box,"Theo từng gia sư");
            menuItem(card,R.drawable.ic_users,"Thu nhập từng gia sư","",()->{money().incomePeriod=money().payrollPeriod;open("admin_income");});return box;
        },0);
    }
    private void wallets(StableViews rows){
        long available=0,withdrawn=0;
        for(PaymentState.Row wallet:money().wallets.values()){available+=TutorWallet.available(wallet);withdrawn+=wallet.number("total_withdrawn");}
        final long walletAvailable=available,walletWithdrawn=withdrawn;
        rows.add("wallet-heading",true,()->ui.text("Ví gia sư",17,Ui.INK,true),ui.dp(12));
        rows.add("wallets",Arrays.asList(available,withdrawn,money().walletLoaded),()->{
            LinearLayout card=ui.card(),stats=ui.row();ui.weight(stats,metric("Số dư khả dụng",walletAvailable,Ui.INK));ui.gap(stats,10);
            ui.weight(stats,metric("Tổng đã rút",walletWithdrawn,Ui.GREEN));ui.add(card,stats);return card;
        },ui.dp(22));
        rows.add("withdrawals",true,()->{
            LinearLayout box=ui.column(),card=menuGroup(box,"Giao dịch ví");
            menuItem(card,R.drawable.ic_wallet,"Lịch sử rút tiền","",()->{money().withdrawalFilter=0;open("admin_withdrawals");});return box;
        },0);
    }
    @Override public void onDestroyView(){super.onDestroyView();body=null;tabs=null;}
}
