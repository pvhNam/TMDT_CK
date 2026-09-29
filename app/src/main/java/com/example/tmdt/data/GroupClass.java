package com.example.tmdt.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import com.example.tmdt.R;

/** A group class with a member limit (UC16–UC20). */
public final class GroupClass {
    public static final String OPEN = "open", RUNNING = "running", ENDED = "ended";
    public static final String[] DAYS = {"Thứ Hai","Thứ Ba","Thứ Tư","Thứ Năm","Thứ Sáu","Thứ Bảy","Chủ nhật"};

    /** A student's request to join; seats are only taken once the tutor accepts it. */
    public static final class Registration {
        public final String student, goal, date;
        public String status;
        public Registration(String student, String goal, String date, String status) { this.student=student; this.goal=goal; this.date=date; this.status=status; }
        public boolean pending() { return Lesson.PENDING.equals(status); }
    }

    public final int id, tutorId;
    public String title, subject, level, mode, address, description, startDate, status;
    public int sessions, price, capacity, hour, minutes, art;
    public boolean perSession;
    public int[] days;
    public final List<String> members = new ArrayList<>();
    public final List<Registration> registrations = new ArrayList<>();

    public GroupClass(int id, int tutorId) { this.id=id; this.tutorId=tutorId; }

    public int seatsLeft() { return Math.max(0, capacity - members.size()); }
    public boolean full() { return seatsLeft() == 0; }
    public boolean recruiting() { return OPEN.equals(status) && !full(); }
    public int pendingCount() { int count=0; for (Registration r : registrations) if (r.pending()) count++; return count; }
    public Registration registrationOf(String student) {
        for (Registration r : registrations) if (r.student.equals(student) && !Lesson.REJECTED.equals(r.status)) return r;
        return null;
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
            if (day == other && hour*60 < otherHour*60+otherMinutes && otherHour*60 < hour*60+minutes) return true;
        return false;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject data = new JSONObject(); data.put("id",id); data.put("tutor",tutorId); data.put("title",title); data.put("subject",subject);
        data.put("level",level); data.put("mode",mode); data.put("address",address); data.put("description",description);
        data.put("start",startDate); data.put("status",status); data.put("sessions",sessions); data.put("price",price);
        data.put("perSession",perSession); data.put("capacity",capacity); data.put("hour",hour); data.put("minutes",minutes); data.put("art",art);
        JSONArray dayList = new JSONArray(); for (int day : days) dayList.put(day); data.put("days",dayList);
        data.put("members",new JSONArray(members));
        JSONArray list = new JSONArray();
        for (Registration r : registrations) {
            JSONObject item = new JSONObject(); item.put("student",r.student); item.put("goal",r.goal); item.put("date",r.date); item.put("status",r.status);
            list.put(item);
        }
        data.put("registrations",list); return data;
    }
    public static GroupClass fromJson(JSONObject data) throws JSONException {
        GroupClass item = new GroupClass(data.getInt("id"),data.getInt("tutor"));
        if (item.tutorId<0 || item.tutorId>=Tutor.ALL.length) throw new JSONException("Invalid tutor");
        item.title=data.getString("title"); item.subject=data.getString("subject"); item.level=data.getString("level");
        item.mode=data.getString("mode"); item.address=data.optString("address"); item.description=data.optString("description");
        item.startDate=data.getString("start"); LocalDate.parse(item.startDate); item.status=data.getString("status");
        item.sessions=data.getInt("sessions"); item.price=data.getInt("price"); item.perSession=data.optBoolean("perSession");
        item.capacity=data.getInt("capacity"); item.hour=data.getInt("hour"); item.minutes=data.getInt("minutes"); item.art=data.optInt("art");
        JSONArray dayList = data.getJSONArray("days"); item.days = new int[dayList.length()];
        for (int i=0;i<dayList.length();i++) item.days[i]=dayList.getInt(i);
        JSONArray memberList = data.getJSONArray("members"); for (int i=0;i<memberList.length();i++) item.members.add(memberList.getString(i));
        JSONArray list = data.getJSONArray("registrations");
        for (int i=0;i<list.length();i++) {
            JSONObject r = list.getJSONObject(i);
            item.registrations.add(new Registration(r.getString("student"),r.getString("goal"),r.getString("date"),r.getString("status")));
        }
        return item;
    }
}
