package com.example.tmdt.ui.home;

import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.tmdt.CatalogState;
import com.example.tmdt.R;
import com.example.tmdt.databinding.DialogTutorBinding;
import com.example.tmdt.databinding.ItemOfferingBinding;

public final class TutorDetailsDialog extends DialogFragment {
    public static TutorDetailsDialog create(String id){
        TutorDetailsDialog dialog=new TutorDetailsDialog();
        Bundle args=new Bundle();args.putString("tutor_id",id);dialog.setArguments(args);return dialog;
    }
    @NonNull @Override public Dialog onCreateDialog(Bundle state){
        CatalogState catalog=new ViewModelProvider(requireActivity()).get(CatalogState.class);
        CatalogState.Teacher teacher=catalog.teacher(requireArguments().getString("tutor_id",""));
        AlertDialog.Builder dialog=new AlertDialog.Builder(requireContext()).setPositiveButton(R.string.close,null);
        if(teacher==null)return dialog.setMessage(R.string.empty_catalog).create();
        DialogTutorBinding binding=DialogTutorBinding.inflate(getLayoutInflater());
        binding.experience.setText(getString(R.string.experience,teacher.experience,CatalogState.mode(teacher.mode)));
        binding.region.setText(teacher.region);binding.education.setText(teacher.education+" · "+teacher.school);
        binding.introduction.setText(teacher.introduction);
        for(CatalogState.Offering offering:teacher.offerings){
            ItemOfferingBinding item=ItemOfferingBinding.inflate(getLayoutInflater(),binding.offerings,false);
            item.subject.setText(getString(R.string.subject_level,catalog.subjects().get(offering.subjectId),CatalogState.level(offering.level)));
            item.price.setText(getString(R.string.offering_price,CatalogState.money(offering.price)));
            binding.offerings.addView(item.getRoot());
        }
        return dialog.setTitle(teacher.name).setView(binding.getRoot()).create();
    }
}

