package com.example.tmdt.ui.exchange;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.tmdt.Contract;
import com.example.tmdt.Exchange;
import com.example.tmdt.MainActivity;
import com.example.tmdt.R;
import com.example.tmdt.Review;
import com.example.tmdt.ReviewState;
import com.example.tmdt.databinding.FragmentReviewFormBinding;
import com.google.android.material.chip.Chip;
import java.util.ArrayList;
import java.util.List;

/**
 * UC26 – Đánh giá hai chiều. Cùng một màn cho hai hướng:
 * màn 09 "Viết đánh giá" (học viên đánh giá gia sư) và màn 46 "Đánh giá học viên" (gia sư đánh giá học viên).
 * Chỉ đánh giá được hợp đồng đã chốt, sau khi buổi học đầu tiên kết thúc; mỗi bên một lần cho mỗi hợp đồng.
 */
public final class ReviewFormFragment extends Fragment {
    private static final String[] TUTOR_TAGS = {"Dễ hiểu", "Đúng giờ", "Tận tâm", "Chuẩn bị kỹ", "Nhiệt tình"};
    private static final String[] STUDENT_TAGS = {"Đúng giờ", "Chuẩn bị bài", "Tích cực", "Tiếp thu tốt", "Hoàn thành bài tập"};

    private FragmentReviewFormBinding binding;
    private Exchange exchange;
    private ReviewState reviews;
    private int rating;
    private boolean tagsBuilt;

    @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        binding = FragmentReviewFormBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        exchange = ((MainActivity) requireActivity()).exchange;
        reviews = exchange.reviews;
        if (state != null) { rating = state.getInt("rating"); binding.comment.setText(state.getString("comment", "")); }
        ExUi.counter(binding.comment, binding.counter, Review.MAX_COMMENT);
        binding.submit.setOnClickListener(v -> submit());
        reviews.changes().observe(getViewLifecycleOwner(), ignored -> render());
        exchange.contracts.changes().observe(getViewLifecycleOwner(), ignored -> render());
        exchange.contracts.start();
        render();
    }

    private Contract contract() { return exchange.contracts.contract(exchange.reviewContractId()); }

    private void render() {
        if (binding == null) return;
        String notice = reviews.takeNotice();
        if (!notice.isEmpty()) ExUi.snack(binding.getRoot(), notice);
        Contract c = contract();
        String me = reviews.myId();
        boolean targetTutor = c == null || c.other(me).equals(c.tutorId);
        ExUi.header(binding.header, getString(targetTutor ? R.string.ex_review_tutor_title : R.string.ex_review_student_title), null, exchange::back);
        if (c == null) { binding.name.setText(exchange.contracts.loading() ? "Đang tải…" : "Không tìm thấy hợp đồng"); binding.submit.setEnabled(false); return; }
        reviews.checkMine(c.id);

        String target = c.otherName(me);
        ExUi.avatar(binding.avatar.avatarPhoto, binding.avatar.avatarInitials, null, target);
        binding.name.setText(target);
        binding.lesson.setText(c.subject + " · " + c.startDate);
        long firstLessonEnd = c.startAt + c.minutes * 60_000L;
        boolean lessonDone = System.currentTimeMillis() >= firstLessonEnd;
        ExUi.pill(binding.status, lessonDone ? "Đã học buổi đầu" : "Chưa diễn ra", lessonDone ? ExUi.OK : ExUi.WAIT);
        binding.question.setText(targetTutor ? R.string.ex_review_tutor_question : R.string.ex_review_student_question);
        binding.tagsTitle.setText(getString(targetTutor ? R.string.ex_review_topics : R.string.ex_review_highlights) + " " + getString(R.string.ex_optional));
        if (!tagsBuilt) buildTags(targetTutor ? TUTOR_TAGS : STUDENT_TAGS);

        Review mine = reviews.mine();
        boolean done = mine != null && c.id.equals(mine.contractId);
        boolean allowed = c.agreed() && lessonDone && !done;
        int shown = done ? mine.rating : rating;
        ExUi.stars(binding.stars, shown, 40, allowed ? value -> { rating = value; render(); } : null);
        binding.ratingLabel.setText(Review.ratingLabel(shown));
        for (int i = 0; i < binding.tags.getChildCount(); i++) {
            Chip chip = (Chip) binding.tags.getChildAt(i);
            if (done) chip.setChecked(mine.tags.contains(chip.getText().toString()));
            chip.setEnabled(allowed);
        }
        if (done && binding.comment.length() == 0) binding.comment.setText(mine.comment);
        binding.comment.setEnabled(allowed);

        String note;
        if (!c.agreed()) note = "Chỉ đánh giá được hợp đồng đã được hai bên chốt.";
        else if (!lessonDone) note = "Bạn có thể đánh giá sau khi buổi học đầu tiên kết thúc (" + ExUi.dateTime(firstLessonEnd) + ").";
        else note = getString(targetTutor ? R.string.ex_review_tutor_note : R.string.ex_review_student_note);
        binding.note.setText(note);
        binding.done.setVisibility(done ? View.VISIBLE : View.GONE);
        if (done) binding.done.setText("Bạn đã gửi đánh giá này" + (mine.createdAt > 0 ? " ngày " + ExUi.date(mine.createdAt) : "") + ". Mỗi hợp đồng được đánh giá một lần.");
        binding.submit.setEnabled(allowed && rating > 0 && !reviews.busy() && !reviews.checking());
        binding.submit.setText(done ? "Đã gửi đánh giá" : getString(R.string.ex_send_review));
    }

    private void buildTags(String[] tags) {
        tagsBuilt = true;
        binding.tags.removeAllViews();
        for (String tag : tags) {
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.view_ex_chip, binding.tags, false);
            chip.setText(tag);
            binding.tags.addView(chip);
        }
    }

    private void submit() {
        Contract c = contract();
        if (c == null || rating == 0) return;
        List<String> tags = new ArrayList<>();
        for (int i = 0; i < binding.tags.getChildCount(); i++) {
            Chip chip = (Chip) binding.tags.getChildAt(i);
            if (chip.isChecked()) tags.add(chip.getText().toString());
        }
        ((MainActivity) requireActivity()).hideKeyboard();
        reviews.submit(c, exchange.myName(), rating, tags, ExUi.text(binding.comment));
    }

    @Override public void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("rating", rating);
        if (binding != null) out.putString("comment", ExUi.text(binding.comment));
    }
    @Override public void onDestroyView() { super.onDestroyView(); binding = null; tagsBuilt = false; }
}
