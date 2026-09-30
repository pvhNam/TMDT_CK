package com.example.tmdt;

import com.google.firebase.firestore.DocumentSnapshot;
import java.util.HashMap;
import java.util.Map;

public final class UserProfile {
    public String name = "", email = "", phone = "", level = "", region = "", goal = "", avatar = "";
    public String role = "STUDENT", address = "", status = "ACTIVE";
    public boolean tutor;
    static UserProfile from(DocumentSnapshot doc) {
        UserProfile p = new UserProfile();
        p.name = text(doc, "ho_ten"); p.email = text(doc, "email"); p.phone = text(doc, "so_dien_thoai");
        p.level = text(doc, "cap_hoc"); p.region = text(doc, "khu_vuc"); p.goal = text(doc, "muc_tieu_hoc_tap");
        p.avatar = text(doc, "anh_dai_dien"); p.address = text(doc, "dia_chi");
        String role = text(doc, "vai_tro"); if (!role.isEmpty()) p.role = role;
        p.status = text(doc, "trang_thai"); p.tutor = Boolean.TRUE.equals(doc.getBoolean("la_gia_su"));
        return p;
    }
    private static String text(DocumentSnapshot doc, String key) {
        Object value = doc.get(key); return value instanceof String ? (String) value : "";
    }
    Map<String,Object> fields() {
        Map<String,Object> data = new HashMap<>();
        data.put("ho_ten",name); data.put("email",email); data.put("so_dien_thoai",AccountValidation.localPhone(phone));
        data.put("cap_hoc",level); data.put("khu_vuc",region); data.put("muc_tieu_hoc_tap",goal);
        data.put("dia_chi",address); data.put("anh_dai_dien",avatar); return data;
    }
}
