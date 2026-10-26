package com.example.tmdt.ui.home;

import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
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
import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;

public final class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private CatalogState catalog;
    private final Map<String,String> shownSubjects=new LinkedHashMap<>();
    private String shownSubject;
    private final Map<String,ItemTutorBinding> cards=new LinkedHashMap<>();
    private boolean renderQueued;
    private final Runnable render=()->{
        renderQueued=false;
        if(binding!=null){renderSubjects();renderResults();}
    };
    public HomeFragment(){super(R.layout.fragment_home);}
    @Override public void onViewCreated(@NonNull View view,@Nullable Bundle savedInstanceState){
        super.onViewCreated(view,savedInstanceState);
        binding=FragmentHomeBinding.bind(view);
        catalog=new ViewModelProvider(requireActivity()).get(CatalogState.class);
        AccountState account=new ViewModelProvider(requireActivity()).get(AccountState.class);
        binding.search.setText(catalog.query());
        binding.searchButton.setOnClickListener(v -> searchTeachers());
        binding.search.setOnEditorActionListener((input, action, event) -> {
            if (action == EditorInfo.IME_ACTION_SEARCH) {
                searchTeachers();
                return true;
            }
            return false;
        });
        binding.account.bindCurrentAccount();
        binding.account.setOnClickListener(v->host().show("account"));
        binding.groupClasses.setOnClickListener(v->host().show("groups"));
        binding.postTutorRequest.setOnClickListener(v->host().show("post_request"));
        binding.manageTutorRequests.setOnClickListener(v->host().openMyRequests("home"));
        binding.refresh.setOnClickListener(v->catalog.reload());binding.retry.setOnClickListener(v->catalog.reload());
        binding.clearSearch.setOnClickListener(v -> {
            catalog.subject("");
            catalog.query("");
            binding.search.setText("");
            requestRender();
        });
        account.changes().observe(getViewLifecycleOwner(),ignored->binding.greeting.setText(account.profile()==null?getString(R.string.greeting):getString(R.string.greeting_name,account.profile().name)));
        catalog.changes().observe(getViewLifecycleOwner(), ignored -> {
            requestRender();
        });
    }
    private void searchTeachers() {
        String name = binding.search.getText().toString();
        catalog.query(name);
        host().hideKeyboard();
        requestRender();
    }
    private void requestRender(){
        if(binding!=null&&!renderQueued){renderQueued=true;binding.getRoot().postOnAnimation(render);}
    }
    private MainActivity host(){return (MainActivity)requireActivity();}
    private void renderSubjects() {
        if(catalog.subject().equals(shownSubject)&&shownSubjects.equals(catalog.subjects()))return;
        shownSubject=catalog.subject();shownSubjects.clear();shownSubjects.putAll(catalog.subjects());
        binding.subjects.removeAllViews();
        chip("", getString(R.string.all_subjects));
        for (Map.Entry<String, String> subject : catalog.subjects().entrySet()) {
            chip(subject.getKey(), subject.getValue());
        }
    }
    private void chip(String id, String title) {
        Chip chip = new Chip(requireContext());
        chip.setId(View.generateViewId());
        chip.setText(title);
        chip.setCheckable(true);
        chip.setChecked(catalog.subject().equals(id));
        chip.setOnClickListener(v -> {
            catalog.subject(id);
            requestRender();
        });
        binding.subjects.addView(chip);
    }
    private void renderResults(){
        boolean loading=catalog.loading(),failed=!catalog.error().isEmpty();
        List<CatalogState.Teacher> teachers=catalog.results();
        boolean empty=!loading&&!failed&&teachers.isEmpty(),searched=!catalog.query().isEmpty()||!catalog.subject().isEmpty();
        binding.progress.setVisibility(loading?View.VISIBLE:View.GONE);
        binding.offline.setVisibility(catalog.offline()&&!failed&&!loading?View.VISIBLE:View.GONE);
        binding.message.setVisibility(failed||empty?View.VISIBLE:View.GONE);
        binding.description.setVisibility(empty?View.VISIBLE:View.GONE);
        binding.retry.setVisibility(failed?View.VISIBLE:View.GONE);
        binding.clearSearch.setVisibility(empty&&searched?View.VISIBLE:View.GONE);
        binding.message.setText(failed?catalog.error():getString(searched?R.string.empty_search:R.string.empty_catalog));
        binding.description.setText(searched?R.string.empty_search_caption:R.string.empty_catalog_caption);
        binding.results.setVisibility(loading||failed?View.GONE:View.VISIBLE);
        if(loading||failed)return;
        java.util.Set<String> visible=new java.util.HashSet<>();
        int position=0;
        for(CatalogState.Teacher teacher:teachers){
            CatalogState.Offering selected=null;
            for(CatalogState.Offering offering:teacher.offerings){
                if((catalog.subject().isEmpty()||catalog.subject().equals(offering.subjectId))
                        &&(selected==null||offering.price<selected.price))selected=offering;
            }
            if(selected==null)continue;
            visible.add(teacher.id);
            ItemTutorBinding item=cards.get(teacher.id);
            if(item==null){item=ItemTutorBinding.inflate(getLayoutInflater(),binding.results,false);cards.put(teacher.id,item);}
            if(binding.results.indexOfChild(item.getRoot())!=position){
                binding.results.removeView(item.getRoot());binding.results.addView(item.getRoot(),position);
            }
            position++;
            item.name.setText(teacher.name);item.avatar.bind(teacher.id,teacher.name);
            item.subject.setText(getString(R.string.subject_level,catalog.subjects().get(selected.subjectId),CatalogState.level(selected.level)));
            item.mode.setText(CatalogState.mode(teacher.mode));
            item.modeIcon.setImageResource("TAI_NHA".equals(teacher.mode)?R.drawable.ic_home_soft:R.drawable.ic_video);
            item.price.setText(getString(R.string.price_from,CatalogState.money(selected.price)));
            item.getRoot().setOnClickListener(v->host().classroom.openTutor(teacher.id,teacher.name,"home"));
            item.getRoot().setOnLongClickListener(v->{
                TutorDetailsDialog.create(teacher.id).show(getParentFragmentManager(),"tutor_details");
                return true;
            });
            bindFavorite(item.favorite,teacher.id);
        }
        java.util.Iterator<Map.Entry<String,ItemTutorBinding>> iterator=cards.entrySet().iterator();
        while(iterator.hasNext()){
            Map.Entry<String,ItemTutorBinding> entry=iterator.next();
            if(!visible.contains(entry.getKey())){binding.results.removeView(entry.getValue().getRoot());iterator.remove();}
        }
    }
    private void bindFavorite(android.widget.ImageView heart,String tutorId){
        AccountState account=new ViewModelProvider(requireActivity()).get(AccountState.class);
        final boolean[] saved={account.signedIn()&&com.example.tmdt.FavoriteTutors.contains(requireContext(),tutorId)};
        heart.setImageResource(saved[0]?R.drawable.ic_pf_heart_filled:R.drawable.ic_pf_heart_blue);
        heart.setContentDescription(saved[0]?"Bỏ lưu gia sư":"Lưu gia sư");
        heart.setOnClickListener(v->{
            if(!account.signedIn()){host().show("login");return;}
            saved[0]=com.example.tmdt.FavoriteTutors.toggle(requireContext(),tutorId);
            heart.setImageResource(saved[0]?R.drawable.ic_pf_heart_filled:R.drawable.ic_pf_heart_blue);
            heart.setContentDescription(saved[0]?"Bỏ lưu gia sư":"Lưu gia sư");
        });
    }
    @Override public void onDestroyView(){
        if(binding!=null)binding.getRoot().removeCallbacks(render);
        renderQueued=false;cards.clear();shownSubjects.clear();shownSubject=null;
        super.onDestroyView();binding=null;
    }
}

