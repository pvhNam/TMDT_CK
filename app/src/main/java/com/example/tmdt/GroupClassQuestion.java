package com.example.tmdt;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.Map;

/** Shared public question/comment or reply. It contains discussion text, never a rating. */
public final class GroupClassQuestion {
    public final String id, authorId, authorName, content, questionId;
    public final long createdAtMillis;

    private GroupClassQuestion(DocumentSnapshot document) {
        Map<String, Object> data = document.getData();
        id = document.getId();
        authorId = text(data.get("author_id"));
        authorName = text(data.get("author_name"));
        content = text(data.get("content"));
        String parent = text(data.get("question_id"));
        questionId = parent.isEmpty() ? text(data.get("comment_id")) : parent;
        Object timestamp = document.get("created_at", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE);
        createdAtMillis = timestamp instanceof Timestamp ? ((Timestamp) timestamp).toDate().getTime() : 0;
    }

    public static GroupClassQuestion from(DocumentSnapshot document) {
        if (document == null || !document.exists() || document.getData() == null) return null;
        Map<String, Object> data = document.getData();
        if (text(data.get("author_id")).isEmpty() || text(data.get("content")).trim().isEmpty()) return null;
        return new GroupClassQuestion(document);
    }

    private static String text(Object value) { return value instanceof String ? (String) value : ""; }
}
