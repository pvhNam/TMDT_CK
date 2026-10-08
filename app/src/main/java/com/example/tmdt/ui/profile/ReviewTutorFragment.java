package com.example.tmdt.ui.profile;

import android.view.View;
import com.example.tmdt.R;
import com.example.tmdt.TutorRegistration;
import com.example.tmdt.databinding.FragmentReviewTutorBinding;
import com.example.tmdt.ui.common.AccountFragmentBase;
import com.example.tmdt.ui.common.AvatarRenderer;
import com.example.tmdt.ui.common.TutorDesignAssets;
import java.text.NumberFormat;
import java.util.Locale;

public final class ReviewTutorFragment extends AccountFragmentBase {
    private FragmentReviewTutorBinding binding;
    public ReviewTutorFragment(){super(R.layout.fragment_review_tutor);}
    @Override protected void bindFields(View view){
        binding=FragmentReviewTutorBinding.bind(view);
        binding.toolbar.setNavigationIcon(TutorDesignAssets.icon(requireContext(),"back"));
        binding.steps.stepInfo.setSelected(true);binding.steps.stepReview.setSelected(true);
        binding.steps.stepInfo.setText("✓");
        binding.steps.stepInfo.setTextColor(requireContext().getColor(android.R.color.white));
        binding.steps.stepReview.setTextColor(requireContext().getColor(android.R.color.white));
        binding.steps.labelInfo.setTextColor(requireContext().getColor(R.color.tutor_blue));
        binding.steps.labelReview.setTextColor(requireContext().getColor(R.color.tutor_blue));
        binding.steps.labelReview.setTypeface(binding.steps.labelReview.getTypeface(),android.graphics.Typeface.BOLD);
        binding.reviewNote.setCompoundDrawablesRelativeWithIntrinsicBounds(TutorDesignAssets.icon(requireContext(),"review_info"),null,null,null);
        TutorRegistration value=state.tutorRegistration();
        if(value!=null){
            binding.name.setText(value.name);binding.subjects.setText(value.subjectName+" · "+value.levelLabel());
            binding.mode.setText(value.modeLabel());binding.phone.setText(value.phone);
            binding.experience.setText(getString(R.string.tutor_experience_value,value.experience));
            binding.price.setText(getString(R.string.tutor_price_value,NumberFormat.getIntegerInstance(new Locale("vi","VN")).format(value.price)));
            AvatarRenderer.render(binding.avatar,state.profile()==null?"":state.profile().avatar,value.name);
            binding.avatar.avatarInitials.setBackgroundResource(R.drawable.bg_tutor_surface);
            binding.avatar.avatarPhoto.setBackgroundResource(R.drawable.bg_tutor_surface);
        }
        binding.confirm.setChecked(Boolean.parseBoolean(state.draft("tutor.confirmed","false")));
        binding.confirm.setOnCheckedChangeListener((button,checked)->state.draft("tutor.confirmed",String.valueOf(checked),false));
        binding.editAgain.setOnClickListener(v->go("register_tutor"));
        binding.submit.setOnClickListener(v->{
            if(!binding.confirm.isChecked()){state.reportError(getString(R.string.tutor_confirm_error));binding.accountScroll.smoothScrollTo(0,0);return;}
            state.submitTutor();
        });
    }
    @Override public void onDestroyView(){super.onDestroyView();binding=null;}
}
