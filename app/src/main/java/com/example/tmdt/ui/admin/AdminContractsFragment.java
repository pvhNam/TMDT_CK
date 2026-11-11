package com.example.tmdt.ui.admin;

import android.view.View;
import android.widget.LinearLayout;
import com.example.tmdt.*;
import com.example.tmdt.ui.common.Ui;
import com.example.tmdt.ui.common.StableViews;
import com.example.tmdt.ui.common.RenderSnapshot;
import java.util.Arrays;

public final class AdminContractsFragment extends AdminScreen {
    private LinearLayout body;
    @Override protected View build(){LinearLayout root=root("Hợp đồng");body=ui.page(root);render();return root;}
    @Override protected boolean updateContent(){if(body==null)return false;render();return true;}
    @Override protected Object dataSnapshot(){return RenderSnapshot.of(state().active,state().busy,state().error,state().notice,state().selectedTutor,state().tutors,state().users,state().tutorContracts,state().tutorQuery,state().loadingTutors,state().loadingTutorContracts,state().moreTutors,state().moreTutorContracts);}
    private void render(){
        StableViews rows=StableViews.at(body);rows.begin();if(!feedback(rows)){rows.commit();return;}
        boolean selected=!state().selectedTutor.isEmpty();
        if(selected){
            final PaymentState.Row tutor=person(state().selectedTutor);
            rows.add("identity",Arrays.asList(state().selectedTutor,tutor),()->{
            LinearLayout identity=ui.row();identity.addView(ui.avatar(state().selectedTutor,tutor==null?"Gia sư":tutor.text("ho_ten"),44));ui.gap(identity,10);
            ui.weight(identity,ui.text(tutor==null?"Gia sư":tutor.text("ho_ten"),19,Ui.INK,true));return identity;
            },ui.dp(10));
            rows.add("choose",true,()->ui.link("‹  Chọn gia sư khác",14,()->{scrollToTop();state().selectTutor("");refreshData();}),ui.dp(16));
            for(PaymentState.Row c:state().tutorContracts.values())rows.add("contract/"+c.id,c,()->{LinearLayout box=ui.column();contractCard(box,c);return box;},0);
            if(state().loadingTutorContracts)rows.add("loading",true,()->ui.text("Đang tải hợp đồng…",14,Ui.BLUE,false),ui.dp(12));
            else if(state().tutorContracts.isEmpty())rows.add("empty",state().selectedTutor,()->ui.text("Gia sư chưa có hợp đồng giảng dạy đã ký.",14,Ui.MUTED,false),ui.dp(12));
            if(state().moreTutorContracts)rows.add("more-contracts",state().loadingTutorContracts,()->{LinearLayout box=ui.column();action(box,"Tải thêm hợp đồng đã ký",Ui.OUTLINE,()->state().moreTutorContracts());return box;},0);
            rows.commit();return;
        }
        rows.add("templates",true,()->{LinearLayout box=ui.column();destination(box,R.drawable.ic_file,"Mẫu hợp đồng","",()->host().exchange.openTemplates());return box;},ui.dp(8));
        rows.add("heading",true,()->ui.text("Hợp đồng theo gia sư",18,Ui.INK,true),ui.dp(12));
        rows.add("search",true,()->{LinearLayout box=ui.column();search(box,"Tìm gia sư theo tên, email, điện thoại",state().tutorQuery,value->{state().tutorQuery=value;refreshData();});return box;},0);
        String query=CatalogState.normalize(state().tutorQuery);int count=0;
        for(PaymentState.Row r:state().tutors.values()){
            if(!CatalogState.normalize(r.text("ho_ten")+" "+r.text("email")+" "+r.text("so_dien_thoai")).contains(query))continue;
            count++;rows.add("tutor/"+r.id,r,()->tutorCard(r),ui.dp(12));
        }
        if(count==0)rows.add("empty",Arrays.asList(state().loadingTutors,query),()->ui.text(state().loadingTutors?"Đang tải gia sư…":"Chưa có gia sư phù hợp trong danh sách đã tải.",14,Ui.MUTED,false),ui.dp(12));
        if(state().moreTutors)rows.add("more-tutors",state().loadingTutors,()->{LinearLayout box=ui.column();action(box,state().loadingTutors?"Đang tải…":"Tải thêm gia sư",state().loadingTutors?Ui.DISABLED:Ui.OUTLINE,()->state().moreTutors());return box;},0);
        final int found=count;rows.add("count",count,()->ui.text(found+" gia sư",12,Ui.MUTED,false),0);rows.commit();
    }
    private View tutorCard(PaymentState.Row r){
            LinearLayout card=ui.card(),row=ui.row(),text=ui.column();row.addView(ui.avatar(r.id,r.text("ho_ten"),44));ui.gap(row,12);
            ui.add(text,ui.text(r.text("ho_ten"),17,Ui.INK,true));ui.space(text,5);
            ui.add(text,ui.text("BANNED".equals(r.text("trang_thai"))?"Tài khoản đã khóa":"Gia sư",13,Ui.MUTED,false));ui.space(text,5);
            ui.add(text,ui.text("Xem hợp đồng đã ký  ›",13,Ui.BLUE,true));ui.weight(row,text);ui.add(card,row);
            card.setContentDescription("Hợp đồng của "+r.text("ho_ten"));ui.clickable(card,()->{host().hideKeyboard();scrollToTop();state().notice="";state().selectTutor(r.id);refreshData();});return card;
    }
    @Override public void onDestroyView(){super.onDestroyView();body=null;}
}
