package com.example.tmdt.ui.messages;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.tmdt.ChatMessage;
import com.example.tmdt.R;
import com.example.tmdt.databinding.ItemChatDayBinding;
import com.example.tmdt.databinding.ItemChatInBinding;
import com.example.tmdt.databinding.ItemChatOutBinding;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Nội dung màn 06: nhãn ngày, tin nhận (trái) và tin gửi (phải) kèm trạng thái gửi. */
final class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    interface OnRetry { void retry(String messageId); }

    private static final int DAY = 0, IN = 1, OUT = 2;
    private final OnRetry onRetry;
    /** Mỗi phần tử là String (nhãn ngày) hoặc ChatMessage. */
    private final List<Object> rows = new ArrayList<>();
    private String peerName = "",peerId="";

    MessageAdapter(OnRetry onRetry) { this.onRetry = onRetry; }

    void submit(List<ChatMessage> messages, String peer, String uid) {
        List<Object> next=snapshot(messages);
        String nextPeer=peer==null?"":peer,nextUid=uid==null?"":uid;
        boolean samePeer=peerName.equals(nextPeer)&&peerId.equals(nextUid);peerName=nextPeer;peerId=nextUid;
        androidx.recyclerview.widget.DiffUtil.DiffResult diff=diff(rows,next,samePeer);
        rows.clear();rows.addAll(next);diff.dispatchUpdatesTo(this);
    }
    static List<Object> snapshot(List<ChatMessage> messages){
        List<Object> next=new ArrayList<>();
        LocalDate previous = null;
        for (ChatMessage message : messages) {
            LocalDate date = ChatFormat.date(message.sentAt);
            if (!date.equals(previous)) { next.add(ChatFormat.day(message.sentAt)); previous = date; }
            // Keep a snapshot: read receipts may update the source model in place.
            next.add(new ChatMessage(message.id,message.senderId,message.content,message.sentAt,message.status,message.mine));
        }
        return next;
    }
    static androidx.recyclerview.widget.DiffUtil.DiffResult diff(List<Object> rows,List<Object> next,boolean samePeer){
        return androidx.recyclerview.widget.DiffUtil.calculateDiff(new androidx.recyclerview.widget.DiffUtil.Callback(){
            @Override public int getOldListSize(){return rows.size();}
            @Override public int getNewListSize(){return next.size();}
            @Override public boolean areItemsTheSame(int oldPosition,int newPosition){
                Object before=rows.get(oldPosition),after=next.get(newPosition);
                if(before instanceof String||after instanceof String)return before instanceof String&&before.equals(after);
                ChatMessage a=(ChatMessage)before,b=(ChatMessage)after;return a.id.equals(b.id)&&a.mine==b.mine;
            }
            @Override public boolean areContentsTheSame(int oldPosition,int newPosition){
                Object before=rows.get(oldPosition),after=next.get(newPosition);
                if(before instanceof String)return before.equals(after);
                ChatMessage a=(ChatMessage)before,b=(ChatMessage)after;
                return samePeer&&a.content.equals(b.content)&&a.sentAt==b.sentAt&&a.status==b.status;
            }
        });
    }

    @Override public int getItemCount() { return rows.size(); }
    @Override public int getItemViewType(int position) {
        Object row = rows.get(position);
        if (row instanceof String) return DAY;
        return ((ChatMessage) row).mine ? OUT : IN;
    }

    @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (type == DAY) return new DayHolder(ItemChatDayBinding.inflate(inflater, parent, false));
        if (type == IN) return new InHolder(ItemChatInBinding.inflate(inflater, parent, false));
        return new OutHolder(ItemChatOutBinding.inflate(inflater, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object row = rows.get(position);
        if (holder instanceof DayHolder) ((DayHolder) holder).b.day.setText((String) row);
        else if (holder instanceof InHolder) ((InHolder) holder).bind((ChatMessage) row);
        else ((OutHolder) holder).bind((ChatMessage) row);
        com.example.tmdt.ui.common.ThemeColors.apply(holder.itemView);
        com.example.tmdt.ui.common.Motion.bindControls(holder.itemView);
    }

    static final class DayHolder extends RecyclerView.ViewHolder {
        final ItemChatDayBinding b;
        DayHolder(ItemChatDayBinding binding) { super(binding.getRoot()); b = binding; }
    }

    final class InHolder extends RecyclerView.ViewHolder {
        private final ItemChatInBinding b;
        InHolder(ItemChatInBinding binding) { super(binding.getRoot()); b = binding; }
        void bind(ChatMessage message) {
            ChatFormat.avatar(b.avatarPhoto, b.avatarInitials, peerId, peerName);
            b.bubble.setText(message.content);
            b.time.setText(ChatFormat.clock(message.sentAt));
        }
    }

    final class OutHolder extends RecyclerView.ViewHolder {
        private final ItemChatOutBinding b;
        OutHolder(ItemChatOutBinding binding) { super(binding.getRoot()); b = binding; }
        void bind(ChatMessage message) {
            android.content.Context context = b.getRoot().getContext();
            b.bubble.setText(message.content);
            b.time.setText(ChatFormat.clock(message.sentAt));
            boolean failed = message.status == ChatMessage.FAILED;
            b.bubble.setAlpha(message.status == ChatMessage.SENDING || failed ? 0.7f : 1f);
            b.error.setVisibility(failed ? View.VISIBLE : View.GONE);
            int icon, color, description;
            switch (message.status) {
                case ChatMessage.SENDING: icon = R.drawable.ic_chat_clock; color = R.color.chat_muted; description = R.string.chat_sending; break;
                case ChatMessage.READ: icon = R.drawable.ic_chat_check_double; color = R.color.chat_blue; description = 0; break;
                case ChatMessage.FAILED: icon = 0; color = R.color.chat_red; description = R.string.chat_failed; break;
                default: icon = R.drawable.ic_chat_check; color = R.color.chat_muted; description = 0;
            }
            b.status.setVisibility(icon == 0 ? View.GONE : View.VISIBLE);
            if (icon != 0) { b.status.setImageResource(icon); b.status.setImageTintList(ColorStateList.valueOf(context.getColor(color))); }
            String state = message.status == ChatMessage.READ ? "Đã xem" : description == 0 ? "Đã gửi" : context.getString(description);
            b.getRoot().setContentDescription(message.content + ". " + state);
            View.OnClickListener retry = failed ? v -> onRetry.retry(message.id) : null;
            b.getRoot().setOnClickListener(retry);
            b.error.setOnClickListener(retry);
            b.getRoot().setClickable(failed);
        }
    }
}
