package com.example.tmdt.ui.booking;

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
import com.example.tmdt.R;
import com.example.tmdt.ui.common.ScreenFragment;
import com.example.tmdt.ui.common.Ui;
import com.example.tmdt.Lesson;
import com.example.tmdt.Store;
import com.example.tmdt.Tutor;

public final class BookingFragment extends ScreenFragment {
    private Tutor tutor;
    private LocalDate selectedDate, windowStart;
    private int hour=19, minutes=60;
    private String mode="Trực tuyến", savedGoal, savedAddress="";
    private LinearLayout modes, days, hours, addressBox;
    private TextView month, durationLabel, subtotal, total;
    private EditText goal, address;

    /** Defaults for a new booking, or the draft kept while the trial screen was open or the activity was recreated. */
    private void start() {
        tutor=Tutor.get(activity.tutorId);Bundle state=activity.bookingDraft;
        selectedDate=LocalDate.now().plusDays(4);windowStart=selectedDate.minusDays(1);
        // Start on a day without an existing lesson at the default hour.
        while(activity.store.conflict(tutor.id,Store.STUDENT,selectedDate.toString(),hour,minutes,-1)!=null) selectedDate=selectedDate.plusDays(1);
        windowStart=selectedDate.minusDays(1);
        savedGoal=tutor.id==0?"Ôn tập phương trình bậc hai":tutor.id==1?"Luyện giao tiếp hằng ngày":"";
        if(state!=null) {
            selectedDate=LocalDate.parse(state.getString("date",selectedDate.toString()));
            windowStart=LocalDate.parse(state.getString("window",selectedDate.minusDays(1).toString()));
            hour=state.getInt("hour",19);minutes=state.getInt("minutes",60);
            mode=state.getString("mode","Trực tuyến");savedGoal=state.getString("goal",savedGoal);savedAddress=state.getString("address","");
        }
    }

