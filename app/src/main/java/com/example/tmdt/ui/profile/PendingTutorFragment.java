package com.example.tmdt.ui.profile;

import android.view.*;
import android.widget.*;
import com.example.tmdt.*;
import com.example.tmdt.ui.common.Ui;

public final class PendingTutorFragment extends TutorReviewScreen {
    @Override protected View build(){
        LinearLayout root=ui.column();root.setBackgroundColor(Ui.WHITE);ui.header(root,"Hồ sơ gia sư",()->go("account"),null);
        LinearLayout body=ui.page(root);feedback(body);
        if(review.loading){ui.add(body,ui.text("Đang tải hồ sơ…",15,Ui.MUTED,false));return root;}
        if(review.own.isEmpty()){ui.add(body,ui.text("Bạn chưa gửi hồ sơ gia sư.",16,Ui.INK,true));ui.space(body,20);action(body,"Đăng ký làm gia sư",Ui.PRIMARY,()->go("register_tutor"));return root;}
        String status=review.status();boolean approved=status.equals("VERIFIED"),needs=status.equals("NEEDS_REVISION"),rejected=status.equals("REJECTED");
        LinearLayout hero=ui.card();ui.surface(hero,approved?Ui.GREEN_BG:0xFFFFF6E5,16,0);hero.setGravity(Gravity.CENTER);
        hero.addView(approved?ui.icon(R.drawable.ic_users,58,Ui.GREEN):ui.art(R.drawable.illustration_review_pending,140,88));ui.space(hero,10);
        TextView heading=ui.text(approved?"Hồ sơ đã được duyệt":needs?"Hồ sơ cần bổ sung":rejected?"Hồ sơ chưa được chấp thuận":"Hồ sơ đang được xét duyệt",19,Ui.INK,true);heading.setGravity(Gravity.CENTER);ui.add(hero,heading);ui.space(hero,8);
        TextView caption=ui.text(approved?"Bạn có thể chuyển sang gia sư để quản lý lịch dạy.":needs||rejected?TutorReviewState.text(review.own,"ly_do"):"Kết quả sẽ được cập nhật tại đây khi có thay đổi.",14,Ui.MUTED,false);caption.setGravity(Gravity.CENTER);ui.add(hero,caption);ui.add(body,hero);ui.space(body,18);
        stage(body,"✓","Đã gửi hồ sơ","Hồ sơ của bạn đã được gửi thành công.",date(review.own,review.own.containsKey("submitted_at")?"submitted_at":"created_at"),Ui.BLUE,true);
        stage(body,status.equals("IN_REVIEW")?"•":approved||needs||rejected?"✓":"2",status.equals("IN_REVIEW")?"Đang kiểm tra":"Chờ kiểm tra","Đội ngũ của chúng tôi đang xem xét thông tin hồ sơ của bạn.","",Ui.BLUE,true);
        stage(body,approved?"✓":"", "Kết quả xét duyệt",approved||needs||rejected?label(status):"Kết quả sẽ được cập nhật trong ứng dụng.","",approved?Ui.GREEN:0xFFC3D1E5,false);
        ui.space(body,8);
        LinearLayout info=ui.column();ui.pad(info,12,12);ui.surface(info,0xFFFAFCFF,12,Ui.BORDER);
        ui.add(info,ui.text("Thông tin hồ sơ",14,Ui.INK,true));ui.space(info,12);
        LinearLayout row=ui.row();row.addView(ui.avatar(review.owner,TutorReviewState.text(review.own,"ho_ten"),56));ui.gap(row,12);
        LinearLayout copy=ui.column();ui.add(copy,ui.text(TutorReviewState.text(review.own,"ho_ten"),15,Ui.INK,true));ui.space(copy,4);
        ui.add(copy,ui.text(TutorReviewState.text(review.own,"mon_hoc")+" · "+TutorReviewState.text(review.own,"cap_lop"),12,Ui.MUTED,false));ui.space(copy,6);
        ui.add(copy,ui.text("Đã gửi "+date(review.own,"submitted_at"),11,Ui.MUTED,false));ui.weight(row,copy);
        row.addView(ui.text("›",24,Ui.BLUE,false));ui.add(info,row);ui.clickable(info,()->go("tutor_submitted"));ui.add(body,info);ui.space(body,12);
        action(body,"Xem hồ sơ đã gửi",Ui.PRIMARY,()->go("tutor_submitted"));
        if(needs)action(body,"Bổ sung minh chứng",Ui.PRIMARY,()->go("tutor_supplement"));
        if(approved)action(body,"Chuyển qua gia sư",Ui.PRIMARY,()->classroom.setTutorMode(true));
        action(body,"Về trang cá nhân",Ui.OUTLINE,()->go("account"));return root;
    }
    private void stage(LinearLayout body,String marker,String title,String caption,String date,int color,boolean line){
        LinearLayout row=ui.row();row.setGravity(Gravity.TOP);
        android.widget.FrameLayout track=new android.widget.FrameLayout(requireContext());
        if(line){View connector=new View(requireContext());connector.setBackgroundColor(0xFFD8E6FA);
            android.widget.FrameLayout.LayoutParams cp=new android.widget.FrameLayout.LayoutParams(ui.dp(2),-1,Gravity.CENTER_HORIZONTAL);cp.topMargin=ui.dp(27);track.addView(connector,cp);}
        TextView dot=ui.text(marker,16,Ui.WHITE,true);dot.setGravity(Gravity.CENTER);ui.surface(dot,color,16,0);
        track.addView(dot,new android.widget.FrameLayout.LayoutParams(ui.dp(26),ui.dp(26),Gravity.TOP|Gravity.CENTER_HORIZONTAL));
        row.addView(track,new LinearLayout.LayoutParams(ui.dp(32),-1));ui.gap(row,10);
        LinearLayout copy=ui.column();copy.setMinimumHeight(ui.dp(76));
        ui.add(copy,ui.text(title,14,Ui.INK,true));ui.space(copy,5);ui.add(copy,ui.text(caption,12,Ui.MUTED,false));
        if(!date.isEmpty()){ui.space(copy,4);ui.add(copy,ui.text(date,11,Ui.MUTED,false));}
        ui.space(copy,16);ui.weight(row,copy);ui.add(body,row);
    }
}
