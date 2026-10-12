package com.example.tmdt.ui.exchange;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.tmdt.Exchange;
import com.example.tmdt.MainActivity;
import com.example.tmdt.R;
import com.example.tmdt.Review;
import com.example.tmdt.ReviewState;
import com.example.tmdt.databinding.FragmentReviewsBinding;
import com.example.tmdt.databinding.ItemReviewBinding;
import com.google.android.material.chip.Chip;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Màn 10 – Đánh giá gia sư (UC26): điểm trung bình, tổng số đánh giá, nhận xét của học viên;
 * lọc theo số sao và xem thêm. Cũng dùng cho "Đánh giá về tôi" của học viên/gia sư.
 */
public final class ReviewsFragment extends Fragment {
    private static final int PAGE = 10;
    private FragmentReviewsBinding binding;
    private ReviewState reviews;
    private Exchange exchange;
    /** 0 = tất cả, 1..5 = số sao. */
    private int filter;
    private int shown = PAGE;

    @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        binding = FragmentReviewsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        exchange = ((MainActivity) requireActivity()).exchange;
        reviews = exchange.reviews;
        if (state != null) { filter = state.getInt("filter"); shown = state.getInt("shown", PAGE); }
        boolean self = exchange.reviewsTarget() != null && exchange.reviewsTarget().equals(reviews.myId());
        ExUi.header(binding.header, getString(self ? R.string.ex_my_reviews_title
                : Review.TUTOR.equals(exchange.reviewsRole()) ? R.string.ex_reviews_title : R.string.ex_my_reviews_title), null, exchange::back);
        String[] labels = {"Tất cả", "5 sao", "4 sao", "3 sao", "2 sao", "1 sao"};
        int[] values = {0, 5, 4, 3, 2, 1};
        for (int i = 0; i < labels.length; i++) {
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.view_ex_chip, binding.filters, false);
            chip.setText(labels[i]); chip.setId(View.generateViewId()); chip.setCheckedIconVisible(false);
            final int value = values[i];
            chip.setChecked(value == filter);
            chip.setOnClickListener(v -> { filter = value; shown = PAGE; render(); });
            binding.filters.addView(chip);
        }
        binding.more.setOnClickListener(v -> { shown += PAGE; render(); });
        reviews.changes().observe(getViewLifecycleOwner(), ignored -> render());
        if (exchange.reviewsTarget() != null) reviews.watch(exchange.reviewsTarget(), exchange.reviewsRole());
        render();
    }

    private void render() {
        if (binding == null) return;
        String name = exchange.reviewsName() == null ? "" : exchange.reviewsName();
        ExUi.avatar(binding.avatar.avatarPhoto, binding.avatar.avatarInitials, null, name);
        binding.name.setText(name);
        List<Review> all = reviews.reviews();
        double average = reviews.average();
        binding.average.setText(String.format(Locale.ROOT, "%.1f / 5", average).replace('.', ','));
        ExUi.stars(binding.stars, (int) Math.round(average), 18, null);
        binding.count.setText(all.size() + " đánh giá");

        for (int i = 0; i < binding.filters.getChildCount(); i++) {
            Chip chip = (Chip) binding.filters.getChildAt(i);
            int value = i == 0 ? 0 : 6 - i;
            chip.setChecked(value == filter);
            if (value > 0) chip.setText(value + " sao (" + reviews.count(value) + ")");
        }

        List<Review> items = new ArrayList<>();
        for (Review r : all) if (filter == 0 || r.rating == filter) items.add(r);
        String state = reviews.loading() && all.isEmpty() ? "Đang tải đánh giá…"
                : !reviews.loadError().isEmpty() ? reviews.loadError()
                : items.isEmpty() ? getString(R.string.ex_no_reviews) : null;
        binding.state.setVisibility(state == null ? View.GONE : View.VISIBLE);
        if (state != null) binding.state.setText(state);

        binding.list.removeAllViews();
        for (int i = 0; i < Math.min(shown, items.size()); i++) {
            Review r = items.get(i);
            ItemReviewBinding item = ItemReviewBinding.inflate(getLayoutInflater(), binding.list, false);
            ExUi.avatar(item.avatar.avatarPhoto, item.avatar.avatarInitials, null, r.authorName);
            item.name.setText(r.authorName.isEmpty() ? "Người dùng" : r.authorName);
            ExUi.stars(item.stars, r.rating, 14, null);
            item.date.setText(ExUi.date(r.createdAt));
            item.comment.setVisibility(r.comment.isEmpty() ? View.GONE : View.VISIBLE);
            item.comment.setText("“" + r.comment + "”");
            item.tags.setVisibility(r.tags.isEmpty() ? View.GONE : View.VISIBLE);
            item.tags.setText(String.join(" · ", r.tags));
            item.subject.setText(r.subject);
            binding.list.addView(item.getRoot());
        }
        binding.more.setVisibility(items.size() > shown ? View.VISIBLE : View.GONE);
    }

    @Override public void onSaveInstanceState(@NonNull Bundle out) { super.onSaveInstanceState(out); out.putInt("filter", filter); out.putInt("shown", shown); }
    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}
