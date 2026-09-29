package com.example.tmdt.student.booking;

import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import com.example.tmdt.MainActivity;
import com.example.tmdt.common.LineIcon;
import com.example.tmdt.common.Ui;
import com.example.tmdt.data.Tutor;

public final class ProfileScreen {
    private final MainActivity activity;
    private final Ui ui;
    private final Tutor tutor;
    public ProfileScreen(MainActivity activity,Tutor tutor){this.activity=activity;this.ui=activity.ui;this.tutor=tutor;}

    public View build() {
        LinearLayout root=ui.column();
        ui.header(root,"Hồ sơ gia sư",activity::back,ui.iconButton(activity.favorite(tutor)?"heart_filled":"heart",
                activity.favorite(tutor)?"Bỏ lưu gia sư":"Lưu gia sư",()->activity.toggleFavorite(tutor)));
        LinearLayout body=ui.body(root);
        LinearLayout intro=ui.row(); intro.addView(ui.portrait(tutor,112,122)); ui.gap(intro,14);
        LinearLayout info=ui.column(); ui.add(info,ui.text(tutor.name,21,Ui.INK,true)); ui.space(info,10);
        info.addView(ui.badge("✓  Đã xác minh"),ui.lp(-2,-2)); ui.space(info,10);
        ui.add(info,ui.text(tutor.subject+" · "+tutor.level,14,Ui.INK,false)); ui.weight(intro,info);
        ui.add(body,intro); ui.space(body,16);
        LinearLayout stats=ui.row(); ui.pad(stats,5,13); ui.surface(stats,Ui.PALE,11,0);
        String[] icons={"star","cap","people"}; String[] values={tutor.rating,"3 năm",String.valueOf(tutor.students)};
        String[] labels={"Đánh giá","Kinh nghiệm","Học viên"};
        for(int i=0;i<3;i++) {
            if(i>0){ View line=new View(activity);line.setBackgroundColor(Ui.BORDER);stats.addView(line,ui.lp(1,52)); }
            LinearLayout stat=ui.column();stat.setGravity(Gravity.CENTER);
            stat.addView(new LineIcon(activity,icons[i],i==0?Ui.GOLD:Ui.MUTED),ui.lp(24,24));ui.space(stat,5);
            stat.addView(ui.text(values[i],18,Ui.INK,true));ui.space(stat,4);stat.addView(ui.text(labels[i],13,Ui.MUTED,false));ui.weight(stats,stat);
        }
        ui.add(body,stats);ui.space(body,21);
        ui.heading(body,"Giới thiệu");
        ui.add(body,ui.text(tutor.id==0?"Hướng dẫn dễ hiểu, bám sát mục tiêu và năng lực của học viên.":
                "Luyện giao tiếp tự tin, phát âm tự nhiên và xây dựng lộ trình phù hợp với từng học viên.",15,Ui.MUTED,false));
        ui.space(body,20);ui.heading(body,"Hình thức dạy");
        LinearLayout modes=ui.row();
        ui.weight(modes,ui.option("Trực tuyến","laptop",true,()->activity.openBooking(tutor)));ui.gap(modes,10);
        ui.weight(modes,ui.option("Tại nhà","home",false,()->{activity.openBooking(tutor);activity.booking.setMode("Tại nhà");}));
        ui.add(body,modes);ui.space(body,19);ui.heading(body,"Học phí");
        LinearLayout price=ui.row();ui.pad(price,14,12);ui.surface(price,Ui.PALE,9,0);
        price.addView(new LineIcon(activity,"money",Ui.INK),ui.lp(22,22));ui.gap(price,12);
        price.addView(ui.text(Tutor.money(tutor.rate),20,Ui.INK,true));price.addView(ui.text(" / giờ",14,Ui.INK,false));
        ui.add(body,price);ui.space(body,22);ui.heading(body,"Đánh giá học viên");
        LinearLayout review=ui.column();ui.pad(review,14,14);ui.surface(review,0xFFF0F7FF,10,0);
        ui.add(review,ui.text(tutor.id==0?"“Cô giảng dễ hiểu, rất tận tâm.”":"“Thầy dạy rất vui, mình tự tin nói hơn nhiều.”",14,Ui.INK,false));
        ui.space(review,7);ui.add(review,ui.text("★★★★★",20,Ui.GOLD,true));ui.space(review,5);
        ui.add(review,ui.text(tutor.id==0?"Phạm Quang Huy · Lớp 11":"Ngọc Linh · Sinh viên",12,Ui.MUTED,false));
        ui.clickable(review,()->activity.dialog("Đánh giá học viên",tutor.id==0?
                "Phạm Quang Huy · Lớp 11\n★★★★★\n\nCô giảng dễ hiểu, rất tận tâm. Mình đã hiểu cách giải phương trình và tự tin hơn khi làm bài.":
                "Ngọc Linh · Sinh viên\n★★★★★\n\nThầy dạy rất vui và luôn tạo cơ hội thực hành giao tiếp trong mỗi buổi học."));
        ui.add(body,review);ui.space(body,8);
        LinearLayout footer=ui.footer(root);ui.weight(footer,ui.button("Nhắn tin","chat",false,()->activity.message(tutor)));
        ui.gap(footer,9);ui.weight(footer,ui.button("Đặt lịch học","calendar",true,()->activity.openBooking(tutor)));
        return root;
    }
}
