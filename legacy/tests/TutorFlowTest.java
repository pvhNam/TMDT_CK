package com.example.tmdt;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.util.Map;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.*;

/** Exercises the real screens and local booking persistence on an Android device. */
@RunWith(AndroidJUnit4.class)
@org.junit.Ignore("Legacy local booking flow is not exposed in the reduced app; use CatalogFirebaseTest for the live home screen")
public class TutorFlowTest {
    private SharedPreferences preferences;
    private Map<String,?> original;
    private ActivityScenario<MainActivity> scenario;

    @Before public void setUp() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        preferences=context.getSharedPreferences("tutor_demo",Context.MODE_PRIVATE);
        original=preferences.getAll();preferences.edit().clear().commit();
        scenario=ActivityScenario.launch(MainActivity.class);
    }
    @After public void tearDown() {
        if(scenario!=null)scenario.close();
        SharedPreferences.Editor editor=preferences.edit().clear();
        for(Map.Entry<String,?> item:original.entrySet()) {
            if(item.getValue() instanceof String)editor.putString(item.getKey(),(String)item.getValue());
            else if(item.getValue() instanceof Boolean)editor.putBoolean(item.getKey(),(Boolean)item.getValue());
        }
        editor.commit();
    }

    @Test public void searchSupportsVietnameseWithoutDiacritics() {
        onView(withContentDescription("Tìm môn học hoặc tên gia sư")).perform(replaceText("nguyen minh anh"),closeSoftKeyboard());
        onView(withText("Nguyễn Minh Anh")).check(matches(isDisplayed()));
        onView(withContentDescription("Tìm môn học hoặc tên gia sư")).perform(replaceText("khong co gia su"),closeSoftKeyboard());
        onView(withText("Chưa tìm thấy gia sư")).check(matches(isDisplayed()));
        onView(withContentDescription("Xóa bộ lọc")).perform(click());
        onView(withText("Trần Hoàng Nam")).check(matches(isDisplayed()));
    }

    @Test public void bookingUpdatesPriceSurvivesRecreationAndPersists() {
        onView(withText("Nguyễn Minh Anh")).perform(click());
        onView(withContentDescription("Đặt lịch học")).perform(click());
        onView(withText("60 phút")).perform(scrollTo(),click());
        onView(withText("90 phút")).perform(click());
        onView(withContentDescription("Mục tiêu buổi học")).perform(scrollTo(),replaceText("Luyen thi dai hoc"),closeSoftKeyboard());
        scenario.recreate();
        onView(withText("90 phút")).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withContentDescription("Mục tiêu buổi học")).check(matches(withText("Luyen thi dai hoc")));
        onView(withContentDescription("Gửi yêu cầu đặt lịch")).perform(click());
        scenario.onActivity(activity->{
            assertEquals("schedule",activity.screen);assertEquals(1,activity.scheduleTab);
            Lesson request=activity.lessons.get(activity.lessons.size()-1);
            assertTrue(request.pending);assertEquals(90,request.minutes);assertEquals(225000,request.total());
        });
        scenario.close();scenario=ActivityScenario.launch(MainActivity.class);
        onView(withContentDescription("Lịch học")).perform(click());
        onView(withContentDescription("Chờ xác nhận")).perform(click());
        onView(withContentDescription("Chi tiết")).perform(click());
        onView(withText(containsString("Luyen thi dai hoc"))).check(matches(isDisplayed()));
        onView(withText(containsString("225.000đ"))).check(matches(isDisplayed()));
    }

    @Test public void homeLessonRequiresAddress() {
        onView(withText("Nguyễn Minh Anh")).perform(click());
        onView(withContentDescription("Đặt lịch học")).perform(click());
        onView(withContentDescription("Tại nhà")).perform(click());
        onView(withContentDescription("Gửi yêu cầu đặt lịch")).perform(click());
        onView(withContentDescription("Địa chỉ học tại nhà")).check(matches(hasErrorText("Vui lòng nhập địa chỉ học")));
        scenario.onActivity(activity->assertEquals("booking",activity.screen));
    }

    @Test public void overlappingLessonsAreRejected() {
        scenario.onActivity(activity->{
            String date=LocalDate.now().plusDays(30).toString();
            assertTrue(activity.saveRequest(new Lesson(0,date,18,90,"Trực tuyến","Ôn thi","",true)));
            assertFalse(activity.saveRequest(new Lesson(1,date,19,60,"Trực tuyến","Luyện nói","",true)));
        });
        onView(withText("Lịch học bị trùng")).check(matches(isDisplayed()));
    }

    @Test public void captureFourScreens() throws Exception {
        screenshot("01-home.png");
        onView(withText("Nguyễn Minh Anh")).perform(click());screenshot("02-profile.png");
        onView(withContentDescription("Đặt lịch học")).perform(click());screenshot("03-booking.png");
        scenario.onActivity(activity->activity.show("schedule"));screenshot("04-schedule.png");
    }

    private void screenshot(String name) throws Exception {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        Bitmap bitmap=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        File directory=new File(context.getExternalFilesDir(null),"ui-previews");
        assertTrue(directory.isDirectory() || directory.mkdirs());
        try(FileOutputStream stream=new FileOutputStream(new File(directory,name))) {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,stream));
        }
        bitmap.recycle();
    }
}
