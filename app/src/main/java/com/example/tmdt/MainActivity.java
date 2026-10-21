package com.example.tmdt;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import androidx.activity.OnBackPressedCallback;
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
import com.example.tmdt.ui.home.HomeFragment;
import com.example.tmdt.ui.profile.AccountFragment;
import com.example.tmdt.ui.profile.EditAccountFragment;
import com.example.tmdt.ui.common.NavigationAssets;
import com.example.tmdt.ui.schedule.ScheduleFragment;
import com.example.tmdt.ui.messages.ChatFragment;
import com.example.tmdt.ui.messages.MessagesFragment;
import com.example.tmdt.ui.common.ScreenFragment;
import com.google.android.material.badge.BadgeDrawable;
import java.util.Map;

/** Hosts independent fragments. Layouts, form handling and Firebase access live outside this activity. */
public class MainActivity extends AppCompatActivity {
    AccountState account;
    CatalogState catalog;
    /** Tin nhắn (member 4, UC21). */
    public ChatState chat;
    TutorReviewState tutorReviews;
    /** Lessons and group classes (member 3). */
    public Classroom classroom;
    /** Hợp đồng dạy (member 4, UC23). */
    public Exchange exchange;
    String screen="home";
    private String pendingDestination;
    private String authenticatedDestination;
    private String selectedRequestId="";
    private String requestManagementOrigin="home";
    private String postRequestOrigin="home";
    private ActivityMainBinding binding;
    private boolean updatingNavigation;
    private boolean keyboardVisible;
    private int bottomInset,systemBottomInset;
    private String navigatingTo;
    private final com.example.tmdt.ui.common.FloatingNavigationInsets navigationInsets=new com.example.tmdt.ui.common.FloatingNavigationInsets();
    private final android.view.ViewTreeObserver.OnGlobalLayoutListener navigationLayout=this::updateNavigationPadding;

