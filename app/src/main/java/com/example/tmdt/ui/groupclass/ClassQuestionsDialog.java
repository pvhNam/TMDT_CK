package com.example.tmdt.ui.groupclass;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import com.example.tmdt.GroupClassQuestion;
import com.example.tmdt.GroupClassQuestionsState;
import com.example.tmdt.MainActivity;
import com.example.tmdt.R;
import com.example.tmdt.databinding.DialogClassQuestionsBinding;
import com.example.tmdt.databinding.ItemClassQuestionBinding;
import com.example.tmdt.databinding.ItemClassReplyBinding;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

/** Public questions and threaded replies about a class; no review or rating actions. */
public final class ClassQuestionsDialog extends DialogFragment {
    private static final String TAG = "class_questions";
    private DialogClassQuestionsBinding binding;
    private GroupClassQuestionsState state;
    private String renderedContentKey = "";
    private TextWatcher editorWatcher;

    public ClassQuestionsDialog() { super(R.layout.dialog_class_questions); }

    public static ClassQuestionsDialog newInstance(String classId) {
        ClassQuestionsDialog dialog = new ClassQuestionsDialog();
        Bundle args = new Bundle(); args.putString("class_id", classId); dialog.setArguments(args);
        return dialog;
    }

    /** The dialog is intentionally reachable without joining or paying for a class. */
    public static void open(Fragment host, String classId) {
        if (!host.isAdded() || classId == null || classId.isEmpty()) return;
        FragmentManager manager = host.getParentFragmentManager();
        if (manager.isStateSaved() || manager.findFragmentByTag(TAG) != null) return;
        newInstance(classId).showNow(manager, TAG);
    }

