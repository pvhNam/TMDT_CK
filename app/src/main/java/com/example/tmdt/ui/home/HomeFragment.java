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
import java.util.ArrayList;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.example.tmdt.ui.common.ThemeColors;
import com.example.tmdt.ui.common.Ui;

public final class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private CatalogState catalog;
    private final Map<String,String> shownSubjects=new LinkedHashMap<>();
    private String shownSubject;
    private final TutorPages pages=new TutorPages();
    private String shownTutors="",shownFilter="";
    private boolean renderQueued;
    private boolean pulling;
    private final Runnable render=()->{
        renderQueued=false;
        if(binding!=null){renderSubjects();renderResults();}
    };
    public HomeFragment(){super(R.layout.fragment_home);}
    @Override public void onViewCreated(@NonNull View view,@Nullable Bundle savedInstanceState){
        super.onViewCreated(view,savedInstanceState);
        binding=FragmentHomeBinding.bind(view);
        catalog=new ViewModelProvider(requireActivity()).get(CatalogState.class);
        binding.pullRefresh.setColorSchemeColors(requireContext().getColor(R.color.app_accent));
        binding.pullRefresh.setProgressBackgroundColorSchemeColor(requireContext().getColor(R.color.app_surface));
        binding.pullRefresh.setOnChildScrollUpCallback((parent,child)->binding!=null&&binding.homeScroll.canScrollVertically(-1));
        binding.pullRefresh.setOnRefreshListener(()->{
            if(pulling)return;pulling=true;
            FragmentHomeBinding current=binding;
            catalog.refreshFromServer().addOnCompleteListener(task->{
                if(binding!=current)return;pulling=false;current.pullRefresh.setRefreshing(false);
                if(!task.isSuccessful())com.google.android.material.snackbar.Snackbar.make(current.getRoot(),"Chưa làm mới được. Kiểm tra kết nối rồi thử lại.",com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show();
                requestRender();
            });
        });
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
        host().classroom.changes().observe(getViewLifecycleOwner(),ignored->{if(binding!=null)com.example.tmdt.ui.groupclass.GroupSearchFragment.upcoming(host().classroom,binding.groupClasses);});
        setUpPager();
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
        boolean searched=!catalog.query().isEmpty()||!catalog.subject().isEmpty();
        boolean retained=pages.getItemCount()>0;
        if(!loading&&!pulling)binding.pullRefresh.setRefreshing(false);
        binding.progress.setVisibility(loading&&!retained&&!pulling?View.VISIBLE:View.GONE);
        binding.offline.setVisibility(catalog.offline()&&!failed&&!loading?View.VISIBLE:View.GONE);
        binding.message.setVisibility(failed?View.VISIBLE:View.GONE);
        binding.description.setVisibility(View.GONE);
        binding.retry.setVisibility(failed?View.VISIBLE:View.GONE);
        binding.clearSearch.setVisibility(View.GONE);
        binding.message.setText(failed?catalog.error():getString(searched?R.string.empty_search:R.string.empty_catalog));
        binding.description.setText(searched?R.string.empty_search_caption:R.string.empty_catalog_caption);
        binding.results.setVisibility((loading||failed)&&!retained?View.GONE:View.VISIBLE);
        if(loading||failed)return;
        List<Shown> shown=new ArrayList<>();StringBuilder key=new StringBuilder();
        for(CatalogState.Teacher teacher:teachers){
            CatalogState.Offering selected=null;
            for(CatalogState.Offering offering:teacher.offerings){
                if((catalog.subject().isEmpty()||catalog.subject().equals(offering.subjectId))
                        &&(selected==null||offering.price<selected.price))selected=offering;
            }
            if(selected==null)continue;
            shown.add(new Shown(teacher,selected));
            key.append(teacher.id).append('|').append(teacher.name).append('|').append(teacher.mode).append('|')
                    .append(selected.subjectId).append('|').append(selected.level).append('|').append(selected.price).append('\n');
        }
        binding.results.setVisibility(shown.isEmpty()?View.GONE:View.VISIBLE);
        if(shown.isEmpty())binding.message.setVisibility(View.VISIBLE);
        String filter=catalog.query()+"|"+catalog.subject();
        if(!key.toString().equals(shownTutors)){
            shownTutors=key.toString();pages.show(shown);
            if(!filter.equals(shownFilter))binding.resultsPager.setCurrentItem(0,false);
            fitPager();
        }
        shownFilter=filter;
        renderPagerBar();
    }

    // Gia sư đang nhận lớp: three tutors a page, swiped left and right inside its own card.
    private static final int PER_PAGE=3;
    private static final class Shown{
        final CatalogState.Teacher teacher;final CatalogState.Offering offering;
        Shown(CatalogState.Teacher teacher,CatalogState.Offering offering){this.teacher=teacher;this.offering=offering;}
    }
    private void setUpPager(){
        Ui ui=host().classroom.ui;
        ui.cardSurface(binding.results,Ui.WHITE,ThemeColors.dark(requireContext())?Ui.BORDER:0);
        binding.results.setElevation(ui.dp(3));
        binding.results.setOutlineAmbientShadowColor(0x226583AD);binding.results.setOutlineSpotShadowColor(0x446583AD);
        binding.resultsPager.setAdapter(pages);binding.resultsPager.setOffscreenPageLimit(1);
        binding.resultsPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback(){
            @Override public void onPageSelected(int position){if(binding!=null)renderPagerBar();}
        });
        binding.pagerPrev.setOnClickListener(v->binding.resultsPager.setCurrentItem(binding.resultsPager.getCurrentItem()-1));
        binding.pagerNext.setOnClickListener(v->binding.resultsPager.setCurrentItem(binding.resultsPager.getCurrentItem()+1));
    }
    /** The pager takes the height of a full page, so every page and the bar below stay in place. */
    private void fitPager(){
        binding.resultsPager.post(()->{
            if(binding==null||pages.getItemCount()==0)return;
            int width=binding.resultsPager.getWidth();if(width==0)return;
            View page=pages.page(binding.resultsPager,0);
            page.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
            ViewGroup.LayoutParams params=binding.resultsPager.getLayoutParams();
            if(params.height!=page.getMeasuredHeight()){params.height=page.getMeasuredHeight();binding.resultsPager.setLayoutParams(params);}
        });
    }
    private void renderPagerBar(){
        int count=pages.getItemCount(),current=binding.resultsPager.getCurrentItem();
        binding.pagerBar.setVisibility(count>1?View.VISIBLE:View.GONE);
        binding.pagerPrev.setEnabled(current>0);binding.pagerPrev.setAlpha(current>0?1f:.35f);
        binding.pagerNext.setEnabled(current<count-1);binding.pagerNext.setAlpha(current<count-1?1f:.35f);
        Ui ui=host().classroom.ui;binding.pagerDots.removeAllViews();
        binding.pagerBar.setContentDescription("Trang "+(current+1)+" trên "+count);
        if(count>7){
            binding.pagerDots.addView(ui.text((current+1)+" / "+count,14,Ui.MUTED,true));
            return;
        }
        for(int i=0;i<count;i++){
            View dot=new View(requireContext());
            ui.surface(dot,i==current?Ui.BLUE:0xFFC9D8EE,3,0);
            android.widget.LinearLayout.LayoutParams params=new android.widget.LinearLayout.LayoutParams(ui.dp(i==current?18:6),ui.dp(6));
            params.leftMargin=params.rightMargin=ui.dp(3);
            int page=i;ui.clickable(dot,()->binding.resultsPager.setCurrentItem(page));
            binding.pagerDots.addView(dot,params);
        }
    }
    private final class TutorPages extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
        private final List<Shown> tutors=new ArrayList<>();
        void show(List<Shown> list){tutors.clear();tutors.addAll(list);notifyDataSetChanged();}
        @Override public int getItemCount(){return (tutors.size()+PER_PAGE-1)/PER_PAGE;}
        @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,int type){
            android.widget.LinearLayout page=new android.widget.LinearLayout(parent.getContext());
            page.setOrientation(android.widget.LinearLayout.VERTICAL);
            page.setLayoutParams(new RecyclerView.LayoutParams(-1,-1));
            return new RecyclerView.ViewHolder(page){};
        }
        @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder,int position){fill((ViewGroup)holder.itemView,position);}
        View page(ViewGroup parent,int position){
            RecyclerView.ViewHolder holder=onCreateViewHolder(parent,0);fill((ViewGroup)holder.itemView,position);return holder.itemView;
        }
        private void fill(ViewGroup page,int position){
            page.removeAllViews();Ui ui=host().classroom.ui;
            for(int i=position*PER_PAGE;i<Math.min(tutors.size(),(position+1)*PER_PAGE);i++){
                if(i>position*PER_PAGE){
                    View divider=new View(page.getContext());divider.setBackgroundColor(ThemeColors.border(page.getContext(),Ui.BORDER));
                    android.widget.LinearLayout.LayoutParams params=new android.widget.LinearLayout.LayoutParams(-1,1);
                    params.leftMargin=ui.dp(82);params.rightMargin=ui.dp(12);page.addView(divider,params);
                }
                page.addView(row(page,tutors.get(i)));
            }
        }
    }
    private View row(ViewGroup page,Shown shown){
        CatalogState.Teacher teacher=shown.teacher;CatalogState.Offering selected=shown.offering;
        ItemTutorBinding item=ItemTutorBinding.inflate(getLayoutInflater(),page,false);
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
        ThemeColors.apply(item.getRoot());
        com.example.tmdt.ui.common.Motion.bindControls(item.getRoot());
        return item.getRoot();
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
        renderQueued=false;pulling=false;shownTutors="";shownFilter="";pages.show(new ArrayList<>());shownSubjects.clear();shownSubject=null;
        super.onDestroyView();binding=null;
    }
}