    @Override public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(),false);
        binding=ActivityMainBinding.inflate(getLayoutInflater());setContentView(binding.getRoot());
        account=new ViewModelProvider(this).get(AccountState.class);
        catalog=new ViewModelProvider(this).get(CatalogState.class);
        chat=new ViewModelProvider(this).get(ChatState.class);
        tutorReviews=new ViewModelProvider(this).get(TutorReviewState.class);
        classroom=new Classroom(this,savedInstanceState);
        exchange=new Exchange(this,savedInstanceState);
        ViewCompat.setOnApplyWindowInsetsListener(binding.appRoot,(view,insets)->{
            Insets safe=insets.getInsets(WindowInsetsCompat.Type.systemBars()|WindowInsetsCompat.Type.displayCutout()|WindowInsetsCompat.Type.ime());
            bottomInset=safe.bottom;
            systemBottomInset=insets.getInsets(WindowInsetsCompat.Type.systemBars()|WindowInsetsCompat.Type.displayCutout()).bottom;
            view.setPadding(safe.left,safe.top,safe.right,safe.bottom);
            keyboardVisible=insets.isVisible(WindowInsetsCompat.Type.ime());
            updateNavigationVisibility();
            return WindowInsetsCompat.CONSUMED;
        });
        WindowCompat.getInsetsController(getWindow(),getWindow().getDecorView()).setAppearanceLightStatusBars(true);
        WindowCompat.getInsetsController(getWindow(),getWindow().getDecorView()).setAppearanceLightNavigationBars(true);
        getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().setNavigationBarContrastEnforced(false);
        NavigationAssets.apply(binding.bottomNavigation);
        binding.bottomNavigation.setBackgroundTintList(null);
        binding.bottomNavigation.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        binding.navigationGlass.setOutlineProvider(android.view.ViewOutlineProvider.BACKGROUND);
        binding.navigationGlass.setClipToOutline(true);
        binding.navigationGlass.setOutlineAmbientShadowColor(0x246583AD);
        binding.navigationGlass.setOutlineSpotShadowColor(0x246583AD);
        binding.navigationGlass.setupWith(binding.navigationBlurTarget,4f,false)
                .setFrameClearDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.WHITE))
                .setBlurRadius(8f);
        binding.screenContainer.getViewTreeObserver().addOnGlobalLayoutListener(navigationLayout);
        binding.bottomNavigation.setItemActiveIndicatorEnabled(false);
        binding.bottomNavigation.setOnItemSelectedListener(item->navigateMenu(item.getItemId()));
        binding.bottomNavigation.setOnItemReselectedListener(item->navigateMenu(item.getItemId()));
        getOnBackPressedDispatcher().addCallback(this,new OnBackPressedCallback(true){
            @Override public void handleOnBackPressed(){if(classroom.root().equals(screen))finish();else back();}
        });
        Onboarding intro=new Onboarding(this);
        if(account.signedIn()&&!intro.introduced())intro.existingAccount();
        String initial=!intro.introduced()?"welcome":intro.pending()&&!account.signedIn()?"register":"home";
        screen=savedInstanceState==null?initial:savedInstanceState.getString("screen",initial);
        authenticatedDestination=savedInstanceState==null?null:savedInstanceState.getString("authenticated_destination");
        if(savedInstanceState!=null){
            selectedRequestId=savedInstanceState.getString("request_id","");
            requestManagementOrigin=savedInstanceState.getString("request_origin","home");
            postRequestOrigin=savedInstanceState.getString("post_request_origin","home");
        }
        if(savedInstanceState==null&&getIntent()!=null&&getIntent().hasExtra("screen")){
            screen=getIntent().getStringExtra("screen");
            if(getIntent().hasExtra("tutor_id")) classroom.tutorProfileId=getIntent().getStringExtra("tutor_id");
        }
        show(screen);
        account.changes().observe(this,ignored->{
            tutorReviews.account(account);
            classroom.onAccount();
            if(account.destination!=null){
                String destination=account.destination;
                account.destination=null;
                if(authenticatedDestination!=null
                        &&("home".equals(destination)||"account".equals(destination))
                        &&account.signedIn()
                        &&account.profile()!=null
                        &&"ACTIVE".equals(account.profile().status)){
                    destination=authenticatedDestination;
                    authenticatedDestination=null;
                }
                show(destination);
            }
            // Tin nhắn theo tài khoản đang đăng nhập; đăng xuất thì bỏ dữ liệu cũ (UC21).
            if(account.signedIn())chat.start();
            else chat.reset();
            renderNavigation();
        });
        tutorReviews.changes.observe(this,ignored->{
            if(tutorReviews.destination!=null){String next=tutorReviews.destination;tutorReviews.destination=null;show(next);}
            else if(classroom.tutorMode()&&"home".equals(screen))show("tutorHome");
            else if(!classroom.isTutor()&&java.util.Arrays.asList("tutorHome","teaching","classes","openClass","review").contains(screen))show("tutor_pending");
            renderNavigation();
        });
        chat.changes().observe(this,ignored->renderMessageBadge());
    }
    @Override protected void onNewIntent(android.content.Intent intent){
        super.onNewIntent(intent);setIntent(intent);
        if(intent!=null&&intent.hasExtra("screen")){
            if(intent.hasExtra("tutor_id")) classroom.tutorProfileId=intent.getStringExtra("tutor_id");
            show(intent.getStringExtra("screen"));
        }
    }
    public void show(String destination){
        if("email_otp".equals(destination))destination="email_verification"; // Older saved navigation state.
        if(account.requiresEmailVerification())destination="email_verification";
        if("email_verification".equals(destination)&&!account.requiresEmailVerification())destination="login";
        destination=classroom.redirect(destination);
        destination=exchange.redirect(destination);
        if("post_request".equals(destination)&&!"post_request".equals(screen))
            postRequestOrigin="my_requests".equals(screen)?"my_requests":"home";
        if(isRequestScreen(destination)&&!account.signedIn()){
            authenticatedDestination=destination;destination="login";
        }
        if(("edit_request".equals(destination)||"request_applicants".equals(destination))&&selectedRequestId.isEmpty())
            destination="my_requests";
        // Màn 06 cần đăng nhập và một hội thoại đang mở (UC21).
        if("chat".equals(destination)&&(!account.signedIn()||chat.openId()==null))destination=account.signedIn()?"messages":"login";
        if(!account.signedIn()&&(destination.equals("account")
                ||destination.equals("edit_account")
                ||destination.equals("otp")
                ||destination.equals("register_tutor")
                ||destination.equals("review_tutor")
                ||destination.equals("tutor_evidence")||destination.equals("tutor_pending")
                ||destination.equals("tutor_submitted")||destination.equals("tutor_supplement")||destination.equals("admin_tutors")
                ||destination.equals("favorites")
                ||destination.equals("settings")))
            destination="login";
        if((destination.equals("register_tutor")||destination.equals("review_tutor")||destination.equals("tutor_evidence"))&&account.profile()!=null&&account.profile().tutor)destination="tutor_pending";
        if(destination.equals("admin_tutors")&&(account.profile()==null||!"ADMIN".equals(account.profile().role)))destination="account";
        if(destination.equals("tutor_evidence")&&account.tutorRegistration()==null)destination="register_tutor";
        if(destination.equals("review_tutor")&&account.tutorRegistration()==null)destination="register_tutor";
        if(account.signedIn()&&(destination.equals("login")||destination.equals("register")))destination="account";
        if(getSupportFragmentManager().isStateSaved()){pendingDestination=destination;return;}
        if(destination.equals(navigatingTo))return;
        Fragment fragment=classroom.fragment(destination);
        if(fragment==null)fragment=exchange.fragment(destination);
        if(fragment==null)switch(destination){
            case "tutor_evidence":fragment=new com.example.tmdt.ui.profile.TutorEvidenceFragment();break;
            case "tutor_supplement":fragment=com.example.tmdt.ui.profile.TutorEvidenceFragment.supplementScreen();break;
            case "tutor_pending":fragment=new com.example.tmdt.ui.profile.PendingTutorFragment();break;
            case "tutor_submitted":fragment=new com.example.tmdt.ui.profile.SubmittedTutorFragment();break;
            case "admin_tutors":fragment=new com.example.tmdt.ui.profile.AdminTutorReviewFragment();break;
            case "welcome":fragment=new com.example.tmdt.ui.onboarding.WelcomeFragment();break;
            case "student_intro":fragment=new com.example.tmdt.ui.onboarding.StudentIntroFragment();break;
            case "login":fragment=new LoginFragment();break;
            case "register":fragment=new RegisterFragment();break;
            case "email_verification":fragment=new com.example.tmdt.ui.auth.EmailVerificationFragment();break;
            case "forgot":fragment=new ForgotPasswordFragment();break;
            case "account":fragment=new AccountFragment();break;
            case "edit_account":fragment=new EditAccountFragment();break;
            case "favorites":fragment=new com.example.tmdt.ui.profile.FavoritesFragment();break;
            case "settings":fragment=new com.example.tmdt.ui.profile.SettingsFragment();break;
            case "register_tutor":fragment=new com.example.tmdt.ui.profile.RegisterTutorFragment();break;
            case "review_tutor":fragment=new com.example.tmdt.ui.profile.ReviewTutorFragment();break;
            case "otp":fragment=new PhoneVerificationFragment();break;
            case "schedule":fragment=new ScheduleFragment();break;
            case "messages":fragment=new MessagesFragment();break;
            case "post_request":fragment=new com.example.tmdt.ui.requests.PostTutorRequestFragment();break;
            case "my_requests":fragment=new com.example.tmdt.ui.requests.ManageTutorRequestsFragment();break;
            case "edit_request":fragment=com.example.tmdt.ui.requests.EditTutorRequestFragment.newInstance(selectedRequestId);break;
            case "request_applicants":fragment=com.example.tmdt.ui.requests.RequestApplicantsFragment.newInstance(selectedRequestId);break;
            case "chat":fragment=new ChatFragment();break;
            case "tutor_profile":fragment=com.example.tmdt.ui.home.TutorProfileFragment.newInstance(classroom.tutorProfileId);break;
            default:destination="home";fragment=new HomeFragment();
        }
        screen=destination;
        Fragment current=getSupportFragmentManager().findFragmentById(R.id.screen_container);
        if(navigatingTo==null&&current instanceof ScreenFragment&&screen.equals(current.getTag())){((ScreenFragment)current).refresh();renderNavigation();return;}
        if(navigatingTo!=null||current==null||!screen.equals(current.getTag())){
            androidx.fragment.app.FragmentTransaction transaction=getSupportFragmentManager().beginTransaction().setReorderingAllowed(true);
            if(current!=null && com.example.tmdt.ui.common.Motion.enabled()
                    && (classroom.tutorMode() || current.getClass().getPackage().getName().endsWith(".tutor")
                    || java.util.Arrays.asList("tutor_evidence","review_tutor","tutor_pending","tutor_supplement","tutor_submitted").contains(screen)))
                transaction.setCustomAnimations(R.anim.teacher_enter,R.anim.teacher_exit);
            final String target=screen;navigatingTo=target;
            transaction.replace(R.id.screen_container,fragment,screen).runOnCommit(()->{
                if(target.equals(navigatingTo))navigatingTo=null;
            }).commit();
        }
        renderNavigation();
    }
    private boolean navigateMenu(int itemId){
        if(updatingNavigation)return true;
        if(account.isBusy()||account.isSendingCode()||(isRequestScreen(screen)&&requestManager().busy()))return false;
        String destination;
        if(itemId==R.id.nav_schedule)destination="schedule";
        else if(itemId==R.id.nav_messages)destination="messages";
        else if(itemId==R.id.nav_account)destination="account";
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
        android.view.Menu menu=binding.bottomNavigation.getMenu();
        for(int i=0;i<menu.size();i++)menu.getItem(i).setEnabled(!account.isBusy()&&!account.isSendingCode());
        int selected=R.id.nav_home;
        if("schedule".equals(screen))selected=R.id.nav_schedule;
        else if("messages".equals(screen)||"chat".equals(screen))selected=R.id.nav_messages;
        else if(!"home".equals(screen))selected=R.id.nav_account;
        if(isRequestScreen(screen))selected="account".equals(requestManagementOrigin)?R.id.nav_account:R.id.nav_home;
        selected=classroom.navigationItem(menu,screen,selected);
        menu.findItem(selected).setChecked(true);
        updateNavigationVisibility();
        updatingNavigation=false;
    }
    private void updateNavigationVisibility(){
        boolean mainScreen="home".equals(screen)||"account".equals(screen)||"schedule".equals(screen)||"messages".equals(screen)||classroom.mainScreen(screen);
        int visibility=mainScreen&&!keyboardVisible?View.VISIBLE:View.GONE;
        // Let the page extend underneath both the floating menu and Android's gesture area.
        int paddingBottom=visibility==View.VISIBLE?0:bottomInset;
        if(binding.appRoot.getPaddingBottom()!=paddingBottom)
            binding.appRoot.setPadding(binding.appRoot.getPaddingLeft(),binding.appRoot.getPaddingTop(),binding.appRoot.getPaddingRight(),paddingBottom);
        android.widget.FrameLayout.LayoutParams glassParams=(android.widget.FrameLayout.LayoutParams)binding.navigationGlass.getLayoutParams();
        int margin=Math.round(8*getResources().getDisplayMetrics().density)+systemBottomInset;
        if(glassParams.bottomMargin!=margin){glassParams.bottomMargin=margin;binding.navigationGlass.setLayoutParams(glassParams);}
        binding.bottomNavigation.setVisibility(visibility);
        binding.navigationGlass.setVisibility(visibility);
        binding.navigationGlass.setBlurAutoUpdate(visibility==View.VISIBLE);
        updateNavigationPadding();
    }
    private void updateNavigationPadding() {
        if(binding==null)return;
        int inset=binding.navigationGlass.getVisibility()==View.VISIBLE
                ?Math.round(96*getResources().getDisplayMetrics().density)+systemBottomInset:0;
        for(int i=0;i<binding.screenContainer.getChildCount();i++)
            navigationInsets.apply(binding.screenContainer.getChildAt(i),inset);
    }
    @Override protected void onDestroy() {
        binding.screenContainer.getViewTreeObserver().removeOnGlobalLayoutListener(navigationLayout);
        binding.navigationGlass.setBlurAutoUpdate(false);
        super.onDestroy();
    }
    public void back(){
        if(account.isBusy()||(isRequestScreen(screen)&&requestManager().busy()))return;hideKeyboard();
        if("login".equals(screen))authenticatedDestination=null;
        String target=classroom.backTarget(screen);
        if(target==null)target=exchange.backTarget(screen);
        if(target!=null){show(target);return;}
        switch(screen){
            case "welcome":finish();break;
            case "student_intro":show("welcome");break;
            case "register":
                account.removeDraft("register.password");account.removeDraft("register.confirm");account.clearFeedback();
                Onboarding intro=new Onboarding(this);
                show(intro.pending()?(intro.teacher()?"welcome":"student_intro"):"login");break;
            case "forgot":
                account.removeDraft("register.password");account.removeDraft("register.confirm");account.clearFeedback();show("login");break;
            case "edit_account":account.clearDraft();account.clearFeedback();show("account");break;
            case "otp":account.clearFeedback();show("account");break;
            case "email_verification":account.leaveEmailVerification();break;
            case "favorites":case "settings":account.clearFeedback();show("account");break;
            case "register_tutor":account.clearFeedback();show("account");break;
            case "review_tutor":account.clearFeedback();show("tutor_evidence");break;
            case "tutor_evidence":show("register_tutor");break;
            case "tutor_supplement":case "tutor_submitted":show("tutor_pending");break;
            case "tutor_pending":case "admin_tutors":show("account");break;
            case "post_request":show(postRequestOrigin);break;
            case "edit_request":requestManager().resetEdit();show("my_requests");break;
            case "request_applicants":show("my_requests");break;
            case "my_requests":show(requestManagementOrigin);break;
            case "tutor_profile":show(classroom.tutorProfileOrigin!=null?classroom.tutorProfileOrigin:"home");break;
            case "chat":{String origin=chat.origin();chat.close();show("chat".equals(origin)?"messages":origin);break;}
            default:account.removeDraft("login.password");show("home");
        }
    }
    // UC12: owner request management and navigation back to the entry screen.
    private static boolean isRequestScreen(String destination){
        return "post_request".equals(destination)||"my_requests".equals(destination)
                ||"edit_request".equals(destination)||"request_applicants".equals(destination);
    }
    private TutorRequestManager requestManager(){return new ViewModelProvider(this).get(TutorRequestManager.class);}
    public String selectedRequestId(){return selectedRequestId;}
    public void openMyRequests(String origin){
        if("account".equals(origin)||"home".equals(origin))requestManagementOrigin=origin;
        else if("post_request".equals(origin)&&!"my_requests".equals(postRequestOrigin))requestManagementOrigin="home";
        hideKeyboard();show("my_requests");
    }
    public void openRequestEditor(String id){
        selectedRequestId=id==null?"":id;hideKeyboard();show("edit_request");
    }
    public void openRequestApplicants(String id){
        selectedRequestId=id==null?"":id;hideKeyboard();show("request_applicants");
    }
    public void reloadRequestEditor(String id){
        if(requestManager().busy())return;
        requestManager().resetEdit();selectedRequestId=id==null?"":id;
        navigatingTo="reload_edit_request";hideKeyboard();show("edit_request");
    }
    // Tin nhắn (UC21, member 4) --------------------------------------------------------------------

    /**
     * Mở cuộc trò chuyện với một người dùng (tạo nếu chưa có). Khách được chuyển tới đăng nhập.
     * @param lesson buổi học liên quan (Conversation.lesson(...)), có thể null
     */
    public void openChat(String peerId,String peerName,String peerRole,Map<String,Object> lesson){
        if(!account.signedIn()){account.clearFeedback();account.notice=getString(R.string.chat_login_notice);show("login");return;}
        UserProfile me=account.profile();
        String myName=me!=null&&!me.name.isEmpty()?me.name:"Học viên";
        String myRole=me!=null&&me.tutor?"TUTOR":"STUDENT";
        hideKeyboard();
        if(chat.openWith(peerId,peerName,peerRole,myName,myRole,lesson,screen))show("chat");
        else classroom.dialog(getString(R.string.chat_unavailable_title,peerName),getString(R.string.chat_unavailable_body));
    }
    /** Mở danh sách tin nhắn, lọc sẵn theo tên (ví dụ từ lịch dạy của gia sư). */
    public void openMessages(String name){
        chat.unreadOnly(false);chat.query(name==null?"":name);show("messages");
    }
    /** Huy hiệu số tin chưa đọc trên mục Tin nhắn của thanh điều hướng. */
    private void renderMessageBadge(){
        int unread=account.signedIn()?chat.unreadTotal():0;
        if(unread<=0){binding.bottomNavigation.removeBadge(R.id.nav_messages);return;}
        BadgeDrawable badge=binding.bottomNavigation.getOrCreateBadge(R.id.nav_messages);
        badge.setBackgroundColor(getColor(R.color.chat_blue));badge.setBadgeTextColor(getColor(R.color.white));
        badge.setNumber(unread);badge.setVisible(true);
    }
    @Override protected void onStart(){super.onStart();if(chat!=null)chat.presence(true);}
    @Override protected void onStop(){if(chat!=null)chat.presence(false);super.onStop();}

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
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putString("screen",screen);out.putString("authenticated_destination",authenticatedDestination);out.putString("request_id",selectedRequestId);out.putString("request_origin",requestManagementOrigin);out.putString("post_request_origin",postRequestOrigin);classroom.save(out);exchange.save(out);}
}
