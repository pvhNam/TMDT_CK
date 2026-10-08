package com.example.tmdt;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Registration input; prices always describe a 90-minute lesson. */
public final class TutorRegistration {
    public static final String[] LEVELS={"TIEU_HOC","THCS","THPT","LUYEN_THI","KHAC"};
    public static final String[] LEVEL_LABELS={"Tiểu học","THCS","THPT","Luyện thi","Khác"};
    public final String name,phone,subjectId,subjectName,mode;
    public final List<String> levels;
    public final int experience;
    public final long price;
    public TutorRegistration(String name,String phone,String subjectId,String subjectName,List<String> levels,int experience,String mode,long price){
        this.name=name.trim();this.phone=AccountValidation.localPhone(phone);this.subjectId=subjectId;
        this.subjectName=subjectName;this.levels=Collections.unmodifiableList(new ArrayList<>(levels));
        this.experience=experience;this.mode=mode;this.price=price;
    }
    public String error(){
        if(!AccountValidation.validName(name))return "Họ tên cần từ 2 đến 100 ký tự.";
        if(!AccountValidation.validPhone(phone))return "Vui lòng nhập số điện thoại hợp lệ.";
        if(subjectId.isEmpty()||subjectId.contains("/")||subjectId.length()>150)return "Vui lòng chọn môn giảng dạy.";
        if(levels.isEmpty()||levels.size()>5||new java.util.HashSet<>(levels).size()!=levels.size()
                ||!Arrays.asList(LEVELS).containsAll(levels))return "Vui lòng chọn ít nhất một cấp học.";
        if(experience<0||experience>60)return "Kinh nghiệm cần từ 0 đến 60 năm.";
        if(!Arrays.asList("TRUC_TUYEN","TAI_NHA","CA_HAI").contains(mode))return "Vui lòng chọn hình thức dạy.";
        if(price<50000||price>2000000)return "Học phí mỗi buổi 90 phút từ 50.000 đến 2.000.000 đồng.";
        return "";
    }
    public String levelLabel(){
        List<String> labels=new ArrayList<>();
        for(String level:levels){int i=Arrays.asList(LEVELS).indexOf(level);if(i>=0)labels.add(LEVEL_LABELS[i]);}
        return String.join(", ",labels);
    }
    public String modeLabel(){return "CA_HAI".equals(mode)?"Trực tuyến · Tại nhà":"TAI_NHA".equals(mode)?"Tại nhà":"Trực tuyến";}
}
