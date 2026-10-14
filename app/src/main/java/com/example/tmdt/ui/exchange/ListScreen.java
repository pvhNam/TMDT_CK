package com.example.tmdt.ui.exchange;

import android.view.View;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.tmdt.databinding.FragmentListScreenBinding;
import com.google.android.material.tabs.TabLayout;
import java.util.function.IntConsumer;

/** Hành vi của khung fragment_list_screen.xml: tab, danh sách và trạng thái trống/lỗi. */
public final class ListScreen {
    private ListScreen() {}

    public static void setup(FragmentListScreenBinding b, RecyclerView.Adapter<?> adapter) {
        b.list.setLayoutManager(new LinearLayoutManager(b.getRoot().getContext()));
        b.list.setAdapter(adapter);
    }
    public static void tabs(FragmentListScreenBinding b, int selected, IntConsumer onSelect, String... titles) {
        b.tabs.setVisibility(View.VISIBLE);
        b.tabs.removeAllTabs();
        for (String title : titles) b.tabs.addTab(b.tabs.newTab().setText(title), false);
        TabLayout.Tab tab = b.tabs.getTabAt(selected); if (tab != null) tab.select();
        b.tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab t) { onSelect.accept(t.getPosition()); }
            @Override public void onTabUnselected(TabLayout.Tab t) {}
            @Override public void onTabReselected(TabLayout.Tab t) {}
        });
    }
    /** Hiện dòng trạng thái (text != null) thay cho danh sách. */
    public static void state(FragmentListScreenBinding b, @Nullable String text, @Nullable String action, @Nullable Runnable onAction) {
        boolean show = text != null;
        b.state.setVisibility(show ? View.VISIBLE : View.GONE);
        b.list.setVisibility(show ? View.GONE : View.VISIBLE);
        if (!show) return;
        b.stateText.setText(text);
        b.stateAction.setVisibility(action == null ? View.GONE : View.VISIBLE);
        b.stateAction.setText(action);
        b.stateAction.setOnClickListener(v -> { if (onAction != null) onAction.run(); });
    }
}
