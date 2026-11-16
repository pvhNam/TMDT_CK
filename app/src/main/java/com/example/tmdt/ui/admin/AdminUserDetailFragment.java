package com.example.tmdt.ui.admin;

import android.view.View;
import android.widget.LinearLayout;
import com.example.tmdt.*;
import com.example.tmdt.ui.common.Ui;
import com.example.tmdt.ui.common.StableViews;
import java.util.Arrays;
import java.util.List;

public final class AdminUserDetailFragment extends AdminScreen {
    private LinearLayout body;
    @Override protected View build(){LinearLayout root=root("Chi tiết tài khoản");body=ui.page(root);render();return root;}
    @Override protected boolean updateContent(){if(body==null)return false;render();return true;}
    private void render(){
        StableViews rows=StableViews.at(body);rows.begin();if(!feedback(rows)){rows.commit();return;}
        final PaymentState.Row r=person(state().selectedUser);
        if(r==null){rows.add("empty",state().loadingUsers,()->ui.text(state().loadingUsers?"Đang tải tài khoản…":"Hãy chọn tài khoản từ danh sách.",15,Ui.MUTED,false),0);rows.commit();return;}
        rows.add("identity",r,()->{
        LinearLayout header=ui.row();header.addView(ui.avatar(r.id,r.text("ho_ten"),64));ui.gap(header,14);
        LinearLayout info=ui.column();ui.add(info,ui.text(r.text("ho_ten"),21,Ui.INK,true));ui.space(info,5);ui.add(info,ui.text(AdminState.role(r),14,Ui.BLUE,true));ui.weight(header,info);return header;
        },ui.dp(18));
        rows.add("profile",r,()->{
        LinearLayout table=ui.table();ui.tableRow(table,R.drawable.ic_info,"Trạng thái","BANNED".equals(r.text("trang_thai"))?"Đã khóa":"Đang hoạt động");
        ui.tableRow(table,R.drawable.ic_field_email,"Email",r.text("email"));ui.tableRow(table,R.drawable.ic_field_phone,"Điện thoại",r.text("so_dien_thoai"));
        ui.tableRow(table,R.drawable.ic_pin,"Địa chỉ",r.text("dia_chi"));ui.tableRow(table,R.drawable.ic_file,"Cấp học / lớp",r.text("cap_hoc")+(r.text("lop").isEmpty()?"":" · "+r.text("lop")));return table;
        },ui.dp(18));
        if(!"ADMIN".equals(r.text("vai_tro")))rows.add("status-action",Arrays.asList(r,state().busy),()->{LinearLayout box=ui.column();action(box,"BANNED".equals(r.text("trang_thai"))?"Mở khóa tài khoản":"Khóa tài khoản","BANNED".equals(r.text("trang_thai"))?Ui.OUTLINE:Ui.DANGER,()->ban(r));return box;},0);
        if(r.flag("la_gia_su"))rows.add("contracts",r.id,()->{LinearLayout box=ui.column();action(box,"Xem hợp đồng đã ký",Ui.OUTLINE,()->{state().selectTutor(r.id);open("admin_contracts");});return box;},0);
        else {
            List<PaymentState.Row> contracts=state().contractsFor(r.id);int signed=0;for(PaymentState.Row c:contracts)if(state().signedContract(c))signed++;
            final int signedCount=signed;rows.add("contracts-heading",Arrays.asList(signed,contracts.size()),()->ui.text("Hợp đồng · "+signedCount+" / "+contracts.size()+" đã ký",16,Ui.INK,true),ui.dp(12));
            for(PaymentState.Row c:contracts)rows.add("contract/"+c.id,c,()->{LinearLayout box=ui.column();contractCard(box,c);return box;},0);
            if(state().moreContracts)rows.add("more-contracts",state().loadingContracts,()->{LinearLayout box=ui.column();action(box,"Tải thêm hợp đồng",Ui.OUTLINE,()->state().moreContracts());return box;},0);
        }
        if(r.flag("la_gia_su")){
            long earned=0,paid=0;for(PaymentState.Row e:money().earnings.values())if(r.id.equals(e.text("tutor_id")))earned+=e.number("amount");
            for(PaymentState.Row p:money().payouts.values())if(r.id.equals(p.text("tutor_id"))&&TutorWallet.salaryPaid(p.text("status")))paid+=p.number("amount");
            final long totalEarned=earned,totalPaid=paid;rows.add("income",Arrays.asList(r.id,r.text("ho_ten"),earned,paid,money().incomeLoaded),()->{
                LinearLayout card=ui.card();ui.add(card,ui.text("Tổng thu nhập",13,Ui.MUTED,false));ui.space(card,10);ui.add(card,ui.text(money().incomeLoaded?PaymentState.money(totalEarned):"Đang tải thu nhập…",24,Ui.INK,true));ui.space(card,14);
                LinearLayout stats=ui.row();ui.weight(stats,metric("Đã trả",totalPaid,Ui.GREEN));ui.gap(stats,12);ui.weight(stats,metric("Chưa trả",Math.max(0,totalEarned-totalPaid),Ui.ORANGE));ui.add(card,stats);ui.space(card,12);
                ui.add(card,ui.text("Xem chi tiết theo tháng  ›",13,Ui.BLUE,true));ui.clickable(card,()->income(r.id,r.text("ho_ten")));return card;
            },0);
        }rows.commit();
    }
    @Override public void onDestroyView(){super.onDestroyView();body=null;}
}