    @Override protected View build() {
        if(tutor==null) start();
        if(goal!=null) savedGoal=goal.getText().toString();
        if(address!=null) savedAddress=address.getText().toString();
        LinearLayout root=ui.column();ui.header(root,"Đặt lịch học",activity::back,null);
        LinearLayout body=ui.body(root);
        LinearLayout summary=ui.row();summary.setPadding(ui.dp(9),ui.dp(9),ui.dp(9),ui.dp(9));ui.surface(summary,Ui.WHITE,10,Ui.BORDER);
        summary.addView(ui.photo(Store.tutorPhoto(tutor.id),"Ảnh gia sư "+tutor.name,72,71,9));ui.gap(summary,13);
        LinearLayout info=ui.column();ui.add(info,ui.text(tutor.name,17,Ui.INK,true));ui.space(info,6);
        ui.add(info,ui.text(tutor.subject+" · "+tutor.level,15,Ui.MUTED,false));ui.space(info,6);
        LinearLayout rating=ui.row();rating.addView(ui.icon(R.drawable.ic_star,18,Ui.GOLD));ui.gap(rating,7);
        ui.weight(rating,ui.text(tutor.ratingLabel(),15,Ui.INK,false));rating.addView(ui.text(tutor.rateLabel(),15,Ui.INK,false));
        ui.add(info,rating);ui.weight(summary,info);ui.add(body,summary);
        TextView trial=ui.link("Muốn học thử trước? Đặt buổi học thử 30 phút  ›",15,()->activity.show("trial"));ui.add(body,trial);ui.space(body,6);
        ui.section(body,"Hình thức học",18);ui.space(body,8);modes=ui.row();ui.add(body,modes);
        addressBox=ui.column();ui.space(addressBox,10);address=ui.input("Địa chỉ học tại nhà");address.setText(savedAddress);
        address.setContentDescription("Địa chỉ học tại nhà");address.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS);
        ui.add(addressBox,address);ui.add(body,addressBox);renderModes();ui.space(body,18);
        ui.section(body,"Chọn ngày học",18);
        month=ui.text("",15,Ui.INK,false);month.setMinimumHeight(ui.dp(34));month.setGravity(Gravity.CENTER_VERTICAL);
        ui.clickable(month,this::datePicker);ui.add(body,month);
        days=ui.row();ui.add(body,days);renderDays();ui.space(body,15);
        ui.section(body,"Chọn giờ học",18);ui.space(body,8);hours=ui.row();ui.add(body,hours);renderHours();ui.space(body,15);
        LinearLayout duration=ui.row();ui.weight(duration,ui.text("Thời lượng",17,Ui.INK,true));
        durationLabel=ui.value(minutes+" phút");
        LinearLayout picker=ui.picker(R.drawable.ic_clock,durationLabel,()->new AlertDialog.Builder(activity).setTitle("Thời lượng buổi học")
                .setSingleChoiceItems(new String[]{"60 phút","90 phút","120 phút"},minutes==60?0:minutes==90?1:2,(dialog,which)->{
                    minutes=new int[]{60,90,120}[which];durationLabel.setText(minutes+" phút");updatePrice();dialog.dismiss();
                }).setNegativeButton("Đóng",null).show());
        duration.addView(picker,ui.lp(148,40));ui.add(body,duration);ui.space(body,13);
        ui.label(body,"Mục tiêu buổi học");goal=ui.entry("Bạn muốn học nội dung gì?",false);goal.setText(savedGoal);
        goal.setSingleLine(false);goal.setMaxLines(3);goal.setContentDescription("Mục tiêu buổi học");
        goal.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(300)});
        ui.add(body,goal);ui.space(body,12);
        LinearLayout cost=ui.column();ui.pad(cost,15,14);ui.surface(cost,Ui.PALE,10,0);
        LinearLayout priceRow=ui.row();ui.weight(priceRow,ui.text("Học phí",16,Ui.MUTED,false));subtotal=ui.text("",16,Ui.INK,false);priceRow.addView(subtotal);
        ui.add(cost,priceRow);ui.space(cost,12);ui.line(cost);ui.space(cost,12);
        LinearLayout totalRow=ui.row();ui.weight(totalRow,ui.text("Tổng cộng",18,Ui.INK,true));total=ui.text("",20,Ui.INK,true);totalRow.addView(total);
        ui.add(cost,totalRow);ui.add(body,cost);updatePrice();ui.space(body,12);
        LinearLayout note=ui.row();note.addView(ui.icon(R.drawable.ic_info,17,Ui.INK));ui.gap(note,8);
        ui.weight(note,ui.text("Gia sư sẽ xác nhận yêu cầu của bạn.",13,Ui.MUTED,false));ui.add(body,note);ui.space(body,5);
        LinearLayout footer=ui.footer(root);ui.weightAction(footer,ui.action("Gửi yêu cầu đặt lịch",R.drawable.ic_send,Ui.PRIMARY,this::submit),48);
        return root;
    }

    private void setMode(String value){mode=value;renderModes();}
    private void renderModes() {
        modes.removeAllViews();ui.weight(modes,ui.option("Trực tuyến","laptop",mode.equals("Trực tuyến"),()->setMode("Trực tuyến")));
        ui.gap(modes,10);ui.weight(modes,ui.option("Tại nhà","home",mode.equals("Tại nhà"),()->setMode("Tại nhà")));
        addressBox.setVisibility(mode.equals("Tại nhà")?View.VISIBLE:View.GONE);
    }
    private void renderDays() {
        days.removeAllViews();
        month.setText("Tháng "+windowStart.getMonthValue()+", "+windowStart.getYear()+"  ⌄");
        month.setContentDescription("Chọn ngày trong lịch. "+month.getText());
        View previous=ui.iconButton(R.drawable.ic_back,Ui.INK,"Năm ngày trước",()->{
            LocalDate next=windowStart.minusDays(5);windowStart=next.isBefore(LocalDate.now())?LocalDate.now():next;renderDays();
        });
        previous.setLayoutParams(ui.lp(28,64));previous.setEnabled(windowStart.isAfter(LocalDate.now()));previous.setAlpha(previous.isEnabled()?1:0.3f);days.addView(previous);
        for(int i=0;i<5;i++) {
            LocalDate date=windowStart.plusDays(i);boolean selected=date.equals(selectedDate);
            ui.weight(days,ui.day(date,selected,()->{selectedDate=date;renderDays();}));if(i<4)ui.gap(days,5);
        }
        View next=ui.iconButton(R.drawable.ic_right,Ui.INK,"Năm ngày tiếp theo",()->{windowStart=windowStart.plusDays(5);renderDays();});next.setLayoutParams(ui.lp(28,64));days.addView(next);
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
            ui.weight(hours,ui.choice(value+":00",value==hour,()->{hour=value;renderHours();}));if(value!=20)ui.gap(hours,8);
        }
    }
    private void updatePrice(){String price=Tutor.money(tutor.price(minutes));subtotal.setText(price);total.setText(price);}
    private void submit() {
        if(!selectedDate.atTime(hour,0).isAfter(LocalDateTime.now())) {
            activity.dialog("Chọn thời gian khác","Thời gian học cần ở trong tương lai.");return;
        }
        String objective=goal.getText().toString().trim(), location=address.getText().toString().trim();
        if(objective.isEmpty()){goal.setError("Vui lòng nhập mục tiêu buổi học");goal.requestFocus();return;}
        if(mode.equals("Tại nhà") && location.isEmpty()){address.setError("Vui lòng nhập địa chỉ học");address.requestFocus();return;}
        activity.saveRequest(new Lesson(activity.store.nextId(),tutor.id,Store.STUDENT,tutor.course,selectedDate.toString(),
                hour,minutes,mode,objective,mode.equals("Tại nhà")?location:"",false,Lesson.PENDING));
    }
    /** Keeps the draft when the trial screen replaces this one or the activity is recreated. */
    @Override public void onPause() {
        super.onPause();
        if(tutor!=null){Bundle state=new Bundle();saveState(state);activity.bookingDraft=state;}
    }
    private void saveState(Bundle state) {
        state.putString("date",selectedDate.toString());state.putString("window",windowStart.toString());
        state.putInt("hour",hour);state.putInt("minutes",minutes);state.putString("mode",mode);
        state.putString("goal",goal==null?savedGoal:goal.getText().toString());
        state.putString("address",address==null?savedAddress:address.getText().toString());
    }
}
