package com.example.tmdt;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
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
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Lessons and group classes (member 3): the state shared by their screens, their destinations and
 * their back navigation. MainActivity only forwards to this class, so the two features stay apart.
 */
public final class Classroom {
    private static final Set<String> PRIVATE = new HashSet<>(Arrays.asList("schedule","booking","trial","lesson","confirm","cancel",
            "reschedule","join","registrations","registration","tutorHome","teaching","classes","openClass","review","cancelClass","matches"));
    private static final Set<String> TUTOR = new HashSet<>(Arrays.asList("tutorHome","teaching","classes","openClass","review","cancelClass","matches"));
    /** Screens without text fields, rebuilt as soon as Firestore sends new data. */
    private static final Set<String> LIVE = new HashSet<>(Arrays.asList("schedule","lesson","reschedule","groups","full","registrations","registration",
            "tutorHome","teaching","classes","review","matches"));
    private final MainActivity host;
    private final SharedPreferences preferences;
    private final ClassroomCloud cloud;
    public final Ui ui;
    public final Store store;
    public final List<Lesson> lessons;
    private boolean tutorView;
    private String modeOwner="";
    public String tutorKey, lessonId="", classId="";
    public String tutorProfileId;
    public String tutorProfileOrigin="home";
    public int scheduleTab, classTab, registrationFilter;
    /** Where the registration history was opened from (class search or schedule), so back returns there. */
    private String registrationsOrigin="groups";
    /** Where "Đặt lịch học" was opened from, so back returns there. */
    private String bookingOrigin="home";
    /** Booking form kept while the trial screen is open or the activity is recreated. */
    public Bundle bookingDraft;
    /** Set by "Tìm lớp tương tự" so the class search opens with matching filters. */
    public String similarSubject;
    /** Day and view (week or month) shown on the tutor's teaching schedule. */
    public LocalDate teachingDay=LocalDate.now();
    public boolean teachingMonth;
    /** Group of students the next new class is filled in for, set by "Gợi ý ghép nhóm". */
    public GroupMatch.Group classDraft;
    /** Where the open class form was opened from, so back returns there. */
    private String openClassOrigin="classes";

    Classroom(MainActivity host,Bundle state){
        this.host=host;ui=new Ui(host);
        preferences=host.getSharedPreferences("tutor_demo",Context.MODE_PRIVATE);
        cloud=new ViewModelProvider(host).get(ClassroomCloud.class);
        store=cloud.store;lessons=store.lessons;
        modeOwner=host.account.signedIn()?host.account.uid():"";
        tutorView=!modeOwner.isEmpty()&&preferences.getBoolean("tutor_mode_"+modeOwner,false);
        if(state!=null){
            tutorKey=state.getString("tutor");scheduleTab=state.getInt("tab",0);classTab=state.getInt("classTab",0);
            lessonId=state.getString("lesson","");classId=state.getString("class","");
            bookingOrigin=state.getString("bookingOrigin","home");bookingDraft=state.getBundle("booking");
            tutorProfileId=state.getString("tutorProfileId",null);
            tutorProfileOrigin=state.getString("tutorProfileOrigin","home");
            registrationsOrigin=state.getString("registrationsOrigin","groups");registrationFilter=state.getInt("registrationFilter",0);
            openClassOrigin=state.getString("openClassOrigin","classes");
        }
        cloud.user(host.account.signedIn()?host.account.uid():"");
        cloud.changes().observe(host,ignored->changed());
    }
    void save(Bundle out){
        out.putString("tutor",tutorKey);out.putInt("tab",scheduleTab);out.putInt("classTab",classTab);
        out.putString("lesson",lessonId);out.putString("class",classId);out.putString("bookingOrigin",bookingOrigin);
        out.putString("tutorProfileId",tutorProfileId);
        out.putString("tutorProfileOrigin",tutorProfileOrigin);
        out.putString("registrationsOrigin",registrationsOrigin);out.putInt("registrationFilter",registrationFilter);
        out.putString("openClassOrigin",openClassOrigin);
        if(bookingDraft!=null)out.putBundle("booking",bookingDraft);
    }

    // Account ------------------------------------------------------------------------------------

