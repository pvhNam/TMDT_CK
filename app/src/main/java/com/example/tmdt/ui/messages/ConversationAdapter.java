package com.example.tmdt.ui.messages;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.tmdt.Conversation;
import com.example.tmdt.R;
import com.example.tmdt.databinding.ItemConversationBinding;
import com.example.tmdt.databinding.ItemMessagesBannerBinding;
import com.example.tmdt.databinding.ItemMessagesStateBinding;
import java.util.ArrayList;
import java.util.List;

/** Danh sách màn 05: các hội thoại, một dòng trạng thái (khi cần) và thẻ "Trao đổi trước buổi học". */
final class ConversationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    interface OnOpen { void open(Conversation conversation); }

    private static final int ROW = 0, STATE = 1, BANNER = 2;
    private final OnOpen onOpen;
    private final List<Conversation> rows = new ArrayList<>();
    private String me = "";
    // Trạng thái hiển thị khi danh sách trống / đang tải / lỗi.
    private String stateText;
    private String stateAction;
    private Runnable stateRunnable;
    private boolean stateLoading;

    ConversationAdapter(OnOpen onOpen) { this.onOpen = onOpen; }

    void submit(List<Conversation> items, String myId) {
        rows.clear(); rows.addAll(items); me = myId == null ? "" : myId; stateText = null;
        notifyDataSetChanged();
    }
    void state(String text, String action, Runnable runnable, boolean loading) {
        rows.clear(); stateText = text; stateAction = action; stateRunnable = runnable; stateLoading = loading;
        notifyDataSetChanged();
    }

    private boolean hasState() { return stateText != null; }
    @Override public int getItemCount() { return rows.size() + (hasState() ? 1 : 0) + 1; }
    @Override public int getItemViewType(int position) {
        if (position == getItemCount() - 1) return BANNER;
        return hasState() ? STATE : ROW;
    }

    @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (type == ROW) return new RowHolder(ItemConversationBinding.inflate(inflater, parent, false));
        if (type == STATE) return new StateHolder(ItemMessagesStateBinding.inflate(inflater, parent, false));
        return new RecyclerView.ViewHolder(ItemMessagesBannerBinding.inflate(inflater, parent, false).getRoot()) {};
    }

    @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof RowHolder) ((RowHolder) holder).bind(rows.get(position));
        else if (holder instanceof StateHolder) ((StateHolder) holder).bind();
    }

    final class RowHolder extends RecyclerView.ViewHolder {
        private final ItemConversationBinding b;
        RowHolder(ItemConversationBinding binding) { super(binding.getRoot()); b = binding; }
        void bind(Conversation item) {
            ChatFormat.avatar(b.avatarPhoto, b.avatarInitials, item.peerId, item.peerName);
            b.name.setText(item.peerName);
            b.time.setText(ChatFormat.listTime(item.sortTime()));
            String preview = item.lastMessage.isEmpty()
                    ? b.getRoot().getContext().getString(R.string.chat_new_conversation)
                    : me.equals(item.lastSenderId) ? b.getRoot().getContext().getString(R.string.chat_you, item.lastMessage) : item.lastMessage;
            b.preview.setText(preview);
            boolean unread = item.unread > 0;
            b.badge.setVisibility(unread ? View.VISIBLE : View.GONE);
            b.badge.setText(item.unread > 99 ? "99+" : String.valueOf(item.unread));
            b.preview.setTextColor(b.getRoot().getContext().getColor(unread ? R.color.chat_ink : R.color.chat_muted));
            b.getRoot().setContentDescription(item.peerName + ". " + preview + (unread ? ". " + item.unread + " tin chưa đọc" : ""));
            b.getRoot().setOnClickListener(v -> onOpen.open(item));
        }
    }

    final class StateHolder extends RecyclerView.ViewHolder {
        private final ItemMessagesStateBinding b;
        StateHolder(ItemMessagesStateBinding binding) { super(binding.getRoot()); b = binding; }
        void bind() {
            b.progress.setVisibility(stateLoading ? View.VISIBLE : View.GONE);
            b.message.setText(stateText);
            b.action.setVisibility(stateAction == null ? View.GONE : View.VISIBLE);
            b.action.setText(stateAction);
            b.action.setOnClickListener(v -> { if (stateRunnable != null) stateRunnable.run(); });
        }
    }
}
