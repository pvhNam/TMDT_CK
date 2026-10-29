package com.example.tmdt;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** A group class with a member limit (UC16–UC20), Firestore "group_classes"; members are student uids. */
public final class GroupClass {
    public static final String OPEN = "OPEN", RUNNING = "RUNNING", ENDED = "ENDED", CANCELLED = "CANCELLED";
    public static final String[] DAYS = {"Thứ Hai","Thứ Ba","Thứ Tư","Thứ Năm","Thứ Sáu","Thứ Bảy","Chủ nhật"};

    /** A student's request to join (Firestore "class_registrations"); seats are only taken once the tutor accepts it. */
    public static final class Registration {
        public final String classId, tutorId, studentId, studentName, goal, date;
        public String status;
        /** When the tutor last answered, read from Firestore; null until the server has stamped it. */
        public java.time.LocalDateTime updated;
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
            registration.updated = Lesson.time(data.get("updated_at"));
            return registration.classId.isEmpty() || registration.studentId.isEmpty() || registration.status.isEmpty() ? null : registration;
        }
    }

    public final String id, tutorId, tutorName;
    public String title, subject, level, mode, address, description, startDate, status;
    /** Why the tutor cancelled the class; empty unless it is cancelled. */
    public String cancelReason = "";
    /** Meet, Classroom or Zoom link shared by every session; empty means the app's own room. */
    public String roomLink = "";
    public int sessions, price, capacity, hour, minute, minutes, art;
    /** Start of every session in minutes after midnight, so a class can begin on the half hour. */
    public int begin() { return hour*60+minute; }
    public boolean perSession;
    public int[] days;
    public final List<String> members = new ArrayList<>();
    public final List<Registration> registrations = new ArrayList<>();
    /** Sessions the tutor moved one by one, by session number (Firestore "doi_lich"). */
    public final Map<Integer,Move> moves = new HashMap<>();
    /** The proposal the members are answering, or null (Firestore "de_nghi_doi_lich"). */
    public Proposal proposal;

    public GroupClass(String id, String tutorId, String tutorName) { this.id=id; this.tutorId=tutorId; this.tutorName=tutorName; }

    /** The new slot of one session and why it moved. */
    public static final class Move {
        public final String date, reason;
        public final int hour, minute;
        public Move(String date, int hour, int minute, String reason) { this.date=date; this.hour=hour; this.minute=minute; this.reason=reason; }
        public int begin() { return hour*60+minute; }
        public LocalDateTime start() { return LocalDate.parse(date).atTime(hour,minute); }
        public String label(int minutes) { return LocalDate.parse(date).format(Lesson.DATE)+" · "+Lesson.range(hour,minute,minutes); }
        Map<String,Object> toMap() {
            Map<String,Object> data = new HashMap<>();
            data.put("ngay",date); data.put("gio",hour); data.put("phut",minute); data.put("ly_do",reason);
            return data;
        }
        @SuppressWarnings("unchecked")
        static Move from(Object value) {
            if (!(value instanceof Map)) return null;
            Map<String,Object> data = (Map<String,Object>) value;
            Move move = new Move(Lesson.text(data,"ngay"),Lesson.number(data,"gio"),Lesson.number(data,"phut"),Lesson.text(data,"ly_do"));
            try { LocalDate.parse(move.date); } catch (RuntimeException invalid) { return null; }
            return move.hour >= 0 && move.hour < 24 && move.minute >= 0 && move.minute < 60 ? move : null;
        }
    }

    /** The tutor asks to move one session; every member answers whether they can come at the new time. */
    public static final class Proposal {
        public static final String JOIN = "THAM_GIA", BUSY = "BAN";
        public final int number;
        public final Move move;
        public final LocalDateTime deadline;
        /** Member uid → JOIN or BUSY; a member who never answers counts as joining once the deadline passes. */
        public final Map<String,String> answers = new HashMap<>();
        public Proposal(int number, Move move, LocalDateTime deadline) { this.number=number; this.move=move; this.deadline=deadline; }
        /** Tells proposals apart, so a settled one is only written once. */
        public String key() { return number+"@"+move.date+"T"+move.begin(); }
        public String deadlineLabel() { return deadline.format(Lesson.TIME)+" ngày "+deadline.format(Lesson.DATE); }
        Map<String,Object> toMap() {
            Map<String,Object> data = move.toMap();
            data.put("buoi",number); data.put("han",Date.from(deadline.atZone(ZoneId.systemDefault()).toInstant())); data.put("phan_hoi",new HashMap<>(answers));
            return data;
        }
        @SuppressWarnings("unchecked")
        static Proposal from(Object value) {
            Move move = Move.from(value);
            if (move == null) return null;
            Map<String,Object> data = (Map<String,Object>) value;
            LocalDateTime deadline = Lesson.time(data.get("han")); int number = Lesson.number(data,"buoi");
            if (deadline == null || number < 1) return null;
            Proposal proposal = new Proposal(number,move,deadline);
            if (data.get("phan_hoi") instanceof Map)
                for (Map.Entry<?,?> answer : ((Map<?,?>) data.get("phan_hoi")).entrySet())
                    if (answer.getKey() instanceof String && (JOIN.equals(answer.getValue()) || BUSY.equals(answer.getValue())))
                        proposal.answers.put((String) answer.getKey(),(String) answer.getValue());
            return proposal;
        }
    }

    /** Members who answered that they cannot come at the proposed time. */
    public List<String> busy() {
        List<String> list = new ArrayList<>();
        if (proposal != null) for (String member : members) if (Proposal.BUSY.equals(proposal.answers.get(member))) list.add(member);
        return list;
    }
    public int joining() {
        int count = 0;
        if (proposal != null) for (String member : members) if (Proposal.JOIN.equals(proposal.answers.get(member))) count++;
        return count;
    }
    /** Nobody is busy, and either every member can come or the deadline has passed. */
    public boolean proposalAccepted() {
        return proposal != null && busy().isEmpty() && (joining() == members.size() || !LocalDateTime.now().isBefore(proposal.deadline));
    }
    /** Members can still answer; until then the session keeps its time and the proposed slot stays reserved. */
    public boolean proposalOpen() { return proposal != null && !proposalAccepted() && LocalDateTime.now().isBefore(proposal.deadline); }
    /** Makes an accepted proposal part of the schedule and drops one that can no longer apply; false while members can still answer. */
    public boolean settleProposal() {
        if (proposal == null || proposalOpen()) return false;
        if (proposalAccepted()) moves.put(proposal.number,proposal.move);
        proposal = null; return true;
    }
    /** The slot a session moved to, or null while it follows the class schedule. */
    public Move moved(int number) { return proposal != null && proposal.number == number && proposalAccepted() ? proposal.move : moves.get(number); }

    /** One meeting of the class; "number" counts from 1 up to the class's number of sessions. */
    public final class Session {
        public final LocalDate date;
        public final int number, hour, minute;
        /** The slot from the class schedule, shown when the session moved away from it. */
        public final LocalDateTime planned;
        public final Move move;
        Session(LocalDate day, int number, Move move) {
            this.number=number; this.move=move; planned = day.atTime(GroupClass.this.hour,GroupClass.this.minute);
            date = move == null ? day : LocalDate.parse(move.date);
            hour = move == null ? GroupClass.this.hour : move.hour; minute = move == null ? GroupClass.this.minute : move.minute;
        }
        public GroupClass groupClass() { return GroupClass.this; }
        public LocalDateTime start() { return date.atTime(hour,minute); }
        public LocalDateTime end() { return start().plusMinutes(minutes); }
        public int begin() { return hour*60+minute; }
        public boolean moved() { return move != null; }
        public String timeLabel() { return Lesson.range(hour,minute,minutes); }
        public String label() { return date.format(Lesson.DATE)+" · "+timeLabel(); }
        public String plannedLabel() { return planned.format(Lesson.DATE)+" · "+Lesson.range(planned.getHour(),planned.getMinute(),minutes); }
        public boolean started() { return !LocalDateTime.now().isBefore(start()); }
        public boolean ended() { return !LocalDateTime.now().isBefore(end()); }
        /** The room opens ten minutes before the session and closes when it ends. */
        public boolean roomOpen() { LocalDateTime now = LocalDateTime.now(); return !now.isBefore(start().minusMinutes(10)) && !ended(); }
    }

    /** An ended class no longer holds sessions, so it takes no time slot. */
    public boolean active() { return !ENDED.equals(status) && !CANCELLED.equals(status); }
    public boolean cancelled() { return CANCELLED.equals(status); }
    public boolean online() { return !"Tại nhà".equals(mode); }
    public String roomHost() { return Lesson.hostOf(roomLink); }
    public String roomUrl() { return Lesson.urlOf(roomLink,"https://meet.jit.si/TMDT-lop-"+id); }
    /** True while one of the sessions of an online class is in its room window. */
    public boolean roomOpen() {
        if (!active() || !online()) return false;
        for (Session session : sessionList()) if (session.roomOpen()) return true;
        return false;
    }
    /** Only a class that is still recruiting or running can be cancelled, and never after its last session. */
    public boolean cancellable() { return (OPEN.equals(status) || RUNNING.equals(status)) && !over(); }
    /** Members and students whose registration is still waiting both keep the class's time slot. */
    public boolean attends(String student) { return members.contains(student) || registrationOf(student) != null; }

    /** True once every session has finished. */
    public boolean over() {
        List<Session> list = sessionList();
        for (Session session : list) if (!session.ended()) return false;
        return !list.isEmpty();
    }

    public List<Session> sessionList() {
        List<Session> list = new ArrayList<>(); List<LocalDate> dates = dates(LocalDate.parse(startDate),days,sessions);
        for (int i = 0; i < dates.size(); i++) list.add(new Session(dates.get(i),i+1,moved(i+1)));
        return list;
    }
    /** The class meets on its weekdays (1 = Monday … 7 = Sunday) from the start date until all sessions are held. */
    public static List<LocalDate> dates(LocalDate start, int[] days, int count) {
        List<LocalDate> list = new ArrayList<>(); LocalDate last = start.plusWeeks(count+1);
        for (LocalDate date = start; list.size() < count && date.isBefore(last); date = date.plusDays(1))
            for (int day : days) if (date.getDayOfWeek().getValue() == day) { list.add(date); break; }
        return list;
    }
    /** True when one of the class's sessions, or the slot an open proposal holds, falls on the date and overlaps the given time. */
    public boolean meetsAt(String date, int otherBegin, int otherMinutes) {
        for (Session session : sessionList())
            if (session.date.toString().equals(date) && overlap(session.begin(),minutes,otherBegin,otherMinutes)) return true;
        return proposalOpen() && proposal.move.date.equals(date) && overlap(proposal.move.begin(),minutes,otherBegin,otherMinutes);
    }
    /** Both starts are minutes after midnight. */
    public static boolean overlap(int begin, int minutes, int otherBegin, int otherMinutes) {
        return begin < otherBegin+otherMinutes && otherBegin < begin+minutes;
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
    public String scheduleLabel() { return daysLabel()+" · "+Lesson.range(hour,minute,minutes); }
    public String priceLabel() { return Tutor.money(price)+(perSession?" / buổi":" / khóa"); }
    public String startLabel() { return LocalDate.parse(startDate).format(Lesson.DATE); }
    /** art: 0 = maths (board/course artwork), 1 = maths (stacked books), 2 = English. */
    /** Cover photo of the class, chosen by its subject. */
    public int art() {
        switch (subject) {
            case "Tiếng Anh": return R.drawable.anh_lop_anh;
            case "Vật lý": return R.drawable.anh_lop_ly;
            case "Hóa học": return R.drawable.anh_lop_hoa;
            default: return R.drawable.anh_lop_toan;
        }
    }

    /** True when both classes meet on a shared weekday at overlapping times. */
    public boolean clashes(int[] otherDays, int otherBegin, int otherMinutes) {
        for (int day : days) for (int other : otherDays)
            if (day == other && overlap(begin(),minutes,otherBegin,otherMinutes)) return true;
        return false;
    }

    public Map<String,Object> toMap() {
        Map<String,Object> data = new HashMap<>();
        data.put("tutor_id",tutorId); data.put("ten_gia_su",tutorName); data.put("ten_lop",title); data.put("mon_hoc",subject); data.put("cap_hoc",level);
        data.put("hinh_thuc",mode); data.put("dia_chi",address); data.put("mo_ta",description); data.put("ngay_bat_dau",startDate);
        List<Integer> dayList = new ArrayList<>(); for (int day : days) dayList.add(day); data.put("thu",dayList);
        data.put("gio",hour); data.put("phut",minute); data.put("thoi_luong",minutes); data.put("so_buoi",sessions); data.put("hoc_phi",price); data.put("tinh_theo_buoi",perSession);
        data.put("suc_chua",capacity); data.put("thanh_vien",new ArrayList<>(members)); data.put("trang_thai",status); data.put("hinh_minh_hoa",art); data.put("ly_do_huy",cancelReason); data.put("link_phong_hoc",roomLink);
        Map<String,Object> moved = new HashMap<>();
        for (Map.Entry<Integer,Move> move : moves.entrySet()) moved.put(String.valueOf(move.getKey()),move.getValue().toMap());
        data.put("doi_lich",moved); data.put("de_nghi_doi_lich",proposal==null?null:proposal.toMap());
        return data;
    }
    public static GroupClass from(String id, Map<String,Object> data) {
        GroupClass item = new GroupClass(id,Lesson.text(data,"tutor_id"),Lesson.text(data,"ten_gia_su"));
        item.title=Lesson.text(data,"ten_lop"); item.subject=Lesson.text(data,"mon_hoc"); item.level=Lesson.text(data,"cap_hoc");
        item.mode=Lesson.text(data,"hinh_thuc"); item.address=Lesson.text(data,"dia_chi"); item.description=Lesson.text(data,"mo_ta");
        item.startDate=Lesson.text(data,"ngay_bat_dau"); item.status=Lesson.text(data,"trang_thai"); item.cancelReason=Lesson.text(data,"ly_do_huy"); item.roomLink=Lesson.text(data,"link_phong_hoc");
        item.hour=Lesson.number(data,"gio"); item.minute=Lesson.number(data,"phut"); item.minutes=Lesson.number(data,"thoi_luong"); item.sessions=Lesson.number(data,"so_buoi");
        item.price=Lesson.number(data,"hoc_phi"); item.perSession=Boolean.TRUE.equals(data.get("tinh_theo_buoi"));
        item.capacity=Lesson.number(data,"suc_chua"); item.art=Lesson.number(data,"hinh_minh_hoa");
        List<Integer> dayList = new ArrayList<>();
        if (data.get("thu") instanceof List) for (Object day : (List<?>) data.get("thu")) if (day instanceof Number) dayList.add(((Number) day).intValue());
        item.days = new int[dayList.size()]; for (int i = 0; i < item.days.length; i++) item.days[i] = dayList.get(i);
        if (data.get("thanh_vien") instanceof List) for (Object member : (List<?>) data.get("thanh_vien")) if (member instanceof String) item.members.add((String) member);
        if (data.get("doi_lich") instanceof Map) for (Map.Entry<?,?> entry : ((Map<?,?>) data.get("doi_lich")).entrySet()) {
            Move move = Move.from(entry.getValue()); String number = String.valueOf(entry.getKey());
            if (move != null && number.matches("[0-9]{1,2}")) item.moves.put(Integer.parseInt(number),move);
        }
        item.proposal = Proposal.from(data.get("de_nghi_doi_lich"));
        if (item.proposal != null && item.proposal.number > item.sessions) item.proposal = null;
        try { LocalDate.parse(item.startDate); } catch (RuntimeException invalid) { return null; }
        boolean valid = !item.tutorId.isEmpty() && !item.status.isEmpty() && item.days.length > 0 && item.sessions > 0 && item.minutes > 0;
        for (int day : item.days) valid &= day >= 1 && day <= 7;
        return valid ? item : null;
    }
}
