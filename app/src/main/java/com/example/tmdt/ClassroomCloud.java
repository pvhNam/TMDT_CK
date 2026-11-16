package com.example.tmdt;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Firestore side of {@link Store}: keeps it filled from live queries and saves its changes. */
public final class ClassroomCloud extends ViewModel implements Store.Remote {
    private static final String LESSONS = "lessons", CLASSES = "group_classes", REGISTRATIONS = "class_registrations", REQUESTS = "tutor_requests";
    public final Store store = new Store(this);
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final MutableLiveData<Integer> changes = new MutableLiveData<>(0);
    private final Map<String,Map<String,Map<String,Object>>> results = new HashMap<>();
    private final List<ListenerRegistration> personal = new ArrayList<>();
    private final ListenerRegistration classListener;
    private String user, error = "";
    private boolean watchingRequests;

    public ClassroomCloud() { classListener = listen("classes",db.collection(CLASSES),null); }

    public LiveData<Integer> changes() { return changes; }
    public String takeError() { String message = error; error = ""; return message; }

    public void user(String uid) {
        if (uid.equals(user)) return;
        for (ListenerRegistration listener : personal) listener.remove();
        personal.clear(); results.keySet().removeIf(name -> !name.equals("classes"));
        user = uid; store.user = uid; watchingRequests = false; store.requestsError = "";
        if (!uid.isEmpty()) {
            personal.add(listen("booked",db.collection(LESSONS).whereEqualTo("student_id",uid),uid));
            personal.add(listen("taught",db.collection(LESSONS).whereEqualTo("tutor_id",uid),uid));
            personal.add(listen("sent",db.collection(REGISTRATIONS).whereEqualTo("student_id",uid),uid));
            personal.add(listen("received",db.collection(REGISTRATIONS).whereEqualTo("tutor_id",uid),uid));
            personal.add(listen("asked",db.collection(REQUESTS).whereEqualTo("student_id",uid),uid));
        }
        rebuild();
    }
    /** Starts reading every open tutor request, which only a verified tutor may do, for the group suggestions; retry starts it again after a failure. */
    public void watchRequests(boolean retry) {
        if ((watchingRequests && !(retry && !store.requestsError.isEmpty())) || user == null || user.isEmpty()) return;
        watchingRequests = true; store.requestsError = "";
        personal.add(listen("open",db.collection(REQUESTS).whereEqualTo("trang_thai","OPEN"),user));
    }

    private ListenerRegistration listen(String name, Query query, String owner) {
        return query.addSnapshotListener((snapshot,failure) -> {
            if (owner != null && !owner.equals(user)) return;
            // Tutor requests belong to another feature: a failure only empties the suggestions instead of interrupting the user.
            if (failure != null && name.equals("open")) { store.requestsError = message(failure); changes.setValue(changes.getValue()+1); return; }
            if (failure != null && name.equals("asked")) return;
            if (failure != null) { fail("Không tải được lịch học và lớp nhóm. "+message(failure)); return; }
            Map<String,Map<String,Object>> docs = new LinkedHashMap<>();
            for (DocumentSnapshot doc : snapshot) docs.put(doc.getId(),doc.getData());
            if(Objects.equals(results.get(name),docs))return;
            results.put(name,docs); rebuild();
        });
    }
    private Map<String,Map<String,Object>> docs(String name) { return results.getOrDefault(name,new LinkedHashMap<>()); }

