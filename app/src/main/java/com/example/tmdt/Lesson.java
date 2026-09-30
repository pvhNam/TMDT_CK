package com.example.tmdt;

import org.json.JSONException;
import org.json.JSONObject;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** One lesson between a tutor and a student: a booking request, a trial or a confirmed session. */
public final class Lesson {
    public static final String PENDING = "pending", CONFIRMED = "confirmed", CANCELLED = "cancelled", REJECTED = "rejected";
    public static final int TRIAL_PRICE = 50000;
    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy"), TIME = DateTimeFormatter.ofPattern("HH:mm");

    public final int id, tutorId, minutes;
    public final String student, title, mode, goal, address;
    public final boolean trial;
    public String date, status, content = "", cancelReason = "";
    public int hour;
    public boolean finished;
    /** A tutor's proposal to move the lesson; the current slot stays until the student agrees (UC24). */
    public String proposedDate, proposalReason = "";
    public int proposedHour = -1;

    public Lesson(int id, int tutorId, String student, String title, String date, int hour, int minutes, String mode,
           String goal, String address, boolean trial, String status) {
        this.id=id; this.tutorId=tutorId; this.student=student; this.title=title; this.date=date; this.hour=hour;
        this.minutes=minutes; this.mode=mode; this.goal=goal; this.address=address; this.trial=trial; this.status=status;
    }

    public boolean pending() { return PENDING.equals(status); }
    public boolean confirmed() { return CONFIRMED.equals(status); }
    public boolean active() { return pending() || confirmed(); }
    public boolean proposal() { return proposedDate != null; }
    public LocalDateTime start() { return LocalDate.parse(date).atTime(hour,0); }
    public LocalDateTime end() { return start().plusMinutes(minutes); }
    public boolean ended() { return !LocalDateTime.now().isBefore(end()); }
    public boolean started() { return !LocalDateTime.now().isBefore(start()); }
    /** The online room opens ten minutes before the start, as noted on screen 12. */
    public boolean roomOpen() { LocalDateTime now = LocalDateTime.now(); return confirmed() && !now.isBefore(start().minusMinutes(10)) && now.isBefore(end()); }
    public boolean needsConfirmation() { return confirmed() && ended() && !finished; }

    public boolean overlaps(String otherDate, int otherHour, int otherMinutes) {
        int begin = hour*60, finish = begin+minutes, otherBegin = otherHour*60;
        return date.equals(otherDate) && begin < otherBegin+otherMinutes && otherBegin < finish;
    }

    public String dateLabel() { return LocalDate.parse(date).format(DATE); }
    public String timeLabel() { return range(hour,minutes); }
    public static String range(int hour, int minutes) {
        LocalTime start = LocalTime.of(hour,0); return start.format(TIME)+"–"+start.plusMinutes(minutes).format(TIME);
    }
    public String proposalLabel() { return LocalDate.parse(proposedDate).format(DATE)+" · "+range(proposedHour,minutes); }
    public int total() { return trial ? TRIAL_PRICE : Tutor.get(tutorId).price(minutes); }
    public String statusLabel() {
        if (CANCELLED.equals(status)) return "Đã hủy";
        if (REJECTED.equals(status)) return "Bị từ chối";
        if (pending()) return "Chờ xác nhận";
        if (finished) return "Đã hoàn thành";
        return ended() ? "Chờ xác nhận hoàn thành" : "Đã xác nhận";
    }

    public JSONObject toJson() throws JSONException {
        JSONObject data=new JSONObject(); data.put("id",id); data.put("tutor",tutorId); data.put("student",student); data.put("title",title);
        data.put("date",date); data.put("hour",hour); data.put("minutes",minutes); data.put("mode",mode); data.put("goal",goal);
        data.put("address",address); data.put("trial",trial); data.put("status",status); data.put("finished",finished);
        data.put("content",content); data.put("cancelReason",cancelReason);
        if (proposal()) { data.put("proposedDate",proposedDate); data.put("proposedHour",proposedHour); data.put("proposalReason",proposalReason); }
        return data;
    }
    public static Lesson fromJson(JSONObject data, int fallbackId) throws JSONException {
        int tutor = data.getInt("tutor"), hour = data.getInt("hour"), minutes = data.getInt("minutes");
        if (Tutor.get(tutor)==null || hour<0 || hour>23 || minutes<30 || minutes>120)
            throw new JSONException("Invalid lesson");
        String date=data.getString("date"); LocalDate.parse(date);
        // Requests saved before this version only carried a "pending" flag.
        String status=data.optString("status",data.optBoolean("pending",true)?PENDING:CONFIRMED);
        String title=data.optString("title",tutor==0?"Toán lớp 12":"Tiếng Anh giao tiếp");
        Lesson lesson=new Lesson(data.optInt("id",fallbackId),tutor,data.optString("student",Store.STUDENT),title,date,hour,minutes,
                data.getString("mode"),data.getString("goal"),data.optString("address"),data.optBoolean("trial"),status);
        lesson.finished=data.optBoolean("finished"); lesson.content=data.optString("content"); lesson.cancelReason=data.optString("cancelReason");
        if (data.has("proposedDate")) {
            lesson.proposedDate=data.getString("proposedDate"); LocalDate.parse(lesson.proposedDate);
            lesson.proposedHour=data.getInt("proposedHour"); lesson.proposalReason=data.optString("proposalReason");
        }
        return lesson;
    }
}
