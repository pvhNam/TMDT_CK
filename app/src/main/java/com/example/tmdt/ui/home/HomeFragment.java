package com.example.tmdt.ui.home;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.tmdt.AccountState;
import com.example.tmdt.CatalogState;
import com.example.tmdt.MainActivity;
import com.example.tmdt.R;
import com.example.tmdt.databinding.FragmentHomeBinding;
import com.example.tmdt.databinding.ItemTutorBinding;
import com.google.android.material.chip.Chip;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private CatalogState catalog;
    public HomeFragment(){super(R.layout.fragment_home);}
    @Override public void onViewCreated(@NonNull View view,@Nullable Bundle savedInstanceState){
        super.onViewCreated(view,savedInstanceState);
        binding=FragmentHomeBinding.bind(view);
        catalog=new ViewModelProvider(requireActivity()).get(CatalogState.class);
        AccountState account=new ViewModelProvider(requireActivity()).get(AccountState.class);
        binding.search.setText(catalog.query());
        binding.search.setOnEditorActionListener((input,action,event)->{host().hideKeyboard();return true;});
        binding.search.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            public void onTextChanged(CharSequence s,int start,int before,int count){catalog.query(s.toString());renderResults();}
            public void afterTextChanged(Editable editable){}
        });
        binding.account.setOnClickListener(v->host().show("account"));
        binding.refresh.setOnClickListener(v->catalog.reload());binding.retry.setOnClickListener(v->catalog.reload());
        binding.clearFilter.setOnClickListener(v->{catalog.subject("");binding.search.setText("");renderSubjects();renderResults();});
        account.changes().observe(getViewLifecycleOwner(),ignored->binding.greeting.setText(account.profile()==null?getString(R.string.greeting):getString(R.string.greeting_name,account.profile().name)));
        catalog.changes().observe(getViewLifecycleOwner(),ignored->{renderSubjects();renderResults();});
    }
    private MainActivity host(){return (MainActivity)requireActivity();}
    private void renderSubjects(){
        binding.subjects.removeAllViews();chip("",getString(R.string.all_subjects));
        for(Map.Entry<String,String> subject:catalog.subjects().entrySet())chip(subject.getKey(),subject.getValue());
    }
    private void chip(String id,String title){
        Chip chip=new Chip(requireContext());chip.setId(View.generateViewId());chip.setText(title);chip.setCheckable(true);
        chip.setChecked(catalog.subject().equals(id));
        chip.setOnClickListener(v->{catalog.subject(id);renderSubjects();renderResults();});
        binding.subjects.addView(chip);
    }
    private void renderResults(){
        binding.results.removeAllViews();
        boolean loading=catalog.loading(),failed=!catalog.error().isEmpty();
        List<CatalogState.Teacher> teachers=catalog.results();
        boolean empty=!loading&&!failed&&teachers.isEmpty(),filtered=!catalog.query().isEmpty()||!catalog.subject().isEmpty();
        binding.progress.setVisibility(loading?View.VISIBLE:View.GONE);
        binding.offline.setVisibility(catalog.offline()&&!failed&&!loading?View.VISIBLE:View.GONE);
        binding.message.setVisibility(failed||empty?View.VISIBLE:View.GONE);
        binding.description.setVisibility(empty?View.VISIBLE:View.GONE);
        binding.retry.setVisibility(failed?View.VISIBLE:View.GONE);
        binding.clearFilter.setVisibility(empty&&filtered?View.VISIBLE:View.GONE);
        binding.message.setText(failed?catalog.error():getString(filtered?R.string.empty_search:R.string.empty_catalog));
        binding.description.setText(filtered?R.string.empty_search_caption:R.string.empty_catalog_caption);
        if(loading||failed)return;
        for(CatalogState.Teacher teacher:teachers){
            ItemTutorBinding item=ItemTutorBinding.inflate(getLayoutInflater(),binding.results,false);
            item.name.setText(teacher.name);item.initial.setText(teacher.name.substring(0,1).toUpperCase(Locale.ROOT));
            CatalogState.Offering selected=null;
            for(CatalogState.Offering offering:teacher.offerings){
                if((catalog.subject().isEmpty()||catalog.subject().equals(offering.subjectId))&&(selected==null||offering.price<selected.price))selected=offering;
            }
            if(selected==null)continue;
            item.subject.setText(getString(R.string.subject_level,catalog.subjects().get(selected.subjectId),CatalogState.level(selected.level)));
            item.mode.setText(CatalogState.mode(teacher.mode));item.price.setText(getString(R.string.price_from,CatalogState.money(selected.price)));
            item.getRoot().setOnClickListener(v->TutorDetailsDialog.create(teacher.id).show(getParentFragmentManager(),"tutor_details"));
            binding.results.addView(item.getRoot());
        }
    }
    @Override public void onDestroyView(){super.onDestroyView();binding=null;}
}

