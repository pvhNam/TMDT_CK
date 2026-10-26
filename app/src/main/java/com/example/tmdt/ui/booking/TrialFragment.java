package com.example.tmdt.ui.booking;

import android.app.DatePickerDialog;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.example.tmdt.R;
import com.example.tmdt.ui.common.ScreenFragment;
import com.example.tmdt.ui.common.TimeRange;
import com.example.tmdt.ui.common.Ui;
import com.example.tmdt.Lesson;
import com.example.tmdt.Tutor;

/** Screen 39 · Đặt buổi học thử (UC22): a short online lesson at a fixed trial price. */
public final class TrialFragment extends ScreenFragment {
    private Tutor tutor;
    private LocalDate date;
    private int hour = 19, minutes = 30;
    private TextView dateValue;
    private EditText goal;

    @Override protected View build() {
        tutor = Tutor.get(classroom.tutorKey);
        if (date == null) {
            // Suggest the first day that is free at the default hour.
            date = LocalDate.now().plusDays(1);
            while (classroom.store.conflict(tutor.id,classroom.me(),date.toString(),hour,minutes,"") != null) date = date.plusDays(1);
        }
        LinearLayout root = ui.column(); ui.header(root,"Đặt buổi học thử",classroom::back,null);
        LinearLayout body = ui.page(root);

        LinearLayout card = ui.row(); card.setPadding(ui.dp(9),ui.dp(9),ui.dp(7),ui.dp(9)); ui.cardSurface(card,Ui.WHITE,Ui.BORDER);
        card.addView(ui.avatar(tutor.id,tutor.name,72)); ui.gap(card,13);
        LinearLayout info = ui.column(); ui.add(info,ui.text(tutor.name,18,Ui.INK,true)); ui.space(info,6);
        ui.add(info,ui.text(tutor.subject+" · "+tutor.level,15,Ui.MUTED,false)); ui.space(info,5);
        LinearLayout rating = ui.row(); rating.addView(ui.icon(R.drawable.ic_star,18,Ui.GOLD)); ui.gap(rating,7);
        rating.addView(ui.text(tutor.ratingLabel(),15,Ui.INK,false)); ui.add(info,rating);
        ui.weight(card,info); card.addView(ui.pill("Học thử",14,Ui.BLUE,Ui.PALE),ui.lp(71,34)); ui.add(body,card);
        TextView quote = ui.text("“Cùng làm quen và trao đổi phương pháp học nhé!”",14,Ui.INK,false);
        quote.setPadding(ui.dp(7),ui.dp(8),ui.dp(7),ui.dp(8)); ui.cardSurface(quote,Ui.PALE,0);
        LinearLayout.LayoutParams quoteParams = ui.lp(-1,-2); quoteParams.setMargins(ui.dp(7),ui.dp(-4),ui.dp(7),0); body.addView(quote,quoteParams);
        ui.space(body,14);
        body.addView(ui.pill("▣  Trực tuyến",14,Ui.BLUE,Ui.PALE),ui.lp(149,41)); ui.space(body,13);

        ui.label(body,"Chọn ngày học"); dateValue = ui.value("");
        ui.add(body,ui.picker(R.drawable.ic_calendar,dateValue,this::pickDate)); ui.space(body,14);
        TimeRange time = new TimeRange(ui,7,23,30,45,15,hour,minutes,(h,mi,m)->{hour=h;minutes=m;});
        hour = time.hour(); minutes = time.minutes();
        ui.add(body,time.view("Chọn giờ học thử")); ui.space(body,14);
        ui.label(body,"Mục tiêu buổi học thử"); goal = ui.entry("Bạn muốn trao đổi điều gì?",false);
        goal.setText("Làm quen phương pháp giảng dạy"); goal.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(200)});
        ui.add(body,goal); ui.space(body,14);
        ui.section(body,"Học phí học thử",18); ui.space(body,8);
        LinearLayout price = ui.row(); price.setPadding(ui.dp(13),ui.dp(9),ui.dp(13),ui.dp(9)); ui.cardSurface(price,Ui.PALE,0);
        price.addView(ui.icon(R.drawable.ic_coin,22,Ui.INK)); ui.gap(price,18); price.addView(ui.text(Tutor.money(Lesson.TRIAL_PRICE),21,Ui.INK,true));
        ui.add(body,price); ui.space(body,6);
        ui.add(body,ui.note(R.drawable.ic_info,"Buổi học thử cần được gia sư xác nhận.",Ui.WHITE,Ui.BLUE,Ui.MUTED)); ui.space(body,6);
        ui.addAction(body,ui.action("Gửi yêu cầu học thử",R.drawable.ic_send,Ui.PRIMARY,this::submit),51);
        render();
        return root;
    }

    private void render() {
        dateValue.setText(date.format(Lesson.DATE));
    }
    private void pickDate() {
        DatePickerDialog dialog = new DatePickerDialog(classroom.context(),R.style.ThemeOverlay_TMDT_Classroom_DatePicker,(view,year,month,day)->{date=LocalDate.of(year,month+1,day);render();},
                date.getYear(),date.getMonthValue()-1,date.getDayOfMonth());
        dialog.getDatePicker().setMinDate(System.currentTimeMillis()-1000); dialog.show();
    }
    private void submit() {
        if (!date.atTime(hour,0).isAfter(LocalDateTime.now())) { classroom.dialog("Chọn thời gian khác","Thời gian học cần ở trong tương lai."); return; }
        String objective = goal.getText().toString().trim();
        if (objective.isEmpty()) { goal.setError("Vui lòng nhập mục tiêu buổi học thử"); goal.requestFocus(); return; }
        String used = classroom.store.trialError(tutor.id,classroom.me());
        if (used != null) { classroom.dialog("Đã có buổi học thử",used); return; }
        classroom.saveRequest(classroom.request(tutor,"Học thử "+tutor.subject,date,hour,minutes,"Trực tuyến",objective,"",true,Lesson.TRIAL_PRICE));
    }
}
