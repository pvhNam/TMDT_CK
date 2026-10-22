package com.example.tmdt.ui.booking;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.tmdt.ui.common.TimeRange;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import com.example.tmdt.R;
import com.example.tmdt.ui.common.ScreenFragment;
import com.example.tmdt.ui.common.Ui;
import com.example.tmdt.Lesson;
import com.example.tmdt.Tutor;

public final class BookingFragment extends ScreenFragment {
    private Tutor tutor;
    private LocalDate selectedDate;
    private static final int FIRST_HOUR=7, LAST_END=23;
    private int hour=19, minutes=60;
    private String mode="Trực tuyến", savedGoal, savedAddress="";
    private LinearLayout modes, days, addressBox;
    private HorizontalScrollView daysScroll;
    private TextView month, subtotal, total;
    private EditText goal, address;

    /** Defaults for a new booking, or the draft kept while the trial screen was open or the classroom was recreated. */
    private void start() {
        tutor=Tutor.get(classroom.tutorKey);Bundle state=classroom.bookingDraft;
        selectedDate=LocalDate.now().plusDays(4);
        // Start on a day without an existing lesson at the default hour.
        while(classroom.store.conflict(tutor.id,classroom.me(),selectedDate.toString(),hour,minutes,"")!=null) selectedDate=selectedDate.plusDays(1);
        savedGoal="";
        if(state!=null) {
            selectedDate=LocalDate.parse(state.getString("date",selectedDate.toString()));
            hour=state.getInt("hour",19);minutes=state.getInt("minutes",60);
            mode=state.getString("mode","Trực tuyến");savedGoal=state.getString("goal",savedGoal);savedAddress=state.getString("address","");
        }
    }

