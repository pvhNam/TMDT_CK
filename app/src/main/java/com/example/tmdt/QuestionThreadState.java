package com.example.tmdt;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Shared question/comment threads with bounded live replies and isolated, account-scoped drafts. */
public abstract class QuestionThreadState extends ViewModel {
    protected enum Scope {
        CLASS("class.qa.", "group_classes", "questions", "class_id", "question_id", "ten_lop", "trang_thai"),
        TUTOR("tutor.comments.", "tutor_profiles", "comments", "tutor_id", "comment_id", "ho_ten", "trang_thai_xac_minh");

        final String prefix, parentCollection, messageCollection, parentField, replyField, titleField, statusField;
        Scope(String prefix, String parentCollection, String messageCollection, String parentField,
              String replyField, String titleField, String statusField) {
            this.prefix = prefix; this.parentCollection = parentCollection; this.messageCollection = messageCollection;
            this.parentField = parentField; this.replyField = replyField; this.titleField = titleField; this.statusField = statusField;
        }
    }
    private final Scope scope;
    private final String prefix;
    private static final int PAGE_SIZE = 30;
    private final SavedStateHandle saved;
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final MutableLiveData<Integer> changes = new MutableLiveData<>(0);
    private final FirebaseAuth.AuthStateListener authListener = ignored -> bindOwner();
    private final List<GroupClassQuestion> questions = new ArrayList<>(), replies = new ArrayList<>();
    private ListenerRegistration parentListener, accountListener, questionsListener, repliesListener;
    private String owner, parentId = "", title = "", tutorId = "", status = "", replyToId = "";
    private String parentError = "", accountError = "", questionsError = "", replyError = "", actionError = "", notice = "";
    private boolean parentExists, parentLoaded, accountLoaded, accountActive, questionsLoading, replyLoading, busy;
    private boolean parentCached = true, accountCached = true, questionsCached = true, repliesCached = true;
    private boolean moreQuestions, moreReplies;
    private int generation, questionsGeneration, repliesGeneration, questionLimit = PAGE_SIZE, replyLimit = PAGE_SIZE;

    protected QuestionThreadState(SavedStateHandle saved, Scope scope) {
        this.scope = scope;
        this.prefix = scope.prefix;
        this.saved = saved;
        bindOwner();
        auth.addAuthStateListener(authListener);
    }

    public LiveData<Integer> changes() { return changes; }
    public String title() { return title; }
    public String tutorId() { return tutorId; }
    public String status() { return status; }
    public boolean available() { return currentOwner() && parentExists && parentError.isEmpty(); }
    public boolean signedIn() { return currentOwner() && !uid().isEmpty(); }
    public boolean busy() { return currentOwner() && busy; }
    public boolean loading() { return !parentLoaded || questionsLoading; }
    public boolean offline() {
        return parentCached || questionsCached || (signedIn() && accountCached)
                || (!replyToId.isEmpty() && repliesCached);
    }
    public boolean readOnly() {
        return (parentLoaded && (!parentExists || !writableStatus(status)))
                || (signedIn() && accountLoaded && (!accountActive || !accountError.isEmpty()));
    }
    public boolean canPost() {
        return available() && signedIn() && accountLoaded && accountActive && accountError.isEmpty()
                && writableStatus(status) && !busy && !offline() && questionsError.isEmpty()
                && (replyToId.isEmpty() || (findQuestion(replyToId) != null && !replyLoading && replyError.isEmpty()));
    }
    public String error() {
        if (!actionError.isEmpty()) return actionError;
        if (!parentError.isEmpty()) return parentError;
        if (!questionsError.isEmpty()) return questionsError;
        if (!accountError.isEmpty()) return accountError;
        if (signedIn() && accountLoaded && !accountActive) return scope == Scope.TUTOR ? "Tài khoản chưa hoạt động nên chưa thể bình luận hoặc trả lời." : "Tài khoản chưa hoạt động nên chưa thể đặt câu hỏi hoặc trả lời.";
        return "";
    }
    public String notice() { return notice; }
    public List<GroupClassQuestion> questions() { return Collections.unmodifiableList(new ArrayList<>(questions)); }
    public List<GroupClassQuestion> replies() { return Collections.unmodifiableList(new ArrayList<>(replies)); }
    public String replyToId() { return replyToId; }
    public String replyToName() { GroupClassQuestion question = findQuestion(replyToId); return question == null ? "" : question.authorName; }
    public boolean repliesLoading() { return replyLoading; }
    public String repliesError() { return replyError; }
    public boolean hasMore() { return moreQuestions; }
    public boolean hasMoreReplies() { return moreReplies; }
    public String draft() { return currentOwner() && !uid().isEmpty() ? savedText(draftKey(parentId, replyToId)) : ""; }
    public void draft(String value) {
        if (busy || !signedIn() || parentId.isEmpty()) return;
        String text = value == null ? "" : value;
        String key = draftKey(parentId, replyToId);
        if (!text.equals(savedText(key))) {
            saved.set(prefix + key, text);
            saved.remove(prefix + nonceKey(parentId, replyToId));
            actionError = ""; notice = "";
        }
    }

