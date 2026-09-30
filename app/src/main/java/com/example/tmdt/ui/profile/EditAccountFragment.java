package com.example.tmdt.ui.profile;

import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.view.View;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import com.example.tmdt.AccountValidation;
import com.example.tmdt.R;
import com.example.tmdt.UserProfile;
import com.example.tmdt.databinding.FragmentEditAccountBinding;
import com.example.tmdt.ui.common.AccountFragmentBase;
import com.example.tmdt.ui.common.AvatarRenderer;

public final class EditAccountFragment extends AccountFragmentBase {
    private FragmentEditAccountBinding binding;
    private boolean populated;
    private final ActivityResultLauncher<String> picker=registerForActivityResult(new ActivityResultContracts.GetContent(),uri->{
        if(uri==null||state==null||!state.signedIn())return;
        try {
            Bitmap bitmap=ImageDecoder.decodeBitmap(ImageDecoder.createSource(requireContext().getContentResolver(),uri),(decoder,info,source)->{
                float ratio=Math.min(1f,256f/Math.max(info.getSize().getWidth(),info.getSize().getHeight()));
                decoder.setTargetSize(Math.max(1,Math.round(info.getSize().getWidth()*ratio)),Math.max(1,Math.round(info.getSize().getHeight()*ratio)));
                decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
            });
            java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG,80,bytes);bitmap.recycle();
            if(bytes.size()>65000)throw new java.io.IOException("Ảnh quá lớn");
            state.draft("edit.avatar",android.util.Base64.encodeToString(bytes.toByteArray(),android.util.Base64.NO_WRAP),true);
        } catch(java.io.IOException|SecurityException|IllegalArgumentException e) {
            new AlertDialog.Builder(requireContext()).setTitle("Không đọc được ảnh").setMessage("Vui lòng chọn ảnh hợp lệ khác.").setPositiveButton(R.string.understood,null).show();
        }
    });
    public EditAccountFragment(){super(R.layout.fragment_edit_account);}
    @Override protected void bindFields(View view){
        binding=FragmentEditAccountBinding.bind(view);populated=false;
        binding.goal.setSingleLine(false);
        binding.email.setKeyListener(null);
        binding.changeAvatar.setOnClickListener(v->picker.launch("image/*"));
        binding.camera.setOnClickListener(v->picker.launch("image/*"));
        binding.level.setOnClickListener(v->new AlertDialog.Builder(requireContext()).setTitle(R.string.level)
                .setItems(R.array.study_levels,(dialog,index)->binding.level.setText(getResources().getStringArray(R.array.study_levels)[index])).show());
        binding.levelLayout.setEndIconOnClickListener(v->binding.level.performClick());
        binding.regionLayout.setEndIconOnClickListener(v->new AlertDialog.Builder(requireContext()).setTitle(R.string.region)
                .setItems(R.array.suggested_regions,(dialog,index)->binding.region.setText(getResources().getStringArray(R.array.suggested_regions)[index])).show());
        binding.save.setOnClickListener(v->save());
    }
    @Override protected void renderState(){
        UserProfile profile=state.profile();
        binding.form.setVisibility(profile==null?View.GONE:View.VISIBLE);
        if(profile==null)return;
        if(!populated){
            draft(binding.name,"edit.name",profile.name);draft(binding.phone,"edit.phone",profile.phone);
            draft(binding.address,"edit.address",profile.address);draft(binding.level,"edit.level",profile.level);
            draft(binding.region,"edit.region",profile.region);draft(binding.goal,"edit.goal",profile.goal);
            binding.email.setText(profile.email);populated=true;
        }
        AvatarRenderer.render(binding.avatar,state.draft("edit.avatar",profile.avatar),state.draft("edit.name",profile.name));
    }
    private void save(){
        if(state.profile()==null||!name(binding.name)||!phone(binding.phone))return;
        UserProfile value=new UserProfile();
        value.name=text(binding.name);value.email=state.profile().email;
        value.phone=AccountValidation.phone(text(binding.phone));value.address=text(binding.address);
        value.level=text(binding.level);value.region=text(binding.region);value.goal=text(binding.goal);
        value.avatar=state.draft("edit.avatar",state.profile().avatar);
        host().hideKeyboard();state.saveProfile(value);
    }
    @Override public void onDestroyView(){super.onDestroyView();binding=null;populated=false;}
}
