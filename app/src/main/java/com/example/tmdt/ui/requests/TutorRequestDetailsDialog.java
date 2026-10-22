package com.example.tmdt.ui.requests;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.tmdt.CatalogState;
import com.example.tmdt.R;
import com.example.tmdt.TutorRequestRecord;
import com.example.tmdt.TutorRequestSearchState;
import com.example.tmdt.databinding.DialogTutorRequestDetailsBinding;

/** Read-only UC13 details that track the current open-request query and access state. */
public final class TutorRequestDetailsDialog extends DialogFragment {
    private DialogTutorRequestDetailsBinding binding;
    private TutorRequestSearchState state;
    private String requestId;

    public TutorRequestDetailsDialog() { super(R.layout.dialog_tutor_request_details); }
    public static TutorRequestDetailsDialog newInstance(String id) {
        TutorRequestDetailsDialog dialog = new TutorRequestDetailsDialog();
        Bundle args = new Bundle(); args.putString("request_id", id); dialog.setArguments(args);
        return dialog;
    }

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); setStyle(STYLE_NO_TITLE, 0);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding = DialogTutorRequestDetailsBinding.bind(view);
        requestId = requireArguments().getString("request_id", "");
        state = new ViewModelProvider(requireActivity()).get(TutorRequestSearchState.class);
        binding.closeRequestDetails.setOnClickListener(v -> dismiss());
        binding.retryRequestDetails.setOnClickListener(v -> state.retry());
        state.changes().observe(getViewLifecycleOwner(), ignored -> render());
    }

    @Override public void onStart() {
        super.onStart();
        Window window = requireDialog().getWindow();
        if (window != null) {
            int margin = Math.round(32 * getResources().getDisplayMetrics().density);
            window.setLayout(getResources().getDisplayMetrics().widthPixels - margin,
                    Math.round(getResources().getDisplayMetrics().heightPixels * 0.85f));
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private void render() {
        if (binding == null) return;
        boolean allowed = !state.checkingAccess() && state.authorized();
        TutorRequestRecord request = allowed ? state.request(requestId) : null;
        boolean available = request != null && request.isOpen();
        boolean loading = state.checkingAccess() || (allowed && state.loading() && !available);
        binding.requestDetailsProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.requestDetailsContent.setVisibility(available ? View.VISIBLE : View.GONE);
        binding.requestDetailsOffline.setVisibility(allowed && state.offline() ? View.VISIBLE : View.GONE);
        binding.requestDetailsUnavailable.setVisibility(!available && !loading ? View.VISIBLE : View.GONE);
        String problem = !state.error().isEmpty() ? state.error()
                : !allowed ? getString(R.string.request_search_permission_caption)
                : getString(R.string.request_search_details_unavailable);
        binding.requestDetailsUnavailable.setText(problem);
        binding.retryRequestDetails.setVisibility(!available && !loading ? View.VISIBLE : View.GONE);
        if (!available) {
            clearDetails();
            return;
        }
        binding.requestDetailTitle.setText(request.title);
        binding.requestDetailSubject.setText(request.subjectName);
        binding.requestDetailLevel.setText(CatalogState.level(request.level));
        binding.requestDetailBudget.setText(getString(R.string.request_search_budget_per_lesson, CatalogState.money(request.budget)));
        binding.requestDetailSchedule.setText(request.schedule);
        binding.requestDetailDescription.setText(request.description);
    }

    private void clearDetails() {
        binding.requestDetailTitle.setText(""); binding.requestDetailSubject.setText("");
        binding.requestDetailLevel.setText(""); binding.requestDetailBudget.setText("");
        binding.requestDetailSchedule.setText(""); binding.requestDetailDescription.setText("");
    }
    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}
