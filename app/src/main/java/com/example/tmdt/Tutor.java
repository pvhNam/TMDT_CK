package com.example.tmdt;

import java.util.HashMap;
import java.util.Map;

/**
 * One subject a catalog tutor teaches, chosen on the home screen before booking. id is the tutor's
 * uid (the tutor_profiles document id); the price is for a 90-minute lesson, as in tutor_subjects.
 */
public final class Tutor {
    private static final Map<String, Tutor> ALL = new HashMap<>();
    private static final int RATE_MINUTES = 90;

    public final String key, id, name, subject, level, course;
    public final int rate;

    private Tutor(String id, String name, String subject, String level, int rate) {
        this.key = id+"|"+subject+"|"+level; this.id = id; this.name = name; this.subject = subject; this.level = level;
        this.course = subject+" "+level; this.rate = rate;
    }

    public static Tutor get(String key) { return key == null ? null : ALL.get(key); }

    public static Tutor fromCatalog(String id, String name, String subject, String level, long price) {
        Tutor tutor = new Tutor(id, name, subject, level, (int) price);
        ALL.put(tutor.key, tutor); return tutor;
    }

    public int price(int minutes) { return rate * minutes / RATE_MINUTES; }
    public String rateLabel() { return money(rate)+" / "+RATE_MINUTES+" phút"; }
    public String ratingLabel() { return "Gia sư đã xác minh"; }

    public static String money(int amount) {
        return String.format(java.util.Locale.forLanguageTag("vi-VN"), "%,dđ", amount);
    }
}
