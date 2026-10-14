package com.example.tmdt;

import android.util.Base64;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;

/** Local Auth emulator accepts unsigned Google fixtures. Never run these against production. */
@RunWith(AndroidJUnit4.class)
public class GoogleSignInTest {
    @BeforeClass public static void setup() {
        org.junit.Assume.assumeTrue("true".equals(InstrumentationRegistry.getArguments().getString("firebaseEmulators")));
        FirebaseTestEnvironment.configure();
    }
    private String encode(String value) {
        return Base64.encodeToString(value.getBytes(StandardCharsets.UTF_8),Base64.URL_SAFE|Base64.NO_WRAP|Base64.NO_PADDING);
    }
    private String token(String id,String email)throws Exception {
        long now=System.currentTimeMillis()/1000;
        JSONObject data=new JSONObject().put("sub",id).put("email",email).put("email_verified",true)
                .put("name","Học viên Google").put("iss","https://accounts.google.com")
                .put("aud","local-test.apps.googleusercontent.com").put("iat",now).put("exp",now+3600);
        return encode("{\"alg\":\"none\",\"typ\":\"JWT\"}")+"."+encode(data.toString())+".";
    }
    private void settled(ActivityScenario<MainActivity> scenario)throws Exception {
        AtomicBoolean done=new AtomicBoolean();long end=System.currentTimeMillis()+35000;
        do {
            scenario.onActivity(a->done.set(!a.account.isBusy()));
            if(done.get()){InstrumentationRegistry.getInstrumentation().waitForIdleSync();return;}
            Thread.sleep(100);
        }while(System.currentTimeMillis()<end);
        fail("Google Firebase request did not settle");
    }
    @Test public void googleAccountCompletesProfileAndReusesSavedData()throws Exception {
        FirebaseAuth.getInstance().signOut();
        String unique="google-"+System.currentTimeMillis(),email=unique+"@example.com",credential=token(unique,email);
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a->{a.show("login");a.getSupportFragmentManager().executePendingTransactions();});
            onView(withId(R.id.google_sign_in)).perform(scrollTo()).check(matches(isDisplayed()));
            CatalogFirebaseTest.screenshot("google-login");
            scenario.onActivity(a->{
                if(GoogleSignIn.clientId(a).isEmpty()) {
                    a.account.startGoogleSignIn(a);
                    assertTrue(a.account.error().contains("chưa được cấu hình"));
                    assertFalse(a.account.isBusy());assertFalse(a.account.signedIn());
                }
                a.account.signInWithGoogle(credential);
                a.account.signInWithGoogle(credential); // Repeated taps cannot launch a second request.
            });
            settled(scenario);
            scenario.onActivity(a->{
                assertEquals("",a.account.error());assertEquals("edit_account",a.screen);
                assertEquals(email,a.account.profile().email);assertEquals("Học viên Google",a.account.profile().name);
            });
            String uid=FirebaseAuth.getInstance().getCurrentUser().getUid();
            assertFalse(Tasks.await(FirebaseFirestore.getInstance().collection("users").document(uid).get(),10,TimeUnit.SECONDS).exists());
            scenario.recreate();
            scenario.onActivity(a->{
                assertEquals("edit_account",a.screen);
                UserProfile profile=new UserProfile();profile.email=email;profile.name="Tên đã chỉnh sửa";
                profile.phone="0912345678";profile.region="TP. Hồ Chí Minh";profile.goal="Ôn tập Toán";
                a.account.saveProfile(profile);
            });
            settled(scenario);
            scenario.onActivity(a->{assertEquals("",a.account.error());a.account.signOut();});
            scenario.onActivity(a->a.account.signInWithGoogle(credential));settled(scenario);
            assertEquals(uid,FirebaseAuth.getInstance().getCurrentUser().getUid());
            scenario.onActivity(a->{
                assertEquals("",a.account.error());assertEquals("home",a.screen);
                assertEquals("Tên đã chỉnh sửa",a.account.profile().name);
                assertEquals("Ôn tập Toán",a.account.profile().goal);
                assertEquals("STUDENT",a.account.profile().role);assertFalse(a.account.profile().tutor);
                a.account.signOut();a.account.signInWithGoogle("");
                assertFalse(a.account.isBusy());assertFalse(a.account.signedIn());assertFalse(a.account.error().isEmpty());
            });
        }finally {FirebaseAuth.getInstance().signOut();}
    }
}
