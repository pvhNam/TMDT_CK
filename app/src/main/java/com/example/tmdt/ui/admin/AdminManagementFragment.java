package com.example.tmdt.ui.admin;

import android.view.View;
import android.widget.LinearLayout;
import com.example.tmdt.R;
import com.example.tmdt.ui.common.StableViews;
import com.example.tmdt.ui.common.Ui;
import java.util.Arrays;

public final class AdminManagementFragment extends AdminScreen {
    private LinearLayout body;
    @Override protected View build(){LinearLayout root=root("Quản lý");body=ui.page(root);render();return root;}
    @Override protected boolean updateContent(){if(body==null)return false;render();return true;}
    private void render(){
        StableViews rows=StableViews.at(body);rows.begin();if(!feedback(rows)){rows.commit();return;}
        rows.add("people",Arrays.asList(state().totalStudents,state().totalTutors),()->{
            LinearLayout stats=ui.row();ui.weight(stats,statistic("Học sinh",String.valueOf(state().totalStudents),()->users("STUDENT")));ui.gap(stats,10);
            ui.weight(stats,statistic("Gia sư",String.valueOf(state().totalTutors),()->host().exchange.openTutorManage()));return stats;
        },ui.dp(22));
        rows.add("accounts",true,()->{
            LinearLayout box=ui.column(),card=menuGroup(box,"Người dùng");
            menuItem(card,R.drawable.ic_cap,"Quản lý gia sư","",()->host().exchange.openTutorManage());
            menuItem(card,R.drawable.ic_pf_user,"Quản lý học sinh","",()->users("STUDENT"));
            menuItem(card,R.drawable.ic_users,"Tất cả tài khoản","",()->users("ALL"));
            menuItem(card,R.drawable.ic_field_lock,"Tài khoản bị khóa","",()->users("BANNED"));return box;
        },ui.dp(22));
        rows.add("classes",true,()->{
            LinearLayout box=ui.column(),card=menuGroup(box,"Lớp học");
            menuItem(card,R.drawable.ic_ex_book,"Lớp học và đăng ký","",()->host().exchange.openClassesAdmin());return box;
        },ui.dp(22));
        rows.add("moderation",true,()->{
            LinearLayout box=ui.column(),card=menuGroup(box,"Kiểm duyệt và phân quyền");
            menuItem(card,R.drawable.ic_pf_shield,"Duyệt hồ sơ gia sư","",()->open("admin_tutors"));
            menuItem(card,R.drawable.ic_ex_alert,"Báo cáo vi phạm","",()->host().exchange.openReportsAdmin());
            menuItem(card,R.drawable.ic_field_shield,"Phân quyền quản trị","",()->host().exchange.openRoles());return box;
        },0);rows.commit();
    }
    private void users(String filter){state().userFilter=filter;state().query="";open("admin_users");}
    @Override public void onDestroyView(){super.onDestroyView();body=null;}
}
