package com.example.tmdt.ui.booking;

import android.app.DatePickerDialog;
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

/** Screen 39 · Đặt buổi học thử (UC22): a short online lesson at a fixed trial price. */
public final class TrialFragment extends ScreenFragment {
    private static final int[] HOURS = {18, 19, 20}, DURATIONS = {30, 45};
    private Tutor tutor;
    private LocalDate date;
    private int hour = 19, minutes = 30;
    private LinearLayout hours;
    private TextView dateValue, durationValue;
    private EditText goal;

    @Override protected View build() {
        tutor = Tutor.get(classroom.tutorId);
        if (date == null) {
            // Suggest the first day that is free at the default hour.
            date = LocalDate.now().plusDays(1);
            while (classroom.store.conflict(tutor.id,Store.STUDENT,date.toString(),hour,minutes,-1) != null) date = date.plusDays(1);
        }
        LinearLayout root = ui.column(); ui.header(root,"Đặt buổi học thử",classroom::back,null);
        LinearLayout body = ui.page(root);

        LinearLayout card = ui.row(); card.setPadding(ui.dp(9),ui.dp(9),ui.dp(7),ui.dp(9)); ui.surface(card,Ui.WHITE,10,Ui.BORDER);
        card.addView(ui.photo(Store.tutorPhoto(tutor.id),"Ảnh gia sư "+tutor.name,72,76,9)); ui.gap(card,13);
        LinearLayout info = ui.column(); ui.add(info,ui.text(tutor.name,18,Ui.INK,true)); ui.space(info,6);
        ui.add(info,ui.text(tutor.subject+" · "+tutor.level,15,Ui.MUTED,false)); ui.space(info,5);
        LinearLayout rating = ui.row(); rating.addView(ui.icon(R.drawable.ic_star,18,Ui.GOLD)); ui.gap(rating,7);
        rating.addView(ui.text(tutor.ratingLabel(),15,Ui.INK,false)); ui.add(info,rating);
        ui.weight(card,info); card.addView(ui.pill("Học thử",14,Ui.BLUE,Ui.PALE),ui.lp(71,34)); ui.add(body,card);
        TextView quote = ui.text("“Cùng làm quen và trao đổi phương pháp học nhé!”",14,Ui.INK,false);
        quote.setPadding(ui.dp(7),ui.dp(8),ui.dp(7),ui.dp(8)); ui.surface(quote,Ui.PALE,8,0);
        LinearLayout.LayoutParams quoteParams = ui.lp(-1,-2); quoteParams.setMargins(ui.dp(7),ui.dp(-4),ui.dp(7),0); body.addView(quote,quoteParams);
        ui.space(body,14);
        body.addView(ui.pill("▣  Trực tuyến",14,Ui.BLUE,Ui.PALE),ui.lp(149,41)); ui.space(body,13);

        ui.label(body,"Chọn ngày học"); dateValue = ui.value("");
        ui.add(body,ui.picker(R.drawable.ic_calendar,dateValue,this::pickDate)); ui.space(body,14);
        ui.section(body,"Chọn khung giờ có sẵn",18); ui.space(body,8);
        hours = ui.row(); ui.add(body,hours); ui.space(body,16);
        ui.label(body,"Thời lượng buổi học"); durationValue = ui.value("");
        ui.add(body,ui.picker(R.drawable.ic_clock,durationValue,this::pickDuration)); ui.space(body,14);
        ui.label(body,"Mục tiêu buổi học thử"); goal = ui.entry("Bạn muốn trao đổi điều gì?",false);
        goal.setText("Làm quen phương pháp giảng dạy"); goal.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(200)});
        ui.add(body,goal); ui.space(body,14);
        ui.section(body,"Học phí học thử",18); ui.space(body,8);
        LinearLayout price = ui.row(); price.setPadding(ui.dp(13),ui.dp(9),ui.dp(13),ui.dp(9)); ui.surface(price,Ui.PALE,9,0);
        price.addView(ui.icon(R.drawable.ic_coin,22,Ui.INK)); ui.gap(price,18); price.addView(ui.text(Tutor.money(Lesson.TRIAL_PRICE),21,Ui.INK,true));
        ui.add(body,price); ui.space(body,6);
        ui.add(body,ui.note(R.drawable.ic_info,"Buổi học thử cần được gia sư xác nhận.",Ui.WHITE,Ui.BLUE,Ui.MUTED)); ui.space(body,6);
        ui.addAction(body,ui.action("Gửi yêu cầu học thử",R.drawable.ic_send,Ui.PRIMARY,this::submit),51);
        render();
        return root;
    }

    private void render() {
        dateValue.setText(date.format(Lesson.DATE)); durationValue.setText(minutes+" phút");
        hours.removeAllViews();
        for (int i = 0; i < HOURS.length; i++) {
            int value = HOURS[i]; if (i > 0) ui.gap(hours,8);
            TextView option = ui.choice(value+":00",value==hour,()->{hour=value;render();}); option.setMinHeight(ui.dp(43)); ui.weight(hours,option);
        }
    }
    private void pickDate() {
        DatePickerDialog dialog = new DatePickerDialog(classroom.context(),(view,year,month,day)->{date=LocalDate.of(year,month+1,day);render();},
                date.getYear(),date.getMonthValue()-1,date.getDayOfMonth());
        dialog.getDatePicker().setMinDate(System.currentTimeMillis()-1000); dialog.show();
    }
    private void pickDuration() {
        new AlertDialog.Builder(classroom.context()).setTitle("Thời lượng buổi học thử")
                .setSingleChoiceItems(new String[]{"30 phút","45 phút"},minutes==30?0:1,(dialog,which)->{minutes=DURATIONS[which];render();dialog.dismiss();})
                .setNegativeButton("Đóng",null).show();
    }
    private void submit() {
        if (!date.atTime(hour,0).isAfter(LocalDateTime.now())) { classroom.dialog("Chọn thời gian khác","Thời gian học cần ở trong tương lai."); return; }
        String objective = goal.getText().toString().trim();
        if (objective.isEmpty()) { goal.setError("Vui lòng nhập mục tiêu buổi học thử"); goal.requestFocus(); return; }
        for (Lesson other : classroom.lessons)
            if (other.trial && other.tutorId==tutor.id && other.student.equals(Store.STUDENT) && other.active()) {
                classroom.dialog("Đã có buổi học thử","Mỗi học viên được học thử một lần với mỗi gia sư. Xem buổi học thử trong mục Lịch học."); return;
            }
        classroom.saveRequest(new Lesson(classroom.store.nextId(),tutor.id,Store.STUDENT,"Học thử "+tutor.subject,date.toString(),hour,minutes,
                "Trực tuyến",objective,"",true,Lesson.PENDING));
    }
}
