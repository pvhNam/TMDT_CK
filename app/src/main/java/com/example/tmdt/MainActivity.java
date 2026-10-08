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
import com.example.tmdt.ui.messages.MessagesFragment;
import com.example.tmdt.ui.common.ScreenFragment;

/** Hosts independent fragments. Layouts, form handling and Firebase access live outside this activity. */
public class MainActivity extends AppCompatActivity {
    AccountState account;
    CatalogState catalog;
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
        classroom=new Classroom(this,savedInstanceState);
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
        binding.bottomNavigation.setClipToOutline(true);
        binding.bottomNavigation.setItemActiveIndicatorEnabled(false);
        binding.bottomNavigation.setOnItemSelectedListener(item->navigateMenu(item.getItemId()));
        binding.bottomNavigation.setOnItemReselectedListener(item->navigateMenu(item.getItemId()));
        getOnBackPressedDispatcher().addCallback(this,new OnBackPressedCallback(true){
            @Override public void handleOnBackPressed(){if(classroom.root().equals(screen))finish();else back();}
        });
        screen=savedInstanceState==null?"home":savedInstanceState.getString("screen","home");
        show(screen);
        account.changes().observe(this,ignored->{
            classroom.onAccount();
            if(account.destination!=null){String destination=account.destination;account.destination=null;show(destination);}
            renderNavigation();
        });
    }
    public void show(String destination){
        destination=classroom.redirect(destination);
        if(!account.signedIn()&&(destination.equals("account")||destination.equals("edit_account")||destination.equals("otp")||destination.equals("register_tutor")||destination.equals("review_tutor")))destination="login";
        if((destination.equals("register_tutor")||destination.equals("review_tutor"))&&account.profile()!=null&&account.profile().tutor)destination="account";
        if(destination.equals("review_tutor")&&account.tutorRegistration()==null)destination="register_tutor";
        if(account.signedIn()&&(destination.equals("login")||destination.equals("register")))destination="account";
        if(getSupportFragmentManager().isStateSaved()){pendingDestination=destination;return;}
        Fragment fragment=classroom.fragment(destination);
        if(fragment==null)switch(destination){
            case "login":fragment=new LoginFragment();break;
            case "register":fragment=new RegisterFragment();break;
            case "forgot":fragment=new ForgotPasswordFragment();break;
            case "account":fragment=new AccountFragment();break;
            case "edit_account":fragment=new EditAccountFragment();break;
            case "register_tutor":fragment=new com.example.tmdt.ui.profile.RegisterTutorFragment();break;
            case "review_tutor":fragment=new com.example.tmdt.ui.profile.ReviewTutorFragment();break;
            case "otp":fragment=new PhoneVerificationFragment();break;
            case "schedule":fragment=new ScheduleFragment();break;
            case "messages":fragment=new MessagesFragment();break;
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
        else if("messages".equals(screen))selected=R.id.nav_messages;
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
        binding.navigationDivider.setVisibility(visibility);
    }
    public void back(){
        if(account.isBusy())return;hideKeyboard();
        String target=classroom.backTarget(screen);
        if(target!=null){show(target);return;}
        switch(screen){
            case "register":case "forgot":
                account.removeDraft("register.password");account.removeDraft("register.confirm");account.clearFeedback();show("login");break;
            case "edit_account":account.clearDraft();account.clearFeedback();show("account");break;
            case "otp":account.clearFeedback();show("account");break;
            case "register_tutor":account.clearFeedback();show("account");break;
            case "review_tutor":account.clearFeedback();show("register_tutor");break;
            default:account.removeDraft("login.password");show("home");
        }
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
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putString("screen",screen);classroom.save(out);}
}
