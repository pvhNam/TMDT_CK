package com.example.tmdt;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import androidx.lifecycle.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.Executors;

/** Live owner/admin review state; all approvals are enforced by server rules. */
public final class TutorReviewState extends ViewModel {
    public final MutableLiveData<Integer> changes=new MutableLiveData<>(0);
    public Map<String,Object> own=new HashMap<>(),selected=new HashMap<>();
    public final List<Map<String,Object>> queue=new ArrayList<>();
    public String error="",notice="",owner="",selectedId="",destination;
    public boolean busy,loading=true,admin;
    private Map<String,Object> ownProfile=new HashMap<>(),ownApplication=new HashMap<>();
    private ListenerRegistration profileListener,applicationListener,queueListener,detailListener;
    private final FirebaseFirestore db=FirebaseFirestore.getInstance();
    private final Context context=FirebaseApp.getInstance().getApplicationContext();
    private final java.util.concurrent.ExecutorService io=Executors.newSingleThreadExecutor();
    private void changed(){changes.setValue(changes.getValue()+1);}
    private void fail(Exception e){busy=false;notice="";error=AccountState.message(e);changed();}
    private void uploadFailed(Exception e){
        busy=false;notice="";
        android.util.Log.w("TutorEvidence","Firestore image upload failed",e);
        if(e instanceof FirebaseFirestoreException){
            FirebaseFirestoreException.Code code=((FirebaseFirestoreException)e).getCode();
            switch(code){
                case PERMISSION_DENIED:
                    if(Arrays.asList("PENDING","IN_REVIEW","VERIFIED","REJECTED").contains(status()))
                        error="Hồ sơ đã gửi nên chưa thể thay ảnh. Hãy chờ admin yêu cầu bổ sung trước khi tải ảnh mới. (PERMISSION_DENIED)";
                    else error="Firestore từ chối lưu ảnh minh chứng. Hãy kiểm tra tài khoản đang hoạt động và Publish rules mới trong dự án "+FirebaseApp.getInstance().getOptions().getProjectId()+". (PERMISSION_DENIED)";
                    break;
                case UNAVAILABLE:case DEADLINE_EXCEEDED:
                    error="Chưa kết nối được Firestore để lưu ảnh. Kiểm tra mạng rồi chọn lại ảnh. ("+code+")";break;
                case RESOURCE_EXHAUSTED:
                    error="Firestore đã hết hạn mức thao tác hoặc dung lượng. Hãy kiểm tra mục Usage trên Firebase. (RESOURCE_EXHAUSTED)";break;
                case UNAUTHENTICATED:
                    error="Phiên đăng nhập đã hết hạn. Hãy đăng nhập lại rồi chọn ảnh. (UNAUTHENTICATED)";break;
                default:error="Chưa lưu được ảnh minh chứng vào Firestore. Vui lòng thử lại. ("+code+")";
            }
        }else error=TutorEvidenceAccess.message(e);
        changed();
    }
    public void account(AccountState account) {
        String uid=account.signedIn()?account.uid():"";
        if(!owner.equals(uid)){
            close();owner=uid;own.clear();ownProfile.clear();ownApplication.clear();selected.clear();queue.clear();selectedId="";error="";notice="";loading=!uid.isEmpty();
            if(!uid.isEmpty()){
                profileListener=db.collection("tutor_profiles").document(uid).addSnapshotListener((doc,e)->{
                    if(!owner.equals(uid))return;if(e!=null){loading=false;error=AccountState.message(e);}else{ownProfile=doc!=null&&doc.exists()?new HashMap<>(doc.getData()):new HashMap<>();}
                    mergeOwner();
                });
                applicationListener=db.collection("tutor_applications").document(uid).addSnapshotListener((doc,e)->{
                    if(!owner.equals(uid))return;loading=false;
                    if(e!=null)error=AccountState.message(e);else ownApplication=doc!=null&&doc.exists()?new HashMap<>(doc.getData()):new HashMap<>();mergeOwner();
                });
            }
        }
        boolean allowed=account.profile()!=null&&"ADMIN".equals(account.profile().role)&&"ACTIVE".equals(account.profile().status);
        if(admin!=allowed){admin=allowed;if(queueListener!=null){queueListener.remove();queueListener=null;}queue.clear();
            if(admin)queueListener=db.collection("tutor_profiles").limit(100).addSnapshotListener((docs,e)->{
                if(!admin)return;if(e!=null){error=AccountState.message(e);changed();return;}queue.clear();
                for(DocumentSnapshot doc:docs){Map<String,Object> item=new HashMap<>(doc.getData());item.put("id",doc.getId());queue.add(item);}changed();
            });
        }
    }
    private void mergeOwner(){own=new HashMap<>(ownProfile);own.putAll(ownApplication);changed();}
    public static String text(Map<String,Object> data,String key){Object value=data.get(key);return value instanceof String?(String)value:"";}
    public String status(){return text(own,"trang_thai_xac_minh");}
    @SuppressWarnings("unchecked") public static List<Map<String,Object>> files(Map<String,Object> data){
        Object value=data.get("minh_chung");return value instanceof List?(List<Map<String,Object>>)value:Collections.emptyList();
    }
    public void select(String uid){
        if(!admin)return;if(detailListener!=null)detailListener.remove();selectedId=uid;selected=new HashMap<>();
        for(Map<String,Object> item:queue)if(uid.equals(item.get("id")))selected.putAll(item);
        detailListener=db.collection("tutor_applications").document(uid).addSnapshotListener((doc,e)->{
            if(!uid.equals(selectedId)||!admin)return;
            Map<String,Object> next=new HashMap<>();for(Map<String,Object> item:queue)if(uid.equals(item.get("id")))next.putAll(item);
            if(e!=null)error=AccountState.message(e);else if(doc!=null&&doc.exists())next.putAll(doc.getData());selected=next;changed();
        });changed();
    }
    public void decide(String uid,String status,String reason){
        if(!admin||busy)return;if((status.equals("NEEDS_REVISION")||status.equals("REJECTED"))&&reason.trim().isEmpty()){error="Vui lòng nhập lý do hoặc yêu cầu bổ sung.";changed();return;}
        if(reason.length()>500){error="Lý do tối đa 500 ký tự.";changed();return;}
        busy=true;error="";String reviewer=owner;changed();
        db.runTransaction(tx->{
            DocumentReference profile=db.collection("tutor_profiles").document(uid),application=db.collection("tutor_applications").document(uid);
            DocumentSnapshot p=tx.get(profile),a=tx.get(application);
            if(!Arrays.asList("PENDING","IN_REVIEW").contains(p.getString("trang_thai_xac_minh")))throw new IllegalStateException("Profile already reviewed");
            if(status.equals("VERIFIED")&&(!a.exists()||files(a.getData()).stream().noneMatch(f->"diploma".equals(f.get("kind"))&&TutorEvidenceDraft.isFirestoreImage(f))))
                throw new IllegalStateException("Missing image evidence");
            Map<String,Object> update=new HashMap<>();update.put("trang_thai_xac_minh",status);update.put("updated_at",FieldValue.serverTimestamp());tx.update(profile,update);
            update.put("nguoi_duyet_id",reviewer);update.put("ly_do",reason.trim());update.put("reviewed_at",FieldValue.serverTimestamp());
            if(!a.exists()){update.put("user_id",uid);update.put("ho_ten",p.getString("ho_ten"));update.put("created_at",p.get("created_at"));}
            tx.set(application,update,SetOptions.merge());return null;
        }).addOnCompleteListener(task->{if(!owner.equals(reviewer))return;if(!task.isSuccessful()){fail(task.getException());return;}busy=false;notice="Đã cập nhật kết quả xét duyệt.";changed();});
    }
    public void resubmit(){
        if(busy||!"NEEDS_REVISION".equals(status()))return;
        TutorEvidenceDraft value=TutorEvidenceDraft.load(context,owner);String invalid=value.error();
        if(!invalid.isEmpty()){error=invalid;changed();return;}busy=true;error="";String uid=owner;notice="Đang kiểm tra ảnh minh chứng…";changed();
        TutorEvidenceAccess.verify(value.files,uid).addOnCompleteListener(checked->{
            if(!uid.equals(owner))return;
            if(!checked.isSuccessful()){busy=false;notice="";error=TutorEvidenceAccess.message(checked.getException());changed();return;}
            notice="Đang gửi lại hồ sơ…";changed();resubmitVerified(value,uid);
        });
    }
    private void resubmitVerified(TutorEvidenceDraft value,String uid){
        db.runTransaction(tx->{
            DocumentReference profile=db.collection("tutor_profiles").document(uid),application=db.collection("tutor_applications").document(uid);
            DocumentSnapshot p=tx.get(profile);tx.get(application);
            if(!"NEEDS_REVISION".equals(p.getString("trang_thai_xac_minh")))throw new IllegalStateException("Review changed");
            Map<String,Object> update=value.fields();update.put("trang_thai_xac_minh","PENDING");update.put("submitted_at",FieldValue.serverTimestamp());update.put("updated_at",FieldValue.serverTimestamp());
            tx.set(application,update,SetOptions.merge());
            Map<String,Object> tutor=new HashMap<>();tutor.put("trang_thai_xac_minh","PENDING");tutor.put("truong",value.school.trim());tutor.put("chuyen_nganh",value.major.trim());tutor.put("updated_at",FieldValue.serverTimestamp());tx.update(profile,tutor);return null;
        }).addOnCompleteListener(task->{if(!uid.equals(owner))return;if(!task.isSuccessful()){fail(task.getException());return;}TutorEvidenceDraft.clear(context,uid);busy=false;destination="tutor_pending";notice="Đã gửi lại hồ sơ để xét duyệt.";changed();});
    }
    public TutorEvidenceDraft draft(boolean supplement){
        TutorEvidenceDraft draft=TutorEvidenceDraft.load(context,owner);
        if(supplement&&draft.files.isEmpty()&&draft.school.isEmpty()){
            draft.school=text(own,"truong");draft.major=text(own,"chuyen_nganh");draft.files.addAll(files(own));draft.save(context,owner);
        }return draft;
    }
    public void upload(Uri uri,String kind,boolean supplement){
        if(busy||owner.isEmpty())return;busy=true;error="";notice="Đang chuẩn bị và lưu ảnh minh chứng…";String uid=owner;changed();
        io.execute(()->{
            File temp=null;
            try {
                String name="minh_chung",type=context.getContentResolver().getType(uri);long declared=-1;
                try(Cursor cursor=context.getContentResolver().query(uri,null,null,null,null)){
                    if(cursor!=null&&cursor.moveToFirst()){
                        int n=cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME),s=cursor.getColumnIndex(OpenableColumns.SIZE);
                        if(n>=0)name=cursor.getString(n);if(s>=0&&!cursor.isNull(s))declared=cursor.getLong(s);
                    }
                }
                long max=(supplement?5:10)*1024L*1024;
                if(!Arrays.asList("image/jpeg","image/png").contains(type))throw new IOException("Minh chứng phải là ảnh JPG hoặc PNG.");
                if(declared>max)throw new IOException("Tệp vượt quá "+(supplement?5:10)+" MB.");
                temp=File.createTempFile("evidence-",".upload",context.getCacheDir());long size=0;
                try(InputStream in=context.getContentResolver().openInputStream(uri);OutputStream out=new FileOutputStream(temp)){
                    if(in==null)throw new IOException("Không đọc được tệp.");byte[] buffer=new byte[8192];int count;
                    while((count=in.read(buffer))!=-1){size+=count;if(size>max)throw new IOException("Tệp quá lớn.");out.write(buffer,0,count);}
                }
                if(size==0)throw new IOException("Tệp trống.");
                try(InputStream header=new FileInputStream(temp)){
                    byte[] magic=new byte[8];int read=header.read(magic);
                    boolean png=read>=8&&(magic[0]&255)==137&&magic[1]=='P'&&magic[2]=='N'&&magic[3]=='G';
                    boolean jpg=read>=3&&(magic[0]&255)==255&&(magic[1]&255)==216&&(magic[2]&255)==255;
                    if(!(type.equals("image/png")&&png||type.equals("image/jpeg")&&jpg))throw new IOException("Nội dung tệp không đúng định dạng ảnh.");
                }
                android.graphics.BitmapFactory.Options bounds=new android.graphics.BitmapFactory.Options();bounds.inJustDecodeBounds=true;
                android.graphics.BitmapFactory.decodeFile(temp.getAbsolutePath(),bounds);
                if(bounds.outWidth<=0||bounds.outHeight<=0)throw new IOException("Ảnh hỏng hoặc không đọc được. Hãy chọn ảnh khác.");
                FirestoreEvidenceImage prepared=FirestoreEvidenceImage.prepare(temp,uid,name,kind);
                temp.delete();temp=null;
                new android.os.Handler(android.os.Looper.getMainLooper()).post(()->{
                    if(!uid.equals(owner))return;
                    TutorEvidenceDraft before=draft(supplement);
                    long others=before.files.stream().filter(f->!kind.equals("diploma")||!"diploma".equals(f.get("kind"))).count();
                    if(others>=4){busy=false;notice="";error="Tối đa 4 ảnh. Hãy xóa một ảnh trước.";changed();return;}
                    db.document((String)prepared.metadata.get("path")).set(prepared.document).continueWithTask(written->{
                        if(!written.isSuccessful())throw written.getException();
                        return TutorEvidenceAccess.read(prepared.metadata);
                    }).addOnCompleteListener(task->{
                        if(!uid.equals(owner))return;
                        if(!task.isSuccessful()){uploadFailed(task.getException());return;}
                        TutorEvidenceDraft draft=draft(supplement);if(kind.equals("diploma"))draft.files.removeIf(f->"diploma".equals(f.get("kind")));
                        draft.files.add(prepared.metadata);draft.save(context,uid);busy=false;notice="Đã nén và lưu ảnh minh chứng.";changed();
                    });
                });
            }catch(Exception e){if(temp!=null)temp.delete();String message=e.getMessage();new android.os.Handler(android.os.Looper.getMainLooper()).post(()->{if(uid.equals(owner)){busy=false;error=message;notice="";changed();}});}
        });
    }
    public void open(Context activity,Map<String,Object> file){
        if(!TutorEvidenceDraft.isFirestoreImage(file)){error="Ảnh này thuộc bản lưu cũ. Vui lòng tải lại ảnh minh chứng.";changed();return;}
        new androidx.appcompat.app.AlertDialog.Builder(activity).setTitle(text(file,"name"))
                .setView(new com.example.tmdt.ui.common.EvidenceImageView(activity,file)).setPositiveButton("Đóng",null).show();
    }
    private void close(){for(ListenerRegistration listener:new ListenerRegistration[]{profileListener,applicationListener,queueListener,detailListener})if(listener!=null)listener.remove();busy=false;admin=false;}
    @Override protected void onCleared(){close();io.shutdownNow();}
}
