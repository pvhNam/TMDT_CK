package com.example.tmdt.ui.common;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.example.tmdt.MainActivity;
import com.example.tmdt.R;
import com.google.android.material.tabs.TabLayout;
import java.util.HashMap;
import java.util.Map;

/** Related pages share one entry, retaining each tab's views and scroll while switching. */
public abstract class SectionHubFragment extends Fragment {
    public static final class Selection extends ViewModel {
        private final Map<String,Integer> tabs=new HashMap<>();
    }
    private Selection selection;
    private int selected;
    protected abstract String title();
    protected abstract String[] sections();
    protected abstract Fragment page(int index);
    protected abstract String selectionKey();

    public static boolean embedded(Fragment fragment){return fragment.getParentFragment() instanceof SectionHubFragment;}

    @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup container,@Nullable Bundle state){
        Ui ui=((MainActivity)requireActivity()).classroom.ui;
        LinearLayout root=ui.column();
        root.setBackgroundColor(ThemeColors.background(requireContext()));
        ui.header(root,title(),()->((MainActivity)requireActivity()).back(),null);
        TabLayout tabs=(TabLayout)inflater.inflate(R.layout.view_section_tabs,root,false);
        root.addView(tabs);
        FragmentContainerView content=new FragmentContainerView(requireContext());content.setId(R.id.section_content);
        root.addView(content,new LinearLayout.LayoutParams(-1,0,1));
        return root;
    }
    @Override public void onViewCreated(@NonNull View view,@Nullable Bundle state){
        super.onViewCreated(view,state);
        selection=new ViewModelProvider(requireActivity()).get(Selection.class);
        selected=state!=null?state.getInt("section",0):selection.tabs.getOrDefault(selectionKey(),0);
        if(state==null&&getArguments()!=null&&getArguments().containsKey("section"))selected=getArguments().getInt("section");
        String[] labels=sections();selected=Math.max(0,Math.min(selected,labels.length-1));
        TabLayout tabs=view.findViewById(R.id.section_tabs);
        for(int i=0;i<labels.length;i++)tabs.addTab(tabs.newTab().setText(labels[i]),i==selected);
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener(){
            @Override public void onTabSelected(TabLayout.Tab tab){showSection(tab.getPosition());}
            @Override public void onTabUnselected(TabLayout.Tab tab){}
            @Override public void onTabReselected(TabLayout.Tab tab){}
        });
        showSection(selected);
        ThemeColors.apply(view);Motion.bindControls(view);
    }
    private void showSection(int index){
        if(getChildFragmentManager().isStateSaved())return;
        selected=index;selection.tabs.put(selectionKey(),index);
        ((MainActivity)requireActivity()).hideKeyboard();
        String prefix="section_"+selectionKey()+"_",tag=prefix+index;
        Fragment active=getChildFragmentManager().findFragmentByTag(tag);
        if(active!=null&&!active.isHidden())return;
        FragmentTransaction transaction=getChildFragmentManager().beginTransaction().setReorderingAllowed(true);
        for(Fragment fragment:getChildFragmentManager().getFragments())if(fragment!=active){
            if(fragment.getTag()==null||!fragment.getTag().startsWith(prefix))transaction.remove(fragment);
            else transaction.hide(fragment).setMaxLifecycle(fragment,Lifecycle.State.STARTED);
        }
        if(active==null){active=page(index);transaction.add(R.id.section_content,active,tag);}
        else transaction.show(active);
        transaction.setMaxLifecycle(active,Lifecycle.State.RESUMED).commitNow();
        // Animate only an intentional tab change; server updates never fade the whole page.
        if(getChildFragmentManager().getFragments().size()>1&&active.getView()!=null)Motion.reveal(active.getView());
    }
    @Override public void onSaveInstanceState(@NonNull Bundle out){super.onSaveInstanceState(out);out.putInt("section",selected);}
}
