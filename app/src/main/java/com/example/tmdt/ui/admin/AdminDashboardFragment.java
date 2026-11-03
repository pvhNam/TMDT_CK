package com.example.tmdt.ui.admin;

import android.view.View;
import android.widget.LinearLayout;
import com.example.tmdt.PaymentState;
import com.example.tmdt.RevenueBreakdown;
import com.example.tmdt.R;
import com.example.tmdt.ui.common.Ui;

public final class AdminDashboardFragment extends AdminScreen {
    @Override protected View build(){
        LinearLayout root=root("Quản trị hệ thống"),body=ui.page(root);if(!feedback(body))return root;
        ui.add(body,ui.text("Tổng quan",24,Ui.INK,true));ui.space(body,5);
        ui.add(body,ui.text("Theo dõi nhanh, xử lý đúng việc.",13,Ui.MUTED,false));ui.space(body,16);
        LinearLayout row=ui.row();
        ui.weight(row,statistic("Học sinh",String.valueOf(state().totalStudents),()->openUsers("STUDENT")));ui.gap(row,10);
        ui.weight(row,statistic("Gia sư",String.valueOf(state().totalTutors),()->openUsers("TUTOR")));ui.add(body,row);ui.space(body,20);
        ui.section(body,"Cần xử lý",17);ui.space(body,10);
        destination(body,R.drawable.ic_pf_shield,"Duyệt gia sư","Kiểm tra hồ sơ và ảnh minh chứng","",()->open("admin_tutors"));
        int ready=0,refunds=0;for(PaymentState.Row p:money().payouts.values())if("READY".equals(p.text("status")))ready++;
        for(PaymentState.Row r:money().refunds.values())if("PENDING".equals(r.text("status")))refunds++;
        destination(body,R.drawable.ic_wallet,"Chi trả gia sư",ready>0?"Bảng lương đã lập, cần đối soát":"Lập bảng lương và theo dõi chuyển tiền",ready>0?String.valueOf(ready):"",this::openPayroll);
        if(refunds>0)destination(body,R.drawable.ic_coin,"Hoàn học phí","Yêu cầu đang chờ xử lý",String.valueOf(refunds),this::openRefunds);
        ui.space(body,10);ui.section(body,"Doanh thu",17);ui.space(body,10);revenue(body);
        destination(body,R.drawable.ic_users,"Quản lý tài khoản","Học sinh, gia sư và trạng thái hoạt động",String.valueOf(state().totalUsers),()->openUsers("ALL"));
        destination(body,R.drawable.ic_file,"Hợp đồng theo gia sư","Chọn gia sư để xem hợp đồng đã ký","",()->{state().selectTutor("");open("admin_contracts");});
        if(!money().error.isEmpty())ui.add(body,ui.text(money().error,14,Ui.RED,false));
        return root;
    }
    private void revenue(LinearLayout body){
        RevenueBreakdown data=new RevenueBreakdown(money().payments.values());LinearLayout card=ui.card();
        ui.add(card,ui.text("Doanh thu học phí",18,Ui.INK,true));ui.space(card,5);
        ui.add(card,ui.text("Toàn thời gian",12,Ui.MUTED,false));ui.space(card,10);
        ui.add(card,ui.text(PaymentState.money(data.received),27,Ui.BLUE,true));
        boolean animate=!state().revenueAnimated&&data.received>0;if(animate)state().revenueAnimated=true;
        card.addView(new RevenuePieView(ui,data,animate),ui.lp(-1,165));
        for(int i=0;i<RevenueBreakdown.LABELS.length;i++){
            if(i==3&&data.amount(i)==0)continue;
            LinearLayout row=ui.row();View dot=new View(requireContext());
            android.graphics.drawable.GradientDrawable circle=new android.graphics.drawable.GradientDrawable();circle.setShape(android.graphics.drawable.GradientDrawable.OVAL);circle.setColor(RevenuePieView.COLORS[i]);dot.setBackground(circle);
            row.addView(dot,ui.lp(9,9));ui.gap(row,9);ui.weight(row,ui.text(RevenueBreakdown.LABELS[i],13,Ui.INK,false));
            android.widget.TextView value=ui.text(PaymentState.money(data.amount(i))+" · "+String.format(java.util.Locale.forLanguageTag("vi-VN"),"%.1f%%",data.percent(i)),12,Ui.MUTED,true);
            row.addView(value);ui.add(card,row);ui.space(card,10);
        }
        ui.add(card,ui.text(data.received==0?"Chưa có học phí đã nhận. Biểu đồ sẽ cập nhật khi phát sinh thanh toán.":"Học phí đã hoàn: "+PaymentState.money(data.refunded)+"\nSau hoàn tiền: "+PaymentState.money(data.received-data.refunded),12,Ui.MUTED,false));
        ui.add(body,card);ui.space(body,18);
    }
    private void openUsers(String filter){state().userFilter=filter;state().query="";open("admin_users");}
}
