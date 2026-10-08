package com.example.tmdt;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.tmdt.ui.booking.BookingFragment;
import com.example.tmdt.ui.booking.TrialFragment;
import com.example.tmdt.ui.common.Ui;
import com.example.tmdt.ui.groupclass.ClassFullFragment;
import com.example.tmdt.ui.groupclass.GroupSearchFragment;
import com.example.tmdt.ui.groupclass.JoinClassFragment;
import com.example.tmdt.ui.home.TutorDetailsDialog;
import com.example.tmdt.ui.schedule.CancelLessonFragment;
import com.example.tmdt.ui.schedule.ConfirmLessonFragment;
import com.example.tmdt.ui.schedule.LessonDetailFragment;
import com.example.tmdt.ui.schedule.RescheduleFragment;
import com.example.tmdt.ui.tutor.OpenClassFragment;
import com.example.tmdt.ui.tutor.OpenedClassesFragment;
import com.example.tmdt.ui.tutor.ReviewRegistrationsFragment;
import com.example.tmdt.ui.tutor.TeachingScheduleFragment;
import com.example.tmdt.ui.tutor.TutorHomeFragment;
import com.google.android.material.snackbar.Snackbar;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Lessons and group classes (member 3): the state shared by their screens, their destinations and
 * their back navigation. MainActivity only forwards to this class, so the two features stay apart.
 */
public final class Classroom {
    private static final Set<String> PRIVATE = new HashSet<>(Arrays.asList("schedule","booking","trial","lesson","confirm","cancel",
            "reschedule","join","tutorHome","teaching","classes","openClass","review"));
    private static final Set<String> TUTOR = new HashSet<>(Arrays.asList("tutorHome","teaching","classes","openClass","review"));
    /** Screens without text fields, rebuilt as soon as Firestore sends new data. */
    private static final Set<String> LIVE = new HashSet<>(Arrays.asList("schedule","lesson","reschedule","groups","full",
            "tutorHome","teaching","classes","review"));
    private final MainActivity host;
    private final SharedPreferences preferences;
    private final ClassroomCloud cloud;
    public final Ui ui;
    public final Store store;
    public final List<Lesson> lessons;
    private boolean tutorView;
    public String tutorKey, lessonId="", classId="";
    public int scheduleTab, classTab;
    /** Where "Đặt lịch học" was opened from, so back returns there. */
    private String bookingOrigin="home";
    /** Booking form kept while the trial screen is open or the activity is recreated. */
    public Bundle bookingDraft;
    /** Set by "Tìm lớp tương tự" so the class search opens with matching filters. */
    public String similarSubject;
    /** Day and view (week or month) shown on the tutor's teaching schedule. */
    public LocalDate teachingDay=LocalDate.now();
    public boolean teachingMonth;

    Classroom(MainActivity host,Bundle state){
        this.host=host;ui=new Ui(host);
        preferences=host.getSharedPreferences("tutor_demo",Context.MODE_PRIVATE);
        cloud=new ViewModelProvider(host).get(ClassroomCloud.class);
        store=cloud.store;lessons=store.lessons;
        tutorView=preferences.getBoolean("tutor_mode",false);
        if(state!=null){
            tutorKey=state.getString("tutor");scheduleTab=state.getInt("tab",0);classTab=state.getInt("classTab",0);
            lessonId=state.getString("lesson","");classId=state.getString("class","");
            bookingOrigin=state.getString("bookingOrigin","home");bookingDraft=state.getBundle("booking");
        }
        cloud.user(host.account.signedIn()?host.account.uid():"");
        cloud.changes().observe(host,ignored->changed());
    }
    void save(Bundle out){
        out.putString("tutor",tutorKey);out.putInt("tab",scheduleTab);out.putInt("classTab",classTab);
        out.putString("lesson",lessonId);out.putString("class",classId);out.putString("bookingOrigin",bookingOrigin);
        if(bookingDraft!=null)out.putBundle("booking",bookingDraft);
    }

    // Account ------------------------------------------------------------------------------------

