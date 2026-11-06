package com.example.tmdt.ui.common;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.example.tmdt.R;
import java.util.*;
import java.util.function.Supplier;

/** Keeps unchanged programmatic cards attached; only changed records replace their own card. */
public final class StableViews {
    private static final class Row {
        final Object snapshot; final View view;
        Row(Object snapshot,View view){this.snapshot=snapshot;this.view=view;}
    }
    private final Map<String,Row> rows=new HashMap<>();
    private final List<View> desired=new ArrayList<>();
    private final Set<String> used=new HashSet<>();
    private final ViewGroup parent;
    private StableViews(ViewGroup parent){this.parent=parent;}
    public static StableViews at(ViewGroup parent){
        Object existing=parent.getTag(R.id.stable_views);
        if(existing instanceof StableViews)return (StableViews)existing;
        StableViews rows=new StableViews(parent);parent.setTag(R.id.stable_views,rows);return rows;
    }
    public void begin(){desired.clear();used.clear();}
    public void add(String id,Object content,Supplier<? extends View> create,int bottomMargin){
        if(!used.add(id))throw new IllegalArgumentException("Duplicate row: "+id);
        Object snapshot=RenderSnapshot.of(content);
        Row row=rows.get(id);
        if(row==null||!Objects.equals(snapshot,row.snapshot)){
            View view=create.get();LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);
            params.bottomMargin=bottomMargin;view.setLayoutParams(params);
            ThemeColors.apply(view);Motion.bindControls(view);
            row=new Row(snapshot,view);rows.put(id,row);
        }
        desired.add(row.view);
    }
    public void commit(){reconcile(parent,desired);rows.keySet().retainAll(used);}
    static void reconcile(ViewGroup parent,List<View> desired){
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
    }
}
