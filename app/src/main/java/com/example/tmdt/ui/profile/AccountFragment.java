package com.example.tmdt.ui.profile;

import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import com.example.tmdt.R;
import com.example.tmdt.UserProfile;
import com.example.tmdt.databinding.FragmentAccountBinding;
import com.example.tmdt.ui.common.AccountFragmentBase;
import com.example.tmdt.ui.common.AvatarRenderer;

public final class AccountFragment extends AccountFragmentBase {
    private FragmentAccountBinding binding;
    public AccountFragment(){super(R.layout.fragment_account);}
    @Override protected void bindFields(View view){
        binding=FragmentAccountBinding.bind(view);
        binding.edit.setOnClickListener(v->{state.clearDraft();go("edit_account");});
        binding.verify.setOnClickListener(v->go("otp"));
        binding.reset.setOnClickListener(v->{if(state.profile()!=null)state.resetPassword(state.profile().email);});
        binding.retry.setOnClickListener(v->state.loadProfile(false));
        binding.signOut.setOnClickListener(v->new AlertDialog.Builder(requireContext())
                .setTitle(R.string.sign_out).setMessage("Bạn có thể đăng nhập lại để xem thông tin cá nhân.")
                .setNegativeButton(R.string.cancel,null).setPositiveButton(R.string.sign_out,(dialog,which)->state.signOut()).show());
    }
    @Override protected void renderState(){
        UserProfile profile=state.profile();
        binding.profileContent.setVisibility(profile==null?View.GONE:View.VISIBLE);
        binding.retry.setVisibility(profile==null&&!state.isBusy()?View.VISIBLE:View.GONE);
        if(profile==null)return;
        AvatarRenderer.render(binding.avatar,profile.avatar,profile.name);
        binding.name.setText(profile.name);
        binding.role.setText("ADMIN".equals(profile.role)?R.string.role_admin:"TUTOR".equals(profile.role)?R.string.role_tutor:R.string.role_student);
        value(binding.email,profile.email);value(binding.phone,profile.phone);value(binding.address,profile.address);
        value(binding.level,profile.level);value(binding.region,profile.region);value(binding.goal,profile.goal);
        binding.verification.setText(state.verified()?getString(R.string.verified_phone,state.verifiedPhone()):getString(R.string.unverified));
        binding.verify.setVisibility(state.verified()?View.GONE:View.VISIBLE);
    }
    private void value(TextView view,String value){view.setText(value.isEmpty()?getString(R.string.not_updated):value);}
    @Override public void onDestroyView(){super.onDestroyView();binding=null;}
}

