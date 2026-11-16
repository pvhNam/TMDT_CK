package com.example.tmdt;

import android.content.Context;
import com.google.firebase.FirebaseApp;
import org.json.JSONObject;

/** Non-secret profile fields only; passwords and OTPs are never written to disk. */
final class PendingEmailRegistration {
    private static android.content.SharedPreferences prefs(){return FirebaseApp.getInstance().getApplicationContext().getSharedPreferences("pending_email_registration",Context.MODE_PRIVATE);}
    static void save(String uid,UserProfile profile){prefs().edit().putString(uid,new JSONObject(profile.fields()).toString()).apply();}
    static UserProfile load(String uid,String email){
        UserProfile p=new UserProfile();p.email=email;
        try{
            JSONObject d=new JSONObject(prefs().getString(uid,"{}"));
            p.name=d.optString("ho_ten");p.phone=d.optString("so_dien_thoai");p.level=d.optString("cap_hoc");
            p.region=d.optString("khu_vuc");p.goal=d.optString("muc_tieu_hoc_tap");p.address=d.optString("dia_chi");
            p.age=d.optInt("tuoi");p.grade=d.optString("lop");
        }catch(Exception ignored){}
        return p;
    }
    static void emailSent(String uid){prefs().edit().putLong(uid+".email_sent_at",System.currentTimeMillis()).apply();}
    static long resendRemaining(String uid){
        long sent=prefs().getLong(uid+".email_sent_at",0);
        return sent==0?0:Math.max(0,Math.min(60000,sent+60000-System.currentTimeMillis()));
    }
    static void clear(String uid){prefs().edit().remove(uid).remove(uid+".email_sent_at").apply();}
}
