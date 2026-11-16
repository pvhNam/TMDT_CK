package com.example.tmdt.ui.exchange;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;
import java.util.ArrayList;
import java.util.List;

/** Adapter RecyclerView gọn cho danh sách một loại dòng dùng View Binding. */
public final class SimpleAdapter<T, B extends ViewBinding> extends RecyclerView.Adapter<SimpleAdapter.Holder<B>> {
    public interface Inflate<B> { B inflate(LayoutInflater inflater, ViewGroup parent, boolean attach); }
    public interface Bind<T, B> { void bind(B binding, T item); }

    public static final class Holder<B extends ViewBinding> extends RecyclerView.ViewHolder {
        final B binding;
        Holder(B binding) { super(binding.getRoot()); this.binding = binding; }
    }

    private final Inflate<B> inflate;
    private final Bind<T, B> bind;
    private final List<T> items = new ArrayList<>();
    private final List<String> ids = new ArrayList<>();
    private final List<Object> snapshots = new ArrayList<>();
    private final java.util.function.Function<T,String> key;
    private final java.util.function.Function<T,?> viewState;

    public SimpleAdapter(Inflate<B> inflate, java.util.function.Function<T,String> key, Bind<T, B> bind) {
        this(inflate,key,null,bind);
    }
    public SimpleAdapter(Inflate<B> inflate, java.util.function.Function<T,String> key,
                         java.util.function.Function<T,?> viewState, Bind<T, B> bind) {
        this.inflate=inflate;this.key=key;this.viewState=viewState;this.bind=bind;
    }

    public void submit(List<T> values) {
        List<T> next=new ArrayList<>(values);List<String> nextIds=new ArrayList<>();List<Object> nextSnapshots=new ArrayList<>();
        for(T item:next){nextIds.add(key.apply(item));nextSnapshots.add(com.example.tmdt.ui.common.RenderSnapshot.of(item,viewState==null?null:viewState.apply(item)));}
        androidx.recyclerview.widget.DiffUtil.DiffResult diff=androidx.recyclerview.widget.DiffUtil.calculateDiff(new androidx.recyclerview.widget.DiffUtil.Callback(){
            @Override public int getOldListSize(){return ids.size();}
            @Override public int getNewListSize(){return nextIds.size();}
            @Override public boolean areItemsTheSame(int oldPosition,int newPosition){return ids.get(oldPosition).equals(nextIds.get(newPosition));}
            @Override public boolean areContentsTheSame(int oldPosition,int newPosition){return snapshots.get(oldPosition).equals(nextSnapshots.get(newPosition));}
        });
        items.clear();items.addAll(next);ids.clear();ids.addAll(nextIds);snapshots.clear();snapshots.addAll(nextSnapshots);
        diff.dispatchUpdatesTo(this);
    }
    @Override public int getItemCount() { return items.size(); }
    @NonNull @Override public Holder<B> onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new Holder<>(inflate.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }
    @Override public void onBindViewHolder(@NonNull Holder<B> holder, int position) {
        bind.bind(holder.binding, items.get(position));
        com.example.tmdt.ui.common.ThemeColors.apply(holder.itemView);
        com.example.tmdt.ui.common.Motion.bindControls(holder.itemView);
    }
}
