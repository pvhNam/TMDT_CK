package com.example.tmdt.ui.admin;

import android.view.View;
import android.widget.LinearLayout;
import com.example.tmdt.PaymentState;
import com.example.tmdt.RevenueBreakdown;
import com.example.tmdt.R;
import com.example.tmdt.ReportPeriod;
import com.example.tmdt.ui.common.StableViews;
import com.example.tmdt.ui.common.Ui;
import java.util.Arrays;

public final class AdminDashboardFragment extends AdminScreen {
    private LinearLayout body;
    private ReportPeriod shownPeriod;
    @Override protected View build(){
        LinearLayout root=root("Tổng quan");body=ui.page(root);render();return root;
    }
    @Override protected boolean updateContent(){if(body==null)return false;render();return true;}
    private void render(){
        StableViews rows=StableViews.at(body);rows.begin();if(!feedback(rows)){rows.commit();return;}
        rows.add("people",Arrays.asList(state().totalStudents,state().totalTutors),()->{
            LinearLayout row=ui.row();
            ui.weight(row,statistic("Học sinh",String.valueOf(state().totalStudents),()->openUsers("STUDENT")));ui.gap(row,10);
            ui.weight(row,statistic("Gia sư",String.valueOf(state().totalTutors),()->host().exchange.openTutorManage()));return row;
        },ui.dp(22));
        int ready=0,refunds=0;for(PaymentState.Row p:money().payouts.values())if("READY".equals(p.text("status")))ready++;
        for(PaymentState.Row r:money().refunds.values())if("PENDING".equals(r.text("status")))refunds++;
        if(ready>0||refunds>0){
            final int pendingPayouts=ready,pendingRefunds=refunds;
            rows.add("pending",Arrays.asList(ready,refunds),()->{
                LinearLayout box=ui.column(),card=menuGroup(box,"Cần xử lý");
                if(pendingPayouts>0)menuItem(card,R.drawable.ic_wallet,"Bảng lương chờ chi",String.valueOf(pendingPayouts),this::openPayroll);
                if(pendingRefunds>0)menuItem(card,R.drawable.ic_coin,"Yêu cầu hoàn học phí",String.valueOf(pendingRefunds),this::openRefunds);return box;
            },ui.dp(22));
        }
        rows.add("revenue-title",true,()->ui.text("Doanh thu",17,Ui.INK,true),ui.dp(10));
        ReportPeriod period=state().revenuePeriod;
        rows.add("period",period,()->AdminPeriodFilter.create(ui,period,value->{state().revenuePeriod=value;state().changed();}),ui.dp(12));
        RevenueBreakdown data=new RevenueBreakdown(money().payments.values(),period);
        boolean animate=!period.equals(shownPeriod)||(!state().revenueAnimated&&data.received>0);
        shownPeriod=period;if(data.received>0)state().revenueAnimated=true;
        rows.add("revenue",Arrays.asList(period,data.signature()),()->AdminRevenueCard.create(ui,data,true,animate),ui.dp(22));
        rows.add("shortcuts",true,()->{
            LinearLayout box=ui.column();ui.section(box,"Truy cập nhanh",17);ui.space(box,10);
            LinearLayout first=ui.row();ui.weight(first,shortcut(R.drawable.ic_pf_shield,"Duyệt gia sư",()->open("admin_tutors")));ui.gap(first,10);
            ui.weight(first,shortcut(R.drawable.ic_ex_book,"Lớp học và đăng ký",()->host().exchange.openClassesAdmin()));ui.add(box,first);ui.space(box,10);
            LinearLayout second=ui.row();ui.weight(second,shortcut(R.drawable.ic_wallet,"Thu nhập gia sư",()->{money().incomePeriod=money().payrollPeriod;open("admin_income");}));ui.gap(second,10);
            ui.weight(second,shortcut(R.drawable.ic_ex_chart,"Thống kê lớp học",()->host().exchange.openStats()));ui.add(box,second);return box;
        },0);
        if(!money().error.isEmpty())rows.add("payment-error",money().error,()->ui.text(money().error,14,Ui.RED,false),0);
        rows.commit();
    }
    private void openUsers(String filter){state().userFilter=filter;state().query="";open("admin_users");}
    @Override public void onDestroyView(){super.onDestroyView();body=null;shownPeriod=null;}
}
