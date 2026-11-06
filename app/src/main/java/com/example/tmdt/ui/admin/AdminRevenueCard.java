package com.example.tmdt.ui.admin;

import android.view.View;
import android.graphics.drawable.GradientDrawable;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.tmdt.PaymentState;
import com.example.tmdt.RevenueBreakdown;
import com.example.tmdt.ui.common.Ui;
import java.util.Locale;

final class AdminRevenueCard {
    private AdminRevenueCard(){}
    static View create(Ui ui,RevenueBreakdown data,boolean pie,boolean animate){
        LinearLayout card=ui.card(),heading=ui.row();ui.weight(heading,ui.text("Học phí đã nhận",14,Ui.MUTED,false));
        heading.addView(ui.pill(data.transactions+" giao dịch",11,Ui.BLUE,Ui.PALE));ui.add(card,heading);ui.space(card,10);
        TextView amount=ui.text(PaymentState.money(data.received),27,Ui.INK,true);amount.setMaxLines(1);
        amount.setAutoSizeTextTypeUniformWithConfiguration(18,27,1,android.util.TypedValue.COMPLEX_UNIT_SP);ui.add(card,amount);
        if(pie){
            ui.space(card,8);card.addView(new RevenuePieView(ui,data,animate),ui.lp(-1,190));
            for(int i=0;i<RevenueBreakdown.LABELS.length;i++){
                if(i==3&&data.amount(i)==0)continue;
                LinearLayout row=ui.row(),copy=ui.column();View dot=new View(ui.context);GradientDrawable circle=new GradientDrawable();
                circle.setShape(GradientDrawable.OVAL);circle.setColor(RevenuePieView.COLORS[i]);dot.setBackground(circle);row.addView(dot,ui.lp(9,9));ui.gap(row,10);
                ui.add(copy,ui.text(RevenueBreakdown.LABELS[i],13,Ui.INK,true));ui.space(copy,4);
                ui.add(copy,ui.text(PaymentState.money(data.amount(i)),12,Ui.MUTED,false));ui.weight(row,copy);
                row.addView(ui.text(String.format(Locale.forLanguageTag("vi-VN"),"%.1f%%",data.percent(i)),13,Ui.INK,true));ui.add(card,row);ui.space(card,12);
            }
        }else ui.space(card,18);
        ui.line(card);ui.space(card,14);LinearLayout totals=ui.row();
        ui.weight(totals,metric(ui,"Hoàn từ khoản thu",data.refunded,Ui.MUTED));ui.gap(totals,12);
        ui.weight(totals,metric(ui,"Thu còn lại",data.received-data.refunded,Ui.GREEN));ui.add(card,totals);return card;
    }
    private static View metric(Ui ui,String label,long value,int color){
        LinearLayout box=ui.column();ui.add(box,ui.text(label,12,Ui.MUTED,false));ui.space(box,6);
        TextView amount=ui.text(PaymentState.money(value),16,color,true);amount.setMaxLines(1);
        amount.setAutoSizeTextTypeUniformWithConfiguration(11,16,1,android.util.TypedValue.COMPLEX_UNIT_SP);ui.add(box,amount);return box;
    }
}
