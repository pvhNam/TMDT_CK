package com.example.tmdt.ui.admin;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.EditText;
import com.example.tmdt.*;
import com.example.tmdt.ui.common.Ui;
import java.util.Locale;

public final class AdminContractsFragment extends AdminScreen {
    @Override protected View build(){
        boolean selected=!state().selectedTutor.isEmpty();
        LinearLayout root=root(selected?"Hợp đồng đã ký":"Hợp đồng giảng dạy"),body=ui.page(root);if(!feedback(body))return root;
        if(selected){
            PaymentState.Row tutor=state().tutors.get(state().selectedTutor);
            if(tutor==null)tutor=state().users.get(state().selectedTutor);
            LinearLayout identity=ui.row();identity.addView(ui.avatar(state().selectedTutor,tutor==null?"Gia sư":tutor.text("ho_ten"),44));ui.gap(identity,10);
            ui.weight(identity,ui.text(tutor==null?"Gia sư":tutor.text("ho_ten"),20,Ui.INK,true));ui.add(body,identity);ui.space(body,10);
            ui.add(body,ui.link("‹  Chọn gia sư khác",14,()->{scrollToTop();state().selectTutor("");refresh();}));ui.space(body,16);
            for(PaymentState.Row c:state().tutorContracts.values())contractCard(body,c);
            if(state().loadingTutorContracts)ui.add(body,ui.text("Đang tải hợp đồng…",14,Ui.BLUE,false));
            if(state().moreTutorContracts)action(body,"Tải thêm hợp đồng đã ký",Ui.OUTLINE,()->state().moreTutorContracts());
            return root;
        }
        ui.section(body,"Hợp đồng theo gia sư",22);ui.space(body,6);
        ui.space(body,10);
        LinearLayout results=ui.column();
        search(body,"Tìm tên, email hoặc số điện thoại gia sư",state().tutorQuery,value->{state().tutorQuery=value;renderTutors(results);});
        ui.add(body,results);renderTutors(results);return root;
    }
    private void renderTutors(LinearLayout body){
        body.removeAllViews();String query=state().tutorQuery.trim().toLowerCase(Locale.ROOT);int count=0;
        for(PaymentState.Row r:state().tutors.values()){
            if(!query.isEmpty()&&!(r.text("ho_ten")+" "+r.text("email")+" "+r.text("so_dien_thoai")).toLowerCase(Locale.ROOT).contains(query))continue;
            count++;LinearLayout card=ui.card(),row=ui.row(),text=ui.column();row.addView(ui.avatar(r.id,r.text("ho_ten"),48));ui.gap(row,12);
            ui.add(text,ui.text(r.text("ho_ten"),17,Ui.INK,true));ui.space(text,5);
            ui.add(text,ui.text("BANNED".equals(r.text("trang_thai"))?"Tài khoản đã khóa":"Gia sư",13,Ui.MUTED,false));ui.space(text,5);
            ui.add(text,ui.text("Xem hợp đồng đã ký  ›",13,Ui.BLUE,true));ui.weight(row,text);ui.add(card,row);
            card.setContentDescription("Hợp đồng của "+r.text("ho_ten"));ui.clickable(card,()->{host().hideKeyboard();scrollToTop();state().notice="";state().selectTutor(r.id);refresh();});ui.add(body,card);ui.space(body,12);
        }
        if(count==0)ui.add(body,ui.text(state().loadingTutors?"Đang tải gia sư…":"Chưa có gia sư phù hợp trong danh sách đã tải.",14,Ui.MUTED,false));
        if(state().moreTutors)action(body,"Tải thêm gia sư",Ui.OUTLINE,()->state().moreTutors());
        ui.space(body,8);ui.add(body,ui.text(count+" gia sư",12,Ui.MUTED,false));
    }
}