    private String uid() { return auth.getCurrentUser() == null ? "" : auth.getCurrentUser().getUid(); }
    private boolean currentOwner() { return owner != null && owner.equals(uid()); }
    private void changed() { Integer value = changes.getValue(); changes.setValue(value == null ? 1 : value + 1); }
    private String savedText(String key) { Object value = saved.get(prefix + key); return value instanceof String ? (String) value : ""; }
    private int savedLimit(String id) { Integer value = saved.get(prefix + "limit." + id); return value == null ? PAGE_SIZE : Math.max(PAGE_SIZE, value); }
    private static String draftKey(String id, String question) { return "draft." + id + "." + question; }
    private static String nonceKey(String id, String question) { return "nonce." + id + "." + question; }
    private static boolean validId(String id) { return id != null && !id.isEmpty() && !id.contains("/"); }
    private boolean writableStatus(String value) {
        return scope == Scope.TUTOR ? "VERIFIED".equals(value) : "OPEN".equals(value) || "RUNNING".equals(value);
    }
    private boolean publicParent(DocumentSnapshot document) {
        return document != null && document.exists()
                && (scope == Scope.CLASS || "VERIFIED".equals(document.get(scope.statusField)));
    }
    private String parentUnavailable() {
        return scope == Scope.TUTOR ? "Hồ sơ gia sư không còn công khai hoặc chưa được duyệt." : "Lớp học không còn khả dụng.";
    }
    private static String text(Object value) { return value instanceof String ? (String) value : ""; }
    private DocumentReference parentRef() { return db.collection(scope.parentCollection).document(parentId); }
    private GroupClassQuestion findQuestion(String id) {
        for (GroupClassQuestion question : questions) if (question.id.equals(id)) return question;
        return null;
    }

    private void bindOwner() {
        String next = uid();
        if (next.equals(owner)) return;
        String keepParent = parentId;
        stopAll();
        if (!next.equals(savedText("owner"))) {
            for (String key : new ArrayList<>(saved.keys())) if (key.startsWith(prefix)) saved.remove(key);
            saved.set(prefix + "owner", next);
        }
        owner = next; parentId = ""; busy = false; actionError = ""; notice = "";
        clearParent();
        if (validId(keepParent)) startParent(keepParent);
        else changed();
    }

    public void watch(String id) {
        bindOwner();
        if (!validId(id) || (busy && id.equals(parentId))) return;
        if (id.equals(parentId) && parentListener != null && parentError.isEmpty() && questionsError.isEmpty()) return;
        // Inline profiles can be left while a send is pending. Retarget immediately;
        // the old transaction keeps its own path/nonce and its late callback is ignored.
        stopAll(); busy = false; clearParent(); startParent(id);
    }

    public void retry() {
        bindOwner();
        if (busy || !validId(parentId)) return;
        String id = parentId;
        stopAll(); clearParent(); startParent(id);
    }