    private void rebuild() {
        Map<String,Lesson> lessons = new LinkedHashMap<>();
        for (String name : new String[]{"booked","taught"})
            for (Map.Entry<String,Map<String,Object>> doc : docs(name).entrySet()) {
                Lesson lesson = Lesson.from(doc.getKey(),doc.getValue());
                if (lesson != null) lessons.put(lesson.id,lesson);
            }
        Map<String,GroupClass> classes = new LinkedHashMap<>();
        for (Map.Entry<String,Map<String,Object>> doc : docs("classes").entrySet()) {
            GroupClass item = GroupClass.from(doc.getKey(),doc.getValue());
            if (item != null) classes.put(item.id,item);
        }
        for (String name : new String[]{"sent","received"})
            for (Map<String,Object> data : docs(name).values()) {
                GroupClass.Registration registration = GroupClass.Registration.from(data);
                GroupClass item = registration == null ? null : classes.get(registration.classId);
                if (item == null) continue;
                item.registrations.removeIf(old -> old.studentId.equals(registration.studentId));
                item.registrations.add(registration);
            }
        store.lessons.clear(); store.lessons.addAll(lessons.values());
        store.classes.clear(); store.classes.addAll(classes.values());
        for (String name : new String[]{"asked","open"}) {
            List<GroupMatch.Request> requests = name.equals("asked") ? store.myRequests : store.openRequests;
            requests.clear();
            for (Map.Entry<String,Map<String,Object>> doc : docs(name).entrySet()) {
                GroupMatch.Request request = GroupMatch.Request.from(doc.getKey(),doc.getValue());
                if (request != null) requests.add(request);
            }
        }
        store.matchesReady = results.containsKey("asked") && results.containsKey("classes"); store.openRequestsLoaded = results.containsKey("open");
        changes.setValue(changes.getValue()+1);
    }

    @Override public String newId(String collection) { return db.collection(collection).document().getId(); }
    @Override public void save(Lesson lesson) {
        WriteBatch batch = db.batch();
        put(batch,db.collection(LESSONS).document(lesson.id),lesson.toMap(),docs("booked").containsKey(lesson.id)||docs("taught").containsKey(lesson.id));
        commit(batch);
    }
    @Override public void save(GroupClass item, List<GroupClass.Registration> registrations) {
        WriteBatch batch = db.batch();
        put(batch,db.collection(CLASSES).document(item.id),item.toMap(),docs("classes").containsKey(item.id));
        for (GroupClass.Registration registration : registrations) put(batch,registration);
        commit(batch);
    }
    @Override public void save(GroupClass.Registration registration) {
        WriteBatch batch = db.batch(); put(batch,registration); commit(batch);
    }
    @Override public void answer(GroupClass item, String student, String answer) {
        String owner = user;
        db.collection(CLASSES).document(item.id)
                .update(FieldPath.of("de_nghi_doi_lich","phan_hoi",student),answer,FieldPath.of("updated_at"),FieldValue.serverTimestamp())
                .addOnFailureListener(failure -> { if (Objects.equals(owner,user)) fail("Chưa lưu được câu trả lời. "+message(failure)); });
    }
    private void put(WriteBatch batch, GroupClass.Registration registration) {
        put(batch,db.collection(REGISTRATIONS).document(registration.id()),registration.toMap(),
                docs("sent").containsKey(registration.id())||docs("received").containsKey(registration.id()));
    }
    private static void put(WriteBatch batch, DocumentReference ref, Map<String,Object> data, boolean exists) {
        data.put("updated_at",FieldValue.serverTimestamp());
        if (!exists) data.put("created_at",FieldValue.serverTimestamp());
        batch.set(ref,data,SetOptions.merge());
    }
    private void commit(WriteBatch batch) {
        String owner = user;
        batch.commit().addOnFailureListener(failure -> { if (Objects.equals(owner,user)) fail("Chưa lưu được thay đổi. "+message(failure)); });
    }

    private void fail(String message) { error = message; changes.setValue(changes.getValue()+1); }
    private static String message(Exception failure) {
        if (failure instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException.Code code = ((FirebaseFirestoreException) failure).getCode();
            if (code == FirebaseFirestoreException.Code.PERMISSION_DENIED) return "Bạn không có quyền thực hiện thao tác này hoặc dữ liệu đã thay đổi.";
            if (code == FirebaseFirestoreException.Code.UNAVAILABLE) return "Kiểm tra kết nối mạng rồi thử lại.";
        }
        return failure.getMessage() == null ? "" : failure.getMessage();
    }

    @Override protected void onCleared() {
        classListener.remove();
        for (ListenerRegistration listener : personal) listener.remove();
    }
}
