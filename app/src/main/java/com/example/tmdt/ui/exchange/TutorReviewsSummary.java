package com.example.tmdt.ui.exchange;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import com.example.tmdt.Exchange;
import com.example.tmdt.MainActivity;
import com.example.tmdt.Review;
import com.example.tmdt.ReviewState;
import com.example.tmdt.databinding.ItemReviewBinding;
import java.util.List;
import java.util.Locale;

/**
 * UC26 – Phần "Đánh giá của học viên" trong hồ sơ gia sư: điểm trung bình, số đánh giá, 3 đánh giá mới nhất
 * và lối sang màn 10 (toàn bộ đánh giá, lọc theo số sao). Hồ sơ gia sư chỉ cần gọi {@link #bind} một dòng.
 */
public final class TutorReviewsSummary {
    private static final int PREVIEW = 3;
    private TutorReviewsSummary() {}

    /**
     * @param tutorUid UID tài khoản gia sư (users/{uid}), là target_id của đánh giá
     * @param stat ô số liệu "Đánh giá" trên đầu hồ sơ (có thể null)
     */
    public static void bind(Fragment fragment, LinearLayout list, TextView summary, TextView stat, String tutorUid, String tutorName) {
        if (tutorUid == null || tutorUid.isEmpty()) return;
        Exchange exchange = ((MainActivity) fragment.requireActivity()).exchange;
        ReviewState reviews = exchange.reviews;
        reviews.watch(tutorUid, Review.TUTOR);
        Runnable draw = () -> draw(fragment, exchange, reviews, list, summary, stat, tutorUid, tutorName);
        if (!tutorUid.equals(list.getTag())) {
            list.setTag(tutorUid);
            reviews.changes().observe(fragment.getViewLifecycleOwner(), ignored -> draw.run());
        }
        draw.run();
    }

    private static void draw(Fragment fragment, Exchange exchange, ReviewState reviews, LinearLayout list, TextView summary,
                             TextView stat, String tutorUid, String tutorName) {
        if (!fragment.isAdded() || !tutorUid.equals(list.getTag())) return;
        List<Review> all = reviews.reviews();
        String average = String.format(Locale.ROOT, "%.1f", reviews.average()).replace('.', ',');
        if (stat != null) stat.setText(all.isEmpty() ? "—" : average);
        list.removeAllViews();
        if (all.isEmpty()) {
            summary.setText(reviews.loading() ? "Đang tải đánh giá…" : !reviews.loadError().isEmpty() ? reviews.loadError()
                    : "Chưa có đánh giá. Học viên đánh giá sau khi hợp đồng đã chốt và học xong buổi đầu tiên.");
            summary.setOnClickListener(null); summary.setClickable(false);
            return;
        }
        summary.setText("★ " + average + " / 5 · " + all.size() + " đánh giá · Xem tất cả ›");
        summary.setOnClickListener(v -> exchange.openReviews(tutorUid, tutorName, Review.TUTOR));
        LayoutInflater inflater = LayoutInflater.from(list.getContext());
        for (int i = 0; i < Math.min(PREVIEW, all.size()); i++) {
            Review r = all.get(i);
            ItemReviewBinding item = ItemReviewBinding.inflate(inflater, list, false);
            ExUi.avatar(item.avatar.avatarPhoto, item.avatar.avatarInitials, null, r.authorName);
            item.name.setText(r.authorName.isEmpty() ? "Học viên" : r.authorName);
            ExUi.stars(item.stars, r.rating, 14, null);
            item.date.setText(ExUi.date(r.createdAt));
            item.comment.setVisibility(r.comment.isEmpty() ? View.GONE : View.VISIBLE);
            item.comment.setText("“" + r.comment + "”");
            item.tags.setVisibility(r.tags.isEmpty() ? View.GONE : View.VISIBLE);
            item.tags.setText(String.join(" · ", r.tags));
            item.subject.setText(r.subject);
            list.addView(item.getRoot());
        }
    }
}
