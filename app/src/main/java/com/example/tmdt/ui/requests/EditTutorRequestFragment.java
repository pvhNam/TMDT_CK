package com.example.tmdt.ui.requests;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.tmdt.CatalogState;
import com.example.tmdt.MainActivity;
import com.example.tmdt.R;
import com.example.tmdt.TutorRequest;
import com.example.tmdt.TutorRequestManager;
import com.example.tmdt.TutorRequestRecord;
import com.example.tmdt.databinding.FragmentEditTutorRequestBinding;
import java.util.ArrayList;
import java.util.List;

/** UC12 edit form; draft data lives in the activity's SavedStateHandle. */
public final class EditTutorRequestFragment extends Fragment {
    private FragmentEditTutorRequestBinding binding;
    private TutorRequestManager manager;
    private CatalogState catalog;
    private String requestId;
    private boolean initialized;

    public EditTutorRequestFragment() { super(R.layout.fragment_edit_tutor_request); }
    public static EditTutorRequestFragment newInstance(String id) {
        EditTutorRequestFragment fragment = new EditTutorRequestFragment();
        Bundle args = new Bundle(); args.putString("request_id", id); fragment.setArguments(args);
        return fragment;
    }
    private MainActivity host() { return (MainActivity) requireActivity(); }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding = FragmentEditTutorRequestBinding.bind(view);
        initialized = false;
        requestId = requireArguments().getString("request_id", "");
        manager = new ViewModelProvider(requireActivity()).get(TutorRequestManager.class);
        catalog = new ViewModelProvider(requireActivity()).get(CatalogState.class);
        binding.toolbar.setNavigationOnClickListener(v -> cancel());
        binding.cancelEdit.setOnClickListener(v -> cancel());
        binding.saveRequest.setOnClickListener(v -> save());
        binding.retryEdit.setOnClickListener(v -> retry());
        manager.changes().observe(getViewLifecycleOwner(), ignored -> render());
    }

    private void initialize() {
        input(binding.requestTitle, "title"); input(binding.requestBudget, "budget");
        input(binding.requestSchedule, "schedule"); input(binding.requestDescription, "description");
        binding.requestSubject.setText(manager.draft("subject_name"));
        String level = manager.draft("level");
        binding.requestLevel.setText(level.isEmpty() ? "" : CatalogState.level(level));
        binding.requestSubject.setOnClickListener(v -> chooseSubject());
        binding.requestLevel.setOnClickListener(v -> {
            host().hideKeyboard();
            String[] labels = new String[TutorRequest.LEVELS.length];
            for (int i = 0; i < labels.length; i++) labels[i] = CatalogState.level(TutorRequest.LEVELS[i]);
            new AlertDialog.Builder(requireContext()).setTitle(R.string.request_level).setItems(labels, (dialog, which) -> {
                manager.draft("level", TutorRequest.LEVELS[which]);
                binding.requestLevel.setText(labels[which]); binding.requestLevel.setError(null);
            }).show();
        });
        initialized = true;
    }

    private void input(EditText input, String key) {
        input.setText(manager.draft(key));
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                manager.draft(key, s.toString()); input.setError(null);
            }
            @Override public void afterTextChanged(Editable value) {}
        });
    }

    private void chooseSubject() {
        host().hideKeyboard();
        if (catalog.subjects().isEmpty()) {
            new AlertDialog.Builder(requireContext()).setTitle(R.string.request_subject)
                    .setMessage(R.string.request_subject_unavailable).setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.refresh_catalog, (dialog, which) -> catalog.reload()).show();
            return;
        }
        List<String> ids = new ArrayList<>(catalog.subjects().keySet());
        String[] names = new String[ids.size()];
        for (int i = 0; i < ids.size(); i++) names[i] = catalog.subjects().get(ids.get(i));
        new AlertDialog.Builder(requireContext()).setTitle(R.string.request_subject).setItems(names, (dialog, which) -> {
            manager.draft("subject_id", ids.get(which)); manager.draft("subject_name", names[which]);
            binding.requestSubject.setText(names[which]); binding.requestSubject.setError(null);
        }).show();
    }

    private void save() {
        if (!initialized || manager.busy()) return;
        TutorRequest request = new TutorRequest(manager.draft("title"), manager.draft("subject_id"), manager.draft("subject_name"),
                manager.draft("level"), manager.draft("budget"), manager.draft("schedule"), manager.draft("description"));
        String invalid = request.invalidField();
        EditText input;
        switch (invalid) {
            case "title": input = binding.requestTitle; break;
            case "subject": input = binding.requestSubject; break;
            case "level": input = binding.requestLevel; break;
            case "budget": input = binding.requestBudget; break;
            case "schedule": input = binding.requestSchedule; break;
            case "description": input = binding.requestDescription; break;
            default: input = null;
        }
        if (input != null) {
            input.setError(getString("budget".equals(invalid) ? R.string.request_budget_error : R.string.request_required));
            input.requestFocus(); return;
        }
        TutorRequestRecord original = manager.request(requestId);
        boolean unchangedSubject = original != null && request.subjectId.equals(original.subjectId)
                && request.subjectName.equals(original.subjectName);
        if (!unchangedSubject && !request.subjectName.equals(catalog.subjects().get(request.subjectId))) {
            binding.requestSubject.setError(getString(R.string.request_subject_unavailable)); return;
        }
        host().hideKeyboard(); manager.saveEdit(requestId, request);
    }

    private void render() {
        if (binding == null) return;
        if (requestId.equals(manager.editSavedId())) {
            manager.consumeEditSaved(); host().show("my_requests"); return;
        }
        TutorRequestRecord record = manager.request(requestId);
        if (!initialized && record != null && record.updatedAt != null && manager.startEdit(requestId)) initialize();
        String error = manager.error();
        if (!manager.loading() && record == null && error.isEmpty()) error = getString(R.string.requests_unavailable);
        if (record != null && record.updatedAt == null && error.isEmpty()) error = getString(R.string.requests_edit_syncing);
        binding.editError.setText(error);
        binding.editError.setVisibility(error.isEmpty() ? View.GONE : View.VISIBLE);
        binding.retryEdit.setVisibility(!error.isEmpty() ? View.VISIBLE : View.GONE);
        binding.retryEdit.setText(initialized ? R.string.requests_reload : R.string.requests_retry);
        binding.retryEdit.setEnabled(!manager.busy());
        binding.editOffline.setVisibility(manager.offline() ? View.VISIBLE : View.GONE);
        binding.editProgress.setVisibility(manager.loading() || manager.busy() ? View.VISIBLE : View.GONE);
        binding.editForm.setVisibility(initialized ? View.VISIBLE : View.GONE);
        enable(binding.editForm, !manager.busy() && !manager.offline() && manager.request(requestId) != null);
        binding.cancelEdit.setEnabled(!manager.busy());
        binding.saveRequest.setText(manager.busy() ? R.string.requests_saving : R.string.requests_save);
    }

    private void retry() {
        if (manager.busy()) return;
        if (!initialized) { manager.retry(); return; }
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.requests_reload_title)
                .setMessage(R.string.requests_reload_confirm)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.requests_reload, (dialog, which) -> {
                    manager.resetEdit(); host().reloadRequestEditor(requestId);
                }).show();
    }
    private void cancel() {
        if (manager.busy()) return;
        manager.resetEdit(); host().back();
    }
    private void enable(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) enable(group.getChildAt(i), enabled);
        }
    }
    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}
