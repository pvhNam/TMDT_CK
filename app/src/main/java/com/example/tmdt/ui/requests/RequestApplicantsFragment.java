package com.example.tmdt.ui.requests;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.tmdt.MainActivity;
import com.example.tmdt.R;
import com.example.tmdt.TutorApplication;
import com.example.tmdt.TutorRequestManager;
import com.example.tmdt.TutorRequestRecord;
import com.example.tmdt.databinding.FragmentRequestApplicantsBinding;
import com.example.tmdt.databinding.ItemTutorApplicationBinding;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

/** UC12: read the real applications attached to an owned request. */
public final class RequestApplicantsFragment extends Fragment {
    private FragmentRequestApplicantsBinding binding;
    private TutorRequestManager manager;
    private String requestId;

    public RequestApplicantsFragment() { super(R.layout.fragment_request_applicants); }
    public static RequestApplicantsFragment newInstance(String id) {
        RequestApplicantsFragment fragment = new RequestApplicantsFragment();
        Bundle args = new Bundle(); args.putString("request_id", id); fragment.setArguments(args);
        return fragment;
    }
    private MainActivity host() { return (MainActivity) requireActivity(); }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding = FragmentRequestApplicantsBinding.bind(view);
        requestId = requireArguments().getString("request_id", "");
        manager = new ViewModelProvider(requireActivity()).get(TutorRequestManager.class);
        binding.toolbar.setNavigationOnClickListener(v -> host().back());
        binding.retryApplicants.setOnClickListener(v -> manager.watchApplicants(requestId));
        manager.watchApplicants(requestId);
        manager.changes().observe(getViewLifecycleOwner(), ignored -> render());
    }

    private void render() {
        if (binding == null) return;
        TutorRequestRecord request = manager.request(requestId);
        binding.applicantsRequestTitle.setText(request == null ? getString(R.string.requests_applicants_caption) : request.title);
        binding.applicantsProgress.setVisibility(manager.applicantsLoading() ? View.VISIBLE : View.GONE);
        binding.applicantsOffline.setVisibility(manager.applicantsOffline() ? View.VISIBLE : View.GONE);
        binding.applicantsError.setText(manager.applicantsError());
        binding.applicantsError.setVisibility(manager.applicantsError().isEmpty() ? View.GONE : View.VISIBLE);
        binding.retryApplicants.setVisibility(manager.applicantsError().isEmpty() ? View.GONE : View.VISIBLE);
        binding.applicantsEmpty.setVisibility(!manager.applicantsLoading() && manager.applicantsError().isEmpty()
                && manager.applicants().isEmpty() ? View.VISIBLE : View.GONE);
        int scrollY = binding.applicantsScroll.getScrollY();
        binding.applicantsList.removeAllViews();
        for (TutorApplication application : manager.applicants()) {
            ItemTutorApplicationBinding row = ItemTutorApplicationBinding.inflate(getLayoutInflater(), binding.applicantsList, false);
            row.applicantName.setText(application.tutorName.isEmpty() ? getString(R.string.requests_applicant_unnamed) : application.tutorName);
            row.applicantMessage.setText(application.introduction.isEmpty() ? getString(R.string.requests_applicant_no_message) : application.introduction);
            row.applicantStatus.setText(status(application.status));
            row.applicantDate.setVisibility(application.createdAtMillis > 0 ? View.VISIBLE : View.GONE);
            if (application.createdAtMillis > 0) {
                String date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT,
                        Locale.forLanguageTag("vi-VN")).format(new Date(application.createdAtMillis));
                row.applicantDate.setText(getString(R.string.requests_application_date, date));
            }
            binding.applicantsList.addView(row.getRoot());
        }
        binding.applicantsScroll.post(() -> {
            if (binding != null) binding.applicantsScroll.scrollTo(0, scrollY);
        });
    }

    private String status(String status) {
        switch (status) {
            case "ACCEPTED": return getString(R.string.requests_application_accepted);
            case "REJECTED": return getString(R.string.requests_application_rejected);
            case "WITHDRAWN": return getString(R.string.requests_application_withdrawn);
            case "":
            case "PENDING":
            case "APPLIED": return getString(R.string.requests_application_pending);
            default: return getString(R.string.requests_application_status, status);
        }
    }
    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}
