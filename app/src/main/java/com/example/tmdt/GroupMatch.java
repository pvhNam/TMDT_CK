package com.example.tmdt;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ghép nhóm có cùng yêu cầu: open tutor requests (Firestore "tutor_requests", read only) for the same subject and level
 * are gathered so a tutor can open one group class for them, and a student sees the open classes that suit their requests.
 */
public final class GroupMatch {
    /** Subjects a group class can be opened for (screen 34). */
    public static final String[] SUBJECTS = {"Toán","Tiếng Anh","Vật lý","Hóa học"};
    public static final String[] LEVELS = {"TIEU_HOC","THCS","THPT","LUYEN_THI","KHAC"};
    public static final String MORNING = "sáng", AFTERNOON = "chiều", EVENING = "tối";

    /** One open request a student posted to find a tutor. */
    public static final class Request {
        public final String id, studentId, studentName, title, subject, level, schedule;
        public final long budget;
        public final Wish wish;
        public Request(String id, String studentId, String studentName, String title, String subject, String level, long budget, String schedule) {
            this.id=id; this.studentId=studentId; this.studentName=studentName; this.title=title; this.subject=subject; this.level=level;
            this.budget=budget; this.schedule=schedule; wish=read(schedule);
        }
        /** The request, or null when it is no longer open. */
        public static Request from(String id, Map<String,Object> data) {
            if (data == null || !"OPEN".equals(data.get("trang_thai"))) return null;
            Object amount = data.get("ngan_sach");
            Request request = new Request(id,Lesson.text(data,"student_id"),Lesson.text(data,"ten_hoc_vien"),Lesson.text(data,"tieu_de"),
                    Lesson.text(data,"mon_hoc"),Lesson.text(data,"cap_hoc"),amount instanceof Number ? Math.max(0,((Number) amount).longValue()) : 0,
                    Lesson.text(data,"lich_hoc"));
            return request.studentId.isEmpty() ? null : request;
        }
    }

    /** Days (1 = Monday … 7 = Sunday), parts of the day and start hour read from a free-text schedule. */
    public static final class Wish {
        public final Set<Integer> days = new TreeSet<>();
        public final Set<String> parts = new LinkedHashSet<>();
        public int hour = -1;
    }

    /** Students who want the same subject at the same level. */
    public static final class Group {
        public final String subject, level;
        public final List<Request> requests = new ArrayList<>();
        Group(String subject, String level) { this.subject=subject; this.level=level; }
        public int size() { return requests.size(); }
        public String levelLabel() { return GroupMatch.levelLabel(level); }
        public String need() { return "KHAC".equals(level) ? subject : subject+" · "+levelLabel(); }

        public String budgetLabel() {
            long low = Long.MAX_VALUE, high = 0;
            for (Request request : requests) if (request.budget > 0) { low = Math.min(low,request.budget); high = Math.max(high,request.budget); }
            if (high == 0) return "";
            return (low == high ? money(low) : money(low)+" – "+money(high))+" / buổi";
        }
        /** Middle budget per session, 0 when nobody gave one. */
        public long budget() {
            List<Long> budgets = new ArrayList<>();
            for (Request request : requests) if (request.budget > 0) budgets.add(request.budget);
            if (budgets.isEmpty()) return 0;
            Collections.sort(budgets); return budgets.get(budgets.size()/2);
        }
        /** Suggested fee per student for the whole course, rounded to 10.000đ. */
        public int price(int sessions) {
            long total = Math.round(budget()*sessions/10000.0)*10000;
            return total == 0 ? 0 : (int) Math.max(50000,Math.min(10000000,total));
        }

