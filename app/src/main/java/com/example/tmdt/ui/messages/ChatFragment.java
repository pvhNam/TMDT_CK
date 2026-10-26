package com.example.tmdt.ui.messages;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.tmdt.ChatMessage;
import com.example.tmdt.ChatState;
import com.example.tmdt.Conversation;
import com.example.tmdt.MainActivity;
import com.example.tmdt.R;
import com.example.tmdt.databinding.FragmentChatBinding;
import com.google.android.material.snackbar.Snackbar;
import java.util.List;

/**
 * Màn 06 – Trò chuyện (UC21). Gửi tin nhắn, trao đổi mục tiêu và bài tập, xem buổi học liên quan
 * ngay trong cuộc trò chuyện. Tin có trạng thái đang gửi / đã gửi / đã xem / lỗi và gửi lại không nhân đôi.
 */
public final class ChatFragment extends Fragment {
    private static final int[] TEMPLATES = {
            R.string.chat_template_goal, R.string.chat_template_homework,
            R.string.chat_template_schedule, R.string.chat_template_thanks};

    private FragmentChatBinding binding;
    private ChatState chat;
    private MessageAdapter adapter;
    private LinearLayoutManager layout;
    private int shownCount = -1;

    @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        binding = FragmentChatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        MainActivity host = (MainActivity) requireActivity();
        chat = new ViewModelProvider(requireActivity()).get(ChatState.class);

        adapter = new MessageAdapter(id -> chat.retry(id));
        layout = new LinearLayoutManager(requireContext());
        layout.setStackFromEnd(true);  // Tin mới nhất ở cuối, sát ô soạn thảo.
        binding.messages.setLayoutManager(layout);
        binding.messages.setAdapter(adapter);
        binding.messages.setItemAnimator(null);
        // Bàn phím mở làm danh sách thấp lại: giữ tin cuối trong tầm nhìn.
        binding.messages.addOnLayoutChangeListener((v, l, t, r, bottom, ol, ot, or, oldBottom) -> {
            if (bottom < oldBottom && adapter.getItemCount() > 0) binding.messages.post(this::scrollToEnd);
        });

        binding.back.setOnClickListener(v -> host.back());
        binding.video.setOnClickListener(v -> snack(getString(R.string.chat_video_info)));
        binding.attach.setOnClickListener(this::showTemplates);
        // Nút ⋮: Hợp đồng dạy (UC23). Xem đánh giá (UC26), Báo cáo vi phạm (UC27) sẽ thêm khi push các UC đó.
        binding.more.setOnClickListener(this::showMore);

