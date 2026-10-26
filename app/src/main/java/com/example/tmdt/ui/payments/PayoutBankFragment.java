package com.example.tmdt.ui.payments;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import com.example.tmdt.MainActivity;
import com.example.tmdt.PaymentState;
import com.example.tmdt.ui.common.Ui;

/** A stable form: incoming snapshots must not replace text while the user is typing. */
public final class PayoutBankFragment extends Fragment {
    private EditText bank,number,holder;private TextView feedback;private View save;
    @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup container,@Nullable Bundle saved){
        MainActivity host=(MainActivity)requireActivity();PaymentState state=host.payments;Ui ui=new Ui(requireContext());
        LinearLayout root=ui.column();ui.header(root,"Tài khoản nhận tiền",host::back,null);LinearLayout body=ui.page(root);
        ui.add(body,ui.text("Kiểm tra kỹ thông tin trước khi lưu. Tài khoản này được dùng khi admin lập bảng chi trả; các bảng đã lập giữ nguyên thông tin đã chốt.",14,Ui.MUTED,false));ui.space(body,16);
        bank=ui.input("Tên ngân hàng");number=ui.input("Số tài khoản");holder=ui.input("Tên chủ tài khoản");number.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        bank.setText(saved==null?String.valueOf(state.bank.getOrDefault("bank_name","")):saved.getString("bank",""));
        number.setText(saved==null?String.valueOf(state.bank.getOrDefault("account_number","")):saved.getString("number",""));
        holder.setText(saved==null?String.valueOf(state.bank.getOrDefault("account_name","")):saved.getString("holder",""));
        for(EditText field:new EditText[]{bank,number,holder}){ui.add(body,field);ui.space(body,14);}
        feedback=ui.text("",14,Ui.MUTED,false);ui.add(body,feedback);ui.space(body,10);
        save=ui.action("Lưu tài khoản nhận tiền",0,Ui.PRIMARY,()->{host.hideKeyboard();state.saveBank(bank.getText().toString().trim(),number.getText().toString().trim(),holder.getText().toString().trim());});ui.addAction(body,save,48);
        return root;
    }
    @Override public void onViewCreated(@NonNull View view,@Nullable Bundle saved){super.onViewCreated(view,saved);PaymentState state=((MainActivity)requireActivity()).payments;state.changes.observe(getViewLifecycleOwner(),ignored->{save.setEnabled(!state.busy);feedback.setText(state.busy?"Đang lưu…":!state.error.isEmpty()?state.error:state.notice);
        if(bank.getText().length()==0&&number.getText().length()==0&&holder.getText().length()==0&&!state.bank.isEmpty()){
            bank.setText(String.valueOf(state.bank.getOrDefault("bank_name","")));number.setText(String.valueOf(state.bank.getOrDefault("account_number","")));holder.setText(String.valueOf(state.bank.getOrDefault("account_name","")));
        }
    });}
    @Override public void onSaveInstanceState(@NonNull Bundle out){super.onSaveInstanceState(out);if(bank!=null){out.putString("bank",bank.getText().toString());out.putString("number",number.getText().toString());out.putString("holder",holder.getText().toString());}}
    @Override public void onDestroyView(){super.onDestroyView();bank=null;number=null;holder=null;feedback=null;save=null;}
}
