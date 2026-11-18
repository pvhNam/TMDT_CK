package com.example.tmdt;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.firestore.*;
import java.util.*;

/** Private administration data is loaded only while an active ADMIN is signed in. */
public final class AdminState extends ViewModel {
    public final MutableLiveData<Integer> changes=new MutableLiveData<>(0);
    public final Map<String,PaymentState.Row> users=new LinkedHashMap<>(),contracts=new LinkedHashMap<>();
    public final Map<String,PaymentState.Row> tutors=new LinkedHashMap<>(),tutorContracts=new LinkedHashMap<>();
    public final Map<String,PaymentState.Row> tutorProfiles=new LinkedHashMap<>(),agreements=new LinkedHashMap<>();
    public String selectedAgreement="",agreementOrigin="admin_contracts";
    public PaymentState.Row agreementDetail,contractDetail;
    public boolean loadingAgreements,moreAgreements,loadingAgreement,loadingContract,loadingTutorProfiles;
    public int contractTab;
    public String selectedTutor="",tutorQuery="";
    public String userOrigin="admin_users",contractOrigin="admin_contracts";
    public String selectedIncomeTutor="",incomeTutorName="",incomeOrigin="admin_income",incomeQuery="";
    public int financeTab;
    public boolean loadingTutors,loadingTutorContracts,moreTutors,moreTutorContracts,revenueAnimated;
    public String selectedUser="",selectedContract="",userFilter="ALL",query="",error="",notice="";
    public boolean active,busy,loadingUsers,loadingContracts,moreUsers,moreContracts;
    public ReportPeriod revenuePeriod=ReportPeriod.month(ReportPeriod.currentMonth());
    public long totalUsers,totalTutors,totalStudents;
    private final FirebaseFirestore db=FirebaseFirestore.getInstance();
    private String owner="";
    private int version,userLimit=50,contractLimit=50;
    private int countedUsers=-1;
    private ListenerRegistration userListener,contractListener;
    private ListenerRegistration tutorListener,tutorContractListener;
    private ListenerRegistration tutorProfileListener,agreementListener,agreementDetailListener,contractDetailListener;
    private int agreementLimit=50,agreementVersion,contractVersion;
    private boolean moreTutorUsers,moreTutorProfiles;
    private int tutorLimit=50,tutorContractLimit=50,tutorVersion;
    public void account(AccountState account) {
        UserProfile p=account.profile();
        String next=account.signedIn()&&p!=null&&"ADMIN".equals(p.role)&&"ACTIVE".equals(p.status)?account.uid():"";
        if(next.equals(owner))return;
        boolean restoreSelection=owner.isEmpty()&&!next.isEmpty();
        stop();owner=next;active=!next.isEmpty();version++;
        users.clear();contracts.clear();if(!restoreSelection){selectedUser="";selectedContract="";}error="";notice="";busy=false;
        tutors.clear();tutorContracts.clear();tutorProfiles.clear();agreements.clear();agreementDetail=null;contractDetail=null;
        loadingAgreement=loadingContract=loadingAgreements=loadingTutorProfiles=false;moreAgreements=moreTutorUsers=moreTutorProfiles=moreTutors=false;agreementLimit=50;
        if(!restoreSelection){selectedTutor="";selectedAgreement="";contractTab=0;userOrigin="admin_users";contractOrigin=agreementOrigin="admin_contracts";}
        if(!restoreSelection){selectedIncomeTutor="";incomeTutorName="";incomeOrigin="admin_income";incomeQuery="";}
        tutorLimit=tutorContractLimit=50;revenueAnimated=false;
        if(!restoreSelection){revenuePeriod=ReportPeriod.month(ReportPeriod.currentMonth());financeTab=0;}
        totalUsers=totalTutors=totalStudents=0;userLimit=contractLimit=50;countedUsers=-1;
        if(active){listenUsers();listenContracts();listenTutors();listenAgreements();if(!selectedTutor.isEmpty())listenTutorContracts();if(!selectedAgreement.isEmpty())listenAgreement();if(!selectedContract.isEmpty())listenContract();}changed();
    }
    public void retry(){if(!active||busy)return;version++;error="";listenUsers();listenContracts();listenTutors();listenAgreements();if(!selectedTutor.isEmpty())listenTutorContracts();if(!selectedAgreement.isEmpty())listenAgreement();if(!selectedContract.isEmpty())listenContract();counts();changed();}
    public void selectTutor(String uid){
        if(!active)return;selectedTutor=uid;tutorContracts.clear();tutorContractLimit=50;
        if(tutorContractListener!=null)tutorContractListener.remove();tutorVersion++;
        loadingTutorContracts=false;moreTutorContracts=false;
        if(!uid.isEmpty()){listenTutorContracts();selectAgreement(uid);}changed();
    }
    public void moreTutors(){if(active&&!loadingTutors&&!loadingTutorProfiles&&moreTutors){tutorLimit+=50;listenTutors();changed();}}
    public void moreTutorContracts(){if(active&&!loadingTutorContracts&&moreTutorContracts){tutorContractLimit+=50;listenTutorContracts();}}
    public PaymentState.Row contract(String id){if(contractDetail!=null&&id.equals(contractDetail.id))return contractDetail;return tutorContracts.containsKey(id)?tutorContracts.get(id):contracts.get(id);}
    public PaymentState.Row agreement(String uid){return uid.equals(selectedAgreement)?agreementDetail:agreements.get(uid);}
    public void selectContract(String id){if(!active)return;selectedContract=id;contractDetail=contract(id);listenContract();changed();}
    public void selectAgreement(String uid){if(!active||(uid.equals(selectedAgreement)&&agreementDetailListener!=null))return;selectedAgreement=uid;agreementDetail=agreements.get(uid);listenAgreement();changed();}
    public void moreAgreements(){if(active&&!loadingAgreements&&moreAgreements){agreementLimit+=50;listenAgreements();changed();}}
    /** Include tutor profiles and signed records even when an older user document lacks la_gia_su. */
    public Map<String,PaymentState.Row> tutorDirectory(){
        Map<String,PaymentState.Row> directory=new LinkedHashMap<>();
        for(PaymentState.Row tutor:tutorProfiles.values())mergeTutor(directory,tutor);
        for(PaymentState.Row user:users.values())if(user.flag("la_gia_su"))mergeTutor(directory,user);
        for(PaymentState.Row tutor:tutors.values())mergeTutor(directory,tutor);
        for(PaymentState.Row agreement:agreements.values())addTutor(directory,agreement.id,agreement.text("ho_ten"));
        for(PaymentState.Row contract:contracts.values())addTutor(directory,contract.text("tutor_id"),contract.text("tutor_name"));
        for(String uid:new ArrayList<>(directory.keySet())){
            PaymentState.Row user=users.get(uid);if(user!=null)mergeTutor(directory,user);
        }
        return directory;
    }
    private static void mergeTutor(Map<String,PaymentState.Row> rows,PaymentState.Row row){
        Map<String,Object> fields=new HashMap<>();if(rows.containsKey(row.id))fields.putAll(rows.get(row.id).data);
        fields.putAll(row.data);fields.put("la_gia_su",true);rows.put(row.id,new PaymentState.Row(row.id,fields));
    }
    private static void addTutor(Map<String,PaymentState.Row> rows,String uid,String name){
        if(uid.isEmpty()||rows.containsKey(uid))return;
        Map<String,Object> fields=new HashMap<>();fields.put("ho_ten",name);fields.put("la_gia_su",true);rows.put(uid,new PaymentState.Row(uid,fields));
    }
    private void listenTutors(){
        if(tutorListener!=null)tutorListener.remove();loadingTutors=true;int token=version;
        tutorListener=db.collection("users").whereEqualTo("la_gia_su",true).limit(tutorLimit).addSnapshotListener((snapshot,e)->{
            if(token!=version)return;loadingTutors=false;if(e!=null){failure(e);return;}tutors.clear();
            if(snapshot!=null)for(DocumentSnapshot d:snapshot)tutors.put(d.getId(),new PaymentState.Row(d.getId(),d.getData()));
            moreTutorUsers=snapshot!=null&&snapshot.size()==tutorLimit;moreTutors=moreTutorUsers||moreTutorProfiles;changed();
        });
        if(tutorProfileListener!=null)tutorProfileListener.remove();loadingTutorProfiles=true;
        tutorProfileListener=db.collection("tutor_profiles").orderBy(FieldPath.documentId()).limit(tutorLimit).addSnapshotListener((snapshot,e)->{
            if(token!=version)return;loadingTutorProfiles=false;if(e!=null){failure(e);return;}tutorProfiles.clear();
            if(snapshot!=null)for(DocumentSnapshot d:snapshot)tutorProfiles.put(d.getId(),new PaymentState.Row(d.getId(),d.getData()));
            moreTutorProfiles=snapshot!=null&&snapshot.size()==tutorLimit;moreTutors=moreTutorUsers||moreTutorProfiles;changed();
        });
    }
    private void listenTutorContracts(){
        if(tutorContractListener!=null)tutorContractListener.remove();loadingTutorContracts=true;
        int token=version,selection=++tutorVersion;
        tutorContractListener=db.collection("contracts").whereEqualTo("tutor_id",selectedTutor)
            .limit(tutorContractLimit).addSnapshotListener((snapshot,e)->{
                if(token!=version||selection!=tutorVersion)return;loadingTutorContracts=false;
                if(e!=null){failure(e);return;}tutorContracts.clear();
                if(snapshot!=null)for(DocumentSnapshot d:snapshot)tutorContracts.put(d.getId(),new PaymentState.Row(d.getId(),d.getData()));
                moreTutorContracts=snapshot!=null&&snapshot.size()==tutorContractLimit;changed();
            });
    }
    private void listenAgreements(){
        if(agreementListener!=null)agreementListener.remove();loadingAgreements=true;int token=version;
        agreementListener=db.collection("tutor_agreements").orderBy(FieldPath.documentId()).limit(agreementLimit).addSnapshotListener((snapshot,e)->{
            if(token!=version)return;loadingAgreements=false;if(e!=null){failure(e);return;}agreements.clear();
            if(snapshot!=null)for(DocumentSnapshot d:snapshot)agreements.put(d.getId(),new PaymentState.Row(d.getId(),d.getData()));
            moreAgreements=snapshot!=null&&snapshot.size()==agreementLimit;changed();
        });
    }
    private void listenAgreement(){
        if(agreementDetailListener!=null)agreementDetailListener.remove();int token=version,selection=++agreementVersion;
        loadingAgreement=!selectedAgreement.isEmpty();if(!loadingAgreement)return;
        String uid=selectedAgreement;
        agreementDetailListener=db.collection("tutor_agreements").document(uid).addSnapshotListener((doc,e)->{
            if(token!=version||selection!=agreementVersion)return;loadingAgreement=false;if(e!=null){failure(e);return;}
            agreementDetail=doc!=null&&doc.exists()?new PaymentState.Row(uid,doc.getData()):null;changed();
        });
    }
    private void listenContract(){
        if(contractDetailListener!=null)contractDetailListener.remove();int token=version,selection=++contractVersion;
        loadingContract=!selectedContract.isEmpty();if(!loadingContract)return;
        String id=selectedContract;
        contractDetailListener=db.collection("contracts").document(id).addSnapshotListener((doc,e)->{
            if(token!=version||selection!=contractVersion)return;loadingContract=false;if(e!=null){failure(e);return;}
            contractDetail=doc!=null&&doc.exists()?new PaymentState.Row(id,doc.getData()):null;changed();
        });
    }
    public void moreUsers(){if(!loadingUsers&&moreUsers){userLimit+=50;listenUsers();}}
    public void moreContracts(){if(!loadingContracts&&moreContracts){contractLimit+=50;listenContracts();}}
    private void listenUsers(){
        if(userListener!=null)userListener.remove();loadingUsers=true;int token=version;
        userListener=db.collection("users").orderBy(FieldPath.documentId()).limit(userLimit).addSnapshotListener((snapshot,e)->{
            if(token!=version)return;loadingUsers=false;
            if(e!=null){failure(e);return;}users.clear();
            if(snapshot!=null)for(DocumentSnapshot d:snapshot)users.put(d.getId(),new PaymentState.Row(d.getId(),d.getData()));
            moreUsers=snapshot!=null&&snapshot.size()==userLimit;
            if(snapshot!=null&&!snapshot.getMetadata().isFromCache()&&countedUsers!=snapshot.size()){
                countedUsers=snapshot.size();counts();
            }changed();
        });
    }
    private void listenContracts(){
        if(contractListener!=null)contractListener.remove();loadingContracts=true;int token=version;
        contractListener=db.collection("contracts").orderBy(FieldPath.documentId()).limit(contractLimit).addSnapshotListener((snapshot,e)->{
            if(token!=version)return;loadingContracts=false;
            if(e!=null){failure(e);return;}contracts.clear();
            if(snapshot!=null)for(DocumentSnapshot d:snapshot)contracts.put(d.getId(),new PaymentState.Row(d.getId(),d.getData()));
            moreContracts=snapshot!=null&&snapshot.size()==contractLimit;changed();
        });
    }
    private void counts(){
        int token=version;
        count(db.collection("users"),token,n->totalUsers=n);
        count(db.collection("users").whereEqualTo("la_gia_su",true),token,n->totalTutors=n);
        count(db.collection("users").whereEqualTo("vai_tro","STUDENT").whereEqualTo("la_gia_su",false),token,n->totalStudents=n);
    }
    private void count(Query query,int token,java.util.function.LongConsumer apply){
        query.count().get(AggregateSource.SERVER).addOnCompleteListener(task->{
            if(token!=version)return;
            if(task.isSuccessful()){apply.accept(task.getResult().getCount());changed();}else failure(task.getException());
        });
    }
    public void status(String uid,boolean ban,String reason){
        if(!active||busy)return;
        if(uid.equals(owner)){error="Không thể khóa tài khoản đang đăng nhập.";changed();return;}
        if(reason==null||reason.trim().length()<5||reason.trim().length()>500){error="Nhập lý do từ 5 đến 500 ký tự.";changed();return;}
        final int token=version;final String actor=owner;busy=true;error="";notice="";changed();
        DocumentReference user=db.collection("users").document(uid),audit=db.collection("admin_audit_logs").document();
        db.runTransaction(tx->{
            DocumentSnapshot d=tx.get(user);
            if(!d.exists()||"ADMIN".equals(d.getString("vai_tro")))throw new IllegalStateException("Không thể khóa hoặc sửa trạng thái quản trị viên.");
            String before=d.getString("trang_thai"),after=ban?"BANNED":"ACTIVE";
            if(after.equals(before))throw new IllegalStateException("Trạng thái tài khoản đã được thay đổi. Hãy kiểm tra lại.");
            tx.update(user,"trang_thai",after,"updated_at",FieldValue.serverTimestamp());
            Map<String,Object> log=new HashMap<>();log.put("actor_id",actor);log.put("target_id",uid);
            log.put("action",ban?"BAN_USER":"UNBAN_USER");log.put("before",before);log.put("after",after);
            log.put("reason",reason.trim());log.put("created_at",FieldValue.serverTimestamp());tx.set(audit,log);return null;
        }).addOnCompleteListener(task->{
            if(token!=version)return;busy=false;
            if(task.isSuccessful())notice=ban?"Đã khóa tài khoản.":"Đã mở khóa tài khoản.";
            else error="Không cập nhật được trạng thái. "+message(task.getException());changed();
        });
    }
    public List<PaymentState.Row> contractsFor(String uid){
        List<PaymentState.Row> result=new ArrayList<>();for(PaymentState.Row r:contracts.values())
            if(uid.equals(r.text("tutor_id"))||uid.equals(r.text("student_id")))result.add(r);return result;
    }
    public boolean signedContract(PaymentState.Row r){return r.flag("tutor_confirmed")&&r.flag("student_confirmed");}
    public static String role(PaymentState.Row r){return "ADMIN".equals(r.text("vai_tro"))?"Quản trị viên":r.flag("la_gia_su")?"Gia sư":"Học sinh";}
    public static String contractStatus(PaymentState.Row r){
        switch(r.text("status")){
            case "CHO_XAC_NHAN":return "Chờ ký xác nhận";case "CHO_KY_QUY":return "Đã ký · Chờ học phí";
            case "HIEU_LUC":return "Đang hiệu lực";case "HOAN_THANH":return "Đã hoàn thành";
            case "TU_CHOI":return "Đã từ chối";case "DA_HUY":return "Đã hủy";default:return r.text("status");
        }
    }
    private void failure(Exception e){error="Không tải được dữ liệu quản trị. "+message(e);changed();}
    private static String message(Exception e){
        if(e instanceof FirebaseFirestoreException&&((FirebaseFirestoreException)e).getCode()==FirebaseFirestoreException.Code.PERMISSION_DENIED)
            return "Kiểm tra vai trò ADMIN và cập nhật Firestore rules.";
        return e==null?"Hãy thử lại.":e.getMessage();
    }
    public void changed(){changes.setValue(changes.getValue()+1);}
    private void stop(){
        for(ListenerRegistration listener:new ListenerRegistration[]{userListener,contractListener,tutorListener,tutorContractListener,tutorProfileListener,agreementListener,agreementDetailListener,contractDetailListener})if(listener!=null)listener.remove();
        tutorVersion++;agreementVersion++;contractVersion++;userListener=contractListener=tutorListener=tutorContractListener=tutorProfileListener=agreementListener=agreementDetailListener=contractDetailListener=null;
    }
    @Override protected void onCleared(){stop();super.onCleared();}
}
