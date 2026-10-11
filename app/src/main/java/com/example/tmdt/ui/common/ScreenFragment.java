package com.example.tmdt.ui.common;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import androidx.core.view.OneShotPreDrawListener;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.tmdt.Classroom;
import com.example.tmdt.MainActivity;

/**
 * Base of the lesson and group-class screens, whose views are built in code with {@link Ui}.
 * Showing the screen that is already open calls {@link #refresh()} instead of replacing the fragment,
 * so tabs and filters kept in fields survive while the content follows the saved data.
 */
public abstract class ScreenFragment extends Fragment {
    protected Classroom classroom;
    protected Ui ui;
    private FrameLayout frame;
    private int restoredScroll;
    private boolean refreshQueued;
    private final Runnable update=()->{refreshQueued=false;rebuild();};

    @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        restoredScroll=state==null?0:state.getInt("screen_scroll",0);
        frame = new FrameLayout(requireContext()); return frame;
    }
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) { super.onViewCreated(view,state); refresh(); }

    /** Rebuilds the whole screen from the current data. */
    public void refresh() {
        if (frame == null) return;
        if(frame.getChildCount()==0){rebuild();return;}
        if(!refreshQueued){refreshQueued=true;frame.postOnAnimation(update);}
    }
    private void rebuild() {
        if(frame==null)return;
        ScrollView previous=scroll(frame);
        int offset=previous==null?restoredScroll:Math.max(restoredScroll,previous.getScrollY());
        classroom = ((MainActivity) requireActivity()).classroom; ui = classroom.ui;
        View content=build();
        frame.removeAllViews(); frame.addView(content, new FrameLayout.LayoutParams(-1,-1));
        ScrollView next=scroll(content);
        restoredScroll=offset;
        if(next!=null)OneShotPreDrawListener.add(next,()->{
            next.scrollTo(0,offset);
            if(frame!=null && frame.getChildAt(0)==content)restoredScroll=0;
        });
        else restoredScroll=0;
    }
    private ScrollView scroll(View view){
        if(view instanceof ScrollView)return (ScrollView)view;
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){
            ScrollView found=scroll(((ViewGroup)view).getChildAt(i));if(found!=null)return found;
        }
        return null;
    }
    protected abstract View build();

    @Override public void onSaveInstanceState(@NonNull Bundle out){
        super.onSaveInstanceState(out);ScrollView current=frame==null?null:scroll(frame);
        out.putInt("screen_scroll",current==null?restoredScroll:Math.max(restoredScroll,current.getScrollY()));
    }
    @Override public void onDestroyView() {
        if(frame!=null)frame.removeCallbacks(update);refreshQueued=false;
        super.onDestroyView(); frame = null;
    }
}
