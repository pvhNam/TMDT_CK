package com.example.tmdt.ui.tutor;

import android.content.res.ColorStateList;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.time.LocalDate;
import com.example.tmdt.R;
import com.google.android.material.slider.RangeSlider;
import com.example.tmdt.ui.common.ScreenFragment;
import com.example.tmdt.ui.common.Ui;
import com.example.tmdt.GroupClass;
import com.example.tmdt.Lesson;

/** Screen 34 · Mở lớp nhóm (UC16), also used to edit an open class (UC17). */
public final class OpenClassFragment extends ScreenFragment {
    private static final String[] SUBJECTS = {"Toán","Tiếng Anh","Vật lý","Hóa học"};
    private static final String[] MODES = {"Trực tuyến","Tại nhà"};
    private static final String[] WEEKDAYS = {"T2","T3","T4","T5","T6","T7","CN"};
    private static final int FIRST_HOUR = 7, LAST_END = 23;
    private GroupClass editing;
    private boolean started;
    private String subject = "Toán", mode = "Trực tuyến";
    private int capacity = 5, sessions = 4, hour = 19, minutes = 60;
    private int[] days = {6};
    private LocalDate start;
    private EditText name, price, address, description;
    private TextView capacityValue, sessionsValue, timeValue, lengthValue, startHint, counter;
    private LinearLayout subjects, modes, addressField, weekdays, dates;
    private RangeSlider slider;

    /** A new class starts from the defaults; an edited one (UC17) from its saved values. */
    private void start() {
        started = true; editing = classroom.store.groupClass(classroom.classId);
        start = nextClassDay(LocalDate.now().plusDays(1));
        if (editing != null) {
            subject=editing.subject; mode=editing.mode; capacity=editing.capacity; sessions=editing.sessions; hour=editing.hour;
            minutes=editing.minutes; days=editing.days; start=LocalDate.parse(editing.startDate);
        }
    }

