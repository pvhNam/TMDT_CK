package com.example.tmdt.ui.common;

import android.content.Context;
import android.content.ContextWrapper;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import com.example.tmdt.AccountState;
import com.example.tmdt.UserProfile;
import com.example.tmdt.databinding.ViewAvatarBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

/** Shared avatar, following saved account changes and releasing listeners when its view disappears. */
public final class AvatarView extends FrameLayout {
    private final ViewAvatarBinding binding;
    private String uid="", name="", viewer=null, encoded="";
    private boolean currentAccount;
    private AccountState account;
    private ListenerRegistration listener;
    private int request;
    private final Observer<Integer> accountObserver=ignored->refresh();

    public AvatarView(Context context){this(context,null);}
    public AvatarView(Context context,AttributeSet attrs){
        super(context,attrs);
        binding=ViewAvatarBinding.inflate(LayoutInflater.from(context),this,false);
        addView(binding.getRoot(),new FrameLayout.LayoutParams(-1,-1));
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        binding.getRoot().setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        draw();
    }
    public void bind(String userId,String displayName){
        if(!currentAccount && uid.equals(userId==null?"":userId) && name.equals(displayName==null?"":displayName))return;
        stop();currentAccount=false;uid=userId==null?"":userId;name=displayName==null?"":displayName;
        encoded="";viewer=null;draw();if(isAttachedToWindow())refresh();
    }
    public void bindCurrentAccount(){stop();currentAccount=true;viewer=null;if(isAttachedToWindow())refresh();}
    @Override protected void onAttachedToWindow(){
        super.onAttachedToWindow();
        Context context=getContext();
        while(context instanceof ContextWrapper && !(context instanceof FragmentActivity))context=((ContextWrapper)context).getBaseContext();
        if(context instanceof FragmentActivity){
            account=new ViewModelProvider((FragmentActivity)context).get(AccountState.class);
            account.changes().observeForever(accountObserver);
        }
        refresh();
    }
    private void refresh(){
        FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();
        String authenticated=user==null?"":user.getUid();
        if(currentAccount || (!uid.isEmpty() && uid.equals(authenticated))){
            stop();viewer=null;UserProfile profile=authenticated.isEmpty()||account==null?null:account.profile();
            encoded=profile==null?"":profile.avatar;
            if(currentAccount)name=profile==null?"":profile.name;
            draw();return;
        }
        if(authenticated.equals(viewer))return;
        stop();viewer=authenticated;encoded="";draw();
        if(uid.isEmpty())return;
        final int version=request;
        listener=FirebaseFirestore.getInstance().collection("profile_avatars").document(uid)
                .addSnapshotListener((snapshot,error)->{
                    if(version!=request || !isAttachedToWindow())return;
                    encoded=error==null && snapshot!=null && snapshot.get("anh_dai_dien") instanceof String
                            ?snapshot.getString("anh_dai_dien"):"";
                    draw();
                });
    }
    private void draw(){
        AvatarRenderer.render(binding,encoded,name);
        setContentDescription(name.isEmpty()?"Thông tin cá nhân":"Ảnh đại diện của "+name);
    }
    private void stop(){request++;if(listener!=null){listener.remove();listener=null;}}
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){
        super.onSizeChanged(w,h,oldw,oldh);
        binding.avatarInitials.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,Math.min(w,h)*0.38f);
    }
    @Override protected void onDetachedFromWindow(){
        stop();viewer=null;
        if(account!=null){account.changes().removeObserver(accountObserver);account=null;}
        super.onDetachedFromWindow();
    }
}
