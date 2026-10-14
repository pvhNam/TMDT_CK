package com.example.tmdt.ui.profile;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.example.tmdt.R;
import com.example.tmdt.UserProfile;
import com.example.tmdt.databinding.FragmentSettingsBinding;
import com.example.tmdt.ui.common.AccountFragmentBase;

/** "Cài đặt": notification switches (saved on the device), display, account and support entries. */
public final class SettingsFragment extends AccountFragmentBase {
    private FragmentSettingsBinding binding;
    public SettingsFragment(){super(R.layout.fragment_settings);}
    @Override protected void bindFields(View view){
        binding=FragmentSettingsBinding.bind(view);
        SharedPreferences prefs=requireContext().getSharedPreferences("app_settings",Context.MODE_PRIVATE);
        binding.reminderSwitch.setChecked(prefs.getBoolean("lesson_reminder",true));
        binding.messageSwitch.setChecked(prefs.getBoolean("new_messages",true));
        binding.reminderSwitch.setOnCheckedChangeListener((b,checked)->prefs.edit().putBoolean("lesson_reminder",checked).apply());
        binding.messageSwitch.setOnCheckedChangeListener((b,checked)->prefs.edit().putBoolean("new_messages",checked).apply());
        binding.rowReminder.setOnClickListener(v->binding.reminderSwitch.toggle());
        binding.rowMessages.setOnClickListener(v->binding.messageSwitch.toggle());
        binding.rowLanguage.setOnClickListener(v->Toast.makeText(requireContext(),"Hiện ứng dụng chỉ hỗ trợ Tiếng Việt",Toast.LENGTH_SHORT).show());
        binding.rowTheme.setOnClickListener(v->Toast.makeText(requireContext(),"Hiện ứng dụng chỉ có giao diện Sáng",Toast.LENGTH_SHORT).show());
        binding.rowPassword.setOnClickListener(v->changePassword());
        binding.rowPrivacy.setOnClickListener(v->info("Quyền riêng tư",
                "Họ tên, email và số điện thoại của bạn chỉ được dùng để tạo tài khoản, đặt lịch học và để gia sư liên hệ với bạn về buổi học. Bạn có thể cập nhật thông tin bất cứ lúc nào trong mục Chỉnh sửa hồ sơ."));
        binding.rowHelp.setOnClickListener(v->info("Trung tâm trợ giúp",
                "Nếu cần hỗ trợ về lịch học, gia sư hoặc tài khoản, hãy nhắn tin trong mục Tin nhắn. Để lấy lại mật khẩu, chọn Đổi mật khẩu trong Cài đặt."));
        binding.rowTerms.setOnClickListener(v->info("Điều khoản sử dụng",
                "Bằng việc sử dụng ứng dụng, bạn đồng ý cung cấp thông tin chính xác, tôn trọng gia sư và học viên khác, và tuân thủ lịch học đã đặt. Lịch học có thể được đổi hoặc hủy theo chính sách hiển thị trong từng buổi học."));
        binding.signOut.setOnClickListener(v->new AlertDialog.Builder(requireContext())
                .setTitle(R.string.sign_out).setMessage("Bạn có thể đăng nhập lại để xem thông tin cá nhân.")
                .setNegativeButton(R.string.cancel,null).setPositiveButton(R.string.sign_out,(dialog,which)->state.signOut()).show());
    }
    private void changePassword(){
        UserProfile profile=state.profile();
        if(profile==null||profile.email.isEmpty()){Toast.makeText(requireContext(),"Chưa tải được thông tin tài khoản",Toast.LENGTH_SHORT).show();return;}
        new AlertDialog.Builder(requireContext()).setTitle("Đổi mật khẩu")
                .setMessage("Chúng tôi sẽ gửi liên kết đặt lại mật khẩu tới "+profile.email+".")
                .setNegativeButton(R.string.cancel,null)
                .setPositiveButton("Gửi liên kết",(dialog,which)->{state.resetPassword(profile.email);binding.accountScroll.smoothScrollTo(0,0);}).show();
    }
    private void info(String title,String message){
        new AlertDialog.Builder(requireContext()).setTitle(title).setMessage(message).setPositiveButton(R.string.understood,null).show();
    }
    @Override public void onDestroyView(){super.onDestroyView();binding=null;}
}
