package com.example.tmdt.ui.profile;

import android.view.View;
import android.widget.LinearLayout;
import com.example.tmdt.*;
import com.example.tmdt.ui.common.Ui;

public final class SubmittedTutorFragment extends TutorReviewScreen {
    @Override protected View build(){
        LinearLayout root=ui.column();root.setBackgroundColor(Ui.WHITE);ui.header(root,"Hồ sơ đã gửi",()->go("tutor_pending"),null);
        LinearLayout body=ui.page(root);feedback(body);summary(body,review.own,review.owner);
        ui.add(body,ui.pill(label(review.status()),13,Ui.BLUE,Ui.PALE));ui.space(body,20);
        ui.label(body,"Trường hoặc đơn vị");ui.add(body,ui.text(TutorReviewState.text(review.own,"truong"),15,Ui.INK,false));ui.space(body,16);
        ui.label(body,"Chuyên ngành");ui.add(body,ui.text(TutorReviewState.text(review.own,"chuyen_nganh"),15,Ui.INK,false));ui.space(body,16);
        ui.label(body,"Minh chứng đã tải lên");files(body,TutorReviewState.files(review.own));ui.space(body,18);
        String note=TutorReviewState.text(review.own,"ghi_chu");if(!note.isEmpty()){ui.label(body,"Ghi chú");ui.add(body,ui.text(note,14,Ui.MUTED,false));ui.space(body,18);}
        action(body,"Về trạng thái hồ sơ",Ui.OUTLINE,()->go("tutor_pending"));return root;
    }
}
