package com.example.tmdt;

import org.json.JSONException;
import org.json.JSONObject;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

final class Lesson {
    final int tutorId, hour, minutes;
    final String date, mode, goal, address;
    final boolean pending;

    Lesson(int tutorId, String date, int hour, int minutes, String mode, String goal, String address, boolean pending) {
        this.tutorId=tutorId; this.date=date; this.hour=hour; this.minutes=minutes;
        this.mode=mode; this.goal=goal; this.address=address; this.pending=pending;
    }
    String title() { return tutorId==0 ? "Toán lớp 12" : "Tiếng Anh giao tiếp"; }
    String dateLabel() { return LocalDate.parse(date).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")); }
    String timeLabel() {
        DateTimeFormatter format=DateTimeFormatter.ofPattern("HH:mm");
        LocalTime start=LocalTime.of(hour,0);
        return start.format(format)+"–"+start.plusMinutes(minutes).format(format);
    }
    int total() { return Tutor.ALL[tutorId].rate * minutes / 60; }
    boolean completed() { return LocalDate.parse(date).atTime(hour,0).plusMinutes(minutes).isBefore(java.time.LocalDateTime.now()); }
    JSONObject toJson() throws JSONException {
        JSONObject data=new JSONObject(); data.put("tutor",tutorId); data.put("date",date); data.put("hour",hour);
        data.put("minutes",minutes); data.put("mode",mode); data.put("goal",goal); data.put("address",address); data.put("pending",pending); return data;
    }
    static Lesson fromJson(JSONObject data) throws JSONException {
        int tutor = data.getInt("tutor"), hour = data.getInt("hour"), minutes = data.getInt("minutes");
        if (tutor<0 || tutor>=Tutor.ALL.length || hour<0 || hour>23 || minutes<30 || minutes>120)
            throw new JSONException("Invalid lesson");
        String date=data.getString("date"); LocalDate.parse(date);
        return new Lesson(tutor,date,hour,minutes,data.getString("mode"),data.getString("goal"),data.optString("address"),data.optBoolean("pending",true));
    }
}
