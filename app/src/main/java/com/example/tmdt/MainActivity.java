package com.example.tmdt;

import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.view.Gravity;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.snackbar.Snackbar;
import java.util.List;
import com.example.tmdt.common.Ui;
import com.example.tmdt.data.GroupClass;
import com.example.tmdt.data.Lesson;
import com.example.tmdt.data.Store;
import com.example.tmdt.data.Tutor;
import com.example.tmdt.student.booking.BookingScreen;
import com.example.tmdt.student.booking.ProfileScreen;
import com.example.tmdt.student.booking.TrialScreen;
import com.example.tmdt.student.groupclass.ClassFullScreen;
import com.example.tmdt.student.groupclass.GroupSearchScreen;
import com.example.tmdt.student.groupclass.JoinClassScreen;
import com.example.tmdt.student.home.HomeScreen;
import com.example.tmdt.student.schedule.CancelLessonScreen;
import com.example.tmdt.student.schedule.ConfirmLessonScreen;
import com.example.tmdt.student.schedule.LessonDetailScreen;
import com.example.tmdt.student.schedule.RescheduleScreen;
import com.example.tmdt.student.schedule.ScheduleScreen;
import com.example.tmdt.tutor.groupclass.OpenClassScreen;
import com.example.tmdt.tutor.groupclass.OpenedClassesScreen;
import com.example.tmdt.tutor.groupclass.ReviewRegistrationsScreen;
import com.example.tmdt.tutor.home.TutorHomeScreen;
import com.example.tmdt.tutor.schedule.TeachingScheduleScreen;

