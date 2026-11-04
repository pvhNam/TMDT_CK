package com.example.tmdt.ui.admin;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.EditText;
import java.util.*;
import com.example.tmdt.*;
import com.example.tmdt.ui.common.Ui;

public final class AdminUsersFragment extends AdminScreen {
    @Override protected View build(){
        LinearLayout root=root("Quản lý tài khoản"),body=ui.page(root);if(!feedback(body))return root;
        LinearLayout results=ui.column();
        search(body,"Tìm tên, email hoặc số điện thoại",state().query,value->{state().query=value;renderUsers(results);});
        String[] filters={"ALL","STUDENT","TUTOR","BANNED"};
        ui.tabs(body,new String[]{"Tất cả","Học sinh","Gia sư","Bị khóa"},Math.max(0,Arrays.asList(filters).indexOf(state().userFilter)),i->{state().userFilter=filters[i];refresh();});ui.space(body,14);
        ui.add(body,ui.link("Duyệt hồ sơ gia sư  ›",14,()->open("admin_tutors")));ui.space(body,14);
        ui.add(body,results);renderUsers(results);return root;
    }
    private void renderUsers(LinearLayout body){
        body.removeAllViews();String query=state().query.trim().toLowerCase(Locale.ROOT);int count=0;
        for(PaymentState.Row r:state().users.values()){
            boolean tutor=r.flag("la_gia_su"),admin="ADMIN".equals(r.text("vai_tro"));
            if("TUTOR".equals(state().userFilter)&&!tutor)continue;
            if("STUDENT".equals(state().userFilter)&&(tutor||admin))continue;
            if("BANNED".equals(state().userFilter)&&!"BANNED".equals(r.text("trang_thai")))continue;
            if(!query.isEmpty()&&!(r.text("ho_ten")+" "+r.text("email")+" "+r.text("so_dien_thoai")).toLowerCase(Locale.ROOT).contains(query))continue;
            count++;LinearLayout card=ui.card(),row=ui.row();row.addView(ui.avatar(r.id,r.text("ho_ten"),46));ui.gap(row,12);
            LinearLayout text=ui.column();ui.add(text,ui.text(r.text("ho_ten"),17,Ui.INK,true));ui.space(text,5);
            ui.add(text,ui.text(AdminState.role(r)+" · "+("BANNED".equals(r.text("trang_thai"))?"Đã khóa":"Đang hoạt động"),13,"BANNED".equals(r.text("trang_thai"))?Ui.RED:Ui.GREEN,false));
            ui.space(text,4);ui.add(text,ui.text(r.text("email"),13,Ui.MUTED,false));ui.weight(row,text);ui.gap(row,8);row.addView(ui.text("›",24,Ui.BLUE,false));ui.add(card,row);
            card.setContentDescription("Xem tài khoản "+r.text("ho_ten"));
            ui.clickable(card,()->user(r.id));ui.add(body,card);ui.space(body,12);
        }
        if(count==0){ui.add(body,ui.text(state().loadingUsers?"Đang tải tài khoản…":"Không có tài khoản khớp với bộ lọc.",14,Ui.MUTED,false));ui.space(body,12);
            if(!state().loadingUsers)action(body,"Xóa bộ lọc",Ui.OUTLINE,()->{state().userFilter="ALL";state().query="";refresh();});}
        if(state().moreUsers)action(body,"Tải thêm tài khoản",Ui.OUTLINE,()->state().moreUsers());
        ui.add(body,ui.text(count+" kết quả · "+state().users.size()+" / "+state().totalUsers+" tài khoản",12,Ui.MUTED,false));
    }
}
