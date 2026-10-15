package com.example.tmdt;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/** UC11: a student's published need for a tutor. Money is VND per lesson. */
public final class TutorRequest {
    public static final String[] LEVELS = {"TIEU_HOC", "THCS", "THPT", "LUYEN_THI", "KHAC"};
    public final String title, subjectId, subjectName, level, schedule, description;
    public final long budget;

    public TutorRequest(String title, String subjectId, String subjectName, String level,
                        String budget, String schedule, String description) {
        this.title = title.trim(); this.subjectId = subjectId.trim();
        this.subjectName = subjectName.trim(); this.level = level;
        this.schedule = schedule.trim(); this.description = description.trim();
        long parsed;
        try { parsed = Long.parseLong(budget.trim()); } catch (NumberFormatException e) { parsed = 0; }
        this.budget = parsed;
    }

    /** Returns the field that needs attention, or an empty string. */
    public String invalidField() {
        if (title.isEmpty() || title.length() > 120) return "title";
        if (subjectId.isEmpty() || subjectId.length() > 150 || subjectName.isEmpty() || subjectName.length() > 100) return "subject";
        if (!Arrays.asList(LEVELS).contains(level)) return "level";
        if (budget < 1 || budget > 10000000) return "budget";
        if (schedule.isEmpty() || schedule.length() > 300) return "schedule";
        if (description.isEmpty() || description.length() > 2000) return "description";
        return "";
    }

    Map<String, Object> fields(String owner, String studentName) {
        Map<String, Object> data = new HashMap<>();
        data.put("student_id", owner); data.put("ten_hoc_vien", studentName);
        data.put("tieu_de", title); data.put("subject_id", subjectId); data.put("mon_hoc", subjectName);
        data.put("cap_hoc", level); data.put("ngan_sach", budget);
        data.put("lich_hoc", schedule); data.put("mo_ta", description); data.put("trang_thai", "OPEN");
        return data;
    }
}
