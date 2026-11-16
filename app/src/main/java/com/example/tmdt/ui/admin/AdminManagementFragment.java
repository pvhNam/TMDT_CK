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
            ui.weight(stats,statistic("Hồ sơ gia sư",String.valueOf(state().totalTutors),()->users("TUTOR")));return stats;
        },ui.dp(20));
        rows.add("accounts",true,()->{
            LinearLayout box=ui.column();ui.section(box,"Tài khoản",17);ui.space(box,12);
            destination(box,R.drawable.ic_users,"Tất cả tài khoản","",()->users("ALL"));
            destination(box,R.drawable.ic_pf_user,"Học sinh","",()->users("STUDENT"));
            destination(box,R.drawable.ic_pf_shield,"Gia sư","",()->users("TUTOR"));
            destination(box,R.drawable.ic_field_shield,"Tài khoản bị khóa","",()->users("BANNED"));return box;
        },ui.dp(10));
        rows.add("moderation",true,()->{
            LinearLayout box=ui.column();ui.section(box,"Hồ sơ và báo cáo",17);ui.space(box,12);
            destination(box,R.drawable.ic_pf_shield,"Duyệt hồ sơ gia sư","",()->open("admin_tutors"));
            destination(box,R.drawable.ic_info,"Báo cáo vi phạm","",()->host().exchange.openReportsAdmin());return box;
        },0);rows.commit();
    }
    private void users(String filter){state().userFilter=filter;state().query="";open("admin_users");}
    @Override public void onDestroyView(){super.onDestroyView();body=null;}
}
