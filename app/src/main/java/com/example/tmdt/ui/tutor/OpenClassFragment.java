package com.example.tmdt.ui.tutor;

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
import com.example.tmdt.ui.common.TimeRange;
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
    private TextView capacityValue, sessionsValue, startHint, counter;
    private LinearLayout subjects, modes, addressField, weekdays, dates;
    private TimeRange time;

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
        time = new TimeRange(ui,FIRST_HOUR,LAST_END,30,Integer.MAX_VALUE,hour,minutes,(h,m)->{hour=h;minutes=m;showHint();});
        hour = time.hour(); minutes = time.minutes();
        ui.add(plan,time.view("Thời gian dạy *")); ui.space(plan,14);
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
        dates.removeAllViews();
        LocalDate date = nextClassDay(LocalDate.now().plusDays(1));
        if (start.isBefore(date)) start = date;
        for (int i = 0; i < 8; i++, date = date.plusWeeks(1)) {
            LocalDate value = date; if (i > 0) ui.gap(dates,8);
            dates.addView(dateCell(value,value.equals(start),()->{start=value;render();}),ui.lp(64,70));
        }
        showHint();
    }
    private void showHint() {
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
