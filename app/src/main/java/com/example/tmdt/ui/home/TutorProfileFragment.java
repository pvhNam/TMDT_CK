package com.example.tmdt.ui.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.tmdt.CatalogState;
import com.example.tmdt.MainActivity;
import com.example.tmdt.R;
import com.example.tmdt.Tutor;
import com.example.tmdt.databinding.FragmentTutorProfileBinding;
import com.example.tmdt.databinding.ItemTutorProfileOfferingBinding;
import java.util.ArrayList;
import java.util.List;

/**
 * UC8: View Tutor Profile (Xem hồ sơ gia sư)
 * Displays tutor experience, bio/intro, teaching formats, pricing/offerings, and student reviews.
 */
public final class TutorProfileFragment extends Fragment {
    private static final String ARG_TUTOR_ID = "tutor_id";

    private FragmentTutorProfileBinding binding;
    private String tutorId = "";
    private SharedPreferences preferences;

    public static TutorProfileFragment newInstance(String tutorId) {
        TutorProfileFragment fragment = new TutorProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TUTOR_ID, tutorId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            tutorId = getArguments().getString(ARG_TUTOR_ID, "");
        }
        if (tutorId.isEmpty() && getActivity() instanceof MainActivity) {
            tutorId = ((MainActivity) getActivity()).classroom.tutorProfileId;
        }
        preferences = requireContext().getSharedPreferences("tutor_demo", Context.MODE_PRIVATE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        binding = FragmentTutorProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MainActivity host = (MainActivity) requireActivity();
        CatalogState catalog = new ViewModelProvider(host).get(CatalogState.class);

        binding.ibBack.setOnClickListener(v -> host.back());
        binding.btnEmptyBack.setOnClickListener(v -> host.back());

        catalog.changes().observe(getViewLifecycleOwner(), ignored -> render(host, catalog));
        render(host, catalog);
    }

    private void render(MainActivity host, CatalogState catalog) {
        if (binding == null) return;

        if (tutorId.isEmpty() && host.classroom != null) {
            tutorId = host.classroom.tutorProfileId;
        }

        CatalogState.Teacher teacher = catalog.teacher(tutorId);
        if (teacher == null) {
            binding.llEmptyState.setVisibility(View.VISIBLE);
            binding.flMainContent.setVisibility(View.GONE);
            return;
        }

        binding.llEmptyState.setVisibility(View.GONE);
        binding.flMainContent.setVisibility(View.VISIBLE);

        final CatalogState.Teacher finalTeacher = teacher;

        binding.avatar.bind(finalTeacher.id, finalTeacher.name);
        binding.tvName.setText(finalTeacher.name);

        String education = finalTeacher.education == null ? "" : finalTeacher.education;
        String school = finalTeacher.school == null ? "" : finalTeacher.school;
        if (!education.isEmpty() && !school.isEmpty()) {
            binding.tvEducation.setText(getString(R.string.tutor_profile_education_label, education, school));
            binding.tvEducation.setVisibility(View.VISIBLE);
        } else if (!school.isEmpty()) {
            binding.tvEducation.setText(school);
            binding.tvEducation.setVisibility(View.VISIBLE);
        } else if (!education.isEmpty()) {
            binding.tvEducation.setText(education);
            binding.tvEducation.setVisibility(View.VISIBLE);
        } else {
            binding.tvEducation.setVisibility(View.GONE);
        }

        String region = finalTeacher.region == null || finalTeacher.region.trim().isEmpty() ? "Toàn quốc" : finalTeacher.region;
        binding.tvRegion.setText(getString(R.string.tutor_profile_region_label, region));

        binding.tvStatRating.setText("—");
        long years = finalTeacher.experience;
        binding.tvStatExperience.setText(years + " năm");
        binding.tvStatStudents.setText("—");

        // Giới thiệu
        String intro = finalTeacher.introduction == null || finalTeacher.introduction.trim().isEmpty()
                ? getString(R.string.tutor_profile_no_introduction)
                : finalTeacher.introduction;
        binding.tvIntroduction.setText(intro);

        // Hình thức dạy
        setupTeachingModes(finalTeacher.mode);

        // Offerings & Pricing
        setupOfferings(host, catalog, finalTeacher);

        // Đánh giá của học viên
        binding.llReviews.removeAllViews();
        binding.tvReviewsSummaryCount.setText(R.string.tutor_profile_no_reviews);

        // Favorite Button
        setupFavorite(host, finalTeacher);

        // Footer Action Buttons
        binding.btnMessage.setOnClickListener(v -> host.classroom.message(finalTeacher.name));
        binding.btnBook.setOnClickListener(v -> book(host, catalog, finalTeacher));
    }