    /** Follows sign-in, sign-out and the loaded profile (MainActivity calls this on every account change). */
    void onAccount(){
        cloud.user(host.account.signedIn()?host.account.uid():"");
        if(tutorMode()&&"home".equals(host.screen))show("tutorHome");
    }
    public String me(){return store.user;}
    public String myName(){
        UserProfile profile=host.account.profile();
        return profile!=null&&!profile.name.isEmpty()?profile.name:"Học viên";
    }
    /** The name students see on the tutor's classes: the public tutor profile, or the account name. */
    public String tutorName(){
        CatalogState.Teacher teacher=host.catalog.teacher(me());
        return teacher!=null?teacher.name:myName();
    }
    /** Tutor accounts are marked la_gia_su by an administrator (UC28). */
    public boolean isTutor(){UserProfile profile=host.account.profile();return host.account.signedIn()&&profile!=null&&profile.tutor;}
    public boolean tutorMode(){return tutorView&&isTutor();}

    private void changed(){
        String error=cloud.takeError();
        if(!error.isEmpty())dialog("Lịch học và lớp nhóm",error);
        if(LIVE.contains(host.screen))show(host.screen);
    }

    // Navigation hooks used by MainActivity ------------------------------------------------------

    /** The start screen: the tutor overview in tutor mode, the home screen otherwise. */
    String root(){return tutorMode()?"tutorHome":"home";}
    String redirect(String destination){
        store.updateClasses();
        if(!host.account.signedIn()&&PRIVATE.contains(destination))return "login";
        if(TUTOR.contains(destination)&&!tutorMode())return root();
        if(tutorMode()&&"home".equals(destination))return "tutorHome";
        if(tutorMode()&&"schedule".equals(destination))return "teaching";
        return missingSelection(destination)?root():destination;
    }
    /** Screens of a lesson, class or tutor that no longer exists fall back to the start screen. */
    private boolean missingSelection(String destination){
        switch(destination){
            case "lesson":case "confirm":case "cancel":case "reschedule":return store.lesson(lessonId)==null;
            case "join":case "full":case "review":return store.groupClass(classId)==null;
            case "booking":case "trial":return Tutor.get(tutorKey)==null;
            default:return false;
        }
    }
    /** The fragment of one of these screens, or null for a destination owned by another feature. */
    Fragment fragment(String destination){
        switch(destination){
            case "booking":return new BookingFragment();
            case "trial":return new TrialFragment();
            case "lesson":return new LessonDetailFragment();
            case "confirm":return new ConfirmLessonFragment();
            case "cancel":return new CancelLessonFragment();
            case "reschedule":return new RescheduleFragment();
            case "groups":return new GroupSearchFragment();
            case "join":return new JoinClassFragment();
            case "full":return new ClassFullFragment();
            case "tutorHome":return new TutorHomeFragment();
            case "teaching":return new TeachingScheduleFragment();
            case "classes":return new OpenedClassesFragment();
            case "openClass":return new OpenClassFragment();
            case "review":return new ReviewRegistrationsFragment();
            default:return null;
        }
    }
    boolean mainScreen(String screen){
        switch(screen){
            case "tutorHome":case "teaching":case "classes":case "groups":
            case "lesson":case "confirm":case "cancel":case "reschedule":case "full":case "review":return true;
            default:return false;
        }
    }
    /** Labels the bar for the current mode and returns the item to check, or the given default for other screens. */
    int navigationItem(Menu menu,String screen,int fallback){
        menu.findItem(R.id.nav_home).setTitle(tutorMode()?R.string.page_tutor_home:R.string.page_home);
        menu.findItem(R.id.nav_schedule).setTitle(tutorMode()?R.string.page_teaching:R.string.page_schedule);
        switch(screen){
            case "tutorHome":case "groups":case "join":case "full":case "classes":case "openClass":case "review":return R.id.nav_home;
            case "teaching":case "lesson":case "confirm":case "cancel":case "reschedule":return R.id.nav_schedule;
            default:return fallback;
        }
    }
    /** Where back leads from one of these screens, or null for other screens. */
    String backTarget(String screen){
        switch(screen){
            case "booking":return bookingOrigin;
            case "trial":return "booking";
            case "lesson":return "schedule";
            case "confirm":case "cancel":case "reschedule":return "lesson";
            case "join":case "full":return "groups";
            case "openClass":case "review":return "classes";
            default:return null;
        }
    }

