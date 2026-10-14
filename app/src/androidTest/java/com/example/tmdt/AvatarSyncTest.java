package com.example.tmdt;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.tmdt.ui.common.AvatarView;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class AvatarSyncTest {
    @BeforeClass public static void setup(){
        org.junit.Assume.assumeTrue("true".equals(InstrumentationRegistry.getArguments().getString("firebaseEmulators")));
        FirebaseTestEnvironment.configure();
    }
    private void await(ActivityScenario<MainActivity> scenario)throws Exception{
        AtomicBoolean ready=new AtomicBoolean();long end=System.currentTimeMillis()+30000;
        do{scenario.onActivity(a->ready.set(!a.account.isBusy()));if(ready.get())break;Thread.sleep(100);}while(System.currentTimeMillis()<end);
        scenario.onActivity(a->{assertFalse(a.account.isBusy());assertEquals("",a.account.error());a.getSupportFragmentManager().executePendingTransactions();});
    }
    private String image(int color){
        Bitmap bitmap=Bitmap.createBitmap(32,32,Bitmap.Config.ARGB_8888);bitmap.eraseColor(color);
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();bitmap.compress(Bitmap.CompressFormat.PNG,100,bytes);bitmap.recycle();
        return Base64.encodeToString(bytes.toByteArray(),Base64.NO_WRAP);
    }
    private AvatarView firstAvatar(View root){
        if(root instanceof AvatarView)return (AvatarView)root;
        if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++){
            AvatarView found=firstAvatar(((ViewGroup)root).getChildAt(i));if(found!=null)return found;
        }
        return null;
    }
    private void assertPhoto(View root,int color){
        ImageView photo=root.findViewById(R.id.avatar_photo);assertNotNull(photo);assertEquals(View.VISIBLE,photo.getVisibility());
        assertEquals(color,((BitmapDrawable)photo.getDrawable()).getBitmap().getPixel(0,0));assertTrue(photo.getClipToOutline());
    }
    @Test public void savedPhotoFollowsHomeTutorRotationAndSignOut()throws Exception{
        FirebaseAuth.getInstance().signOut();
        String red=image(Color.RED),blue=image(Color.BLUE);
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            UserProfile profile=new UserProfile();profile.name="Nam Avatar";profile.email="avatar-ui-"+System.currentTimeMillis()+"@example.com";profile.phone="0912345678";
            scenario.onActivity(a->a.account.register(profile,"HocTap123!"));await(scenario);
            scenario.onActivity(a->{profile.avatar=red;a.account.saveProfile(profile);});await(scenario);
            String uid=FirebaseAuth.getInstance().getCurrentUser().getUid();
            assertEquals(red,Tasks.await(FirebaseFirestore.getInstance().collection("profile_avatars").document(uid).get(),10,TimeUnit.SECONDS).getString("anh_dai_dien"));
            scenario.onActivity(a->{a.show("home");a.getSupportFragmentManager().executePendingTransactions();});
            scenario.onActivity(a->assertPhoto(a.findViewById(R.id.account),Color.RED));
            CatalogFirebaseTest.screenshot("avatar-home");
            scenario.recreate();
            scenario.onActivity(a->assertPhoto(a.findViewById(R.id.account),Color.RED));
            scenario.onActivity(a->{a.account.reviewTutor(new TutorRegistration(profile.name,profile.phone,"toan","Toán",Collections.singletonList("THPT"),2,"TRUC_TUYEN",150000));a.account.submitTutor();});await(scenario);
            scenario.onActivity(a->{a.classroom.setTutorMode(true);a.getSupportFragmentManager().executePendingTransactions();});
            scenario.onActivity(a->{assertEquals("tutorHome",a.screen);assertPhoto(firstAvatar(a.findViewById(R.id.screen_container)),Color.RED);});
            CatalogFirebaseTest.screenshot("avatar-tutor-home");
            scenario.onActivity(a->{profile.avatar=blue;a.account.saveProfile(profile);});await(scenario);
            scenario.onActivity(a->{a.show("tutorHome");a.getSupportFragmentManager().executePendingTransactions();});
            scenario.onActivity(a->assertPhoto(firstAvatar(a.findViewById(R.id.screen_container)),Color.BLUE));
            scenario.onActivity(a->{a.account.signOut();a.show("home");a.getSupportFragmentManager().executePendingTransactions();});
            scenario.onActivity(a->assertEquals(View.GONE,((View)a.findViewById(R.id.account)).findViewById(R.id.avatar_photo).getVisibility()));
            UserProfile other=new UserProfile();other.name="Học viên khác";other.email="avatar-viewer-"+System.currentTimeMillis()+"@example.com";other.phone="0987654321";
            scenario.onActivity(a->a.account.register(other,"HocTap123!"));await(scenario);
            scenario.onActivity(a->{a.show("home");a.getSupportFragmentManager().executePendingTransactions();});
            scenario.onActivity(a->((AvatarView)a.findViewById(R.id.account)).bind(uid,profile.name));
            AtomicBoolean loaded=new AtomicBoolean();long deadline=System.currentTimeMillis()+15000;
            do{
                scenario.onActivity(a->loaded.set(a.findViewById(R.id.account).findViewById(R.id.avatar_photo).getVisibility()==View.VISIBLE));
                if(loaded.get())break;Thread.sleep(100);
            }while(System.currentTimeMillis()<deadline);
            scenario.onActivity(a->{
                AvatarView avatar=a.findViewById(R.id.account);assertPhoto(avatar,Color.BLUE);
                avatar.bind("missing-avatar-"+uid,"Chưa có ảnh");
                assertEquals(View.GONE,avatar.findViewById(R.id.avatar_photo).getVisibility());
            });
        }finally{FirebaseAuth.getInstance().signOut();}
    }
}
