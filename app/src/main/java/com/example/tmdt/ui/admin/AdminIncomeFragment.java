package com.example.tmdt.ui.admin;

import android.view.View;
import android.widget.LinearLayout;
import com.example.tmdt.CatalogState;
import com.example.tmdt.PaymentState;
import com.example.tmdt.R;
import com.example.tmdt.TutorIncomeReport;
import com.example.tmdt.ui.common.StableViews;
import com.example.tmdt.ui.common.Ui;
import java.util.Arrays;
import java.util.List;

public final class AdminIncomeFragment extends AdminScreen {
    private LinearLayout body;
    @Override protected View build(){LinearLayout root=root("Thu nhập gia sư");body=ui.page(root);render();return root;}
    @Override protected boolean updateContent(){if(body==null)return false;render();return true;}
    private void render(){
        StableViews rows=StableViews.at(body);rows.begin();if(!feedback(rows)){rows.commit();return;}
        rows.add("period",money().incomePeriod,()->AdminMonthPicker.create(ui,money().incomePeriod,value->{money().incomePeriod=value;money().changed();}),ui.dp(14));
        List<TutorIncomeReport> reports=TutorIncomeReport.month(money().incomePeriod,money().earnings,money().payouts);
        long total=0,paid=0,waiting=0;for(TutorIncomeReport report:reports){total+=report.total;paid+=report.paid;waiting+=report.waiting;}
        final long earned=total,received=paid,remaining=waiting;
        rows.add("summary",Arrays.asList(total,paid,waiting,reports.size(),money().incomeLoaded),()->{
            LinearLayout card=ui.card();ui.cardSurface(card,Ui.PALE,Ui.BORDER);ui.add(card,ui.text("Tổng thu nhập trong tháng",13,Ui.MUTED,false));ui.space(card,10);
            ui.add(card,ui.text(money().incomeLoaded?PaymentState.money(earned):"Đang tải thu nhập…",25,Ui.INK,true));ui.space(card,8);
            ui.add(card,ui.text(reports.size()+" gia sư",12,Ui.MUTED,false));ui.space(card,14);ui.line(card);ui.space(card,14);
            LinearLayout stats=ui.row();ui.weight(stats,metric("Đã trả",received,Ui.GREEN));ui.gap(stats,12);ui.weight(stats,metric("Chưa trả",remaining,Ui.ORANGE));ui.add(card,stats);return card;
        },ui.dp(16));
        rows.add("search",true,()->{LinearLayout box=ui.column();search(box,"Tìm tên gia sư",state().incomeQuery,value->{state().incomeQuery=value;refreshData();});return box;},0);
        String query=CatalogState.normalize(state().incomeQuery);int shown=0;
        for(TutorIncomeReport report:reports){String tutorName=name(report.tutorId,report.name);if(!CatalogState.normalize(tutorName).contains(query))continue;shown++;
            PaymentState.Row person=person(report.tutorId);
            rows.add("tutor/"+report.tutorId,Arrays.asList(report,tutorName,person),()->teacher(report,tutorName,person),ui.dp(12));
        }
        if(shown==0)rows.add("empty",Arrays.asList(query,money().incomeLoaded),()->ui.text(!money().incomeLoaded?"Đang tải dữ liệu…":query.isEmpty()?"Chưa có thu nhập trong tháng này.":"Không có gia sư khớp với tên đã nhập.",14,Ui.MUTED,false),ui.dp(16));
        rows.add("payroll",true,()->{LinearLayout box=ui.column();action(box,"Xem bảng lương tháng",Ui.OUTLINE,()->{money().payrollPeriod=money().incomePeriod;open("admin_payroll");});return box;},0);
        if(!money().error.isEmpty())rows.add("error",money().error,()->ui.text(money().error,13,Ui.RED,false),0);rows.commit();
    }
    private View teacher(TutorIncomeReport report,String name,PaymentState.Row person){
        LinearLayout card=ui.card(),top=ui.row(),copy=ui.column();top.addView(ui.avatar(report.tutorId,name,44));ui.gap(top,12);
        ui.add(copy,ui.text(name,16,Ui.INK,true));ui.space(copy,5);ui.add(copy,ui.text(report.sessions+" buổi học",12,Ui.MUTED,false));ui.weight(top,copy);top.addView(ui.icon(R.drawable.ic_right,18,Ui.BLUE));ui.add(card,top);ui.space(card,14);
        ui.add(card,ui.text(PaymentState.money(report.total),22,Ui.INK,true));ui.space(card,14);ui.line(card);ui.space(card,12);
        LinearLayout stats=ui.row();ui.weight(stats,metric("Đã trả",report.paid,Ui.GREEN));ui.gap(stats,12);ui.weight(stats,metric("Chưa trả",report.waiting,Ui.ORANGE));ui.add(card,stats);
        if(person!=null&&"BANNED".equals(person.text("trang_thai"))){ui.space(card,10);ui.add(card,ui.pill("Tạm dừng chi trả",11,Ui.RED,0xFFFFE9EC));}
        card.setContentDescription("Xem thu nhập của "+name);ui.clickable(card,()->income(report.tutorId,name));return card;
    }
    @Override public void onDestroyView(){super.onDestroyView();body=null;}
}
