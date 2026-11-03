package com.example.tmdt.ui.requests;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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
        binding.requestsSectionCaption.setText(closed ? R.string.requests_caption_closed : R.string.requests_caption_open);
        binding.requestsEmptyTitle.setText(closed ? R.string.requests_empty_closed_title : R.string.requests_empty_open_title);
        binding.requestsEmpty.setText(closed ? R.string.requests_empty_closed : R.string.requests_empty_open);
        binding.requestsEmptyPanel.setVisibility(!manager.loading() && manager.error().isEmpty() && requests.isEmpty() ? View.VISIBLE : View.GONE);
        int scrollY = binding.requestsScroll.getScrollY();
        binding.requestsList.removeAllViews();
        for (TutorRequestRecord request : requests) addRequest(request);
        binding.requestsScroll.post(() -> {
            if (binding != null) binding.requestsScroll.scrollTo(0, scrollY);
        });
    }

    private void updateTabCount(int position, int label, int count) {
        TabLayout.Tab tab = binding.requestTabs.getTabAt(position);
        if (tab != null) tab.setText(manager.loading() || !manager.error().isEmpty()
                ? getString(label) : getString(R.string.requests_tab_count, getString(label), count));
    }

    private void addRequest(TutorRequestRecord request) {
        ItemTutorRequestBinding row = ItemTutorRequestBinding.inflate(getLayoutInflater(), binding.requestsList, false);
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
        row.editRequest.setOnClickListener(v -> host().openRequestEditor(request.id));
        row.requestApplicants.setOnClickListener(v -> host().openRequestApplicants(request.id));
        row.closeRequest.setVisibility(request.isOpen() ? View.VISIBLE : View.GONE);
        row.closeRequest.setOnClickListener(v -> new AlertDialog.Builder(requireContext())
                .setTitle(R.string.requests_close_title)
                .setMessage(getString(R.string.requests_close_confirm, request.title))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.requests_close, (dialog, which) -> manager.close(request.id)).show());
        row.deleteRequest.setContentDescription(getString(R.string.requests_card_delete, request.title));
        row.deleteRequest.setOnClickListener(v -> new AlertDialog.Builder(requireContext())
                .setTitle(R.string.requests_delete_title)
                .setMessage(getString(R.string.requests_delete_confirm, request.title))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.requests_delete, (dialog, which) -> manager.delete(request.id)).show());
        boolean editable = !manager.busy() && !manager.offline();
        row.editRequest.setEnabled(editable);
        row.closeRequest.setEnabled(editable);
        row.deleteRequest.setEnabled(editable);
        row.requestApplicants.setEnabled(!manager.busy());
        row.editRequest.setAlpha(editable ? 1f : .45f);
        row.closeRequest.setAlpha(editable ? 1f : .45f);
        row.deleteRequest.setAlpha(editable ? 1f : .45f);
        row.requestApplicants.setAlpha(manager.busy() ? .45f : 1f);
        binding.requestsList.addView(row.getRoot());
    }

    @Override public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState); outState.putBoolean("closed", closed);
    }
    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}
