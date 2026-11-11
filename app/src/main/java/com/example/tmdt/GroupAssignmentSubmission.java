package com.example.tmdt;

import com.google.firebase.firestore.DocumentSnapshot;
import java.util.Map;

/** A student's own answer; only the class tutor may write its review fields. */
public final class GroupAssignmentSubmission {
    public static final String SUBMITTED = "SUBMITTED", REVIEWED = "REVIEWED";
    public final String studentId, studentName, content, link, status, feedback;
    public final Double score;
    public final long submittedAtMillis, updatedAtMillis, reviewedAtMillis;
    public final GroupAssignment.Attachment attachment;
    private GroupAssignmentSubmission(Map<String, Object> data) {
        studentId = GroupAssignment.text(data.get("student_id")); studentName = GroupAssignment.text(data.get("student_name"));
        content = GroupAssignment.text(data.get("content")); link = GroupAssignment.text(data.get("link"));
        status = GroupAssignment.text(data.get("status")); feedback = GroupAssignment.text(data.get("feedback"));
        Object grade = data.get("score"); score = grade instanceof Number ? ((Number) grade).doubleValue() : null;
        submittedAtMillis = GroupAssignment.millis(data.get("submitted_at")); updatedAtMillis = GroupAssignment.millis(data.get("updated_at"));
        reviewedAtMillis = GroupAssignment.millis(data.get("reviewed_at")); attachment = GroupAssignment.Attachment.from(data.get("attachment"));
    }
    public boolean reviewed() { return REVIEWED.equals(status); }
    public static GroupAssignmentSubmission from(DocumentSnapshot doc) {
        if (doc == null || !doc.exists() || doc.getData() == null) return null;
        GroupAssignmentSubmission value = new GroupAssignmentSubmission(doc.getData());
        return !doc.getId().equals(value.studentId) || (!SUBMITTED.equals(value.status) && !REVIEWED.equals(value.status)) ? null : value;
    }
}
