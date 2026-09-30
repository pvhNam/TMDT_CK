package com.example.tmdt.ui.auth;

import android.view.View;
import androidx.appcompat.app.AlertDialog;
import com.example.tmdt.AccountValidation;
import com.example.tmdt.UserProfile;
import com.example.tmdt.R;
import com.example.tmdt.databinding.FragmentRegisterBinding;
import com.example.tmdt.ui.common.AccountFragmentBase;

public final class RegisterFragment extends AccountFragmentBase {
    private FragmentRegisterBinding binding;
    public RegisterFragment(){super(R.layout.fragment_register);}
    @Override protected void bindFields(View view){
        binding=FragmentRegisterBinding.bind(view);
        draft(binding.name,"register.name","");draft(binding.email,"register.email","");draft(binding.phone,"register.phone","");
        password(binding.password,binding.showPassword,"register.password");
        password(binding.confirm,binding.showConfirm,"register.confirm");
        binding.terms.setChecked(state.acceptedTerms());
        binding.terms.setOnCheckedChangeListener((button,checked)->{state.acceptTerms(checked);binding.terms.setError(null);});
        binding.termsInfo.setOnClickListener(v->new AlertDialog.Builder(requireContext())
                .setTitle(R.string.terms_link)
                .setMessage("Bạn cần cung cấp thông tin chính xác và bảo mật tài khoản. Email, họ tên, điện thoại và hồ sơ được lưu để cung cấp dịch vụ. Không chia sẻ mật khẩu hoặc mã OTP.")
                .setPositiveButton(R.string.understood,null).show());
        binding.register.setOnClickListener(v->register());
        binding.signIn.setOnClickListener(v->{
            state.removeDraft("register.password");state.removeDraft("register.confirm");go("login");
        });
    }
    private void register(){
        if(!name(binding.name)||!email(binding.email)||!phone(binding.phone)
                ||!check(binding.password,AccountValidation.validPassword(binding.password.getText().toString()),R.string.error_password)
                ||!check(binding.confirm,binding.confirm.getText().toString().equals(binding.password.getText().toString()),R.string.error_confirm))return;
        if(!binding.terms.isChecked()){binding.terms.setError(getString(R.string.error_terms));binding.terms.requestFocus();return;}
        UserProfile profile=new UserProfile();
        profile.name=text(binding.name);profile.email=AccountValidation.email(text(binding.email));
        profile.phone=AccountValidation.phone(text(binding.phone));
        host().hideKeyboard();state.register(profile,binding.password.getText().toString());
    }
    @Override public void onDestroyView(){super.onDestroyView();binding=null;}
}

