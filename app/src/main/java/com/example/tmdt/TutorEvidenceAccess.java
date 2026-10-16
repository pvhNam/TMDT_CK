package com.example.tmdt;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.*;
import java.util.*;

/** Verifies the server copy; a saved draft entry is not proof that its image is readable. */
public final class TutorEvidenceAccess {
    private TutorEvidenceAccess(){}
    private static final class EvidenceException extends Exception {
        EvidenceException(String message){super(message);}
        EvidenceException(String message,Exception cause){super(message,cause);}
    }
    private static String label(Map<String,Object> file){return "Ảnh “"+TutorReviewState.text(file,"name")+"”: ";}
    public static String message(Exception error){
        if(error instanceof EvidenceException)return error.getMessage();
        if(error instanceof FirebaseFirestoreException){
            FirebaseFirestoreException.Code code=((FirebaseFirestoreException)error).getCode();
            switch(code){
                case PERMISSION_DENIED:return "Firebase từ chối quyền đọc ảnh. Kiểm tra rules và trạng thái tài khoản trong dự án "+FirebaseApp.getInstance().getOptions().getProjectId()+". (PERMISSION_DENIED)";
                case UNAVAILABLE:case DEADLINE_EXCEEDED:return "Chưa kết nối được Firebase để kiểm tra ảnh. Kiểm tra mạng rồi thử lại. ("+code+")";
                case UNAUTHENTICATED:return "Phiên đăng nhập không hợp lệ. Hãy đăng nhập lại. (UNAUTHENTICATED)";
                case RESOURCE_EXHAUSTED:return "Firestore đã hết hạn mức thao tác hoặc dung lượng. (RESOURCE_EXHAUSTED)";
                default:return "Chưa đọc được ảnh từ Firebase. ("+code+")";
            }
        }
        return "Chưa kiểm tra được ảnh minh chứng. Vui lòng thử lại.";
    }
    public static Task<DocumentSnapshot> read(Map<String,Object> file){
        if(!TutorEvidenceDraft.isFirestoreImage(file))return Tasks.forException(new EvidenceException(label(file)+"Thuộc bản lưu cũ. Hãy chọn lại ảnh minh chứng."));
        String path=TutorReviewState.text(file,"path");
        return FirebaseFirestore.getInstance().document(path).get(Source.SERVER).continueWith(task->{
            if(!task.isSuccessful()){
                android.util.Log.w("TutorEvidence","Cannot read evidence image from Firestore",task.getException());
                throw new EvidenceException(label(file)+message(task.getException()),task.getException());
            }
            DocumentSnapshot doc=task.getResult();
            if(!doc.exists())throw new EvidenceException(label(file)+"Không còn tồn tại trên Firebase. Hãy quay lại Minh chứng, xóa mục này và chọn lại ảnh.");
            Object stored=doc.get("anh"),size=file.get("size"),serverSize=doc.get("size");
            if(!(stored instanceof Blob))throw new EvidenceException(label(file)+"Chưa có dữ liệu ảnh trên Firebase. Hãy chọn lại ảnh.");
            int bytes=((Blob)stored).toBytes().length;
            if(bytes<=0||bytes>FirestoreEvidenceImage.MAX_BYTES)throw new EvidenceException(label(file)+"Dữ liệu ảnh không hợp lệ. Hãy chọn lại ảnh.");
            if(!path.split("/")[1].equals(doc.getString("user_id"))
                    ||!Objects.equals(file.get("name"),doc.get("name"))
                    ||!Objects.equals(file.get("type"),doc.get("type"))
                    ||!Objects.equals(file.get("kind"),doc.get("kind"))
                    ||!(size instanceof Number)||!(serverSize instanceof Number)
                    ||((Number)size).longValue()!=bytes||((Number)serverSize).longValue()!=bytes)
                throw new EvidenceException(label(file)+"Thông tin bản nháp không khớp ảnh đã lưu. Hãy xóa mục này và chọn lại ảnh.");
            return doc;
        });
    }
    public static Task<Void> verify(List<Map<String,Object>> files,String owner){
        if(files.isEmpty()||files.size()>4)return Tasks.forException(new EvidenceException("Hồ sơ cần từ 1 đến 4 ảnh minh chứng."));
        for(Map<String,Object> file:files)if(!TutorReviewState.text(file,"path").startsWith("tutor_applications/"+owner+"/evidence_images/"))
            return Tasks.forException(new EvidenceException(label(file)+"Không thuộc tài khoản đang đăng nhập. Hãy chọn lại ảnh."));
        List<Task<?>> reads=new ArrayList<>();for(Map<String,Object> file:files)reads.add(read(file));
        return Tasks.whenAll(reads).continueWith(done->{
            for(Task<?> read:reads)if(!read.isSuccessful())throw read.getException();
            return null;
        });
    }
}
