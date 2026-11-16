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
import com.example.tmdt.TutorRequestState;
import com.example.tmdt.databinding.FragmentPostTutorRequestBinding;
import java.util.ArrayList;
import java.util.List;

public final class PostTutorRequestFragment extends Fragment {
    private FragmentPostTutorRequestBinding binding;
    private TutorRequestState state;
    private CatalogState catalog;
    public PostTutorRequestFragment() { super(R.layout.fragment_post_tutor_request); }
    private MainActivity host() { return (MainActivity) requireActivity(); }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding = FragmentPostTutorRequestBinding.bind(view);
        state = new ViewModelProvider(requireActivity()).get(TutorRequestState.class);
        catalog = new ViewModelProvider(requireActivity()).get(CatalogState.class);
        binding.toolbar.setNavigationOnClickListener(v -> host().back());
        input(binding.requestTitle, "title"); input(binding.requestBudget, "budget");
        input(binding.requestSchedule, "schedule"); input(binding.requestDescription, "description");
        binding.requestSubject.setText(state.draft("subject_name"));
        binding.requestLevel.setText(state.draft("level").isEmpty() ? "" : CatalogState.level(state.draft("level")));
        binding.requestSubject.setOnClickListener(v -> chooseSubject());
        binding.requestLevel.setOnClickListener(v -> {
            String[] labels = new String[TutorRequest.LEVELS.length];
            for (int i = 0; i < labels.length; i++) labels[i] = CatalogState.level(TutorRequest.LEVELS[i]);
            new AlertDialog.Builder(requireContext()).setTitle(R.string.request_level).setItems(labels, (dialog, which) -> {
                state.draft("level", TutorRequest.LEVELS[which]);
                binding.requestLevel.setText(labels[which]); binding.requestLevel.setError(null);
            }).show();
        });
        binding.publishRequest.setOnClickListener(v -> publish());
        binding.newRequest.setOnClickListener(v -> { state.reset(); clearInputs(); });
        binding.requestHome.setOnClickListener(v -> host().show("home"));
        binding.manageRequests.setOnClickListener(v -> host().openMyRequests("post_request"));
        state.changes().observe(getViewLifecycleOwner(), ignored -> render());
    }
    private void input(EditText input, String key) {
        input.setText(state.draft(key));
        input.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) { state.draft(key, s.toString()); input.setError(null); }
            public void afterTextChanged(Editable value) {}
        });
    }
    private void chooseSubject() {
        host().hideKeyboard();
        if (catalog.subjects().isEmpty()) {
            new AlertDialog.Builder(requireContext()).setTitle(R.string.request_subject)
                    .setMessage(R.string.request_subject_unavailable).setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.refresh_catalog, (d, w) -> catalog.reload()).show();
            return;
        }
        List<String> ids = new ArrayList<>(catalog.subjects().keySet());
        String[] names = new String[ids.size()];
        for (int i = 0; i < ids.size(); i++) names[i] = catalog.subjects().get(ids.get(i));
        new AlertDialog.Builder(requireContext()).setTitle(R.string.request_subject).setItems(names, (d, which) -> {
            state.draft("subject_id", ids.get(which)); state.draft("subject_name", names[which]);
            binding.requestSubject.setText(names[which]); binding.requestSubject.setError(null);
        }).show();
    }
    private void publish() {
        TutorRequest request = new TutorRequest(state.draft("title"), state.draft("subject_id"), state.draft("subject_name"),
                state.draft("level"), state.draft("budget"), state.draft("schedule"), state.draft("description"));
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
        if (!request.subjectName.equals(catalog.subjects().get(request.subjectId))) {
            binding.requestSubject.setError(getString(R.string.request_subject_unavailable)); return;
        }
        host().hideKeyboard(); state.publish(request);
    }
    private void render() {
        binding.requestForm.setVisibility(state.published() ? View.GONE : View.VISIBLE);
        binding.requestSuccess.setVisibility(state.published() ? View.VISIBLE : View.GONE);
        binding.requestSummary.setText(state.publishedText());
        binding.requestError.setText(state.error());
        binding.requestError.setVisibility(state.error().isEmpty() ? View.GONE : View.VISIBLE);
        binding.requestProgress.setVisibility(state.busy() ? View.VISIBLE : View.GONE);
        binding.publishRequest.setText(state.busy() ? R.string.request_publishing : R.string.request_publish);
        enable(binding.requestForm, !state.busy());
    }
    private void enable(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) enable(((ViewGroup) view).getChildAt(i), enabled);
    }
    private void clearInputs() {
        binding.requestTitle.setText(""); binding.requestSubject.setText(""); binding.requestLevel.setText("");
        binding.requestBudget.setText(""); binding.requestSchedule.setText(""); binding.requestDescription.setText("");
        binding.requestScroll.smoothScrollTo(0, 0);
    }
    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}
