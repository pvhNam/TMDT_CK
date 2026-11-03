package com.example.tmdt.ui.admin;

import android.widget.LinearLayout;
import android.view.View;
import com.example.tmdt.ui.common.Ui;
import java.time.YearMonth;
import java.util.function.Consumer;

/** Month-only navigation: no date-format entry or irrelevant day selection. */
public final class AdminMonthPicker {
    private AdminMonthPicker(){}
    public static void add(Ui ui,LinearLayout body,String period,Consumer<String> select){
        YearMonth month=YearMonth.parse(period);LinearLayout row=ui.row();
        View previous=ui.action("‹",0,Ui.OUTLINE,()->select.accept(month.minusMonths(1).toString()));previous.setContentDescription("Tháng trước");
        row.addView(previous,ui.lp(44,48));ui.gap(row,8);
        View current=ui.action("Tháng "+month.getMonthValue()+" / "+month.getYear(),0,Ui.OUTLINE,()->{
            YearMonth newest=YearMonth.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));if(month.isAfter(newest))newest=month;
            if(month.isBefore(newest.minusMonths(23)))newest=month.plusMonths(12);
            String[] labels=new String[24],values=new String[24];int checked=0;
            for(int i=0;i<24;i++){YearMonth value=newest.minusMonths(i);values[i]=value.toString();labels[i]="Tháng "+value.getMonthValue()+" / "+value.getYear();if(value.equals(month))checked=i;}
            ui.dialog().setTitle("Chọn tháng").setSingleChoiceItems(labels,checked,(dialog,index)->{dialog.dismiss();select.accept(values[index]);}).setNegativeButton("Đóng",null).show();
        });current.setContentDescription("Chọn tháng");ui.weightAction(row,current,48);ui.gap(row,8);
        View next=ui.action("›",0,Ui.OUTLINE,()->select.accept(month.plusMonths(1).toString()));next.setContentDescription("Tháng sau");row.addView(next,ui.lp(44,48));
        ui.add(body,row);ui.space(body,16);
    }
}
