package com.example.tmdt.ui.admin;

import android.view.View;
import android.widget.LinearLayout;
import com.example.tmdt.*;
import com.example.tmdt.ui.common.Ui;

public final class AdminUserDetailFragment extends AdminScreen {
    @Override protected View build(){
        LinearLayout root=root("Chi tiết tài khoản"),body=ui.page(root);if(!feedback(body))return root;
        PaymentState.Row found=state().users.get(state().selectedUser);
        final PaymentState.Row r=found==null?state().tutors.get(state().selectedUser):found;
        if(r==null){ui.add(body,ui.text("Hãy chọn tài khoản từ danh sách.",15,Ui.MUTED,false));return root;}
        LinearLayout header=ui.row();header.addView(ui.avatar(r.id,r.text("ho_ten"),64));ui.gap(header,14);
        LinearLayout info=ui.column();ui.add(info,ui.text(r.text("ho_ten"),22,Ui.INK,true));ui.space(info,5);ui.add(info,ui.text(AdminState.role(r),14,Ui.BLUE,true));ui.weight(header,info);ui.add(body,header);ui.space(body,18);
        LinearLayout table=ui.table();ui.tableRow(table,R.drawable.ic_info,"Trạng thái","BANNED".equals(r.text("trang_thai"))?"Đã khóa":"Đang hoạt động");
        ui.tableRow(table,R.drawable.ic_field_email,"Email",r.text("email"));ui.tableRow(table,R.drawable.ic_field_phone,"Điện thoại",r.text("so_dien_thoai"));
        ui.tableRow(table,R.drawable.ic_pin,"Địa chỉ",r.text("dia_chi"));ui.tableRow(table,R.drawable.ic_file,"Cấp học / lớp",r.text("cap_hoc")+" · "+r.text("lop"));ui.add(body,table);ui.space(body,18);
        if(!"ADMIN".equals(r.text("vai_tro")))action(body,"BANNED".equals(r.text("trang_thai"))?"Mở khóa tài khoản":"Khóa tài khoản", "BANNED".equals(r.text("trang_thai"))?Ui.PRIMARY:Ui.DANGER,()->ban(r));
        if(r.flag("la_gia_su"))action(body,"Xem hợp đồng đã ký của gia sư",Ui.PRIMARY,()->{state().selectTutor(r.id);host().show("admin_contracts");});
        else {
        ui.section(body,"Hợp đồng",18);ui.space(body,10);
        int signed=0,total=0;for(PaymentState.Row c:state().contractsFor(r.id)){total++;if(state().signedContract(c))signed++;}
        ui.add(body,ui.text("Đã ký: "+signed+" / "+total,14,Ui.MUTED,false));ui.space(body,12);
        for(PaymentState.Row c:state().contractsFor(r.id))contractCard(body,c);
        if(state().moreContracts)action(body,"Tải thêm hợp đồng để đối chiếu",Ui.OUTLINE,()->state().moreContracts());
        }
        if(r.flag("la_gia_su")){
            long earned=0,paid=0;for(PaymentState.Row e:money().earnings.values())if(r.id.equals(e.text("tutor_id")))earned+=e.number("amount");
            for(PaymentState.Row p:money().payouts.values())if(r.id.equals(p.text("tutor_id"))&&"PAID".equals(p.text("status")))paid+=p.number("amount");
            card(body,"Thu nhập",PaymentState.money(earned),"Đã chi: "+PaymentState.money(paid)+"\nChưa chi: "+PaymentState.money(Math.max(0,earned-paid)),()->host().show("admin_income"));
        }return root;
    }
}
