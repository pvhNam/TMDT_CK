package com.example.tmdt.ui.profile;

import android.content.res.ColorStateList;
import android.view.View;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import com.example.tmdt.CatalogState;
import com.example.tmdt.R;
import com.example.tmdt.TutorRegistration;
import com.example.tmdt.UserProfile;
import com.example.tmdt.databinding.FragmentRegisterTutorBinding;
import com.example.tmdt.ui.common.AccountFragmentBase;
import com.example.tmdt.ui.common.TutorDesignAssets;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class RegisterTutorFragment extends AccountFragmentBase {
    private FragmentRegisterTutorBinding binding;
    private CatalogState catalog;
    private String subjectId="",subjectName="";
    private final List<String> levels=new ArrayList<>();
    private boolean online,inPerson;
    public RegisterTutorFragment(){super(R.layout.fragment_register_tutor);}
    @Override protected void bindFields(View view){
        binding=FragmentRegisterTutorBinding.bind(view);
        catalog=new ViewModelProvider(requireActivity()).get(CatalogState.class);
        binding.toolbar.setNavigationIcon(TutorDesignAssets.icon(requireContext(),"back"));
        binding.steps.stepInfo.setSelected(true);binding.steps.stepInfo.setTextColor(requireContext().getColor(android.R.color.white));
        binding.steps.stepReview.setTextColor(requireContext().getColor(R.color.tutor_ink));
        binding.steps.labelInfo.setTextColor(requireContext().getColor(R.color.tutor_blue));
        binding.steps.labelInfo.setTypeface(binding.steps.labelInfo.getTypeface(),android.graphics.Typeface.BOLD);
        binding.steps.labelReview.setTextColor(requireContext().getColor(R.color.tutor_muted));
        UserProfile profile=state.profile();
        draft(binding.name,"tutor.name",profile==null?"":profile.name);
        draft(binding.phone,"tutor.phone",profile==null?"":profile.phone);
        draft(binding.experience,"tutor.experience","");draft(binding.price,"tutor.price","");
        subjectId=state.draft("tutor.subjectId","");subjectName=state.draft("tutor.subjectName","");
        binding.subject.setText(subjectName);
        for(String id:state.draft("tutor.levels","").split(","))if(Arrays.asList(TutorRegistration.LEVELS).contains(id))levels.add(id);
        binding.levels.setText(levelLabel());
        binding.subject.setCompoundDrawablesRelativeWithIntrinsicBounds(null,null,TutorDesignAssets.icon(requireContext(),"down"),null);
        binding.levels.setCompoundDrawablesRelativeWithIntrinsicBounds(null,null,TutorDesignAssets.icon(requireContext(),"down"),null);
        binding.nextNote.setCompoundDrawablesRelativeWithIntrinsicBounds(TutorDesignAssets.icon(requireContext(),"info"),null,null,null);
        binding.online.setIcon(TutorDesignAssets.icon(requireContext(),"laptop"));
        binding.inPerson.setIcon(TutorDesignAssets.icon(requireContext(),"home"));
        online=Boolean.parseBoolean(state.draft("tutor.online","true"));inPerson=Boolean.parseBoolean(state.draft("tutor.inPerson","false"));
        renderModes();
        binding.online.setOnClickListener(v->{online=!online;saveModes();});
        binding.inPerson.setOnClickListener(v->{inPerson=!inPerson;saveModes();});
        binding.subject.setOnClickListener(v->chooseSubject());
        binding.levels.setOnClickListener(v->chooseLevels());
        binding.continueButton.setOnClickListener(v->review());
    }
    private void chooseSubject(){
        if(catalog.subjects().isEmpty()){
            new AlertDialog.Builder(requireContext()).setTitle(R.string.tutor_subject)
                    .setMessage(catalog.error().isEmpty()?getString(R.string.tutor_subject_empty):catalog.error())
                    .setPositiveButton(R.string.tutor_subject_reload,(dialog,which)->catalog.reload())
                    .setNegativeButton(R.string.cancel,null).show();return;
        }
        List<String> ids=new ArrayList<>(catalog.subjects().keySet());
        String[] labels=ids.stream().map(id->catalog.subjects().get(id)).toArray(String[]::new);
        new AlertDialog.Builder(requireContext()).setTitle(R.string.tutor_select_subject)
                .setSingleChoiceItems(labels,ids.indexOf(subjectId),(dialog,which)->{
                    subjectId=ids.get(which);subjectName=labels[which];binding.subject.setText(subjectName);
                    state.draft("tutor.subjectId",subjectId,false);state.draft("tutor.subjectName",subjectName,false);dialog.dismiss();
                }).setNegativeButton(R.string.cancel,null).show();
    }
    private void chooseLevels(){
        boolean[] checked=new boolean[TutorRegistration.LEVELS.length];
        for(int i=0;i<checked.length;i++)checked[i]=levels.contains(TutorRegistration.LEVELS[i]);
        new AlertDialog.Builder(requireContext()).setTitle(R.string.tutor_select_levels)
                .setMultiChoiceItems(TutorRegistration.LEVEL_LABELS,checked,(dialog,which,value)->checked[which]=value)
                .setPositiveButton(android.R.string.ok,(dialog,which)->{
                    levels.clear();for(int i=0;i<checked.length;i++)if(checked[i])levels.add(TutorRegistration.LEVELS[i]);
                    binding.levels.setText(levelLabel());state.draft("tutor.levels",String.join(",",levels),false);
                }).setNegativeButton(R.string.cancel,null).show();
    }
    private String levelLabel(){
        List<String> labels=new ArrayList<>();for(String id:levels)labels.add(TutorRegistration.LEVEL_LABELS[Arrays.asList(TutorRegistration.LEVELS).indexOf(id)]);
        return String.join(", ",labels);
    }
    private void saveModes(){state.draft("tutor.online",String.valueOf(online),false);state.draft("tutor.inPerson",String.valueOf(inPerson),false);renderModes();}
    private void renderModes(){mode(binding.online,online);mode(binding.inPerson,inPerson);}
    private void mode(MaterialButton button,boolean selected){
        button.setSelected(selected);button.setCheckable(true);button.setChecked(selected);
        button.setBackgroundTintList(ColorStateList.valueOf(requireContext().getColor(selected?R.color.tutor_surface:android.R.color.white)));
        button.setStrokeColor(ColorStateList.valueOf(requireContext().getColor(selected?R.color.tutor_blue:R.color.tutor_border)));
        button.setTextColor(requireContext().getColor(selected?R.color.tutor_blue:R.color.tutor_ink));
    }
    private void review(){
        int years=-1;long price=-1;
        try{years=Integer.parseInt(text(binding.experience));}catch(NumberFormatException ignored){}
        try{price=Long.parseLong(text(binding.price));}catch(NumberFormatException ignored){}
        TutorRegistration value=new TutorRegistration(text(binding.name),text(binding.phone),subjectId,subjectName,levels,years,
                online&&inPerson?"CA_HAI":online?"TRUC_TUYEN":inPerson?"TAI_NHA":"",price);
        String error=value.error();
        if(error.isEmpty()&&!catalog.subjects().containsKey(subjectId))error="Môn học này chưa khả dụng. Vui lòng tải lại danh mục và chọn môn.";
        if(!error.isEmpty()){state.reportError(error);binding.accountScroll.smoothScrollTo(0,0);return;}
        state.clearFeedback();state.draft("tutor.confirmed","false",false);host().hideKeyboard();state.reviewTutor(value);
    }
    @Override public void onDestroyView(){super.onDestroyView();binding=null;}
}
