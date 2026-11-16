package com.example.tmdt.ui.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.example.tmdt.AccountState;
import com.example.tmdt.R;
import com.example.tmdt.UserProfile;
import com.example.tmdt.ui.common.RenderSnapshot;
import com.example.tmdt.ui.common.StableViews;
import com.example.tmdt.ui.common.Ui;
import java.util.Arrays;

/** Admin profile actions live here; system management is reached through the other four tabs. */
public final class AdminAccountFragment extends AdminScreen {
    private LinearLayout body;
    private AccountState account(){return new ViewModelProvider(requireActivity()).get(AccountState.class);}

    @Override public void onViewCreated(@NonNull View view,@Nullable Bundle saved){
        super.onViewCreated(view,saved);
        account().changes().observe(getViewLifecycleOwner(),ignored->refreshData());
        host().chat.changes().observe(getViewLifecycleOwner(),ignored->refreshData());
    }
    @Override protected View build(){LinearLayout root=root("Cá nhân");body=ui.page(root);render();return root;}
    @Override protected boolean updateContent(){if(body==null)return false;render();return true;}
    @Override protected Object dataSnapshot(){
        AccountState account=account();return RenderSnapshot.of(account.profile(),account.error(),account.notice(),account.isBusy(),host().chat.unreadTotal());
    }

    private void render(){
        AccountState account=account();UserProfile profile=account.profile();
        StableViews rows=StableViews.at(body);rows.begin();
        if(!account.error().isEmpty())rows.add("error",account.error(),()->ui.note(R.drawable.ic_info,account.error(),0xFFFFF0F0,Ui.RED,Ui.RED),ui.dp(12));
        if(!account.notice().isEmpty())rows.add("notice",account.notice(),()->ui.note(R.drawable.ic_info,account.notice(),Ui.GREEN_BG,Ui.GREEN,Ui.INK),ui.dp(12));
        if(account.isBusy())rows.add("busy",true,()->ui.text("Đang cập nhật…",14,Ui.BLUE,false),ui.dp(12));
        if(profile==null){
            rows.add("loading",account.isBusy(),()->{
                LinearLayout box=ui.card();ui.add(box,ui.text("Đang tải thông tin cá nhân…",15,Ui.MUTED,false));
                if(!account.isBusy()){ui.space(box,12);ui.addAction(box,ui.action("Tải lại",0,Ui.OUTLINE,()->account.loadProfile(false)),48);}return box;
            },0);rows.commit();return;
        }
        rows.add("profile",Arrays.asList(profile.name,profile.email),()->{
            LinearLayout card=ui.card(),row=ui.row(),copy=ui.column();ui.pad(card,18,18);
            row.addView(ui.accountAvatar(76));ui.gap(row,16);
            TextView name=ui.text(profile.name,22,Ui.INK,true);name.setMaxLines(2);ui.add(copy,name);ui.space(copy,6);
            TextView email=ui.text(profile.email,13,Ui.MUTED,false);email.setMaxLines(2);ui.add(copy,email);ui.space(copy,9);
            copy.addView(ui.pill("Quản trị viên",12,Ui.BLUE,Ui.PALE));ui.weight(row,copy);ui.add(card,row);ui.space(card,18);
            ui.addAction(card,ui.action("Chỉnh sửa hồ sơ",R.drawable.ic_edit,Ui.OUTLINE,()->{account.clearDraft();go("edit_account");}),46);return card;
        },ui.dp(22));
        rows.add("contact",Arrays.asList(profile.phone,profile.address),()->{
            LinearLayout box=ui.column();ui.section(box,"Thông tin liên hệ",17);ui.space(box,10);
            LinearLayout card=ui.card();contact(card,R.drawable.ic_field_phone,"Số điện thoại",profile.phone);
            ui.space(card,14);ui.line(card);ui.space(card,14);contact(card,R.drawable.ic_field_address,"Địa chỉ",profile.address);
            ui.add(box,card);return box;
        },ui.dp(22));
        int unread=host().chat.unreadTotal();
        rows.add("application",unread,()->{
            LinearLayout box=ui.column(),card=menuGroup(box,"Tài khoản và ứng dụng");
            profileItem(card,R.drawable.ic_pf_chat,"Tin nhắn",unread>0?unread+" mới":"",()->go("messages"));
            profileItem(card,R.drawable.ic_settings_gear,"Cài đặt","",()->go("settings"));
            profileItem(card,R.drawable.ic_pf_helpdesk,"Trung tâm trợ giúp","",()->go("help_chat"));return box;
        },ui.dp(22));
        rows.add("sign-out",account.isBusy(),()->ui.action("Đăng xuất",R.drawable.ic_pf_logout,account.isBusy()?Ui.DISABLED:Ui.DANGER,()->
            ui.dialog().setTitle("Đăng xuất khỏi tài khoản?").setNegativeButton("Hủy",null)
                    .setPositiveButton("Đăng xuất",(dialog,which)->account.signOut()).show()),ui.dp(12));
        rows.commit();
    }
    private void contact(LinearLayout card,int icon,String label,String value){
        LinearLayout row=ui.row(),copy=ui.column();row.addView(ui.icon(icon,22,Ui.BLUE));ui.gap(row,12);
        ui.add(copy,ui.text(label,12,Ui.MUTED,false));ui.space(copy,5);
        TextView text=ui.text(value.isEmpty()?getString(R.string.not_updated):value,15,Ui.INK,false);text.setTextIsSelectable(true);
        ui.add(copy,text);ui.weight(row,copy);ui.add(card,row);
    }
    private void profileItem(LinearLayout card,int icon,String title,String badge,Runnable run){
        if(card.getChildCount()>0)ui.line(card);
        LinearLayout row=ui.row();ui.pad(row,16,12);row.setMinimumHeight(ui.dp(56));
        ImageView symbol=ui.icon(icon,24,Ui.INK);if(icon!=R.drawable.ic_settings_gear)symbol.setImageTintList(null);
        row.addView(symbol);ui.gap(row,18);ui.weight(row,ui.text(title,17,Ui.INK,false));
        if(!badge.isEmpty()){row.addView(ui.pill(badge,11,Ui.BLUE,Ui.PALE));ui.gap(row,8);}
        row.addView(ui.icon(R.drawable.ic_right,18,Ui.INK));row.setContentDescription(title);
        ui.clickable(row,run);ui.add(card,row);
    }
    private void go(String destination){account().clearFeedback();host().hideKeyboard();host().show(destination);}
    @Override public void onDestroyView(){super.onDestroyView();body=null;}
}
