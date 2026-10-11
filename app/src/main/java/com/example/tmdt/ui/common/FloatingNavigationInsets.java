package com.example.tmdt.ui.common;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.WeakHashMap;

/** Adds scrollable breathing room, while content still renders underneath the floating menu. */
public final class FloatingNavigationInsets {
    private final WeakHashMap<View,Integer> originalBottom=new WeakHashMap<>();

    public void apply(View page,int inset) {
        if(page==null)return;
        if(!padScrollers(page,inset))pad(page,inset);
    }
    private boolean padScrollers(View view,int inset) {
        if(view instanceof ScrollView || view instanceof NestedScrollView || view instanceof RecyclerView) {
            pad(view,inset);
            ((ViewGroup)view).setClipToPadding(false);
            return true;
        }
        boolean found=false;
        if(view instanceof ViewGroup) {
            ViewGroup group=(ViewGroup)view;
            for(int i=0;i<group.getChildCount();i++)found=padScrollers(group.getChildAt(i),inset)||found;
        }
        // A dynamically rebuilt page may gain a scroller after its first empty layout.
        if(found && originalBottom.containsKey(view))pad(view,0);
        return found;
    }
    private void pad(View view,int inset) {
        Integer original=originalBottom.get(view);
        if(original==null){original=view.getPaddingBottom();originalBottom.put(view,original);}
        int bottom=original+inset;
        if(bottom!=view.getPaddingBottom())view.setPadding(view.getPaddingLeft(),view.getPaddingTop(),view.getPaddingRight(),bottom);
    }
}
