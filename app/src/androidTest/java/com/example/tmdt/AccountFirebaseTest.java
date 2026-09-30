package com.example.tmdt;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;

/** Opt-in integration test. It only communicates with the local Firebase Emulator Suite. */
@RunWith(AndroidJUnit4.class)
public class AccountFirebaseTest {
    @BeforeClass public static void connectEmulators() {
        org.junit.Assume.assumeTrue("true".equals(InstrumentationRegistry.getArguments().getString("firebaseEmulators")));
        FirebaseTestEnvironment.configure();
    }
    private void await(ActivityScenario<MainActivity> scenario, Predicate<AccountState> done) throws Exception {
        long deadline=System.currentTimeMillis()+35000;
        AtomicBoolean ready=new AtomicBoolean();
        do {
            scenario.onActivity(a->ready.set(done.test(a.account)));
            if(ready.get())return;
            Thread.sleep(100);
        } while(System.currentTimeMillis()<deadline);
        scenario.onActivity(a->fail("Timed out: busy="+a.account.busy+", error="+a.account.error+", screen="+a.screen));
    }
    @Test public void realAuthOtpProfileAndSecurityRules() throws Exception {
        String email="student"+System.currentTimeMillis()+"@example.com";
        String password="HocTap123!";
        String phone="+849"+String.format(java.util.Locale.ROOT,"%08d",System.currentTimeMillis()%100000000);
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            UserProfile profile=new UserProfile();profile.name="Học viên kiểm thử";profile.email=email;profile.phone=phone;
            scenario.onActivity(a->a.account.register(profile,password));
            await(scenario,s->!s.busy);
            scenario.onActivity(a->{
                com.google.android.material.bottomnavigation.BottomNavigationView nav=a.findViewById(R.id.bottom_navigation);
                assertFalse(nav.getMenu().findItem(R.id.nav_login).isVisible());
                assertFalse(nav.getMenu().findItem(R.id.nav_register).isVisible());
                assertTrue(nav.getMenu().findItem(R.id.nav_account).isVisible());
                assertEquals(R.id.nav_account,nav.getSelectedItemId());
            });
            scenario.onActivity(a->{assertEquals("",a.account.error);assertEquals("account",a.screen);a.account.sendCode(a,phone,false);});
            await(scenario,s->!s.sendingCode);
            scenario.onActivity(a->{assertEquals("",a.account.error);assertNotNull(a.account.verificationId);});
            scenario.recreate();
            String project=FirebaseApp.getInstance().getOptions().getProjectId();
            HttpURLConnection connection=(HttpURLConnection)new URL("http://10.0.2.2:9099/emulator/v1/projects/"+project+"/verificationCodes").openConnection();
            connection.setConnectTimeout(5000);connection.setReadTimeout(5000);
            String json;
            try(java.io.InputStream stream=connection.getInputStream()){json=new String(stream.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);}finally{connection.disconnect();}
            JSONArray codes=new JSONObject(json).getJSONArray("verificationCodes");String code=null;
            for(int i=0;i<codes.length();i++)if(phone.equals(codes.getJSONObject(i).getString("phoneNumber")))code=codes.getJSONObject(i).getString("code");
            assertNotNull(code);final String correct=code;
            scenario.onActivity(a->a.account.verifyCode(correct.equals("000000")?"111111":"000000"));
            await(scenario,s->!s.busy);
            scenario.onActivity(a->{assertTrue(a.account.error.contains("OTP"));a.account.verifyCode(correct);});
            await(scenario,s->!s.signedIn());
            scenario.onActivity(a->{assertEquals("login",a.screen);a.account.signIn(email,"WrongPassword!");});
            await(scenario,s->!s.busy);
            scenario.onActivity(a->{assertFalse(a.account.signedIn());assertFalse(a.account.error.isEmpty());a.account.signIn(email,password);});
            await(scenario,s->!s.busy);
            scenario.onActivity(a->{assertEquals("",a.account.error);assertTrue(a.account.verified());assertEquals("home",a.screen);});
            String uid=FirebaseAuth.getInstance().getCurrentUser().getUid();
            assertEquals("STUDENT",Tasks.await(FirebaseFirestore.getInstance().collection("users").document(uid).get(),10,TimeUnit.SECONDS).getString("vai_tro"));
            try { Tasks.await(FirebaseFirestore.getInstance().collection("users").document(uid).update("vai_tro","ADMIN"),10,TimeUnit.SECONDS);fail("Role escalation allowed"); }
            catch(java.util.concurrent.ExecutionException expected){assertTrue(expected.getCause().getMessage().contains("PERMISSION_DENIED"));}
            try { Tasks.await(FirebaseFirestore.getInstance().collection("users").document("another-user").get(),10,TimeUnit.SECONDS);fail("Cross-user read allowed"); }
            catch(java.util.concurrent.ExecutionException expected){assertTrue(expected.getCause().getMessage().contains("PERMISSION_DENIED"));}
            onView(withId(R.id.nav_account)).perform(click());
            onView(withContentDescription("Chỉnh sửa hồ sơ")).perform(scrollTo(),click());
            onView(withId(R.id.name)).perform(scrollTo(),replaceText("Nam đã cập nhật"),closeSoftKeyboard());
            onView(withId(R.id.address)).perform(scrollTo(),replaceText("Quận 1"),closeSoftKeyboard());
            onView(withId(R.id.region)).perform(scrollTo(),replaceText("TP. Hồ Chí Minh"),closeSoftKeyboard());
            onView(withId(R.id.goal)).perform(scrollTo(),replaceText("Củng cố kiến thức Toán"),closeSoftKeyboard());
            scenario.recreate();
            onView(withContentDescription("Lưu thay đổi")).perform(scrollTo(),click());
            await(scenario,s->!s.busy);
            scenario.onActivity(a->{assertEquals("",a.account.error);assertEquals("Nam đã cập nhật",a.account.profile.name);});
            scenario.onActivity(a->a.show("account"));
            CatalogFirebaseTest.screenshot("account");
            scenario.onActivity(a->a.show("edit_account"));
            CatalogFirebaseTest.screenshot("edit-account");
            scenario.recreate();
            scenario.onActivity(a->{assertEquals("Nam đã cập nhật",a.account.profile.name);assertEquals("Quận 1",a.account.profile.address);a.account.resetPassword(email);});
            await(scenario,s->!s.busy);
            scenario.onActivity(a->{assertEquals("",a.account.error);assertFalse(a.account.notice.isEmpty());a.account.signOut();a.account.register(profile,password);});
            await(scenario,s->!s.busy);
            scenario.onActivity(a->{assertTrue(a.account.error.contains("đã được đăng ký"));assertFalse(a.account.signedIn());});
            UserProfile second=new UserProfile();second.name="Người học thứ hai";second.email="second"+email;second.phone="+84812345678";
            scenario.onActivity(a->a.account.register(second,password));
            await(scenario,s->!s.busy);
            scenario.onActivity(a->{assertEquals("",a.account.error);assertEquals("Người học thứ hai",a.account.profile.name);a.account.signOut();a.account.signIn(email,password);});
            await(scenario,s->!s.busy);
            scenario.onActivity(a->{assertEquals("Nam đã cập nhật",a.account.profile.name);assertEquals("Quận 1",a.account.profile.address);a.account.signOut();});
        } finally {FirebaseAuth.getInstance().signOut();}
    }
}