        /** How many students wrote the day (1 = Monday … 7 = Sunday). */
        private int count(int day) { int count = 0; for (Request request : requests) if (request.wish.days.contains(day)) count++; return count; }
        /** How many students wrote the part of the day. */
        private int count(String part) { int count = 0; for (Request request : requests) if (request.wish.parts.contains(part)) count++; return count; }
        /** The day most students wrote, if at least two did; 0 otherwise. */
        public int day() {
            int best = 0;
            for (int day = 1; day <= 7; day++) if (count(day) >= 2 && count(day) > count(best)) best = day;
            return best;
        }
        /** The part of the day most students wrote, if at least two did; empty otherwise. */
        public String part() {
            String best = "";
            for (String part : new String[]{MORNING,AFTERNOON,EVENING}) if (count(part) >= 2 && count(part) > count(best)) best = part;
            return best;
        }
        /** Start hour to suggest: the hour students wrote most, or one that fits the usual part of the day; -1 when unknown. */
        public int hour() {
            Map<Integer,Integer> hours = new LinkedHashMap<>(); int best = -1, most = 0;
            for (Request request : requests) if (request.wish.hour >= 0) hours.merge(request.wish.hour,1,Integer::sum);
            for (Map.Entry<Integer,Integer> entry : hours.entrySet()) if (entry.getValue() > most) { best = entry.getKey(); most = entry.getValue(); }
            if (best >= 0) return best;
            switch (part()) { case MORNING: return 8; case AFTERNOON: return 14; case EVENING: return 19; default: return -1; }
        }
        /** "Thứ Bảy (3/3 bạn) · Buổi tối (2/3 bạn)", or empty when the students wrote nothing in common. */
        public String scheduleLabel() {
            List<String> parts = new ArrayList<>();
            if (day() > 0) parts.add(GroupClass.DAYS[day()-1]+" ("+count(day())+"/"+size()+" bạn)");
            if (!part().isEmpty()) parts.add("Buổi "+part()+" ("+count(part())+"/"+size()+" bạn)");
            return String.join(" · ",parts);
        }
        public String title() { return "Nhóm "+subject+("KHAC".equals(level) ? "" : " "+levelLabel()); }
        public String description() {
            String when = (part().isEmpty() ? "" : part()+" ")+(day() > 0 ? GroupClass.DAYS[day()-1].toLowerCase(Locale.ROOT) : "");
            return "Lớp nhóm nhỏ cho các bạn cần học "+subject+("KHAC".equals(level) ? "" : " "+levelLabel())
                    +(when.trim().isEmpty() ? "." : ", lịch dự kiến "+when.trim()+".")+" Học cùng nhau, ôn chắc kiến thức và luyện bài tập.";
        }
    }

    private GroupMatch() {}

    /** Groups of at least two students wanting the same subject and level, largest first; the tutor's own requests are left out. */
    public static List<Group> groups(List<Request> open, String tutor) {
        Map<String,Group> groups = new LinkedHashMap<>();
        Map<String,Set<String>> students = new LinkedHashMap<>();
        for (Request request : open) {
            String subject = subject(request.subject);
            if (subject.isEmpty() || request.studentId.equals(tutor) || !Arrays.asList(LEVELS).contains(request.level)) continue;
            String key = subject+"|"+request.level;
            Group group = groups.computeIfAbsent(key,k -> new Group(subject,request.level));
            if (students.computeIfAbsent(key,k -> new LinkedHashSet<>()).add(request.studentId)) group.requests.add(request);
        }
        List<Group> list = new ArrayList<>();
        for (Group group : groups.values()) if (group.size() >= 2) list.add(group);
        list.sort(Comparator.comparingInt((Group group) -> -group.size())
                .thenComparingInt(group -> Arrays.asList(SUBJECTS).indexOf(group.subject))
                .thenComparingInt(group -> Arrays.asList(LEVELS).indexOf(group.level)));
        return list;
    }

    /** The first of the student's own open requests that the class suits, or null. */
    public static Request suits(GroupClass item, List<Request> mine) {
        Set<String> levels = levels(item);
        for (Request request : mine) {
            String subject = subject(request.subject);
            boolean sameSubject = subject.isEmpty() ? plain(request.subject).equals(plain(item.subject)) : subject.equals(item.subject);
            if (sameSubject && (levels.isEmpty() || levels.contains(request.level))) return request;
        }
        return null;
    }

    /** Levels a class is meant for, read from its level and name; empty means every level. */
    public static Set<String> levels(GroupClass item) {
        Set<String> levels = new LinkedHashSet<>();
        String text = plain(item.level+" "+item.title);
        Matcher grade = Pattern.compile("lop\\s*(\\d{1,2})\\b").matcher(text);
        while (grade.find()) { int value = Integer.parseInt(grade.group(1)); if (value >= 1 && value <= 12) levels.add(value <= 5 ? "TIEU_HOC" : value <= 9 ? "THCS" : "THPT"); }
        if (text.contains("tieu hoc")) levels.add("TIEU_HOC");
        if (text.contains("thcs")) levels.add("THCS");
        if (text.contains("thpt")) levels.add("THPT");
        if (text.contains("luyen thi") || text.contains("on thi")) levels.add("LUYEN_THI");
        return levels;
    }

    /** One of {@link #SUBJECTS} for a subject name such as "Toán học" or "Anh văn", or empty. */
    public static String subject(String name) {
        String value = plain(name);
        if (value.matches("toan( hoc)?")) return "Toán";
        if (value.matches("(tieng anh|anh van|anh ngu|english)")) return "Tiếng Anh";
        if (value.matches("(vat ly|vat li|ly)")) return "Vật lý";
        if (value.matches("(hoa hoc|hoa)")) return "Hóa học";
        return "";
    }

