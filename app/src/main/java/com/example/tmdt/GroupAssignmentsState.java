package com.example.tmdt;

import android.app.Application;
import android.net.Uri;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import com.google.firebase.Timestamp;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.ClearTokenRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.util.*;

/** Account-scoped live classroom assignments, with server-checked publication of private files. */
public final class GroupAssignmentsState extends AndroidViewModel {
    private static final String PREFIX = "group.assignments.";
    private final SavedStateHandle saved;
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final MutableLiveData<Integer> changes = new MutableLiveData<>(0);
    private final List<GroupAssignment> assignments = new ArrayList<>();
    private final List<GroupAssignmentSubmission> submissions = new ArrayList<>();
    private final FirebaseAuth.AuthStateListener authListener = ignored -> bindOwner();
    private ListenerRegistration classListener, accountListener, assignmentsListener, submissionsListener;
    private AssignmentFileTransfer.Operation upload;
    private String owner = "", classId = "", title = "", status = "", tutorId = "", selectedId = "";
    private String classError = "", accountError = "", assignmentsError = "", submissionError = "", actionError = "", notice = "";
    private boolean classLoaded, classExists, accountLoaded, accountActive, member, assignmentsLoading, submissionLoading, busy;
    private boolean classCached = true, accountCached = true, assignmentsCached = true, submissionsCached = true;
    private int generation, childGeneration, actionGeneration, progress = -1;
    private GroupAssignmentSubmission mine;
    private boolean contactPublished, contactSaving;

