package com.example.tmdt.ui.tutor;

import android.app.DatePickerDialog;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.time.LocalDate;
import com.example.tmdt.R;
import com.example.tmdt.ui.common.ScreenFragment;
import com.example.tmdt.ui.common.Ui;
import com.example.tmdt.GroupClass;
import com.example.tmdt.Lesson;

/** Screen 34 · Mở lớp nhóm (UC16), also used to edit an open class (UC17). */
public final class OpenClassFragment extends ScreenFragment {
    private static final String[] SUBJECTS = {"Toán","Tiếng Anh","Vật lý","Hóa học"}, SUBJECT_MARKS = {"∑","A","Φ","⚗"};
    private static final String[] MODES = {"Trực tuyến","Tại nhà"};
    private static final int[] START_HOURS = {7,8,9,14,15,16,17,18,19,20};
    private GroupClass editing;
    private boolean started;
    private String subject = "Toán", mode = "Trực tuyến";
    private int capacity = 5, sessions = 4, hour = 19, minutes = 60;
    private int[] days = {6};
    private LocalDate start;
    private EditText name, price, address, description;
    private TextView subjectMark, subjectValue, modeValue, capacityValue, sessionsValue, scheduleValue, startValue, counter;
    private android.widget.ImageView modeIcon;
    private LinearLayout addressField;

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

        ui.label(body,"Tên lớp *"); name = ui.entry("Ví dụ: Ôn Toán lớp 12",false);
        name.setFilters(new InputFilter[]{new InputFilter.LengthFilter(60)}); ui.add(body,name); ui.space(body,11);

        ui.label(body,"Môn học *");
        subjectMark = ui.text("",30,Ui.INK,true); subjectMark.setGravity(Gravity.CENTER); subjectValue = ui.value("");
        LinearLayout subjectBox = ui.picker(0,subjectValue,this::pickSubject); subjectBox.addView(subjectMark,0,ui.lp(25,-2));
        subjectBox.addView(new View(classroom.context()),1,ui.lp(10,1)); ui.add(body,subjectBox); ui.space(body,11);

        ui.label(body,"Hình thức học *"); modeValue = ui.value("");
        LinearLayout modeBox = ui.picker(R.drawable.ic_laptop,modeValue,this::pickMode); modeIcon = (android.widget.ImageView) modeBox.getChildAt(0);
        ui.add(body,modeBox);
        addressField = ui.column(); ui.space(addressField,11); ui.label(addressField,"Địa điểm học *");
        address = ui.entry("Ví dụ: Quận Cầu Giấy, Hà Nội",false); address.setFilters(new InputFilter[]{new InputFilter.LengthFilter(120)});
        ui.add(addressField,address); ui.add(body,addressField); ui.space(body,11);

        LinearLayout pair = ui.row(); pair.setGravity(Gravity.TOP);
        LinearLayout left = ui.column(), right = ui.column();
        ui.label(left,"Số thành viên tối đa *"); capacityValue = ui.value("");
        ui.add(left,ui.picker(R.drawable.ic_users,capacityValue,this::pickCapacity));
        ui.label(right,"Số buổi *"); sessionsValue = ui.value("");
        ui.add(right,ui.picker(R.drawable.ic_calendar,sessionsValue,this::pickSessions));
        ui.weight(pair,left); ui.gap(pair,25); ui.weight(pair,right); ui.add(body,pair); ui.space(body,11);

        ui.label(body,"Học phí mỗi học viên *");
        LinearLayout priceBox = ui.row(); priceBox.setPadding(ui.dp(12),0,ui.dp(12),0); ui.surface(priceBox,Ui.WHITE,10,Ui.BORDER);
        priceBox.addView(ui.icon(R.drawable.ic_coin,22,Ui.INK)); ui.gap(priceBox,10);
        price = ui.entry("300000",false); price.setBackgroundColor(android.graphics.Color.TRANSPARENT); price.setPadding(0,0,0,0);
        price.setInputType(InputType.TYPE_CLASS_NUMBER); price.setFilters(new InputFilter[]{new InputFilter.LengthFilter(8)});
        price.setTypeface(android.graphics.Typeface.create("sans-serif-condensed",android.graphics.Typeface.BOLD));
        price.setContentDescription("Học phí mỗi học viên, đồng một khóa");
        ui.weight(priceBox,price); priceBox.addView(ui.text("đ / khóa",16,Ui.INK,true)); ui.add(body,priceBox); ui.space(body,11);

