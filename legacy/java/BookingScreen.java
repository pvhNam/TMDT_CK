package com.example.tmdt;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import java.time.LocalDate;
import java.time.LocalDateTime;

final class BookingScreen {
    private final MainActivity activity;
    private final Ui ui;
    private final Tutor tutor;
    private LocalDate selectedDate, windowStart;
    private int hour=19, minutes=60;
    private String mode="Trực tuyến", savedGoal, savedAddress="";
    private LinearLayout modes, days, hours, addressBox;
    private TextView month, durationLabel, subtotal, total;
    private EditText goal, address;

    BookingScreen(MainActivity activity,Tutor tutor,Bundle state) {
        this.activity=activity;this.ui=activity.ui;this.tutor=tutor;
        selectedDate=LocalDate.now().plusDays(4);windowStart=selectedDate.minusDays(1);
        // Start on a day without an existing lesson at the default hour.
        boolean conflict;
        do {
            conflict=false;
            for(Lesson lesson:activity.lessons) {
                if(lesson.date.equals(selectedDate.toString()) && lesson.hour*60 < hour*60+minutes
                        && lesson.hour*60+lesson.minutes > hour*60) {
                    selectedDate=selectedDate.plusDays(1);conflict=true;break;
                }
            }
        } while(conflict);
        windowStart=selectedDate.minusDays(1);
        savedGoal=tutor.id==0?"Ôn tập phương trình bậc hai":"Luyện giao tiếp hằng ngày";
        if(state!=null) {
            selectedDate=LocalDate.parse(state.getString("date",selectedDate.toString()));
            windowStart=LocalDate.parse(state.getString("window",selectedDate.minusDays(1).toString()));
            hour=state.getInt("hour",19);minutes=state.getInt("minutes",60);
            mode=state.getString("mode","Trực tuyến");savedGoal=state.getString("goal",savedGoal);savedAddress=state.getString("address","");
        }
    }

