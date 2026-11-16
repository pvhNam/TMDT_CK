package com.example.tmdt;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** UC15 reads only the verified public tutor profile and public teaching offerings. */
public final class TutorApplicantProfileState extends ViewModel {
    public static final class Profile{
        public final String name,education,school,major,introduction,region,mode;
        public final long experience;
        Profile(DocumentSnapshot doc){Map<String,Object>d=doc.getData();name=text(d.get("ho_ten"));education=text(d.get("hoc_van"));
            school=text(d.get("truong"));major=text(d.get("chuyen_nganh"));introduction=text(d.get("gioi_thieu"));region=text(d.get("khu_vuc"));mode=text(d.get("hinh_thuc_day"));
            Object years=d.get("so_nam_kinh_nghiem");experience=years instanceof Number?Math.max(0,((Number)years).longValue()):0;}
    }
    public static final class Offering{
        public final String subjectId,level;public final long price;
        Offering(DocumentSnapshot doc){Map<String,Object>d=doc.getData();subjectId=text(d.get("subject_id"));level=text(d.get("cap_lop"));Object amount=d.get("hoc_phi_moi_buoi");price=amount instanceof Number?((Number)amount).longValue():0;}
    }
    private final MutableLiveData<Integer> changes=new MutableLiveData<>(0);
    private final FirebaseFirestore db=FirebaseFirestore.getInstance();
    private final FirebaseAuth auth=FirebaseAuth.getInstance();
    private final FirebaseAuth.AuthStateListener authListener=ignored->accountChanged();
    private ListenerRegistration profileListener,offeringsListener;
    private String tutorId="",owner="",error="",offeringError="";
    private int generation;private boolean loading;
    private Profile profile;
    private final List<Offering> offerings=new ArrayList<>();
    public TutorApplicantProfileState(){owner=uid();auth.addAuthStateListener(authListener);}
    public LiveData<Integer> changes(){return changes;}
    public Profile profile(){return owner.equals(uid())?profile:null;}
    public List<Offering> offerings(){return Collections.unmodifiableList(new ArrayList<>(offerings));}
    public String error(){return error;}
    public String offeringError(){return offeringError;}
    public boolean loading(){return loading;}
    private String uid(){return auth.getCurrentUser()==null?"":auth.getCurrentUser().getUid();}
    private void changed(){changes.setValue(changes.getValue()+1);}
    private void accountChanged(){if(!owner.equals(uid())){clear();owner=uid();tutorId="";changed();}}
    private void clear(){generation++;if(profileListener!=null)profileListener.remove();if(offeringsListener!=null)offeringsListener.remove();
        profileListener=null;offeringsListener=null;profile=null;offerings.clear();error="";offeringError="";loading=false;}
    public void retry(){String id=tutorId;clear();watch(id);}
    public void watch(String id){
        accountChanged();if(id==null||id.isEmpty()||id.contains("/"))return;
        if(id.equals(tutorId)&&profileListener!=null&&error.isEmpty())return;
        clear();tutorId=id;final int token=generation;final String actor=uid();loading=true;changed();
        profileListener=db.collection("tutor_profiles").document(id).addSnapshotListener((doc,failure)->{
            if(token!=generation||!actor.equals(uid()))return;loading=false;
            if(failure!=null||doc==null||!doc.exists()||!"VERIFIED".equals(doc.get("trang_thai_xac_minh"))){profile=null;error="Hồ sơ gia sư không còn được công khai hoặc chưa thể tải. Vui lòng thử lại.";}
            else{profile=new Profile(doc);error="";}changed();
        });
        offeringsListener=db.collection("tutor_subjects").whereEqualTo("tutor_id",id).addSnapshotListener((docs,failure)->{
            if(token!=generation||!actor.equals(uid()))return;offerings.clear();
            offeringError=failure==null?"":"Chưa tải được danh sách môn dạy.";
            if(docs!=null)for(DocumentSnapshot doc:docs)offerings.add(new Offering(doc));changed();
        });
    }
    private static String text(Object value){return value instanceof String?(String)value:"";}
    @Override protected void onCleared(){auth.removeAuthStateListener(authListener);clear();}
}
