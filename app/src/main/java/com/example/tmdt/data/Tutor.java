package com.example.tmdt.data;

/** Local display data for the two tutors in the supplied mockup. */
public final class Tutor {
    public static final Tutor[] ALL = {
            new Tutor(0, "Nguyễn Minh Anh", "Toán", "Lớp 10–12", "4.9", 128, 150000),
            new Tutor(1, "Trần Hoàng Nam", "Tiếng Anh", "Giao tiếp", "4.8", 96, 180000)
    };
    public final int id, students, rate;
    public final String name, subject, level, rating;

    private Tutor(int id, String name, String subject, String level, String rating, int students, int rate) {
        this.id = id;
        this.name = name;
        this.subject = subject;
        this.level = level;
        this.rating = rating;
        this.students = students;
        this.rate = rate;
    }

    public static String money(int amount) {
        return String.format(java.util.Locale.forLanguageTag("vi-VN"), "%,dđ", amount);
    }
}
