package com.example.tmdt;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.tmdt.databinding.ActivityMainBinding;
import com.example.tmdt.ui.auth.ForgotPasswordFragment;
import com.example.tmdt.ui.auth.LoginFragment;
import com.example.tmdt.ui.auth.PhoneVerificationFragment;
import com.example.tmdt.ui.auth.RegisterFragment;
import com.example.tmdt.ui.booking.BookingFragment;
import com.example.tmdt.ui.booking.TrialFragment;
import com.example.tmdt.ui.common.ScreenFragment;
import com.example.tmdt.ui.common.Ui;
import com.example.tmdt.ui.groupclass.ClassFullFragment;
import com.example.tmdt.ui.groupclass.GroupSearchFragment;
import com.example.tmdt.ui.groupclass.JoinClassFragment;
import com.example.tmdt.ui.home.HomeFragment;
import com.example.tmdt.ui.home.TutorDetailsDialog;
import com.example.tmdt.ui.profile.AccountFragment;
import com.example.tmdt.ui.profile.EditAccountFragment;
import com.example.tmdt.ui.schedule.CancelLessonFragment;
import com.example.tmdt.ui.schedule.ConfirmLessonFragment;
import com.example.tmdt.ui.schedule.LessonDetailFragment;
import com.example.tmdt.ui.schedule.RescheduleFragment;
import com.example.tmdt.ui.schedule.ScheduleFragment;
import com.example.tmdt.ui.tutor.OpenClassFragment;
import com.example.tmdt.ui.tutor.OpenedClassesFragment;
import com.example.tmdt.ui.tutor.ReviewRegistrationsFragment;
import com.example.tmdt.ui.tutor.TeachingScheduleFragment;
import com.example.tmdt.ui.tutor.TutorHomeFragment;
import com.google.android.material.snackbar.Snackbar;
import java.time.LocalDate;
import java.util.List;

/** Hosts independent fragments. Layouts, form handling and Firebase access live outside this activity. */
public class MainActivity extends AppCompatActivity {
    AccountState account;
    CatalogState catalog;
    String screen="home";
    private String pendingDestination;
    private ActivityMainBinding binding;
    private boolean updatingNavigation;
    private boolean keyboardVisible;
    // Lessons and group classes (member 3): sample data saved on the device, see Store.
    public Ui ui;
    public Store store;
    public List<Lesson> lessons;
    private SharedPreferences preferences;
    public boolean tutorMode;
    public int tutorId, scheduleTab, classTab, lessonId=-1, classId=-1;
    /** Where "Đặt lịch học" was opened from, so back returns there. */
    private String bookingOrigin="home";
    /** Booking form kept while the trial screen is open or the activity is recreated. */
    public Bundle bookingDraft;
    /** Set by "Tìm lớp tương tự" so the class search opens with matching filters. */
    public String similarSubject;
    /** Day and view (week or month) shown on the tutor's teaching schedule. */
    public LocalDate teachingDay=LocalDate.now();
    public boolean teachingMonth;

