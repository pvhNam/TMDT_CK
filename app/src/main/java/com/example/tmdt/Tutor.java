package com.example.tmdt;

/** Local display data for the two tutors in the supplied mockup. */
final class Tutor {
    static final Tutor[] ALL = {
            new Tutor(0, "Nguyễn Minh Anh", "Toán", "Lớp 10–12", "4.9", 128, 150000),
            new Tutor(1, "Trần Hoàng Nam", "Tiếng Anh", "Giao tiếp", "4.8", 96, 180000)
    };
    final int id, students, rate;
    final String name, subject, level, rating;

    private Tutor(int id, String name, String subject, String level, String rating, int students, int rate) {
        this.id = id;
        this.name = name;
        this.subject = subject;
        this.level = level;
        this.rating = rating;
        this.students = students;
        this.rate = rate;
    }

    static String money(int amount) {
        return String.format(java.util.Locale.forLanguageTag("vi-VN"), "%,dđ", amount);
    }
}
