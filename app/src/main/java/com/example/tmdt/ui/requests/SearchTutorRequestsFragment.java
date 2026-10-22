package com.example.tmdt.ui.requests;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.tmdt.CatalogState;
import com.example.tmdt.MainActivity;
import com.example.tmdt.R;
import com.example.tmdt.TutorRequestRecord;
import com.example.tmdt.TutorRequestSearchState;
import com.example.tmdt.databinding.FragmentSearchTutorRequestsBinding;
import com.example.tmdt.databinding.ItemSearchTutorRequestBinding;
import java.util.ArrayList;
import java.util.List;

/** UC13: verified tutors browse open student requests, filtered by subject. */
public final class SearchTutorRequestsFragment extends Fragment {
    private FragmentSearchTutorRequestsBinding binding;
    private TutorRequestSearchState state;
    private CatalogState catalog;

    public SearchTutorRequestsFragment() { super(R.layout.fragment_search_tutor_requests); }
    private MainActivity host() { return (MainActivity) requireActivity(); }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding = FragmentSearchTutorRequestsBinding.bind(view);
        state = new ViewModelProvider(requireActivity()).get(TutorRequestSearchState.class);
        catalog = new ViewModelProvider(requireActivity()).get(CatalogState.class);
        binding.toolbar.setNavigationOnClickListener(v -> host().back());
        binding.requestSearchSubject.setOnClickListener(v -> chooseSubject());
        binding.resetSearchFilter.setOnClickListener(v -> selectSubject("", ""));
        binding.retrySearch.setOnClickListener(v -> state.retry());
        state.changes().observe(getViewLifecycleOwner(), ignored -> render());
        catalog.changes().observe(getViewLifecycleOwner(), ignored -> renderFilter());
    }

    private void chooseSubject() {
        if (!state.authorized() || state.checkingAccess()) return;
        host().hideKeyboard();
        List<String> ids = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        ids.add(""); labels.add(getString(R.string.request_search_all_subjects));
        for (String id : catalog.subjects().keySet()) {
            ids.add(id); labels.add(catalog.subjects().get(id));
        }
        String retainedId = state.selectedSubjectId();
        if (!retainedId.isEmpty() && !ids.contains(retainedId)) {
            ids.add(retainedId);
            labels.add(state.selectedSubjectName().isEmpty()
                    ? getString(R.string.request_search_subject) : state.selectedSubjectName());
        }
        int selected = ids.indexOf(retainedId);
        AlertDialog.Builder dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.request_search_subject)
                .setSingleChoiceItems(labels.toArray(new String[0]), Math.max(0, selected), (choice, which) -> {
                    selectSubject(ids.get(which), which == 0 ? "" : labels.get(which));
                    choice.dismiss();
                })
                .setNegativeButton(R.string.cancel, null);
        if (catalog.subjects().isEmpty()) {
            dialog.setPositiveButton(R.string.request_search_reload_subjects, (choice, which) -> catalog.reload());
        }
        dialog.show();
    }

    private void selectSubject(String id, String name) {
        binding.searchRequestsScroll.scrollTo(0, 0);
        state.selectSubject(id, name);
    }

    private void renderFilter() {
        if (binding == null) return;
        String label = state.selectedSubjectId().isEmpty()
                ? getString(R.string.request_search_all_subjects) : state.selectedSubjectName();
        if (label.isEmpty()) label = catalog.subjects().get(state.selectedSubjectId());
        binding.requestSearchSubject.setText(label == null || label.isEmpty()
                ? getString(R.string.request_search_choose_subject) : label);
        binding.resetSearchFilter.setEnabled(state.authorized() && !state.selectedSubjectId().isEmpty());
    }

    private void render() {
        if (binding == null) return;
        boolean checking = state.checkingAccess();
        boolean allowed = !checking && state.authorized();
        boolean loading = checking || (allowed && state.loading());
        boolean failed = !state.error().isEmpty();
        binding.searchFilters.setVisibility(allowed ? View.VISIBLE : View.GONE);
        binding.searchPermissionPanel.setVisibility(!checking && !allowed ? View.VISIBLE : View.GONE);
        binding.searchRequestsProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.searchRequestsOffline.setVisibility(allowed && state.offline() ? View.VISIBLE : View.GONE);
        binding.searchRequestsError.setText(state.error());
        binding.searchRequestsError.setVisibility(!checking && failed ? View.VISIBLE : View.GONE);
        binding.retrySearch.setVisibility(!checking && (!allowed || failed) ? View.VISIBLE : View.GONE);
        binding.searchRequestsCount.setVisibility(allowed && !state.loading() && !failed ? View.VISIBLE : View.GONE);
        List<TutorRequestRecord> requests = allowed ? state.requests() : java.util.Collections.emptyList();
        binding.searchRequestsCount.setText(getString(R.string.request_search_count, requests.size()));
        binding.searchRequestsEmpty.setVisibility(allowed && !loading && !failed && requests.isEmpty() ? View.VISIBLE : View.GONE);
        int scrollY = binding.searchRequestsScroll.getScrollY();
        binding.searchRequestsList.removeAllViews();
        for (TutorRequestRecord request : requests) {
            if (!request.isOpen()) continue;
            ItemSearchTutorRequestBinding row = ItemSearchTutorRequestBinding.inflate(
                    getLayoutInflater(), binding.searchRequestsList, false);
            row.searchRequestTitle.setText(request.title);
            row.searchRequestSubjectLevel.setText(getString(R.string.request_search_subject_level,
                    request.subjectName, CatalogState.level(request.level)));
            row.searchRequestBudget.setText(getString(R.string.request_search_budget_value, CatalogState.money(request.budget)));
            row.searchRequestSchedule.setText(getString(R.string.request_search_schedule_value, request.schedule));
            row.searchRequestDescription.setText(request.description);
            row.viewRequestDetails.setOnClickListener(v -> openDetails(request.id));
            binding.searchRequestsList.addView(row.getRoot());
        }
        binding.searchRequestsScroll.post(() -> {
            if (binding != null) binding.searchRequestsScroll.scrollTo(0, scrollY);
        });
        renderFilter();
    }

    private void openDetails(String id) {
        if (!state.authorized() || getParentFragmentManager().isStateSaved()) return;
        if (getParentFragmentManager().findFragmentByTag("tutor_request_details") == null) {
            TutorRequestDetailsDialog.newInstance(id).show(getParentFragmentManager(), "tutor_request_details");
        }
    }
    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}
