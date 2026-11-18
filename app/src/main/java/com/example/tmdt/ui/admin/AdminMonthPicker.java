package com.example.tmdt.ui.admin;

import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.view.View;
import android.view.Gravity;
import com.example.tmdt.R;
import com.example.tmdt.ui.common.ThemeColors;
import com.example.tmdt.ui.common.Ui;
import java.time.YearMonth;
import java.util.function.Consumer;

/** Month-only navigation: no date-format entry or irrelevant day selection. */
public final class AdminMonthPicker {
    private AdminMonthPicker(){}
    public static void add(Ui ui,LinearLayout body,String period,Consumer<String> select){
        ui.add(body,create(ui,period,select));ui.space(body,16);
    }
    public static View create(Ui ui,String period,Consumer<String> select){
        YearMonth month=YearMonth.parse(period);LinearLayout row=ui.row();ui.surface(row,Ui.WHITE,16,Ui.BORDER);
        row.addView(ui.iconButton(R.drawable.ic_back,Ui.BLUE,"Tháng trước",()->select.accept(month.minusMonths(1).toString())));
        TextView current=ui.link(label(month),15,()->show(ui,month,value->select.accept(value.toString())));
        current.setGravity(Gravity.CENTER);current.setMinHeight(ui.dp(48));current.setContentDescription("Chọn tháng: "+label(month));ui.weight(row,current);
        row.addView(ui.iconButton(R.drawable.ic_right,Ui.BLUE,"Tháng sau",()->select.accept(month.plusMonths(1).toString())));return row;
    }
    public static String label(YearMonth month){return "Tháng "+String.format(java.util.Locale.ROOT,"%02d/%d",month.getMonthValue(),month.getYear());}
    public static void show(Ui ui,YearMonth selected,Consumer<YearMonth> select){
        LinearLayout content=ui.row();ui.pad(content,20,8);
        NumberPicker month=new NumberPicker(ui.context),year=new NumberPicker(ui.context);
        month.setMinValue(1);month.setMaxValue(12);month.setDisplayedValues(new String[]{"Tháng 1","Tháng 2","Tháng 3","Tháng 4","Tháng 5","Tháng 6","Tháng 7","Tháng 8","Tháng 9","Tháng 10","Tháng 11","Tháng 12"});
        month.setValue(selected.getMonthValue());year.setMinValue(Math.min(2000,selected.getYear()));year.setMaxValue(Math.max(2099,selected.getYear()));year.setValue(selected.getYear());year.setWrapSelectorWheel(false);
        for(NumberPicker picker:new NumberPicker[]{month,year}){
            picker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
            picker.setTextColor(ThemeColors.text(ui.context,Ui.INK));
            content.addView(picker,new LinearLayout.LayoutParams(0,ui.dp(168),1));
        }
        month.setContentDescription("Chọn tháng");year.setContentDescription("Chọn năm");
        ui.dialog().setTitle("Chọn tháng").setView(content).setNegativeButton("Đóng",null)
                .setPositiveButton("Chọn",(dialog,which)->select.accept(YearMonth.of(year.getValue(),month.getValue()))).show();
    }
}
