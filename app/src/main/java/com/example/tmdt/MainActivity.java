package com.example.tmdt;

import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.Gravity;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import org.json.JSONArray;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    Ui ui;
    SharedPreferences preferences;
    final List<Lesson> lessons = new ArrayList<>();
    String screen = "home";
    String profileOrigin = "home";
    int tutorId = 0;
    int scheduleTab = 0;
    BookingScreen booking;
    private FrameLayout container;
    private LinearLayout navigation;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);
        ui = new Ui(this);
        preferences = getSharedPreferences("tutor_demo", MODE_PRIVATE);
        container = findViewById(R.id.screen_container);
        navigation = findViewById(R.id.bottom_navigation);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.app_root), (view, insets) -> {
            Insets safe = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
            view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
            return insets;
        });
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(true);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightNavigationBars(true);
        loadLessons();
        if (savedInstanceState != null) {
            screen=savedInstanceState.getString("screen","home");
            profileOrigin=savedInstanceState.getString("profileOrigin","home");
            tutorId=savedInstanceState.getInt("tutor",0);
            scheduleTab=savedInstanceState.getInt("tab",0);
            if ("booking".equals(screen)) booking=new BookingScreen(this,Tutor.ALL[tutorId],savedInstanceState);
        }
        getOnBackPressedDispatcher().addCallback(this,new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if ("home".equals(screen)) finish(); else back();
            }
        });
        show(screen);
    }

    private void loadLessons() {
        LocalDate anchor=LocalDate.now().plusDays(4);
        lessons.add(new Lesson(0,anchor.toString(),19,60,"Trực tuyến","Ôn tập phương trình bậc hai","",false));
        lessons.add(new Lesson(1,anchor.plusDays(2).toString(),18,60,"Trực tuyến","Luyện giao tiếp hằng ngày","",false));
        try {
            JSONArray items=new JSONArray(preferences.getString("requests","[]"));
            for(int i=0;i<items.length();i++) {
                try { lessons.add(Lesson.fromJson(items.getJSONObject(i))); }
                catch (org.json.JSONException | java.time.DateTimeException ignored) { /* Skip an invalid local entry. */ }
            }
        } catch (org.json.JSONException ignored) { /* Start with sample lessons if storage is invalid. */ }
    }

    void openTutor(Tutor tutor) {
        profileOrigin=screen;
        tutorId=tutor.id;
        show("profile");
    }
    void openBooking(Tutor tutor) {
        tutorId=tutor.id; booking=new BookingScreen(this,tutor,null); show("booking");
    }
    void back() {
        hideKeyboard();
        if("booking".equals(screen)) show("profile");
        else if("profile".equals(screen)) show(profileOrigin);
        else show("home");
    }
    void show(String destination) {
        screen=destination;
        container.removeAllViews();
        View view;
        switch(destination) {
            case "profile": view=new ProfileScreen(this,Tutor.ALL[tutorId]).build(); break;
            case "booking":
                if(booking==null) booking=new BookingScreen(this,Tutor.ALL[tutorId],null);
                view=booking.build(); break;
            case "schedule": view=new ScheduleScreen(this).build(); break;
            default: screen="home"; view=new HomeScreen(this).build();
        }
        container.addView(view,new FrameLayout.LayoutParams(-1,-1));
        renderNavigation();
    }

    private void renderNavigation() {
        navigation.removeAllViews();
        if(!screen.equals("home") && !screen.equals("schedule")) { navigation.setVisibility(View.GONE); return; }
        navigation.setVisibility(View.VISIBLE); ui.line(navigation);
        LinearLayout row=ui.row(); ui.pad(row,8,6);
        String[] labels={"Trang chủ","Lịch học","Tin nhắn","Cá nhân"};
        String[] icons={"home","calendar","chat","person"};
        for(int i=0;i<labels.length;i++) {
            final int index=i;
            boolean active=(i==0 && screen.equals("home")) || (i==1 && screen.equals("schedule"));
            int color=active?Ui.BLUE:Ui.MUTED;
            LinearLayout item=ui.column(); item.setGravity(Gravity.CENTER); item.setMinimumHeight(ui.dp(62));
            item.addView(new LineIcon(this,icons[i],color),ui.lp(24,24)); ui.space(item,6);
            item.addView(ui.text(labels[i],11,color,active)); item.setContentDescription(labels[i]); item.setSelected(active);
            ui.clickable(item,()-> {
                hideKeyboard();
                if(index==0) show("home");
                else if(index==1) show("schedule");
                else if(index==2) dialog("Tin nhắn","Chưa có cuộc trò chuyện. Chức năng nhắn tin sẽ hoạt động khi ứng dụng được kết nối máy chủ.");
                else dialog("Hồ sơ của Nam","Tài khoản học viên mẫu\n\nBạn đang xem bản giao diện ứng dụng Gia Sư. Các yêu cầu đặt học hiện được lưu trên thiết bị này.");
            });
            ui.weight(row,item);
        }
        ui.add(navigation,row);
    }

    boolean favorite(Tutor tutor) { return preferences.getBoolean("favorite_"+tutor.id,false); }
    void toggleFavorite(Tutor tutor) { preferences.edit().putBoolean("favorite_"+tutor.id,!favorite(tutor)).apply(); show("profile"); }
    void message(Tutor tutor) { dialog("Nhắn tin với "+tutor.name,"Chức năng trò chuyện đang được chuẩn bị. Bạn có thể thử gửi yêu cầu đặt học trong bản giao diện này."); }
    void dialog(String title,String message) { new AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("Đã hiểu",null).show(); }
    void hideKeyboard() {
        View focus=getCurrentFocus();
        if(focus!=null) ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(focus.getWindowToken(),0);
    }
    boolean saveRequest(Lesson lesson) {
        for(Lesson existing:lessons) {
            int start=lesson.hour*60, end=start+lesson.minutes, otherStart=existing.hour*60, otherEnd=otherStart+existing.minutes;
            if(existing.date.equals(lesson.date) && start<otherEnd && otherStart<end) {
                dialog("Lịch học bị trùng","Bạn đã có buổi học hoặc yêu cầu trong khung giờ này. Hãy chọn giờ khác."); return false;
            }
        }
        try {
            JSONArray requests=new JSONArray();
            for(Lesson existing:lessons) if(existing.pending) requests.put(existing.toJson());
            requests.put(lesson.toJson());
            preferences.edit().putString("requests",requests.toString()).apply();
            lessons.add(lesson); hideKeyboard(); scheduleTab=1; booking=null; show("schedule");
            com.google.android.material.snackbar.Snackbar.make(container,"Đã lưu yêu cầu đặt học trên thiết bị",com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show();
            return true;
        } catch(org.json.JSONException exception) { dialog("Chưa lưu được yêu cầu","Vui lòng thử lại."); return false; }
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putString("screen",screen); state.putString("profileOrigin",profileOrigin);
        state.putInt("tutor",tutorId); state.putInt("tab",scheduleTab);
        if(booking!=null && screen.equals("booking")) booking.saveState(state);
    }
}
