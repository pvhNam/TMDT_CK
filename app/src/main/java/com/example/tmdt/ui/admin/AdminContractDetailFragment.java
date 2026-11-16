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
        PaymentState.Row c=state().contract(state().selectedContract);if(c==null){rows.add("empty",state().loadingTutorContracts,()->ui.text(state().loadingTutorContracts?"Đang tải hợp đồng…":"Chọn hợp đồng từ danh sách.",15,Ui.MUTED,false),0);rows.commit();return;}
        rows.add("summary",Arrays.asList(c.text("code"),c.text("subject"),c.text("status")),()->{LinearLayout box=ui.column();card(box,c.text("code"),c.text("subject"),AdminState.contractStatus(c),null);return box;},0);
        rows.add("details",c,()->{
        LinearLayout table=ui.table();ui.tableRow(table,R.drawable.ic_pf_user,"Gia sư",c.text("tutor_name"));ui.tableRow(table,R.drawable.ic_pf_user,"Học sinh",c.text("student_name"));
        ui.tableRow(table,R.drawable.ic_file,"Gia sư xác nhận",c.flag("tutor_confirmed")?"Đã ký xác nhận":"Chưa ký");
        ui.tableRow(table,R.drawable.ic_file,"Học sinh xác nhận",c.flag("student_confirmed")?"Đã ký xác nhận":"Chưa ký");
        ui.tableRow(table,R.drawable.ic_calendar,"Ngày bắt đầu",c.text("start_date"));ui.tableRow(table,R.drawable.ic_clock,"Lịch học",c.text("schedule"));
        ui.tableRow(table,R.drawable.ic_file,"Số buổi",c.number("sessions")+" buổi × "+c.number("minutes")+" phút");
        ui.tableRow(table,R.drawable.ic_coin,"Học phí",PaymentState.money(c.number("fee"))+" / buổi");return table;
        },ui.dp(18));
        rows.add("terms",c.text("terms"),()->{LinearLayout box=ui.column();ui.section(box,"Điều khoản đã thống nhất",17);ui.space(box,8);ui.add(box,ui.text(c.text("terms"),14,Ui.INK,false));return box;},ui.dp(18));
        PaymentState.Row p=money().paymentFor("CONTRACT",c.id);
        rows.add("payment",Arrays.asList(c.id,c.number("fee"),c.number("sessions"),p),()->{LinearLayout box=ui.column();card(box,"Thanh toán hợp đồng",p==null?"Chưa ghi nhận thanh toán":PaymentState.status(p.text("status")),PaymentState.money(c.number("fee")*c.number("sessions")),p==null?null:()->money().openExisting(p.id,"admin_contract"));return box;},0);
        if(person(c.text("tutor_id"))!=null)rows.add("tutor",c.text("tutor_id"),()->{LinearLayout box=ui.column();action(box,"Xem tài khoản gia sư",Ui.OUTLINE,()->user(c.text("tutor_id")));return box;},0);rows.commit();
    }
    @Override public void onDestroyView(){super.onDestroyView();body=null;}
}
