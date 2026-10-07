package com.example.tmdt;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Lessons and group classes (member 3): the state shared by their screens, their destinations and
 * their back navigation. MainActivity only forwards to this class, so the two features stay apart.
 * Data is the sample data saved on the device, see {@link Store}.
 */
public final class Classroom {
    private final MainActivity host;
    private final SharedPreferences preferences;
    public final Ui ui;
    public final Store store;
    public final List<Lesson> lessons;
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

    Classroom(MainActivity host,Bundle state){
        this.host=host;ui=new Ui(host);
        preferences=host.getSharedPreferences("tutor_demo",Context.MODE_PRIVATE);
        store=new Store(preferences);lessons=store.lessons;
        tutorMode=preferences.getBoolean("tutor_mode",false);
        if(state!=null){
            tutorId=state.getInt("tutor",0);scheduleTab=state.getInt("tab",0);classTab=state.getInt("classTab",0);
            lessonId=state.getInt("lesson",-1);classId=state.getInt("class",-1);
            bookingOrigin=state.getString("bookingOrigin","home");bookingDraft=state.getBundle("booking");
        }
    }
    void save(Bundle out){
        out.putInt("tutor",tutorId);out.putInt("tab",scheduleTab);out.putInt("classTab",classTab);
        out.putInt("lesson",lessonId);out.putInt("class",classId);out.putString("bookingOrigin",bookingOrigin);
        if(bookingDraft!=null)out.putBundle("booking",bookingDraft);
    }

    // Navigation hooks used by MainActivity ------------------------------------------------------

    /** The start screen: the tutor overview in tutor mode, the home screen otherwise. */
    String root(){return tutorMode?"tutorHome":"home";}
    /** Tutor mode reuses the bottom bar: "Trang chủ" opens the overview and "Lịch học" the teaching schedule. */
    String redirect(String destination){
        // Class statuses follow the clock, so they are brought up to date before any screen reads them.
        store.updateClasses();
        if(tutorMode&&"home".equals(destination))return "tutorHome";
        if(tutorMode&&"schedule".equals(destination))return "teaching";
        return missingSelection(destination)?root():destination;
    }
    /** Screens of a lesson, class or tutor that no longer exists fall back to the start screen. */
    private boolean missingSelection(String destination){
        switch(destination){
            case "lesson":case "confirm":case "cancel":case "reschedule":return store.lesson(lessonId)==null;
            case "join":case "full":case "review":return store.groupClass(classId)==null;
            case "booking":case "trial":return Tutor.get(tutorId)==null;
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
    /** Tab roots that keep the bottom bar; forms and detail screens hide it like the account forms do. */
    boolean mainScreen(String screen){
        return "tutorHome".equals(screen)||"teaching".equals(screen)||"classes".equals(screen)||"groups".equals(screen);
    }
    /** Labels the bar for the current mode and returns the item to check, or the given default for other screens. */
    int navigationItem(Menu menu,String screen,int fallback){
        menu.findItem(R.id.nav_home).setTitle(tutorMode?R.string.page_tutor_home:R.string.page_home);
        menu.findItem(R.id.nav_schedule).setTitle(tutorMode?R.string.page_teaching:R.string.page_schedule);
        switch(screen){
            case "tutorHome":case "groups":case "classes":return R.id.nav_home;
            case "teaching":return R.id.nav_schedule;
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
    /** Tutor profile from a lesson card: the catalog details of a Firestore tutor, a short summary of a sample one. */
    public void openTutor(Tutor tutor){
        if(!tutor.sample()&&host.catalog.teacher(tutor.catalogId)!=null){
            TutorDetailsDialog.create(tutor.catalogId).show(host.getSupportFragmentManager(),"tutor_details");return;
        }
        new AlertDialog.Builder(host).setTitle(tutor.name)
                .setMessage(tutor.subject+" · "+tutor.level+"\n"+tutor.ratingLabel()+"\nHọc phí: "+tutor.rateLabel())
                .setNegativeButton(R.string.close,null).setPositiveButton(R.string.book_lesson,(dialog,which)->openBooking(tutor)).show();
    }
    public void openBooking(Tutor tutor){
        tutorId=tutor.id;bookingDraft=null;
        if(!"booking".equals(host.screen)&&!"trial".equals(host.screen))bookingOrigin=host.screen;
        show("booking");
    }
    /** Subjects to filter classes by: those published on Firestore, then any other subject an active class uses. */
    public List<String> subjects(){
        Set<String> names=new LinkedHashSet<>(host.catalog.subjects().values());
        for(GroupClass item:store.classes)if(item.active())names.add(item.subject);
        names.remove("");
        return new ArrayList<>(names);
    }
    public void openLesson(Lesson lesson){lessonId=lesson.id;show("lesson");}
    public void openClass(GroupClass item,String destination){classId=item==null?-1:item.id;show(destination);}
    /** Demo switch between the sample student and the sample tutor. */
    public void setTutorMode(boolean value){
        tutorMode=value;preferences.edit().putBoolean("tutor_mode",value).apply();show(root());
    }
    public void message(Tutor tutor){message(tutor.name);}
    public void message(String person){dialog("Nhắn tin với "+person,"Chức năng trò chuyện (màn hình 05–06) do Thành viên 4 phụ trách và sẽ được nối vào đây.");}
    public void dialog(String title,String message){new AlertDialog.Builder(host).setTitle(title).setMessage(message).setPositiveButton(R.string.understood,null).show();}
    public void notice(String message){
        Snackbar bar=Snackbar.make(host.findViewById(R.id.screen_container),message,Snackbar.LENGTH_LONG);
        View navigation=host.findViewById(R.id.bottom_navigation);
        if(navigation.getVisibility()==View.VISIBLE)bar.setAnchorView(navigation);
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
}
