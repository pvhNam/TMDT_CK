package com.example.tmdt.ui.requests;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.tmdt.CatalogState;
import com.example.tmdt.MainActivity;
import com.example.tmdt.R;
import com.example.tmdt.TutorRequestManager;
import com.example.tmdt.TutorRequestRecord;
import com.example.tmdt.databinding.FragmentManageTutorRequestsBinding;
import com.example.tmdt.databinding.ItemTutorRequestBinding;
import com.google.android.material.tabs.TabLayout;
import java.util.List;

/** UC12: the signed-in student's own open and closed tutor requests. */
public final class ManageTutorRequestsFragment extends Fragment {
    private FragmentManageTutorRequestsBinding binding;
    private TutorRequestManager manager;
    private boolean closed;

    public ManageTutorRequestsFragment() { super(R.layout.fragment_manage_tutor_requests); }
    private MainActivity host() { return (MainActivity) requireActivity(); }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding = FragmentManageTutorRequestsBinding.bind(view);
        if(com.example.tmdt.ui.common.SectionHubFragment.embedded(this))binding.toolbar.setVisibility(View.GONE);
        manager = new ViewModelProvider(requireActivity()).get(TutorRequestManager.class);
        closed = savedInstanceState == null ? manager.closedTab() : savedInstanceState.getBoolean("closed");
        binding.toolbar.setNavigationOnClickListener(v -> host().back());
        binding.createRequest.setOnClickListener(v -> host().show("post_request"));
        binding.retryRequests.setOnClickListener(v -> manager.retry());
        binding.requestTabs.addTab(binding.requestTabs.newTab().setText(R.string.requests_open), !closed);
        binding.requestTabs.addTab(binding.requestTabs.newTab().setText(R.string.requests_closed), closed);
        binding.requestTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                closed = tab.getPosition() == 1; manager.closedTab(closed);
                binding.requestsScroll.scrollTo(0, 0); render();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
        manager.changes().observe(getViewLifecycleOwner(), ignored -> render());
    }

    private void render() {
        if (binding == null) return;
        binding.requestsProgress.setVisibility(manager.loading() || manager.busy() ? View.VISIBLE : View.GONE);
        binding.requestsOffline.setVisibility(manager.offline() ? View.VISIBLE : View.GONE);
        binding.requestsError.setText(manager.error());
        binding.requestsError.setVisibility(manager.error().isEmpty() ? View.GONE : View.VISIBLE);
        binding.retryRequests.setVisibility(manager.error().isEmpty() ? View.GONE : View.VISIBLE);
        binding.retryRequests.setEnabled(!manager.busy());
        binding.requestsNotice.setText(manager.notice());
        binding.requestsNotice.setVisibility(manager.notice().isEmpty() ? View.GONE : View.VISIBLE);
        List<TutorRequestRecord> requests = manager.requests(closed);
        updateTabCount(0, R.string.requests_open, manager.requests(false).size());
        updateTabCount(1, R.string.requests_closed, manager.requests(true).size());
        binding.requestsSectionTitle.setText(closed ? R.string.requests_list_closed : R.string.requests_list_open);
        binding.requestsEmptyTitle.setText(closed ? R.string.requests_empty_closed_title : R.string.requests_empty_open_title);
        binding.requestsEmpty.setText(closed ? R.string.requests_empty_closed : R.string.requests_empty_open);
        binding.requestsEmptyPanel.setVisibility(!manager.loading() && manager.error().isEmpty() && requests.isEmpty() ? View.VISIBLE : View.GONE);
        binding.requestsEmpty.setVisibility(View.GONE);
        com.example.tmdt.ui.common.StableBindings<ItemTutorRequestBinding> rows=com.example.tmdt.ui.common.StableBindings.at(binding.requestsList);rows.begin();
        for (TutorRequestRecord request : requests) addRequest(request);
        rows.commit(binding.requestsList);
    }

    private void updateTabCount(int position, int label, int count) {
        TabLayout.Tab tab = binding.requestTabs.getTabAt(position);
        if (tab != null) tab.setText(manager.loading() || !manager.error().isEmpty()
                ? getString(label) : getString(R.string.requests_tab_count, getString(label), count));
    }

    private void addRequest(TutorRequestRecord request) {
        ItemTutorRequestBinding row = com.example.tmdt.ui.common.StableBindings.<ItemTutorRequestBinding>at(binding.requestsList)
                .item(request.id,()->ItemTutorRequestBinding.inflate(getLayoutInflater(), binding.requestsList, false));
        row.requestTitle.setText(request.title);
        row.requestStatus.setText(request.isOpen() ? R.string.requests_open : R.string.requests_closed);
        row.requestStatus.setTextColor(ContextCompat.getColor(requireContext(), request.isOpen()
                ? R.color.request_manage_green : R.color.request_manage_closed));
        row.requestStatus.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), request.isOpen()
                ? R.color.request_manage_green_pale : R.color.request_manage_closed_pale)));
        row.requestSubjectLevel.setText(getString(R.string.requests_subject_level, request.subjectName, CatalogState.level(request.level)));
        row.requestBudget.setText(getString(R.string.requests_card_budget, CatalogState.money(request.budget)));
        row.requestSchedule.setText(getString(R.string.requests_schedule_value, request.schedule));
        row.requestDescription.setText(request.description);
        row.requestApplicants.setOnClickListener(v -> host().openRequestApplicants(request.id));
        row.requestMore.setOnClickListener(v -> showActions(v, request));
        boolean editable = !manager.busy() && !manager.offline();
        row.requestMore.setEnabled(editable);
        row.requestApplicants.setEnabled(!manager.busy());
        row.requestMore.setAlpha(editable ? 1f : .45f);
        row.requestApplicants.setAlpha(manager.busy() ? .45f : 1f);
        com.example.tmdt.ui.common.ThemeColors.apply(row.getRoot());
        com.example.tmdt.ui.common.Motion.bindControls(row.getRoot());
    }

    private void showActions(View anchor, TutorRequestRecord request) {
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        menu.getMenu().add(0, 1, 0, R.string.requests_edit);
        if (request.isOpen()) menu.getMenu().add(0, 2, 1, R.string.requests_close);
        menu.getMenu().add(0, 3, 2, R.string.requests_delete);
        menu.setOnMenuItemClickListener(item -> {
            if (manager.busy() || manager.offline()) return true;
            if (item.getItemId() == 1) host().openRequestEditor(request.id);
            else if (item.getItemId() == 2) new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.requests_close_title)
                    .setMessage(getString(R.string.requests_close_confirm, request.title))
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.requests_close, (dialog, which) -> manager.close(request.id)).show();
            else if (item.getItemId() == 3) new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.requests_delete_title)
                    .setMessage(getString(R.string.requests_delete_confirm, request.title))
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.requests_delete, (dialog, which) -> manager.delete(request.id)).show();
            return true;
        });
        menu.show();
    }

    @Override public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState); outState.putBoolean("closed", closed);
    }
    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}
