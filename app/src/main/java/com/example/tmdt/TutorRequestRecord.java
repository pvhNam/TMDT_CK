package com.example.tmdt;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.Map;

/** A persisted UC11/UC12 post. Invalid optional server fields use safe display defaults. */
public final class TutorRequestRecord {
    public final String id, title, subjectId, subjectName, level, schedule, description, status;
    public final long budget, createdAtMillis;
    public final Timestamp updatedAt;

    private TutorRequestRecord(String id, Map<String, Object> data) {
        this.id = id;
        title = text(data.get("tieu_de"));
        subjectId = text(data.get("subject_id"));
        subjectName = text(data.get("mon_hoc"));
        level = text(data.get("cap_hoc"));
        schedule = text(data.get("lich_hoc"));
        description = text(data.get("mo_ta"));
        status = text(data.get("trang_thai"));
        Object amount = data.get("ngan_sach");
        budget = amount instanceof Number ? Math.max(0, ((Number) amount).longValue()) : 0;
        Object updated = data.get("updated_at");
        updatedAt = updated instanceof Timestamp ? (Timestamp) updated : null;
        Object created = data.get("created_at");
        createdAtMillis = created instanceof Timestamp ? ((Timestamp) created).toDate().getTime() : 0;
    }

    public static TutorRequestRecord from(DocumentSnapshot document) {
        if (document == null || !document.exists() || document.getData() == null) return null;
        return new TutorRequestRecord(document.getId(), document.getData());
    }

    public boolean isOpen() { return "OPEN".equals(status); }
    public boolean isClosed() { return "CLOSED".equals(status); }

    public TutorRequest value() {
        return new TutorRequest(title, subjectId, subjectName, level, Long.toString(budget), schedule, description);
    }

    private static String text(Object value) { return value instanceof String ? (String) value : ""; }
}