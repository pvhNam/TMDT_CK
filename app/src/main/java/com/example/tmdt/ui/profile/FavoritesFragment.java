package com.example.tmdt.ui.profile;

import android.view.View;
import androidx.lifecycle.ViewModelProvider;
import com.example.tmdt.CatalogState;
import com.example.tmdt.FavoriteTutors;
import com.example.tmdt.R;
import com.example.tmdt.databinding.FragmentFavoritesBinding;
import com.example.tmdt.databinding.ItemFavoriteBinding;
import com.example.tmdt.ui.common.AccountFragmentBase;
import java.util.ArrayList;
import java.util.List;

/** "Gia sư yêu thích": the tutors the student saved with the heart on the home screen. */
public final class FavoritesFragment extends AccountFragmentBase {
    private FragmentFavoritesBinding binding;
    private CatalogState catalog;
    public FavoritesFragment(){super(R.layout.fragment_favorites);}
    @Override protected void bindFields(View view){
        binding=FragmentFavoritesBinding.bind(view);
        catalog=new ViewModelProvider(requireActivity()).get(CatalogState.class);
        catalog.changes().observe(getViewLifecycleOwner(),ignored->render());
    }
    @Override protected void renderState(){render();}
    private void render(){
        if(binding==null||catalog==null)return;
        binding.list.removeAllViews();
        if(catalog.loading()){binding.count.setText("Đang tải…");binding.empty.setVisibility(View.GONE);return;}
        List<CatalogState.Teacher> shown=new ArrayList<>();
        for(String id:FavoriteTutors.ids(requireContext())){
            CatalogState.Teacher teacher=catalog.teacher(id);
            if(teacher!=null&&!teacher.offerings.isEmpty())shown.add(teacher);
        }
        binding.count.setText(shown.size()+" gia sư đã lưu");
        binding.empty.setVisibility(shown.isEmpty()?View.VISIBLE:View.GONE);
        for(CatalogState.Teacher teacher:shown){
            CatalogState.Offering cheapest=teacher.offerings.get(0);
            for(CatalogState.Offering offering:teacher.offerings)if(offering.price<cheapest.price)cheapest=offering;
            ItemFavoriteBinding item=ItemFavoriteBinding.inflate(getLayoutInflater(),binding.list,false);
            item.name.setText(teacher.name);item.avatar.bind(teacher.id,teacher.name);
            String subject=catalog.subjects().get(cheapest.subjectId);
            item.course.setText((subject==null?"":subject+" · ")+CatalogState.level(cheapest.level));
            item.price.setText(CatalogState.money(cheapest.price)+" / buổi");
            item.heart.setOnClickListener(v->{FavoriteTutors.toggle(requireContext(),teacher.id);render();});
            item.viewProfile.setOnClickListener(v->host().classroom.openTutor(teacher.id,teacher.name,"favorites"));
            item.getRoot().setOnClickListener(v->host().classroom.openTutor(teacher.id,teacher.name,"favorites"));
            binding.list.addView(item.getRoot());
        }
    }
    @Override public void onDestroyView(){super.onDestroyView();binding=null;}
}
