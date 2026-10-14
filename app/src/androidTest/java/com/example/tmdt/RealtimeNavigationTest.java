package com.example.tmdt;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Pixel checks against a scrolling fixture, with no production writes or accounts. */
@RunWith(AndroidJUnit4.class)
public class RealtimeNavigationTest {
    @BeforeClass public static void setup() {
        org.junit.Assume.assumeTrue("true".equals(InstrumentationRegistry.getArguments().getString("firebaseEmulators")));
        FirebaseTestEnvironment.configure();
    }
    public static class BackdropFragment extends Fragment {
        @Override public View onCreateView(@NonNull LayoutInflater inflater,ViewGroup container,Bundle state) {
            ScrollView scroll=new ScrollView(requireContext());
            scroll.setTag("blur-scroll");scroll.setFillViewport(true);
            LinearLayout body=new LinearLayout(requireContext());body.setOrientation(LinearLayout.VERTICAL);
            scroll.addView(body);
            int height=Math.round(1200*getResources().getDisplayMetrics().density);
            for(int color:new int[]{0xFFC82B4B,0xFF126FE8}) {
                View band=new View(requireContext()) {
                    final Paint paint=new Paint();
                    @Override protected void onDraw(Canvas canvas) {
                        canvas.drawColor(color);paint.setColor(Color.WHITE);
                        // This sharp stripe must become diffuse only inside the navigation pill.
                        float middle=getWidth()/2f;
                        canvas.drawRect(middle-2,0,middle+2,getHeight(),paint);
                    }
                };
                body.addView(band,new LinearLayout.LayoutParams(-1,height));
            }
            Button last=new Button(requireContext());last.setText("Nút cuối trang");last.setTag("last-action");
            body.addView(last,new LinearLayout.LayoutParams(-1,Math.round(56*getResources().getDisplayMetrics().density)));
            return scroll;
        }
    }
    private void frames()throws Exception {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();Thread.sleep(450);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }
    private int[] glassBounds(ActivityScenario<MainActivity> scenario) {
        int[] result=new int[4];
        scenario.onActivity(a->{View glass=a.findViewById(R.id.navigation_glass);int[] xy=new int[2];glass.getLocationOnScreen(xy);
            result[0]=xy[0];result[1]=xy[1];result[2]=glass.getWidth();result[3]=glass.getHeight();
            assertTrue(glass.getClipToOutline());
        });return result;
    }
    private Bitmap pixels(){return InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();}
    @Test public void backgroundReallyBlursAndUpdatesWithScrolling()throws Exception {
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a->a.getSupportFragmentManager().beginTransaction()
                    .replace(R.id.screen_container,new BackdropFragment(),"home").commitNow());
            frames();int[] bounds=glassBounds(scenario);
            int x=bounds[0]+bounds[2]/2,y=bounds[1]+bounds[3]/3;
            Bitmap red=pixels();int redPixel=red.getPixel(x,y);
            CatalogFirebaseTest.screenshot("realtime-menu-check");
            assertTrue("Blur must soften the white stripe behind the menu",Color.green(redPixel)<230);
            assertTrue("First backdrop must tint the menu red",Color.red(redPixel)>Color.blue(redPixel)+25);
            int corner=red.getPixel(bounds[0]+2,bounds[1]+2);
            assertTrue("Rounded corners must reveal page content, not an opaque wrapper",Color.red(corner)>Color.green(corner)+70);
            int gap=red.getPixel(Math.max(1,bounds[0]/2),y);
            assertTrue("Outside the pill the page must continue to the screen edge",Color.red(gap)>Color.green(gap)+70);
            red.recycle();CatalogFirebaseTest.screenshot("realtime-menu-red");
            scenario.onActivity(a->{ScrollView scroll=a.findViewById(R.id.screen_container).findViewWithTag("blur-scroll");
                scroll.scrollTo(0,Math.round(1200*a.getResources().getDisplayMetrics().density));});
            frames();Bitmap blue=pixels();int bluePixel=blue.getPixel(x,y);blue.recycle();
            assertTrue("Scrolling must update the captured menu background",Color.blue(bluePixel)>Color.red(bluePixel)+25);
            CatalogFirebaseTest.screenshot("realtime-menu-blue");
            scenario.recreate();frames();glassBounds(scenario);
            scenario.onActivity(a->{ScrollView scroll=a.findViewById(R.id.screen_container).findViewWithTag("blur-scroll");
                assertFalse(scroll.getClipToPadding());assertTrue(scroll.getPaddingBottom()>a.findViewById(R.id.navigation_glass).getHeight());
                scroll.fullScroll(View.FOCUS_DOWN);});
            frames();
            scenario.onActivity(a->{View last=a.findViewById(R.id.screen_container).findViewWithTag("last-action");
                int[] button=new int[2],menu=new int[2];last.getLocationOnScreen(button);a.findViewById(R.id.navigation_glass).getLocationOnScreen(menu);
                assertTrue("Last action must scroll fully above floating menu",button[1]+last.getHeight()<=menu[1]);
                a.show("login");a.getSupportFragmentManager().executePendingTransactions();
                assertEquals(View.GONE,a.findViewById(R.id.navigation_glass).getVisibility());
                assertEquals(0,a.findViewById(R.id.account_scroll).getPaddingBottom());
            });
        }
    }
}