    private void startParent(String id) {
        parentId = id;
        questionLimit = savedLimit(id);
        replyToId = savedText("reply." + id);
        replyLimit = PAGE_SIZE;
        final int token = generation;
        final String actor = uid();
        parentLoaded = false; parentCached = true; accountCached = !actor.isEmpty();
        accountLoaded = actor.isEmpty(); accountActive = false;
        questionsLoading = true; questionsCached = true;
        parentError = ""; accountError = ""; questionsError = ""; replyError = "";
        actionError = ""; notice = "";
        if (!actor.isEmpty()) {
            accountListener = db.collection("users").document(actor).addSnapshotListener(MetadataChanges.INCLUDE, (doc, failure) -> {
                if (token != generation || !actor.equals(uid())) return;
                accountLoaded = true;
                accountCached = doc != null && doc.getMetadata().isFromCache();
                accountActive = failure == null && doc != null && doc.exists() && "ACTIVE".equals(doc.get("trang_thai"));
                accountError = failure == null ? "" : message(failure);
                changed();
            });
        }
        parentListener = parentRef().addSnapshotListener(MetadataChanges.INCLUDE, (doc, failure) -> {
            if (token != generation || !actor.equals(uid())) return;
            parentCached = doc != null && doc.getMetadata().isFromCache();
            if (failure == null && doc != null && !doc.exists() && parentCached) {
                // An empty local cache does not prove the parent was removed from the server.
                parentLoaded = false; changed(); return;
            }
            parentLoaded = true;
            if (failure != null || !publicParent(doc)) {
                parentExists = false; title = ""; tutorId = ""; status = "";
                parentError = failure == null ? parentUnavailable() : message(failure);
                stopQuestions(); questions.clear(); stopReplies(); replies.clear();
                questionsLoading = false; clearReplyTarget();
            } else {
                parentExists = true; title = text(doc.get(scope.titleField));
                tutorId = scope == Scope.TUTOR ? parentId : text(doc.get("tutor_id"));
                status = text(doc.get(scope.statusField)); parentError = "";
                if (questionsListener == null) listenQuestions();
            }
            changed();
        });
        changed();
    }

    private void listenQuestions() {
        stopQuestions();
        final int token = generation, query = questionsGeneration;
        final String actor = uid();
        questionsLoading = true;
        questionsListener = parentRef().collection(scope.messageCollection).orderBy("created_at", Query.Direction.DESCENDING)
                .limit(questionLimit).addSnapshotListener(MetadataChanges.INCLUDE, (snapshot, failure) -> {
                    if (token != generation || query != questionsGeneration || !actor.equals(uid())) return;
                    questionsLoading = false;
                    if (failure != null || snapshot == null) {
                        questionsError = message(failure); questions.clear(); moreQuestions = false;
                        stopReplies(); replies.clear(); clearReplyTarget();
                    } else {
                        questionsError = ""; questionsCached = snapshot.getMetadata().isFromCache(); questions.clear();
                        for (DocumentSnapshot doc : snapshot) {
                            GroupClassQuestion question = GroupClassQuestion.from(doc);
                            if (question != null) questions.add(question);
                        }
                        moreQuestions = snapshot.size() >= questionLimit;
                        if (!replyToId.isEmpty()) {
                            if (findQuestion(replyToId) == null && !questionsCached) { stopReplies(); replies.clear(); clearReplyTarget(); }
                            else if (findQuestion(replyToId) != null && repliesListener == null) listenReplies();
                        }
                    }
                    changed();
                });
    }

    public void loadMore() {
        if (busy || questionsLoading || !moreQuestions || !available()) return;
        questionLimit += PAGE_SIZE;
        saved.set(prefix + "limit." + parentId, questionLimit);
        listenQuestions(); changed();
    }

    public void replyTo(String id) {
        bindOwner();
        if (busy || !available() || !validId(id) || findQuestion(id) == null) return;
        if (id.equals(replyToId) && repliesListener != null && replyError.isEmpty()) return;
        stopReplies(); replies.clear(); replyToId = id;
        saved.set(prefix + "reply." + parentId, id);
        replyLimit = PAGE_SIZE; actionError = ""; notice = ""; listenReplies(); changed();
    }

    public void cancelReply() {
        if (busy) return;
        stopReplies(); replies.clear(); clearReplyTarget(); actionError = ""; notice = ""; changed();
    }

