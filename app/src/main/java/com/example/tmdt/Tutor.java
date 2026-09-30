package com.example.tmdt;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Tutors of lessons and group classes: the two sample tutors of the mock-up (ids 0 and 1) and the
 * catalog tutors a student booked from the home screen. A catalog tutor keeps one entry per subject
 * and level, because the lesson title and price depend on them; {@link Store} saves these entries.
 */
public final class Tutor {
    private static final Map<Integer, Tutor> ALL = new LinkedHashMap<>();
    static {
        put(new Tutor(0, "", "Nguyễn Minh Anh", "Toán", "Lớp 10–12", "Toán lớp 12", "4.9", 128, 150000, 60));
        put(new Tutor(1, "", "Trần Hoàng Nam", "Tiếng Anh", "Giao tiếp", "Tiếng Anh giao tiếp", "4.8", 96, 180000, 60));
    }

    /** catalogId is the Firestore tutor_profiles id, empty for the sample tutors. */
    public final int id, students, rate, rateMinutes;
    public final String catalogId, name, subject, level, course, rating;

    private Tutor(int id, String catalogId, String name, String subject, String level, String course, String rating,
                  int students, int rate, int rateMinutes) {
        this.id = id; this.catalogId = catalogId; this.name = name; this.subject = subject; this.level = level;
        this.course = course; this.rating = rating; this.students = students; this.rate = rate; this.rateMinutes = rateMinutes;
    }
    private static void put(Tutor tutor) { ALL.put(tutor.id, tutor); }

    public static Tutor get(int id) { return ALL.get(id); }
    public boolean sample() { return catalogId.isEmpty(); }

    /** Registers a catalog offering (fee per 90-minute lesson) or refreshes the entry already registered for it. */
    public static Tutor fromCatalog(String catalogId, String name, String subject, String level, long price) {
        int id = -1, next = 0;
        for (Tutor tutor : ALL.values()) {
            if (tutor.catalogId.equals(catalogId) && tutor.subject.equals(subject) && tutor.level.equals(level)) id = tutor.id;
            next = Math.max(next, tutor.id+1);
        }
        if (id < 0) id = next;
        Tutor tutor = new Tutor(id, catalogId, name, subject, level, subject+" "+level, "", 0, (int) price, 90);
        put(tutor); return tutor;
    }

    /** Price of a lesson of the given length. */
    public int price(int minutes) { return rate * minutes / rateMinutes; }
    public String rateLabel() { return money(rate)+(rateMinutes == 60 ? " / giờ" : " / "+rateMinutes+" phút"); }
    public String ratingLabel() { return rating.isEmpty() ? "Gia sư đã xác minh" : rating+" ("+students+")"; }

    public static String money(int amount) {
        return String.format(java.util.Locale.forLanguageTag("vi-VN"), "%,dđ", amount);
    }

    static List<JSONObject> catalogJson() throws JSONException {
        List<JSONObject> list = new ArrayList<>();
        for (Tutor tutor : ALL.values()) {
            if (tutor.sample()) continue;
            JSONObject data = new JSONObject(); data.put("id",tutor.id); data.put("catalogId",tutor.catalogId); data.put("name",tutor.name);
            data.put("subject",tutor.subject); data.put("level",tutor.level); data.put("rate",tutor.rate); list.add(data);
        }
        return list;
    }
    static void restore(JSONObject data) throws JSONException {
        int id = data.getInt("id");
        if (id < 2 || data.getString("catalogId").isEmpty()) throw new JSONException("Invalid tutor");
        String subject = data.getString("subject"), level = data.getString("level");
        put(new Tutor(id, data.getString("catalogId"), data.getString("name"), subject, level, subject+" "+level, "", 0, data.getInt("rate"), 90));
    }
}
