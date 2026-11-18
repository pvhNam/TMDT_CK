package com.example.tmdt;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.HashMap;
import java.util.Map;

/** A private assignment shared by the tutor and approved members of one online class. */
public final class GroupAssignment {
    public static final String OPEN = "OPEN", CLOSED = "CLOSED";
    public final String id, classId, tutorId, title, description, status;
    public final long dueAtMillis, createdAtMillis, updatedAtMillis;
    public final Attachment attachment;

    private GroupAssignment(String id, Map<String, Object> data) {
        this.id = id; classId = text(data.get("class_id")); tutorId = text(data.get("tutor_id"));
        title = text(data.get("title")); description = text(data.get("description")); status = text(data.get("status"));
        dueAtMillis = millis(data.get("due_at")); createdAtMillis = millis(data.get("created_at"));
        updatedAtMillis = millis(data.get("updated_at")); attachment = Attachment.from(data.get("attachment"));
    }
    public boolean isOpen() { return OPEN.equals(status); }
    public boolean overdue() { return dueAtMillis <= System.currentTimeMillis(); }
    public static GroupAssignment from(DocumentSnapshot doc) {
        if (doc == null || !doc.exists() || doc.getData() == null) return null;
        GroupAssignment value = new GroupAssignment(doc.getId(), doc.getData());
        return value.classId.isEmpty() || value.tutorId.isEmpty() || value.title.isEmpty()
                || value.dueAtMillis <= 0 || (!OPEN.equals(value.status) && !CLOSED.equals(value.status)) ? null : value;
    }
    static String text(Object value) { return value instanceof String ? (String) value : ""; }
    static long millis(Object value) { return value instanceof Timestamp ? ((Timestamp) value).toDate().getTime()
            : value instanceof java.util.Date ? ((java.util.Date) value).getTime() : 0; }

    /** A logical class path and private Google Drive file ID; the viewer URL is derived locally. */
    public static final class Attachment {
        public final String path, name, mime, driveFileId;
        public final long size;
        public Attachment(String path, String name, String mime, long size) {
            this(path, name, mime, size, "");
        }
        public Attachment(String path, String name, String mime, long size, String driveFileId) {
            this.path = path; this.name = name; this.mime = mime; this.size = size;
            this.driveFileId = driveFileId == null ? "" : driveFileId;
        }
        public boolean drive() { return driveFileId.matches("[A-Za-z0-9_-]{10,200}"); }
        public String driveUrl() { return drive() ? "https://drive.google.com/file/d/" + driveFileId + "/view" : ""; }
        public boolean image() { return mime.startsWith("image/"); }
        public Map<String, Object> toMap() {
            Map<String, Object> data = new HashMap<>();
            data.put("path", path); data.put("name", name); data.put("mime", mime); data.put("size", size);
            if (!driveFileId.isEmpty()) data.put("drive_file_id", driveFileId); return data;
        }
        public static Attachment from(Object value) {
            if (!(value instanceof Map)) return null;
            Map<?, ?> data = (Map<?, ?>) value;
            String path = text(data.get("path")), name = text(data.get("name")), mime = text(data.get("mime"));
            Object amount = data.get("size"); long size = amount instanceof Number ? ((Number) amount).longValue() : 0;
            String driveId = text(data.get("drive_file_id"));
            if (data.containsKey("drive_file_id") && !driveId.matches("[A-Za-z0-9_-]{10,200}")) return null;
            return path.isEmpty() || name.isEmpty() || mime.isEmpty() || size <= 0 || size > 10L * 1024 * 1024
                    ? null : new Attachment(path, name, mime, size, driveId);
        }
    }
}
