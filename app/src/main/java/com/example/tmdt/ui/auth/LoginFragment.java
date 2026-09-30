package com.example.tmdt.ui.auth;

import android.view.View;
import com.example.tmdt.AccountValidation;
import com.example.tmdt.R;
import com.example.tmdt.databinding.FragmentLoginBinding;
import com.example.tmdt.ui.common.AccountFragmentBase;

public final class LoginFragment extends AccountFragmentBase {
    private FragmentLoginBinding binding;
    public LoginFragment() { super(R.layout.fragment_login); }
    @Override protected void bindFields(View view) {
        binding=FragmentLoginBinding.bind(view);
        draft(binding.email,"login.email","");
        draft(binding.password,"login.password","");
        binding.signIn.setOnClickListener(v->{
            if(email(binding.email) && check(binding.password,!binding.password.getText().toString().isEmpty(),R.string.error_password_empty)){
                host().hideKeyboard();
                state.signIn(AccountValidation.email(text(binding.email)),binding.password.getText().toString());
            }
        });
        binding.register.setOnClickListener(v->go("register"));
        binding.forgot.setOnClickListener(v->{
            state.draft("forgot.email",text(binding.email),false);go("forgot");
        });
    }
    @Override public void onDestroyView(){super.onDestroyView();binding=null;}
}