        binding.input.setText(chat.draft());
        binding.input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { chat.draft(s.toString()); updateSend(); }
        });
        binding.send.setOnClickListener(v -> send());
        updateSend();

        chat.changes().observe(getViewLifecycleOwner(), ignored -> render());
        render();
    }

    @Override public void onResume() { super.onResume(); chat.visible(true); }
    @Override public void onPause() { chat.visible(false); super.onPause(); }

    private void send() {
        if (binding == null) return;
        String text = binding.input.getText() == null ? "" : binding.input.getText().toString();
        if (chat.send(text)) { binding.input.setText(""); scrollToEnd(); }
    }

    private void updateSend() {
        if (binding == null) return;
        boolean ready = binding.input.getText() != null && !binding.input.getText().toString().trim().isEmpty();
        binding.send.setEnabled(ready);
        binding.send.setAlpha(ready ? 1f : 0.45f);
    }

    /** Nút (+): chèn mẫu câu để trao đổi mục tiêu, bài tập, lịch học. */
    private void showTemplates(View anchor) {
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        for (int i = 0; i < TEMPLATES.length; i++) menu.getMenu().add(Menu.NONE, i, i, getString(TEMPLATES[i]).trim());
        menu.setOnMenuItemClickListener(item -> {
            String template = getString(TEMPLATES[item.getItemId()]);
            Editable text = binding.input.getText();
            if (text == null) return true;
            int at = Math.max(0, binding.input.getSelectionStart());
            String prefix = at > 0 && text.charAt(at - 1) != ' ' && text.charAt(at - 1) != '\n' ? "\n" : "";
            text.insert(at, prefix + template);
            binding.input.requestFocus();
            binding.input.setSelection(Math.min(text.length(), at + prefix.length() + template.length()));
            android.view.inputmethod.InputMethodManager keyboard = requireContext().getSystemService(android.view.inputmethod.InputMethodManager.class);
            if (keyboard != null) keyboard.showSoftInput(binding.input, 0);
            return true;
        });
        menu.show();
    }

    private void showMore(View anchor) {
        Conversation conversation = chat.conversation();
        if (conversation == null || conversation.peerId.isEmpty()) return;
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        menu.getMenu().add(Menu.NONE, 1, 1, R.string.chat_contract);
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) ((MainActivity) requireActivity()).exchange.openContractFor(conversation);
            return true;
        });
        menu.show();
    }

    private void render() {
        if (binding == null) return;
        Conversation conversation = chat.conversation();
        if (chat.openId() == null || conversation == null) return;  // Đang rời màn hình.

        // Đầu trang
        ChatFormat.avatar(binding.avatarPhoto, binding.avatarInitials, conversation.peerId, conversation.peerName);
        binding.name.setText(conversation.peerName);
        boolean online = chat.peerOnline();
        binding.onlineDot.setVisibility(online ? View.VISIBLE : View.GONE);
        String status;
        if (online) status = getString(R.string.chat_online);
        else if (chat.peerLastSeen() > 0) status = ChatFormat.lastSeen(chat.peerLastSeen());
        else status = getString("TUTOR".equals(conversation.peerRole) ? R.string.chat_role_tutor : R.string.chat_role_student);
        binding.status.setText(status);
        binding.status.setTextColor(requireContext().getColor(online ? R.color.chat_green : R.color.chat_muted));

        // Buổi học liên quan
        binding.lessonCard.setVisibility(conversation.hasLesson() ? View.VISIBLE : View.GONE);
        if (conversation.hasLesson()) {
            ChatFormat.avatar(binding.lessonPhoto, binding.lessonInitials, conversation.peerId, conversation.peerName);
            binding.lessonTitle.setText(conversation.lessonTitle);
            binding.lessonTime.setText(conversation.lessonTime);
            binding.lessonStatus.setText(conversation.lessonStatus);
            binding.lessonStatus.setVisibility(conversation.lessonStatus.isEmpty() ? View.GONE : View.VISIBLE);
        }

        // Tin nhắn
        List<ChatMessage> messages = chat.messages();
        adapter.submit(messages, conversation.peerName, conversation.peerId);
        if (messages.size() != shownCount) { shownCount = messages.size(); scrollToEnd(); }

        boolean loading = chat.messagesLoading() && messages.isEmpty();
        String error = chat.chatError();
        boolean showState = messages.isEmpty() && (loading || !error.isEmpty() || !chat.messagesLoading());
        binding.state.setVisibility(showState ? View.VISIBLE : View.GONE);
        binding.progress.setVisibility(loading && error.isEmpty() ? View.VISIBLE : View.GONE);
        binding.stateText.setText(!error.isEmpty() ? error : loading ? getString(R.string.chat_loading) : getString(R.string.chat_start_hint));

        String notice = chat.takeNotice();
        if (!notice.isEmpty()) snack(notice);
    }

    private void scrollToEnd() {
        if (binding != null && adapter.getItemCount() > 0) binding.messages.scrollToPosition(adapter.getItemCount() - 1);
    }

    private void snack(String text) {
        if (binding == null) return;
        Snackbar.make(binding.getRoot(), text, Snackbar.LENGTH_LONG).show();
    }

    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}