    /** Follows sign-in, sign-out and the loaded profile (MainActivity calls this on every account change). */
    void onAccount(){
        String owner=host.account.signedIn()?host.account.uid():"";
        if(!owner.equals(modeOwner)){modeOwner=owner;tutorView=!owner.isEmpty()&&preferences.getBoolean("tutor_mode_"+owner,false);}
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
    /** A submitted application does not grant teaching access; approval is required. */
    public boolean isTutor(){UserProfile profile=host.account.profile();return host.account.signedIn()&&profile!=null&&profile.tutor
            &&"VERIFIED".equals(host.tutorReviews.status());}
    public boolean tutorMode(){return tutorView&&isTutor();}

    /** Last known answer to a proposal per lesson the user teaches, to announce new answers while the app is open. */
    private final Map<String,String> replies=new HashMap<>();
    private void announceReplies(){
        for(Lesson lesson:lessons){
            if(!lesson.tutorId.equals(me()))continue;
            String before=replies.put(lesson.id,lesson.reply);
            if(before!=null&&!before.equals(lesson.reply)&&lesson.replied())notice(lesson.replyText());
        }
    }
    /** Last known status of the classes the user joined or applied to, to announce a cancellation while the app is open. */
    private final Map<String,String> classStates=new HashMap<>();
    private void announceClasses(){
        for(GroupClass item:store.classes){
            boolean mine=item.members.contains(me());
            for(GroupClass.Registration registration:item.registrations)mine|=registration.studentId.equals(me());
            if(!mine)continue;
            String before=classStates.put(item.id,item.status);
            if(before!=null&&!before.equals(item.status)&&item.cancelled())notice("Lớp \""+item.title+"\" đã bị gia sư hủy.");
            String asked=item.members.contains(me())&&item.proposalOpen()?item.proposal.key():"";
            String previous=classMoves.put(item.id,asked);
            if(previous!=null&&!asked.isEmpty()&&!asked.equals(previous))
                notice("Gia sư đề nghị đổi giờ buổi "+item.proposal.number+" của lớp \""+item.title+"\". Mở Lịch học để trả lời.");
        }
    }
    /** The proposal each joined class last asked about, to announce a new one while the app is open. */
    private final Map<String,String> classMoves=new HashMap<>();
    /** Open classes with seats that suit one of the user's own tutor requests, each with the request it suits. */
    public Map<GroupClass,GroupMatch.Request> suggestions(){
        Map<GroupClass,GroupMatch.Request> map=new LinkedHashMap<>();
        for(GroupClass item:store.classes){
            if(!item.recruiting()||item.tutorId.equals(me())||item.attends(me()))continue;
            GroupMatch.Request request=GroupMatch.suits(item,store.myRequests);
            if(request!=null)map.put(item,request);
        }
        return map;
    }
    /** Classes last suggested to the user, to announce a new one while the app is open. */
    private Set<String> suggested;
    private String suggestedOwner="";
    private void announceMatches(){
        if(!store.matchesReady)return;
        Map<GroupClass,GroupMatch.Request> now=suggestions();
        if(suggested!=null&&me().equals(suggestedOwner)&&!tutorMode())
            for(Map.Entry<GroupClass,GroupMatch.Request> entry:now.entrySet())
                if(!suggested.contains(entry.getKey().id)){
                    notice("Lớp \""+entry.getKey().title+"\" hợp với tin đăng \""+entry.getValue().title+"\" của bạn. Mở Lớp học nhóm để xem.");break;
                }
        suggested=new HashSet<>();for(GroupClass item:now.keySet())suggested.add(item.id);
        suggestedOwner=me();
    }
    public boolean replySeen(Lesson lesson){
        return preferences.getStringSet("reply_seen_"+me(),new HashSet<>()).contains(lesson.id+":"+lesson.reply);
    }
    public void markReplySeen(Lesson lesson){
        Set<String> seen=new HashSet<>(preferences.getStringSet("reply_seen_"+me(),new HashSet<>()));
        seen.add(lesson.id+":"+lesson.reply);preferences.edit().putStringSet("reply_seen_"+me(),seen).apply();
    }

    private void changed(){
        announceReplies();announceClasses();announceMatches();
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
        if(tutorMode()&&("classes".equals(destination)||"matches".equals(destination)))cloud.watchRequests(false);
        return missingSelection(destination)?root():destination;
    }
    /** Screens of a lesson, class or tutor that no longer exists fall back to the start screen. */
    private boolean missingSelection(String destination){
        switch(destination){
            case "lesson":case "confirm":case "cancel":case "reschedule":return store.lesson(lessonId)==null;
            case "join":case "full":case "review":case "registration":case "cancelClass":return store.groupClass(classId)==null;
            case "booking":case "trial":return Tutor.get(tutorKey)==null;
            default:return false;
        }
    }
    /** The fragment of one of these screens, or null for a destination owned by another feature. */
    Fragment fragment(String destination){
        switch(destination){
            case "tutor_profile":return com.example.tmdt.ui.home.TutorProfileFragment.newInstance(tutorProfileId);
            case "booking":return new BookingFragment();
            case "trial":return new TrialFragment();
            case "lesson":return new LessonDetailFragment();
            case "confirm":return new ConfirmLessonFragment();
            case "cancel":return new CancelLessonFragment();
            case "reschedule":return new RescheduleFragment();
            case "groups":return new GroupSearchFragment();
            case "join":return new JoinClassFragment();
            case "full":return new ClassFullFragment();
            case "registrations":return new com.example.tmdt.ui.groupclass.RegistrationHistoryFragment();
            case "registration":return new com.example.tmdt.ui.groupclass.RegistrationDetailFragment();
            case "tutorHome":return new TutorHomeFragment();
            case "teaching":return new TeachingScheduleFragment();
            case "classes":return new OpenedClassesFragment();
            case "openClass":return new OpenClassFragment();
            case "matches":return new com.example.tmdt.ui.tutor.GroupMatchFragment();
            case "review":return new ReviewRegistrationsFragment();
            case "cancelClass":return new com.example.tmdt.ui.tutor.CancelClassFragment();
            default:return null;
        }
    }
    boolean mainScreen(String screen){
        switch(screen){
            case "tutorHome":case "teaching":case "classes":case "groups":case "matches":
            case "lesson":case "confirm":case "cancel":case "reschedule":case "full":case "review":case "registrations":case "registration":case "cancelClass":return true;
            default:return false;
        }
    }
    /** Labels the bar for the current mode and returns the item to check, or the given default for other screens. */
    int navigationItem(Menu menu,String screen,int fallback){
        menu.findItem(R.id.nav_home).setTitle(tutorMode()?R.string.page_tutor_home:R.string.page_home);
        menu.findItem(R.id.nav_schedule).setTitle(tutorMode()?R.string.page_teaching:R.string.page_schedule);
        switch(screen){
            case "tutorHome":case "groups":case "join":case "full":case "classes":case "openClass":case "review":case "cancelClass":case "matches":return R.id.nav_home;
            case "teaching":case "lesson":case "confirm":case "cancel":case "reschedule":return R.id.nav_schedule;
            case "registrations":case "registration":return "schedule".equals(registrationsOrigin)?R.id.nav_schedule:R.id.nav_home;
            default:return fallback;
        }
    }
    /** Where back leads from one of these screens, or null for other screens. */
    String backTarget(String screen){
        switch(screen){
            case "tutor_profile":return tutorProfileOrigin!=null?tutorProfileOrigin:"home";
            case "booking":return bookingOrigin;
            case "trial":return "booking";
            case "lesson":return "schedule";
            case "confirm":case "cancel":case "reschedule":return "lesson";
            case "join":case "full":return "groups";
            case "openClass":return openClassOrigin;
            case "review":case "matches":return "classes";
            case "cancelClass":return "review";
            case "registrations":return registrationsOrigin;
            case "registration":return "registrations";
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
        openTutor(tutorId,name,"home");
    }
    public void openTutor(String tutorId,String name,String origin){
        this.tutorProfileId=tutorId;
        this.tutorProfileOrigin=origin!=null?origin:"home";
        this.bookingOrigin=origin!=null?origin:"home";
        show("tutor_profile");
    }
    public void openBooking(Tutor tutor){
        tutorKey=tutor.key;bookingDraft=null;
        if(!"booking".equals(host.screen)&&!"trial".equals(host.screen))bookingOrigin=host.screen;
        show("booking");
    }
    public void openLesson(Lesson lesson){lessonId=lesson.id;show("lesson");}
    public void openClass(GroupClass item,String destination){
        classId=item==null?"":item.id;
        if("openClass".equals(destination)){openClassOrigin="classes";classDraft=null;}
        show(destination);
    }
    /** Opens the new class form filled in for a group of students who posted the same need. */
    public void openDraft(GroupMatch.Group group){classId="";classDraft=group;openClassOrigin="matches";show("openClass");}
    public void retryRequests(){cloud.watchRequests(true);show(host.screen);}
    /** Lịch sử đăng ký lớp học, opened from the class search or the schedule. */
    public void openRegistrations(){
        if(!"registrations".equals(host.screen)&&!"registration".equals(host.screen))registrationsOrigin=host.screen;
        show("registrations");
    }
    public void openRegistration(GroupClass item){classId=item.id;show("registration");}
    /** Tutors switch between their teaching view and their own student view. */
    public void setTutorMode(boolean value){
        if(value&&!isTutor())return;
        tutorView=value;preferences.edit().putBoolean("tutor_mode_"+host.account.uid(),value).apply();show(root());
    }
    public void openRoom(Lesson lesson){
        if(!host.payments.paid("LESSON",lesson.id)){host.openPayment("LESSON",lesson.id);return;}
        if(!lesson.roomOpen()){dialog("Phòng học trực tuyến","Phòng học mở trước giờ bắt đầu 10 phút và đóng khi buổi học kết thúc.");return;}
        openUrl(lesson.roomUrl());
    }
    /** A group class has one room for its tutor and members, open around each session. */
    public void openClassRoom(GroupClass item){
        if(!item.tutorId.equals(me())&&!item.members.contains(me())){dialog("Phòng học trực tuyến","Chỉ gia sư và học viên của lớp mới vào được phòng học.");return;}
        if(!item.roomOpen()){dialog("Phòng học trực tuyến","Phòng học mở trước giờ bắt đầu 10 phút và đóng khi buổi học kết thúc.");return;}
        openUrl(item.roomUrl());
    }
    private void openUrl(String url){
        try{host.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}
        catch(ActivityNotFoundException missing){dialog("Phòng học trực tuyến","Không tìm thấy trình duyệt để mở phòng học.\n"+url);}
    }
    /**
     * "Nhắn tin" (UC21, member 4): mở màn 06 với người có tên này, kèm buổi học còn hiệu lực gần nhất giữa hai người.
     * Không xác định được tài khoản của người đó thì mở danh sách tin nhắn lọc sẵn theo tên.
     */
    public void message(String person){
        String name=person==null?"":person.trim();
        Lesson lesson=chatLesson(name);
        if(lesson!=null){
            boolean iTeach=me().equals(lesson.tutorId);
            String peerId=iTeach?lesson.studentId:tutorUid(lesson.tutorId);
            if(peerId!=null&&!peerId.isEmpty()){
                host.openChat(peerId,iTeach?lesson.studentName:lesson.tutorName,iTeach?"STUDENT":"TUTOR",
                        Conversation.lesson(lesson.title,lesson.dateLabel()+" · "+lesson.timeLabel(),lesson.statusLabel()));
                return;
            }
        }
        CatalogState.Teacher teacher=host.catalog.teacherNamed(name);
        if(teacher!=null&&!teacher.userId.isEmpty()&&!teacher.userId.equals(me())){host.openChat(teacher.userId,teacher.name,"TUTOR",null);return;}
        host.openMessages(name);
    }
    /** UID tài khoản của gia sư: user_id trong hồ sơ gia sư, mặc định chính mã hồ sơ (Rules bắt buộc user_id == mã hồ sơ). */
    private String tutorUid(String tutorId){
        CatalogState.Teacher teacher=host.catalog.teacher(tutorId);
        return teacher!=null&&!teacher.userId.isEmpty()?teacher.userId:tutorId;
    }
    /** Buổi học còn hiệu lực sắp tới nhất giữa mình và người có tên này; nếu không có thì buổi gần đây nhất. */
    private Lesson chatLesson(String person){
        String key=CatalogState.normalize(person).trim(),mine=me();
        if(key.isEmpty()||mine.isEmpty())return null;
        Lesson upcoming=null,recent=null;
        for(Lesson lesson:lessons){
            boolean withTutor=mine.equals(lesson.studentId)&&CatalogState.normalize(lesson.tutorName).trim().equals(key);
            boolean withStudent=mine.equals(lesson.tutorId)&&CatalogState.normalize(lesson.studentName).trim().equals(key);
            if((!withTutor&&!withStudent)||!lesson.active())continue;
            if(!lesson.ended()){if(upcoming==null||lesson.start().isBefore(upcoming.start()))upcoming=lesson;}
            else if(recent==null||lesson.start().isAfter(recent.start()))recent=lesson;
        }
        return upcoming!=null?upcoming:recent;
    }
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