public class MainActivity extends AppCompatActivity {
    public Ui ui;
    public SharedPreferences preferences;
    public Store store;
    public List<Lesson> lessons;
    public String screen = "home";
    public String profileOrigin = "home";
    public int tutorId = 0;
    public int scheduleTab = 0;
    public int classTab = 0;
    public int lessonId = -1;
    public int classId = -1;
    public boolean tutorMode;
    /** Set by "Tìm lớp tương tự" so the class search opens with matching filters. */
    public String similarSubject;
    /** Day and view (week or month) shown on the tutor's teaching schedule. */
    public java.time.LocalDate teachingDay = java.time.LocalDate.now();
    public boolean teachingMonth;
    public BookingScreen booking;
    private FrameLayout container;
    private LinearLayout navigation;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);
        ui = new Ui(this);
        preferences = getSharedPreferences("tutor_demo", MODE_PRIVATE);
        store = new Store(preferences);
        lessons = store.lessons;
        tutorMode = preferences.getBoolean("tutor_mode", false);
        container = findViewById(R.id.screen_container);
        navigation = findViewById(R.id.bottom_navigation);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.app_root), (view, insets) -> {
            Insets safe = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
            view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
            return insets;
        });
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(true);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightNavigationBars(true);
        if (savedInstanceState != null) {
            screen=savedInstanceState.getString("screen","home");
            profileOrigin=savedInstanceState.getString("profileOrigin","home");
            tutorId=savedInstanceState.getInt("tutor",0);
            scheduleTab=savedInstanceState.getInt("tab",0);
            classTab=savedInstanceState.getInt("classTab",0);
            lessonId=savedInstanceState.getInt("lesson",-1);
            classId=savedInstanceState.getInt("class",-1);
            if ("booking".equals(screen)) booking=new BookingScreen(this,Tutor.ALL[tutorId],savedInstanceState);
        } else if (tutorMode) screen="tutorHome";
        getOnBackPressedDispatcher().addCallback(this,new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (screen.equals(root())) finish(); else back();
            }
        });
        show(screen);
    }

    private String root() { return tutorMode ? "tutorHome" : "home"; }

    public void openTutor(Tutor tutor) {
        profileOrigin=screen;
        tutorId=tutor.id;
        show("profile");
    }
    public void openBooking(Tutor tutor) {
        tutorId=tutor.id; booking=new BookingScreen(this,tutor,null); show("booking");
    }
    public void openLesson(Lesson lesson) { lessonId=lesson.id; show("lesson"); }
    public void openClass(GroupClass item, String destination) { classId=item==null?-1:item.id; show(destination); }
    public void back() {
        hideKeyboard();
        switch (screen) {
            case "booking": show("profile"); break;
            case "profile": show(profileOrigin); break;
            case "trial": show("booking"); break;
            case "lesson": show(tutorMode ? "teaching" : "schedule"); break;
            case "confirm": case "cancel": case "reschedule": show("lesson"); break;
            case "join": case "full": show("groups"); break;
            case "openClass": case "review": show("classes"); break;
            default: show(root());
        }
    }
    public void show(String destination) {
        screen=destination;
        container.removeAllViews();
        View view;
        switch(destination) {
            case "profile": view=new ProfileScreen(this,Tutor.ALL[tutorId]).build(); break;
            case "booking":
                if(booking==null) booking=new BookingScreen(this,Tutor.ALL[tutorId],null);
                view=booking.build(); break;
            case "schedule": view=new ScheduleScreen(this).build(); break;
            case "trial": view=new TrialScreen(this,Tutor.ALL[tutorId]).build(); break;
            case "groups": view=new GroupSearchScreen(this).build(); break;
            case "tutorHome": view=new TutorHomeScreen(this).build(); break;
            case "teaching": view=new TeachingScheduleScreen(this).build(); break;
            case "classes": view=new OpenedClassesScreen(this).build(); break;
            case "openClass": view=new OpenClassScreen(this,store.groupClass(classId)).build(); break;
            default: view=lessonOrClassScreen(destination);
        }
        container.addView(view,new FrameLayout.LayoutParams(-1,-1));
        renderNavigation();
    }
    /** Screens that need a selected lesson or class; falls back to the start screen if it no longer exists. */
    private View lessonOrClassScreen(String destination) {
        Lesson lesson=store.lesson(lessonId); GroupClass item=store.groupClass(classId);
        if (lesson != null) switch (destination) {
            case "lesson": return new LessonDetailScreen(this,lesson).build();
            case "confirm": return new ConfirmLessonScreen(this,lesson).build();
            case "cancel": return new CancelLessonScreen(this,lesson).build();
            case "reschedule": return new RescheduleScreen(this,lesson).build();
        }
        if (item != null) switch (destination) {
            case "join": return new JoinClassScreen(this,item).build();
            case "full": return new ClassFullScreen(this,item).build();
            case "review": return new ReviewRegistrationsScreen(this,item).build();
        }
        screen=root();
        return tutorMode ? new TutorHomeScreen(this).build() : new HomeScreen(this).build();
    }

    private void renderNavigation() {
        navigation.removeAllViews();
        String[] labels, targets; int[] icons, activeIcons; String active;
        if (tutorMode) {
            if (!java.util.Arrays.asList("tutorHome","teaching","classes","review").contains(screen)) { navigation.setVisibility(View.GONE); return; }
            labels=new String[]{"Tổng quan","Lịch dạy","Lớp học","Tin nhắn","Cá nhân"};
            targets=new String[]{"tutorHome","teaching","classes","chat","account"};
            icons=new int[]{R.drawable.ic_home_nav,R.drawable.ic_calendar_nav,R.drawable.ic_users,R.drawable.ic_chat_nav,R.drawable.ic_user};
            activeIcons=new int[]{R.drawable.ic_home_nav_filled,R.drawable.ic_calendar_nav_filled,R.drawable.ic_users_filled,R.drawable.ic_chat_nav,R.drawable.ic_user};
            active=screen.equals("review")?"classes":screen;
        } else {
            if (!java.util.Arrays.asList("home","schedule","groups","full","confirm","cancel","reschedule").contains(screen)) { navigation.setVisibility(View.GONE); return; }
            labels=new String[]{"Trang chủ","Lịch học","Tin nhắn","Cá nhân"};
            targets=new String[]{"home","schedule","chat","account"};
            icons=new int[]{R.drawable.ic_home_nav,R.drawable.ic_calendar_nav,R.drawable.ic_chat_nav,R.drawable.ic_user};
            activeIcons=new int[]{R.drawable.ic_home_nav_filled,R.drawable.ic_calendar_nav_filled,R.drawable.ic_chat_nav,R.drawable.ic_user};
            active=screen.equals("groups")||screen.equals("full")?"home":screen.equals("home")?"home":"schedule";
        }
        navigation.setVisibility(View.VISIBLE); ui.line(navigation);
        LinearLayout row=ui.row(); ui.pad(row,4,6);
        for(int i=0;i<labels.length;i++) {
            final String target=targets[i];
            boolean selected=target.equals(active);
            int color=selected?Ui.BLUE:Ui.MUTED;
            LinearLayout item=ui.column(); item.setGravity(Gravity.CENTER); item.setMinimumHeight(ui.dp(62));
            item.addView(ui.icon(selected?activeIcons[i]:icons[i],24,selected?Ui.BLUE:Ui.INK)); ui.space(item,4);
            item.addView(ui.text(labels[i],13,color,selected)); item.setContentDescription(labels[i]); item.setSelected(selected);
            ui.clickable(item,()-> {
                hideKeyboard();
                if(target.equals("chat")) dialog("Tin nhắn","Chưa có cuộc trò chuyện. Màn hình Tin nhắn (05–06) do Thành viên 4 phụ trách.");
                else if(target.equals("account")) account();
                else show(target);
            });
            ui.weight(row,item);
        }
        ui.add(navigation,row);
    }

    /** Placeholder of the "Cá nhân" tab (screen 07 belongs to member 1); lets the demo switch between the two roles. */
    private void account() {
        AlertDialog.Builder builder=new AlertDialog.Builder(this).setPositiveButton("Đóng",null);
        if (tutorMode) builder.setTitle("Hồ sơ của cô Minh Anh").setMessage("Tài khoản gia sư mẫu.\n\nChuyển về chế độ học viên để xem lịch học của Phạm Nam.")
                .setNeutralButton("Chế độ học viên",(d,w)->setTutorMode(false));
        else builder.setTitle("Hồ sơ của Nam").setMessage("Tài khoản học viên mẫu.\n\nBạn đang xem bản giao diện ứng dụng Gia Sư. Dữ liệu buổi học và lớp nhóm được lưu trên thiết bị này.")
                .setNeutralButton("Chế độ gia sư",(d,w)->setTutorMode(true));
        builder.show();
    }
    public void setTutorMode(boolean value) {
        tutorMode=value; preferences.edit().putBoolean("tutor_mode",value).apply(); show(root());
    }

    public boolean favorite(Tutor tutor) { return preferences.getBoolean("favorite_"+tutor.id,false); }
    public void toggleFavorite(Tutor tutor) { preferences.edit().putBoolean("favorite_"+tutor.id,!favorite(tutor)).apply(); show("profile"); }
    public void message(Tutor tutor) { message(tutor.name); }
    public void message(String person) { dialog("Nhắn tin với "+person,"Chức năng trò chuyện (màn hình 05–06) do Thành viên 4 phụ trách và sẽ được nối vào đây."); }
    public void dialog(String title,String message) { new AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("Đã hiểu",null).show(); }
    public void notice(String message) { Snackbar.make(container,message,Snackbar.LENGTH_LONG).show(); }
    public void hideKeyboard() {
        View focus=getCurrentFocus();
        if(focus!=null) ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(focus.getWindowToken(),0);
    }
    /** Saves a new booking or trial request unless it overlaps an existing lesson, then shows it under "Chờ xác nhận". */
    public boolean saveRequest(Lesson lesson) {
        String clash=store.conflict(lesson.tutorId,lesson.student,lesson.date,lesson.hour,lesson.minutes,-1);
        if(clash!=null) { dialog("Lịch học bị trùng",clash); return false; }
        lessons.add(lesson); store.save(); hideKeyboard(); scheduleTab=1; booking=null; show("schedule");
        notice("Đã gửi yêu cầu. Gia sư sẽ xác nhận yêu cầu của bạn.");
        return true;
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putString("screen",screen); state.putString("profileOrigin",profileOrigin);
        state.putInt("tutor",tutorId); state.putInt("tab",scheduleTab); state.putInt("classTab",classTab);
        state.putInt("lesson",lessonId); state.putInt("class",classId);
        if(booking!=null && screen.equals("booking")) booking.saveState(state);
    }
}
