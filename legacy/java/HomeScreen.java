package com.example.tmdt;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

final class HomeScreen {
    private final MainActivity activity;
    private final Ui ui;
    private final CatalogState catalog;
    private LinearLayout results, subjects;
    private EditText search;
    HomeScreen(MainActivity activity){this.activity=activity;ui=activity.ui;catalog=activity.catalog;}
    View build(){
        LinearLayout root=ui.column(),body=ui.body(root);
        LinearLayout greeting=ui.row();
        ui.weight(greeting,ui.text(activity.account.profile==null?"Chào bạn!":"Chào "+activity.account.profile.name+"!",16,Ui.INK,false));
        greeting.addView(ui.iconButton("person","Thông tin cá nhân",()->activity.show("account")));
        ui.add(body,greeting);ui.space(body,6);
        ui.add(body,ui.text("Hôm nay bạn muốn học gì?",24,Ui.INK,true));ui.space(body,16);
        LinearLayout searchBox=ui.row();ui.pad(searchBox,13,0);ui.surface(searchBox,0xFFF0F5FB,18,0);
        searchBox.addView(new LineIcon(activity,"search",Ui.MUTED),ui.lp(23,23));
        search=ui.input("Tìm môn học, gia sư...");search.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        search.setContentDescription("Tìm môn học hoặc tên gia sư");search.setText(catalog.query);
        search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        search.setOnEditorActionListener((v,id,event)->{activity.hideKeyboard();return true;});
        ui.weight(searchBox,search);ui.add(body,searchBox);ui.space(body,12);
        HorizontalScrollView horizontal=new HorizontalScrollView(activity);horizontal.setHorizontalScrollBarEnabled(false);
        subjects=ui.row();horizontal.addView(subjects);ui.add(body,horizontal);ui.space(body,18);
        ui.add(body,new DesignImage(activity,2));ui.space(body,20);
        LinearLayout heading=ui.row();ui.weight(heading,ui.text("Gia sư đang nhận lớp",20,Ui.INK,true));
        heading.addView(ui.iconButton("search","Làm mới danh sách",catalog::reload));ui.add(body,heading);
        results=ui.column();ui.add(body,results);
        search.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            public void onTextChanged(CharSequence s,int start,int before,int count){catalog.query=s.toString();renderResults();}
            public void afterTextChanged(Editable s){}
        });
        refresh();return root;
    }
    void refresh(){if(results!=null){renderSubjects();renderResults();}}
    private void renderSubjects(){
        subjects.removeAllViews();addSubject("","Tất cả");
        for(java.util.Map.Entry<String,String> entry:catalog.subjects.entrySet())addSubject(entry.getKey(),entry.getValue());
    }
    private void addSubject(String id,String name){
        subjects.addView(ui.option(name,null,catalog.subject.equals(id),()->{
            catalog.subject=id;renderSubjects();renderResults();
        }),ui.lp(-2,-2));ui.gap(subjects,8);
    }
    private void renderResults(){
        results.removeAllViews();
        if(catalog.loading()){ui.add(results,ui.text("Đang tải danh sách gia sư…",15,Ui.MUTED,false));return;}
        if(!catalog.error().isEmpty()){
            ui.add(results,ui.text(catalog.error(),15,Ui.MUTED,false));ui.space(results,12);
            ui.add(results,ui.button("Thử lại",null,true,catalog::reload));return;
        }
        if(catalog.offline){ui.add(results,ui.text("Đang xem dữ liệu đã lưu trên thiết bị.",12,Ui.MUTED,false));ui.space(results,10);}
        java.util.List<CatalogState.Teacher> teachers=catalog.results();
        for(CatalogState.Teacher teacher:teachers){
            LinearLayout card=ui.card(),row=ui.row();
            TextView initials=ui.text(teacher.name.substring(0,1).toUpperCase(java.util.Locale.ROOT),30,Ui.BLUE,true);
            initials.setGravity(Gravity.CENTER);ui.surface(initials,Ui.PALE,14,0);row.addView(initials,ui.lp(64,82));ui.gap(row,12);
            LinearLayout info=ui.column();ui.add(info,ui.text(teacher.name,16,Ui.INK,true));ui.space(info,6);
            CatalogState.Offering first=teacher.offerings.get(0);long price=Long.MAX_VALUE;
            for(CatalogState.Offering offering:teacher.offerings){
                if(catalog.subject.isEmpty()||catalog.subject.equals(offering.subjectId)){
                    if(offering.price<price){price=offering.price;first=offering;}
                }
            }
            ui.add(info,ui.text(catalog.subjects.get(first.subjectId)+" · "+CatalogState.level(first.level),13,Ui.MUTED,false));ui.space(info,6);
            ui.add(info,ui.text(CatalogState.mode(teacher.mode),12,Ui.MUTED,false));ui.space(info,6);
            ui.add(info,ui.text("Từ "+CatalogState.money(price)+" / 90 phút",14,Ui.BLUE,true));
            ui.weight(row,info);row.addView(new LineIcon(activity,"next",Ui.INK),ui.lp(18,18));ui.add(card,row);
            ui.clickable(card,()->showTeacher(teacher));ui.add(results,card);ui.space(results,12);
        }
        if(teachers.isEmpty()){
            LinearLayout empty=ui.card();ui.pad(empty,18,24);
            boolean filtered=!catalog.query.isEmpty()||!catalog.subject.isEmpty();
            ui.add(empty,ui.text(filtered?"Chưa tìm thấy gia sư":"Chưa có gia sư đang nhận lớp",17,Ui.INK,true));ui.space(empty,8);
            ui.add(empty,ui.text(filtered?"Thử tên hoặc môn học khác để tìm gia sư phù hợp.":"Hồ sơ gia sư sẽ xuất hiện tại đây khi được duyệt và có môn dạy.",14,Ui.MUTED,false));
            if(filtered){ui.space(empty,16);ui.add(empty,ui.button("Xóa bộ lọc",null,false,()->{
                catalog.subject="";search.setText("");renderSubjects();renderResults();
            }));}ui.add(results,empty);
        }
    }
    private void showTeacher(CatalogState.Teacher teacher){
        LinearLayout body=ui.column();ui.pad(body,22,18);
        body.addView(ui.badge("Đã xác minh"));ui.space(body,16);
        ui.add(body,ui.text(teacher.experience+" năm kinh nghiệm · "+CatalogState.mode(teacher.mode),14,Ui.INK,true));ui.space(body,12);
        if(!teacher.region.isEmpty()){ui.add(body,ui.text(teacher.region,14,Ui.MUTED,false));ui.space(body,12);}
        if(!teacher.education.isEmpty()||!teacher.school.isEmpty()){
            ui.add(body,ui.text(teacher.education+" · "+teacher.school,14,Ui.MUTED,false));ui.space(body,12);
        }
        if(!teacher.introduction.isEmpty()){ui.add(body,ui.text(teacher.introduction,15,Ui.INK,false));ui.space(body,18);}
        ui.heading(body,"Môn dạy và học phí");
        for(CatalogState.Offering offering:teacher.offerings){
            ui.add(body,ui.text(catalog.subjects.get(offering.subjectId)+" · "+CatalogState.level(offering.level),15,Ui.INK,true));ui.space(body,5);
            ui.add(body,ui.text(CatalogState.money(offering.price)+" / buổi 90 phút",14,Ui.BLUE,false));ui.space(body,12);
        }
        android.widget.ScrollView scroll=new android.widget.ScrollView(activity);scroll.addView(body);
        new androidx.appcompat.app.AlertDialog.Builder(activity).setTitle(teacher.name).setView(scroll).setPositiveButton("Đóng",null).show();
    }
}
