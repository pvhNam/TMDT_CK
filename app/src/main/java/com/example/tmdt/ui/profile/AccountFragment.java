package com.example.tmdt.ui.profile;

import android.view.View;
import android.widget.Toast;
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
        roundAvatar(binding.avatar.avatarInitials);roundAvatar(binding.avatar.avatarPhoto);
        binding.rowInfo.setOnClickListener(v->{
            boolean open=binding.infoDetails.getVisibility()!=View.VISIBLE;
            com.example.tmdt.ui.common.Motion.change((android.view.ViewGroup)binding.infoDetails.getParent());
            binding.infoDetails.setVisibility(open?View.VISIBLE:View.GONE);
            binding.infoChevron.animate().rotation(open?90f:0f)
                    .setDuration(com.example.tmdt.ui.common.Motion.enabled()?150:0).start();
        });
        View.OnClickListener soon=v->Toast.makeText(requireContext(),"Tính năng sắp ra mắt",Toast.LENGTH_SHORT).show();
        binding.rowFavorites.setOnClickListener(v->go("favorites"));binding.rowSettings.setOnClickListener(v->go("settings"));
        binding.rowRequests.setOnClickListener(v->host().openMyRequests("account"));
        // UC23: Hợp đồng của tôi (kèm số hợp đồng chờ xác nhận), hợp đồng cộng tác (gia sư), mẫu hợp đồng (quản trị viên).
        binding.rowContracts.setOnClickListener(v->host().exchange.openContracts());
        binding.rowTemplates.setOnClickListener(v->host().exchange.openTemplates());
        host().exchange.contracts.changes().observe(getViewLifecycleOwner(),ignored->renderContracts());
        binding.rowPayments.setOnClickListener(v->go("payments"));
        binding.rowReviews.setOnClickListener(v->host().exchange.openMyReviews());
        binding.rowLearners.setOnClickListener(v->host().exchange.openLearners());
        binding.rowReports.setOnClickListener(v->host().exchange.openReports());
        binding.rowReportAdmin.setOnClickListener(v->host().exchange.openReportsAdmin());
        binding.rowStats.setOnClickListener(v->host().exchange.openStats());
        binding.rowClassesAdmin.setOnClickListener(v->host().exchange.openClassesAdmin());
        binding.rowRoles.setOnClickListener(v->host().exchange.openRoles());
        binding.rowTutorManage.setOnClickListener(v->host().exchange.openTutorManage());
        binding.rowNotifications.setOnClickListener(soon);
        binding.edit.setOnClickListener(v->{state.clearDraft();go("edit_account");});
        binding.tutorBooks.setImageDrawable(com.example.tmdt.ui.common.TutorDesignAssets.icon(requireContext(),"books"));
        binding.tutorAction.setOnClickListener(v->{
            com.example.tmdt.TutorReviewState review=new androidx.lifecycle.ViewModelProvider(requireActivity()).get(com.example.tmdt.TutorReviewState.class);
            if(state.profile()!=null&&"ADMIN".equals(state.profile().role))go("admin_dashboard");
            else if(state.profile()!=null&&state.profile().tutor&&"VERIFIED".equals(review.status())){state.clearFeedback();
                host().classroom.setTutorMode(!host().classroom.tutorMode());}
            else if(state.profile()!=null&&state.profile().tutor)go("tutor_pending");
            else go("register_tutor");
        });
        new androidx.lifecycle.ViewModelProvider(requireActivity()).get(com.example.tmdt.TutorReviewState.class).changes.observe(getViewLifecycleOwner(),ignored->renderState());
        binding.verify.setOnClickListener(v->go("otp"));
        binding.reset.setOnClickListener(v->{if(state.profile()!=null)state.resetPassword(state.profile().email);});
        binding.retry.setOnClickListener(v->state.loadProfile(false));
        binding.signOut.setOnClickListener(v->new AlertDialog.Builder(requireContext())
                .setTitle(R.string.sign_out)
                .setNegativeButton(R.string.cancel,null).setPositiveButton(R.string.sign_out,(dialog,which)->state.signOut()).show());
    }
    /** Profile photos use the same circular outline as all shared avatars. */
    private void roundAvatar(View v){
        v.setBackgroundResource(R.drawable.bg_avatar);AvatarRenderer.circle(v);
    }
    @Override protected void renderState(){
        UserProfile profile=state.profile();
        binding.profileContent.setVisibility(profile==null?View.GONE:View.VISIBLE);
        binding.retry.setVisibility(profile==null&&!state.isBusy()?View.VISIBLE:View.GONE);
        if(profile==null)return;
        binding.paymentsLabel.setText("ADMIN".equals(profile.role)?"Quản lý thanh toán & chi trả":profile.tutor?"Ví & thanh toán":"Thanh toán của tôi");
        AvatarRenderer.render(binding.avatar,profile.avatar,profile.name);
        binding.name.setText(profile.name);
        // TODO: replace with real counts (completed lessons / favourite tutors) once Classroom exposes them.
        binding.statLessons.setText("0");binding.statFavorites.setText(String.valueOf(com.example.tmdt.FavoriteTutors.count(requireContext())));
        binding.tutorAction.setText(profile.tutor?(host().classroom.tutorMode()?R.string.tutor_switch_student:R.string.tutor_switch):R.string.tutor_register);
        binding.tutorTitle.setText(profile.tutor?R.string.tutor_manage_title:R.string.tutor_invite);
        binding.tutorCaption.setText(profile.tutor?R.string.tutor_manage_caption:R.string.tutor_invite_caption);
        com.example.tmdt.TutorReviewState review=new androidx.lifecycle.ViewModelProvider(requireActivity()).get(com.example.tmdt.TutorReviewState.class);
        if("ADMIN".equals(profile.role)){binding.tutorAction.setText("Mở trang quản trị");binding.tutorTitle.setText("Quản trị hệ thống");binding.tutorCaption.setText("Tài khoản, duyệt gia sư, hợp đồng và chi trả.");}
        else if(profile.tutor&&!"VERIFIED".equals(review.status())){binding.tutorAction.setText("Theo dõi hồ sơ gia sư");binding.tutorCaption.setText("Xem trạng thái xét duyệt và yêu cầu bổ sung.");}
        binding.role.setText("ADMIN".equals(profile.role)?R.string.role_admin:host().classroom.tutorMode()?R.string.role_tutor:R.string.role_student);
        value(binding.email,profile.email);value(binding.phone,profile.phone);value(binding.address,profile.address);
        value(binding.level,profile.level);value(binding.region,profile.region);value(binding.goal,profile.goal);
        binding.studyDetails.setText((profile.grade.isEmpty()?"":profile.grade)+(profile.age>0?(profile.grade.isEmpty()?"":" · ")+profile.age+" tuổi":""));
        binding.studyDetails.setVisibility(profile.grade.isEmpty()&&profile.age==0?View.GONE:View.VISIBLE);
        binding.verification.setText(state.verified()?getString(R.string.verified_phone,state.verifiedPhone()):getString(R.string.unverified));
        binding.verify.setVisibility(state.verified()?View.GONE:View.VISIBLE);
        int templates="ADMIN".equals(profile.role)?View.VISIBLE:View.GONE;
        binding.rowTemplates.setVisibility(templates);binding.templatesDivider.setVisibility(templates);
        binding.rowReportAdmin.setVisibility(templates);binding.reportAdminDivider.setVisibility(templates);
        binding.rowStats.setVisibility(templates);binding.statsDivider.setVisibility(templates);
        binding.rowClassesAdmin.setVisibility(templates);binding.classesAdminDivider.setVisibility(templates);
        binding.rowRoles.setVisibility(templates);binding.rolesDivider.setVisibility(templates);
        binding.rowTutorManage.setVisibility(templates);binding.tutorManageDivider.setVisibility(templates);
        renderContracts();
    }
    private void renderContracts(){
        if(binding==null)return;
        int waiting=host().exchange.contracts.awaitingMe();
        binding.contractsLabel.setText(waiting>0?getString(R.string.ex_menu_contracts)+" ("+waiting+" chờ xác nhận)":getString(R.string.ex_menu_contracts));
    }
    private void value(TextView view,String value){view.setText(value.isEmpty()?getString(R.string.not_updated):value);}
    @Override public void onDestroyView(){super.onDestroyView();binding=null;}
}
