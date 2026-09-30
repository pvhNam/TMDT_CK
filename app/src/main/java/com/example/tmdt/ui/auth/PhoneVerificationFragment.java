package com.example.tmdt.ui.auth;

import android.os.SystemClock;
import android.view.View;
import com.example.tmdt.AccountValidation;
import com.example.tmdt.R;
import com.example.tmdt.databinding.FragmentPhoneVerificationBinding;
import com.example.tmdt.ui.common.AccountFragmentBase;

public final class PhoneVerificationFragment extends AccountFragmentBase {
    private FragmentPhoneVerificationBinding binding;
    private final Runnable timer=new Runnable(){public void run(){
        if(binding==null)return;renderTimer();binding.timer.postDelayed(this,1000);
    }};
    public PhoneVerificationFragment(){super(R.layout.fragment_phone_verification);}
    @Override protected void bindFields(View view){
        binding=FragmentPhoneVerificationBinding.bind(view);
        String phone=state.verificationPhone().isEmpty()?(state.profile()==null?"":state.profile().phone):state.verificationPhone();
        draft(binding.phone,"otp.phone",phone);draft(binding.code,"otp.code","");
        binding.send.setOnClickListener(v->{
            if(phone(binding.phone))state.sendCode(requireActivity(),AccountValidation.phone(text(binding.phone)),state.codeSent());
        });
        binding.confirm.setOnClickListener(v->{
            if(check(binding.code,text(binding.code).matches("[0-9]{6}"),R.string.error_otp)){
                host().hideKeyboard();state.verifyCode(text(binding.code));
            }
        });
        binding.changePhone.setOnClickListener(v->{if(!state.isSendingCode()){state.changePhone();binding.code.setText("");}});
        binding.signIn.setOnClickListener(v->state.signOut());
        binding.timer.post(timer);
    }
    @Override protected void renderState(){renderTimer();}
    private void renderTimer(){
        if(binding==null)return;
        long remaining=Math.max(0,(state.resendAt()-SystemClock.elapsedRealtime()+999)/1000);
        binding.phone.setEnabled(!state.isBusy()&&!state.isSendingCode()&&!state.codeSent());
        binding.send.setEnabled(!state.isBusy()&&!state.isSendingCode()&&remaining==0);
        binding.send.setText(state.codeSent()?R.string.resend_otp:R.string.send_otp);
        binding.timer.setText(state.isSendingCode()?getString(R.string.sending_otp):remaining>0?getString(R.string.resend_seconds,remaining):"");
        binding.changePhone.setEnabled(!state.isBusy()&&!state.isSendingCode());
    }
    @Override public void onDestroyView(){
        binding.timer.removeCallbacks(timer);super.onDestroyView();binding=null;
    }
}

