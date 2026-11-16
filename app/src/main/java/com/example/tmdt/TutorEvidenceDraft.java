package com.example.tmdt;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

/** Only metadata for successfully uploaded private files is kept in the local draft. */
public final class TutorEvidenceDraft {
    public String school="",major="",note="";
    public final List<Map<String,Object>> files=new ArrayList<>();
    public static TutorEvidenceDraft load(Context context,String uid) {
        TutorEvidenceDraft value=new TutorEvidenceDraft();
        try {
            JSONObject json=new JSONObject(context.getSharedPreferences("tutor_evidence",0).getString(uid,"{}"));
            value.school=json.optString("school");value.major=json.optString("major");value.note=json.optString("note");
            JSONArray files=json.optJSONArray("files");
            if(files!=null)for(int i=0;i<files.length();i++) {
                JSONObject f=files.getJSONObject(i);Map<String,Object> file=new HashMap<>();
                file.put("name",f.getString("name"));file.put("path",f.getString("path"));
                file.put("type",f.getString("type"));file.put("size",f.getLong("size"));file.put("kind",f.getString("kind"));value.files.add(file);
            }
        }catch(Exception ignored){}
        return value;
    }
    public void save(Context context,String uid) {
        try {
            JSONObject json=new JSONObject();json.put("school",school);json.put("major",major);json.put("note",note);
            json.put("files",new JSONArray(files));context.getSharedPreferences("tutor_evidence",0).edit().putString(uid,json.toString()).apply();
        }catch(Exception e){throw new IllegalStateException(e);}
    }
    public static void clear(Context context,String uid){context.getSharedPreferences("tutor_evidence",0).edit().remove(uid).apply();}
    public boolean hasDiploma(){return files.stream().anyMatch(f->"diploma".equals(f.get("kind"))&&isFirestoreImage(f));}
    public static boolean isImage(Map<String,Object> file){return Arrays.asList("image/jpeg","image/png").contains(file.get("type"));}
    public static boolean isFirestoreImage(Map<String,Object> file){return "image/jpeg".equals(file.get("type"))&&String.valueOf(file.get("path")).matches("tutor_applications/[^/]+/evidence_images/[a-zA-Z0-9-]+");}
    public String error(){
        if(school.trim().isEmpty()||school.length()>150)return "Vui lòng nhập trường hoặc đơn vị (tối đa 150 ký tự).";
        if(major.trim().isEmpty()||major.length()>150)return "Vui lòng nhập chuyên ngành (tối đa 150 ký tự).";
        if(!hasDiploma())return "Vui lòng gửi ảnh JPG/PNG chụp bằng cấp hoặc thẻ sinh viên để admin xác nhận.";
        if(files.stream().anyMatch(f->!isFirestoreImage(f)))return "Ảnh thuộc bản lưu cũ hoặc sai định dạng. Vui lòng xóa và tải lại để lưu bằng Firestore.";
        if(files.size()>4||note.length()>500)return "Tối đa 4 tệp và 500 ký tự ghi chú.";
        return "";
    }
    public Map<String,Object> fields(){
        Map<String,Object> data=new HashMap<>();data.put("truong",school.trim());data.put("chuyen_nganh",major.trim());
        data.put("ghi_chu",note.trim());data.put("minh_chung",new ArrayList<>(files));return data;
    }
}
