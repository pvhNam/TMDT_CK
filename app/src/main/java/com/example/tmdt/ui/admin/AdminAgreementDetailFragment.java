package com.example.tmdt.ui.admin;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.tmdt.PaymentState;
import com.example.tmdt.ReportPeriod;
import com.example.tmdt.ui.common.StableViews;
import com.example.tmdt.ui.common.Ui;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/** Reads the signed snapshot from tutor_agreements, independently of the latest editable template. */
public final class AdminAgreementDetailFragment extends AdminScreen {
    private LinearLayout body;
    @Override protected View build(){LinearLayout root=root("Hợp đồng với hệ thống");body=ui.page(root);render();return root;}
    @Override protected boolean updateContent(){if(body==null)return false;render();return true;}
    private void render(){
        StableViews rows=StableViews.at(body);rows.begin();if(!feedback(rows)){rows.commit();return;}
        String uid=state().selectedAgreement;PaymentState.Row agreement=state().agreement(uid);
        if(agreement==null){
            if(state().error.isEmpty())rows.add("empty",state().loadingAgreement,()->ui.text(state().loadingAgreement?"Đang tải hợp đồng…":"Gia sư chưa ký hợp đồng cộng tác với hệ thống.",15,Ui.MUTED,false),0);
            rows.commit();return;
        }
        String tutorName=name(uid,agreement.text("ho_ten"));
        rows.add("identity",Arrays.asList(agreement,tutorName),()->{
            LinearLayout card=ui.card(),row=ui.row(),copy=ui.column();row.addView(ui.avatar(uid,tutorName,52));ui.gap(row,12);
            ui.add(copy,ui.text(tutorName,19,Ui.INK,true));ui.space(copy,8);
            copy.addView(ui.pill("DA_KY".equals(agreement.text("trang_thai"))?"Đã ký":agreement.text("trang_thai"),12,Ui.GREEN,Ui.GREEN_BG));ui.weight(row,copy);ui.add(card,row);return card;
        },ui.dp(16));
        rows.add("details",agreement,()->{
            LinearLayout card=ui.card();detailField(card,"Bên hệ thống","Edu Connect");
            detailField(card,"Tên gia sư tại thời điểm ký",agreement.text("ho_ten"));
            detailField(card,"Tên hợp đồng",agreement.text("tieu_de"));
            detailField(card,"Phiên bản",String.valueOf(agreement.number("phien_ban")));
            detailField(card,"Ngày ký",agreement.time("signed_at")<=0?"":Instant.ofEpochMilli(agreement.time("signed_at")).atZone(ReportPeriod.ZONE).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));return card;
        },ui.dp(18));
        rows.add("content",agreement.text("noi_dung"),()->{
            LinearLayout card=ui.card();ui.add(card,ui.text("Nội dung đã ký",17,Ui.INK,true));ui.space(card,12);
            TextView text=ui.text(agreement.text("noi_dung"),14,Ui.INK,false);text.setTextIsSelectable(true);text.setLineSpacing(ui.dp(5),1);ui.add(card,text);return card;
        },ui.dp(18));
        rows.add("actions",uid,()->{
            LinearLayout box=ui.column();action(box,"Xem hợp đồng giảng dạy",Ui.OUTLINE,()->{state().contractTab=0;state().selectTutor(uid);open("admin_contracts");});
            action(box,"Xem thu nhập gia sư",Ui.OUTLINE,()->income(uid,tutorName));return box;
        },0);rows.commit();
    }
    @Override public void onDestroyView(){super.onDestroyView();body=null;}
}
