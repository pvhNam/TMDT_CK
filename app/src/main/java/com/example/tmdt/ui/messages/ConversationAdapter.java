package com.example.tmdt.ui.messages;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.tmdt.Conversation;
import com.example.tmdt.R;
import com.example.tmdt.databinding.ItemConversationBinding;
import com.example.tmdt.databinding.ItemMessagesStateBinding;
import java.util.ArrayList;
import java.util.List;

/** Conversations with loading and error feedback. */
final class ConversationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    interface OnOpen { void open(Conversation conversation); }

    private static final int ROW = 0, STATE = 1;
    private final OnOpen onOpen;
    private final List<Conversation> rows = new ArrayList<>();
    private final List<String> renderedKeys = new ArrayList<>();
    private String me = "";
    // Trạng thái hiển thị khi danh sách trống / đang tải / lỗi.
    private String stateText;
    private String stateAction;
    private Runnable stateRunnable;
    private boolean stateLoading;

    ConversationAdapter(OnOpen onOpen) { this.onOpen = onOpen; }

    void submit(List<Conversation> items, String myId) {
        boolean hadState=hasState();me=myId==null?"":myId;
        List<Conversation> next=new ArrayList<>(items);
        List<String> nextKeys=keys(next,me);
        androidx.recyclerview.widget.DiffUtil.DiffResult diff=hadState?null:diff(rows,next,renderedKeys,nextKeys);
        rows.clear();rows.addAll(next);renderedKeys.clear();renderedKeys.addAll(nextKeys);stateText=null;
        if(diff==null)notifyDataSetChanged();else diff.dispatchUpdatesTo(this);
    }
    static List<String> keys(List<Conversation> items,String me){
        List<String> keys=new ArrayList<>();
        for(Conversation item:items)keys.add(me+"|"+item.peerId+"|"+item.peerName+"|"+item.peerRole+"|"+item.lastMessage+"|"+item.lastSenderId+"|"+item.sortTime()+"|"+item.unread);
        return keys;
    }
    static androidx.recyclerview.widget.DiffUtil.DiffResult diff(List<Conversation> rows,List<Conversation> next,List<String> renderedKeys,List<String> nextKeys){
        return androidx.recyclerview.widget.DiffUtil.calculateDiff(new androidx.recyclerview.widget.DiffUtil.Callback(){
            @Override public int getOldListSize(){return rows.size();}
            @Override public int getNewListSize(){return next.size();}
            @Override public boolean areItemsTheSame(int oldPosition,int newPosition){return rows.get(oldPosition).id.equals(next.get(newPosition).id);}
            @Override public boolean areContentsTheSame(int oldPosition,int newPosition){return renderedKeys.get(oldPosition).equals(nextKeys.get(newPosition));}
        });
    }
    void state(String text, String action, Runnable runnable, boolean loading) {
        boolean same=hasState()&&java.util.Objects.equals(stateText,text)&&java.util.Objects.equals(stateAction,action)&&stateLoading==loading;
        rows.clear(); renderedKeys.clear(); stateText = text; stateAction = action; stateRunnable = runnable; stateLoading = loading;
        if(!same)notifyDataSetChanged();
    }

    private boolean hasState() { return stateText != null; }
    @Override public int getItemCount() { return rows.size() + (hasState() ? 1 : 0); }
    @Override public int getItemViewType(int position) {
        return hasState() ? STATE : ROW;
    }

    @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (type == ROW) return new RowHolder(ItemConversationBinding.inflate(inflater, parent, false));
        return new StateHolder(ItemMessagesStateBinding.inflate(inflater, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof RowHolder) ((RowHolder) holder).bind(rows.get(position));
        else if (holder instanceof StateHolder) ((StateHolder) holder).bind();
        com.example.tmdt.ui.common.ThemeColors.apply(holder.itemView);
        com.example.tmdt.ui.common.Motion.bindControls(holder.itemView);
    }

    final class RowHolder extends RecyclerView.ViewHolder {
        private final ItemConversationBinding b;
        RowHolder(ItemConversationBinding binding) { super(binding.getRoot()); b = binding; }
        void bind(Conversation item) {
            ChatFormat.avatar(b.avatarPhoto, b.avatarInitials, item.peerId, item.peerName);
            b.name.setText(item.peerName);
            String role="TUTOR".equals(item.peerRole)?"Gia sư":"STUDENT".equals(item.peerRole)?"Học viên":"";
            b.role.setText(role);b.role.setVisibility(role.isEmpty()?View.GONE:View.VISIBLE);
            b.time.setText(ChatFormat.listTime(item.sortTime()));
            String preview = item.lastMessage.isEmpty()
                    ? b.getRoot().getContext().getString(R.string.chat_new_conversation)
                    : me.equals(item.lastSenderId) ? b.getRoot().getContext().getString(R.string.chat_you, item.lastMessage) : item.lastMessage;
            b.preview.setText(preview);
            boolean unread = item.unread > 0;
            b.badge.setVisibility(unread ? View.VISIBLE : View.GONE);
            b.badge.setText(item.unread > 99 ? "99+" : String.valueOf(item.unread));
            b.preview.setTextColor(b.getRoot().getContext().getColor(unread ? R.color.chat_ink : R.color.chat_muted));
            b.preview.setTypeface(null,unread?android.graphics.Typeface.BOLD:android.graphics.Typeface.NORMAL);
            b.time.setTextColor(b.getRoot().getContext().getColor(unread?R.color.app_accent:R.color.chat_muted));
            b.getRoot().setStrokeColor(b.getRoot().getContext().getColor(unread?R.color.app_accent:R.color.chat_border));
            b.getRoot().setCardBackgroundColor(b.getRoot().getContext().getColor(unread?R.color.chat_pale:R.color.app_surface));
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
