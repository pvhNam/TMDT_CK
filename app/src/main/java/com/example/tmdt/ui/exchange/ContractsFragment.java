package com.example.tmdt.ui.exchange;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.tmdt.Contract;
import com.example.tmdt.ContractState;
import com.example.tmdt.Exchange;
import com.example.tmdt.MainActivity;
import com.example.tmdt.R;
import com.example.tmdt.databinding.FragmentListScreenBinding;
import com.example.tmdt.databinding.ItemContractBinding;
import java.util.ArrayList;
import java.util.List;

/** Hợp đồng của tôi: các hợp đồng đang chờ, đã chốt hoặc đã đóng (mở từ trang Cá nhân). */
public final class ContractsFragment extends Fragment {
    private FragmentListScreenBinding binding;
    private ContractState contracts;
    private SimpleAdapter<Contract, ItemContractBinding> adapter;
    private int tab;

    @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        binding = FragmentListScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        Exchange exchange = ((MainActivity) requireActivity()).exchange;
        contracts = exchange.contracts;
        if (state != null) tab = state.getInt("tab");
        ExUi.header(binding.header, getString(R.string.ex_contracts_title), null, exchange::back);
        adapter = new SimpleAdapter<>(ItemContractBinding::inflate, (b, c) -> {
            String me = contracts.myId();
            b.subject.setText(c.subject);
            b.code.setText(c.code + " · " + ExUi.date(c.createdAt));
            ExUi.pill(b.status, c.statusLabel(), ExUi.kind(c));
            b.party.setText((me.equals(c.studentId) ? "Gia sư: " : "Học viên: ") + c.otherName(me));
            b.schedule.setText(c.sessionsLabel() + " · " + c.schedule);
            b.total.setText(ExUi.money(c.total()));
            b.getRoot().setOnClickListener(v -> exchange.openContract(c));
        });
        ListScreen.setup(binding, adapter);
        ListScreen.tabs(binding, tab, position -> { tab = position; render(); }, "Đang thực hiện", "Đã đóng");
        contracts.changes().observe(getViewLifecycleOwner(), ignored -> render());
        contracts.start();
        render();
    }

    private void render() {
        if (binding == null) return;
        if (contracts.loading() && contracts.contracts().isEmpty()) { ListScreen.state(binding, "Đang tải hợp đồng…", null, null); return; }
        if (!contracts.loadError().isEmpty()) { ListScreen.state(binding, contracts.loadError(), getString(R.string.chat_retry), contracts::retry); return; }
        List<Contract> items = new ArrayList<>();
        for (Contract c : contracts.contracts()) if (c.closed() == (tab == 1)) items.add(c);
        if (items.isEmpty()) {
            ListScreen.state(binding, tab == 0 ? "Chưa có hợp đồng nào. Hãy trao đổi với gia sư qua tin nhắn rồi chọn \"Hợp đồng dạy\" để lập đề nghị." : "Không có hợp đồng đã đóng.",
                    tab == 0 ? getString(R.string.chat_title) : null, () -> ((MainActivity) requireActivity()).show("messages"));
            return;
        }
        ListScreen.state(binding, null, null, null);
        adapter.submit(items);
    }

    @Override public void onSaveInstanceState(@NonNull Bundle out) { super.onSaveInstanceState(out); out.putInt("tab", tab); }
    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}
