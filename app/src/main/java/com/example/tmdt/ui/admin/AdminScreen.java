package com.example.tmdt.ui.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.example.tmdt.*;
import com.example.tmdt.ui.common.ScreenFragment;
import com.example.tmdt.ui.common.Ui;

abstract class AdminScreen extends ScreenFragment {
    private EditText liveSearch;
    private java.util.function.Consumer<String> liveResults;
    protected MainActivity host(){return (MainActivity)requireActivity();}
    protected AdminState state(){return new ViewModelProvider(requireActivity()).get(AdminState.class);}
    protected PaymentState money(){return host().payments;}
    @Override public void onViewCreated(@NonNull View view,@Nullable Bundle saved){
        super.onViewCreated(view,saved);
        state().changes.observe(getViewLifecycleOwner(),ignored->dataChanged());
        money().changes.observe(getViewLifecycleOwner(),ignored->dataChanged());
    }
    private void dataChanged(){
        if(liveSearch!=null&&liveSearch.isAttachedToWindow()&&liveSearch.hasFocus()&&state().error.isEmpty()&&money().error.isEmpty())
            liveResults.accept(liveSearch.getText().toString());
        else refresh();
    }
    @Override public void onDestroyView(){liveSearch=null;liveResults=null;super.onDestroyView();}
    protected LinearLayout root(String title){
        LinearLayout root=ui.column();root.setBackgroundColor(0xFFF7F9FD);
        boolean dashboard="admin_dashboard".equals(host().currentScreen());
        View trailing=dashboard?ui.iconButton(R.drawable.ic_pf_user,Ui.BLUE,"Tài khoản quản trị",()->open("account")):
            ui.link("Làm mới",12,()->{if(!state().busy)state().retry();});
        ui.header(root,title,dashboard?null:host()::back,trailing);return root;
    }
    protected void open(String screen){state().notice="";if("payments".equals(screen))money().adminPaymentTab=0;host().hideKeyboard();host().show(screen);}
    protected void openRefunds(){money().adminPaymentTab=1;state().notice="";host().hideKeyboard();host().show("payments");}
    protected void openPayroll(){
        String period=money().payouts.values().stream().filter(p->"READY".equals(p.text("status")))
            .map(p->p.text("period")).filter(p->p.matches("[0-9]{4}-[0-9]{2}")).sorted().findFirst().orElse(money().payrollPeriod);
        money().payrollPeriod=period;open("admin_payroll");
    }
    protected void destination(LinearLayout body,int icon,String title,String detail,String badge,Runnable run){
        LinearLayout card=ui.card(),row=ui.row(),copy=ui.column();card.setMinimumHeight(ui.dp(74));
        LinearLayout symbol=ui.row();symbol.setGravity(android.view.Gravity.CENTER);ui.surface(symbol,Ui.PALE,14,0);
        symbol.addView(ui.icon(icon,23,Ui.BLUE));row.addView(symbol,ui.lp(44,44));ui.gap(row,12);
        ui.add(copy,ui.text(title,16,Ui.INK,true));ui.space(copy,4);ui.add(copy,ui.text(detail,12,Ui.MUTED,false));ui.weight(row,copy);ui.gap(row,8);
        if(!badge.isEmpty()){row.addView(ui.pill(badge,12,Ui.BLUE,Ui.PALE));ui.gap(row,6);}
        row.addView(ui.text("›",24,Ui.MUTED,false));ui.add(card,row);card.setContentDescription(title);
        ui.clickable(card,()->{if(!state().busy)run.run();});ui.add(body,card);ui.space(body,10);
    }
    protected LinearLayout statistic(String title,String value,Runnable click){
        LinearLayout card=ui.card();ui.add(card,ui.text(title,12,Ui.MUTED,false));ui.space(card,7);
        android.widget.TextView number=ui.text(value,22,Ui.INK,true);number.setMaxLines(1);
        number.setAutoSizeTextTypeUniformWithConfiguration(13,22,1,android.util.TypedValue.COMPLEX_UNIT_SP);ui.add(card,number);
        if(click!=null){card.setContentDescription(title);ui.clickable(card,click);}return card;
    }
    /** Only rebuild the results while typing, keeping keyboard focus and caret stable. */
    protected void search(LinearLayout body,String hint,String value,java.util.function.Consumer<String> changed){
        LinearLayout row=ui.row();EditText input=ui.input(hint);input.setText(value);input.setContentDescription(hint);
        liveSearch=input;liveResults=changed;
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);input.setSingleLine(true);
        input.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        input.setOnEditorActionListener((v,id,event)->{if(id==android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH){host().hideKeyboard();return true;}return false;});
        ui.weight(row,input);ui.gap(row,6);View clear=ui.link("Xóa",13,()->input.setText(""));clear.setMinimumHeight(ui.dp(48));clear.setMinimumWidth(ui.dp(44));clear.setContentDescription("Xóa từ khóa");row.addView(clear);
        input.addTextChangedListener(new android.text.TextWatcher(){
            public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            public void onTextChanged(CharSequence s,int start,int before,int count){changed.accept(s.toString());}
            public void afterTextChanged(android.text.Editable s){}
        });ui.add(body,row);ui.space(body,12);
    }
    protected boolean feedback(LinearLayout body){
        if(!state().active){ui.add(body,ui.text("Chỉ quản trị viên đang hoạt động được truy cập.",16,Ui.INK,true));return false;}
        if(!state().error.isEmpty()){
            ui.add(body,ui.note(R.drawable.ic_info,state().error,0xFFFFF0F0,Ui.RED,Ui.RED));ui.space(body,10);
            action(body,"Tải lại dữ liệu",Ui.OUTLINE,()->state().retry());
        }
        if(!state().notice.isEmpty()){ui.add(body,ui.note(R.drawable.ic_info,state().notice,Ui.GREEN_BG,Ui.GREEN,Ui.INK));ui.space(body,12);}
        if(state().busy){ui.add(body,ui.text("Đang cập nhật…",14,Ui.BLUE,false));ui.space(body,10);}
        return true;
    }
    protected void action(LinearLayout body,String label,int style,Runnable run){ui.addAction(body,ui.action(label,0,state().busy?Ui.DISABLED:style,state().busy?null:run),48);ui.space(body,10);}
    protected void card(LinearLayout body,String title,String value,String detail,Runnable click){
        LinearLayout card=ui.card();ui.add(card,ui.text(title,15,Ui.MUTED,false));ui.space(card,7);
        ui.add(card,ui.text(value,23,Ui.BLUE,true));
        if(!detail.isEmpty()){ui.space(card,7);ui.add(card,ui.text(detail,13,Ui.MUTED,false));}
        if(click!=null)ui.clickable(card,click);ui.add(body,card);ui.space(body,12);
    }
    protected void user(String uid){state().notice="";state().userOrigin=host().currentScreen();state().selectedUser=uid;host().show("admin_user");}
    protected void contract(String id){state().contractOrigin=host().currentScreen();state().selectedContract=id;host().show("admin_contract");}
    protected String name(String uid,String fallback){PaymentState.Row user=state().users.get(uid);return user==null?fallback:user.text("ho_ten");}
    protected void contractCard(LinearLayout body,PaymentState.Row row){
        card(body,row.text("code").isEmpty()?"Hợp đồng":row.text("code"),row.text("subject"),
                row.text("tutor_name")+" ↔ "+row.text("student_name")+"\n"+com.example.tmdt.AdminState.contractStatus(row),()->contract(row.id));
    }
    protected void ban(PaymentState.Row user){
        boolean ban=!"BANNED".equals(user.text("trang_thai"));
        EditText input=ui.entry("Lý do (5–500 ký tự)",true);input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(500)});
        LinearLayout content=ui.column();ui.pad(content,22,8);
        ui.add(content,ui.text(ban?"Tài khoản sẽ bị chặn thao tác và tạm dừng chi trả mới. Dữ liệu học tập và số tiền đã ghi nhận vẫn được giữ để đối soát.":"Tài khoản có thể hoạt động trở lại.",14,Ui.MUTED,false));ui.space(content,12);ui.add(content,input);
        androidx.appcompat.app.AlertDialog dialog=ui.dialog().setTitle((ban?"Khóa ":"Mở khóa ")+user.text("ho_ten")).setView(content).setNegativeButton("Đóng",null).setPositiveButton("Xác nhận",null).create();
        dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{String reason=input.getText().toString().trim();if(reason.length()<5){input.setError("Nhập ít nhất 5 ký tự");return;}state().status(user.id,ban,reason);dialog.dismiss();}));dialog.show();
    }
}
