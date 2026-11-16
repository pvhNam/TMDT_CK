package com.example.tmdt.ui.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import com.example.tmdt.PaymentState;
import com.example.tmdt.R;
import com.example.tmdt.ReportPeriod;
import com.example.tmdt.TutorIncomeReport;
import com.example.tmdt.TutorWallet;
import com.example.tmdt.ui.common.StableViews;
import com.example.tmdt.ui.common.RenderSnapshot;
import com.example.tmdt.ui.common.Ui;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public final class AdminTutorIncomeFragment extends AdminScreen {
    private LinearLayout body;
    private int section,visibleEntries=30;
    @Override public void onCreate(Bundle saved){super.onCreate(saved);if(saved!=null){section=saved.getInt("income_section",0);visibleEntries=saved.getInt("visible_entries",30);}}
    @Override public void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putInt("income_section",section);out.putInt("visible_entries",visibleEntries);}
    @Override protected View build(){LinearLayout root=root("Thu nhập từng gia sư");body=ui.page(root);render();return root;}
    @Override protected boolean updateContent(){if(body==null)return false;render();return true;}
    @Override protected Object dataSnapshot(){return RenderSnapshot.of(super.dataSnapshot(),section,visibleEntries);}
    private void render(){
        StableViews rows=StableViews.at(body);rows.begin();if(!feedback(rows)){rows.commit();return;}
        String uid=state().selectedIncomeTutor;if(uid.isEmpty()){rows.add("empty",true,()->ui.link("Chọn gia sư để xem thu nhập",14,()->open("admin_income")),0);rows.commit();return;}
        TutorIncomeReport report=new TutorIncomeReport(uid,money().incomePeriod,money().earnings,money().payouts);
        String tutorName=name(uid,report.name.isEmpty()?state().incomeTutorName:report.name);PaymentState.Row person=person(uid);
        rows.add("identity",Arrays.asList(uid,tutorName,person),()->{
            LinearLayout card=ui.card(),top=ui.row(),copy=ui.column();top.addView(ui.avatar(uid,tutorName,52));ui.gap(top,12);
            ui.add(copy,ui.text(tutorName,19,Ui.INK,true));ui.space(copy,6);ui.add(copy,ui.text(person!=null&&"BANNED".equals(person.text("trang_thai"))?"Đã khóa · Tạm dừng chi trả":"Gia sư",12,Ui.MUTED,false));ui.weight(top,copy);
            if(person!=null){top.addView(ui.icon(R.drawable.ic_right,18,Ui.BLUE));ui.clickable(card,()->{if("admin_user".equals(state().incomeOrigin))host().back();else user(uid);});}ui.add(card,top);return card;
        },ui.dp(14));
        rows.add("period",money().incomePeriod,()->AdminMonthPicker.create(ui,money().incomePeriod,value->{visibleEntries=30;money().incomePeriod=value;money().changed();}),ui.dp(14));
        if(!money().incomeLoaded&&report.earnings.isEmpty()&&report.payouts.isEmpty())rows.add("loading",true,()->ui.text("Đang tải thu nhập…",14,Ui.MUTED,false),ui.dp(12));
        else{
            rows.add("summary",Arrays.asList(report.total,report.paid,report.waiting,report.sessions),()->{
                LinearLayout card=ui.card();ui.cardSurface(card,Ui.PALE,Ui.BORDER);ui.add(card,ui.text("Thu nhập trong tháng",13,Ui.MUTED,false));ui.space(card,10);
                ui.add(card,ui.text(PaymentState.money(report.total),26,Ui.INK,true));ui.space(card,6);ui.add(card,ui.text(report.sessions+" buổi học",12,Ui.MUTED,false));ui.space(card,14);ui.line(card);ui.space(card,14);
                LinearLayout stats=ui.row();ui.weight(stats,metric("Đã trả",report.paid,Ui.GREEN));ui.gap(stats,12);ui.weight(stats,metric("Chưa trả",report.waiting,Ui.ORANGE));ui.add(card,stats);return card;
            },ui.dp(14));
            PaymentState.Row wallet=money().wallets.get(uid);
            if(wallet!=null)rows.add("wallet",wallet,()->{
                LinearLayout card=ui.card();ui.add(card,ui.text("Ví hiện tại",15,Ui.INK,true));ui.space(card,12);LinearLayout stats=ui.row();
                ui.weight(stats,metric("Số dư khả dụng",TutorWallet.available(wallet),Ui.INK));ui.gap(stats,12);ui.weight(stats,metric("Tổng đã rút",wallet.number("total_withdrawn"),Ui.GREEN));ui.add(card,stats);return card;
            },ui.dp(16));
            rows.add("sections",section,()->{LinearLayout box=ui.column();ui.tabs(box,new String[]{"Buổi dạy","Bảng lương"},section,index->{section=index;visibleEntries=30;refreshData();});return box;},ui.dp(14));
            if(section==0){
                int shown=0;for(PaymentState.Row earning:report.earnings){if(shown++>=visibleEntries)break;PaymentState.Row salary=money().payouts.get(earning.text("payout_id")),payment=money().payments.get(earning.text("payment_id"));
                    rows.add("earning/"+earning.id,Arrays.asList(earning,salary,payment),()->earning(earning,salary,payment),ui.dp(12));
                }
                if(report.earnings.isEmpty())rows.add("empty-earnings",true,()->ui.text("Chưa có buổi dạy được ghi nhận trong tháng.",13,Ui.MUTED,false),ui.dp(14));
            }else{
                int shown=0;for(PaymentState.Row payout:report.payouts){if(shown++>=visibleEntries)break;rows.add("payout/"+payout.id,payout,()->payout(payout),ui.dp(12));}
                if(report.payouts.isEmpty())rows.add("empty-payouts",true,()->ui.text("Chưa có bảng lương trong tháng.",13,Ui.MUTED,false),ui.dp(14));
            }
            if((section==0?report.earnings.size():report.payouts.size())>visibleEntries)rows.add("more",section,()->ui.link("Xem thêm",14,()->{visibleEntries+=30;refreshData();}),ui.dp(16));
            rows.add("payroll",true,()->{LinearLayout box=ui.column();action(box,"Xem bảng lương tháng",Ui.OUTLINE,()->{money().payrollPeriod=money().incomePeriod;open("admin_payroll");});return box;},0);
        }
        if(!money().error.isEmpty())rows.add("error",money().error,()->ui.text(money().error,13,Ui.RED,false),0);rows.commit();
    }
    private View earning(PaymentState.Row row,PaymentState.Row salary,PaymentState.Row payment){
        LinearLayout card=ui.card(),top=ui.row();String title=row.text("title").isEmpty()?"Buổi học":row.text("title");
        ui.weight(top,ui.text(title,15,Ui.INK,true));if(payment!=null)top.addView(ui.icon(R.drawable.ic_right,17,Ui.BLUE));ui.add(card,top);ui.space(card,7);
        ui.add(card,ui.text((row.number("session_number")>0?"Buổi "+row.number("session_number")+" · ":"")+date(row.time("created_at")),12,Ui.MUTED,false));ui.space(card,12);
        LinearLayout bottom=ui.row();ui.weight(bottom,ui.text(PaymentState.money(row.number("amount")),20,Ui.INK,true));
        String status=salary==null?(row.text("payout_id").isEmpty()?"Chưa lập bảng":"Đang đối chiếu"):"CANCELLED".equals(salary.text("status"))?"Chưa lập bảng":PaymentState.payoutStatus(salary.text("status"));
        bottom.addView(ui.pill(status,11,salary!=null&&TutorWallet.salaryPaid(salary.text("status"))?Ui.GREEN:Ui.MUTED,Ui.PALE));ui.add(card,bottom);
        if(payment!=null)ui.clickable(card,()->money().openExisting(payment.id,"admin_tutor_income"));return card;
    }
    private View payout(PaymentState.Row row){
        LinearLayout card=ui.card(),top=ui.row();ui.weight(top,ui.text("Lương "+row.text("period"),15,Ui.INK,true));top.addView(ui.icon(R.drawable.ic_right,17,Ui.BLUE));ui.add(card,top);ui.space(card,8);
        ui.add(card,ui.text(PaymentState.payoutStatus(row.text("status"))+" · "+row.number("session_count")+" buổi",12,TutorWallet.salaryPaid(row.text("status"))?Ui.GREEN:Ui.MUTED,false));ui.space(card,12);
        ui.add(card,ui.text(PaymentState.money(row.number("amount")),21,Ui.INK,true));ui.clickable(card,()->{money().selectedPayout=row.id;money().origin="admin_tutor_income";open("payout_detail");});return card;
    }
    private String date(long value){return value<=0?"Đang cập nhật":Instant.ofEpochMilli(value).atZone(ReportPeriod.ZONE).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));}
    @Override public void onDestroyView(){super.onDestroyView();body=null;}
}
