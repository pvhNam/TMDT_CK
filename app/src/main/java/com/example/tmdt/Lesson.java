package com.example.tmdt;

import com.google.firebase.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/** One lesson between a tutor and a student: a booking request, a trial or a confirmed session (Firestore "lessons"). */
public final class Lesson {
    public static final String PENDING = "PENDING", CONFIRMED = "CONFIRMED", CANCELLED = "CANCELLED", REJECTED = "REJECTED";
    public static final int TRIAL_PRICE = 50000, MAX_PRICE = 50000000, DAY = 24*60;
    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy"), TIME = DateTimeFormatter.ofPattern("HH:mm");

    public final String id, tutorId, tutorName, studentId, studentName, title, mode, goal, address;
    public final int minutes, price;
    public final boolean trial;
    public String date, status, content = "", cancelReason = "", roomLink = "";
    public int hour, minute;
    public boolean finished;
    /** A tutor's proposal to move the lesson; the current slot stays until the student agrees (UC24). */
    public String proposedDate, proposalReason = "";
    /** The student's answer to the last proposal, kept after the proposal itself is cleared so the tutor can see it. */
    public static final String REPLY_ACCEPTED = "DONG_Y", REPLY_KEPT = "GIU_LICH";
    public String reply = "";
    public int proposedHour = -1;

    public Lesson(String id, String tutorId, String tutorName, String studentId, String studentName, String title, String date, int hour,
                  int minutes, String mode, String goal, String address, boolean trial, int price, String status) {
        this.id=id; this.tutorId=tutorId; this.tutorName=tutorName; this.studentId=studentId; this.studentName=studentName;
        this.title=title; this.date=date; this.hour=hour; this.minutes=minutes; this.mode=mode; this.goal=goal; this.address=address;
        this.trial=trial; this.price=price; this.status=status;
    }

    public boolean pending() { return PENDING.equals(status); }
    public boolean confirmed() { return CONFIRMED.equals(status); }
    public boolean active() { return pending() || confirmed(); }
    public boolean proposal() { return proposedDate != null; }
    public LocalDateTime start() { return LocalDate.parse(date).atTime(hour,minute); }
    public LocalDateTime end() { return start().plusMinutes(minutes); }
    public boolean ended() { return !LocalDateTime.now().isBefore(end()); }
    public boolean started() { return !LocalDateTime.now().isBefore(start()); }
    /** The online room opens ten minutes before the start, as noted on screen 12. */
    public boolean roomOpen() { LocalDateTime now = LocalDateTime.now(); return confirmed() && !now.isBefore(start().minusMinutes(10)) && now.isBefore(end()); }
    public boolean online() { return !"Tại nhà".equals(mode); }
    public String roomHost() { return hostOf(roomLink); }
    public String roomUrl() { return urlOf(roomLink,"https://meet.jit.si/TMDT-"+id); }
    /** Domain of a pasted room link without "www.", used to show where the room opens. */
    static String hostOf(String link) {
        try { String host = new java.net.URI(link).getHost(); return host == null ? link : host.replaceFirst("^www\\.",""); }
        catch (java.net.URISyntaxException invalid) { return link; }
    }
    /** The pasted link, or the app's own room when none was pasted. */
    static String urlOf(String link, String fallback) {
        if (link.isEmpty()) return fallback;
        return link.matches("(?i)https?://.*") ? link : "https://" + link;
    }
    /** An answer the tutor should still see: the lesson has not started and no newer proposal is waiting. */
    public boolean replied() { return !reply.isEmpty() && !proposal() && !started(); }
    public boolean kept() { return REPLY_KEPT.equals(reply); }
    public String replyText() {
        return kept() ? studentName+" giữ lịch cũ cho buổi "+title+" · "+dateLabel()+" "+timeLabel()+"."
                : studentName+" đã đồng ý đổi buổi "+title+" sang "+dateLabel()+" "+timeLabel()+".";
    }
    public boolean needsConfirmation() { return confirmed() && ended() && !finished; }

    public boolean overlaps(String otherDate, int otherHour, int otherMinutes) {
        int begin = hour*60+minute, finish = begin+minutes, otherBegin = otherHour*60;
        return date.equals(otherDate) && begin < otherBegin+otherMinutes && otherBegin < finish;
    }