    private void clearReplyTarget() { replyToId = ""; saved.remove(prefix + "reply." + parentId); }
    private void listenReplies() {
        stopReplies();
        final int token = generation, query = repliesGeneration;
        final String actor = uid(), selected = replyToId;
        if (!validId(selected)) return;
        replyLoading = true; repliesCached = true; replyError = "";
        repliesListener = parentRef().collection(scope.messageCollection).document(selected).collection("replies")
                .orderBy("created_at", Query.Direction.DESCENDING).limit(replyLimit)
                .addSnapshotListener(MetadataChanges.INCLUDE, (snapshot, failure) -> {
                    if (token != generation || query != repliesGeneration || !actor.equals(uid()) || !selected.equals(replyToId)) return;
                    replyLoading = false; replies.clear();
                    if (failure != null || snapshot == null) { replyError = message(failure); moreReplies = false; }
                    else {
                        replyError = ""; repliesCached = snapshot.getMetadata().isFromCache();
                        for (DocumentSnapshot doc : snapshot) {
                            GroupClassQuestion reply = GroupClassQuestion.from(doc);
                            if (reply != null) replies.add(reply);
                        }
                        replies.sort(Comparator.comparingLong((GroupClassQuestion reply) -> reply.createdAtMillis).thenComparing(reply -> reply.id));
                        moreReplies = snapshot.size() >= replyLimit;
                    }
                    changed();
                });
    }

    public void loadMoreReplies() {
        if (busy || replyLoading || !moreReplies || replyToId.isEmpty() || !available()) return;
        replyLimit += PAGE_SIZE; listenReplies(); changed();
    }

    public void submit() {
        bindOwner();
        if (busy || !validId(parentId)) return;
        final String actor = uid(), id = parentId, target = replyToId, content = draft().trim();
        if (actor.isEmpty()) { actionError = scope == Scope.TUTOR ? "Vui lòng đăng nhập để bình luận hoặc trả lời." : "Vui lòng đăng nhập để đặt câu hỏi hoặc trả lời."; changed(); return; }
        if (content.isEmpty() || content.length() > 1000) { actionError = "Nhập nội dung từ 1 đến 1.000 ký tự."; changed(); return; }
        if (!canPost()) {
            actionError = readOnly() ? (scope == Scope.TUTOR ? "Hồ sơ hoặc tài khoản hiện chỉ cho phép xem bình luận." : "Lớp hoặc tài khoản hiện chỉ cho phép xem hỏi đáp.")
                    : offline() ? "Chưa kết nối được máy chủ. Bản nháp vẫn được giữ; hãy thử lại."
                    : (scope == Scope.TUTOR ? "Đang tải quyền gửi hoặc nội dung bình luận. Vui lòng đợi rồi thử lại." : "Đang tải quyền gửi hoặc nội dung câu hỏi. Vui lòng đợi rồi thử lại.");
            changed(); return;
        }
        String nonce = savedText(nonceKey(id, target));
        if (nonce.isEmpty()) { nonce = actor + "_" + UUID.randomUUID(); saved.set(prefix + nonceKey(id, target), nonce); }
        final String messageId = nonce;
        final int token = generation;
        busy = true; actionError = ""; notice = ""; changed();
        DocumentReference parent = db.collection(scope.parentCollection).document(id);
        DocumentReference question = target.isEmpty() ? null : parent.collection(scope.messageCollection).document(target);
        CollectionReference collection = question == null ? parent.collection(scope.messageCollection) : question.collection("replies");
        DocumentReference message = collection.document(messageId);
        db.runTransaction(tx -> {
            DocumentSnapshot existing = tx.get(message);
            if (existing.exists()) {
                if (!actor.equals(existing.get("author_id")) || !content.equals(existing.get("content")))
                    throw new QuestionFailure("Nội dung đã thay đổi. Vui lòng nhập lại rồi gửi.");
                return false;
            }
            DocumentSnapshot user = tx.get(db.collection("users").document(actor));
            DocumentSnapshot parentSnapshot = tx.get(parent);
            DocumentSnapshot root = question == null ? null : tx.get(question);
            if (!user.exists() || !"ACTIVE".equals(user.get("trang_thai"))) throw new QuestionFailure(scope == Scope.TUTOR ? "Tài khoản chưa hoạt động nên chưa thể gửi bình luận." : "Tài khoản chưa hoạt động nên chưa thể gửi hỏi đáp.");
            if (!publicParent(parentSnapshot) || !writableStatus(text(parentSnapshot.get(scope.statusField))))
                throw new QuestionFailure(scope == Scope.TUTOR ? parentUnavailable() : "Lớp đã kết thúc, bị hủy hoặc không còn nhận hỏi đáp mới.");
            if (question != null && (root == null || !root.exists())) throw new QuestionFailure(scope == Scope.TUTOR ? "Bình luận này không còn khả dụng. Bản nháp vẫn được giữ." : "Câu hỏi này không còn khả dụng. Bản nháp vẫn được giữ.");
            String name = text(user.get("ho_ten"));
            if (name.trim().isEmpty() || name.length() > 100) throw new QuestionFailure(scope == Scope.TUTOR ? "Hãy cập nhật tên tài khoản trước khi gửi bình luận." : "Hãy cập nhật tên tài khoản trước khi gửi hỏi đáp.");
            Map<String, Object> data = new HashMap<>();
            data.put(scope.parentField, id); data.put("author_id", actor); data.put("author_name", name);
            data.put("content", content); data.put("created_at", FieldValue.serverTimestamp());
            if (question != null) data.put(scope.replyField, target);
            tx.set(message, data); return true;
        }).addOnCompleteListener(task -> {
            if (token != generation || !actor.equals(uid())) return;
            busy = false;
            if (task.isSuccessful()) {
                saved.remove(prefix + draftKey(id, target)); saved.remove(prefix + nonceKey(id, target));
                notice = target.isEmpty() ? (scope == Scope.TUTOR ? "Đã gửi bình luận." : "Đã gửi câu hỏi.") : "Đã gửi câu trả lời.";
            } else actionError = message(task.getException());
            changed();
        });
    }

