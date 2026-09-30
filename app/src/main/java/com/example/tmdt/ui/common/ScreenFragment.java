package com.example.tmdt.ui.common;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.tmdt.MainActivity;

/**
 * Base of the lesson and group-class screens, whose views are built in code with {@link Ui}.
 * Showing the screen that is already open calls {@link #refresh()} instead of replacing the fragment,
 * so tabs and filters kept in fields survive while the content follows the saved data.
 */
public abstract class ScreenFragment extends Fragment {
    protected MainActivity activity;
    protected Ui ui;
    private FrameLayout frame;

    @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        frame = new FrameLayout(requireContext()); return frame;
    }
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) { super.onViewCreated(view,state); refresh(); }

    /** Rebuilds the whole screen from the current data. */
    public void refresh() {
        if (frame == null) return;
        activity = (MainActivity) requireActivity(); ui = activity.ui;
        frame.removeAllViews(); frame.addView(build(), new FrameLayout.LayoutParams(-1,-1));
    }
    protected abstract View build();

    @Override public void onDestroyView() { super.onDestroyView(); frame = null; }
}