    public static String levelLabel(String code) {
        switch (code) {
            case "TIEU_HOC": return "Tiểu học";
            case "THCS": return "THCS";
            case "THPT": return "THPT";
            case "LUYEN_THI": return "Luyện thi";
            default: return "Khác";
        }
    }

    private static final Pattern TOKEN = Pattern.compile(
            "\\bd([2-8])\\b|(\\d{1,2})\\s*(?:(?:h|g|gio)(?=\\d|\\b)|:)\\s*(?:\\d{2})?|\\b(\\d)\\b|\\b(den|toi)\\b|(-)|\\b(sang|trua|chieu|dem)\\b|(,|/|&|\\bva\\b)");

    /** Reads "Tối thứ 2, 4 từ 19h", "T2 - T6", "cuối tuần" and the like; anything else is left out. */
    public static Wish read(String text) {
        Wish wish = new Wish();
        // "tới" (until) and "tối" (evening) look the same once the accents are gone; "toi" typed without accents is told apart below.
        String value = plain((text == null ? "" : text.toLowerCase(Locale.ROOT)).replace("tới"," đến ").replace("tối"," đêm "));
        value = value.replaceAll("chu nhat|\\bcn\\b"," d8 ").replaceAll("cuoi tuan"," d7 d8 ");
        String[] names = {"hai","ba","tu","nam","sau","bay"};
        for (int i = 0; i < names.length; i++) value = value.replaceAll("\\bthu\\s+"+names[i]+"\\b","thu "+(i+2));
        value = value.replaceAll("\\b(?:thu|t)\\s*([2-7])\\b"," d$1 ");
        List<String[]> tokens = new ArrayList<>();
        Matcher match = TOKEN.matcher(value);
        while (match.find()) {
            if (match.group(1) != null) tokens.add(new String[]{"day",match.group(1)});
            else if (match.group(2) != null) tokens.add(new String[]{"hour",match.group(2)});
            else if (match.group(3) != null) tokens.add(new String[]{"number",match.group(3)});
            else if (match.group(4) != null) tokens.add(new String[]{match.group(4),""});
            else if (match.group(5) != null) tokens.add(new String[]{"den",""});
            else if (match.group(6) != null) tokens.add(new String[]{"part",match.group(6)});
            else tokens.add(new String[]{"and",""});
        }
        int last = 0; boolean range = false, list = false;
        for (int i = 0; i < tokens.size(); i++) {
            String kind = tokens.get(i)[0], word = tokens.get(i)[1];
            int day = "day".equals(kind) || ("number".equals(kind) && list) ? Integer.parseInt(word)-1 : 0;
            if (day >= 1 && day <= 7) {
                if (range && last > 0 && last < day) for (int d = last; d <= day; d++) wish.days.add(d);
                else wish.days.add(day);
                last = day; range = false; list = true;
            } else if ("den".equals(kind)) {
                range = last > 0;
            } else if ("toi".equals(kind)) {
                boolean dayNext = i+1 < tokens.size() && ("day".equals(tokens.get(i+1)[0]) || ("number".equals(tokens.get(i+1)[0]) && list));
                if (last > 0 && dayNext) range = true; else { wish.parts.add(EVENING); list = false; }
            } else if ("part".equals(kind)) {
                wish.parts.add(word.equals("sang") ? MORNING : word.equals("dem") ? EVENING : AFTERNOON); list = false;
            } else if ("hour".equals(kind)) {
                if (wish.hour < 0) wish.hour = Integer.parseInt(word);
                last = 0; list = false;
            } else if (!"and".equals(kind)) {
                list = false;
            }
        }
        if (wish.hour > 23) wish.hour = -1;
        if (wish.hour >= 0) {
            if (wish.parts.contains(EVENING) && wish.hour < 12) wish.hour += 12;
            else if (wish.parts.contains(AFTERNOON) && wish.hour < 7) wish.hour += 12;
            if (wish.parts.isEmpty()) wish.parts.add(wish.hour < 12 ? MORNING : wish.hour < 17 ? AFTERNOON : EVENING);
        }
        return wish;
    }

    static String plain(String value) {
        return Normalizer.normalize(value == null ? "" : value,Normalizer.Form.NFD).replaceAll("\\p{M}","")
                .toLowerCase(Locale.ROOT).replace('đ','d').replaceAll("\\s+"," ").trim();
    }
    private static String money(long amount) { return Tutor.money((int) amount); }
}
