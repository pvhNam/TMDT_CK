package com.example.tmdt;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.tmdt.ui.common.ScreenFragment;
import com.google.firebase.auth.FirebaseAuth;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;

/** In-memory lesson fixtures exercise rendering only; no lesson/role is written to Firebase. */
@RunWith(AndroidJUnit4.class)
public class TeacherMotionTest {
    @BeforeClass public static void setup(){
        org.junit.Assume.assumeTrue("true".equals(InstrumentationRegistry.getArguments().getString("firebaseEmulators")));
        FirebaseTestEnvironment.configure();
    }
    private ScrollView scroll(View view){
        if(view instanceof ScrollView)return (ScrollView)view;
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){
            ScrollView found=scroll(((ViewGroup)view).getChildAt(i));if(found!=null)return found;
        }
        return null;
    }
    private void idle()throws Exception{
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();Thread.sleep(300);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }
    @Test public void rapidNavigationExpansionAndRefreshKeepCorrectState()throws Exception{
        FirebaseAuth.getInstance().signOut();
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            UserProfile profile=new UserProfile();profile.name="Gia sư chuyển động";profile.email="motion-"+System.currentTimeMillis()+"@example.com";profile.phone="0912345678";
            scenario.onActivity(a->a.account.register(profile,"HocTap123!"));
            AtomicBoolean ready=new AtomicBoolean();long deadline=System.currentTimeMillis()+30000;
            do{scenario.onActivity(a->ready.set(!a.account.isBusy()&&a.account.profile()!=null));if(ready.get())break;Thread.sleep(100);}while(System.currentTimeMillis()<deadline);
            assertTrue(ready.get());idle();
            scenario.onActivity(a->{
                assertEquals("",a.account.error());a.account.profile().tutor=true;
                String uid=a.classroom.me();a.classroom.lessons.clear();
                for(int i=0;i<12;i++)a.classroom.lessons.add(new Lesson("motion-"+i,uid,profile.name,"student-"+i,"Học viên "+i,"Toán THPT",LocalDate.now().plusDays(1).toString(),18,90,"Trực tuyến","Ôn tập","",false,150000,Lesson.PENDING));
                a.classroom.setTutorMode(true);a.getSupportFragmentManager().executePendingTransactions();
            });idle();
            onView(withContentDescription("Xem tất cả yêu cầu mới")).perform(scrollTo(),click());idle();
            onView(withContentDescription("Thu gọn yêu cầu mới")).check(matches(withText("Thu gọn")));
            AtomicInteger offset=new AtomicInteger();
            scenario.onActivity(a->{ScrollView page=scroll(a.findViewById(R.id.screen_container));page.scrollTo(0,500);offset.set(page.getScrollY());
                ScreenFragment screen=(ScreenFragment)a.getSupportFragmentManager().findFragmentById(R.id.screen_container);
                for(int i=0;i<25;i++)screen.refresh();
            });idle();
            assertTrue("Expanded requests must be scrollable",offset.get()>0);
            scenario.onActivity(a->assertEquals(offset.get(),scroll(a.findViewById(R.id.screen_container)).getScrollY()));
            scenario.recreate();idle();
            scenario.onActivity(a->assertEquals(offset.get(),scroll(a.findViewById(R.id.screen_container)).getScrollY()));
            onView(withContentDescription("Thu gọn yêu cầu mới")).perform(scrollTo()).check(matches(withText("Thu gọn")));
            onView(withContentDescription("Thu gọn yêu cầu mới")).perform(click());idle();
            onView(withContentDescription("Xem tất cả yêu cầu mới")).check(matches(withText("Xem tất cả")));
            scenario.onActivity(a->{a.show("teaching");a.show("classes");a.show("tutorHome");a.show("tutorHome");a.getSupportFragmentManager().executePendingTransactions();});idle();
            scenario.onActivity(a->{assertEquals("tutorHome",a.screen);assertEquals("tutorHome",a.getSupportFragmentManager().findFragmentById(R.id.screen_container).getTag());a.show("teaching");a.getSupportFragmentManager().executePendingTransactions();});idle();
            onView(withText("Tháng")).perform(click());idle();
            scenario.onActivity(a->assertTrue(a.classroom.teachingMonth));
            onView(withText("Tuần")).perform(click());idle();
            scenario.onActivity(a->{assertFalse(a.classroom.teachingMonth);a.show("classes");a.getSupportFragmentManager().executePendingTransactions();});idle();
            onView(withText("Đang học (0)")).perform(click());idle();
            scenario.onActivity(a->assertEquals(1,a.classroom.classTab));
            onView(withText("Đã kết thúc (0)")).perform(click());idle();
            scenario.onActivity(a->assertEquals(2,a.classroom.classTab));
        }finally{FirebaseAuth.getInstance().signOut();}
    }
}
