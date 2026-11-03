package com.example.tmdt.ui.admin;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.EditText;
import com.example.tmdt.*;
import com.example.tmdt.ui.common.Ui;
import java.util.*;

public final class AdminIncomeFragment extends AdminScreen {
    @Override protected View build(){
        LinearLayout root=root("Thu nhập gia sư"),body=ui.page(root);if(!feedback(body))return root;
        AdminMonthPicker.add(ui,body,money().incomePeriod,value->{money().incomePeriod=value;refresh();});
        Map<String,long[]> totals=new LinkedHashMap<>();Map<String,String> names=new HashMap<>();
        for(PaymentState.Row e:money().earnings.values())if(money().incomePeriod.equals(e.text("period"))){long[] v=totals.computeIfAbsent(e.text("tutor_id"),id->new long[3]);v[0]+=e.number("amount");v[1]++;names.put(e.text("tutor_id"),e.text("tutor_name"));}
        for(PaymentState.Row p:money().payouts.values())if(money().incomePeriod.equals(p.text("period"))&&"PAID".equals(p.text("status")))totals.computeIfAbsent(p.text("tutor_id"),id->new long[3])[2]+=p.number("amount");
        if(totals.isEmpty())ui.add(body,ui.text("Chưa có thu nhập trong tháng.",15,Ui.MUTED,false));
        for(Map.Entry<String,long[]> e:totals.entrySet()){
            long[] n=e.getValue();PaymentState.Row u=state().users.get(e.getKey());
            card(body,name(e.getKey(),names.getOrDefault(e.getKey(),e.getKey())),PaymentState.money(n[0]),n[1]+" buổi đã xác nhận\nĐã trả: "+PaymentState.money(n[2])+" · Chưa trả: "+PaymentState.money(Math.max(0,n[0]-n[2]))+(u!=null&&"BANNED".equals(u.text("trang_thai"))?"\nĐã khóa · Tạm dừng chi trả":""),u==null?null:()->user(e.getKey()));
        }
        action(body,"Đến bảng lương & chi trả",Ui.PRIMARY,()->{money().payrollPeriod=money().incomePeriod;host().show("admin_payroll");});
        if(!money().error.isEmpty())ui.add(body,ui.text(money().error,14,Ui.RED,false));return root;
    }
}
