package com.example.tmdt.ui.admin;

import android.view.View;
import android.widget.LinearLayout;
import com.example.tmdt.*;
import com.example.tmdt.ui.common.Ui;
import com.example.tmdt.ui.common.StableViews;
import java.util.Arrays;

public final class AdminContractDetailFragment extends AdminScreen {
    private LinearLayout body;
    @Override protected View build(){LinearLayout root=root("Chi tiết hợp đồng");body=ui.page(root);render();return root;}
    @Override protected boolean updateContent(){if(body==null)return false;render();return true;}
    private void render(){
        StableViews rows=StableViews.at(body);rows.begin();if(!feedback(rows)){rows.commit();return;}
        PaymentState.Row c=state().contract(state().selectedContract);if(c==null){
            boolean loading=state().loadingContract||state().loadingTutorContracts||state().loadingContracts;
            if(state().error.isEmpty())rows.add("empty",loading,()->ui.text(loading?"Đang tải hợp đồng…":"Hợp đồng không còn trong hệ thống.",15,Ui.MUTED,false),0);rows.commit();return;
        }
        rows.add("summary",Arrays.asList(c.text("code"),c.text("subject"),c.text("status")),()->{LinearLayout box=ui.column();card(box,c.text("code").isEmpty()?c.id:c.text("code"),c.text("subject"),AdminState.contractStatus(c),null);return box;},0);
        rows.add("details",c,()->{
        LinearLayout table=ui.card();detailField(table,"Gia sư",c.text("tutor_name")+" · "+(c.flag("tutor_confirmed")?"Đã ký":"Chưa ký"));
        detailField(table,"Học sinh",c.text("student_name")+" · "+(c.flag("student_confirmed")?"Đã ký":"Chưa ký"));
        detailField(table,"Ngày bắt đầu",c.text("start_date"));detailField(table,"Lịch học",c.text("schedule"));
        detailField(table,"Số buổi",c.number("sessions")+" buổi × "+c.number("minutes")+" phút");
        detailField(table,"Học phí",PaymentState.money(c.number("fee"))+" / buổi");return table;
        },ui.dp(18));
        if(!c.text("base_terms").isEmpty())rows.add("base-terms",Arrays.asList(c.text("base_terms"),c.number("template_version")),()->{
            LinearLayout card=ui.card();ui.add(card,ui.text("Điều khoản chung · Phiên bản "+c.number("template_version"),16,Ui.INK,true));ui.space(card,10);
            android.widget.TextView text=ui.text(c.text("base_terms"),14,Ui.INK,false);text.setTextIsSelectable(true);ui.add(card,text);return card;
        },ui.dp(18));
        if(!c.text("terms").isEmpty())rows.add("terms",c.text("terms"),()->{LinearLayout box=ui.column();ui.section(box,"Điều khoản đã thống nhất",17);ui.space(box,8);android.widget.TextView text=ui.text(c.text("terms"),14,Ui.INK,false);text.setTextIsSelectable(true);ui.add(box,text);return box;},ui.dp(18));
        PaymentState.Row p=money().paymentFor("CONTRACT",c.id);
        rows.add("payment",Arrays.asList(c.id,c.number("fee"),c.number("sessions"),p),()->{LinearLayout box=ui.column();card(box,"Thanh toán hợp đồng",p==null?"Chưa ghi nhận thanh toán":PaymentState.status(p.text("status")),PaymentState.money(c.number("fee")*c.number("sessions")),p==null?null:()->money().openExisting(p.id,"admin_contract"));return box;},0);
        if(person(c.text("tutor_id"))!=null)rows.add("tutor",c.text("tutor_id"),()->{LinearLayout box=ui.column();action(box,"Xem tài khoản gia sư",Ui.OUTLINE,()->user(c.text("tutor_id")));return box;},0);rows.commit();
    }
    @Override public void onDestroyView(){super.onDestroyView();body=null;}
}
