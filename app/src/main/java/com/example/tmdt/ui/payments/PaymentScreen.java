package com.example.tmdt.ui.payments;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.tmdt.MainActivity;
import com.example.tmdt.PaymentState;
import com.example.tmdt.R;
import com.example.tmdt.ui.common.ScreenFragment;
import com.example.tmdt.ui.common.Ui;
import java.util.*;

abstract class PaymentScreen extends ScreenFragment {
    protected PaymentState state(){return ((MainActivity)requireActivity()).payments;}
    protected MainActivity host(){return (MainActivity)requireActivity();}
    @Override public void onViewCreated(@NonNull View view,@Nullable Bundle saved){super.onViewCreated(view,saved);state().changes.observe(getViewLifecycleOwner(),ignored->refresh());}
    protected LinearLayout root(String title){LinearLayout root=ui.column();ui.header(root,title,host()::back,null);return root;}
    protected void feedback(LinearLayout body){
        if(state().sandbox()){ui.add(body,ui.note(R.drawable.ic_info,"VNPay TEST · Dữ liệu học phí và chi trả thử. Không chuyển tiền thật.",Ui.PALE,Ui.BLUE,Ui.INK));ui.space(body,12);}
        if(!state().error.isEmpty()){ui.add(body,ui.note(R.drawable.ic_info,state().error,0xFFFFF0F0,Ui.RED,Ui.RED));ui.space(body,12);}
        if(!state().notice.isEmpty()){ui.add(body,ui.note(R.drawable.ic_info,state().notice,Ui.GREEN_BG,Ui.GREEN,Ui.INK));ui.space(body,12);}
        if(state().busy){ui.add(body,ui.text("Đang xử lý…",14,Ui.BLUE,false));ui.space(body,10);}
    }
    protected void action(LinearLayout body,String label,int style,Runnable run){View button=ui.action(label,0,state().busy?Ui.DISABLED:style,state().busy?null:run);ui.addAction(body,button,48);ui.space(body,10);}
    protected void prompt(String title,String hint,java.util.function.Consumer<String> submit){
        android.widget.EditText input=ui.input(hint);input.setSingleLine(false);input.setMaxLines(4);
        input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(500)});
        LinearLayout content=ui.column();ui.pad(content,20,8);ui.add(content,input);
        ui.dialog().setTitle(title).setView(content).setNegativeButton("Đóng",null).setPositiveButton("Xác nhận",(d,w)->submit.accept(input.getText().toString().trim())).show();
    }
    protected void summary(LinearLayout body,String heading,String detail,String amount){
        LinearLayout card=ui.card();ui.add(card,ui.text(heading,17,Ui.INK,true));ui.space(card,6);ui.add(card,ui.text(detail,14,Ui.MUTED,false));
        if(!amount.isEmpty()){ui.space(card,12);ui.add(card,ui.text(amount,25,Ui.BLUE,true));}ui.add(body,card);ui.space(body,12);
    }
    protected List<PaymentState.Row> newest(Collection<PaymentState.Row> rows){List<PaymentState.Row> result=new ArrayList<>(rows);result.sort((a,b)->Long.compare(b.time("created_at"),a.time("created_at")));return result;}
    protected String date(long value){return value==0?"Đang cập nhật":java.time.Instant.ofEpochMilli(value).atZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));}
}
