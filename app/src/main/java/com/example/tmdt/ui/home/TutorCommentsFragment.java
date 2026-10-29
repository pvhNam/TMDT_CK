package com.example.tmdt.ui.home;

import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewParent;
import android.widget.ScrollView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.tmdt.GroupClassQuestion;
import com.example.tmdt.MainActivity;
import com.example.tmdt.R;
import com.example.tmdt.TutorCommentsState;
import com.example.tmdt.databinding.FragmentTutorCommentsBinding;
import com.example.tmdt.databinding.ItemTutorCommentBinding;
import com.example.tmdt.databinding.ItemTutorCommentReplyBinding;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

/** Inline public questions/comments below the profile's separate reviews section. */
public final class TutorCommentsFragment extends Fragment {
    private FragmentTutorCommentsBinding binding;
    private TutorCommentsState state;
    private String renderedContentKey = "";
    private TextWatcher editorWatcher;

    public TutorCommentsFragment() { super(R.layout.fragment_tutor_comments); }
    public static TutorCommentsFragment newInstance(String tutorId) {
        TutorCommentsFragment fragment = new TutorCommentsFragment();
        Bundle args = new Bundle(); args.putString("tutor_id", tutorId); fragment.setArguments(args);
        return fragment;
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedState) {
        super.onViewCreated(view, savedState);
        binding = FragmentTutorCommentsBinding.bind(view);
        state = new ViewModelProvider(requireActivity()).get(TutorCommentsState.class);
        state.watch(requireArguments().getString("tutor_id", ""));
        binding.tutorCommentInput.setText(state.draft());
        editorWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence value, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence value, int start, int before, int count) {
                state.draft(value.toString());
                if (binding != null) {
                    binding.tutorCommentInput.setError(null);
                    renderCounter(value.length());
                }
            }
            @Override public void afterTextChanged(Editable value) {}
        };
        binding.tutorCommentInput.addTextChangedListener(editorWatcher);
        binding.tutorCommentInput.setOnFocusChangeListener((v, focused) -> { if (focused) revealComposer(false); });
        binding.sendTutorComment.setOnClickListener(v -> {
            if (binding.tutorCommentInput.getText().toString().trim().isEmpty()) {
                binding.tutorCommentInput.setError(getString(R.string.tutor_comments_required));
                binding.tutorCommentInput.requestFocus(); revealComposer(false); return;
            }
            state.submit();
        });
        binding.cancelTutorCommentReply.setOnClickListener(v -> state.cancelReply());
        binding.tutorCommentsRetry.setOnClickListener(v -> state.retry());
        binding.tutorCommentsLoadMore.setOnClickListener(v -> state.loadMore());
        binding.tutorCommentsSignIn.setOnClickListener(v -> ((MainActivity) requireActivity()).show("login"));
        state.changes().observe(getViewLifecycleOwner(), ignored -> render());
    }

    private void render() {
        if (binding == null) return;
        boolean available = state.available();
        binding.tutorCommentsProgress.setVisibility(state.loading() || state.busy() ? View.VISIBLE : View.GONE);
        binding.tutorCommentsOffline.setVisibility(state.offline() ? View.VISIBLE : View.GONE);
        String error = state.error();
        if (!available && !state.loading() && error.isEmpty()) error = getString(R.string.tutor_comments_unavailable);
        binding.tutorCommentsError.setText(error);
        binding.tutorCommentsError.setVisibility(error.isEmpty() ? View.GONE : View.VISIBLE);
        binding.tutorCommentsNotice.setText(state.notice());
        binding.tutorCommentsNotice.setVisibility(state.notice().isEmpty() ? View.GONE : View.VISIBLE);
        binding.tutorCommentsRetry.setVisibility(!error.isEmpty() || state.offline() ? View.VISIBLE : View.GONE);
        binding.tutorCommentsRetry.setEnabled(!state.busy());
        binding.tutorCommentsEmpty.setVisibility(available && !state.loading() && state.questions().isEmpty() && error.isEmpty() ? View.VISIBLE : View.GONE);
        binding.tutorCommentsLoadMore.setVisibility(available && state.hasMore() ? View.VISIBLE : View.GONE);
        binding.tutorCommentsLoadMore.setEnabled(!state.loading() && !state.busy() && !state.offline());
        binding.tutorCommentsComposer.setVisibility(available && state.signedIn() && !state.readOnly() ? View.VISIBLE : View.GONE);
        binding.tutorCommentsSignIn.setVisibility(available && !state.signedIn() && !state.readOnly() ? View.VISIBLE : View.GONE);
        String caption = "";
        if (available && state.readOnly()) caption = getString(R.string.tutor_comments_account_blocked);
        else if (available && !state.signedIn()) caption = getString(R.string.tutor_comments_sign_in_caption);
        binding.tutorCommentsReadOnly.setText(caption);
        binding.tutorCommentsReadOnly.setVisibility(caption.isEmpty() ? View.GONE : View.VISIBLE);
        boolean replying = !state.replyToId().isEmpty();
        binding.tutorCommentReplyTarget.setText(replying ? getString(R.string.tutor_comments_reply_target, state.replyToName())
                : getString(R.string.tutor_comments_question_label));
        binding.cancelTutorCommentReply.setVisibility(replying ? View.VISIBLE : View.GONE);
        binding.cancelTutorCommentReply.setEnabled(!state.busy());
        binding.tutorCommentInput.setHint(replying ? R.string.tutor_comments_reply_hint : R.string.tutor_comments_input_hint);
        binding.tutorCommentInput.setEnabled(!state.busy());
        String draft = state.draft();
        if (!binding.tutorCommentInput.getText().toString().equals(draft)) {
            binding.tutorCommentInput.setText(draft);
            binding.tutorCommentInput.setSelection(binding.tutorCommentInput.length());
        }
        renderCounter(draft.length());
        binding.sendTutorComment.setText(replying ? R.string.tutor_comments_send_reply : R.string.tutor_comments_send);
        binding.sendTutorComment.setEnabled(state.canPost());
        renderComments(available);
    }

    private void renderCounter(int length) {
        binding.tutorCommentCharacterCount.setText(getString(R.string.tutor_comments_counter, length));
    }

    /** Uses the profile's own scrolling container; comments never introduce a nested scroll surface. */
    private ScrollView profileScroll() {
        if (binding == null) return null;
        for (ViewParent parent = binding.getRoot().getParent(); parent != null; parent = parent.getParent())
            if (parent instanceof ScrollView) return (ScrollView) parent;
        return null;
    }

    private void revealComposer(boolean focus) {
        if (binding == null || !state.signedIn() || state.readOnly() || !state.available()) return;
        FragmentTutorCommentsBinding current = binding;
        current.tutorCommentsComposer.post(() -> {
            if (binding != current) return;
            if (focus) current.tutorCommentInput.requestFocus();
            Rect area = new Rect(); current.tutorCommentsComposer.getDrawingRect(area);
            // The profile retains its 96dp bottom padding for its sticky booking/message footer.
            area.bottom += Math.round(96 * getResources().getDisplayMetrics().density);
            current.tutorCommentsComposer.requestRectangleOnScreen(area, false);
        });
    }

    /** Draft keystrokes only update the editor; server changes redraw comment rows. */
    private void renderComments(boolean available) {
        StringBuilder key = new StringBuilder().append(available).append('|').append(state.replyToId())
                .append('|').append(state.busy()).append('|').append(state.readOnly()).append('|').append(state.signedIn())
                .append('|').append(state.tutorId()).append('|').append(state.repliesLoading()).append('|').append(state.repliesError())
                .append('|').append(state.hasMoreReplies());
        if (available) {
            for (GroupClassQuestion comment : state.questions()) appendKey(key, comment);
            for (GroupClassQuestion reply : state.replies()) appendKey(key, reply);
        }
        String nextKey = key.toString();
        if (nextKey.equals(renderedContentKey)) return;
        boolean preserveScroll = !renderedContentKey.isEmpty();
        renderedContentKey = nextKey;
        // Let Android restore the profile hierarchy's scroll on this view's first render.
        ScrollView scroll = preserveScroll ? profileScroll() : null;
        int offset = scroll == null ? 0 : scroll.getScrollY();
        binding.tutorCommentsList.removeAllViews();
        if (available) for (GroupClassQuestion comment : state.questions()) {
            ItemTutorCommentBinding row = ItemTutorCommentBinding.inflate(getLayoutInflater(), binding.tutorCommentsList, false);
            row.tutorCommentAuthor.setText(comment.authorName);
            row.tutorCommentTutorBadge.setVisibility(comment.authorId.equals(state.tutorId()) ? View.VISIBLE : View.GONE);
            row.tutorCommentDate.setText(date(comment.createdAtMillis));
            row.tutorCommentContent.setText(comment.content);
            boolean expanded = comment.id.equals(state.replyToId());
            row.replyToTutorComment.setText(expanded ? R.string.tutor_comments_collapse_replies
                    : state.readOnly() || !state.signedIn() ? R.string.tutor_comments_view_replies : R.string.tutor_comments_reply);
            row.replyToTutorComment.setEnabled(!state.busy());
            row.replyToTutorComment.setOnClickListener(v -> {
                if (expanded) state.cancelReply();
                else { state.replyTo(comment.id); revealComposer(true); }
            });
            row.tutorCommentRepliesContainer.setVisibility(expanded ? View.VISIBLE : View.GONE);
            if (expanded) {
                row.tutorCommentRepliesProgress.setVisibility(state.repliesLoading() ? View.VISIBLE : View.GONE);
                row.tutorCommentRepliesError.setText(state.repliesError());
                row.tutorCommentRepliesError.setVisibility(state.repliesError().isEmpty() ? View.GONE : View.VISIBLE);
                row.retryTutorCommentReplies.setVisibility(state.repliesError().isEmpty() ? View.GONE : View.VISIBLE);
                row.retryTutorCommentReplies.setEnabled(!state.busy());
                row.retryTutorCommentReplies.setOnClickListener(v -> state.retry());
                row.tutorCommentRepliesEmpty.setVisibility(!state.repliesLoading() && state.repliesError().isEmpty() && state.replies().isEmpty() ? View.VISIBLE : View.GONE);
                row.tutorCommentLoadMoreReplies.setVisibility(state.hasMoreReplies() ? View.VISIBLE : View.GONE);
                row.tutorCommentLoadMoreReplies.setEnabled(!state.repliesLoading() && !state.busy() && !state.offline());
                row.tutorCommentLoadMoreReplies.setOnClickListener(v -> state.loadMoreReplies());
                for (GroupClassQuestion reply : state.replies()) {
                    ItemTutorCommentReplyBinding child = ItemTutorCommentReplyBinding.inflate(getLayoutInflater(), row.tutorCommentRepliesList, false);
                    child.tutorCommentReplyAuthor.setText(reply.authorName);
                    child.tutorCommentReplyTutorBadge.setVisibility(reply.authorId.equals(state.tutorId()) ? View.VISIBLE : View.GONE);
                    child.tutorCommentReplyDate.setText(date(reply.createdAtMillis));
                    child.tutorCommentReplyContent.setText(reply.content);
                    row.tutorCommentRepliesList.addView(child.getRoot());
                }
            }
            binding.tutorCommentsList.addView(row.getRoot());
        }
        FragmentTutorCommentsBinding renderedBinding = binding;
        if (scroll != null) renderedBinding.getRoot().post(() -> { if (binding == renderedBinding) scroll.scrollTo(0, offset); });
    }

    private static void appendKey(StringBuilder key, GroupClassQuestion message) {
        key.append('|').append(message.id).append('|').append(message.authorId).append('|').append(message.authorName)
                .append('|').append(message.createdAtMillis).append('|').append(message.content);
    }
    private String date(long timestamp) {
        return timestamp <= 0 ? getString(R.string.tutor_comments_just_sent)
                : DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.forLanguageTag("vi-VN")).format(new Date(timestamp));
    }
    @Override public void onDestroyView() {
        if (binding != null && editorWatcher != null) binding.tutorCommentInput.removeTextChangedListener(editorWatcher);
        super.onDestroyView(); binding = null; editorWatcher = null; renderedContentKey = "";
    }
}
