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
    /** Hợp đồng, đánh giá, báo cáo vi phạm và quản trị (member 4). */
    public Exchange exchange;
    /** Lessons and group classes (member 3). */
    public Classroom classroom;
    String screen="home";
    private String pendingDestination;
    private ActivityMainBinding binding;
    private boolean updatingNavigation;
    private boolean keyboardVisible;

    @Override public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(),false);
        binding=ActivityMainBinding.inflate(getLayoutInflater());setContentView(binding.getRoot());
        account=new ViewModelProvider(this).get(AccountState.class);
        catalog=new ViewModelProvider(this).get(CatalogState.class);
        chat=new ViewModelProvider(this).get(ChatState.class);
        classroom=new Classroom(this,savedInstanceState);
        exchange=new Exchange(this,savedInstanceState);
        ViewCompat.setOnApplyWindowInsetsListener(binding.appRoot,(view,insets)->{
            Insets safe=insets.getInsets(WindowInsetsCompat.Type.systemBars()|WindowInsetsCompat.Type.displayCutout()|WindowInsetsCompat.Type.ime());
            view.setPadding(safe.left,safe.top,safe.right,safe.bottom);
            keyboardVisible=insets.isVisible(WindowInsetsCompat.Type.ime());
            updateNavigationVisibility();
            return WindowInsetsCompat.CONSUMED;
        });
        WindowCompat.getInsetsController(getWindow(),getWindow().getDecorView()).setAppearanceLightStatusBars(true);
        WindowCompat.getInsetsController(getWindow(),getWindow().getDecorView()).setAppearanceLightNavigationBars(true);
        NavigationAssets.apply(binding.bottomNavigation);
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
        show(screen);
        account.changes().observe(this,ignored->{
            if(account.destination!=null){String destination=account.destination;account.destination=null;show(destination);}
            // Tin nhắn theo tài khoản đang đăng nhập; đăng xuất thì bỏ dữ liệu cũ.
            if(account.signedIn())chat.start();else chat.reset();
            exchange.accountChanged(account.signedIn());
            renderNavigation();
        });
        chat.changes().observe(this,ignored->renderMessageBadge());
    }
    public void show(String destination){
        destination=classroom.redirect(destination);
        destination=exchange.redirect(destination);
        if("chat".equals(destination)&&(!account.signedIn()||chat.openId()==null))destination=account.signedIn()?"messages":"login";
        if(!account.signedIn()&&(destination.equals("account")||destination.equals("edit_account")||destination.equals("otp")))destination="login";
        if(account.signedIn()&&(destination.equals("login")||destination.equals("register")))destination="account";
        if(getSupportFragmentManager().isStateSaved()){pendingDestination=destination;return;}
        Fragment fragment=classroom.fragment(destination);
        if(fragment==null)fragment=exchange.fragment(destination);
        if(fragment==null)switch(destination){
            case "welcome":fragment=new com.example.tmdt.ui.onboarding.WelcomeFragment();break;
            case "student_intro":fragment=new com.example.tmdt.ui.onboarding.StudentIntroFragment();break;
            case "login":fragment=new LoginFragment();break;
            case "register":fragment=new RegisterFragment();break;
            case "forgot":fragment=new ForgotPasswordFragment();break;
            case "account":fragment=new AccountFragment();break;
            case "edit_account":fragment=new EditAccountFragment();break;
            case "otp":fragment=new PhoneVerificationFragment();break;
            case "schedule":fragment=new ScheduleFragment();break;
            case "messages":fragment=new MessagesFragment();break;
            case "chat":fragment=new ChatFragment();break;
            default:destination="home";fragment=new HomeFragment();
        }
        screen=destination;
        Fragment current=getSupportFragmentManager().findFragmentById(R.id.screen_container);
        if(current instanceof ScreenFragment&&screen.equals(current.getTag())){((ScreenFragment)current).refresh();renderNavigation();return;}
        if(current==null||!screen.equals(current.getTag()))getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true).replace(R.id.screen_container,fragment,screen).commit();
        renderNavigation();
    }
    private boolean navigateMenu(int itemId){
        if(updatingNavigation)return true;
        if(account.isBusy()||account.isSendingCode())return false;
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
        selected=classroom.navigationItem(menu,screen,selected);
        menu.findItem(selected).setChecked(true);
        updateNavigationVisibility();
        updatingNavigation=false;
    }
    private void updateNavigationVisibility(){
        boolean mainScreen="home".equals(screen)||"account".equals(screen)||"schedule".equals(screen)||"messages".equals(screen)||classroom.mainScreen(screen);
        int visibility=mainScreen&&!keyboardVisible?View.VISIBLE:View.GONE;
        binding.bottomNavigation.setVisibility(visibility);

    }
    public void back(){
        if(account.isBusy())return;hideKeyboard();
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
            case "chat":{String origin=chat.origin();chat.close();show("chat".equals(origin)?"messages":origin);break;}
            default:account.removeDraft("login.password");show("home");
        }
    }
    // Tin nhắn (UC21) --------------------------------------------------------------------------------

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
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putString("screen",screen);classroom.save(out);exchange.save(out);}
}
