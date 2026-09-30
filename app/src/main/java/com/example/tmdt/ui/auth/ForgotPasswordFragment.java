package com.example.tmdt.ui.auth;

import android.view.View;
import com.example.tmdt.AccountValidation;
import com.example.tmdt.R;
import com.example.tmdt.databinding.FragmentForgotPasswordBinding;
import com.example.tmdt.ui.common.AccountFragmentBase;

public final class ForgotPasswordFragment extends AccountFragmentBase {
    private FragmentForgotPasswordBinding binding;
    public ForgotPasswordFragment(){super(R.layout.fragment_forgot_password);}
    @Override protected void bindFields(View view){
        binding=FragmentForgotPasswordBinding.bind(view);
        draft(binding.email,"forgot.email","");
        binding.send.setOnClickListener(v->{
            if(email(binding.email)){host().hideKeyboard();state.resetPassword(AccountValidation.email(text(binding.email)));}
        });
        binding.signIn.setOnClickListener(v->go("login"));
    }
    @Override public void onDestroyView(){super.onDestroyView();binding=null;}
}

