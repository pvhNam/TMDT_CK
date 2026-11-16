package com.example.tmdt;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Source;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * UC26 – Đánh giá hai chiều. reviews/{contractId__authorId}: mỗi bên đánh giá bên kia một lần cho mỗi hợp đồng đã chốt,
 * sau khi buổi học đầu tiên đã diễn ra. Đánh giá gia sư công khai; đánh giá học viên chỉ hai bên và quản trị viên xem.
 */
public final class ReviewState extends MemberState {
    // Màn 10: danh sách đánh giá của một người.
    private final List<Review> reviews = new ArrayList<>();
    private ListenerRegistration listener;
    private String targetKey;
    private boolean loading;
    private String loadError = "";
    // Màn 09/46: đánh giá của chính mình cho một hợp đồng (null = chưa đánh giá).
    private String checkedContract;
    private Review mine;
    private boolean checking;

    public void watch(String targetId, String role) {
        String key = targetId + "|" + role;
        if (key.equals(targetKey) && listener != null) return;
        stopWatch(); targetKey = key; loading = true; notifyUi();
        listener = db.collection("reviews").whereEqualTo("target_id", targetId).whereEqualTo("target_role", role)
                .addSnapshotListener((snapshot, error) -> {
                    if (!key.equals(targetKey)) return;
                    loading = false;
                    if (error != null || snapshot == null) { loadError = Docs.message(error); listener = null; notifyUi(); return; }
                    reviews.clear();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) reviews.add(Review.from(doc));
                    Collections.sort(reviews, (a, b) -> Long.compare(b.createdAt, a.createdAt));
                    loadError = ""; notifyUi();
                });
    }
    public void stopWatch() { if (listener != null) listener.remove(); listener = null; targetKey = null; reviews.clear(); loadError = ""; }
    @Override protected void onCleared() { stopWatch(); }

    public boolean loading() { return loading; }
    public String loadError() { return loadError; }
    public List<Review> reviews() { return Collections.unmodifiableList(reviews); }
    public double average() {
        if (reviews.isEmpty()) return 0;
        double sum = 0; for (Review r : reviews) sum += r.rating; return sum / reviews.size();
    }
    public int count(int stars) { int n = 0; for (Review r : reviews) if (r.rating == stars) n++; return n; }

    /** Kiểm tra người dùng đã đánh giá hợp đồng này chưa. */
    public void checkMine(String contractId) {
        String me = uid();
        if (me == null || contractId.equals(checkedContract)) return;
        checkedContract = contractId; mine = null; checking = true; notifyUi();
        db.collection("reviews").document(Review.idFor(contractId, me)).get(Source.DEFAULT).addOnCompleteListener(task -> {
            if (!contractId.equals(checkedContract)) return;
            checking = false;
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) mine = Review.from(task.getResult());
            notifyUi();
        });
    }
    public boolean checking() { return checking; }
    public Review mine() { return mine; }

    public void submit(Contract contract, String authorName, int rating, List<String> tags, String comment) {
        final String me = uid();
        if (busy || me == null || !contract.member(me) || rating < 1 || rating > 5) return;
        String target = contract.other(me);
        boolean targetTutor = target.equals(contract.tutorId);
        Map<String, Object> data = new HashMap<>();
        data.put("contract_id", contract.id); data.put("author_id", me); data.put("author_name", authorName);
        data.put("target_id", target); data.put("target_name", contract.otherName(me));
        data.put("target_role", targetTutor ? Review.TUTOR : Review.STUDENT);
        data.put("rating", rating); data.put("tags", new ArrayList<>(tags)); data.put("comment", comment.trim());
        data.put("subject", contract.subject); data.put("created_at", FieldValue.serverTimestamp());
        busy = true; notifyUi();
        final String id = Review.idFor(contract.id, me);
        db.collection("reviews").document(id).set(data).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) { failed(task.getException()); return; }
            Review review = new Review(); review.id = id; review.contractId = contract.id; review.authorId = me;
            review.rating = rating; review.tags.addAll(tags); review.comment = comment.trim(); review.targetId = target;
            review.createdAt = System.currentTimeMillis();
            mine = review; checkedContract = contract.id;
            done("Cảm ơn bạn! Đánh giá đã được gửi.");
        });
    }
}