    @Override public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(),false);
        binding=ActivityMainBinding.inflate(getLayoutInflater());setContentView(binding.getRoot());
        account=new ViewModelProvider(this).get(AccountState.class);
        catalog=new ViewModelProvider(this).get(CatalogState.class);
        ui=new Ui(this);
        preferences=getSharedPreferences("tutor_demo",MODE_PRIVATE);
        store=new Store(preferences);lessons=store.lessons;
        tutorMode=preferences.getBoolean("tutor_mode",false);
        ViewCompat.setOnApplyWindowInsetsListener(binding.appRoot,(view,insets)->{
            Insets safe=insets.getInsets(WindowInsetsCompat.Type.systemBars()|WindowInsetsCompat.Type.displayCutout()|WindowInsetsCompat.Type.ime());
            view.setPadding(safe.left,safe.top,safe.right,safe.bottom);
            keyboardVisible=insets.isVisible(WindowInsetsCompat.Type.ime());
            binding.bottomNavigation.setVisibility(keyboardVisible?View.GONE:View.VISIBLE);
            return WindowInsetsCompat.CONSUMED;
        });
        WindowCompat.getInsetsController(getWindow(),getWindow().getDecorView()).setAppearanceLightStatusBars(true);
        WindowCompat.getInsetsController(getWindow(),getWindow().getDecorView()).setAppearanceLightNavigationBars(true);
        binding.bottomNavigation.setOnItemSelectedListener(item->navigateMenu(item.getItemId()));
        binding.bottomNavigation.setOnItemReselectedListener(item->navigateMenu(item.getItemId()));
        getOnBackPressedDispatcher().addCallback(this,new OnBackPressedCallback(true){
            @Override public void handleOnBackPressed(){if(root().equals(screen))finish();else back();}
        });
        screen=savedInstanceState==null?root():savedInstanceState.getString("screen",root());
        if(savedInstanceState!=null){
            tutorId=savedInstanceState.getInt("tutor",0);scheduleTab=savedInstanceState.getInt("tab",0);
            classTab=savedInstanceState.getInt("classTab",0);lessonId=savedInstanceState.getInt("lesson",-1);
            classId=savedInstanceState.getInt("class",-1);bookingOrigin=savedInstanceState.getString("bookingOrigin","home");
            bookingDraft=savedInstanceState.getBundle("booking");
        }
        show(screen);
        account.changes().observe(this,ignored->{
            if(account.destination!=null){String destination=account.destination;account.destination=null;show(destination);}
            renderNavigation();
        });
    }
    /** The start screen: the tutor overview in tutor mode, the home screen otherwise. */
    private String root(){return tutorMode?"tutorHome":"home";}
    public void show(String destination){
        if(tutorMode&&"home".equals(destination))destination=root();
        if(missingSelection(destination))destination=root();
        if(!account.signedIn()&&(destination.equals("account")||destination.equals("edit_account")||destination.equals("otp")))destination="login";
        if(account.signedIn()&&(destination.equals("login")||destination.equals("register")))destination="account";
        if(getSupportFragmentManager().isStateSaved()){pendingDestination=destination;return;}
        Fragment fragment;
        switch(destination){
            case "login":fragment=new LoginFragment();break;
            case "register":fragment=new RegisterFragment();break;
            case "forgot":fragment=new ForgotPasswordFragment();break;
            case "account":fragment=new AccountFragment();break;
            case "edit_account":fragment=new EditAccountFragment();break;
            case "otp":fragment=new PhoneVerificationFragment();break;
            case "booking":fragment=new BookingFragment();break;
            case "trial":fragment=new TrialFragment();break;
            case "schedule":fragment=new ScheduleFragment();break;
            case "lesson":fragment=new LessonDetailFragment();break;
            case "confirm":fragment=new ConfirmLessonFragment();break;
            case "cancel":fragment=new CancelLessonFragment();break;
            case "reschedule":fragment=new RescheduleFragment();break;
            case "groups":fragment=new GroupSearchFragment();break;
            case "join":fragment=new JoinClassFragment();break;
            case "full":fragment=new ClassFullFragment();break;
            case "tutorHome":fragment=new TutorHomeFragment();break;
            case "teaching":fragment=new TeachingScheduleFragment();break;
            case "classes":fragment=new OpenedClassesFragment();break;
            case "openClass":fragment=new OpenClassFragment();break;
            case "review":fragment=new ReviewRegistrationsFragment();break;
            default:destination="home";fragment=new HomeFragment();
        }
        screen=destination;
        Fragment current=getSupportFragmentManager().findFragmentById(R.id.screen_container);
        // Showing the open lesson or class screen again rebuilds it with the saved data.
        if(current instanceof ScreenFragment&&screen.equals(current.getTag())){((ScreenFragment)current).refresh();renderNavigation();return;}
        if(current==null||!screen.equals(current.getTag()))getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true).replace(R.id.screen_container,fragment,screen).commit();
        renderNavigation();
    }
    private boolean navigateMenu(int itemId){
        if(updatingNavigation)return true;
        if(account.isBusy()||account.isSendingCode())return false;
        String destination;
        if(itemId==R.id.nav_login)destination="login";
        else if(itemId==R.id.nav_register)destination="register";
        else if(itemId==R.id.nav_account)destination="account";
        else if(itemId==R.id.nav_schedule)destination="schedule";
        else if(itemId==R.id.nav_tutor_home)destination="tutorHome";
        else if(itemId==R.id.nav_teaching)destination="teaching";
        else if(itemId==R.id.nav_classes)destination="classes";
        else destination="home";
        if(!screen.equals(destination)){
            account.clearFeedback();
            account.removeDraft("login.password");
            account.removeDraft("register.password");
            account.removeDraft("register.confirm");
            if("edit_account".equals(screen))account.clearDraft();
            hideKeyboard();show(destination);
        }
        return true;
    }
    private void renderNavigation(){
        updatingNavigation=true;
        boolean signedIn=account.signedIn();
        android.view.Menu menu=binding.bottomNavigation.getMenu();
        // A bottom bar holds at most five items, so tutor mode swaps in its own menu (without "Đăng ký").
        if(menu.findItem(tutorMode?R.id.nav_tutor_home:R.id.nav_home)==null){
            menu.clear();binding.bottomNavigation.inflateMenu(tutorMode?R.menu.menu_tutor_navigation:R.menu.menu_bottom_navigation);
        }
        menu.findItem(R.id.nav_login).setVisible(!signedIn);
        MenuItem register=menu.findItem(R.id.nav_register);
        if(register!=null)register.setVisible(!signedIn);
        menu.findItem(R.id.nav_account).setVisible(signedIn);
        for(int i=0;i<menu.size();i++)menu.getItem(i).setEnabled(!account.isBusy()&&!account.isSendingCode());
        MenuItem selected=menu.findItem(selectedItem(signedIn));
        if(selected==null)selected=menu.findItem(signedIn?R.id.nav_account:R.id.nav_login);
        selected.setChecked(true);
        binding.bottomNavigation.setVisibility(keyboardVisible?View.GONE:View.VISIBLE);
        updatingNavigation=false;
    }
    /** The bottom bar item of the section the open screen belongs to. */
    private int selectedItem(boolean signedIn){
        switch(screen){
            case "home":case "groups":case "join":case "full":return R.id.nav_home;
            case "booking":case "trial":return "schedule".equals(bookingOrigin)||"lesson".equals(bookingOrigin)?R.id.nav_schedule:R.id.nav_home;
            case "schedule":case "lesson":case "confirm":case "cancel":case "reschedule":return R.id.nav_schedule;
            case "tutorHome":return R.id.nav_tutor_home;
            case "teaching":return R.id.nav_teaching;
            case "classes":case "openClass":case "review":return R.id.nav_classes;
            default:return signedIn?R.id.nav_account:"register".equals(screen)?R.id.nav_register:R.id.nav_login;
        }
    }
    public void back(){
        if(account.isBusy())return;hideKeyboard();
        switch(screen){
            case "booking":show(bookingOrigin);break;
            case "trial":show("booking");break;
            case "lesson":show("schedule");break;
            case "confirm":case "cancel":case "reschedule":show("lesson");break;
            case "join":case "full":show("groups");break;
            case "openClass":case "review":show("classes");break;
            case "register":case "forgot":
                account.removeDraft("register.password");account.removeDraft("register.confirm");account.clearFeedback();show("login");break;
            case "edit_account":account.clearDraft();account.clearFeedback();show("account");break;
            case "otp":account.clearFeedback();show("account");break;
            default:account.removeDraft("login.password");show(root());
        }
    }

    // Lessons and group classes ------------------------------------------------------------------

    /** Screens of a lesson, class or tutor that no longer exists fall back to the start screen. */
    private boolean missingSelection(String destination){
        switch(destination){
            case "lesson":case "confirm":case "cancel":case "reschedule":return store.lesson(lessonId)==null;
            case "join":case "full":case "review":return store.groupClass(classId)==null;
            case "booking":case "trial":return Tutor.get(tutorId)==null;
            default:return false;
        }
    }
    /** Tutor profile from a lesson card: the catalog details of a Firestore tutor, a short summary of a sample one. */
    public void openTutor(Tutor tutor){
        if(!tutor.sample()&&catalog.teacher(tutor.catalogId)!=null){
            TutorDetailsDialog.create(tutor.catalogId).show(getSupportFragmentManager(),"tutor_details");return;
        }
        new AlertDialog.Builder(this).setTitle(tutor.name)
                .setMessage(tutor.subject+" · "+tutor.level+"\n"+tutor.ratingLabel()+"\nHọc phí: "+tutor.rateLabel())
                .setNegativeButton(R.string.close,null).setPositiveButton(R.string.book_lesson,(dialog,which)->openBooking(tutor)).show();
    }
    public void openBooking(Tutor tutor){
        tutorId=tutor.id;bookingDraft=null;
        if(!"booking".equals(screen)&&!"trial".equals(screen))bookingOrigin=screen;
        show("booking");
    }
    public void openLesson(Lesson lesson){lessonId=lesson.id;show("lesson");}
    public void openClass(GroupClass item,String destination){classId=item==null?-1:item.id;show(destination);}
    /** Demo switch between the sample student and the sample tutor (the account screen belongs to member 1). */
    public void setTutorMode(boolean value){
        tutorMode=value;preferences.edit().putBoolean("tutor_mode",value).apply();show(root());
    }
    public void message(Tutor tutor){message(tutor.name);}
    public void message(String person){dialog("Nhắn tin với "+person,"Chức năng trò chuyện (màn hình 05–06) do Thành viên 4 phụ trách và sẽ được nối vào đây.");}
    public void dialog(String title,String message){new AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton(R.string.understood,null).show();}
    public void notice(String message){
        Snackbar bar=Snackbar.make(binding.screenContainer,message,Snackbar.LENGTH_LONG);
        if(binding.bottomNavigation.getVisibility()==View.VISIBLE)bar.setAnchorView(binding.bottomNavigation);
        bar.show();
    }
    /** Saves a new booking or trial request unless it overlaps an existing lesson, then shows it under "Chờ xác nhận". */
    public boolean saveRequest(Lesson lesson){
        String clash=store.conflict(lesson.tutorId,lesson.student,lesson.date,lesson.hour,lesson.minutes,-1);
        if(clash!=null){dialog("Lịch học bị trùng",clash);return false;}
        lessons.add(lesson);store.save();hideKeyboard();scheduleTab=1;bookingDraft=null;show("schedule");
        notice("Đã gửi yêu cầu. Gia sư sẽ xác nhận yêu cầu của bạn.");
        return true;
    }
    @Override protected void onResumeFragments(){
        super.onResumeFragments();
        if(pendingDestination!=null){String destination=pendingDestination;pendingDestination=null;show(destination);}
    }
    @Override protected void onResume(){
        super.onResume();
        if(account!=null&&account.isSendingCode()&&account.signedIn()){
            account.sendingCode=false;account.sendCode(this,account.verificationPhone(),false);
        }
    }
    public void hideKeyboard(){
        View focus=getCurrentFocus();
        if(focus!=null)((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(focus.getWindowToken(),0);
    }
    @Override protected void onSaveInstanceState(Bundle out){
        super.onSaveInstanceState(out);out.putString("screen",screen);
        out.putInt("tutor",tutorId);out.putInt("tab",scheduleTab);out.putInt("classTab",classTab);
        out.putInt("lesson",lessonId);out.putInt("class",classId);out.putString("bookingOrigin",bookingOrigin);
        if(bookingDraft!=null)out.putBundle("booking",bookingDraft);
    }
}
