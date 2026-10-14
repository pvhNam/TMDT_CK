package com.example.tmdt;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

/** Regression checks for independent fragments and authentication navigation guards. */
@RunWith(AndroidJUnit4.class)
public class ScreenNavigationTest {
    @org.junit.BeforeClass public static void configure(){FirebaseTestEnvironment.configure();}
    private void assertScreen(ActivityScenario<MainActivity> scenario,String tag,Class<?> type){
        scenario.onActivity(a->{a.getSupportFragmentManager().executePendingTransactions();
            assertEquals(tag,a.screen);
            assertTrue(type.isInstance(a.getSupportFragmentManager().findFragmentById(R.id.screen_container)));
        });
    }
    @Test public void eachDestinationHasItsOwnFragmentAndBackRoute() throws Exception {
        org.junit.Assume.assumeTrue(com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser()==null);
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            assertScreen(scenario,"home",com.example.tmdt.ui.home.HomeFragment.class);
            assertMenu(scenario,R.id.nav_home,true);
            onView(withId(R.id.nav_schedule)).perform(click());
            assertScreen(scenario,"schedule",com.example.tmdt.ui.schedule.ScheduleFragment.class);
            assertMenu(scenario,R.id.nav_schedule,true);
            CatalogFirebaseTest.screenshot("figma-menu-schedule");
            scenario.recreate();
            assertScreen(scenario,"schedule",com.example.tmdt.ui.schedule.ScheduleFragment.class);
            assertMenu(scenario,R.id.nav_schedule,true);
            onView(withId(R.id.nav_messages)).perform(click());
            assertScreen(scenario,"messages",com.example.tmdt.ui.messages.MessagesFragment.class);
            assertMenu(scenario,R.id.nav_messages,true);
            CatalogFirebaseTest.screenshot("figma-menu-messages");
            onView(withId(R.id.nav_home)).perform(click());
            assertScreen(scenario,"home",com.example.tmdt.ui.home.HomeFragment.class);
            onView(withId(R.id.nav_account)).perform(click());
            assertScreen(scenario,"login",com.example.tmdt.ui.auth.LoginFragment.class);
            assertMenu(scenario,R.id.nav_account,false);
            onView(withId(R.id.register)).perform(androidx.test.espresso.action.ViewActions.scrollTo(),click());
            assertScreen(scenario,"register",com.example.tmdt.ui.auth.RegisterFragment.class);
            assertMenu(scenario,R.id.nav_account,false);
            scenario.onActivity(a->a.show("home"));
            assertScreen(scenario,"home",com.example.tmdt.ui.home.HomeFragment.class);
            scenario.onActivity(a->a.show("account"));
            assertScreen(scenario,"login",com.example.tmdt.ui.auth.LoginFragment.class);
            scenario.onActivity(a->a.show("register"));
            assertScreen(scenario,"register",com.example.tmdt.ui.auth.RegisterFragment.class);
            scenario.recreate();
            assertScreen(scenario,"register",com.example.tmdt.ui.auth.RegisterFragment.class);
            assertMenu(scenario,R.id.nav_account,false);
            scenario.onActivity(MainActivity::back);
            assertScreen(scenario,"login",com.example.tmdt.ui.auth.LoginFragment.class);
            scenario.onActivity(a->a.show("forgot"));
            assertScreen(scenario,"forgot",com.example.tmdt.ui.auth.ForgotPasswordFragment.class);
            onView(withId(R.id.sign_in)).perform(androidx.test.espresso.action.ViewActions.scrollTo(),click());
            assertScreen(scenario,"login",com.example.tmdt.ui.auth.LoginFragment.class);
            scenario.onActivity(MainActivity::back);
            assertScreen(scenario,"home",com.example.tmdt.ui.home.HomeFragment.class);
        }
    }
    private void assertMenu(ActivityScenario<MainActivity> scenario,int selected,boolean visible){
        scenario.onActivity(a->{
            com.google.android.material.bottomnavigation.BottomNavigationView nav=a.findViewById(R.id.bottom_navigation);
            assertEquals(visible?android.view.View.VISIBLE:android.view.View.GONE,nav.getVisibility());
            assertEquals(4,nav.getMenu().size());
            int[] ids={R.id.nav_home,R.id.nav_schedule,R.id.nav_messages,R.id.nav_account};
            for(int i=0;i<ids.length;i++){
                assertEquals(ids[i],nav.getMenu().getItem(i).getItemId());
                assertTrue(nav.getMenu().getItem(i).isVisible());
                assertNotNull(nav.getMenu().getItem(i).getIcon());
            }
            assertFalse(nav.isItemActiveIndicatorEnabled());
            assertEquals(selected,nav.getSelectedItemId());
        });
    }
}

