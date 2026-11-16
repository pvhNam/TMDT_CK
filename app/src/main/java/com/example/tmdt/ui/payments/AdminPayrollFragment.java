package com.example.tmdt.ui.payments;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.content.res.ColorStateList;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.tmdt.MonthlyPayroll;
import com.example.tmdt.PaymentState;
import com.example.tmdt.R;
import com.example.tmdt.ReportPeriod;
import com.example.tmdt.TutorWallet;
import com.example.tmdt.ui.admin.AdminMonthPicker;
import com.example.tmdt.ui.common.RenderSnapshot;
import com.example.tmdt.ui.common.StableViews;
import com.example.tmdt.ui.common.ThemeColors;
import com.example.tmdt.ui.common.Ui;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public final class AdminPayrollFragment extends PaymentScreen {
    private LinearLayout body;
    @Override public void onViewCreated(@NonNull View view,@Nullable Bundle saved){
        super.onViewCreated(view,saved);
        host().administration.changes.observe(getViewLifecycleOwner(),ignored->refreshData());
    }
    @Override public void onResume(){super.onResume();refreshData();}
    @Override protected Object dataSnapshot(){return RenderSnapshot.of(super.dataSnapshot(),host().administration.users,host().administration.tutors,LocalDate.now(ReportPeriod.ZONE));}
    @Override protected View build(){
        LinearLayout root=root("Bảng lương gia sư");body=ui.page(root);render();return root;
    }
    @Override protected boolean updateContent(){if(body==null)return false;render();return true;}
    private void render(){
        StableViews rows=StableViews.at(body);rows.begin();
        if(!state().admin){rows.add("permission",true,()->ui.text("Chỉ quản trị viên được xem bảng lương.",15,Ui.INK,true),0);rows.commit();return;}
        if(!state().error.isEmpty()||!state().notice.isEmpty()||state().busy)
            rows.add("feedback",Arrays.asList(state().error,state().notice,state().busy),()->{LinearLayout box=ui.column();feedback(box);return box;},0);
        rows.add("period",state().payrollPeriod,()->AdminMonthPicker.create(ui,state().payrollPeriod,value->{
            if(state().busy)return;state().clearFeedback();state().payrollPeriod=value;state().payrollFilter=0;state().changed();
        }),ui.dp(12));
        MonthlyPayroll payroll=new MonthlyPayroll(state().payrollPeriod,state().earnings,state().payouts);
        rows.add("summary",Arrays.asList(payroll.period,payroll.total,payroll.sessions,payroll.tutors,payroll.paidAmount,payroll.waitingAmount,payroll.unpreparedAmount),()->summaryCard(payroll),ui.dp(14));
        if(!payroll.unprepared.isEmpty())rows.add("prepare",Arrays.asList(payroll.period,payroll.unpreparedAmount,payroll.unprepared.size(),payroll.ended(),state().busy),()->{
            LinearLayout card=ui.card(),heading=ui.row();ui.weight(heading,ui.text("Chưa lập bảng lương",14,Ui.INK,true));
            heading.addView(ui.pill(payroll.unprepared.size()+" gia sư",11,Ui.BLUE,Ui.PALE));ui.add(card,heading);ui.space(card,8);
            ui.add(card,ui.text(PaymentState.money(payroll.unpreparedAmount),20,Ui.INK,true));ui.space(card,12);
            boolean ready=payroll.ended()&&!state().busy;
            ui.addAction(card,ui.action(state().busy?"Đang lập bảng…":payroll.ended()?"Lập bảng lương":"Chờ kết thúc tháng",0,ready?Ui.PRIMARY:Ui.DISABLED,ready?()->prepare(payroll):null),48);return card;
        },ui.dp(16));
        rows.add("filters",Arrays.asList(state().payrollFilter,payroll.unprepared.size(),payroll.ready.size(),payroll.paid.size(),payroll.cancelled.size()),()->filters(payroll),ui.dp(14));
        int selected=state().payrollFilter;boolean all=selected==0;int shown=0;
        if((all||selected==1)&&!payroll.unprepared.isEmpty()){
            heading(rows,"unprepared","Chưa lập bảng",payroll.unprepared.size());
            for(MonthlyPayroll.Unprepared earning:payroll.unprepared.values()){
                String name=tutorName(earning.tutorId,earning.tutorName);boolean blocked=banned(earning.tutorId);
                rows.add("unprepared/"+earning.tutorId,Arrays.asList(earning,name,blocked),()->unpreparedCard(earning,name,blocked),ui.dp(12));shown++;
            }
        }
        if(all||selected==2)shown+=payouts(rows,"ready","Chờ chi trả",payroll.ready);
        if(all||selected==3)shown+=payouts(rows,"paid","Đã chi trả",payroll.paid);
        if(all)shown+=payouts(rows,"cancelled","Đã hủy",payroll.cancelled);
        if(shown==0){String empty=selected==1?"Không có khoản lương chưa lập bảng.":selected==2?"Không có bảng lương chờ chi.":selected==3?"Chưa có khoản lương đã chi.":"Chưa có lương trong tháng này.";
            rows.add("empty",empty,()->ui.text(empty,13,Ui.MUTED,false),0);}
        rows.commit();
    }
    private View summaryCard(MonthlyPayroll payroll){
        LinearLayout card=ui.card();ui.cardSurface(card,Ui.PALE,Ui.BORDER);
        ui.add(card,ui.text("Tổng lương trong tháng",13,Ui.MUTED,false));ui.space(card,10);
        TextView total=ui.text(PaymentState.money(payroll.total),26,Ui.INK,true);total.setMaxLines(1);total.setAutoSizeTextTypeUniformWithConfiguration(18,26,1,android.util.TypedValue.COMPLEX_UNIT_SP);ui.add(card,total);ui.space(card,8);
        ui.add(card,ui.text(payroll.tutors+" gia sư · "+payroll.sessions+" buổi học",12,Ui.MUTED,false));ui.space(card,14);
        ui.add(card,ui.fact(R.drawable.ic_calendar,"Ngày chi trả: "+date(payroll.dueAt).substring(0,10),18,13,Ui.INK,true));ui.space(card,12);ui.line(card);ui.space(card,14);
        LinearLayout stats=ui.row();ui.weight(stats,metric("Chờ chi",payroll.waitingAmount,Ui.ORANGE));ui.gap(stats,12);ui.weight(stats,metric("Đã chi",payroll.paidAmount,Ui.GREEN));ui.add(card,stats);ui.space(card,14);
        ProgressBar progress=new ProgressBar(requireContext(),null,android.R.attr.progressBarStyleHorizontal);progress.setMax(1000);
        progress.setProgress(payroll.total<=0?0:(int)Math.min(1000,1000d*payroll.paidAmount/payroll.total));
        progress.setProgressTintList(ColorStateList.valueOf(ThemeColors.text(requireContext(),Ui.GREEN)));
        progress.setProgressBackgroundTintList(ColorStateList.valueOf(ThemeColors.border(requireContext(),Ui.BORDER)));
        progress.setContentDescription("Đã chi "+(payroll.total==0?0:Math.round(100d*payroll.paidAmount/payroll.total))+" phần trăm quỹ lương");
        card.addView(progress,ui.lp(-1,6));return card;
    }
    private View metric(String label,long value,int color){
        LinearLayout box=ui.column();ui.add(box,ui.text(label,12,Ui.MUTED,false));ui.space(box,5);
        TextView amount=ui.text(PaymentState.money(value),17,color,true);amount.setMaxLines(1);amount.setAutoSizeTextTypeUniformWithConfiguration(11,17,1,android.util.TypedValue.COMPLEX_UNIT_SP);ui.add(box,amount);return box;
    }
    private View filters(MonthlyPayroll payroll){
        HorizontalScrollView scroll=new HorizontalScrollView(requireContext());scroll.setHorizontalScrollBarEnabled(false);scroll.setClipToPadding(false);
        LinearLayout row=ui.row();scroll.addView(row);
        String[] labels={"Tất cả","Chưa lập ("+payroll.unprepared.size()+")","Chờ chi ("+payroll.ready.size()+")","Đã chi ("+payroll.paid.size()+")"};
        for(int i=0;i<labels.length;i++){
            int index=i;if(i>0)ui.gap(row,8);boolean selected=state().payrollFilter==i;
            TextView tab=ui.text(labels[i],13,selected?Ui.BLUE:Ui.MUTED,selected);tab.setGravity(Gravity.CENTER);ui.pad(tab,14,0);tab.setMinHeight(ui.dp(42));
            ui.surface(tab,selected?Ui.PALE:Ui.WHITE,14,selected?Ui.BLUE:Ui.BORDER);tab.setSelected(selected);
            ui.clickable(tab,()->{state().payrollFilter=index;state().changed();});row.addView(tab);
            if(selected)scroll.post(()->{if(scroll.isAttachedToWindow())scroll.smoothScrollTo(Math.max(0,tab.getLeft()-ui.dp(8)),0);});
        }return scroll;
    }
    private void heading(StableViews rows,String id,String title,int count){
        rows.add("heading/"+id,count,()->{
            LinearLayout row=ui.row();ui.weight(row,ui.text(title,16,Ui.INK,true));row.addView(ui.pill(String.valueOf(count),11,Ui.BLUE,Ui.PALE));return row;
        },ui.dp(10));
    }
    private int payouts(StableViews rows,String section,String title,List<PaymentState.Row> payouts){
        if(payouts.isEmpty())return 0;heading(rows,section,title,payouts.size());
        for(PaymentState.Row payout:payouts){
            String name=tutorName(payout.text("tutor_id"),payout.text("tutor_name"));boolean blocked=banned(payout.text("tutor_id"));
            rows.add("payout/"+payout.id,Arrays.asList(payout,name,blocked),()->payoutCard(payout,name,blocked),ui.dp(12));
        }return payouts.size();
    }
    private View payoutCard(PaymentState.Row payout,String name,boolean blocked){
        LinearLayout card=ui.card();boolean paid=TutorWallet.salaryPaid(payout.text("status")),cancelled="CANCELLED".equals(payout.text("status"));
        int color=paid?Ui.GREEN:cancelled?Ui.MUTED:blocked?Ui.RED:Ui.ORANGE;
        int fill=paid?Ui.GREEN_BG:cancelled?Ui.PALE:blocked?0xFFFFE9EC:Ui.ORANGE_BG;
        ui.add(card,teacher(payout.text("tutor_id"),name,payout.number("session_count")+" buổi học"));ui.space(card,12);
        ui.add(card,ui.pill(blocked&&!paid&&!cancelled?"Tạm dừng chi trả":PaymentState.payoutStatus(payout.text("status")),11,color,fill));ui.space(card,12);ui.line(card);ui.space(card,12);
        LinearLayout bottom=ui.row();ui.weight(bottom,ui.text(PaymentState.money(payout.number("amount")),20,Ui.INK,true));bottom.addView(ui.icon(R.drawable.ic_right,18,Ui.BLUE));ui.add(card,bottom);
        long paidAt=payout.time("credited_at")>0?payout.time("credited_at"):payout.time("paid_at");
        if(paid&&paidAt>0){ui.space(card,6);ui.add(card,ui.text("Đã trả ngày "+date(paidAt).substring(0,10),12,Ui.MUTED,false));}
        else if(!cancelled&&payout.time("due_at")>0){ui.space(card,6);ui.add(card,ui.text("Đến hạn "+date(payout.time("due_at")).substring(0,10),12,Ui.MUTED,false));}
        card.setContentDescription("Xem bảng lương của "+name);ui.clickable(card,()->{state().selectedPayout=payout.id;state().origin="admin_payroll";host().show("payout_detail");});return card;
    }
    private View unpreparedCard(MonthlyPayroll.Unprepared earning,String name,boolean blocked){
        LinearLayout card=ui.card();ui.add(card,teacher(earning.tutorId,name,earning.sessions+" buổi học"));ui.space(card,12);
        LinearLayout amount=ui.row();ui.weight(amount,ui.text(PaymentState.money(earning.amount),20,Ui.INK,true));amount.addView(ui.pill(blocked?"Tài khoản đã khóa":"Chưa lập bảng",11,blocked?Ui.RED:Ui.MUTED,blocked?0xFFFFE9EC:Ui.PALE));ui.add(card,amount);return card;
    }
    private View teacher(String uid,String name,String detail){
        LinearLayout row=ui.row();row.addView(ui.avatar(uid,name,44));ui.gap(row,12);LinearLayout copy=ui.column();
        ui.add(copy,ui.text(name,16,Ui.INK,true));ui.space(copy,5);ui.add(copy,ui.text(detail,12,Ui.MUTED,false));ui.weight(row,copy);return row;
    }
    private String tutorName(String uid,String fallback){
        PaymentState.Row user=host().administration.users.get(uid);if(user==null)user=host().administration.tutors.get(uid);
        String value=user==null?fallback:user.text("ho_ten");return value==null||value.isEmpty()?"Gia sư":value;
    }
    private boolean banned(String uid){
        PaymentState.Row user=host().administration.users.get(uid);if(user==null)user=host().administration.tutors.get(uid);
        return user!=null&&"BANNED".equals(user.text("trang_thai"));
    }
    private void prepare(MonthlyPayroll payroll){
        ui.dialog().setTitle("Lập bảng lương "+AdminMonthPicker.label(payroll.month).toLowerCase(java.util.Locale.forLanguageTag("vi-VN")))
                .setMessage("Chưa lập bảng: "+PaymentState.money(payroll.unpreparedAmount)+"\nNgày chi trả: "+date(payroll.dueAt).substring(0,10))
                .setNegativeButton("Đóng",null).setPositiveButton("Lập bảng",(dialog,which)->{
                    if(!payroll.period.equals(state().payrollPeriod)||state().busy)return;state().preparePayroll();
                }).show();
    }
    @Override public void onDestroyView(){super.onDestroyView();body=null;}
}