    public String dateLabel() { return LocalDate.parse(date).format(DATE); }
    public String timeLabel() { return range(hour,minute,minutes); }
    public static String range(int hour, int minutes) { return range(hour,0,minutes); }
    public static String range(int hour, int minute, int minutes) {
        LocalTime start = LocalTime.of(hour,minute); return start.format(TIME)+"–"+start.plusMinutes(minutes).format(TIME);
    }
    public String proposalLabel() { return LocalDate.parse(proposedDate).format(DATE)+" · "+range(proposedHour,minutes); }
    public int total() { return price; }
    public String statusLabel() {
        if (CANCELLED.equals(status)) return "Đã hủy";
        if (REJECTED.equals(status)) return "Bị từ chối";
        if (pending()) return "Chờ xác nhận";
        if (finished) return "Đã hoàn thành";
        return ended() ? "Chờ xác nhận hoàn thành" : "Đã xác nhận";
    }

    public Map<String,Object> toMap() {
        Map<String,Object> data = new HashMap<>();
        data.put("tutor_id",tutorId); data.put("ten_gia_su",tutorName); data.put("student_id",studentId); data.put("ten_hoc_vien",studentName);
        data.put("tieu_de",title); data.put("bat_dau",instant(date,hour,minute)); data.put("thoi_luong",minutes); data.put("hinh_thuc",mode);
        data.put("dia_chi",address); data.put("muc_tieu",goal); data.put("hoc_thu",trial); data.put("hoc_phi",price); data.put("trang_thai",status);
        data.put("da_hoan_thanh",finished); data.put("noi_dung",content); data.put("ly_do_huy",cancelReason);
        data.put("doi_lich_bat_dau",proposal()?instant(proposedDate,proposedHour):null); data.put("doi_lich_ly_do",proposalReason);
        data.put("doi_lich_phan_hoi",reply);
        data.put("link_phong_hoc",roomLink);
        return data;
    }
    /** Null when the document is incomplete, so one bad record never hides the others. */
    public static Lesson from(String id, Map<String,Object> data) {
        LocalDateTime start = time(data.get("bat_dau"));
        int minutes = number(data,"thoi_luong");
        String tutor = text(data,"tutor_id"), student = text(data,"student_id"), status = text(data,"trang_thai");
        if (start == null || minutes < 30 || minutes > DAY || tutor.isEmpty() || student.isEmpty() || status.isEmpty()) return null;
        Lesson lesson = new Lesson(id,tutor,text(data,"ten_gia_su"),student,text(data,"ten_hoc_vien"),text(data,"tieu_de"),
                start.toLocalDate().toString(),start.getHour(),minutes,text(data,"hinh_thuc"),text(data,"muc_tieu"),text(data,"dia_chi"),
                Boolean.TRUE.equals(data.get("hoc_thu")),number(data,"hoc_phi"),status);
        lesson.minute = start.getMinute(); lesson.roomLink = text(data,"link_phong_hoc"); lesson.reply = text(data,"doi_lich_phan_hoi");
        lesson.finished = Boolean.TRUE.equals(data.get("da_hoan_thanh"));
        lesson.content = text(data,"noi_dung"); lesson.cancelReason = text(data,"ly_do_huy");
        LocalDateTime proposed = time(data.get("doi_lich_bat_dau"));
        if (proposed != null) {
            lesson.proposedDate = proposed.toLocalDate().toString(); lesson.proposedHour = proposed.getHour();
            lesson.proposalReason = text(data,"doi_lich_ly_do");
        }
        return lesson;
    }

    static Date instant(String date, int hour) { return instant(date,hour,0); }
    static Date instant(String date, int hour, int minute) { return Date.from(LocalDate.parse(date).atTime(hour,minute).atZone(ZoneId.systemDefault()).toInstant()); }
    static LocalDateTime time(Object value) {
        if (value instanceof Timestamp) value = ((Timestamp) value).toDate();
        return value instanceof Date ? LocalDateTime.ofInstant(((Date) value).toInstant(),ZoneId.systemDefault()) : null;
    }
    static String text(Map<String,Object> data, String key) { Object value = data.get(key); return value instanceof String ? (String) value : ""; }
    static int number(Map<String,Object> data, String key) { Object value = data.get(key); return value instanceof Number ? ((Number) value).intValue() : 0; }
}
