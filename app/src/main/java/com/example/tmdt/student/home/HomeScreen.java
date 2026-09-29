package com.example.tmdt.student.home;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import java.text.Normalizer;
import java.util.Locale;
import com.example.tmdt.R;
import com.example.tmdt.MainActivity;
import com.example.tmdt.common.LineIcon;
import com.example.tmdt.common.Ui;
import com.example.tmdt.data.Tutor;

public final class HomeScreen {
    private final MainActivity activity;
    private final Ui ui;
    private LinearLayout results, subjects;
    private EditText search;
    private String subject="";

    public HomeScreen(MainActivity activity) { this.activity=activity; this.ui=activity.ui; }

    public View build() {
        LinearLayout root=ui.column();
        LinearLayout body=ui.body(root);
        LinearLayout greeting=ui.row(); ui.weight(greeting,ui.text("Chào Nam!",16,Ui.INK,false));
        greeting.addView(ui.iconButton("bell","Thông báo",()->activity.dialog("Thông báo","Bạn có 2 buổi học mẫu sắp tới. Xem chi tiết tại mục Lịch học.")));
        ui.add(body,greeting); ui.space(body,6);
        ui.add(body,ui.text("Hôm nay bạn muốn học gì?",24,Ui.INK,true)); ui.space(body,16);
        LinearLayout searchBox=ui.row(); ui.pad(searchBox,13,0); ui.surface(searchBox,0xFFF0F5FB,18,0);
        searchBox.addView(new LineIcon(activity,"search",Ui.MUTED),ui.lp(23,23));
        search=ui.input("Tìm môn học, gia sư..."); search.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        search.setContentDescription("Tìm môn học hoặc tên gia sư"); search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        search.setOnEditorActionListener((v,id,event)->{activity.hideKeyboard(); return true;});
        ui.weight(searchBox,search); ui.add(body,searchBox); ui.space(body,12);
        HorizontalScrollView horizontal=new HorizontalScrollView(activity); horizontal.setHorizontalScrollBarEnabled(false);
        subjects=ui.row(); horizontal.addView(subjects); ui.add(body,horizontal); renderSubjects(); ui.space(body,18);
        View banner=banner(); ui.add(body,banner);
        ui.clickable(banner,()->{subject=""; search.setText(""); renderSubjects(); results.requestFocus();});
        ui.space(body,12);
        // Entry to screen 37 · Tìm lớp học nhóm (member 3).
        LinearLayout groups=ui.row(); groups.setPadding(ui.dp(14),ui.dp(12),ui.dp(12),ui.dp(12)); ui.surface(groups,Ui.PALE,10,0);
        groups.addView(ui.icon(R.drawable.ic_users,24,Ui.BLUE)); ui.gap(groups,12);
        ui.weight(groups,ui.text("Lớp học nhóm · Học cùng bạn, chia sẻ học phí",15,Ui.INK,false));
        groups.addView(ui.icon(R.drawable.ic_right,18,Ui.INK)); ui.clickable(groups,()->activity.show("groups")); ui.add(body,groups);
        ui.space(body,24);
        LinearLayout title=ui.row(); ui.weight(title,ui.text("Gia sư nổi bật",20,Ui.INK,true));
        android.widget.TextView more=ui.text("Xem thêm  ›",13,Ui.BLUE,false); more.setGravity(android.view.Gravity.CENTER);
        more.setMinimumHeight(ui.dp(48)); ui.clickable(more,()->{subject=""; search.setText(""); renderSubjects();
            activity.dialog("Danh sách gia sư","Bản giao diện hiện có 2 gia sư mẫu: Nguyễn Minh Anh và Trần Hoàng Nam.");});
        title.addView(more); ui.add(body,title); ui.space(body,4);
        results=ui.column(); ui.add(body,results); renderResults();
        search.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            public void onTextChanged(CharSequence s,int start,int before,int count){renderResults();}
            public void afterTextChanged(Editable s){}
        });
        return root;
    }

    /** "Tìm gia sư phù hợp" banner with the home-books illustration of the design. */
    private View banner() {
        LinearLayout banner=ui.row(); banner.setPadding(ui.dp(16),ui.dp(22),ui.dp(7),ui.dp(10)); banner.setMinimumHeight(ui.dp(154)); ui.surface(banner,Ui.PALE,11,0);
        LinearLayout text=ui.column(); ui.add(text,ui.text("Tìm gia sư phù hợp",21,Ui.INK,true)); ui.space(text,8);
        ui.add(text,ui.text("Linh hoạt thời gian học",16,Ui.MUTED,false)); ui.weight(banner,text);
        banner.addView(ui.art(R.drawable.illus_home_books,126,120));
        banner.setContentDescription("Tìm gia sư phù hợp. Linh hoạt thời gian học.");
        return banner;
    }

    private void renderSubjects() {
        subjects.removeAllViews();
        String[] labels={"Toán","Tiếng Anh","Vật lý","Hóa học"}; String[] icons={"Σ","A","atom","flask"};
        for(int i=0;i<labels.length;i++) {
            String value=labels[i];
            LinearLayout chip=ui.option(value,icons[i],subject.equals(value),()->{
                subject=subject.equals(value)?"":value; renderSubjects(); renderResults();
            });
            ui.pad(chip,8,7);
            for(int child=0;child<chip.getChildCount();child++) {
                View part=chip.getChildAt(child);
                if(part instanceof android.widget.TextView)((android.widget.TextView)part).setTextSize(11);
                if(part instanceof LineIcon)part.setLayoutParams(ui.lp(20,20));
            }
            ui.surface(chip,subject.equals(value)?0xFFD9EAFF:Ui.PALE,19,subject.equals(value)?Ui.BLUE:0);
            subjects.addView(chip,ui.lp(-2,-2)); if(i<3)ui.gap(subjects,7);
        }
    }
    private String normalized(String value) {
        return Normalizer.normalize(value,Normalizer.Form.NFD).replaceAll("\\p{M}","")
                .toLowerCase(Locale.ROOT).replace('đ','d').trim();
    }
    private void renderResults() {
        if(results==null)return;
        results.removeAllViews(); String query=normalized(search.getText().toString()); int count=0;
        for(Tutor tutor:Tutor.ALL) {
            if(!subject.isEmpty() && !subject.equals(tutor.subject))continue;
            if(!normalized(tutor.name+" "+tutor.subject+" "+tutor.level).contains(query))continue;
            ui.add(results,ui.tutorCard(tutor,()->activity.openTutor(tutor))); ui.space(results,10); count++;
        }
        if(count==0) {
            LinearLayout empty=ui.card(); ui.pad(empty,18,24);
            ui.add(empty,ui.text("Chưa tìm thấy gia sư",17,Ui.INK,true)); ui.space(empty,8);
            ui.add(empty,ui.text("Thử tên hoặc môn học khác. Hiện có gia sư Toán và Tiếng Anh trong dữ liệu mẫu.",14,Ui.MUTED,false));
            ui.space(empty,16); ui.add(empty,ui.button("Xóa bộ lọc",null,false,()->{subject="";search.setText("");renderSubjects();renderResults();}));
            ui.add(results,empty);
        }
    }
}
