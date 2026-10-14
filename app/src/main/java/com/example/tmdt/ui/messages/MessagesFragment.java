package com.example.tmdt.ui.messages;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.tmdt.ChatState;
import com.example.tmdt.Conversation;
import com.example.tmdt.MainActivity;
import com.example.tmdt.R;
import com.example.tmdt.databinding.FragmentMessagesBinding;
import com.google.android.material.tabs.TabLayout;
import java.util.List;

/**
 * Màn 05 – Tin nhắn (UC21). Xem các cuộc trò chuyện, tìm theo tên, lọc tin chưa đọc
 * và chọn một người để mở màn 06.
 */
public final class MessagesFragment extends Fragment {
    private FragmentMessagesBinding binding;
    private ChatState chat;
    private ConversationAdapter adapter;

    @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        binding = FragmentMessagesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        MainActivity host = (MainActivity) requireActivity();
        chat = new ViewModelProvider(requireActivity()).get(ChatState.class);
        adapter = new ConversationAdapter(item -> { host.hideKeyboard(); chat.open(item); host.show("chat"); });
        binding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.list.setAdapter(adapter);

        // Ô tìm kiếm: giữ từ khóa trong ViewModel để quay lại màn này vẫn còn.
        binding.search.setText(chat.query());
        binding.search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { if (!s.toString().equals(chat.query())) chat.query(s.toString()); }
        });
        binding.search.setOnEditorActionListener((v, actionId, event) -> { host.hideKeyboard(); return true; });

        // Tab "Tất cả" / "Chưa đọc".
        TabLayout.Tab selected = binding.tabs.getTabAt(chat.unreadOnly() ? 1 : 0);
        if (selected != null) selected.select();
        binding.tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { chat.unreadOnly(tab.getPosition() == 1); }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        chat.changes().observe(getViewLifecycleOwner(), ignored -> render(host));
        chat.start();
        render(host);
    }

    private void render(MainActivity host) {
        if (binding == null) return;
        if (!chat.signedIn()) {
            adapter.state(getString(R.string.chat_sign_in_needed), getString(R.string.chat_sign_in), () -> host.show("login"), false);
            return;
        }
        if (chat.listLoading() && !chat.hasConversations()) {
            adapter.state(getString(R.string.chat_loading), null, null, true); return;
        }
        if (!chat.listError().isEmpty() && !chat.hasConversations()) {
            adapter.state(chat.listError(), getString(R.string.chat_retry), chat::retryList, false); return;
        }
        List<Conversation> results = chat.results();
        if (results.isEmpty()) {
            if (!chat.hasConversations())
                adapter.state(getString(R.string.chat_empty), getString(R.string.chat_find_tutor), () -> host.show("home"), false);
            else if (chat.unreadOnly() && chat.query().trim().isEmpty())
                adapter.state(getString(R.string.chat_all_read), null, null, false);
            else adapter.state(getString(R.string.chat_no_match), null, null, false);
            return;
        }
        adapter.submit(results, chat.myId());
    }

    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}
