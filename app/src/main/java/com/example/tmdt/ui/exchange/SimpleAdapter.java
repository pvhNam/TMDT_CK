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

    public SimpleAdapter(Inflate<B> inflate, Bind<T, B> bind) { this.inflate = inflate; this.bind = bind; }

    public void submit(List<T> values) { items.clear(); items.addAll(values); notifyDataSetChanged(); }
    @Override public int getItemCount() { return items.size(); }
    @NonNull @Override public Holder<B> onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new Holder<>(inflate.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }
    @Override public void onBindViewHolder(@NonNull Holder<B> holder, int position) { bind.bind(holder.binding, items.get(position)); }
}
