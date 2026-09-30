package com.example.tmdt;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.firebase.auth.FirebaseAuth;
import org.junit.Test;
import org.junit.runner.RunWith;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class AccountUiTest {
    @org.junit.BeforeClass public static void configure(){FirebaseTestEnvironment.configure();}
    @Test public void registrationValidatesAndPreservesDraftAcrossRotation() {
        org.junit.Assume.assumeTrue(FirebaseAuth.getInstance().getCurrentUser()==null);
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a->a.show("register"));
            onView(withContentDescription("Tạo tài khoản")).perform(scrollTo(),click());
            onView(withId(R.id.name)).check(matches(hasErrorText("Họ tên cần từ 2 đến 100 ký tự")));
            onView(withId(R.id.name)).perform(scrollTo(),replaceText("Test Student"),closeSoftKeyboard());
            onView(withId(R.id.email)).perform(scrollTo(),replaceText("not-email"),closeSoftKeyboard());
            scenario.recreate();
            onView(withId(R.id.name)).check(matches(withText("Test Student")));
            onView(withContentDescription("Tạo tài khoản")).perform(scrollTo(),click());
            onView(withId(R.id.email)).check(matches(hasErrorText("Vui lòng nhập email hợp lệ")));
            assertNull(FirebaseAuth.getInstance().getCurrentUser());
        }
    }
    @Test public void guestCanOpenLoginAndPasswordReset() {
        org.junit.Assume.assumeTrue(FirebaseAuth.getInstance().getCurrentUser()==null);
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.nav_login)).perform(click());
            onView(withId(R.id.sign_in)).perform(scrollTo(),click());
            onView(withId(R.id.email)).check(matches(hasErrorText("Vui lòng nhập email hợp lệ")));
            onView(withContentDescription("Quên mật khẩu?")).perform(scrollTo(),click());
            onView(withContentDescription("Gửi liên kết")).perform(scrollTo(),click());
            onView(withId(R.id.email)).check(matches(hasErrorText("Vui lòng nhập email hợp lệ")));
        }
    }
}



