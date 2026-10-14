package com.example.tmdt.ui.home;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.tmdt.CatalogState;
import com.example.tmdt.MainActivity;
import com.example.tmdt.R;
import com.example.tmdt.Tutor;
import com.example.tmdt.databinding.DialogTutorBinding;
import com.example.tmdt.databinding.ItemOfferingBinding;
import java.util.ArrayList;
import java.util.List;

public final class TutorDetailsDialog extends DialogFragment {
    public static TutorDetailsDialog create(String id){
        TutorDetailsDialog dialog=new TutorDetailsDialog();
        Bundle args=new Bundle();args.putString("tutor_id",id);dialog.setArguments(args);return dialog;
    }
    @NonNull @Override public Dialog onCreateDialog(Bundle state){
        CatalogState catalog=new ViewModelProvider(requireActivity()).get(CatalogState.class);
        CatalogState.Teacher teacher=catalog.teacher(requireArguments().getString("tutor_id",""));
        AlertDialog.Builder dialog=new AlertDialog.Builder(requireContext(),R.style.ThemeOverlay_TMDT_Classroom_Dialog);
        if(teacher==null)return dialog.setMessage(R.string.empty_catalog).setPositiveButton(R.string.close,null).create();
        DialogTutorBinding binding=DialogTutorBinding.inflate(getLayoutInflater());
        binding.avatar.bind(teacher.id,teacher.name);binding.name.setText(teacher.name.trim());
        binding.experience.setText(getString(R.string.tutor_years,teacher.experience));
        binding.experienceRow.setVisibility(teacher.experience>0?View.VISIBLE:View.GONE);
        binding.mode.setText(CatalogState.mode(teacher.mode));
        binding.modeIcon.setImageResource("TAI_NHA".equals(teacher.mode)?R.drawable.ic_home_soft:R.drawable.ic_video);
        binding.region.setText(teacher.region.trim());binding.regionRow.setVisibility(teacher.region.trim().isEmpty()?View.GONE:View.VISIBLE);
        String education=join(teacher.education,teacher.school);
        binding.education.setText(education);binding.educationRow.setVisibility(education.isEmpty()?View.GONE:View.VISIBLE);
        binding.introduction.setText(teacher.introduction.trim());
        int intro=teacher.introduction.trim().isEmpty()?View.GONE:View.VISIBLE;
        binding.introductionLabel.setVisibility(intro);binding.introduction.setVisibility(intro);
        for(CatalogState.Offering offering:teacher.offerings){
            ItemOfferingBinding item=ItemOfferingBinding.inflate(getLayoutInflater(),binding.offerings,false);
            item.subject.setText(getString(R.string.subject_level,catalog.subjects().get(offering.subjectId),CatalogState.level(offering.level)));
            item.price.setText(getString(R.string.offering_price,CatalogState.money(offering.price)));
            binding.offerings.addView(item.getRoot());
        }
        MainActivity host=(MainActivity)requireActivity();
        binding.close.setOnClickListener(v->dismiss());
        binding.book.setVisibility(teacher.offerings.isEmpty()?View.GONE:View.VISIBLE);
        binding.book.setOnClickListener(v->{dismiss();book(host,catalog,teacher);});
        return dialog.setView(binding.getRoot()).create();
    }
    private static String join(String first,String second){
        String a=first==null?"":first.trim(),b=second==null?"":second.trim();
        return a.isEmpty()?b:b.isEmpty()?a:a+" · "+b;
        dialog.setNegativeButton(R.string.tutor_profile_full_profile,(d,which)->host.classroom.openTutor(teacher.id,teacher.name,"home"));
        if(!teacher.offerings.isEmpty())dialog.setNeutralButton(R.string.book_lesson,(d,which)->book(host,catalog,teacher));
        return dialog.setTitle(teacher.name).setView(binding.getRoot()).create();
    }
    /** Opens the booking form (member 3); a tutor teaching several subjects first asks which one. */
    private static void book(MainActivity host,CatalogState catalog,CatalogState.Teacher teacher){
        List<CatalogState.Offering> offerings=new ArrayList<>(teacher.offerings);
        if(offerings.size()==1){host.classroom.openBooking(tutor(catalog,teacher,offerings.get(0)));return;}
        String[] labels=new String[offerings.size()];
        for(int i=0;i<labels.length;i++){
            CatalogState.Offering offering=offerings.get(i);
            labels[i]=host.getString(R.string.subject_level,catalog.subjects().get(offering.subjectId),CatalogState.level(offering.level))
                    +" · "+CatalogState.money(offering.price);
        }
        new AlertDialog.Builder(host).setTitle(R.string.choose_subject).setNegativeButton(R.string.close,null)
                .setItems(labels,(d,which)->host.classroom.openBooking(tutor(catalog,teacher,offerings.get(which)))).show();
    }
    private static Tutor tutor(CatalogState catalog,CatalogState.Teacher teacher,CatalogState.Offering offering){
        return Tutor.fromCatalog(teacher.id,teacher.name,catalog.subjects().get(offering.subjectId),CatalogState.level(offering.level),offering.price);
    }
}

