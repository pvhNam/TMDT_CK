package com.example.tmdt;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** A group class with a member limit (UC16–UC20), Firestore "group_classes"; members are student uids. */
public final class GroupClass {
    public static final String OPEN = "OPEN", RUNNING = "RUNNING", ENDED = "ENDED";
    public static final String[] DAYS = {"Thứ Hai","Thứ Ba","Thứ Tư","Thứ Năm","Thứ Sáu","Thứ Bảy","Chủ nhật"};

    /** A student's request to join (Firestore "class_registrations"); seats are only taken once the tutor accepts it. */
    public static final class Registration {
        public final String classId, tutorId, studentId, studentName, goal, date;
        public String status;
        public Registration(String classId, String tutorId, String studentId, String studentName, String goal, String date, String status) {
            this.classId=classId; this.tutorId=tutorId; this.studentId=studentId; this.studentName=studentName; this.goal=goal; this.date=date; this.status=status;
        }
        public String id() { return classId+"_"+studentId; }
        public boolean pending() { return Lesson.PENDING.equals(status); }
        public Map<String,Object> toMap() {
            Map<String,Object> data = new HashMap<>();
            data.put("class_id",classId); data.put("tutor_id",tutorId); data.put("student_id",studentId); data.put("ten_hoc_vien",studentName);
            data.put("muc_tieu",goal); data.put("ngay_gui",date); data.put("trang_thai",status);
            return data;
        }
        public static Registration from(Map<String,Object> data) {
            Registration registration = new Registration(Lesson.text(data,"class_id"),Lesson.text(data,"tutor_id"),Lesson.text(data,"student_id"),
                    Lesson.text(data,"ten_hoc_vien"),Lesson.text(data,"muc_tieu"),Lesson.text(data,"ngay_gui"),Lesson.text(data,"trang_thai"));
            try { LocalDate.parse(registration.date); } catch (RuntimeException invalid) { return null; }
            return registration.classId.isEmpty() || registration.studentId.isEmpty() || registration.status.isEmpty() ? null : registration;
        }
    }

    public final String id, tutorId, tutorName;
    public String title, subject, level, mode, address, description, startDate, status;
    public int sessions, price, capacity, hour, minutes, art;
    public boolean perSession;
    public int[] days;
    public final List<String> members = new ArrayList<>();
    public final List<Registration> registrations = new ArrayList<>();

    public GroupClass(String id, String tutorId, String tutorName) { this.id=id; this.tutorId=tutorId; this.tutorName=tutorName; }

    /** One meeting of the class; "number" counts from 1 up to the class's number of sessions. */
    public final class Session {
        public final LocalDate date;
        public final int number;
        Session(LocalDate date, int number) { this.date=date; this.number=number; }
        public GroupClass groupClass() { return GroupClass.this; }
        public LocalDateTime start() { return date.atTime(hour,0); }
        public boolean ended() { return !LocalDateTime.now().isBefore(start().plusMinutes(minutes)); }
    }

    /** An ended class no longer holds sessions, so it takes no time slot. */
    public boolean active() { return !ENDED.equals(status); }
    /** Members and students whose registration is still waiting both keep the class's time slot. */
    public boolean attends(String student) { return members.contains(student) || registrationOf(student) != null; }

    /** True once the last session has finished. */
    public boolean over() { List<Session> list = sessionList(); return !list.isEmpty() && list.get(list.size()-1).ended(); }

    public List<Session> sessionList() {
        List<Session> list = new ArrayList<>(); List<LocalDate> dates = dates(LocalDate.parse(startDate),days,sessions);
        for (int i = 0; i < dates.size(); i++) list.add(new Session(dates.get(i),i+1));
        return list;
    }
    /** The class meets on its weekdays (1 = Monday … 7 = Sunday) from the start date until all sessions are held. */
    public static List<LocalDate> dates(LocalDate start, int[] days, int count) {
        List<LocalDate> list = new ArrayList<>(); LocalDate last = start.plusWeeks(count+1);
        for (LocalDate date = start; list.size() < count && date.isBefore(last); date = date.plusDays(1))
            for (int day : days) if (date.getDayOfWeek().getValue() == day) { list.add(date); break; }
        return list;
    }
    /** True when one of the class's sessions falls on the date and overlaps the given time. */
    public boolean meetsAt(String date, int otherHour, int otherMinutes) {
        return overlap(hour,minutes,otherHour,otherMinutes) && dates(LocalDate.parse(startDate),days,sessions).contains(LocalDate.parse(date));
    }
    public static boolean overlap(int hour, int minutes, int otherHour, int otherMinutes) {
        return hour*60 < otherHour*60+otherMinutes && otherHour*60 < hour*60+minutes;
    }