    private void stopQuestions() { questionsGeneration++; if (questionsListener != null) questionsListener.remove(); questionsListener = null; questionsLoading = false; }
    private void stopReplies() { repliesGeneration++; if (repliesListener != null) repliesListener.remove(); repliesListener = null; replyLoading = false; replyError = ""; moreReplies = false; }
    private void stopAll() {
        generation++;
        if (parentListener != null) parentListener.remove(); if (accountListener != null) accountListener.remove();
        parentListener = null; accountListener = null; stopQuestions(); stopReplies();
    }
    private void clearParent() {
        questions.clear(); replies.clear(); title = ""; tutorId = ""; status = ""; replyToId = "";
        parentExists = false; parentLoaded = false; accountLoaded = false; accountActive = false;
        parentCached = true; accountCached = true; questionsCached = true; repliesCached = true;
        moreQuestions = false; moreReplies = false; parentError = ""; accountError = ""; questionsError = "";
    }
    private String message(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof QuestionFailure) return cause.getMessage();
            if (cause instanceof FirebaseFirestoreException) {
                FirebaseFirestoreException.Code code = ((FirebaseFirestoreException) cause).getCode();
                if (code == FirebaseFirestoreException.Code.PERMISSION_DENIED) return scope == Scope.TUTOR ? "Không có quyền gửi hoặc tải bình luận. Kiểm tra tài khoản, hồ sơ gia sư và Firestore Rules rồi thử lại." : "Không có quyền gửi hoặc tải hỏi đáp. Kiểm tra tài khoản, trạng thái lớp và Firestore Rules rồi thử lại.";
                if (code == FirebaseFirestoreException.Code.UNAVAILABLE || code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED)
                    return "Chưa kết nối được máy chủ. Bản nháp vẫn được giữ; hãy thử lại.";
            }
            if (cause.getCause() == cause) break;
        }
        return scope == Scope.TUTOR ? "Chưa hoàn tất thao tác bình luận. Kiểm tra kết nối rồi thử lại; bản nháp vẫn được giữ." : "Chưa hoàn tất thao tác hỏi đáp. Kiểm tra kết nối rồi thử lại; bản nháp vẫn được giữ.";
    }
    private static final class QuestionFailure extends RuntimeException { QuestionFailure(String value) { super(value); } }
    @Override protected void onCleared() { auth.removeAuthStateListener(authListener); stopAll(); }
}
