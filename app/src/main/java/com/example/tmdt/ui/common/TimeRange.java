package com.example.tmdt.ui.common;

import android.content.res.ColorStateList;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;
import com.example.tmdt.Lesson;
import com.example.tmdt.R;
import com.google.android.material.slider.RangeSlider;

/** Start hour and length of a lesson on one two-thumb slider; the arrows change the length in exact steps. A fixed length only moves the lesson. */
public final class TimeRange {
    public interface Listener { void changed(int hour, int minutes); }

    private final Ui ui;
    private final int firstHour, lastEnd, shortest, longest, step;
    private final Listener listener;
    private RangeSlider slider;
    private TextView range, length;
    private int hour, minutes;

    public TimeRange(Ui ui, int firstHour, int lastEnd, int shortest, int longest, int hour, int minutes, Listener listener) {
        this(ui,firstHour,lastEnd,shortest,longest,30,hour,minutes,listener);
    }
    public TimeRange(Ui ui, int firstHour, int lastEnd, int shortest, int longest, int step, int hour, int minutes, Listener listener) {
        this.ui=ui; this.firstHour=firstHour; this.lastEnd=lastEnd; this.shortest=shortest; this.longest=longest; this.step=step;
        this.hour=hour; this.minutes=minutes; this.listener=listener; fit();
    }

    public int hour() { return hour; }
    public int minutes() { return minutes; }

    public LinearLayout view(String title) {
        LinearLayout box = ui.column();
        LinearLayout head = ui.row(); head.setGravity(Gravity.CENTER_VERTICAL);
        ui.weight(head,ui.text(title,16,Ui.INK,true));
        boolean adjustable = shortest < longest;
        if (adjustable) { head.addView(arrow(true)); ui.gap(head,6); }
        length = ui.pill("",15,Ui.BLUE,Ui.PALE); head.addView(length,ui.lp(-2,-2));
        if (adjustable) { ui.gap(head,6); head.addView(arrow(false)); }
        ui.add(box,head); ui.space(box,6);
        range = ui.text("",28,Ui.BLUE,true); range.setSingleLine(true); ui.add(box,range); ui.space(box,2);
        ui.add(box,slider());
        LinearLayout ends = ui.row(); ends.setPadding(ui.dp(6),0,ui.dp(6),0);
        ui.weight(ends,ui.text(String.format("%02d:00",firstHour),14,Ui.MUTED,false)); ends.addView(ui.text(lastEnd+":00",14,Ui.MUTED,false));
        ui.add(box,ends); ui.space(box,4);
        ui.add(box,ui.text(adjustable?"Kéo 2 đầu để chọn giờ bắt đầu và giờ kết thúc.":"Kéo để dời giờ học, thời lượng giữ nguyên.",14,Ui.MUTED,false));
        show(); return box;
    }

    private RangeSlider slider() {
        slider = new RangeSlider(ui.context);
        slider.setValueFrom(firstHour); slider.setValueTo(lastEnd); slider.setStepSize(step/60f); slider.setMinSeparationValue(shortest/60f);
        slider.setValues(hour*1f,hour+minutes/60f);
        slider.setTickVisible(false); slider.setLabelFormatter(value->String.format("%02d:%02d",(int)value,Math.round((value%1)*60)));
        slider.setThumbWidth(ui.dp(22)); slider.setThumbHeight(ui.dp(22)); slider.setThumbTrackGapSize(0); slider.setTrackHeight(ui.dp(14));
        slider.setMinimumHeight(ui.dp(64));
        slider.setThumbStrokeColor(ColorStateList.valueOf(Ui.WHITE)); slider.setThumbStrokeWidth(ui.dp(3));
        slider.setThumbTintList(ColorStateList.valueOf(Ui.BLUE)); slider.setHaloTintList(ColorStateList.valueOf(0x33006BFF));
        slider.setTrackActiveTintList(ColorStateList.valueOf(Ui.BLUE)); slider.setTrackInactiveTintList(ColorStateList.valueOf(Ui.BORDER));
        slider.setContentDescription("Giờ bắt đầu và giờ kết thúc");
        slider.addOnChangeListener((view,value,user)->{
            if (!user) return;
            List<Float> values = slider.getValues(); float begin = values.get(0), end = values.get(1);
            if (begin != hour) hour = Math.round(begin);
            else if (shortest == longest) hour = Math.round(end - minutes/60f);
            else minutes = Math.round((end-begin)*60/step)*step;
            fit();
            float wantBegin = hour, wantEnd = hour + minutes/60f;
            if (wantBegin != begin || wantEnd != end) slider.post(()->slider.setValues(wantBegin,wantEnd));
            show(); listener.changed(hour,minutes);
        });
        return slider;
    }

    private View arrow(boolean shorter) {
        FrameLayout button = new FrameLayout(ui.context); ui.fieldSurface(button,Ui.PALE,0);
        ImageView image = ui.art(R.drawable.mui_ten,20,20); if (shorter) image.setRotation(180);
        button.addView(image,new FrameLayout.LayoutParams(ui.dp(20),ui.dp(20),Gravity.CENTER));
        button.setContentDescription((shorter?"Giảm":"Tăng")+" thời lượng "+step+" phút");
        ui.clickable(button,()->change(shorter?-step:step)); button.setLayoutParams(ui.lp(36,36)); return button;
    }

    private void change(int delta) {
        int next = Math.max(shortest,Math.min(Math.min(longest,lastEnd*60-hour*60),minutes+delta));
        if (next == minutes) return;
        minutes = next; slider.setValues(hour*1f,hour+minutes/60f); show(); listener.changed(hour,minutes);
    }

    /** Keeps the length within its bounds and the whole lesson inside the day, moving the start earlier when needed. */
    private void fit() {
        minutes = Math.min(Math.max(shortest,Math.min(longest,minutes)),(lastEnd-firstHour)*60);
        if (hour*60+minutes > lastEnd*60) hour = (lastEnd*60-minutes)/60;
        hour = Math.max(firstHour,hour);
    }

    private void show() {
        range.setText(Lesson.range(hour,minutes).replace("–"," – ")); length.setText("⏱ "+minutes+" phút");
    }
}
