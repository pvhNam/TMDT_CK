package com.example.tmdt.ui.payments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.tmdt.MainActivity;
import com.example.tmdt.PaymentState;
import com.example.tmdt.TutorWallet;
import com.example.tmdt.ui.common.Ui;

/** Account snapshots update labels, preserving the entered amount and request id. */
public final class WalletWithdrawalFragment extends Fragment {
    private PaymentState state;
    private Ui ui;
    private EditText amount;
    private TextView balance,bank,error;
    private View submit;
    @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup container,@Nullable Bundle saved){
        MainActivity host=(MainActivity)requireActivity();state=host.payments;ui=new Ui(requireContext());
        if(state.withdrawalRequestId.isEmpty())state.withdrawalRequestId=state.owner+"_"+java.util.UUID.randomUUID();
        if(saved!=null){state.withdrawalRequestId=saved.getString("request_id",state.withdrawalRequestId);state.withdrawalDraft=saved.getString("amount",state.withdrawalDraft);}
        if(!state.withdrawalRequestId.startsWith(state.owner+"_")){state.withdrawalRequestId=state.owner+"_"+java.util.UUID.randomUUID();state.withdrawalDraft="";}
        LinearLayout root=ui.column();ui.header(root,"Rút tiền",host::back,null);LinearLayout body=ui.page(root),card=ui.card();
        ui.add(card,ui.text("Số dư khả dụng",13,Ui.MUTED,false));ui.space(card,8);balance=ui.text("",25,Ui.INK,true);ui.add(card,balance);ui.add(body,card);ui.space(body,20);
        ui.label(body,"Số tiền muốn rút");amount=ui.input("Từ 50.000 đ");amount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);amount.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(12)});amount.setText(state.withdrawalDraft);ui.add(body,amount);
        TextView all=ui.link("Rút toàn bộ số dư",13,()->amount.setText(String.valueOf(Math.min(TutorWallet.MAX_WITHDRAWAL,TutorWallet.available(state.wallet())))));ui.add(body,all);ui.space(body,16);
        ui.label(body,"Ngân hàng nhận tiền");bank=ui.text("",14,Ui.INK,true);ui.add(body,bank);ui.space(body,6);
        ui.add(body,ui.link("Thay đổi tài khoản",13,()->{state.bankOrigin="withdraw_funds";host.show("payout_bank");}));ui.space(body,16);
        error=ui.text("",13,Ui.RED,false);error.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);ui.add(body,error);ui.space(body,12);
        submit=ui.action("Rút tiền",0,Ui.PRIMARY,()->submit(host));ui.addAction(body,submit,48);
        amount.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){state.withdrawalDraft=s.toString();}public void afterTextChanged(Editable value){}});
        return root;
    }
    @Override public void onViewCreated(@NonNull View view,@Nullable Bundle saved){super.onViewCreated(view,saved);state.changes.observe(getViewLifecycleOwner(),ignored->render());}
    private void render(){
        balance.setText(state.walletLoaded?PaymentState.money(TutorWallet.available(state.wallet())):"Đang tải số dư…");
        String name=String.valueOf(state.bank.getOrDefault("bank_name","")),number=String.valueOf(state.bank.getOrDefault("account_number","")),holder=String.valueOf(state.bank.getOrDefault("account_name",""));
        bank.setText(number.isEmpty()?"Chưa có tài khoản ngân hàng":name+"\n"+number+"\n"+holder);
        boolean enabled=!state.busy&&state.tutor&&state.walletLoaded&&!number.isEmpty()&&TutorWallet.available(state.wallet())>=TutorWallet.MIN_WITHDRAWAL;
        submit.setEnabled(enabled);submit.setAlpha(enabled?1f:.45f);amount.setEnabled(!state.busy);error.setText(state.busy?"Đang rút tiền…":state.error);
    }
    private void submit(MainActivity host){
        long value;try{value=Long.parseLong(amount.getText().toString().trim());}catch(NumberFormatException failure){amount.setError("Nhập số tiền cần rút.");return;}
        if(value<TutorWallet.MIN_WITHDRAWAL||value>TutorWallet.MAX_WITHDRAWAL){amount.setError("Số tiền từ 50.000 đ đến 100.000.000 đ.");return;}
        if(value>TutorWallet.available(state.wallet())){amount.setError("Số dư khả dụng không đủ.");return;}
        java.util.Map<String,Object> snapshot=new java.util.HashMap<>();for(String key:new String[]{"bank_name","account_number","account_name"})snapshot.put(key,state.bank.getOrDefault(key,""));
        String requestId=state.withdrawalRequestId;
        host.hideKeyboard();ui.dialog().setTitle("Xác nhận rút tiền").setMessage(PaymentState.money(value)+"\n"+bank.getText())
                .setNegativeButton("Đóng",null).setPositiveButton("Rút tiền",(dialog,which)->state.requestWithdrawal(value,requestId,snapshot)).show();
    }
    @Override public void onSaveInstanceState(@NonNull Bundle out){super.onSaveInstanceState(out);out.putString("request_id",state.withdrawalRequestId);out.putString("amount",state.withdrawalDraft);}
    @Override public void onDestroyView(){super.onDestroyView();amount=null;balance=null;bank=null;error=null;submit=null;}
}
