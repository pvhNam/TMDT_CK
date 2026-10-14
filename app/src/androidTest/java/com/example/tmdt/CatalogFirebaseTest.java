package com.example.tmdt;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;

/** Uses catalog fixtures from scripts/test-firebase.mjs, exclusively on the local emulator. */
@RunWith(AndroidJUnit4.class)
public class CatalogFirebaseTest {
    @BeforeClass public static void connect(){
        org.junit.Assume.assumeTrue("true".equals(InstrumentationRegistry.getArguments().getString("firebaseEmulators")));
        FirebaseTestEnvironment.configure();
        FirebaseAuth.getInstance().signOut();
    }
    static void screenshot(String name) throws Exception {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        Thread.sleep(400); // Wait for the screen's first frame before reading pixels.
        File dir=new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir(null),"previews");
        dir.mkdirs();
        try(FileOutputStream output=new FileOutputStream(new File(dir,name+".png"))){
            InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot().compress(android.graphics.Bitmap.CompressFormat.PNG,100,output);
        }
    }
    @Test public void liveCatalogSearchAndAccountNavigation() throws Exception {
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            AtomicBoolean ready=new AtomicBoolean();long until=System.currentTimeMillis()+20000;
            do {scenario.onActivity(a->ready.set(!a.catalog.loading() && a.catalog.teachers.containsKey("demo_math")));
                if(ready.get())break;Thread.sleep(100);
            }while(System.currentTimeMillis()<until);
            assertTrue("Live Firestore fixture should load",ready.get());
            screenshot("home");
            onView(withId(R.id.search)).perform(replaceText("nguyen minh anh"),closeSoftKeyboard());
            onView(withText("Nguyễn Minh Anh")).perform(scrollTo()).check(matches(isDisplayed()));
            onView(withText("Nguyễn Minh Anh")).perform(click());
            onView(withText("Môn dạy và học phí")).check(matches(isDisplayed()));
            onView(withText("Đóng")).perform(click());
            onView(withId(R.id.search)).perform(scrollTo(),replaceText("khong co gia su"),closeSoftKeyboard());
            onView(withText("Chưa tìm thấy gia sư")).perform(scrollTo()).check(matches(isDisplayed()));
            onView(withContentDescription("Xóa bộ lọc")).perform(scrollTo(),click());
            scenario.recreate();
            onView(withId(R.id.nav_account)).perform(click());
            screenshot("login");
            onView(withContentDescription("Chưa có tài khoản? Đăng ký")).perform(scrollTo(),click());
            screenshot("register");
        }
    }
}


