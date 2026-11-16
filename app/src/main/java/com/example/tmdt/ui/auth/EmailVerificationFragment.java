package com.example.tmdt.ui.auth;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import com.example.tmdt.R;
import com.example.tmdt.databinding.FragmentEmailVerificationBinding;
import com.example.tmdt.ui.common.AccountFragmentBase;
import com.example.tmdt.ui.common.Motion;

/** Firebase's hosted email verification page works without a separate app server. */
public final class EmailVerificationFragment extends AccountFragmentBase {
    private FragmentEmailVerificationBinding binding;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable ticker=new Runnable(){public void run(){renderState();if(binding!=null)handler.postDelayed(this,1000);}};
    public EmailVerificationFragment(){super(R.layout.fragment_email_verification);}
    @Override protected void bindFields(View view){
        binding=FragmentEmailVerificationBinding.bind(view);
        binding.toolbar.setNavigationOnClickListener(v->state.leaveEmailVerification());
        binding.email.setText(state.verificationEmail());
        binding.openEmail.setOnClickListener(v->{
            Intent intent=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_EMAIL);
            try{startActivity(Intent.createChooser(intent,getString(R.string.email_verification_open)));}
            catch(ActivityNotFoundException e){state.reportError(getString(R.string.email_verification_no_mail_app));}
        });
        binding.verify.setOnClickListener(v->state.checkEmailVerification());
        binding.resend.setOnClickListener(v->state.sendVerificationEmail());
        binding.changeAccount.setOnClickListener(v->state.leaveEmailVerification());
        Motion.press(binding.openEmail);Motion.press(binding.verify);Motion.press(binding.resend);Motion.press(binding.changeAccount);
    }
    @Override protected void renderState(){
        if(binding==null)return;
        long remaining=Math.max(0,(state.emailResendAt()-SystemClock.elapsedRealtime()+999)/1000);
        binding.resend.setEnabled(!state.isBusy()&&remaining==0);
        binding.resend.setText(remaining>0?getString(R.string.email_verification_resend_wait,remaining):getString(R.string.email_verification_resend));
        binding.verify.setText(state.isBusy()?R.string.email_verification_processing:R.string.email_verification_confirm);
    }
    @Override public void onResume(){super.onResume();handler.removeCallbacks(ticker);handler.post(ticker);}
    @Override public void onPause(){handler.removeCallbacks(ticker);super.onPause();}
    @Override public void onDestroyView(){handler.removeCallbacks(ticker);super.onDestroyView();binding=null;}
}