    public int seatsLeft() { return Math.max(0, capacity - members.size()); }
    public boolean full() { return seatsLeft() == 0; }
    public boolean recruiting() { return OPEN.equals(status) && !full(); }
    public int pendingCount() { int count=0; for (Registration r : registrations) if (r.pending()) count++; return count; }
    public Registration registrationOf(String student) {
        for (Registration r : registrations) if (r.studentId.equals(student) && !Lesson.REJECTED.equals(r.status)) return r;
        return null;
    }
    public String nameOf(String student) {
        for (Registration r : registrations) if (r.studentId.equals(student) && !r.studentName.isEmpty()) return r.studentName;
        return "Học viên";
    }
    public List<String> memberNames() {
        List<String> names = new ArrayList<>();
        for (String member : members) names.add(nameOf(member));
        return names;
    }

    public String daysLabel() {
        if (days.length == 1) return DAYS[days[0]-1];
        StringBuilder label = new StringBuilder();
        for (int day : days) label.append(label.length()==0?"":", ").append(day==7?"Chủ nhật":"Thứ "+(day+1));
        return label.toString();
    }
    public String scheduleLabel() { return daysLabel()+" · "+Lesson.range(hour,minutes); }
    public String priceLabel() { return Tutor.money(price)+(perSession?" / buổi":" / khóa"); }
    public String startLabel() { return LocalDate.parse(startDate).format(Lesson.DATE); }
    /** art: 0 = maths (board/course artwork), 1 = maths (stacked books), 2 = English. */
    public int smallArt() { return art==0 ? R.drawable.illus_math_board : R.drawable.illus_stack_books; }
    public int largeArt() { return art==2 ? R.drawable.illus_english_course : R.drawable.illus_math_course; }

    /** True when both classes meet on a shared weekday at overlapping times. */
    public boolean clashes(int[] otherDays, int otherHour, int otherMinutes) {
        for (int day : days) for (int other : otherDays)
            if (day == other && overlap(hour,minutes,otherHour,otherMinutes)) return true;
        return false;
    }

    public Map<String,Object> toMap() {
        Map<String,Object> data = new HashMap<>();
        data.put("tutor_id",tutorId); data.put("ten_gia_su",tutorName); data.put("ten_lop",title); data.put("mon_hoc",subject); data.put("cap_hoc",level);
        data.put("hinh_thuc",mode); data.put("dia_chi",address); data.put("mo_ta",description); data.put("ngay_bat_dau",startDate);
        List<Integer> dayList = new ArrayList<>(); for (int day : days) dayList.add(day); data.put("thu",dayList);
        data.put("gio",hour); data.put("thoi_luong",minutes); data.put("so_buoi",sessions); data.put("hoc_phi",price); data.put("tinh_theo_buoi",perSession);
        data.put("suc_chua",capacity); data.put("thanh_vien",new ArrayList<>(members)); data.put("trang_thai",status); data.put("hinh_minh_hoa",art);
        return data;
    }
    public static GroupClass from(String id, Map<String,Object> data) {
        GroupClass item = new GroupClass(id,Lesson.text(data,"tutor_id"),Lesson.text(data,"ten_gia_su"));
        item.title=Lesson.text(data,"ten_lop"); item.subject=Lesson.text(data,"mon_hoc"); item.level=Lesson.text(data,"cap_hoc");
        item.mode=Lesson.text(data,"hinh_thuc"); item.address=Lesson.text(data,"dia_chi"); item.description=Lesson.text(data,"mo_ta");
        item.startDate=Lesson.text(data,"ngay_bat_dau"); item.status=Lesson.text(data,"trang_thai");
        item.hour=Lesson.number(data,"gio"); item.minutes=Lesson.number(data,"thoi_luong"); item.sessions=Lesson.number(data,"so_buoi");
        item.price=Lesson.number(data,"hoc_phi"); item.perSession=Boolean.TRUE.equals(data.get("tinh_theo_buoi"));
        item.capacity=Lesson.number(data,"suc_chua"); item.art=Lesson.number(data,"hinh_minh_hoa");
        List<Integer> dayList = new ArrayList<>();
        if (data.get("thu") instanceof List) for (Object day : (List<?>) data.get("thu")) if (day instanceof Number) dayList.add(((Number) day).intValue());
        item.days = new int[dayList.size()]; for (int i = 0; i < item.days.length; i++) item.days[i] = dayList.get(i);
        if (data.get("thanh_vien") instanceof List) for (Object member : (List<?>) data.get("thanh_vien")) if (member instanceof String) item.members.add((String) member);
        try { LocalDate.parse(item.startDate); } catch (RuntimeException invalid) { return null; }
        boolean valid = !item.tutorId.isEmpty() && !item.status.isEmpty() && item.days.length > 0 && item.sessions > 0 && item.minutes > 0;
        for (int day : item.days) valid &= day >= 1 && day <= 7;
        return valid ? item : null;
    }
}
