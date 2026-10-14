package com.example.tmdt;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;

@RunWith(AndroidJUnit4.class)
public class TutorRegistrationTest {
    @BeforeClass public static void setup(){
        org.junit.Assume.assumeTrue("true".equals(InstrumentationRegistry.getArguments().getString("firebaseEmulators")));
        FirebaseTestEnvironment.configure();
    }
    private void await(ActivityScenario<MainActivity> scenario,Predicate<MainActivity> condition)throws Exception{
        AtomicBoolean ready=new AtomicBoolean();long end=System.currentTimeMillis()+30000;
        do{scenario.onActivity(a->ready.set(condition.test(a)));if(ready.get())return;Thread.sleep(100);}while(System.currentTimeMillis()<end);
        scenario.onActivity(a->fail("Timed out on "+a.screen+": "+a.account.error()));
    }
    @Test public void registerReviewPersistAndSwitchModes()throws Exception{
        FirebaseAuth auth=FirebaseAuth.getInstance();FirebaseFirestore db=FirebaseFirestore.getInstance();
        String email="tutor-ui-"+System.currentTimeMillis()+"@example.com",password="HocTap123!";
        Tasks.await(auth.createUserWithEmailAndPassword(email,password),15,TimeUnit.SECONDS);
        String uid=auth.getCurrentUser().getUid();
        UserProfile user=new UserProfile();user.name="Phạm Nam";user.email=email;user.phone="0912345678";user.region="TP. Hồ Chí Minh";
        Map<String,Object> data=user.fields();data.put("vai_tro","STUDENT");data.put("la_gia_su",false);data.put("trang_thai","ACTIVE");
        data.put("created_at",FieldValue.serverTimestamp());data.put("updated_at",FieldValue.serverTimestamp());
        Tasks.await(db.collection("users").document(uid).set(data),15,TimeUnit.SECONDS);
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            await(scenario,a->a.account.profile()!=null&&!a.account.isBusy()&&a.catalog.subjects().containsKey("toan"));
            scenario.onActivity(a->a.show("account"));
            onView(withId(R.id.tutor_action)).perform(scrollTo()).check(matches(withText("Đăng ký làm gia sư")));
            CatalogFirebaseTest.screenshot("tutor-account-before");
            onView(withId(R.id.tutor_action)).perform(click());
            onView(withId(R.id.continue_button)).perform(scrollTo(),click());
            scenario.onActivity(a->assertEquals("register_tutor",a.screen));
            onView(withId(R.id.subject)).perform(scrollTo(),click());onView(withText("Toán")).perform(click());
            onView(withId(R.id.levels)).perform(scrollTo(),click());onView(withText("THPT")).perform(click());
            onView(withText("OK")).perform(click());
            onView(withId(R.id.experience)).perform(scrollTo(),replaceText("2"),closeSoftKeyboard());
            onView(withId(R.id.price)).perform(scrollTo(),replaceText("150000"),closeSoftKeyboard());
            scenario.recreate();
            onView(withId(R.id.price)).check(matches(withText("150000")));
            scenario.onActivity(a->{a.account.reportError("");a.getSupportFragmentManager().executePendingTransactions();((android.widget.ScrollView)a.findViewById(R.id.account_scroll)).fullScroll(android.view.View.FOCUS_UP);});
            CatalogFirebaseTest.screenshot("tutor-registration");
            onView(withId(R.id.continue_button)).perform(scrollTo(),click());
            CatalogFirebaseTest.screenshot("tutor-review");
            onView(withId(R.id.edit_again)).perform(scrollTo(),click());
            onView(withId(R.id.price)).check(matches(withText("150000")));
            onView(withId(R.id.continue_button)).perform(scrollTo(),click());
            onView(withId(R.id.submit)).perform(scrollTo(),click());
            scenario.onActivity(a->assertEquals("review_tutor",a.screen));
            onView(withId(R.id.confirm)).perform(scrollTo(),click());
            onView(withId(R.id.submit)).perform(scrollTo(),click());
            await(scenario,a->!a.account.isBusy()&&a.account.profile()!=null&&a.account.profile().tutor&&a.screen.equals("account"));
            assertEquals("PENDING",Tasks.await(db.collection("tutor_profiles").document(uid).get(),10,TimeUnit.SECONDS).getString("trang_thai_xac_minh"));
            assertEquals(Long.valueOf(150000),Tasks.await(db.collection("tutor_subjects").document(uid+"_toan_THPT").get(),10,TimeUnit.SECONDS).getLong("hoc_phi_moi_buoi"));
            onView(withId(R.id.tutor_action)).perform(scrollTo()).check(matches(withText("Chuyển qua gia sư")));
            CatalogFirebaseTest.screenshot("tutor-account-after");
            onView(withId(R.id.tutor_action)).perform(click());
            scenario.onActivity(a->{assertTrue(a.classroom.tutorMode());assertEquals("tutorHome",a.screen);});
            scenario.recreate();
            scenario.onActivity(a->{assertTrue(a.classroom.tutorMode());a.show("account");});
            onView(withId(R.id.tutor_action)).perform(scrollTo()).check(matches(withText("Chuyển qua học viên"))).perform(click());
            scenario.onActivity(a->{assertFalse(a.classroom.tutorMode());a.account.signOut();a.account.signIn(email,password);});
            await(scenario,a->!a.account.isBusy()&&a.account.profile()!=null);
            scenario.onActivity(a->{assertTrue(a.account.profile().tutor);a.show("account");});
            onView(withId(R.id.tutor_action)).perform(scrollTo()).check(matches(withText("Chuyển qua gia sư")));
        }finally{auth.signOut();}
    }
}