    public GroupAssignmentsState(Application app, SavedStateHandle saved) {
        super(app); this.saved = saved; bindOwner(); auth.addAuthStateListener(authListener);
    }
    public LiveData<Integer> changes() { return changes; }
    public String classTitle() { return title; }
    public String classStatus() { return status; }
    public String selectedId() { return selectedId; }
    public GroupAssignment selected() { return available() ? find(selectedId) : null; }
    public boolean teacher() { return currentOwner() && owner.equals(tutorId); }
    public boolean available() { return currentOwner() && !classId.isEmpty() && classLoaded && classExists
            && accountLoaded && accountActive && (teacher() || member) && classError.isEmpty() && accountError.isEmpty(); }
    public boolean loading() { return !classLoaded || !accountLoaded || assignmentsLoading; }
    public boolean busy() { return busy; }
    public boolean offline() { return classCached || accountCached || assignmentsCached || (!selectedId.isEmpty() && submissionsCached); }
    public String error() {
        if (!actionError.isEmpty()) return actionError;
        if (!accountError.isEmpty()) return accountError;
        if (!classError.isEmpty()) return classError;
        if (accountLoaded && !accountActive) return "Tài khoản chưa hoạt động hoặc bạn đã đăng xuất.";
        if (classLoaded && (!classExists || (!teacher() && !member))) return "Bạn không còn quyền truy cập bài tập của lớp trực tuyến này.";
        return assignmentsError;
    }
    public String notice() { return notice; }
    public int progress() { return progress; }
    public boolean canCreate() { return available() && teacher() && writable(status) && !busy && !offline() && assignmentsError.isEmpty(); }
    public boolean canSubmit() { GroupAssignment task = selected(); return available() && !teacher() && writable(status) && task != null
            && task.isOpen() && task.dueAtMillis > System.currentTimeMillis() && (mine == null || !mine.reviewed())
            && !busy && !offline() && !submissionLoading && submissionError.isEmpty() && assignmentsError.isEmpty(); }
    public boolean canReview() { return available() && teacher() && (writable(status) || "ENDED".equals(status)) && selected() != null
            && !busy && !offline() && !submissionLoading && submissionError.isEmpty() && assignmentsError.isEmpty(); }
    public List<GroupAssignment> assignments() { return available() ? Collections.unmodifiableList(new ArrayList<>(assignments)) : Collections.emptyList(); }
    public GroupAssignmentSubmission mine() { return available() && !teacher() ? mine : null; }
    public List<GroupAssignmentSubmission> submissions() { return available() && teacher()
            ? Collections.unmodifiableList(new ArrayList<>(submissions)) : Collections.emptyList(); }
    public boolean submissionsLoading() { return submissionLoading; }
    public String submissionsError() { return submissionError; }
    public String draft(String key) {
        String value = saved.get(draftKey(key));
        if (value != null) return value;
        if (key.equals("answer.content") && mine != null) return mine.content;
        if (key.equals("answer.link") && mine != null) return mine.link;
        return "";
    }
    public void draft(String key, String value) {
        String normalized = value == null ? "" : value;
        String previous = saved.get(draftKey(key)); saved.set(draftKey(key), normalized);
        if (key.startsWith("new.") && !normalized.equals(previous == null ? "" : previous)) saved.remove(baseKey() + "nonce");
        actionError = ""; notice = "";
    }
    private String baseKey() { return PREFIX + owner + "." + classId + "."; }
    private String draftKey(String key) { return baseKey() + (key.startsWith("answer.") ? selectedId + "." : "") + key; }
    private boolean currentOwner() { return !owner.isEmpty() && owner.equals(uid()); }
    private String uid() { return auth.getCurrentUser() == null ? "" : auth.getCurrentUser().getUid(); }
    private static boolean writable(String value) { return "OPEN".equals(value) || "RUNNING".equals(value); }
    private static boolean validId(String value) { return value != null && !value.isEmpty() && !value.contains("/"); }
    private void changed() { changes.setValue(changes.getValue() == null ? 1 : changes.getValue() + 1); }
    private GroupAssignment find(String id) { for (GroupAssignment task : assignments) if (task.id.equals(id)) return task; return null; }
    private void bindOwner() {
        String next = uid(); if (owner.equals(next) && accountListener != null) return;
        String target = classId; stopAll(); generation++; invalidateAction();
        for (String key : new HashSet<>(saved.keys())) if (key.startsWith(PREFIX) && !key.startsWith(PREFIX + next + ".")) saved.remove(key);
        owner = next; classId = ""; resetClass();
        if (!target.isEmpty()) watch(target); else changed();
    }
    public void watch(String id) {
        if (busy || !validId(id)) return;
        if (id.equals(classId) && classListener != null && accountListener != null) return;
        stopAll(); generation++; classId = id; resetClass();
        String restored = saved.get(baseKey() + "selected"); selectedId = validId(restored) ? restored : "";
        if (owner.isEmpty()) { classLoaded = accountLoaded = true; changed(); return; }
        listenParent(generation, owner, id);
    }
    private void listenParent(int token, String actor, String target) {
        accountListener = db.collection("users").document(actor).addSnapshotListener(MetadataChanges.INCLUDE, (doc, failure) -> {
            if (!same(token, actor, target)) return;
            accountLoaded = true; accountCached = doc == null || doc.getMetadata().isFromCache();
            accountActive = failure == null && doc != null && doc.exists() && "ACTIVE".equals(doc.getString("trang_thai"));
            accountError = failure == null ? "" : message(failure);
            if (doc != null && !doc.exists() && accountCached && failure == null) accountLoaded = false;
            accessChanged();
        });
        classListener = classRef(target).addSnapshotListener(MetadataChanges.INCLUDE, (doc, failure) -> {
            if (!same(token, actor, target)) return;
            classLoaded = true; classCached = doc == null || doc.getMetadata().isFromCache();
            classExists = failure == null && doc != null && doc.exists() && "Trực tuyến".equals(doc.getString("hinh_thuc"));
            title = classExists ? GroupAssignment.text(doc.get("ten_lop")) : "";
            status = classExists ? GroupAssignment.text(doc.get("trang_thai")) : "";
            tutorId = classExists ? GroupAssignment.text(doc.get("tutor_id")) : "";
            Object roster = classExists ? doc.get("thanh_vien") : null;
            member = roster instanceof List && ((List<?>) roster).contains(actor);
            classError = failure == null ? "" : message(failure);
            if (doc != null && !doc.exists() && classCached && failure == null) classLoaded = false;
            accessChanged();
        }); changed();
    }
    private void accessChanged() {
        if (!available()) {
            stopAssignments(); invalidateAction(); assignments.clear(); stopSubmissions();
        } else {
            if (assignmentsListener == null) listenAssignments();
            publishDriveContact();
        }
        changed();
    }
    private boolean same(int token, String actor, String target) { return token == generation && actor.equals(owner)
            && actor.equals(uid()) && target.equals(classId); }
    private void listenAssignments() {
        int token = generation; String actor = owner, target = classId;
        assignmentsLoading = true; assignmentsError = "";
        assignmentsListener = classRef(target).collection("assignments").addSnapshotListener(MetadataChanges.INCLUDE, (snapshot, failure) -> {
            if (!same(token, actor, target) || !available()) return;
            assignmentsLoading = false; assignmentsCached = snapshot == null || snapshot.getMetadata().isFromCache();
            assignmentsError = failure == null ? "" : message(failure); assignments.clear();
            if (failure == null && snapshot != null) for (DocumentSnapshot doc : snapshot.getDocuments()) {
                GroupAssignment value = GroupAssignment.from(doc);
                if (value != null && value.classId.equals(target) && value.tutorId.equals(tutorId)) assignments.add(value);
            }
            assignments.sort((a, b) -> { int order = Long.compare(b.createdAtMillis, a.createdAtMillis); return order == 0 ? a.id.compareTo(b.id) : order; });
            if (find(selectedId) == null) stopSubmissions();
            else if (submissionsListener == null) listenSubmissions();
            changed();
        });
    }
    public void select(String id) {
        if (busy || !available() || find(id) == null || id.equals(selectedId)) return;
        stopSubmissions(); selectedId = id; saved.set(baseKey() + "selected", id); actionError = notice = ""; listenSubmissions(); changed();
    }
    public void unselect() {
        if (busy) return; stopSubmissions(); selectedId = ""; saved.remove(baseKey() + "selected"); actionError = notice = ""; changed();
    }
    private void listenSubmissions() {
        stopSubmissions(); if (!available() || find(selectedId) == null) return;
        int token = generation, child = childGeneration; String actor = owner, target = classId, assignment = selectedId;
        boolean asTeacher = teacher(); submissionLoading = true; submissionsCached = true;
        CollectionReference collection = assignmentRef(target, assignment).collection("submissions");
        if (asTeacher) submissionsListener = collection.addSnapshotListener(MetadataChanges.INCLUDE, (snapshot, failure) -> {
            if (!sameChild(token, child, actor, target, assignment) || !teacher()) return;
            submissionLoading = false; submissionsCached = snapshot == null || snapshot.getMetadata().isFromCache();
            submissionError = failure == null ? "" : message(failure); submissions.clear();
            if (failure == null && snapshot != null) for (DocumentSnapshot doc : snapshot.getDocuments()) {
                GroupAssignmentSubmission value = GroupAssignmentSubmission.from(doc); if (value != null) submissions.add(value);
            }
            submissions.sort((a, b) -> Long.compare(b.updatedAtMillis, a.updatedAtMillis)); changed();
        }); else submissionsListener = collection.document(actor).addSnapshotListener(MetadataChanges.INCLUDE, (doc, failure) -> {
            if (!sameChild(token, child, actor, target, assignment) || teacher()) return;
            submissionLoading = false; submissionsCached = doc == null || doc.getMetadata().isFromCache();
            submissionError = failure == null ? "" : message(failure); mine = failure == null ? GroupAssignmentSubmission.from(doc) : null; changed();
        });
    }
    private boolean sameChild(int token, int child, String actor, String target, String task) {
        return same(token, actor, target) && child == childGeneration && task.equals(selectedId) && available();
    }
    public void retry() { if (busy || classId.isEmpty()) return; String target = classId; stopAll(); watch(target); }
    private void resetClass() {
        contactPublished = contactSaving = false;
        title = status = tutorId = ""; classError = accountError = assignmentsError = submissionError = actionError = notice = "";
        classLoaded = classExists = accountLoaded = accountActive = member = assignmentsLoading = false;
        classCached = accountCached = assignmentsCached = submissionsCached = true; assignments.clear(); stopSubmissions(); selectedId = "";
    }
    private void stopAssignments() { if (assignmentsListener != null) assignmentsListener.remove(); assignmentsListener = null; assignmentsLoading = false; }
    private void stopSubmissions() {
        childGeneration++; if (submissionsListener != null) submissionsListener.remove(); submissionsListener = null;
        mine = null; submissions.clear(); submissionLoading = false; submissionsCached = true; submissionError = "";
    }
    private void stopAll() {
        if (classListener != null) classListener.remove(); if (accountListener != null) accountListener.remove();
        classListener = accountListener = null; stopAssignments(); stopSubmissions();
    }
    private DocumentReference classRef(String target) { return db.collection("group_classes").document(target); }
    private DocumentReference assignmentRef(String target, String task) { return classRef(target).collection("assignments").document(task); }
    private int beginAction() { busy = true; progress = -1; actionError = notice = ""; int token = ++actionGeneration; changed(); return token; }
    private void invalidateAction() { actionGeneration++; if (upload != null) upload.cancel(); upload = null; busy = false; progress = -1; }
    private boolean currentAction(int action, int token, String actor, String target) { return action == actionGeneration && same(token, actor, target); }
    private interface FileReady { void run(@Nullable GroupAssignment.Attachment file); }
    private void prepareFile(Uri file, String prefix, String driveToken, int action, int token,
                             String actor, String target, FileReady ready) {
        if (file == null) { ready.run(null); return; }
        String expectedEmail = currentEmail();
        if (driveToken == null || driveToken.isEmpty() || expectedEmail.isEmpty()) {
            finishFailure(action, token, actor, target, new AssignmentFailure("Cấp quyền Google Drive bằng tài khoản trùng email đang dùng trong ứng dụng.")); return;
        }
        String[] parts = prefix.split("/");
        boolean asTeacher = parts.length > 3 && "task".equals(parts[3]);
        progress = 0; changed();
        driveAudience(asTeacher, true, parts[2], action, token, actor, target, readers -> {
            if (!currentAction(action, token, actor, target)) return;
            upload = AssignmentFileTransfer.upload(getApplication(), file, prefix, driveToken, expectedEmail, readers,
                    new AssignmentFileTransfer.Callback() {
                public void progress(int value) { if (currentAction(action, token, actor, target)) { progress = value; changed(); } }
                public void uploaded(GroupAssignment.Attachment value) {
                    if (!currentAction(action, token, actor, target)) return;
                    upload = null; progress = -1; ready.run(value);
                }
                public void failed(Exception failure) {
                    if (currentAction(action, token, actor, target)) {
                        upload = null; clearRejectedDriveToken(driveToken, failure);
                    }
                    finishFailure(action, token, actor, target, failure);
                }
            });
        });
    }
    private String currentEmail() {
        com.google.firebase.auth.FirebaseUser user = auth.getCurrentUser();
        return user != null && user.isEmailVerified() && user.getEmail() != null ? user.getEmail().trim() : "";
    }
    private void publishDriveContact() {
        if (contactPublished || contactSaving || !available() || classCached || accountCached) return;
        String email = currentEmail(); if (email.isEmpty()) return;
        int token = generation; String actor = owner, target = classId;
        contactSaving = true;
        Map<String, Object> data = new HashMap<>(); data.put("user_id", actor); data.put("email", email);
        data.put("updated_at", FieldValue.serverTimestamp());
        classRef(target).collection("drive_contacts").document(actor).set(data).addOnCompleteListener(result -> {
            if (!same(token, actor, target)) return;
            contactSaving = false; contactPublished = result.isSuccessful();
        });
    }
    private interface AudienceReady { void ready(List<String> readers); }
    private static final class Audience {
        final List<String> members = new ArrayList<>(), readers = new ArrayList<>();
    }
    private void driveAudience(boolean asTeacher, boolean writing, String taskId, int action, int token,
                               String actor, String target, AudienceReady ready) {
        db.runTransaction(transaction -> {
            DocumentSnapshot user = transaction.get(db.collection("users").document(actor));
            DocumentSnapshot group = transaction.get(classRef(target));
            requireAccess(user, group, actor, asTeacher, writing);
            Audience audience = new Audience();
            if (asTeacher) {
                Object roster = group.get("thanh_vien");
                if (roster instanceof List) for (Object uid : (List<?>) roster) if (uid instanceof String) audience.members.add((String) uid);
            } else {
                DocumentReference taskRef = assignmentRef(target, taskId);
                DocumentSnapshot task = transaction.get(taskRef);
                requireTask(task, target, group.getString("tutor_id"));
                if (!GroupAssignment.OPEN.equals(task.getString("status")) || GroupAssignment.millis(task.get("due_at")) <= System.currentTimeMillis())
                    throw new AssignmentFailure("Bài tập đã đóng hoặc đã quá hạn nộp.");
                DocumentSnapshot answer = transaction.get(taskRef.collection("submissions").document(actor));
                if (answer.exists() && "REVIEWED".equals(answer.getString("status"))) throw new AssignmentFailure("Bài đã được chấm, không thể sửa.");
                DocumentSnapshot teacher = transaction.get(classRef(target).collection("drive_contacts").document(group.getString("tutor_id")));
                String email = teacher.exists() ? teacher.getString("email") : null;
                if (email == null || email.isEmpty()) throw new AssignmentFailure("Gia sư cần mở mục Bài tập lớp online trước để nhận quyền xem tệp.");
                audience.readers.add(email);
            }
            return audience;
        }).addOnCompleteListener(result -> {
            if (!currentAction(action, token, actor, target)) return;
            if (!result.isSuccessful()) { finishFailure(action, token, actor, target, result.getException()); return; }
            Audience audience = result.getResult();
            if (!asTeacher) { ready.ready(audience.readers); return; }
            classRef(target).collection("drive_contacts").get(Source.SERVER).addOnCompleteListener(contacts -> {
                if (!currentAction(action, token, actor, target)) return;
                if (!contacts.isSuccessful()) { finishFailure(action, token, actor, target, contacts.getException()); return; }
                for (DocumentSnapshot doc : contacts.getResult().getDocuments()) {
                    String email = doc.getString("email");
                    if (audience.members.contains(doc.getId()) && doc.getId().equals(doc.getString("user_id")) && email != null && !email.isEmpty()) audience.readers.add(email);
                }
                ready.ready(audience.readers);
            });
        });
    }
    public boolean canSyncDrive() {
        return available() && teacher() && !busy && !offline() && !assignmentsLoading
                && assignmentsError.isEmpty() && assignments.stream().anyMatch(a -> a.attachment != null && a.attachment.drive());
    }
    public void syncDrivePermissions(String driveToken) {
        if (!canSyncDrive()) return;
        String email = currentEmail();
        if (email.isEmpty() || driveToken == null || driveToken.isEmpty()) { reject("Cấp quyền Google Drive để cập nhật người xem tài liệu."); return; }
        String actor = owner, target = classId; int token = generation, action = beginAction();
        List<GroupAssignment.Attachment> files = new ArrayList<>();
        for (GroupAssignment a : assignments) if (a.attachment != null && a.attachment.drive()) files.add(a.attachment);
        driveAudience(true, false, "", action, token, actor, target, readers -> {
            upload = AssignmentFileTransfer.syncReaders(driveToken, email, files, readers, new AssignmentFileTransfer.SyncCallback() {
                public void done() {
                    if (!currentAction(action, token, actor, target)) return;
                    upload = null; busy = false; notice = "Đã cập nhật quyền xem tài liệu cho học viên đã mở mục Bài tập của lớp."; changed();
                }
                public void failed(Exception failure) {
                    if (currentAction(action, token, actor, target)) {
                        upload = null; clearRejectedDriveToken(driveToken, failure);
                    }
                    finishFailure(action, token, actor, target, failure);
                }
            });
        });
    }
    private void clearRejectedDriveToken(String token, Exception failure) {
        if (token == null || token.isEmpty() || !(failure instanceof AssignmentDriveApi.DriveFailure)
                || !((AssignmentDriveApi.DriveFailure) failure).requiresReauthorization()) return;
        try {
            Identity.getAuthorizationClient(getApplication()).clearToken(ClearTokenRequest.builder().setToken(token).build())
                    .addOnFailureListener(ignored -> { });
        } catch (RuntimeException ignored) { /* Preserve the original Drive error if Play services is unavailable. */ }
    }
    private void finishFailure(int action, int token, String actor, String target, Exception failure) {
        if (!currentAction(action, token, actor, target)) return; busy = false; progress = -1; actionError = message(failure); changed();
    }
    private void reject(String text) { actionError = text; changed(); }
    public void create(String rawTitle, String rawDescription, long dueAtMillis, @Nullable Uri file, String driveToken) {
        if (busy) return; String name = clean(rawTitle), description = clean(rawDescription);
        if (!canCreate()) { reject("Chưa thể giao bài tập. Kiểm tra kết nối và trạng thái lớp."); return; }
        if (name.isEmpty() || name.length() > 120 || description.isEmpty() || description.length() > 6000) {
            reject("Nhập tiêu đề tối đa 120 ký tự và mô tả tối đa 6000 ký tự."); return;
        }
        if (dueAtMillis <= System.currentTimeMillis()) { reject("Hạn nộp bài phải ở trong tương lai."); return; }
        final String actor = owner, target = classId; final int token = generation;
        String nonce = saved.get(baseKey() + "nonce");
        if (!validId(nonce)) { nonce = UUID.randomUUID().toString(); saved.set(baseKey() + "nonce", nonce); }
        final String id = nonce; final int action = beginAction();
        prepareFile(file, "group_assignments/" + target + "/" + id + "/task/" + actor + "/", driveToken, action, token, actor, target, attached -> {
            DocumentReference reference = assignmentRef(target, id);
            db.runTransaction(transaction -> {
                DocumentSnapshot user = transaction.get(db.collection("users").document(actor));
                DocumentSnapshot group = transaction.get(classRef(target));
                DocumentSnapshot existing = transaction.get(reference);
                requireAccess(user, group, actor, true, true);
                if (existing.exists()) {
                    if (!actor.equals(existing.getString("tutor_id")) || !target.equals(existing.getString("class_id"))) throw new AssignmentFailure("Không thể dùng mã bài tập này.");
                    return false;
                }
                if (dueAtMillis <= System.currentTimeMillis()) throw new AssignmentFailure("Hạn nộp bài đã qua. Hãy chọn lại hạn nộp.");
                Map<String, Object> data = new HashMap<>();
                data.put("class_id", target); data.put("tutor_id", actor); data.put("title", name); data.put("description", description);
                data.put("due_at", new Timestamp(new Date(dueAtMillis))); data.put("status", GroupAssignment.OPEN);
                data.put("attachment", attached == null ? null : attached.toMap());
                data.put("created_at", FieldValue.serverTimestamp()); data.put("updated_at", FieldValue.serverTimestamp());
                transaction.set(reference, data); return true;
            }).addOnCompleteListener(result -> {
                if (!result.isSuccessful()) { finishFailure(action, token, actor, target, result.getException()); return; }
                if (!currentAction(action, token, actor, target)) return;
                busy = false; progress = -1; clearDrafts(false); saved.remove(baseKey() + "nonce");
                stopSubmissions(); selectedId = id; saved.set(baseKey() + "selected", id);
                if (find(id) != null) listenSubmissions(); notice = "Đã giao bài tập cho lớp."; changed();
            });
        });
    }
    public void setClosed(String assignmentId, boolean closed) {
        if (busy) return;
        if (!canCreate() || find(assignmentId) == null) { reject("Chưa thể thay đổi trạng thái bài tập."); return; }
        String actor = owner, target = classId; int token = generation, action = beginAction();
        DocumentReference reference = assignmentRef(target, assignmentId);
        db.runTransaction(transaction -> {
            DocumentSnapshot user = transaction.get(db.collection("users").document(actor));
            DocumentSnapshot group = transaction.get(classRef(target)); DocumentSnapshot task = transaction.get(reference);
            requireAccess(user, group, actor, true, true); requireTask(task, target, GroupAssignment.text(group.get("tutor_id")));
            if (!closed && GroupAssignment.millis(task.get("due_at")) <= System.currentTimeMillis()) throw new AssignmentFailure("Bài đã quá hạn nộp, không thể mở lại.");
            transaction.update(reference, "status", closed ? GroupAssignment.CLOSED : GroupAssignment.OPEN, "updated_at", FieldValue.serverTimestamp());
            return null;
        }).addOnCompleteListener(result -> {
            if (!result.isSuccessful()) { finishFailure(action, token, actor, target, result.getException()); return; }
            if (currentAction(action, token, actor, target)) { busy = false; notice = closed ? "Đã đóng bài tập." : "Đã mở lại bài tập."; changed(); }
        });
    }
    public void submit(String rawContent, String rawLink, @Nullable Uri file, boolean removeFile, String driveToken) {
        if (busy) return; String content = clean(rawContent), link = clean(rawLink);
        if (!canSubmit()) { reject("Chưa thể nộp bài. Bài có thể đã đóng, quá hạn hoặc đã được chấm."); return; }
        if (content.length() > 6000 || link.length() > 1000 || !validLink(link)) {
            reject("Nội dung tối đa 6000 ký tự; đường dẫn tối đa 1000 ký tự và bắt đầu bằng https://."); return;
        }
        GroupAssignmentSubmission previous = mine;
        if (content.isEmpty() && link.isEmpty() && file == null && (removeFile || previous == null || previous.attachment == null)) {
            reject("Thêm nội dung, đường dẫn hoặc tệp bài làm trước khi nộp."); return;
        }
        String actor = owner, target = classId, assignment = selectedId; int token = generation, action = beginAction();
        prepareFile(file, "group_assignments/" + target + "/" + assignment + "/submissions/" + actor + "/", driveToken, action, token, actor, target, attached -> {
            DocumentReference reference = assignmentRef(target, assignment), submission = reference.collection("submissions").document(actor);
            db.runTransaction(transaction -> {
                DocumentSnapshot user = transaction.get(db.collection("users").document(actor));
                DocumentSnapshot group = transaction.get(classRef(target)); DocumentSnapshot task = transaction.get(reference);
                DocumentSnapshot existing = transaction.get(submission);
                requireAccess(user, group, actor, false, true); requireTask(task, target, GroupAssignment.text(group.get("tutor_id")));
                if (!GroupAssignment.OPEN.equals(task.getString("status")) || GroupAssignment.millis(task.get("due_at")) <= System.currentTimeMillis())
                    throw new AssignmentFailure("Bài tập đã đóng hoặc đã quá hạn nộp.");
                if (existing.exists() && GroupAssignmentSubmission.REVIEWED.equals(existing.getString("status"))) throw new AssignmentFailure("Bài đã được chấm, không thể sửa.");
                GroupAssignment.Attachment old = GroupAssignment.Attachment.from(existing.get("attachment"));
                GroupAssignment.Attachment next = attached != null ? attached : removeFile ? null : old;
                if (content.isEmpty() && link.isEmpty() && next == null) throw new AssignmentFailure("Bài nộp cần có nội dung, đường dẫn hoặc tệp.");
                String studentName = GroupAssignment.text(user.get("ho_ten"));
                if (studentName.trim().isEmpty() || studentName.length() > 100) throw new AssignmentFailure("Hãy cập nhật họ tên hợp lệ trong hồ sơ trước khi nộp bài.");
                Map<String, Object> data = new HashMap<>(); data.put("student_id", actor); data.put("student_name", studentName);
                data.put("content", content); data.put("link", link); data.put("attachment", next == null ? null : next.toMap());
                data.put("status", GroupAssignmentSubmission.SUBMITTED); data.put("score", null); data.put("feedback", ""); data.put("reviewed_at", null);
                data.put("submitted_at", existing.exists() ? existing.get("submitted_at") : FieldValue.serverTimestamp());
                data.put("updated_at", FieldValue.serverTimestamp()); transaction.set(submission, data);
                return old != null && (next == null || !old.path.equals(next.path)) ? old.path : "";
            }).addOnCompleteListener(result -> {
                if (!result.isSuccessful()) {
                    finishFailure(action, token, actor, target, result.getException()); return;
                }
                if (!currentAction(action, token, actor, target)) return;
                busy = false; progress = -1; clearDrafts(true);
                // The transaction callback and own-answer listener may arrive in either order.
                // Retain the committed values so a late snapshot cannot hydrate an older answer.
                saved.set(draftKey("answer.content"), content); saved.set(draftKey("answer.link"), link);
                saved.set(draftKey("answer.initialized"), "1"); notice = "Đã nộp bài tập."; changed();
            });
        });
    }
    public void review(String studentId, @Nullable Double score, String rawFeedback) {
        if (busy) return; String feedback = clean(rawFeedback);
        if (!canReview() || !validId(studentId)) { reject("Chưa thể chấm bài tập này."); return; }
        if (score != null && (score.isNaN() || score.isInfinite() || score < 0 || score > 10)) { reject("Điểm phải nằm trong khoảng 0 đến 10."); return; }
        if (feedback.length() > 2000 || score == null && feedback.isEmpty()) { reject("Nhập điểm hoặc nhận xét tối đa 2000 ký tự."); return; }
        String actor = owner, target = classId, assignment = selectedId; int token = generation, action = beginAction();
        DocumentReference reference = assignmentRef(target, assignment), submission = reference.collection("submissions").document(studentId);
        db.runTransaction(transaction -> {
            DocumentSnapshot user = transaction.get(db.collection("users").document(actor));
            DocumentSnapshot group = transaction.get(classRef(target)); DocumentSnapshot task = transaction.get(reference);
            DocumentSnapshot answer = transaction.get(submission);
            requireAccess(user, group, actor, true, false); requireTask(task, target, actor);
            if (!writable(group.getString("trang_thai")) && !"ENDED".equals(group.getString("trang_thai"))) throw new AssignmentFailure("Trạng thái lớp không cho phép chấm bài.");
            if (!answer.exists() || !studentId.equals(answer.getString("student_id"))) throw new AssignmentFailure("Bài nộp không còn tồn tại.");
            Map<String, Object> data = new HashMap<>(); data.put("status", GroupAssignmentSubmission.REVIEWED);
            data.put("score", score); data.put("feedback", feedback); data.put("reviewed_at", FieldValue.serverTimestamp());
            data.put("updated_at", FieldValue.serverTimestamp()); transaction.update(submission, data); return null;
        }).addOnCompleteListener(result -> {
            if (!result.isSuccessful()) { finishFailure(action, token, actor, target, result.getException()); return; }
            if (currentAction(action, token, actor, target)) { busy = false; notice = "Đã lưu điểm và nhận xét."; changed(); }
        });
    }
    private static void requireAccess(DocumentSnapshot user, DocumentSnapshot group, String actor, boolean tutor, boolean writing) {
        if (!user.exists() || !"ACTIVE".equals(user.getString("trang_thai"))) throw new AssignmentFailure("Tài khoản chưa hoạt động.");
        if (!group.exists() || !"Trực tuyến".equals(group.getString("hinh_thuc"))) throw new AssignmentFailure("Lớp trực tuyến không còn tồn tại.");
        boolean teacher = actor.equals(group.getString("tutor_id")); Object roster = group.get("thanh_vien");
        if (tutor ? !teacher : teacher || !(roster instanceof List) || !((List<?>) roster).contains(actor))
            throw new AssignmentFailure("Bạn không có quyền thực hiện thao tác trong lớp này.");
        if (writing && !writable(group.getString("trang_thai"))) throw new AssignmentFailure("Lớp đã kết thúc hoặc đã hủy.");
    }
    private static void requireTask(DocumentSnapshot task, String target, String tutor) {
        if (!task.exists() || !target.equals(task.getString("class_id")) || !tutor.equals(task.getString("tutor_id")))
            throw new AssignmentFailure("Bài tập không còn tồn tại hoặc không thuộc lớp này.");
    }
    private void clearDrafts(boolean answer) {
        String prefix = baseKey() + (answer ? selectedId + ".answer." : "new.");
        for (String key : new HashSet<>(saved.keys())) if (key.startsWith(prefix)) saved.remove(key);
    }
    private static String clean(String text) { return text == null ? "" : text.trim(); }
    private static boolean validLink(String text) {
        if (text.isEmpty()) return true;
        try { Uri uri = Uri.parse(text); return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null
                && !uri.getHost().isEmpty() && uri.getUserInfo() == null && !text.matches(".*\\s+.*"); }
        catch (RuntimeException failure) { return false; }
    }
    private static String message(Exception failure) {
        Throwable cause = failure;
        while (cause != null) { if (cause instanceof AssignmentFailure) return cause.getMessage(); cause = cause.getCause(); }
        if (failure instanceof AssignmentDriveApi.DriveFailure) return failure.getMessage();
        if (failure instanceof java.io.IOException || failure instanceof SecurityException) return "Không đọc được tệp đã chọn. Hãy chọn lại tệp tối đa 10 MB.";
        if (failure instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException.Code code = ((FirebaseFirestoreException) failure).getCode();
            if (code == FirebaseFirestoreException.Code.PERMISSION_DENIED || code == FirebaseFirestoreException.Code.UNAUTHENTICATED)
                return "Không thể lưu. Quyền truy cập, hạn nộp hoặc trạng thái bài đã thay đổi. Hãy tải lại.";
            if (code == FirebaseFirestoreException.Code.UNAVAILABLE || code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED)
                return "Chưa kết nối được máy chủ. Kiểm tra mạng rồi thử lại.";
        }
        return "Chưa thể lưu thay đổi. Hãy tải lại rồi thử lại.";
    }
    private static final class AssignmentFailure extends RuntimeException { AssignmentFailure(String message) { super(message); } }
    @Override protected void onCleared() { generation++; stopAll(); invalidateAction(); auth.removeAuthStateListener(authListener); super.onCleared(); }
}
