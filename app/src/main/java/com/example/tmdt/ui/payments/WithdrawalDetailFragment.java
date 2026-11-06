package com.example.tmdt.ui.payments;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import com.example.tmdt.PaymentState;
import com.example.tmdt.TutorWallet;
import com.example.tmdt.ui.common.RenderSnapshot;
import com.example.tmdt.ui.common.StableViews;
import com.example.tmdt.ui.common.ThemeColors;
import com.example.tmdt.ui.common.Ui;
import java.util.Arrays;
import java.util.Map;

public final class WithdrawalDetailFragment extends PaymentScreen {
    private LinearLayout body;
    @Override public void onViewCreated(@NonNull View view,@Nullable Bundle saved){super.onViewCreated(view,saved);host().administration.changes.observe(getViewLifecycleOwner(),ignored->refreshData());}
    @Override protected Object dataSnapshot(){return RenderSnapshot.of(super.dataSnapshot(),host().administration.users,host().administration.tutors);}
    @Override protected View build(){LinearLayout root=root("Chi tiết rút tiền");body=ui.page(root);render();return root;}
    @Override protected boolean updateContent(){if(body==null)return false;render();return true;}
    private void render(){
        StableViews rows=StableViews.at(body);rows.begin();
        if(!state().error.isEmpty()||!state().notice.isEmpty()||state().busy)rows.add("feedback",Arrays.asList(state().error,state().notice,state().busy),()->{LinearLayout box=ui.column();feedback(box);return box;},0);
        PaymentState.Row row=state().withdrawals.get(state().selectedWithdrawal);
        if(row==null){rows.add("loading",true,()->ui.text("Đang tải yêu cầu rút tiền…",14,Ui.MUTED,false),0);rows.commit();return;}
        boolean pending="PENDING".equals(row.text("status")),processing="PROCESSING".equals(row.text("status")),paid="PAID".equals(row.text("status"));
        rows.add("summary",row,()->{
            LinearLayout card=ui.card();ui.cardSurface(card,Ui.PALE,Ui.BORDER);ui.add(card,ui.text(row.text("tutor_name"),17,Ui.INK,true));ui.space(card,12);
            ui.add(card,ui.text(PaymentState.money(row.number("amount")),27,Ui.INK,true));ui.space(card,10);
            ui.add(card,ui.text(TutorWallet.withdrawalStatus(row.text("status")),13,paid?Ui.GREEN:Ui.MUTED,true));ui.space(card,14);
            fact(card,"Ngày yêu cầu",date(row.time("created_at")));return card;
        },ui.dp(14));
        Map<String,Object> bank=row.map("bank");rows.add("bank",bank,()->{
            LinearLayout card=ui.card();ui.add(card,ui.text("Ngân hàng nhận tiền",15,Ui.INK,true));ui.space(card,10);
            fact(card,"Ngân hàng",value(bank,"bank_name"));fact(card,"Số tài khoản",value(bank,"account_number"));fact(card,"Chủ tài khoản",value(bank,"account_name"));return card;
        },ui.dp(14));
        if(!pending&&!processing)rows.add("result",row,()->{
            LinearLayout card=ui.card();fact(card,"Ngày xử lý",date(row.time("processed_at")));
            if(paid)fact(card,"Mã giao dịch",row.text("bank_reference"));
            else{ui.space(card,6);ui.add(card,ui.text(row.text("review_note"),13,Ui.MUTED,false));ui.space(card,8);ui.add(card,ui.text("Đã hoàn "+PaymentState.money(row.number("amount"))+" về số dư khả dụng.",13,Ui.GREEN,true));}return card;
        },ui.dp(14));
        if(pending||processing){
            PaymentState.Row user=host().administration.users.get(row.text("tutor_id"));if(user==null)user=host().administration.tutors.get(row.text("tutor_id"));boolean blocked=user!=null&&"BANNED".equals(user.text("trang_thai"));
            rows.add("actions",Arrays.asList(row.id,row.text("status"),row.text("processing_by"),state().admin,state().busy,blocked),()->{
                LinearLayout actions=ui.column();
                if(state().admin){
                    if(processing&&!state().owner.equals(row.text("processing_by"))){ui.add(actions,ui.text("Quản trị viên khác đang xử lý yêu cầu này.",13,Ui.MUTED,false));return actions;}
                    boolean enabled=!state().busy&&!blocked;
                    String title=blocked?"Gia sư đang bị khóa":pending?"Nhận xử lý yêu cầu":"Xác nhận đã chuyển khoản";
                    ui.addAction(actions,ui.action(title,0,enabled?Ui.PRIMARY:Ui.DISABLED,enabled?()->{if(pending)state().processWithdrawal(row.id,"claim","");else process(row.id,"paid");}:null),48);ui.space(actions,10);
                    ui.addAction(actions,ui.action("Từ chối và hoàn về ví",0,state().busy?Ui.DISABLED:Ui.OUTLINE,state().busy?null:()->process(row.id,"reject")),48);
                }else if(pending&&state().owner.equals(row.text("tutor_id")))ui.addAction(actions,ui.action("Hủy yêu cầu rút tiền",0,state().busy?Ui.DISABLED:Ui.OUTLINE,state().busy?null:()->ui.dialog().setTitle("Hủy yêu cầu rút tiền?")
                        .setMessage(PaymentState.money(row.number("amount"))).setNegativeButton("Đóng",null).setPositiveButton("Hủy yêu cầu",(dialog,which)->state().processWithdrawal(row.id,"cancel","")).show()),48);
                return actions;
            },0);
        }rows.commit();
    }
    private String value(Map<String,Object> bank,String key){Object value=bank.get(key);return value instanceof String?(String)value:"";}
    private void process(String id,String action){
        boolean paid="paid".equals(action);LinearLayout content=ui.column();ui.pad(content,20,8);
        EditText input=ui.entry(paid?"Mã giao dịch ngân hàng":"Lý do từ chối",!paid);int min=paid?3:5,max=paid?150:500;
        input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(max)});ui.add(content,input);
        AlertDialog dialog=ui.dialog().setTitle(paid?"Xác nhận đã chuyển khoản":"Từ chối yêu cầu rút tiền").setView(content).setNegativeButton("Đóng",null).setPositiveButton("Xác nhận",null).create();
        dialog.setOnShowListener(ignored->{ThemeColors.apply(dialog.getWindow().getDecorView());dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button->{
            String value=input.getText().toString().trim();if(value.length()<min){input.setError("Nhập ít nhất "+min+" ký tự.");return;}state().processWithdrawal(id,action,value);dialog.dismiss();
        });});dialog.show();
    }
    @Override public void onDestroyView(){super.onDestroyView();body=null;}
}
