package com.example.tmdt.ui.admin;

import android.view.View;
import android.widget.LinearLayout;
import com.example.tmdt.*;
import com.example.tmdt.ui.common.Ui;

public final class AdminContractDetailFragment extends AdminScreen {
    @Override protected View build(){
        LinearLayout root=root("Chi tiết hợp đồng"),body=ui.page(root);if(!feedback(body))return root;
        PaymentState.Row c=state().contract(state().selectedContract);if(c==null){ui.add(body,ui.text(state().loadingTutorContracts?"Đang tải hợp đồng…":"Chọn hợp đồng từ danh sách.",15,Ui.MUTED,false));return root;}
        card(body,c.text("code"),c.text("subject"),AdminState.contractStatus(c),null);
        LinearLayout table=ui.table();ui.tableRow(table,R.drawable.ic_pf_user,"Gia sư",c.text("tutor_name"));ui.tableRow(table,R.drawable.ic_pf_user,"Học sinh",c.text("student_name"));
        ui.tableRow(table,R.drawable.ic_file,"Gia sư xác nhận",c.flag("tutor_confirmed")?"Đã ký xác nhận":"Chưa ký");
        ui.tableRow(table,R.drawable.ic_file,"Học sinh xác nhận",c.flag("student_confirmed")?"Đã ký xác nhận":"Chưa ký");
        ui.tableRow(table,R.drawable.ic_calendar,"Ngày bắt đầu",c.text("start_date"));ui.tableRow(table,R.drawable.ic_clock,"Lịch học",c.text("schedule"));
        ui.tableRow(table,R.drawable.ic_file,"Số buổi",c.number("sessions")+" buổi × "+c.number("minutes")+" phút");
        ui.tableRow(table,R.drawable.ic_coin,"Học phí",PaymentState.money(c.number("fee"))+" / buổi");ui.add(body,table);ui.space(body,18);
        ui.section(body,"Điều khoản đã thống nhất",18);ui.space(body,8);ui.add(body,ui.text(c.text("terms"),14,Ui.INK,false));ui.space(body,18);
        PaymentState.Row p=money().paymentFor("CONTRACT",c.id);
        card(body,"Thanh toán hợp đồng",p==null?"Chưa ghi nhận thanh toán":PaymentState.status(p.text("status")),PaymentState.money(c.number("fee")*c.number("sessions")),p==null?null:()->money().openExisting(p.id,"admin_contract"));
        if(state().users.containsKey(c.text("tutor_id"))||state().tutors.containsKey(c.text("tutor_id")))action(body,"Xem tài khoản gia sư",Ui.OUTLINE,()->user(c.text("tutor_id")));
        return root;
    }
}
