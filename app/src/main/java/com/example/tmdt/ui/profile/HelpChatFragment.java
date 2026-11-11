package com.example.tmdt.ui.profile;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.OneShotPreDrawListener;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.tmdt.MainActivity;
import com.example.tmdt.R;
import com.example.tmdt.ui.common.Motion;
import com.example.tmdt.ui.common.ThemeColors;
import com.example.tmdt.ui.common.Ui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class HelpChatFragment extends Fragment {
    private static final String FLOATING_KEY = "floating_help";
    private static final int SUGGESTED_TOPICS = 4;

    private final Map<Long, TurnViews> renderedTurns = new LinkedHashMap<>();
    private final ArrayList<View> topicControls = new ArrayList<>();
    private HelpChatState state;
    private boolean firstRender = true;
    private Ui ui;
    private ScrollView conversation;
    private LinearLayout messages;
    private LinearLayout turns;
    private EditText input;
    private ImageButton send;

    public static HelpChatFragment floating() {
        HelpChatFragment fragment = new HelpChatFragment();
        Bundle arguments = new Bundle();
        arguments.putBoolean(FLOATING_KEY, true);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override public void onCreate(@Nullable Bundle saved) {
        super.onCreate(saved);
        state = new ViewModelProvider(requireActivity()).get(HelpChatState.class);
    }

    @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                       @Nullable Bundle saved) {
        MainActivity host = (MainActivity) requireActivity();
        ui = new Ui(requireContext());
        LinearLayout root = ui.column();
        root.setBackgroundColor(ThemeColors.background(requireContext()));
        root.setFocusableInTouchMode(true);
        TextView topics = ui.link("Chủ đề", 13, this::showTopics);
        topicControls.add(topics);
        ui.pad(topics, 10, 4);
        boolean floating = getArguments() != null && getArguments().getBoolean(FLOATING_KEY);
        if (floating) {
            LinearLayout actions = ui.row();
            actions.addView(topics);
            actions.addView(ui.iconButton(android.R.drawable.ic_menu_close_clear_cancel, Ui.MUTED,
                    getString(R.string.help_close_popup), host::closeFloatingHelp));
            ui.header(root, getString(R.string.help_popup_title), null, actions);
        } else {
            ui.header(root, "Trung tâm trợ giúp", host::back, topics);
        }
        ui.line(root);

        conversation = ui.scroll(root);
        conversation.setFillViewport(false);
        messages = ui.column();
        ui.pad(messages, 16, 10);
        conversation.addView(messages, new ScrollView.LayoutParams(-1, -2));
        addBubble(messages, "Chào bạn! Bạn cần hướng dẫn tìm gia sư, đặt lịch học hay sử dụng tài khoản? "
                + "Hãy nhập câu hỏi hoặc chọn một chủ đề gợi ý.", false);
        turns = ui.column();
        ui.add(messages, turns);

        ui.line(root);
        LinearLayout footer = ui.column();
        footer.setBackgroundColor(requireContext().getColor(R.color.app_surface));
        if (!floating) addSuggestions(footer);
        addComposer(footer);
        ui.add(root, footer);
        root.requestFocus();
        return root;
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle saved) {
        super.onViewCreated(view, saved);
        state.changes().observe(getViewLifecycleOwner(), ignored -> render());
    }

    @Override public void onResume() {
        super.onResume();
        if (input != null && !input.getText().toString().equals(state.draft())) {
            input.setText(state.draft());
            input.setSelection(input.length());
        }
    }

    private void addSuggestions(LinearLayout footer) {
        HorizontalScrollView scroll = new HorizontalScrollView(requireContext());
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = ui.row();
        ui.pad(row, 12, 5);
        for (HelpFaq.Topic topic : HelpFaq.TOPICS.subList(0, SUGGESTED_TOPICS)) {
            if (row.getChildCount() > 0) ui.gap(row, 8);
            TextView chip = ui.pill(topic.question, 13, Ui.BLUE, Ui.PALE);
            chip.setMinHeight(ui.dp(44));
            ui.clickable(chip, () -> submit(topic.question));
            topicControls.add(chip);
            row.addView(chip);
        }
        scroll.addView(row, new HorizontalScrollView.LayoutParams(-2, -2));
        ui.add(footer, scroll);
    }

    private void addComposer(LinearLayout footer) {
        LinearLayout row = ui.row();
        ui.pad(row, 12, 8);
        input = ui.input("Nhập câu hỏi…");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setSingleLine(true);
        input.setImeOptions(EditorInfo.IME_ACTION_SEND);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(HelpChatState.MAX_QUESTION_LENGTH)});
        input.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        input.setText(state.draft());
        input.setSelection(input.length());
        ui.weight(row, input);
        ui.gap(row, 8);

        send = new ImageButton(requireContext());
        send.setImageResource(R.drawable.ic_send);
        send.setBackgroundResource(R.drawable.bg_chat_send);
        send.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        ui.pad(send, 12, 12);
        send.setContentDescription(getString(R.string.chat_send));
        ui.clickable(send, this::submitInput);
        row.addView(send, ui.lp(48, 48));

        input.setOnEditorActionListener((view, action, event) -> {
            if (action != EditorInfo.IME_ACTION_SEND) return false;
            submitInput();
            return true;
        });
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence text, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable text) {
                state.draft(text.toString());
                updateSend();
            }
        });
        updateSend();
        ui.add(footer, row);
    }

    private void updateSend() {
        if (send == null || input == null) return;
        boolean enabled = !state.busy() && !input.getText().toString().trim().isEmpty();
        send.setEnabled(enabled);
        send.setAlpha(enabled ? 1f : 0.45f);
    }

    private void showTopics() {
        String[] labels = new String[HelpFaq.TOPICS.size()];
        for (int i = 0; i < labels.length; i++) labels[i] = HelpFaq.TOPICS.get(i).question;
        ui.dialog().setTitle("Câu hỏi thường gặp")
                .setItems(labels, (dialog, which) -> submit(labels[which]))
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void submitInput() {
        String question = input.getText().toString().trim();
        if (question.isEmpty()) return;
        if (state.submit(question, false)) input.setText("");
    }

    private void submit(String question) {
        state.submit(question, true);
    }

    private void render() {
        if (turns == null) return;
        boolean followLatest = firstRender || !conversation.canScrollVertically(1);
        Set<Long> kept = new HashSet<>();
        for (HelpChatState.Turn turn : state.turns()) kept.add(turn.id);
        Iterator<Map.Entry<Long, TurnViews>> iterator = renderedTurns.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, TurnViews> entry = iterator.next();
            if (kept.contains(entry.getKey())) continue;
            turns.removeView(entry.getValue().root);
            iterator.remove();
        }
        for (HelpChatState.Turn turn : state.turns()) {
            TurnViews row = renderedTurns.get(turn.id);
            if (row == null) {
                row = createTurn(turn);
                renderedTurns.put(turn.id, row);
                ui.add(turns, row.root);
                if (!firstRender) Motion.reveal(row.root);
            }
            boolean waiting = turn.kind == HelpChatState.Kind.PENDING;
            row.reply.sender.setText(turn.kind == HelpChatState.Kind.FAQ || turn.kind == HelpChatState.Kind.FALLBACK
                    ? "Hướng dẫn có sẵn" : "Trợ lý AI");
            String answer = waiting ? "Đang trả lời…" : turn.answer;
            if (!answer.contentEquals(row.reply.body.getText())) row.reply.body.setText(answer);
            if (row.reply.body.isTextSelectable() == waiting) row.reply.body.setTextIsSelectable(!waiting);
            row.notice.setText(turn.notice);
            row.notice.setVisibility(turn.notice.isEmpty() ? View.GONE : View.VISIBLE);
            row.retry.setVisibility(turn.kind == HelpChatState.Kind.FALLBACK ? View.VISIBLE : View.GONE);
            row.retry.setEnabled(!state.busy());
            row.retry.setAlpha(state.busy() ? 0.45f : 1f);
        }
        for (View control : topicControls) {
            control.setEnabled(!state.busy());
            control.setAlpha(state.busy() ? 0.45f : 1f);
        }
        if (state.turns().isEmpty() && !input.getText().toString().equals(state.draft())) {
            input.setText(state.draft());
        }
        updateSend();
        if (followLatest) scrollToLatest(!firstRender);
        firstRender = false;
    }

    private TurnViews createTurn(HelpChatState.Turn turn) {
        LinearLayout root = ui.column();
        addBubble(root, turn.question, true);
        Bubble reply = addBubble(root, "", false);
        TextView notice = ui.text("", 12, Ui.MUTED, false);
        notice.setVisibility(View.GONE);
        LinearLayout.LayoutParams noticeParams = ui.lp(-1, -2);
        noticeParams.topMargin = ui.dp(6);
        root.addView(notice, noticeParams);
        TextView retry = ui.link("Thử lại", 13, () -> state.retry(turn.id));
        ui.pad(retry, 8, 8);
        retry.setVisibility(View.GONE);
        root.addView(retry, ui.lp(-2, -2));
        return new TurnViews(root, reply, notice, retry);
    }

    private Bubble addBubble(LinearLayout parent, String text, boolean outgoing) {
        TextView sender = null;
        if (!outgoing) {
            sender = ui.text("Trợ lý hỗ trợ", 12, Ui.MUTED, true);
            LinearLayout.LayoutParams label = ui.lp(-2, -2);
            label.topMargin = ui.dp(12);
            label.bottomMargin = ui.dp(5);
            parent.addView(sender, label);
        }
        TextView bubble = ui.text(text, 14, outgoing ? Ui.WHITE : Ui.INK, false);
        ui.surface(bubble, outgoing ? Ui.BLUE : Ui.WHITE, 16, outgoing ? 0 : Ui.BORDER);
        ui.pad(bubble, 14, 11);
        bubble.setTextIsSelectable(true);
        LinearLayout.LayoutParams params = ui.lp(-2, -2);
        params.gravity = outgoing ? Gravity.END : Gravity.START;
        if (outgoing) {
            params.setMarginStart(ui.dp(42));
            params.topMargin = ui.dp(12);
        } else {
            params.setMarginEnd(ui.dp(42));
        }
        parent.addView(bubble, params);
        return new Bubble(sender, bubble);
    }

    private void scrollToLatest(boolean animate) {
        if (conversation == null || messages == null) return;
        ScrollView scroll = conversation;
        LinearLayout content = messages;
        OneShotPreDrawListener.add(scroll, () -> {
            int bottom = Math.max(0, content.getHeight() - scroll.getHeight());
            if (animate && Motion.enabled()) scroll.smoothScrollTo(0, bottom);
            else scroll.scrollTo(0, bottom);
        });
    }

    @Override public void onDestroyView() {
        input = null;
        send = null;
        conversation = null;
        messages = null;
        turns = null;
        renderedTurns.clear();
        topicControls.clear();
        firstRender = true;
        ui = null;
        super.onDestroyView();
    }

    private static final class Bubble {
        final TextView sender;
        final TextView body;

        Bubble(TextView sender, TextView body) {
            this.sender = sender;
            this.body = body;
        }
    }

    private static final class TurnViews {
        final LinearLayout root;
        final Bubble reply;
        final TextView notice;
        final TextView retry;

        TurnViews(LinearLayout root, Bubble reply, TextView notice, TextView retry) {
            this.root = root;
            this.reply = reply;
            this.notice = notice;
            this.retry = retry;
        }
    }
}
