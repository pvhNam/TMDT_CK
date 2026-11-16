package com.example.tmdt.ui.requests;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import com.example.tmdt.TutorApplication;
import com.example.tmdt.TutorApplicationState;
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

/** UC13 details and UC14 application, bound to the current tutor account. */
public final class TutorRequestDetailsDialog extends DialogFragment {
    private DialogTutorRequestDetailsBinding binding;
    private TutorRequestSearchState state;
    private String requestId;
    private TutorApplicationState applications;

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
        applications = new ViewModelProvider(requireActivity()).get(TutorApplicationState.class);
        applications.watch(requestId);
        binding.applicationMessage.setText(applications.draft());
        binding.applicationMessage.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int count,int after) {}
            public void onTextChanged(CharSequence s,int start,int before,int count) { applications.draft(s.toString()); }
            public void afterTextChanged(Editable s) {}
        });
        binding.applyRequest.setOnClickListener(v -> applications.submit());
        binding.retryApplication.setOnClickListener(v -> { applications.watch(requestId); state.retry(); });
        applications.changes().observe(getViewLifecycleOwner(), ignored -> render());
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
        renderApplication(request, available, allowed);
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

    private void renderApplication(TutorRequestRecord request, boolean available, boolean allowed) {
        if (!binding.applicationMessage.getText().toString().equals(applications.draft())) {
            binding.applicationMessage.setText(applications.draft());
            binding.applicationMessage.setSelection(binding.applicationMessage.length());
        }
        TutorApplication application = applications.application();
        boolean own = applications.ownRequest(request);
        boolean showForm = allowed && available && !own && application == null;
        binding.applicationPanel.setVisibility(allowed || application != null ? View.VISIBLE : View.GONE);
        binding.applicationMessage.setVisibility(showForm ? View.VISIBLE : View.GONE);
        binding.applyRequest.setVisibility(showForm ? View.VISIBLE : View.GONE);
        binding.applicationMessage.setEnabled(!applications.busy());
        binding.applyRequest.setEnabled(showForm && !applications.busy() && !applications.loading()
                && !applications.offline() && !state.offline());
        binding.applicationProgress.setVisibility(applications.loading() || applications.busy() ? View.VISIBLE : View.GONE);
        String status = own ? getString(R.string.application_own_request) : getString(R.string.application_caption);
        if (application != null) {
            status = getString("ACCEPTED".equals(application.status) ? R.string.application_selected : R.string.application_pending)
                    + "\n\n" + application.introduction;
        } else if (!applications.notice().isEmpty()) status = applications.notice();
        binding.applicationStatus.setText(status);
        binding.applicationError.setText(applications.error());
        binding.applicationError.setVisibility(applications.error().isEmpty() ? View.GONE : View.VISIBLE);
        binding.retryApplication.setVisibility(!applications.error().isEmpty() || applications.offline() ? View.VISIBLE : View.GONE);
        binding.closeRequestDetails.setEnabled(!applications.busy());
        setCancelable(!applications.busy());
    }

    private void clearDetails() {
        binding.requestDetailTitle.setText(""); binding.requestDetailSubject.setText("");
        binding.requestDetailLevel.setText(""); binding.requestDetailBudget.setText("");
        binding.requestDetailSchedule.setText(""); binding.requestDetailDescription.setText("");
    }
    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}