        ui.label(body,"Lịch học *"); scheduleValue = ui.value("");
        ui.add(body,ui.picker(R.drawable.ic_calendar,scheduleValue,this::pickDay)); ui.space(body,11);
        ui.label(body,"Ngày bắt đầu *"); startValue = ui.value("");
        ui.add(body,ui.picker(R.drawable.ic_calendar,startValue,this::pickStart)); ui.space(body,11);

        ui.label(body,"Mô tả lớp *"); description = ui.entry("Nội dung, đối tượng và cách học của lớp",true);
        description.setFilters(new InputFilter[]{new InputFilter.LengthFilter(200)}); ui.add(body,description);
        counter = ui.text("",12,Ui.MUTED,false); counter.setGravity(Gravity.END); ui.add(body,counter);
        description.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int a,int b,int c) {}
            public void onTextChanged(CharSequence s,int a,int b,int c) { counter.setText(s.length()+"/200"); }
            public void afterTextChanged(Editable s) {}
        });
        ui.space(body,12);
        ui.addAction(body,ui.action(editing==null?"＋ Tạo lớp":"Lưu thay đổi",0,Ui.PRIMARY,this::submit),47);

        if (editing != null) {
            name.setText(editing.title); price.setText(String.valueOf(editing.price));
            address.setText(editing.address); description.setText(editing.description);
        }
        counter.setText(description.length()+"/200");
        render();
        return root;
    }

    private void render() {
        int index = java.util.Arrays.asList(SUBJECTS).indexOf(subject);
        subjectMark.setText(index<0?"∑":SUBJECT_MARKS[index]); subjectValue.setText(subject);
        modeValue.setText(mode); modeIcon.setImageResource(mode.equals("Tại nhà")?R.drawable.ic_home:R.drawable.ic_laptop);
        addressField.setVisibility(mode.equals("Tại nhà")?View.VISIBLE:View.GONE);
        capacityValue.setText(String.valueOf(capacity)); sessionsValue.setText(String.valueOf(sessions));
        scheduleValue.setText(label()); startValue.setText(start.format(Lesson.DATE));
    }
    private String label() {
        GroupClass preview = new GroupClass("",classroom.me(),""); preview.days=days; preview.hour=hour; preview.minutes=minutes;
        return preview.scheduleLabel();
    }
    private LocalDate nextClassDay(LocalDate from) {
        return from.with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.of(days[0])));
    }

    private void choose(String title, String[] items, int selected, java.util.function.IntConsumer apply) {
        classroom.ui.dialog().setTitle(title)
                .setSingleChoiceItems(items,selected,(dialog,which)->{apply.accept(which);render();dialog.dismiss();})
                .setNegativeButton("Đóng",null).show();
    }
    private void pickSubject() { choose("Môn học",SUBJECTS,java.util.Arrays.asList(SUBJECTS).indexOf(subject),i->subject=SUBJECTS[i]); }
    private void pickMode() { choose("Hình thức học",MODES,mode.equals("Tại nhà")?1:0,i->mode=MODES[i]); }
    private void pickCapacity() {
        String[] values = new String[9]; for (int i=0;i<9;i++) values[i]=String.valueOf(i+2);
        choose("Số thành viên tối đa",values,capacity-2,i->capacity=i+2);
    }
    private void pickSessions() {
        String[] values = new String[12]; for (int i=0;i<12;i++) values[i]=String.valueOf(i+1);
        choose("Số buổi",values,sessions-1,i->sessions=i+1);
    }
    private void pickDay() {
        choose("Ngày học trong tuần",GroupClass.DAYS,days.length==1?days[0]-1:-1,i->{
            days=new int[]{i+1}; start=nextClassDay(start.isAfter(LocalDate.now())?start:LocalDate.now().plusDays(1));
            pickHour();
        });
    }
    private void pickHour() {
        String[] values = new String[START_HOURS.length]; int selected = -1;
        for (int i=0;i<START_HOURS.length;i++) { values[i]=Lesson.range(START_HOURS[i],minutes); if (START_HOURS[i]==hour) selected=i; }
        choose("Giờ học",values,selected,i->hour=START_HOURS[i]);
    }
    private void pickStart() {
        DatePickerDialog dialog = new DatePickerDialog(classroom.context(),R.style.ThemeOverlay_TMDT_Classroom_DatePicker,(view,year,month,day)->{start=LocalDate.of(year,month+1,day);render();},
                start.getYear(),start.getMonthValue()-1,start.getDayOfMonth());
        dialog.getDatePicker().setMinDate(System.currentTimeMillis()+24L*60*60*1000-1000); dialog.show();
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
