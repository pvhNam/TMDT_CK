package com.example.tmdt;

import android.content.Context;
import android.content.SharedPreferences;

/** Installation-local introduction and registration intent; never stores credentials. */
public final class Onboarding {
    private final SharedPreferences prefs;
    public Onboarding(Context context) { prefs=context.getSharedPreferences("onboarding",Context.MODE_PRIVATE); }
    public boolean introduced() { return prefs.getBoolean("introduced",false); }
    public boolean pending() { return prefs.getBoolean("pending",false); }
    public boolean teacher() { return "teacher".equals(prefs.getString("role","")); }
    public String level() { return prefs.getString("level",""); }
    public String grade() { return prefs.getString("grade",""); }
    public String age() { return prefs.getString("age",""); }
    public void studentDraft(String level,String grade,String age) {
        prefs.edit().putString("role","student").putString("level",level)
                .putString("grade",grade).putString("age",age).apply();
    }
    public void register(boolean teacher) {
        prefs.edit().putBoolean("introduced",true).putBoolean("pending",true)
                .putString("role",teacher?"teacher":"student").apply();
    }
    public void existingAccount() { prefs.edit().putBoolean("introduced",true).putBoolean("pending",false).apply(); }
    public void applyStudent(UserProfile profile) {
        if(!pending()||teacher())return;
        profile.level=level();profile.grade=grade();
        try { profile.age=Integer.parseInt(age()); } catch(NumberFormatException ignored) { profile.age=0; }
    }
    public String finish(UserProfile profile,String fallback) {
        if(!pending())return fallback;
        boolean tutor=teacher()&&profile!=null&&!profile.tutor;
        prefs.edit().putBoolean("pending",false).remove("level").remove("grade").remove("age").apply();
        return tutor?"register_tutor":fallback;
    }
    public static String[] grades(String level) {
        int start,end;
        switch(level) {
            case "Tiểu học":start=1;end=5;break;
            case "THCS":start=6;end=9;break;
            case "THPT":start=10;end=12;break;
            case "Đại học":return new String[]{"Năm 1","Năm 2","Năm 3","Năm 4","Năm 5","Năm 6"};
            default:return new String[]{"Không áp dụng"};
        }
        String[] values=new String[end-start+1];
        for(int i=start;i<=end;i++)values[i-start]="Lớp "+i;
        return values;
    }
}
