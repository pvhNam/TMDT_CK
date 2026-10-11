package com.example.tmdt.ui.onboarding;

import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.tmdt.MainActivity;
import com.example.tmdt.Onboarding;
import com.example.tmdt.R;
import com.example.tmdt.ui.common.ScreenFragment;
import com.example.tmdt.ui.common.Ui;

public final class StudentIntroFragment extends ScreenFragment {
    private static final String[] LEVELS={"Tiểu học","THCS","THPT","Đại học","Khác"};
    @Override protected View build() {
        Onboarding intro=new Onboarding(requireContext());
        LinearLayout root=ui.column();root.setBackgroundColor(Ui.WHITE);
        ui.header(root,"Dành cho học sinh",()->host().show("welcome"),null);
        LinearLayout body=ui.page(root);body.setPadding(ui.dp(24),ui.dp(20),ui.dp(24),ui.dp(28));
        ui.add(body,ui.text("Hiểu bạn hơn một chút",26,Ui.INK,true));ui.space(body,10);
        ui.add(body,ui.text("Cho chúng mình biết thông tin học tập để bắt đầu hồ sơ của bạn.",14,Ui.MUTED,false));ui.space(body,28);
        TextView level=ui.value(intro.level().isEmpty()?"Chọn cấp học":intro.level());
        TextView grade=ui.value(intro.grade().isEmpty()?"Chọn lớp / năm học":intro.grade());
        EditText age=ui.entry("Nhập tuổi của bạn",false);age.setId(R.id.onboarding_age);
        age.setInputType(InputType.TYPE_CLASS_NUMBER);age.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(3)});
        age.setText(intro.age());
        ui.label(body,"Cấp học");
        View levelPicker=ui.picker(R.drawable.ic_field_level,level,()->ui.dialog().setTitle("Chọn cấp học").setItems(LEVELS,(dialog,index)->{
            String selected=LEVELS[index];
            String selectedGrade=selected.equals(intro.level())?intro.grade():"";
            intro.studentDraft(selected,selectedGrade,age.getText().toString());level.setText(selected);
            grade.setText(selectedGrade.isEmpty()?"Chọn lớp / năm học":selectedGrade);
        }).show());levelPicker.setContentDescription("Chọn cấp học");ui.add(body,levelPicker);ui.space(body,20);
        ui.label(body,"Tuổi");ui.add(body,age);ui.space(body,20);
        ui.label(body,"Lớp / năm học");
        View gradePicker=ui.picker(R.drawable.ic_field_level,grade,()->{
            if(intro.level().isEmpty()){ui.dialog().setMessage("Bạn hãy chọn cấp học trước.").setPositiveButton("Đã hiểu",null).show();return;}
            String[] choices=Onboarding.grades(intro.level());
            ui.dialog().setTitle("Chọn lớp / năm học").setItems(choices,(dialog,index)->{
                intro.studentDraft(intro.level(),choices[index],age.getText().toString());grade.setText(choices[index]);
            }).show();
        });gradePicker.setContentDescription("Chọn lớp / năm học");ui.add(body,gradePicker);ui.space(body,24);
        age.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            public void onTextChanged(CharSequence s,int start,int before,int count){intro.studentDraft(intro.level(),intro.grade(),s.toString());age.setError(null);}
            public void afterTextChanged(Editable s){}
        });
        ui.add(body,ui.note(R.drawable.ic_info,"Thông tin này sẽ được điền vào hồ sơ khi bạn tạo tài khoản.",Ui.PALE,Ui.BLUE,Ui.MUTED));ui.space(body,24);
        ui.addAction(body,ui.action("Tiếp tục đăng ký",0,Ui.PRIMARY,()->{
            int years;try{years=Integer.parseInt(age.getText().toString());}catch(NumberFormatException e){years=0;}
            if(years<5||years>100){age.setError("Vui lòng nhập tuổi từ 5 đến 100");age.requestFocus();return;}
            if(intro.level().isEmpty()||intro.grade().isEmpty()){
                ui.dialog().setMessage("Bạn hãy chọn đầy đủ cấp học và lớp / năm học.").setPositiveButton("Đã hiểu",null).show();return;
            }
            intro.register(false);host().hideKeyboard();host().show("register");
        }),52);
        return root;
    }
    private MainActivity host(){return (MainActivity)requireActivity();}
}