    private void setupTeachingModes(String mode) {
        boolean onlineAllowed = "TRUC_TUYEN".equals(mode) || "CA_HAI".equals(mode) || mode == null || mode.isEmpty();
        boolean homeAllowed = "TAI_NHA".equals(mode) || "CA_HAI".equals(mode) || mode == null || mode.isEmpty();

        // Online mode
        if (onlineAllowed) {
            binding.cardModeOnline.setBackgroundResource(R.drawable.bg_mode_card_active);
            binding.ivModeOnline.setImageTintList(ContextCompat.getColorStateList(requireContext(), R.color.tutor_blue));
            binding.tvModeOnlineTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.tutor_ink));
            binding.tvModeOnlineBadge.setText(R.string.tutor_profile_mode_supported);
            binding.tvModeOnlineBadge.setTextColor(0xFF009D70);
            binding.tvModeOnlineBadge.setBackgroundResource(R.drawable.bg_verified_pill);
        } else {
            binding.cardModeOnline.setBackgroundResource(R.drawable.bg_mode_card_inactive);
            binding.ivModeOnline.setImageTintList(ContextCompat.getColorStateList(requireContext(), R.color.tutor_muted));
            binding.tvModeOnlineTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.tutor_muted));
            binding.tvModeOnlineBadge.setText(R.string.tutor_profile_mode_not_supported);
            binding.tvModeOnlineBadge.setTextColor(0xFF8E9BAE);
            binding.tvModeOnlineBadge.setBackgroundResource(R.drawable.bg_account_note);
        }

        // Home mode
        if (homeAllowed) {
            binding.cardModeHome.setBackgroundResource(R.drawable.bg_mode_card_active);
            binding.ivModeHome.setImageTintList(ContextCompat.getColorStateList(requireContext(), R.color.tutor_blue));
            binding.tvModeHomeTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.tutor_ink));
            binding.tvModeHomeBadge.setText(R.string.tutor_profile_mode_supported);
            binding.tvModeHomeBadge.setTextColor(0xFF009D70);
            binding.tvModeHomeBadge.setBackgroundResource(R.drawable.bg_verified_pill);
        } else {
            binding.cardModeHome.setBackgroundResource(R.drawable.bg_mode_card_inactive);
            binding.ivModeHome.setImageTintList(ContextCompat.getColorStateList(requireContext(), R.color.tutor_muted));
            binding.tvModeHomeTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.tutor_muted));
            binding.tvModeHomeBadge.setText(R.string.tutor_profile_mode_not_supported);
            binding.tvModeHomeBadge.setTextColor(0xFF8E9BAE);
            binding.tvModeHomeBadge.setBackgroundResource(R.drawable.bg_account_note);
        }
    }

    private void setupOfferings(MainActivity host, CatalogState catalog, CatalogState.Teacher teacher) {
        binding.llOfferings.removeAllViews();
        if (teacher.offerings.isEmpty()) {
            binding.tvEmptyOfferings.setVisibility(View.VISIBLE);
            return;
        }
        binding.tvEmptyOfferings.setVisibility(View.GONE);

        for (CatalogState.Offering offering : teacher.offerings) {
            ItemTutorProfileOfferingBinding itemBinding = ItemTutorProfileOfferingBinding.inflate(
                    getLayoutInflater(), binding.llOfferings, false);

            String subjectName = catalog.subjects().get(offering.subjectId);
            if (subjectName == null) subjectName = "Môn học";
            String levelName = CatalogState.level(offering.level);

            itemBinding.tvSubjectLevel.setText(getString(R.string.subject_level, subjectName, levelName));
            itemBinding.tvOfferingPrice.setText(getString(R.string.offering_price, CatalogState.money(offering.price)));

            Tutor tutorOffering = Tutor.fromCatalog(teacher.id, teacher.name, subjectName, levelName, offering.price);
            View.OnClickListener bookClick = v -> host.classroom.openBooking(tutorOffering);

            itemBinding.getRoot().setOnClickListener(bookClick);
            itemBinding.btnBookOffering.setOnClickListener(bookClick);

            binding.llOfferings.addView(itemBinding.getRoot());
        }
    }

    private void setupFavorite(MainActivity host, CatalogState.Teacher teacher) {
        String favKey = "fav_tutor_" + teacher.id;
        boolean isFav = preferences.getBoolean(favKey, false);
        updateFavoriteIcon(isFav);

        binding.ibFavorite.setOnClickListener(v -> {
            boolean current = preferences.getBoolean(favKey, false);
            boolean updated = !current;
            preferences.edit().putBoolean(favKey, updated).apply();
            updateFavoriteIcon(updated);
            host.classroom.notice(getString(updated
                    ? R.string.tutor_profile_saved_toast
                    : R.string.tutor_profile_unsaved_toast));
        });
    }

    private void updateFavoriteIcon(boolean isFav) {
        binding.ibFavorite.setImageResource(isFav ? R.drawable.ic_heart_filled : R.drawable.ic_heart);
        binding.ibFavorite.setContentDescription(getString(isFav
                ? R.string.tutor_profile_unsave
                : R.string.tutor_profile_save));
    }

    private static void book(MainActivity host, CatalogState catalog, CatalogState.Teacher teacher) {
        List<CatalogState.Offering> offerings = new ArrayList<>(teacher.offerings);
        if (offerings.isEmpty()) {
            host.classroom.dialog(teacher.name, host.getString(R.string.tutor_profile_no_offerings));
            return;
        }
        if (offerings.size() == 1) {
            host.classroom.openBooking(tutor(catalog, teacher, offerings.get(0)));
            return;
        }
        String[] labels = new String[offerings.size()];
        for (int i = 0; i < labels.length; i++) {
            CatalogState.Offering offering = offerings.get(i);
            labels[i] = host.getString(R.string.subject_level,
                    catalog.subjects().get(offering.subjectId),
                    CatalogState.level(offering.level))
                    + " · " + CatalogState.money(offering.price);
        }
        new AlertDialog.Builder(host)
                .setTitle(R.string.choose_subject)
                .setNegativeButton(R.string.close, null)
                .setItems(labels, (d, which) -> host.classroom.openBooking(tutor(catalog, teacher, offerings.get(which))))
                .show();
    }

    private static Tutor tutor(CatalogState catalog, CatalogState.Teacher teacher, CatalogState.Offering offering) {
        return Tutor.fromCatalog(
                teacher.id,
                teacher.name,
                catalog.subjects().get(offering.subjectId),
                CatalogState.level(offering.level),
                offering.price);
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
