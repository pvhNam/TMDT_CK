package com.example.tmdt.ui.common;

import android.view.View;
import android.view.ViewGroup;
import androidx.viewbinding.ViewBinding;
import com.example.tmdt.R;
import java.util.*;
import java.util.function.Supplier;

/** Keyed rows stay attached while repeated snapshots update their text and actions. */
public final class StableBindings<B extends ViewBinding> {
    private final Map<String,B> rows=new LinkedHashMap<>();
    private final List<String> order=new ArrayList<>();
    private StableBindings(){}
    @SuppressWarnings("unchecked") public static <B extends ViewBinding> StableBindings<B> at(ViewGroup parent){
        Object existing=parent.getTag(R.id.stable_bindings);
        if(existing!=null)return (StableBindings<B>)existing;
        StableBindings<B> bindings=new StableBindings<>();parent.setTag(R.id.stable_bindings,bindings);return bindings;
    }
    public void begin(){order.clear();}
    public B item(String id,Supplier<B> create){
        B binding=rows.get(id);if(binding==null){binding=create.get();rows.put(id,binding);}
        order.add(id);return binding;
    }
    public void commit(ViewGroup parent){
        List<View> desired=new ArrayList<>();for(String id:order)desired.add(rows.get(id).getRoot());
        boolean changed=parent.getChildCount()!=desired.size();
        if(!changed)for(int i=0;i<desired.size();i++)if(parent.getChildAt(i)!=desired.get(i)){changed=true;break;}
        if(!changed)return;
        Motion.change(parent);
        Set<View> retained=new HashSet<>(desired);
        for(int i=parent.getChildCount()-1;i>=0;i--)if(!retained.contains(parent.getChildAt(i)))parent.removeViewAt(i);
        for(int i=0;i<desired.size();i++){
            View view=desired.get(i);if(parent.getChildAt(i)==view)continue;
            if(view.getParent()==parent)parent.removeView(view);parent.addView(view,i);
            ThemeColors.apply(view);Motion.bindControls(view);
        }
        rows.keySet().retainAll(new HashSet<>(order));
    }
}
