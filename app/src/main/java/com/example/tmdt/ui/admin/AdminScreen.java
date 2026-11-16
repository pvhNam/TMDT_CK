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
import com.example.tmdt.ui.common.StableViews;
import com.example.tmdt.ui.common.Ui;

abstract class AdminScreen extends ScreenFragment {
    private EditText liveSearch;
    protected MainActivity host(){return (MainActivity)requireActivity();}
    protected AdminState state(){return new ViewModelProvider(requireActivity()).get(AdminState.class);}
    protected PaymentState money(){return host().payments;}
    @Override public void onViewCreated(@NonNull View view,@Nullable Bundle saved){
        super.onViewCreated(view,saved);
        state().changes.observe(getViewLifecycleOwner(),ignored->dataChanged());
        money().changes.observe(getViewLifecycleOwner(),ignored->dataChanged());
    }
    private void dataChanged(){refreshData();}
    @Override protected Object dataSnapshot(){return com.example.tmdt.ui.common.RenderSnapshot.of(state(),money());}
    @Override public void onDestroyView(){liveSearch=null;super.onDestroyView();}
    protected LinearLayout root(String title){
        LinearLayout root=ui.column();root.setBackgroundColor(com.example.tmdt.ui.common.ThemeColors.background(requireContext()));
        boolean main=AdminNavigation.mainScreen(host().currentScreen());
        View trailing="account".equals(host().currentScreen())?null:ui.link("Làm mới",12,this::reload);
        ui.header(root,title,main?null:host()::back,trailing);return root;
    }
    protected void reload(){if(state().busy||money().busy)return;state().retry();money().retry();}
    protected void open(String screen){state().notice="";if("payments".equals(screen))money().adminPaymentTab=0;if("admin_payroll".equals(screen))money().payrollOrigin=host().currentScreen();host().hideKeyboard();host().show(screen);}
    protected void openRefunds(){money().adminPaymentTab=1;state().notice="";host().hideKeyboard();host().show("payments");}
    protected void openPayroll(){
        String period=money().payouts.values().stream().filter(p->"READY".equals(p.text("status")))
            .map(p->p.text("period")).filter(p->p.matches("[0-9]{4}-[0-9]{2}")).sorted().findFirst().orElse(money().payrollPeriod);
        money().payrollPeriod=period;open("admin_payroll");
    }
    protected void destination(LinearLayout body,int icon,String title,String badge,Runnable run){
        LinearLayout card=ui.card(),row=ui.row(),copy=ui.column();ui.pad(card,14,13);card.setMinimumHeight(ui.dp(64));
        LinearLayout symbol=ui.row();symbol.setGravity(android.view.Gravity.CENTER);ui.surface(symbol,Ui.PALE,14,0);
        symbol.addView(ui.icon(icon,23,Ui.BLUE));row.addView(symbol,ui.lp(44,44));ui.gap(row,12);
        ui.add(copy,ui.text(title,16,Ui.INK,true));ui.weight(row,copy);ui.gap(row,8);
        if(!badge.isEmpty()){row.addView(ui.pill(badge,12,Ui.BLUE,Ui.PALE));ui.gap(row,6);}
        row.addView(ui.text("›",24,Ui.MUTED,false));ui.add(card,row);card.setContentDescription(title);
        ui.clickable(card,()->{if(!state().busy)run.run();});ui.add(body,card);ui.space(body,10);
    }
    protected LinearLayout menuGroup(LinearLayout body,String title){
        ui.section(body,title,17);ui.space(body,10);
        LinearLayout card=ui.card();ui.pad(card,0,0);card.setClipToOutline(true);ui.add(body,card);return card;
    }
    /** Related actions share one surface, with comfortable touch targets and a consistent arrow. */
    protected void menuItem(LinearLayout card,int icon,String title,String badge,Runnable run){
        if(card.getChildCount()>0){
            View divider=new View(requireContext());divider.setBackgroundColor(com.example.tmdt.ui.common.ThemeColors.border(requireContext(),Ui.BORDER));
            LinearLayout.LayoutParams params=ui.lp(-1,1);params.leftMargin=ui.dp(64);params.rightMargin=ui.dp(14);card.addView(divider,params);
        }
        LinearLayout row=ui.row(),symbol=ui.row();ui.pad(row,14,12);row.setMinimumHeight(ui.dp(66));
        symbol.setGravity(android.view.Gravity.CENTER);ui.surface(symbol,Ui.PALE,12,0);
        symbol.addView(ui.icon(icon,21,Ui.BLUE));row.addView(symbol,ui.lp(38,38));ui.gap(row,12);
        android.widget.TextView label=ui.text(title,15,Ui.INK,true);label.setMaxLines(2);ui.weight(row,label);ui.gap(row,8);
        if(!badge.isEmpty()){row.addView(ui.pill(badge,11,Ui.BLUE,Ui.PALE));ui.gap(row,6);}
        row.addView(ui.text("›",23,Ui.MUTED,false));row.setContentDescription(title+(badge.isEmpty()?"":", "+badge));
        ui.clickable(row,()->{if(!state().busy)run.run();});ui.add(card,row);
    }
    protected View shortcut(int icon,String title,Runnable run){
        LinearLayout card=ui.card();card.setMinimumHeight(ui.dp(106));
        LinearLayout row=ui.row();LinearLayout symbol=ui.row();symbol.setGravity(android.view.Gravity.CENTER);
        ui.surface(symbol,Ui.PALE,13,0);symbol.addView(ui.icon(icon,23,Ui.BLUE));row.addView(symbol,ui.lp(40,40));
        ui.weight(row,new View(requireContext()));row.addView(ui.text("›",22,Ui.MUTED,false));ui.add(card,row);ui.space(card,12);
        android.widget.TextView label=ui.text(title,15,Ui.INK,true);label.setMaxLines(2);ui.add(card,label);
        card.setContentDescription(title);ui.clickable(card,()->{if(!state().busy)run.run();});return card;
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
        liveSearch=input;
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
    protected void clearSearch(){if(liveSearch!=null)liveSearch.setText("");host().hideKeyboard();}
    protected boolean feedback(LinearLayout body){
        if(!state().active){ui.add(body,ui.text("Chỉ quản trị viên đang hoạt động được truy cập.",16,Ui.INK,true));return false;}
        if(!state().error.isEmpty()){
            ui.add(body,ui.note(R.drawable.ic_info,state().error,0xFFFFF0F0,Ui.RED,Ui.RED));ui.space(body,10);
            action(body,"Tải lại dữ liệu",Ui.OUTLINE,this::reload);
        }
        if(!state().notice.isEmpty()){ui.add(body,ui.note(R.drawable.ic_info,state().notice,Ui.GREEN_BG,Ui.GREEN,Ui.INK));ui.space(body,12);}
        if(state().busy){ui.add(body,ui.text("Đang cập nhật…",14,Ui.BLUE,false));ui.space(body,10);}
        return true;
    }
    protected boolean feedback(StableViews rows){
        rows.add("admin-feedback",java.util.Arrays.asList(state().active,state().error,state().notice,state().busy),()->{
            LinearLayout box=ui.column();feedback(box);return box;
        },0);
        return state().active;
    }
    protected void action(LinearLayout body,String label,int style,Runnable run){ui.addAction(body,ui.action(label,0,state().busy?Ui.DISABLED:style,state().busy?null:run),48);ui.space(body,10);}
    protected void card(LinearLayout body,String title,String value,String detail,Runnable click){
        LinearLayout card=ui.card();ui.add(card,ui.text(title,15,Ui.MUTED,false));ui.space(card,7);
        ui.add(card,ui.text(value,20,Ui.INK,true));
        if(!detail.isEmpty()){ui.space(card,7);ui.add(card,ui.text(detail,13,Ui.MUTED,false));}
        if(click!=null)ui.clickable(card,click);ui.add(body,card);ui.space(body,12);
    }
    protected void user(String uid){state().notice="";state().userOrigin=host().currentScreen();state().selectedUser=uid;host().show("admin_user");}
    protected void income(String uid,String tutorName){
        String origin=host().currentScreen();if("admin_user".equals(origin)&&"admin_tutor_income".equals(state().userOrigin))origin=state().incomeOrigin;
        state().selectedIncomeTutor=uid;state().incomeTutorName=tutorName;state().incomeOrigin=origin;open("admin_tutor_income");
    }
    protected void contract(String id){state().contractOrigin=host().currentScreen();state().selectContract(id);host().show("admin_contract");}
    protected void agreement(String uid){state().agreementOrigin=host().currentScreen();state().selectAgreement(uid);open("admin_agreement");}
    protected PaymentState.Row person(String uid){PaymentState.Row row=state().users.get(uid);return row==null?state().tutorDirectory().get(uid):row;}
    protected String name(String uid,String fallback){PaymentState.Row user=person(uid);String value=user==null?fallback:user.text("ho_ten");return value==null||value.isEmpty()?"Gia sư":value;}
    protected View metric(String title,long value,int color){
        LinearLayout box=ui.column();ui.add(box,ui.text(title,12,Ui.MUTED,false));ui.space(box,6);
        android.widget.TextView text=ui.text(PaymentState.money(value),17,color,true);text.setMaxLines(1);text.setAutoSizeTextTypeUniformWithConfiguration(11,17,1,android.util.TypedValue.COMPLEX_UNIT_SP);ui.add(box,text);return box;
    }
    protected void detailField(LinearLayout card,String label,String value){
        if(card.getChildCount()>0){ui.space(card,12);ui.line(card);ui.space(card,12);}
        ui.add(card,ui.text(label,12,Ui.MUTED,false));ui.space(card,5);
        android.widget.TextView text=ui.text(value.isEmpty()?"Chưa cập nhật":value,15,Ui.INK,false);text.setTextIsSelectable(true);ui.add(card,text);
    }
    protected View incomeCard(TutorIncomeReport report){
        String tutorName=name(report.tutorId,report.name);PaymentState.Row person=person(report.tutorId);
        LinearLayout card=ui.card(),top=ui.row(),copy=ui.column();top.addView(ui.avatar(report.tutorId,tutorName,44));ui.gap(top,12);
        ui.add(copy,ui.text(tutorName,16,Ui.INK,true));ui.space(copy,5);ui.add(copy,ui.text(money().incomeLoaded?report.sessions+" buổi · "+PaymentState.money(report.total):money().incomeError().isEmpty()?"Đang tải thu nhập…":"Chưa tải được thu nhập",14,Ui.BLUE,true));ui.weight(top,copy);
        top.addView(ui.icon(R.drawable.ic_right,18,Ui.BLUE));ui.add(card,top);ui.space(card,12);
        if(money().incomeLoaded){LinearLayout stats=ui.row();ui.weight(stats,metric("Đã trả",report.paid,Ui.GREEN));ui.gap(stats,12);ui.weight(stats,metric("Chưa trả",report.waiting,Ui.ORANGE));ui.add(card,stats);}
        if(person!=null&&"BANNED".equals(person.text("trang_thai"))){ui.space(card,10);ui.add(card,ui.pill("Tạm dừng chi trả",11,Ui.RED,0xFFFFE9EC));}
        card.setContentDescription("Xem thu nhập của "+tutorName);ui.clickable(card,()->{money().incomePeriod=report.period;income(report.tutorId,tutorName);});return card;
    }
    protected void incomeFeedback(StableViews rows){
        String error=money().incomeError();if(error.isEmpty())return;
        rows.add("income-error",error,()->{
            LinearLayout box=ui.column();ui.add(box,ui.note(R.drawable.ic_info,error,0xFFFFF0F0,Ui.RED,Ui.RED));ui.space(box,10);
            ui.addAction(box,ui.action("Tải lại thu nhập",0,Ui.OUTLINE,this::reload),46);return box;
        },ui.dp(14));
    }
    protected void contractCard(LinearLayout body,PaymentState.Row row){
        card(body,row.text("code").isEmpty()?"Hợp đồng":row.text("code"),row.text("subject"),
                row.text("tutor_name")+" ↔ "+row.text("student_name")+"\n"+com.example.tmdt.AdminState.contractStatus(row),()->contract(row.id));
    }
    protected void ban(PaymentState.Row user){
        boolean ban=!"BANNED".equals(user.text("trang_thai"));
        EditText input=ui.entry("Lý do (5–500 ký tự)",true);input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(500)});
        LinearLayout content=ui.column();ui.pad(content,22,8);
        ui.add(content,input);
        androidx.appcompat.app.AlertDialog dialog=ui.dialog().setTitle((ban?"Khóa ":"Mở khóa ")+user.text("ho_ten")).setView(content).setNegativeButton("Đóng",null).setPositiveButton("Xác nhận",null).create();
        dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{String reason=input.getText().toString().trim();if(reason.length()<5){input.setError("Nhập ít nhất 5 ký tự");return;}state().status(user.id,ban,reason);dialog.dismiss();}));dialog.show();
    }
}
