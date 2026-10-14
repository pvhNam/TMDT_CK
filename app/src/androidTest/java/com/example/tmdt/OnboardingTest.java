package com.example.tmdt;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;

@RunWith(AndroidJUnit4.class)
public class OnboardingTest {
    @BeforeClass public static void configure() {
        Assume.assumeTrue("true".equals(InstrumentationRegistry.getArguments().getString("firebaseEmulators")));
        FirebaseTestEnvironment.configure();
    }
    @Before public void freshInstall() {
        FirebaseAuth.getInstance().signOut();
        InstrumentationRegistry.getInstrumentation().getTargetContext().getSharedPreferences("onboarding",0).edit().clear().commit();
    }
    @After public void cleanup(){FirebaseAuth.getInstance().signOut();}
    private void await(ActivityScenario<MainActivity> scenario,Predicate<MainActivity> condition)throws Exception{
        AtomicBoolean ready=new AtomicBoolean();long end=System.currentTimeMillis()+20000;
        do{scenario.onActivity(a->ready.set(condition.test(a)));if(ready.get())return;Thread.sleep(100);}while(System.currentTimeMillis()<end);
        scenario.onActivity(a->fail("Timed out on "+a.screen+": "+a.account.error()));
    }
    private void createAccount() {
        onView(withId(R.id.name)).perform(scrollTo(),replaceText("Nguyễn Minh An"));
        onView(withId(R.id.email)).perform(scrollTo(),replaceText("intro-"+System.nanoTime()+"@example.com"));
        onView(withId(R.id.phone)).perform(scrollTo(),replaceText("0912345678"));
        onView(withId(R.id.password)).perform(scrollTo(),replaceText("HocTap123!"));
        onView(withId(R.id.confirm)).perform(scrollTo(),replaceText("HocTap123!"));
        onView(withId(R.id.terms)).perform(scrollTo(),click());
        onView(withId(R.id.register)).perform(scrollTo(),click());
    }
    @Test public void studentInformationSurvivesRotationAndRegistrationAndRelaunch()throws Exception {
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            onView(withText("Chào mừng bạn!")).check(matches(isDisplayed()));
            CatalogFirebaseTest.screenshot("onboarding-welcome");
            onView(withContentDescription("Tôi là học sinh")).perform(scrollTo(),click());
            onView(withContentDescription("Tiếp tục đăng ký")).perform(scrollTo(),click());
            onView(withId(R.id.onboarding_age)).check(matches(hasErrorText("Vui lòng nhập tuổi từ 5 đến 100")));
            onView(withContentDescription("Chọn cấp học")).perform(scrollTo(),click());onView(withText("THCS")).perform(click());
            onView(withId(R.id.onboarding_age)).perform(scrollTo(),replaceText("13"),closeSoftKeyboard());
            onView(withContentDescription("Chọn lớp / năm học")).perform(scrollTo(),click());onView(withText("Lớp 8")).perform(click());
            scenario.recreate();
            onView(withId(R.id.onboarding_age)).check(matches(withText("13")));
            onView(withText("Lớp 8")).check(matches(isDisplayed()));
            CatalogFirebaseTest.screenshot("onboarding-student");
            onView(withContentDescription("Tiếp tục đăng ký")).perform(scrollTo(),click());
            scenario.onActivity(a->a.back());
            onView(withText("Lớp 8")).check(matches(isDisplayed()));
            onView(withContentDescription("Tiếp tục đăng ký")).perform(scrollTo(),click());
            createAccount();await(scenario,a->!a.account.isBusy()&&"account".equals(a.screen));
            scenario.onActivity(a->{assertEquals(13,a.account.profile().age);assertEquals("Lớp 8",a.account.profile().grade);});
            String uid=FirebaseAuth.getInstance().getCurrentUser().getUid();
            com.google.firebase.firestore.DocumentSnapshot doc=Tasks.await(FirebaseFirestore.getInstance().collection("users").document(uid).get(Source.SERVER),10,TimeUnit.SECONDS);
            assertEquals(Long.valueOf(13),doc.getLong("tuoi"));assertEquals("Lớp 8",doc.getString("lop"));assertEquals("THCS",doc.getString("cap_hoc"));
            scenario.onActivity(a->a.account.signOut());
        }
        try(ActivityScenario<MainActivity> reopened=ActivityScenario.launch(MainActivity.class)) {
            reopened.onActivity(a->assertEquals("home",a.screen));
        }
    }
    @Test public void teacherIntentSurvivesRestartAndOpensTutorFormAfterAccountCreation()throws Exception {
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            onView(withContentDescription("Tôi là giáo viên")).perform(scrollTo(),click());
            onView(withId(R.id.register_heading)).check(matches(withText("Tạo tài khoản giáo viên")));
            CatalogFirebaseTest.screenshot("onboarding-teacher-account");
        }
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a->assertEquals("register",a.screen));
            createAccount();await(scenario,a->!a.account.isBusy()&&"register_tutor".equals(a.screen));
            onView(withId(R.id.name)).check(matches(withText("Nguyễn Minh An")));
            scenario.onActivity(a->assertFalse(a.account.profile().tutor));
            CatalogFirebaseTest.screenshot("onboarding-teacher-profile");
        }
    }
    @Test public void interruptedTeacherProfileIsCompletedBeforeTutorRegistration()throws Exception {
        new Onboarding(InstrumentationRegistry.getInstrumentation().getTargetContext()).register(true);
        Tasks.await(FirebaseAuth.getInstance().createUserWithEmailAndPassword("intro-recover-"+System.nanoTime()+"@example.com","HocTap123!"),10,TimeUnit.SECONDS);
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            await(scenario,a->!a.account.isBusy()&&"edit_account".equals(a.screen));
            onView(withId(R.id.name)).perform(scrollTo(),replaceText("Giáo viên mới"),closeSoftKeyboard());
            onView(withId(R.id.phone)).perform(scrollTo(),replaceText("0912345678"),closeSoftKeyboard());
            onView(withId(R.id.save)).perform(scrollTo(),click());
            await(scenario,a->!a.account.isBusy()&&"register_tutor".equals(a.screen));
        }
    }
}
