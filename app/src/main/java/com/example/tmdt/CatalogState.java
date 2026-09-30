package com.example.tmdt;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Public catalog only: never fetches users or their private contact information. */
public final class CatalogState extends ViewModel {
    public static final class Teacher {
        public String id, name, introduction, region, education, school, mode;
        public long experience;
        public final List<Offering> offerings = new ArrayList<>();
    }
    public static final class Offering {
        public String tutorId, subjectId, level;
        public long price;
    }
    final MutableLiveData<Integer> changes = new MutableLiveData<>(0);
    final Map<String,String> subjects = new LinkedHashMap<>();
    final Map<String,Teacher> teachers = new LinkedHashMap<>();
    private final List<Offering> offerings = new ArrayList<>();
    private final List<ListenerRegistration> listeners = new ArrayList<>();
    private final Map<String,String> errors = new LinkedHashMap<>();
    private final java.util.Set<String> loaded = new java.util.HashSet<>();
    String query = "", subject = "";
    boolean offline;
    public CatalogState() { reload(); }
    public androidx.lifecycle.LiveData<Integer> changes() { return changes; }
    public Map<String,String> subjects() { return java.util.Collections.unmodifiableMap(subjects); }
    public Teacher teacher(String id) { return teachers.get(id); }
    public String query() { return query; }
    public void query(String value) { query=value; }
    public String subject() { return subject; }
    public void subject(String value) { subject=value; }
    public boolean offline() { return offline; }
    public boolean loading() { return loaded.size() < 3 && errors.isEmpty(); }
    public String error() { return errors.isEmpty() ? "" : "Không tải được danh sách gia sư. Kiểm tra kết nối rồi thử lại."; }
    private void changed() { changes.setValue(changes.getValue()+1); }
    public void reload() {
        for(ListenerRegistration listener:listeners) listener.remove();
        listeners.clear(); errors.clear(); loaded.clear(); offline=false; changed();
        FirebaseFirestore db=FirebaseFirestore.getInstance();
        listeners.add(db.collection("subjects").whereEqualTo("dang_su_dung",true)
                .addSnapshotListener(MetadataChanges.INCLUDE,(snapshot,error)->{
                    if(error!=null){errors.put("subjects",error.getMessage());changed();return;}
                    subjects.clear();
                    for(DocumentSnapshot doc:snapshot) subjects.put(doc.getId(),text(doc,"ten_mon"));
                    loaded.add("subjects");errors.remove("subjects");join();
                }));
        listeners.add(db.collection("tutor_profiles").whereEqualTo("trang_thai_xac_minh","VERIFIED")
                .addSnapshotListener(MetadataChanges.INCLUDE,(snapshot,error)->{
                    if(error!=null){errors.put("teachers",error.getMessage());changed();return;}
                    teachers.clear();offline=snapshot.getMetadata().isFromCache();
                    for(DocumentSnapshot doc:snapshot) {
                        if(!Boolean.TRUE.equals(doc.getBoolean("nhan_lop")))continue;
                        Teacher teacher=new Teacher();teacher.id=doc.getId();teacher.name=text(doc,"ho_ten");
                        if(teacher.name.isEmpty())continue;
                        teacher.introduction=text(doc,"gioi_thieu");teacher.region=text(doc,"khu_vuc");
                        teacher.education=text(doc,"hoc_van");teacher.school=text(doc,"truong");
                        teacher.mode=text(doc,"hinh_thuc_day");teacher.experience=number(doc,"so_nam_kinh_nghiem");
                        teachers.put(teacher.id,teacher);
                    }
                    loaded.add("teachers");errors.remove("teachers");join();
                }));
        listeners.add(db.collection("tutor_subjects").addSnapshotListener((snapshot,error)->{
            if(error!=null){errors.put("offerings",error.getMessage());changed();return;}
            offerings.clear();
            for(DocumentSnapshot doc:snapshot){
                Offering offering=new Offering();offering.tutorId=text(doc,"tutor_id");
                offering.subjectId=text(doc,"subject_id");offering.level=text(doc,"cap_lop");
                offering.price=number(doc,"hoc_phi_moi_buoi");
                if(offering.price>=50000 && offering.price<=2000000)offerings.add(offering);
            }
            loaded.add("offerings");errors.remove("offerings");join();
        }));
    }
    private void join() {
        for(Teacher teacher:teachers.values())teacher.offerings.clear();
        for(Offering offering:offerings){
            Teacher teacher=teachers.get(offering.tutorId);
            if(teacher!=null && subjects.containsKey(offering.subjectId))teacher.offerings.add(offering);
        }
        changed();
    }
    public List<Teacher> results() {
        List<Teacher> result = new ArrayList<>();
        String keyword = normalize(query);

        for (Teacher teacher : teachers.values()) {
            if (teacher.offerings.isEmpty()) {
                continue;
            }

            boolean matchesSubject = subject.isEmpty();
            for (Offering offering : teacher.offerings) {
                if (offering.subjectId.equals(subject)) {
                    matchesSubject = true;
                }
            }

            String teacherName = normalize(teacher.name);
            boolean matchesName = teacherName.contains(keyword);
            if (matchesName && matchesSubject) {
                result.add(teacher);
            }
        }
        return result;
    }
    public static String normalize(String value) {
        String text = Normalizer.normalize(value, Normalizer.Form.NFD);
        text = text.replaceAll("\\p{M}", "");
        text = text.toLowerCase(Locale.ROOT);
        text = text.replace('đ', 'd');
        return text.trim();
    }
    public static String level(String value) {
        switch(value){case "TIEU_HOC":return "Tiểu học";case "THCS":return "THCS";
            case "THPT":return "THPT";case "LUYEN_THI":return "Luyện thi";default:return "Khác";}
    }
    public static String mode(String value) {
        return "TAI_NHA".equals(value)?"Tại nhà":"TRUC_TUYEN".equals(value)?"Trực tuyến":"Trực tuyến · Tại nhà";
    }
    public static String money(long value) { return String.format(Locale.forLanguageTag("vi-VN"),"%,d ₫",value); }
    private static String text(DocumentSnapshot doc,String field){Object v=doc.get(field);return v instanceof String?(String)v:"";}
    private static long number(DocumentSnapshot doc,String field){Object v=doc.get(field);return v instanceof Number?((Number)v).longValue():0;}
    @Override protected void onCleared(){for(ListenerRegistration listener:listeners)listener.remove();}
}

