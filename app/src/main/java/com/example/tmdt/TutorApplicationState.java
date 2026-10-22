package com.example.tmdt;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/** UC14: one idempotent application per tutor/request; drafts are isolated by account and request. */
public final class TutorApplicationState extends ViewModel {
    private static final String PREFIX="uc14.";
    private final SavedStateHandle saved;
    private final FirebaseAuth auth=FirebaseAuth.getInstance();
    private final FirebaseFirestore db=FirebaseFirestore.getInstance();
    private final MutableLiveData<Integer> changes=new MutableLiveData<>(0);
    private final FirebaseAuth.AuthStateListener authListener=ignored->bindOwner();
    private ListenerRegistration listener;
    private String owner, requestId="", error="", notice="";
    private int generation, watchGeneration;
    private boolean loading, busy, offline;
    private TutorApplication application;

    public TutorApplicationState(SavedStateHandle saved){this.saved=saved;bindOwner();auth.addAuthStateListener(authListener);}
    public LiveData<Integer> changes(){return changes;}
    public TutorApplication application(){return sameOwner()?application:null;}
    public boolean loading(){return loading;}
    public boolean busy(){return busy;}
    public boolean offline(){return offline;}
    public String error(){return error;}
    public String notice(){return notice;}
    public boolean ownRequest(TutorRequestRecord record){return record!=null&&uid().equals(record.studentId);}
    public String draft(){String v=saved.get(PREFIX+"draft."+requestId);return v==null?"":v;}
    public void draft(String value){if(!busy&&sameOwner())saved.set(PREFIX+"draft."+requestId,value);}
    private String uid(){return auth.getCurrentUser()==null?"":auth.getCurrentUser().getUid();}
    private boolean sameOwner(){return owner!=null&&!owner.isEmpty()&&owner.equals(uid());}
    private void changed(){changes.setValue(changes.getValue()+1);}
    private void bindOwner(){
        String next=uid();if(next.equals(owner))return;
        generation++;watchGeneration++;if(listener!=null)listener.remove();listener=null;
        String previous=saved.get(PREFIX+"owner");
        if(!next.equals(previous)){
            for(String key:new ArrayList<>(saved.keys()))if(key.startsWith(PREFIX))saved.remove(key);
            saved.set(PREFIX+"owner",next);
        }
        owner=next;requestId="";application=null;busy=false;loading=false;offline=false;error="";notice="";changed();
    }
    public void watch(String id){
        bindOwner();String next=id==null?"":id;
        if(next.equals(requestId)&&listener!=null&&error.isEmpty())return;
        if(listener!=null)listener.remove();listener=null;
        requestId=next;application=null;error="";notice="";offline=false;
        final String actor=uid();final int token=generation, query=++watchGeneration;
        if(actor.isEmpty()||next.isEmpty()||next.contains("/")){loading=false;changed();return;}
        loading=true;changed();
        listener=db.collection("tutor_requests").document(next).collection("applications").document(actor)
                .addSnapshotListener(MetadataChanges.INCLUDE,(doc,failure)->{
                    if(token!=generation||query!=watchGeneration||!actor.equals(uid()))return;
                    loading=false;
                    if(failure!=null){application=null;offline=false;error=message(failure);}
                    else if(doc!=null){error="";offline=doc.getMetadata().isFromCache();application=TutorApplication.from(doc);}
                    changed();
                });
    }
    public void submit(){
        bindOwner();if(busy||requestId.isEmpty()||application!=null)return;
        final String actor=uid(), id=requestId, introduction=draft().trim();
        if(actor.isEmpty()){error="Vui lòng đăng nhập để ứng tuyển.";changed();return;}
        if(introduction.isEmpty()||introduction.length()>2000){error="Nhập lời giới thiệu từ 1 đến 2.000 ký tự.";changed();return;}
        final int token=generation;busy=true;error="";notice="";changed();
        com.google.firebase.firestore.DocumentReference parent=db.collection("tutor_requests").document(id);
        com.google.firebase.firestore.DocumentReference ref=parent.collection("applications").document(actor);
        db.runTransaction(tx->{
            DocumentSnapshot existing=tx.get(ref);
            if(existing.exists())return false;
            DocumentSnapshot request=tx.get(parent);
            DocumentSnapshot user=tx.get(db.collection("users").document(actor));
            DocumentSnapshot tutor=tx.get(db.collection("tutor_profiles").document(actor));
            if(!user.exists()||!"ACTIVE".equals(user.get("trang_thai"))||!tutor.exists()||!"VERIFIED".equals(tutor.get("trang_thai_xac_minh")))
                throw new ApplicationFailure("Chỉ gia sư đang hoạt động và đã được duyệt mới được ứng tuyển.");
            if(!request.exists()||!"OPEN".equals(request.get("trang_thai")))throw new ApplicationFailure("Tin đã đóng hoặc không còn nhận ứng tuyển.");
            if(actor.equals(request.get("student_id")))throw new ApplicationFailure("Bạn không thể ứng tuyển vào tin do mình đăng.");
            String name=tutor.getString("ho_ten");
            if(name==null||name.trim().isEmpty())throw new ApplicationFailure("Hồ sơ gia sư chưa có tên hợp lệ.");
            Map<String,Object> data=new HashMap<>();
            data.put("tutor_id",actor);data.put("ten_gia_su",name);data.put("gioi_thieu",introduction);
            data.put("trang_thai","PENDING");data.put("created_at",FieldValue.serverTimestamp());data.put("updated_at",FieldValue.serverTimestamp());
            tx.set(ref,data);return true;
        }).addOnCompleteListener(task->{
            if(token!=generation||!actor.equals(uid()))return;
            busy=false;
            if(task.isSuccessful())notice=Boolean.TRUE.equals(task.getResult())?"Đã gửi hồ sơ ứng tuyển.":"Bạn đã ứng tuyển vào tin này.";
            else error=message(task.getException());
            changed();
        });
    }
    private static String message(Throwable failure){
        Throwable cause=failure;
        while(cause!=null){
            if(cause instanceof ApplicationFailure)return cause.getMessage();
            if(cause instanceof FirebaseFirestoreException){
                FirebaseFirestoreException.Code code=((FirebaseFirestoreException)cause).getCode();
                if(code==FirebaseFirestoreException.Code.PERMISSION_DENIED)return "Chưa thể ứng tuyển hoặc xem trạng thái. Tin có thể đã đóng/xóa hoặc tài khoản chưa được cấp quyền.";
                if(code==FirebaseFirestoreException.Code.UNAVAILABLE||code==FirebaseFirestoreException.Code.DEADLINE_EXCEEDED)return "Chưa kết nối được máy chủ. Bản nháp vẫn được giữ; hãy thử lại.";
            }
            if(cause.getCause()==cause)break;cause=cause.getCause();
        }
        return "Chưa gửi được hồ sơ. Kiểm tra kết nối rồi thử lại; bản nháp vẫn được giữ.";
    }
    private static final class ApplicationFailure extends RuntimeException{ApplicationFailure(String value){super(value);}}
    @Override protected void onCleared(){generation++;watchGeneration++;auth.removeAuthStateListener(authListener);if(listener!=null)listener.remove();}
}