    // Actions used by the screens ------------------------------------------------------------------

    public Context context(){return host;}
    public void show(String destination){host.show(destination);}
    public void back(){host.back();}
    public void hideKeyboard(){host.hideKeyboard();}
    /** Subjects to filter classes by: those published on Firestore, then any other subject an active class uses. */
    public List<String> subjects(){
        Set<String> names=new LinkedHashSet<>(host.catalog.subjects().values());
        for(GroupClass item:store.classes)if(item.active())names.add(item.subject);
        names.remove("");
        return new ArrayList<>(names);
    }
    public String teaches(String tutorId){
        CatalogState.Teacher teacher=host.catalog.teacher(tutorId);
        if(teacher==null||teacher.offerings.isEmpty())return "";
        CatalogState.Offering offering=teacher.offerings.get(0);
        return host.catalog.subjects().get(offering.subjectId)+" · "+CatalogState.level(offering.level);
    }
    /** Public profile of a lesson's or class's tutor, from the home screen catalog when it is still listed. */
    public void openTutor(String tutorId,String name){
        if(host.catalog.teacher(tutorId)!=null){
            TutorDetailsDialog.create(tutorId).show(host.getSupportFragmentManager(),"tutor_details");return;
        }
        dialog(name,"Gia sư hiện không nhận lớp mới nên hồ sơ không còn hiển thị trên trang chủ.");
    }
    public void openBooking(Tutor tutor){
        tutorKey=tutor.key;bookingDraft=null;
        if(!"booking".equals(host.screen)&&!"trial".equals(host.screen))bookingOrigin=host.screen;
        show("booking");
    }
    public void openLesson(Lesson lesson){lessonId=lesson.id;show("lesson");}
    public void openClass(GroupClass item,String destination){classId=item==null?"":item.id;show(destination);}
    /** Tutors switch between their teaching view and their own student view. */
    public void setTutorMode(boolean value){
        tutorView=value;preferences.edit().putBoolean("tutor_mode",value).apply();show(root());
    }
    public void message(String person){dialog("Nhắn tin với "+person,"Chức năng trò chuyện (màn hình 05–06) do Thành viên 4 phụ trách và sẽ được nối vào đây.");}
    public void dialog(String title,String message){ui.dialog().setTitle(title).setMessage(message).setPositiveButton(R.string.understood,null).show();}
    public void notice(String message){
        Snackbar bar=Snackbar.make(host.findViewById(R.id.screen_container),message,Snackbar.LENGTH_LONG);
        View navigation=host.findViewById(R.id.bottom_navigation);
        if(navigation.getVisibility()==View.VISIBLE)bar.setAnchorView(navigation);
        bar.show();
    }
    /** A booking or trial request from the signed-in student to the tutor chosen on the home screen. */
    public Lesson request(Tutor tutor,String title,LocalDate date,int hour,int minutes,String mode,String goal,String address,boolean trial,int price){
        return new Lesson(store.newId("lessons"),tutor.id,tutor.name,me(),myName(),title,date.toString(),hour,minutes,mode,goal,address,trial,price,Lesson.PENDING);
    }
    /** Saves a new booking or trial request unless a rule refuses it, then shows it under "Chờ xác nhận". */
    public boolean saveRequest(Lesson lesson){
        String error=store.request(lesson);
        if(error!=null){dialog("Chưa gửi được yêu cầu",error);return false;}
        hideKeyboard();scheduleTab=1;bookingDraft=null;show("schedule");
        notice("Đã gửi yêu cầu. Gia sư sẽ xác nhận yêu cầu của bạn.");
        return true;
    }
}
