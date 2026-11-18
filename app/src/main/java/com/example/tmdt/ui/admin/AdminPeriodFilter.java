package com.example.tmdt.ui.admin;

import android.app.DatePickerDialog;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import com.example.tmdt.R;
import com.example.tmdt.ReportPeriod;
import com.example.tmdt.ui.common.Motion;
import com.example.tmdt.ui.common.ThemeColors;
import com.example.tmdt.ui.common.Ui;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/** Edits a draft period; cancel never changes the report currently displayed. */
final class AdminPeriodFilter {
    private static final DateTimeFormatter DAY=DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private AdminPeriodFilter(){}

    static View create(Ui ui,ReportPeriod period,Consumer<ReportPeriod> select){
        LinearLayout card=ui.card();ui.pad(card,14,12);
        ui.add(card,ui.text("Thời gian báo cáo",12,Ui.MUTED,false));ui.space(card,6);
        LinearLayout range=ui.row();range.setMinimumHeight(ui.dp(44));range.addView(ui.icon(R.drawable.ic_calendar,20,Ui.BLUE));ui.gap(range,10);
        ui.weight(range,ui.text(period.label(),14,Ui.INK,true));ui.gap(range,8);range.addView(ui.icon(R.drawable.ic_down,18,Ui.MUTED));
        range.setContentDescription("Chọn thời gian báo cáo, "+period.label());ui.clickable(range,()->show(ui,period,select));ui.add(card,range);ui.space(card,8);
        YearMonth current=ReportPeriod.currentMonth();
        ReportPeriod[] periods={ReportPeriod.month(current),ReportPeriod.months(current.minusMonths(2),current),ReportPeriod.all()};
        String[] titles={"Tháng này","3 tháng","Tất cả"};LinearLayout shortcuts=ui.row();
        for(int i=0;i<titles.length;i++){
            int index=i;if(i>0)ui.gap(shortcuts,6);
            ui.weight(shortcuts,choice(ui,titles[i],period.equals(periods[i]),()->select.accept(periods[index])));
        }
        ui.add(card,shortcuts);return card;
    }

    private static TextView choice(Ui ui,String title,boolean selected,Runnable action){
        TextView text=ui.text(title,12,selected?Ui.BLUE:Ui.MUTED,selected);text.setGravity(Gravity.CENTER);text.setMinHeight(ui.dp(40));
        ui.surface(text,selected?Ui.PALE:Ui.WHITE,12,selected?Ui.BLUE:Ui.BORDER);text.setSelected(selected);
        text.setContentDescription(title+(selected?", đang chọn":""));ui.clickable(text,action);return text;
    }

    private static void show(Ui ui,ReportPeriod original,Consumer<ReportPeriod> select){
        int[] mode={original.mode==ReportPeriod.Mode.MONTHS?1:original.mode==ReportPeriod.Mode.DATES?2:0};
        YearMonth current=ReportPeriod.currentMonth();
        YearMonth[] months={original.from==null?current:YearMonth.from(original.from),original.to==null?current:YearMonth.from(original.to)};
        LocalDate[] dates={original.from==null?current.atDay(1):original.from,original.to==null?current.atEndOfMonth():original.to};
        LinearLayout panel=ui.column();ui.pad(panel,20,10);ScrollView scroll=new ScrollView(ui.context);scroll.addView(panel);scroll.setFillViewport(false);
        TextView error=ui.text("",13,Ui.RED,false);error.setVisibility(View.GONE);error.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        Runnable[] render={null};
        render[0]=()->{
            panel.removeAllViews();LinearLayout modes=ui.row();String[] labels={"Một tháng","Nhiều tháng","Theo ngày"};
            for(int i=0;i<labels.length;i++){
                int index=i;if(i>0)ui.gap(modes,6);
                ui.weight(modes,choice(ui,labels[i],mode[0]==i,()->{mode[0]=index;error.setVisibility(View.GONE);render[0].run();}));
            }
            ui.add(panel,modes);ui.space(panel,18);
            if(mode[0]==0){
                ui.add(panel,field(ui,"Tháng cần xem",AdminMonthPicker.label(months[0]),()->AdminMonthPicker.show(ui,months[0],value->{months[0]=value;render[0].run();})));
            }else if(mode[0]==1){
                ui.add(panel,field(ui,"Từ tháng",AdminMonthPicker.label(months[0]),()->AdminMonthPicker.show(ui,months[0],value->{months[0]=value;render[0].run();})));
                ui.space(panel,12);
                ui.add(panel,field(ui,"Đến tháng",AdminMonthPicker.label(months[1]),()->AdminMonthPicker.show(ui,months[1],value->{months[1]=value;render[0].run();})));
            }else{
                ui.add(panel,field(ui,"Từ ngày",dates[0].format(DAY),()->day(ui,dates[0],value->{dates[0]=value;render[0].run();})));
                ui.space(panel,12);
                ui.add(panel,field(ui,"Đến ngày",dates[1].format(DAY),()->day(ui,dates[1],value->{dates[1]=value;render[0].run();})));
            }
            ui.space(panel,10);ui.add(panel,error);ThemeColors.apply(panel);Motion.bindControls(panel);
        };
        render[0].run();
        AlertDialog dialog=ui.dialog().setTitle("Thời gian báo cáo").setView(scroll).setNegativeButton("Hủy",null).setPositiveButton("Áp dụng",null).create();
        dialog.setOnShowListener(ignored->{
            ThemeColors.apply(dialog.getWindow().getDecorView());
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view->{
                try{
                    ReportPeriod result=mode[0]==0?ReportPeriod.month(months[0]):mode[0]==1?ReportPeriod.months(months[0],months[1]):ReportPeriod.dates(dates[0],dates[1]);
                    select.accept(result);dialog.dismiss();
                }catch(IllegalArgumentException failure){
                    error.setText(mode[0]==1?"Tháng kết thúc phải từ tháng bắt đầu trở đi.":failure.getMessage());error.setVisibility(View.VISIBLE);
                }
            });
        });dialog.show();
    }

    private static View field(Ui ui,String label,String value,Runnable click){
        LinearLayout field=ui.column();ui.add(field,ui.text(label,12,Ui.MUTED,false));ui.space(field,6);
        TextView text=ui.text(value,14,Ui.INK,true);ui.add(field,ui.picker(R.drawable.ic_calendar,text,click));return field;
    }
    private static void day(Ui ui,LocalDate value,Consumer<LocalDate> select){
        DatePickerDialog picker=new DatePickerDialog(ui.context,(view,year,month,day)->select.accept(LocalDate.of(year,month+1,day)),value.getYear(),value.getMonthValue()-1,value.getDayOfMonth());
        picker.setOnShowListener(dialog->{if(picker.getWindow()!=null)ThemeColors.apply(picker.getWindow().getDecorView());});picker.show();
    }
}