    @Override protected View build() {
        if(tutor==null) start();
        if(goal!=null) savedGoal=goal.getText().toString();
        if(address!=null) savedAddress=address.getText().toString();
        LinearLayout root=ui.column();ui.header(root,"Đặt lịch học",classroom::back,null);
        LinearLayout body=ui.page(root);
        LinearLayout summary=ui.row();summary.setGravity(Gravity.CENTER_VERTICAL);summary.setPadding(ui.dp(12),ui.dp(12),ui.dp(14),ui.dp(12));ui.cardSurface(summary,Ui.WHITE,Ui.BORDER);
        summary.addView(ui.avatar(tutor.id,tutor.name,64));ui.gap(summary,14);
        LinearLayout info=ui.column();TextView name=ui.text(tutor.name,18,Ui.INK,true);name.setSingleLine(true);name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        ui.add(info,name);ui.space(info,3);ui.add(info,ui.text(tutor.subject+" · "+tutor.level,14,Ui.MUTED,false));ui.space(info,8);
        LinearLayout meta=ui.row();meta.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout verified=ui.row();verified.setGravity(Gravity.CENTER_VERTICAL);verified.setPadding(ui.dp(7),ui.dp(3),ui.dp(9),ui.dp(3));ui.fieldSurface(verified,Ui.PALE,0);
        verified.addView(ui.icon(R.drawable.ic_star,14,Ui.GOLD));ui.gap(verified,4);verified.addView(ui.text("Đã xác minh",12,Ui.BLUE,true));
        meta.addView(verified);ui.weight(meta,new View(classroom.context()));
        TextView price=ui.text(tutor.rateLabel(),15,Ui.INK,true);price.setSingleLine(true);meta.addView(price);
        ui.add(info,meta);ui.weight(summary,info);ui.add(body,summary);ui.space(body,10);
        LinearLayout trial=ui.row();trial.setGravity(Gravity.CENTER_VERTICAL);trial.setPadding(ui.dp(14),ui.dp(12),ui.dp(10),ui.dp(12));ui.fieldSurface(trial,Ui.PALE,0);
        LinearLayout trialText=ui.column();ui.add(trialText,ui.text("Muốn học thử trước?",17,Ui.BLUE,true));ui.space(trialText,2);
        ui.add(trialText,ui.text("Đặt buổi học thử 30 phút",15,Ui.INK,false));ui.weight(trial,trialText);
        trial.addView(ui.icon(R.drawable.ic_right,22,Ui.BLUE));trial.setContentDescription("Muốn học thử trước? Đặt buổi học thử 30 phút");
        ui.clickable(trial,()->classroom.show("trial"));ui.add(body,trial);ui.space(body,16);
        ui.section(body,"Hình thức học",18);ui.space(body,8);modes=ui.row();ui.add(body,modes);
        addressBox=ui.column();ui.space(addressBox,10);address=ui.input("Địa chỉ học tại nhà");address.setText(savedAddress);
        address.setContentDescription("Địa chỉ học tại nhà");address.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS);
        ui.add(addressBox,address);ui.add(body,addressBox);renderModes();ui.space(body,18);
        ui.section(body,"Chọn ngày học",18);
        month=ui.text("",15,Ui.INK,false);month.setMinimumHeight(ui.dp(26));month.setGravity(Gravity.CENTER_VERTICAL);
        ui.clickable(month,this::datePicker);ui.add(body,month);
        daysScroll=new HorizontalScrollView(classroom.context());daysScroll.setHorizontalScrollBarEnabled(false);
        days=ui.row();daysScroll.addView(days);ui.add(body,daysScroll);renderDays(true);ui.space(body,15);
        TimeRange time=new TimeRange(ui,FIRST_HOUR,LAST_END,30,Integer.MAX_VALUE,hour,minutes,(h,mi,m)->{hour=h;minutes=m;updatePrice();});
        hour=time.hour();minutes=time.minutes();
        ui.add(body,time.view("Chọn giờ học"));ui.space(body,15);
        ui.label(body,"Mục tiêu buổi học");goal=ui.entry("Bạn muốn học nội dung gì?",false);goal.setText(savedGoal);
        goal.setSingleLine(false);goal.setMaxLines(3);goal.setContentDescription("Mục tiêu buổi học");
        goal.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(300)});
        ui.add(body,goal);ui.space(body,12);
        LinearLayout cost=ui.column();ui.pad(cost,15,14);ui.cardSurface(cost,Ui.PALE,0);
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
        modes.removeAllViews();ui.weight(modes,ui.option("Trực tuyến",R.drawable.ic_laptop,mode.equals("Trực tuyến"),()->setMode("Trực tuyến")));
        ui.gap(modes,10);ui.weight(modes,ui.option("Tại nhà",R.drawable.ic_home_soft,mode.equals("Tại nhà"),()->setMode("Tại nhà")));
        addressBox.setVisibility(mode.equals("Tại nhà")?View.VISIBLE:View.GONE);
    }
    private void renderDays(boolean reveal) {
        days.removeAllViews();
        month.setText("Tháng "+selectedDate.getMonthValue()+", "+selectedDate.getYear());
        month.setContentDescription("Chọn ngày trong lịch. "+month.getText());
        LocalDate first=LocalDate.now(), last=first.plusDays(59);
        if(selectedDate.plusDays(4).isAfter(last)) last=selectedDate.plusDays(4);
        int gap=ui.dp(5), cell=(getResources().getDisplayMetrics().widthPixels-ui.dp(32)-4*gap)/5;
        for(LocalDate date=first;!date.isAfter(last);date=date.plusDays(1)) {
            LocalDate value=date;if(date.isAfter(first))ui.gap(days,5);
            days.addView(ui.day(date,date.equals(selectedDate),()->{selectedDate=value;renderDays(false);}),new LinearLayout.LayoutParams(cell,ui.dp(61)));
        }
        if(reveal){int x=(int)Math.max(0,ChronoUnit.DAYS.between(first,selectedDate)-1)*(cell+gap);daysScroll.post(()->daysScroll.scrollTo(x,0));}
    }
    private void datePicker() {
        DatePickerDialog dialog=new DatePickerDialog(classroom.context(),R.style.ThemeOverlay_TMDT_Classroom_DatePicker,(view,year,month,day)->{
            selectedDate=LocalDate.of(year,month+1,day);renderDays(true);
        },selectedDate.getYear(),selectedDate.getMonthValue()-1,selectedDate.getDayOfMonth());
        dialog.getDatePicker().setMinDate(System.currentTimeMillis()-1000);dialog.show();
    }
    private void updatePrice(){String price=Tutor.money(tutor.price(minutes));subtotal.setText(price);total.setText(price);}
    private void submit() {
        if(!selectedDate.atTime(hour,0).isAfter(LocalDateTime.now())) {
            classroom.dialog("Chọn thời gian khác","Thời gian học cần ở trong tương lai.");return;
        }
        String objective=goal.getText().toString().trim(), location=address.getText().toString().trim();
        if(objective.isEmpty()){goal.setError("Vui lòng nhập mục tiêu buổi học");goal.requestFocus();return;}
        if(mode.equals("Tại nhà") && location.isEmpty()){address.setError("Vui lòng nhập địa chỉ học");address.requestFocus();return;}
        classroom.saveRequest(classroom.request(tutor,tutor.course,selectedDate,hour,minutes,mode,objective,
                mode.equals("Tại nhà")?location:"",false,tutor.price(minutes)));
    }
    /** Keeps the draft when the trial screen replaces this one or the classroom is recreated. */
    @Override public void onPause() {
        super.onPause();
        if(tutor!=null){Bundle state=new Bundle();saveState(state);classroom.bookingDraft=state;}
    }
    private void saveState(Bundle state) {
        state.putString("date",selectedDate.toString());
        state.putInt("hour",hour);state.putInt("minutes",minutes);state.putString("mode",mode);
        state.putString("goal",goal==null?savedGoal:goal.getText().toString());
        state.putString("address",address==null?savedAddress:address.getText().toString());
    }
}
