package com.example.tmdt.ui.onboarding;

import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.tmdt.MainActivity;
import com.example.tmdt.Onboarding;
import com.example.tmdt.R;
import com.example.tmdt.ui.common.ScreenFragment;
import com.example.tmdt.ui.common.Ui;

public final class WelcomeFragment extends ScreenFragment {
    @Override protected View build() {
        LinearLayout root=ui.column();root.setBackgroundColor(Ui.WHITE);
        LinearLayout body=ui.page(root);body.setPadding(ui.dp(24),ui.dp(24),ui.dp(24),ui.dp(28));
        TextView badge=ui.pill("CÙNG NHAU TIẾN BỘ",12,Ui.BLUE,Ui.PALE);body.addView(badge);ui.space(body,20);
        LinearLayout art=ui.row();art.setGravity(Gravity.CENTER);
        art.addView(ui.art(R.drawable.illustration_learning,180,150));ui.add(body,art);ui.space(body,20);
        ui.add(body,ui.text("Chào mừng bạn!",28,Ui.INK,true));ui.space(body,8);
        ui.add(body,ui.text("Bắt đầu hành trình học tập và chia sẻ kiến thức. Bạn muốn tham gia với vai trò nào?",15,Ui.MUTED,false));ui.space(body,24);
        role(body,"Tôi là học sinh","Tìm gia sư và học theo mục tiêu của bạn.",R.drawable.ic_field_level,()->host().show("student_intro"));
        ui.space(body,14);
        role(body,"Tôi là giáo viên","Đăng ký làm gia sư, mở lớp và chia sẻ kiến thức.",R.drawable.ic_users,()->{
            new Onboarding(requireContext()).register(true);host().show("register");
        });
        ui.space(body,24);
        TextView login=ui.link("Đã có tài khoản? Đăng nhập",14,()->{
            new Onboarding(requireContext()).existingAccount();host().show("login");
        });login.setGravity(Gravity.CENTER);ui.add(body,login);
        return root;
    }
    private MainActivity host(){return (MainActivity)requireActivity();}
    private void role(LinearLayout body,String title,String caption,int icon,Runnable click) {
        LinearLayout card=ui.card();ui.cardSurface(card,Ui.PALE,Ui.BORDER);
        LinearLayout row=ui.row();row.addView(ui.icon(icon,28,Ui.BLUE));ui.gap(row,14);
        LinearLayout copy=ui.column();ui.add(copy,ui.text(title,18,Ui.INK,true));ui.space(copy,6);
        ui.add(copy,ui.text(caption,14,Ui.MUTED,false));ui.weight(row,copy);ui.gap(row,8);
        row.addView(ui.text("›",26,Ui.BLUE,false));ui.add(card,row);
        card.setContentDescription(title);ui.clickable(card,click);ui.add(body,card);
    }
}
