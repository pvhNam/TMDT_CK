package com.example.tmdt.ui.profile;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.lifecycle.ViewModelProvider;
import com.example.tmdt.*;
import com.example.tmdt.ui.common.*;
import java.util.*;

abstract class TutorReviewScreen extends ScreenFragment {
    protected TutorReviewState review;
    protected AccountState account;
    @Override public void onViewCreated(View view,Bundle state){
        review=new ViewModelProvider(requireActivity()).get(TutorReviewState.class);
        account=new ViewModelProvider(requireActivity()).get(AccountState.class);
        super.onViewCreated(view,state);
        review.changes.observe(getViewLifecycleOwner(),ignored->refresh());
    }
    protected void go(String screen){((MainActivity)requireActivity()).show(screen);}
    protected void feedback(LinearLayout body){
        if(review.busy){ProgressBar progress=new ProgressBar(requireContext());body.addView(progress,ui.lp(32,32));ui.space(body,8);}
        if(!review.error.isEmpty()){ui.add(body,ui.note(R.drawable.ic_info,review.error,0xFFFFF0F0,Ui.RED,Ui.RED));ui.space(body,12);}
        if(!review.notice.isEmpty()){ui.add(body,ui.note(R.drawable.ic_info,review.notice,Ui.PALE,Ui.BLUE,Ui.MUTED));ui.space(body,12);}
    }
    protected void action(LinearLayout body,String label,int style,Runnable click){
        View button=ui.action(label,0,review.busy?Ui.DISABLED:style,click);
        int fill=review.busy?Ui.GRAY:style==Ui.PRIMARY?Ui.BLUE:Ui.WHITE;
        ui.surface(button,fill,12,style==Ui.OUTLINE?Ui.BLUE:0);
        if(!review.busy)ui.clickable(button,click);
        ui.addAction(body,button,48);ui.space(body,10);
    }
    protected void files(LinearLayout body,List<Map<String,Object>> files){
        if(files.isEmpty()){ui.add(body,ui.text("Hồ sơ cũ chưa có tệp minh chứng.",14,Ui.MUTED,false));return;}
        for(Map<String,Object> file:files){
            LinearLayout card=ui.card(),row=ui.row();row.addView(ui.icon(R.drawable.ic_file,28,Ui.BLUE));ui.gap(row,12);
            LinearLayout copy=ui.column();ui.add(copy,ui.text(TutorReviewState.text(file,"name"),14,Ui.INK,true));ui.space(copy,4);
            Object size=file.get("size");ui.add(copy,ui.text((size instanceof Number?String.format(Locale.US,"%.1f MB",((Number)size).doubleValue()/1048576):"")+" · Đã tải lên",12,Ui.MUTED,false));
            ui.weight(row,copy);ui.add(card,row);
            if(TutorEvidenceDraft.isImage(file)){ui.space(card,12);ui.add(card,new EvidenceImageView(requireContext(),file));}
            else ui.clickable(card,()->review.open(requireContext(),file));
            ui.add(body,card);ui.space(body,10);
        }
    }
    protected void summary(LinearLayout body,Map<String,Object> data,String uid){
        LinearLayout card=ui.card(),row=ui.row();row.addView(ui.avatar(uid,TutorReviewState.text(data,"ho_ten"),64));ui.gap(row,14);
        LinearLayout copy=ui.column();ui.add(copy,ui.text(TutorReviewState.text(data,"ho_ten"),18,Ui.INK,true));ui.space(copy,6);
        String subject=TutorReviewState.text(data,"mon_hoc"),levels=TutorReviewState.text(data,"cap_lop");
        ui.add(copy,ui.text(subject+(levels.isEmpty()?"":" · "+levels),14,Ui.MUTED,false));ui.weight(row,copy);ui.add(card,row);ui.add(body,card);ui.space(body,18);
    }
    protected static String label(String status){
        switch(status){case "VERIFIED":return "Đã duyệt";case "NEEDS_REVISION":return "Cần bổ sung";case "REJECTED":return "Chưa được chấp thuận";case "IN_REVIEW":return "Đang kiểm tra";default:return "Chờ xét duyệt";}
    }
    protected String date(Map<String,Object> data,String field){
        Object value=data.get(field);return value instanceof com.google.firebase.Timestamp?new java.text.SimpleDateFormat("dd/MM/yyyy · HH:mm",Locale.getDefault()).format(((com.google.firebase.Timestamp)value).toDate()):"Đang đồng bộ thời gian gửi";
    }
}
