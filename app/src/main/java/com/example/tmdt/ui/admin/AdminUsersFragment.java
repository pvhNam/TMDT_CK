package com.example.tmdt.ui.admin;

import android.view.View;
import android.widget.LinearLayout;
import java.util.*;
import com.example.tmdt.*;
import com.example.tmdt.ui.common.Ui;
import com.example.tmdt.ui.common.StableViews;
import com.example.tmdt.ui.common.RenderSnapshot;

public final class AdminUsersFragment extends AdminScreen {
    private LinearLayout body;
    @Override protected View build(){LinearLayout root=root("Tài khoản");body=ui.page(root);render();return root;}
    @Override protected boolean updateContent(){if(body==null)return false;render();return true;}
    @Override protected Object dataSnapshot(){return RenderSnapshot.of(state().active,state().busy,state().error,state().notice,state().users,state().query,state().userFilter,state().loadingUsers,state().moreUsers,state().totalUsers);}
    private void render(){
        StableViews rows=StableViews.at(body);rows.begin();if(!feedback(rows)){rows.commit();return;}
        rows.add("search",true,()->{LinearLayout box=ui.column();search(box,"Tìm tên, email hoặc số điện thoại",state().query,value->{state().query=value;refreshData();});return box;},0);
        rows.add("filters",state().userFilter,()->{
            LinearLayout box=ui.column();String[] filters={"ALL","STUDENT","TUTOR","BANNED"};
            ui.tabs(box,new String[]{"Tất cả","Học sinh","Gia sư","Bị khóa"},Math.max(0,Arrays.asList(filters).indexOf(state().userFilter)),i->{state().userFilter=filters[i];refreshData();});return box;
        },ui.dp(14));
        String query=CatalogState.normalize(state().query);int count=0;
        for(PaymentState.Row r:state().users.values()){
            boolean tutor=r.flag("la_gia_su"),admin="ADMIN".equals(r.text("vai_tro"));
            if("TUTOR".equals(state().userFilter)&&!tutor)continue;
            if("STUDENT".equals(state().userFilter)&&(tutor||admin))continue;
            if("BANNED".equals(state().userFilter)&&!"BANNED".equals(r.text("trang_thai")))continue;
            if(!CatalogState.normalize(r.text("ho_ten")+" "+r.text("email")+" "+r.text("so_dien_thoai")).contains(query))continue;
            count++;rows.add("user/"+r.id,r,()->userCard(r),ui.dp(12));
        }
        if(count==0)rows.add("empty",Arrays.asList(state().loadingUsers,state().userFilter,query),()->{
            LinearLayout box=ui.column();ui.add(box,ui.text(state().loadingUsers?"Đang tải tài khoản…":"Không có tài khoản khớp trong danh sách đã tải.",14,Ui.MUTED,false));ui.space(box,12);
            if(!state().loadingUsers)action(box,"Xóa bộ lọc",Ui.OUTLINE,()->{state().userFilter="ALL";state().query="";clearSearch();refreshData();});return box;
        },ui.dp(12));
        if(state().moreUsers)rows.add("more",state().loadingUsers,()->{LinearLayout box=ui.column();action(box,state().loadingUsers?"Đang tải…":"Tải thêm tài khoản",state().loadingUsers?Ui.DISABLED:Ui.OUTLINE,()->state().moreUsers());return box;},0);
        final int found=count;rows.add("count",Arrays.asList(count,state().users.size(),state().totalUsers),()->ui.text(found+" kết quả · "+state().users.size()+" / "+state().totalUsers+" tài khoản",12,Ui.MUTED,false),0);rows.commit();
    }
    private View userCard(PaymentState.Row r){
            LinearLayout card=ui.card(),row=ui.row();row.addView(ui.avatar(r.id,r.text("ho_ten"),44));ui.gap(row,12);
            LinearLayout text=ui.column();ui.add(text,ui.text(r.text("ho_ten"),17,Ui.INK,true));ui.space(text,5);
            ui.add(text,ui.text(AdminState.role(r)+" · "+("BANNED".equals(r.text("trang_thai"))?"Đã khóa":"Đang hoạt động"),13,"BANNED".equals(r.text("trang_thai"))?Ui.RED:Ui.GREEN,false));
            ui.space(text,4);ui.add(text,ui.text(r.text("email"),13,Ui.MUTED,false));ui.weight(row,text);ui.gap(row,8);row.addView(ui.text("›",24,Ui.BLUE,false));ui.add(card,row);
            card.setContentDescription("Xem tài khoản "+r.text("ho_ten"));
            ui.clickable(card,()->user(r.id));return card;
    }
    @Override public void onDestroyView(){super.onDestroyView();body=null;}
}