    @Override protected View build() {
        if (!started) start();
        LinearLayout root = ui.column(); ui.header(root,editing==null?"Mở lớp nhóm":"Chỉnh sửa lớp",classroom::back,null);
        LinearLayout body = ui.page(root);

        LinearLayout info = group(body,"Thông tin lớp");
        ui.label(info,"Tên lớp *"); name = ui.entry("Ví dụ: Ôn Toán lớp 12",false);
        name.setFilters(new InputFilter[]{new InputFilter.LengthFilter(60)}); ui.add(info,name); ui.space(info,12);
        ui.label(info,"Môn học *"); subjects = ui.row(); ui.add(info,subjects); ui.space(info,12);
        ui.label(info,"Mô tả lớp *"); description = ui.entry("Nội dung, đối tượng và cách học của lớp",true);
        description.setFilters(new InputFilter[]{new InputFilter.LengthFilter(200)}); ui.add(info,description);
        counter = ui.text("",12,Ui.MUTED,false); counter.setGravity(Gravity.END); counter.setPadding(0,ui.dp(4),0,0); ui.add(info,counter);
        description.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int a,int b,int c) {}
            public void onTextChanged(CharSequence s,int a,int b,int c) { counter.setText(s.length()+"/200"); }
            public void afterTextChanged(Editable s) {}
        });

        LinearLayout where = group(body,"Hình thức học");
        modes = ui.row(); ui.add(where,modes);
        addressField = ui.column(); ui.space(addressField,12); ui.label(addressField,"Địa điểm học *");
        address = ui.entry("Ví dụ: Quận Cầu Giấy, Hà Nội",false); address.setFilters(new InputFilter[]{new InputFilter.LengthFilter(120)});
        ui.add(addressField,address); ui.add(where,addressField);

        LinearLayout plan = group(body,"Quy mô và lịch học");
        capacityValue = ui.text("",17,Ui.INK,true);
        ui.add(plan,stepper("Số thành viên tối đa","Từ 2 đến 10 học viên",capacityValue,()->{if(capacity>2)capacity--;render();},()->{if(capacity<10)capacity++;render();}));
        ui.space(plan,10);
        sessionsValue = ui.text("",17,Ui.INK,true);
        ui.add(plan,stepper("Số buổi","Tối đa 12 buổi",sessionsValue,()->{if(sessions>1)sessions--;render();},()->{if(sessions<12)sessions++;render();}));
        ui.space(plan,14);
        ui.label(plan,"Ngày học trong tuần *"); weekdays = ui.row(); ui.add(plan,weekdays); ui.space(plan,14);
        LinearLayout timeHead = ui.row(); timeHead.setGravity(Gravity.CENTER_VERTICAL);
        ui.weight(timeHead,ui.text("Thời gian dạy *",16,Ui.INK,true));
        timeHead.addView(arrow(true)); ui.gap(timeHead,6);
        lengthValue = ui.pill("",15,Ui.BLUE,Ui.PALE); timeHead.addView(lengthValue,ui.lp(-2,-2));
        ui.gap(timeHead,6); timeHead.addView(arrow(false)); ui.add(plan,timeHead); ui.space(plan,6);
        timeValue = ui.text("",28,Ui.BLUE,true); timeValue.setSingleLine(true); ui.add(plan,timeValue); ui.space(plan,2);
        ui.add(plan,timeSlider());
        LinearLayout ends = ui.row(); ends.setPadding(ui.dp(6),0,ui.dp(6),0);
        ui.weight(ends,ui.text(String.format("%02d:00",FIRST_HOUR),14,Ui.MUTED,false)); ends.addView(ui.text(LAST_END+":00",14,Ui.MUTED,false));
        ui.add(plan,ends); ui.space(plan,4);
        ui.add(plan,ui.text("Kéo 2 đầu để chọn giờ bắt đầu và giờ kết thúc.",14,Ui.MUTED,false)); ui.space(plan,14);
        ui.label(plan,"Ngày khai giảng *");
        HorizontalScrollView scroll = new HorizontalScrollView(classroom.context()); scroll.setHorizontalScrollBarEnabled(false);
        dates = ui.row(); scroll.addView(dates); ui.add(plan,scroll); ui.space(plan,6);
        startHint = ui.text("",13,Ui.MUTED,false); ui.add(plan,startHint);

        LinearLayout fee = group(body,"Học phí");
        LinearLayout priceBox = ui.row(); priceBox.setPadding(ui.dp(12),0,ui.dp(12),0); priceBox.setMinimumHeight(ui.dp(46)); ui.fieldSurface(priceBox,0xFFF8FAFF,Ui.BORDER);
        priceBox.addView(ui.icon(R.drawable.ic_coin,22,Ui.INK)); ui.gap(priceBox,10);
        price = ui.entry("300000",false); price.setBackgroundColor(android.graphics.Color.TRANSPARENT); price.setPadding(0,0,0,0);
        price.setInputType(InputType.TYPE_CLASS_NUMBER); price.setFilters(new InputFilter[]{new InputFilter.LengthFilter(8)});
        price.setTypeface(android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.NORMAL));
        price.setContentDescription("Học phí mỗi học viên, đồng một khóa");
        ui.weight(priceBox,price); priceBox.addView(ui.text("đ / khóa",15,Ui.MUTED,true)); ui.add(fee,priceBox); ui.space(fee,6);
        ui.add(fee,ui.text("Mỗi học viên, từ 50.000đ đến 10.000.000đ cho cả khóa.",13,Ui.MUTED,false));

        LinearLayout footer = ui.footer(root);
        ui.weightAction(footer,ui.action(editing==null?"Tạo lớp":"Lưu thay đổi",0,Ui.PRIMARY,this::submit),48);

        if (editing != null) {
            name.setText(editing.title); price.setText(String.valueOf(editing.price));
            address.setText(editing.address); description.setText(editing.description);
        }
        counter.setText(description.length()+"/200");
        render();
        return root;
    }

    private void render() {
        subjects.removeAllViews();
        for (int i = 0; i < SUBJECTS.length; i++) {
            String value = SUBJECTS[i]; if (i > 0) ui.gap(subjects,6);
            TextView chip = ui.choice(value,value.equals(subject),()->{subject=value;render();}); chip.setTextSize(14); ui.weight(subjects,chip);
        }
        modes.removeAllViews();
        ui.weight(modes,ui.option(MODES[0],R.drawable.ic_laptop,mode.equals(MODES[0]),()->{mode=MODES[0];render();})); ui.gap(modes,10);
        ui.weight(modes,ui.option(MODES[1],R.drawable.ic_home_soft,mode.equals(MODES[1]),()->{mode=MODES[1];render();}));
        com.example.tmdt.ui.common.Motion.change((android.view.ViewGroup)addressField.getParent());
        addressField.setVisibility(mode.equals("Tại nhà")?View.VISIBLE:View.GONE);
        capacityValue.setText(String.valueOf(capacity)); sessionsValue.setText(String.valueOf(sessions));
        weekdays.removeAllViews();
        for (int i = 0; i < 7; i++) {
            int day = i+1; boolean on = false; for (int d : days) on |= d==day;
            if (i > 0) ui.gap(weekdays,5);
            TextView chip = ui.choice(WEEKDAYS[i],on,()->{days=new int[]{day};start=nextClassDay(LocalDate.now().plusDays(1));render();});
            chip.setTextSize(14); chip.setContentDescription(GroupClass.DAYS[i]+(on?", đã chọn":"")); ui.weight(weekdays,chip);
        }
        showTime();
        dates.removeAllViews();
        LocalDate date = nextClassDay(LocalDate.now().plusDays(1));
        if (start.isBefore(date)) start = date;
        for (int i = 0; i < 8; i++, date = date.plusWeeks(1)) {
            LocalDate value = date; if (i > 0) ui.gap(dates,8);
            dates.addView(dateCell(value,value.equals(start),()->{start=value;render();}),ui.lp(64,70));
        }
        startHint.setText(label()+" · khai giảng "+start.format(Lesson.DATE)+", "+sessions+" buổi");
    }
    private RangeSlider timeSlider() {
        slider = new RangeSlider(classroom.context());
        slider.setValueFrom(FIRST_HOUR); slider.setValueTo(LAST_END); slider.setStepSize(0.5f); slider.setMinSeparationValue(0.5f);
        hour = Math.max(FIRST_HOUR,Math.min(hour,(int)Math.floor(LAST_END-minutes/60f)));
        slider.setValues(hour*1f,hour+minutes/60f);
        slider.setTickVisible(false); slider.setLabelFormatter(value->String.format("%02d:%02d",(int)value,Math.round((value%1)*60)));
        slider.setThumbWidth(ui.dp(22)); slider.setThumbHeight(ui.dp(22)); slider.setThumbTrackGapSize(0); slider.setTrackHeight(ui.dp(14));
        slider.setMinimumHeight(ui.dp(64));
        slider.setThumbStrokeColor(ColorStateList.valueOf(Ui.WHITE)); slider.setThumbStrokeWidth(ui.dp(3));
        slider.setThumbTintList(ColorStateList.valueOf(Ui.BLUE)); slider.setHaloTintList(ColorStateList.valueOf(0x33006BFF));
        slider.setTrackActiveTintList(ColorStateList.valueOf(Ui.BLUE)); slider.setTrackInactiveTintList(ColorStateList.valueOf(Ui.BORDER));
        slider.setContentDescription("Thời gian dạy: giờ bắt đầu và giờ kết thúc");
        slider.addOnChangeListener((view,value,user)->{
            if (!user) return;
            java.util.List<Float> values = slider.getValues(); float begin = values.get(0), end = values.get(1);
            if (Math.round(begin) != hour || begin % 1 != 0) {
                hour = Math.round(begin); if (hour + minutes/60f > LAST_END) hour = (int)Math.floor(LAST_END - minutes/60f);
            } else minutes = Math.max(30,Math.round((end-begin)*2)*30);
            float wantBegin = hour, wantEnd = hour + minutes/60f;
            if (wantBegin != begin || wantEnd != end) slider.post(()->slider.setValues(wantBegin,wantEnd));
            showTime();
        });
        return slider;
    }
    /** Arrow buttons beside the duration for exact 30-minute steps when dragging is imprecise. */
    private View arrow(boolean shorter) {
        android.widget.FrameLayout button = new android.widget.FrameLayout(classroom.context()); ui.fieldSurface(button,Ui.PALE,0);
        android.widget.ImageView image = ui.art(R.drawable.mui_ten,20,20); if (shorter) image.setRotation(180);
        button.addView(image,new android.widget.FrameLayout.LayoutParams(ui.dp(20),ui.dp(20),Gravity.CENTER));
        button.setContentDescription(shorter?"Giảm thời lượng 30 phút":"Tăng thời lượng 30 phút");
        ui.clickable(button,()->changeLength(shorter?-30:30)); button.setLayoutParams(ui.lp(36,36)); return button;
    }
    private void changeLength(int delta) {
        int next = Math.max(30,Math.min(LAST_END*60-hour*60,minutes+delta));
        if (next == minutes) return;
        minutes = next; slider.setValues(hour*1f,hour+minutes/60f); showTime();
    }
    private void showTime() {
        timeValue.setText(Lesson.range(hour,minutes).replace("–"," – ")); lengthValue.setText("⏱ "+minutes+" phút");
        startHint.setText(label()+" · khai giảng "+start.format(Lesson.DATE)+", "+sessions+" buổi");
    }
    private View dateCell(LocalDate date, boolean selected, Runnable click) {
        LinearLayout cell = ui.column(); cell.setGravity(Gravity.CENTER); ui.fieldSurface(cell,selected?Ui.BLUE:Ui.WHITE,selected?Ui.BLUE:Ui.BORDER);
        cell.addView(ui.text("Th"+date.getMonthValue(),12,selected?Ui.WHITE:Ui.MUTED,false)); ui.space(cell,4);
        cell.addView(ui.text(String.valueOf(date.getDayOfMonth()),19,selected?Ui.WHITE:Ui.INK,true));
        cell.setContentDescription(date.format(Lesson.DATE)+(selected?", đã chọn":"")); ui.clickable(cell,click); return cell;
    }
    private LinearLayout group(LinearLayout body, String title) {
        LinearLayout card = ui.card(); ui.add(card,ui.text(title,17,Ui.INK,true)); ui.space(card,12);
        ui.add(body,card); ui.space(body,12); return card;
    }
    private LinearLayout stepper(String title, String hint, TextView value, Runnable minus, Runnable plus) {
        LinearLayout row = ui.row(); row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout text = ui.column(); ui.add(text,ui.text(title,15,Ui.INK,true)); ui.space(text,2); ui.add(text,ui.text(hint,13,Ui.MUTED,false));
        ui.weight(row,text);
        row.addView(step("−",title+" giảm",minus)); value.setGravity(Gravity.CENTER); row.addView(value,ui.lp(44,-2)); row.addView(step("+",title+" tăng",plus));
        return row;
    }
    private TextView step(String sign, String description, Runnable click) {
        TextView button = ui.text(sign,20,Ui.BLUE,true); button.setGravity(Gravity.CENTER); ui.fieldSurface(button,Ui.PALE,0);
        button.setContentDescription(description); ui.clickable(button,click); button.setLayoutParams(ui.lp(38,38)); return button;
    }
    private String label() {
        GroupClass preview = new GroupClass("",classroom.me(),""); preview.days=days; preview.hour=hour; preview.minutes=minutes;
        return preview.scheduleLabel();
    }
    private LocalDate nextClassDay(LocalDate from) {
        return from.with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.of(days[0])));
    }

    private void submit() {
        String title = name.getText().toString().trim(), place = address.getText().toString().trim(), about = description.getText().toString().trim();
        int fee;
        try { fee = Integer.parseInt(price.getText().toString().trim()); } catch (NumberFormatException invalid) { fee = 0; }
        if (title.isEmpty()) { name.setError("Vui lòng nhập tên lớp"); name.requestFocus(); return; }
        if (mode.equals("Tại nhà") && place.isEmpty()) { address.setError("Vui lòng nhập địa điểm học"); address.requestFocus(); return; }
        if (fee < 50000 || fee > 10000000) { price.setError("Học phí từ 50.000đ đến 10.000.000đ"); price.requestFocus(); return; }
        if (about.isEmpty()) { description.setError("Vui lòng mô tả lớp học"); description.requestFocus(); return; }
        if (!start.isAfter(LocalDate.now())) { classroom.dialog("Ngày bắt đầu chưa hợp lệ","Ngày bắt đầu cần sau hôm nay."); return; }
        boolean onClassDay = false; for (int day : days) onClassDay |= start.getDayOfWeek().getValue()==day;
        if (!onClassDay) { classroom.dialog("Ngày bắt đầu chưa khớp lịch","Ngày bắt đầu cần rơi vào "+label().split(" · ")[0]+"."); return; }
        if (editing != null && capacity < editing.members.size()) {
            classroom.dialog("Số thành viên chưa hợp lệ","Lớp đã có "+editing.members.size()+" học viên, không thể giảm giới hạn xuống "+capacity+"."); return;
        }
        String clash = classroom.store.classClash(classroom.me(),days,hour,minutes,start,sessions,editing==null?"":editing.id);
        if (clash != null) { classroom.dialog("Lịch học bị trùng",clash); return; }

        GroupClass item = editing != null ? editing : new GroupClass(classroom.store.newId("group_classes"),classroom.me(),classroom.tutorName());
        item.title=title; item.subject=subject; item.level=levelOf(title); item.mode=mode; item.address=mode.equals("Tại nhà")?place:"";
        item.description=about; item.startDate=start.toString(); item.sessions=sessions; item.price=fee; item.capacity=capacity;
        item.days=days; item.hour=hour; item.minutes=minutes;
        if (editing == null) {
            item.status=GroupClass.OPEN; item.art=subject.equals("Tiếng Anh")?2:subject.equals("Toán")?0:1;
        }
        classroom.store.saveClass(item); classroom.hideKeyboard(); classroom.classTab=0; classroom.show("classes");
        classroom.notice(editing==null?"Đã mở lớp \""+title+"\".":"Đã lưu thay đổi của lớp.");
    }
    /** The level is taken from the class name ("… lớp 12"), or left general. */
    private String levelOf(String title) {
        java.util.regex.Matcher match = java.util.regex.Pattern.compile("(?i)lớp\\s*(\\d{1,2})").matcher(title);
        return match.find() ? "Lớp "+match.group(1) : editing != null ? editing.level : "Nhiều cấp độ";
    }
}
