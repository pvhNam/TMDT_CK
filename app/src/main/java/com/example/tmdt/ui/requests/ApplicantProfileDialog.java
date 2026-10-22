package com.example.tmdt.ui.requests;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.example.tmdt.CatalogState;
import com.example.tmdt.R;
import com.example.tmdt.TutorApplication;
import com.example.tmdt.TutorApplicantProfileState;
import com.example.tmdt.TutorRequestManager;
import com.example.tmdt.TutorRequestRecord;
import com.example.tmdt.databinding.DialogApplicantProfileBinding;

/** Owner-only application information plus the tutor's verified public profile. */
public final class ApplicantProfileDialog extends DialogFragment {
    private DialogApplicantProfileBinding binding;
    private TutorRequestManager manager;
    private TutorApplicantProfileState profiles;
    private CatalogState catalog;
    private String requestId, tutorId;
    public ApplicantProfileDialog(){super(R.layout.dialog_applicant_profile);}
    public static ApplicantProfileDialog newInstance(String requestId,String tutorId){
        ApplicantProfileDialog dialog=new ApplicantProfileDialog();Bundle args=new Bundle();
        args.putString("request_id",requestId);args.putString("tutor_id",tutorId);dialog.setArguments(args);return dialog;
    }
    @Override public void onCreate(@Nullable Bundle saved){super.onCreate(saved);setStyle(STYLE_NO_TITLE,0);}
    @Override public void onViewCreated(@NonNull View view,@Nullable Bundle saved){
        binding=DialogApplicantProfileBinding.bind(view);
        requestId=requireArguments().getString("request_id","");tutorId=requireArguments().getString("tutor_id","");
        ViewModelProvider provider=new ViewModelProvider(requireActivity());
        manager=provider.get(TutorRequestManager.class);profiles=provider.get(TutorApplicantProfileState.class);catalog=provider.get(CatalogState.class);
        manager.watchApplicants(requestId);profiles.watch(tutorId);
        binding.closeApplicantProfile.setOnClickListener(v->dismiss());
        binding.retryApplicantProfile.setOnClickListener(v->{profiles.retry();manager.retry();manager.watchApplicants(requestId);});
        binding.chooseApplicant.setOnClickListener(v->new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.applicant_choose_title).setMessage(R.string.applicant_choose_caption)
                .setNegativeButton(R.string.applicant_cancel,null)
                .setPositiveButton(R.string.applicant_choose,(dialog,which)->manager.selectApplicant(requestId,tutorId)).show());
        manager.changes().observe(getViewLifecycleOwner(),ignored->render());
        profiles.changes().observe(getViewLifecycleOwner(),ignored->render());
        catalog.changes().observe(getViewLifecycleOwner(),ignored->render());
    }
    @Override public void onStart(){super.onStart();Window window=requireDialog().getWindow();if(window!=null){
        int margin=Math.round(32*getResources().getDisplayMetrics().density);
        window.setLayout(getResources().getDisplayMetrics().widthPixels-margin,Math.round(getResources().getDisplayMetrics().heightPixels*0.85f));
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));}}
    private void render(){
        if(binding==null)return;
        TutorRequestRecord request=manager.request(requestId);TutorApplication application=manager.applicant(tutorId);
        boolean accessible=request!=null&&application!=null;
        TutorApplicantProfileState.Profile profile=accessible?profiles.profile():null;
        boolean selected=accessible&&tutorId.equals(request.selectedTutorId);
        boolean other=accessible&&!request.selectedTutorId.isEmpty()&&!selected;
        binding.applicantProfileStatus.setText(selected?getString(R.string.application_selected):other?getString(R.string.application_other_selected)
                :accessible?getString(R.string.requests_application_pending):getString(R.string.applicant_profile_unavailable));
        binding.applicantProfileName.setText(profile==null?"":profile.name);
        binding.applicantProfileMessage.setText(accessible?application.introduction:"");
        binding.applicantProfileInformation.setText(profile==null?"":profile.education+" · "+profile.school+"\n"+profile.major
                +"\nKinh nghiệm: "+profile.experience+" năm\n"+profile.region+" · "+CatalogState.mode(profile.mode)+"\n\n"+profile.introduction);
        StringBuilder offerings=new StringBuilder();
        if(profile!=null)for(TutorApplicantProfileState.Offering offering:profiles.offerings()){
            String subject=catalog.subjects().get(offering.subjectId);
            if(offerings.length()>0)offerings.append("\n");
            offerings.append(subject==null?offering.subjectId:subject).append(" · ").append(CatalogState.level(offering.level))
                    .append(" · ").append(CatalogState.money(offering.price)).append("/buổi");
        }
        binding.applicantProfileOfferings.setText(!accessible?"":!profiles.offeringError().isEmpty()?profiles.offeringError()
                :offerings.length()==0?getString(R.string.applicant_no_offerings):offerings.toString());
        String error=!manager.error().isEmpty()?manager.error():!manager.applicantsError().isEmpty()?manager.applicantsError():accessible?profiles.error():"";
        binding.applicantProfileError.setText(error);binding.applicantProfileError.setVisibility(error.isEmpty()?View.GONE:View.VISIBLE);
        binding.retryApplicantProfile.setVisibility(!error.isEmpty()||!profiles.offeringError().isEmpty()?View.VISIBLE:View.GONE);
        binding.applicantProfileProgress.setVisibility(manager.busy()||manager.applicantsLoading()||profiles.loading()?View.VISIBLE:View.GONE);
        binding.chooseApplicant.setEnabled(accessible&&profile!=null&&request.selectedTutorId.isEmpty()
                &&("OPEN".equals(request.status)||"CLOSED".equals(request.status))&&"PENDING".equals(application.status)
                &&!manager.busy()&&!manager.offline()&&!manager.applicantsOffline());
        binding.chooseApplicant.setText(selected?R.string.application_selected:R.string.applicant_choose);
        binding.closeApplicantProfile.setEnabled(!manager.busy());setCancelable(!manager.busy());
    }
    @Override public void onDestroyView(){super.onDestroyView();binding=null;}
}
