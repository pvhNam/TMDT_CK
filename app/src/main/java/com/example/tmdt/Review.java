package com.example.tmdt;

import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

/** Đánh giá hai chiều (UC26): học viên đánh giá gia sư (màn 09) và gia sư đánh giá học viên (màn 46). */
public final class Review {
    public static final String TUTOR = "TUTOR", STUDENT = "STUDENT";
    public static final int MAX_COMMENT = 500;

    public String id = "", contractId = "", authorId = "", authorName = "", targetId = "", targetName = "", targetRole = "", comment = "", subject = "";
    public int rating;
    public final List<String> tags = new ArrayList<>();
    public long createdAt;

    /** Mỗi người chỉ đánh giá một lần cho mỗi hợp đồng. */
    public static String idFor(String contractId, String authorId) { return contractId + "__" + authorId; }

    public static String ratingLabel(int stars) {
        switch (stars) {
            case 1: return "Rất không hài lòng";
            case 2: return "Chưa hài lòng";
            case 3: return "Bình thường";
            case 4: return "Hài lòng";
            case 5: return "Rất hài lòng";
            default: return "Chọn số sao";
        }
    }

    static Review from(DocumentSnapshot doc) {
        Review r = new Review();
        r.id = doc.getId(); r.contractId = Docs.text(doc, "contract_id"); r.authorId = Docs.text(doc, "author_id");
        r.authorName = Docs.text(doc, "author_name"); r.targetId = Docs.text(doc, "target_id"); r.targetName = Docs.text(doc, "target_name");
        r.targetRole = Docs.text(doc, "target_role"); r.comment = Docs.text(doc, "comment"); r.subject = Docs.text(doc, "subject");
        r.rating = (int) Docs.number(doc, "rating"); r.tags.addAll(Docs.strings(doc.get("tags"))); r.createdAt = Docs.millis(doc, "created_at");
        return r;
    }
}
