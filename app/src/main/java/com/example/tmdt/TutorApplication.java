package com.example.tmdt;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.Map;

/** Read-only application for the owner of a tutor request. */
public final class TutorApplication {
    public final String id, tutorId, tutorName, introduction, status;
    public final long createdAtMillis;

    private TutorApplication(String id, Map<String, Object> data) {
        this.id = id;
        tutorId = text(data.get("tutor_id"));
        String name = text(data.get("ten_gia_su"));
        tutorName = name.isEmpty() ? "Gia sư" : name;
        introduction = text(data.get("gioi_thieu"));
        status = text(data.get("trang_thai"));
        Object created = data.get("created_at");
        createdAtMillis = created instanceof Timestamp ? ((Timestamp) created).toDate().getTime() : 0;
    }

    public static TutorApplication from(DocumentSnapshot document) {
        if (document == null || !document.exists() || document.getData() == null) return null;
        return new TutorApplication(document.getId(), document.getData());
    }

    private static String text(Object value) { return value instanceof String ? (String) value : ""; }
}