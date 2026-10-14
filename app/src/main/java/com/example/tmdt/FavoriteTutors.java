package com.example.tmdt;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Tutors the signed-in student saved with the heart button. Kept on the device, one list per account. */
public final class FavoriteTutors {
    private FavoriteTutors(){}
    private static SharedPreferences prefs(Context context){return context.getApplicationContext().getSharedPreferences("favorite_tutors",Context.MODE_PRIVATE);}
    private static String key(){
        FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();
        return user==null?"":"ids_"+user.getUid();
    }
    private static Set<String> load(Context context){
        Set<String> ids=new LinkedHashSet<>();String key=key();
        if(key.isEmpty())return ids;
        for(String id:prefs(context).getString(key,"").split(","))if(!id.isEmpty())ids.add(id);
        return ids;
    }
    private static void save(Context context,Set<String> ids){
        String key=key();if(key.isEmpty())return;
        prefs(context).edit().putString(key,String.join(",",ids)).apply();
    }
    public static List<String> ids(Context context){return new ArrayList<>(load(context));}
    public static int count(Context context){return load(context).size();}
    public static boolean contains(Context context,String tutorId){return load(context).contains(tutorId);}
    /** Saves or removes the tutor and returns whether the tutor is saved now. */
    public static boolean toggle(Context context,String tutorId){
        Set<String> ids=load(context);boolean saved=!ids.remove(tutorId);
        if(saved)ids.add(tutorId);
        save(context,ids);return saved;
    }
}