    @Override public void onCreate(@Nullable Bundle savedState) {
        super.onCreate(savedState); setStyle(STYLE_NO_TITLE, 0);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedState) {
        super.onViewCreated(view, savedState);
        binding = DialogClassQuestionsBinding.bind(view);
        state = new ViewModelProvider(requireActivity()).get(GroupClassQuestionsState.class);
        state.watch(requireArguments().getString("class_id", ""));
        binding.classQuestionInput.setText(state.draft());
        editorWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence value, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence value, int start, int before, int count) {
                state.draft(value.toString());
                if (binding != null) {
                    binding.classQuestionInput.setError(null);
                    renderCounter(value.length());
                }
            }
            @Override public void afterTextChanged(Editable value) {}
        };
        binding.classQuestionInput.addTextChangedListener(editorWatcher);
        binding.sendClassQuestion.setOnClickListener(v -> {
            if (binding.classQuestionInput.getText().toString().trim().isEmpty()) {
                binding.classQuestionInput.setError(getString(R.string.class_questions_required));
                binding.classQuestionInput.requestFocus();
                return;
            }
            state.submit();
        });
        binding.cancelClassQuestionReply.setOnClickListener(v -> state.cancelReply());
        binding.classQuestionsRetry.setOnClickListener(v -> state.retry());
        binding.classQuestionsLoadMore.setOnClickListener(v -> state.loadMore());
        binding.classQuestionsSignIn.setOnClickListener(v -> {
            MainActivity host = (MainActivity) requireActivity();
            dismiss(); host.show("login");
        });
        binding.closeClassQuestions.setOnClickListener(v -> dismiss());
        state.changes().observe(getViewLifecycleOwner(), ignored -> render());
    }

    @Override public void onStart() {
        super.onStart();
        Window window = requireDialog().getWindow();
        if (window != null) {
            int width = getResources().getDisplayMetrics().widthPixels;
            int maxWidth = Math.round(620 * getResources().getDisplayMetrics().density);
            int margin = Math.round(24 * getResources().getDisplayMetrics().density);
            window.setLayout(Math.min(width - margin, maxWidth), Math.round(getResources().getDisplayMetrics().heightPixels * 0.90f));
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
    }

    private void render() {
        if (binding == null) return;
        boolean available = state.available();
        binding.classQuestionsClassTitle.setText(state.title());
        binding.classQuestionsProgress.setVisibility(state.loading() || state.busy() ? View.VISIBLE : View.GONE);
        binding.classQuestionsOffline.setVisibility(state.offline() ? View.VISIBLE : View.GONE);
        String error = state.error();
        if (!available && !state.loading() && error.isEmpty()) error = getString(R.string.class_questions_unavailable);
        binding.classQuestionsError.setText(error);
        binding.classQuestionsError.setVisibility(error.isEmpty() ? View.GONE : View.VISIBLE);
        binding.classQuestionsNotice.setText(state.notice());
        binding.classQuestionsNotice.setVisibility(state.notice().isEmpty() ? View.GONE : View.VISIBLE);
        binding.classQuestionsRetry.setVisibility(!error.isEmpty() || state.offline() ? View.VISIBLE : View.GONE);
        binding.classQuestionsRetry.setEnabled(!state.busy());
        binding.classQuestionsEmpty.setVisibility(available && !state.loading() && state.questions().isEmpty() && error.isEmpty() ? View.VISIBLE : View.GONE);
        binding.classQuestionsLoadMore.setVisibility(available && state.hasMore() ? View.VISIBLE : View.GONE);
        binding.classQuestionsLoadMore.setEnabled(!state.loading() && !state.busy() && !state.offline());
        binding.classQuestionsComposer.setVisibility(available && state.signedIn() && !state.readOnly() ? View.VISIBLE : View.GONE);
        binding.classQuestionsSignIn.setVisibility(available && !state.signedIn() && !state.readOnly() ? View.VISIBLE : View.GONE);
        boolean readOnly = available && state.readOnly();
        String caption = "";
        if (readOnly) caption = getString("ENDED".equals(state.status()) || "CANCELLED".equals(state.status())
                ? R.string.class_questions_read_only : R.string.class_questions_account_blocked);
        else if (available && !state.signedIn()) caption = getString(R.string.class_questions_sign_in_caption);
        binding.classQuestionsReadOnly.setText(caption);
        binding.classQuestionsReadOnly.setVisibility(caption.isEmpty() ? View.GONE : View.VISIBLE);
        boolean replying = !state.replyToId().isEmpty();
        binding.classQuestionReplyTarget.setText(replying ? getString(R.string.class_questions_reply_target, state.replyToName())
                : getString(R.string.class_questions_question_label));
        binding.cancelClassQuestionReply.setVisibility(replying ? View.VISIBLE : View.GONE);
        binding.cancelClassQuestionReply.setEnabled(!state.busy());
        binding.classQuestionInput.setHint(replying ? R.string.class_questions_reply_hint : R.string.class_questions_input_hint);
        binding.classQuestionInput.setEnabled(!state.busy());
        String draft = state.draft();
        if (!binding.classQuestionInput.getText().toString().equals(draft)) {
            binding.classQuestionInput.setText(draft);
            binding.classQuestionInput.setSelection(binding.classQuestionInput.length());
        }
        renderCounter(draft.length());
        binding.sendClassQuestion.setText(replying ? R.string.class_questions_send_reply : R.string.class_questions_send);
        binding.sendClassQuestion.setEnabled(state.canPost());
        binding.closeClassQuestions.setEnabled(!state.busy());
        setCancelable(!state.busy());
        renderQuestions(available);
    }

    private void renderCounter(int length) {
        binding.classQuestionCharacterCount.setText(getString(R.string.class_questions_counter, length));
    }

    /** Draft keystrokes and status messages never rebuild rows or steal the editor focus. */
    private void renderQuestions(boolean available) {
        StringBuilder key = new StringBuilder().append(available).append('|').append(state.replyToId())
                .append('|').append(state.busy()).append('|').append(state.readOnly()).append('|').append(state.signedIn())
                .append('|').append(state.tutorId()).append('|').append(state.repliesLoading()).append('|').append(state.repliesError())
                .append('|').append(state.hasMoreReplies());
        if (available) {
            for (GroupClassQuestion question : state.questions()) appendKey(key, question);
            for (GroupClassQuestion reply : state.replies()) appendKey(key, reply);
        }
        String nextKey = key.toString();
        if (nextKey.equals(renderedContentKey)) return;
        renderedContentKey = nextKey;
        int scroll = binding.classQuestionsScroll.getScrollY();
        binding.classQuestionsList.removeAllViews();
        if (available) for (GroupClassQuestion question : state.questions()) {
            ItemClassQuestionBinding row = ItemClassQuestionBinding.inflate(getLayoutInflater(), binding.classQuestionsList, false);
            row.classQuestionAuthor.setText(question.authorName);
            row.classQuestionTutorBadge.setVisibility(question.authorId.equals(state.tutorId()) ? View.VISIBLE : View.GONE);
            row.classQuestionDate.setText(date(question.createdAtMillis));
            row.classQuestionContent.setText(question.content);
            boolean expanded = question.id.equals(state.replyToId());
            row.replyToClassQuestion.setText(expanded ? R.string.class_questions_collapse_replies
                    : state.readOnly() || !state.signedIn() ? R.string.class_questions_view_replies : R.string.class_questions_reply);
            row.replyToClassQuestion.setEnabled(!state.busy());
            row.replyToClassQuestion.setOnClickListener(v -> { if (expanded) state.cancelReply(); else state.replyTo(question.id); });
            row.classQuestionRepliesContainer.setVisibility(expanded ? View.VISIBLE : View.GONE);
            if (expanded) {
                row.classQuestionRepliesProgress.setVisibility(state.repliesLoading() ? View.VISIBLE : View.GONE);
                row.classQuestionRepliesError.setText(state.repliesError());
                row.classQuestionRepliesError.setVisibility(state.repliesError().isEmpty() ? View.GONE : View.VISIBLE);
                row.retryClassQuestionReplies.setVisibility(state.repliesError().isEmpty() ? View.GONE : View.VISIBLE);
                row.retryClassQuestionReplies.setEnabled(!state.busy());
                row.retryClassQuestionReplies.setOnClickListener(v -> state.retry());
                row.classQuestionRepliesEmpty.setVisibility(!state.repliesLoading() && state.repliesError().isEmpty() && state.replies().isEmpty() ? View.VISIBLE : View.GONE);
                row.classQuestionLoadMoreReplies.setVisibility(state.hasMoreReplies() ? View.VISIBLE : View.GONE);
                row.classQuestionLoadMoreReplies.setEnabled(!state.repliesLoading() && !state.busy() && !state.offline());
                row.classQuestionLoadMoreReplies.setOnClickListener(v -> state.loadMoreReplies());
                for (GroupClassQuestion reply : state.replies()) {
                    ItemClassReplyBinding child = ItemClassReplyBinding.inflate(getLayoutInflater(), row.classQuestionRepliesList, false);
                    child.classReplyAuthor.setText(reply.authorName);
                    child.classReplyTutorBadge.setVisibility(reply.authorId.equals(state.tutorId()) ? View.VISIBLE : View.GONE);
                    child.classReplyDate.setText(date(reply.createdAtMillis));
                    child.classReplyContent.setText(reply.content);
                    row.classQuestionRepliesList.addView(child.getRoot());
                }
            }
            binding.classQuestionsList.addView(row.getRoot());
        }
        DialogClassQuestionsBinding renderedBinding = binding;
        renderedBinding.classQuestionsScroll.post(() -> {
            if (binding == renderedBinding) renderedBinding.classQuestionsScroll.scrollTo(0, scroll);
        });
    }

    private static void appendKey(StringBuilder key, GroupClassQuestion message) {
        key.append('|').append(message.id).append('|').append(message.authorId).append('|').append(message.authorName)
                .append('|').append(message.createdAtMillis).append('|').append(message.content);
    }

    private String date(long timestamp) {
        return timestamp <= 0 ? getString(R.string.class_questions_just_sent)
                : DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.forLanguageTag("vi-VN")).format(new Date(timestamp));
    }

    @Override public void onDestroyView() {
        if (binding != null && editorWatcher != null) binding.classQuestionInput.removeTextChangedListener(editorWatcher);
        super.onDestroyView(); binding = null; editorWatcher = null; renderedContentKey = "";
    }
}