    View build() {
        if(goal!=null) savedGoal=goal.getText().toString();
        if(address!=null) savedAddress=address.getText().toString();
        LinearLayout root=ui.column();ui.header(root,"Đặt lịch học",activity::back,null);
        LinearLayout body=ui.body(root);
        LinearLayout summary=ui.card(), tutorRow=ui.row();tutorRow.addView(ui.portrait(tutor,77,83));ui.gap(tutorRow,11);
        LinearLayout info=ui.column();ui.add(info,ui.text(tutor.name,16,Ui.INK,true));ui.space(info,7);
        ui.add(info,ui.text(tutor.subject+" · "+tutor.level,13,Ui.MUTED,false));ui.space(info,7);
        LinearLayout rating=ui.row();ui.weight(rating,ui.rating(tutor));rating.addView(ui.text(Tutor.money(tutor.rate)+" / giờ",13,Ui.INK,true));
        ui.add(info,rating);ui.weight(tutorRow,info);ui.add(summary,tutorRow);ui.add(body,summary);ui.space(body,18);
        ui.heading(body,"Hình thức học");modes=ui.row();ui.add(body,modes);
        addressBox=ui.column();ui.space(addressBox,10);address=ui.input("Địa chỉ học tại nhà");address.setText(savedAddress);
        address.setContentDescription("Địa chỉ học tại nhà");address.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS);
        ui.add(addressBox,address);ui.add(body,addressBox);renderModes();ui.space(body,18);
        ui.heading(body,"Chọn ngày học");
        month=ui.text("",14,Ui.INK,false);month.setMinimumHeight(ui.dp(40));month.setGravity(Gravity.CENTER_VERTICAL);
        ui.clickable(month,this::datePicker);ui.add(body,month);
        days=ui.row();ui.add(body,days);renderDays();ui.space(body,15);
        ui.heading(body,"Chọn giờ học");hours=ui.row();ui.add(body,hours);renderHours();ui.space(body,15);
        LinearLayout duration=ui.row();ui.weight(duration,ui.text("Thời lượng",16,Ui.INK,true));
        LinearLayout picker=ui.row();ui.pad(picker,12,10);picker.setMinimumHeight(ui.dp(48));ui.surface(picker,Ui.WHITE,9,Ui.BORDER);
        picker.addView(new LineIcon(activity,"clock",Ui.INK),ui.lp(21,21));ui.gap(picker,12);
        durationLabel=ui.text(minutes+" phút",15,Ui.INK,false);picker.addView(durationLabel);ui.gap(picker,16);
        picker.addView(new LineIcon(activity,"down",Ui.INK),ui.lp(17,17));
        ui.clickable(picker,()->new AlertDialog.Builder(activity).setTitle("Thời lượng buổi học")
                .setSingleChoiceItems(new String[]{"60 phút","90 phút","120 phút"},minutes==60?0:minutes==90?1:2,(dialog,which)->{
                    minutes=new int[]{60,90,120}[which];durationLabel.setText(minutes+" phút");updatePrice();dialog.dismiss();
                }).setNegativeButton("Đóng",null).show());
        duration.addView(picker);ui.add(body,duration);ui.space(body,15);
        ui.heading(body,"Mục tiêu buổi học");goal=ui.input("Bạn muốn học nội dung gì?");goal.setText(savedGoal);
        goal.setSingleLine(false);goal.setMaxLines(3);goal.setContentDescription("Mục tiêu buổi học");
        goal.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(300)});
        ui.add(body,goal);ui.space(body,12);
        LinearLayout cost=ui.column();ui.pad(cost,15,14);ui.surface(cost,Ui.PALE,10,0);
        LinearLayout priceRow=ui.row();ui.weight(priceRow,ui.text("Học phí",14,Ui.MUTED,false));subtotal=ui.text("",15,Ui.INK,false);priceRow.addView(subtotal);
        ui.add(cost,priceRow);ui.space(cost,12);ui.line(cost);ui.space(cost,12);
        LinearLayout totalRow=ui.row();ui.weight(totalRow,ui.text("Tổng cộng",16,Ui.INK,true));total=ui.text("",19,Ui.INK,true);totalRow.addView(total);
        ui.add(cost,totalRow);ui.add(body,cost);updatePrice();ui.space(body,12);
        LinearLayout note=ui.row();note.addView(new LineIcon(activity,"info",Ui.MUTED),ui.lp(17,17));ui.gap(note,7);
        ui.weight(note,ui.text("Gia sư sẽ xác nhận yêu cầu của bạn.",12,Ui.MUTED,false));ui.add(body,note);ui.space(body,5);
        LinearLayout footer=ui.footer(root);ui.weight(footer,ui.button("Gửi yêu cầu đặt lịch","send",true,this::submit));
        return root;
    }

    void setMode(String value){mode=value;renderModes();}
    private void renderModes() {
        modes.removeAllViews();ui.weight(modes,ui.option("Trực tuyến","laptop",mode.equals("Trực tuyến"),()->setMode("Trực tuyến")));
        ui.gap(modes,10);ui.weight(modes,ui.option("Tại nhà","home",mode.equals("Tại nhà"),()->setMode("Tại nhà")));
        addressBox.setVisibility(mode.equals("Tại nhà")?View.VISIBLE:View.GONE);
    }
    private void renderDays() {
        days.removeAllViews();
        month.setText("Tháng "+windowStart.getMonthValue()+", "+windowStart.getYear()+"  ⌄");
        month.setContentDescription("Chọn ngày trong lịch. "+month.getText());
        View previous=ui.iconButton("back","Năm ngày trước",()->{
            LocalDate next=windowStart.minusDays(5);windowStart=next.isBefore(LocalDate.now())?LocalDate.now():next;renderDays();
        });
        previous.setLayoutParams(ui.lp(28,64));previous.setEnabled(windowStart.isAfter(LocalDate.now()));previous.setAlpha(previous.isEnabled()?1:0.3f);days.addView(previous);
        for(int i=0;i<5;i++) {
            LocalDate date=windowStart.plusDays(i);boolean selected=date.equals(selectedDate);
            LinearLayout day=ui.column();day.setGravity(Gravity.CENTER);ui.pad(day,0,10);day.setMinimumHeight(ui.dp(67));
            ui.surface(day,selected?Ui.BLUE:Ui.WHITE,10,selected?0:Ui.BORDER);
            int weekday=date.getDayOfWeek().getValue();day.addView(ui.text(weekday==7?"CN":"T"+(weekday+1),11,selected?Ui.WHITE:Ui.MUTED,false));
            ui.space(day,7);day.addView(ui.text(String.valueOf(date.getDayOfMonth()),19,selected?Ui.WHITE:Ui.INK,true));
            day.setContentDescription("Ngày "+date.getDayOfMonth()+" tháng "+date.getMonthValue()+" năm "+date.getYear()+(selected?", đã chọn":""));day.setSelected(selected);
            ui.clickable(day,()->{selectedDate=date;renderDays();});ui.weight(days,day);if(i<4)ui.gap(days,5);
        }
        View next=ui.iconButton("next","Năm ngày tiếp theo",()->{windowStart=windowStart.plusDays(5);renderDays();});next.setLayoutParams(ui.lp(28,64));days.addView(next);
    }
    private void datePicker() {
        DatePickerDialog dialog=new DatePickerDialog(activity,(view,year,month,day)->{
            selectedDate=LocalDate.of(year,month+1,day);windowStart=selectedDate;renderDays();
        },selectedDate.getYear(),selectedDate.getMonthValue()-1,selectedDate.getDayOfMonth());
        dialog.getDatePicker().setMinDate(System.currentTimeMillis()-1000);dialog.show();
    }
    private void renderHours() {
        hours.removeAllViews();
        for(int value:new int[]{18,19,20}) {
            LinearLayout option=ui.button(value+":00",null,value==hour,()->{hour=value;renderHours();});
            if(value!=hour)ui.surface(option,Ui.WHITE,9,Ui.BORDER);
            option.setSelected(value==hour);option.setContentDescription(value+":00"+(value==hour?", đã chọn":""));
            ui.weight(hours,option);if(value!=20)ui.gap(hours,8);
        }
    }
    private void updatePrice(){String price=Tutor.money(tutor.rate*minutes/60);subtotal.setText(price);total.setText(price);}
    private void submit() {
        if(!selectedDate.atTime(hour,0).isAfter(LocalDateTime.now())) {
            activity.dialog("Chọn thời gian khác","Thời gian học cần ở trong tương lai.");return;
        }
        String objective=goal.getText().toString().trim(), location=address.getText().toString().trim();
        if(objective.isEmpty()){goal.setError("Vui lòng nhập mục tiêu buổi học");goal.requestFocus();return;}
        if(mode.equals("Tại nhà") && location.isEmpty()){address.setError("Vui lòng nhập địa chỉ học");address.requestFocus();return;}
        activity.saveRequest(new Lesson(tutor.id,selectedDate.toString(),hour,minutes,mode,objective,mode.equals("Tại nhà")?location:"",true));
    }
    void saveState(Bundle state) {
        state.putString("date",selectedDate.toString());state.putString("window",windowStart.toString());
        state.putInt("hour",hour);state.putInt("minutes",minutes);state.putString("mode",mode);
        state.putString("goal",goal==null?savedGoal:goal.getText().toString());
        state.putString("address",address==null?savedAddress:address.getText().toString());
    }
}
