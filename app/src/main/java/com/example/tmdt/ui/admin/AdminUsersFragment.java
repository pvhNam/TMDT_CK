package com.example.tmdt.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.tmdt.AdminState;
import com.example.tmdt.CatalogState;
import com.example.tmdt.Exchange;
import com.example.tmdt.MainActivity;
import com.example.tmdt.ManagedUser;
import com.example.tmdt.R;
import com.example.tmdt.databinding.FragmentAdminUsersBinding;
import com.example.tmdt.databinding.ItemAdminUserBinding;
import com.example.tmdt.ui.exchange.ExUi;
import com.example.tmdt.ui.exchange.SimpleAdapter;
import com.google.android.material.chip.Chip;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Màn 51 – Quản lý tài khoản (UC30): tìm theo tên/email, lọc vai trò/trạng thái, xem thông tin, khóa hoặc mở khóa. */
public final class AdminUsersFragment extends Fragment {
    private static final int ALL = 0, STUDENTS = 1, TUTORS = 2, BANNED = 3;
    private static final String[] FILTER_NAMES = {"Tất cả", "Học viên", "Gia sư", "Đã khóa"};
    private FragmentAdminUsersBinding binding;
    private AdminState admin;
    private SimpleAdapter<ManagedUser, ItemAdminUserBinding> adapter;
    private final Set<String> expanded = new HashSet<>();
    private int filter;
    private String query = "";

    @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        binding = FragmentAdminUsersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        Exchange exchange = ((MainActivity) requireActivity()).exchange;
        admin = exchange.admin;
        if (state != null) {
            filter = state.getInt("filter"); query = state.getString("query", "");
            ArrayList<String> open = state.getStringArrayList("expanded"); if (open != null) expanded.addAll(open);
        }
        ExUi.header(binding.header, getString(R.string.ex_users_title), getString(R.string.ex_admin_subtitle), exchange::back);
        binding.search.setText(query);
        ExUi.afterChange(binding.search, value -> { query = value; render(); });
        binding.search.setOnEditorActionListener((v, id, e) -> { ((MainActivity) requireActivity()).hideKeyboard(); return true; });
        for (int i = 0; i < FILTER_NAMES.length; i++) {
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.view_ex_chip, binding.filters, false);
            chip.setId(View.generateViewId()); chip.setCheckedIconVisible(false);
            final int value = i;
            chip.setOnClickListener(v -> { filter = value; render(); });
            binding.filters.addView(chip);
        }
        adapter = new SimpleAdapter<>(ItemAdminUserBinding::inflate, this::bind);
        binding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.list.setAdapter(adapter);
        admin.changes().observe(getViewLifecycleOwner(), ignored -> render());
        admin.startUsers();
        render();
    }

    private boolean matches(ManagedUser u, int f) {
        switch (f) {
            case STUDENTS: return !u.isTutor() && !u.admin();
            case TUTORS: return u.isTutor();
            case BANNED: return u.banned();
            default: return true;
        }
    }

    private void render() {
        if (binding == null) return;
        String notice = admin.takeNotice();
        if (!notice.isEmpty()) ExUi.snack(binding.getRoot(), notice);
        List<ManagedUser> all = admin.users();
        for (int i = 0; i < binding.filters.getChildCount(); i++) {
            int count = 0; for (ManagedUser u : all) if (matches(u, i)) count++;
            Chip chip = (Chip) binding.filters.getChildAt(i);
            chip.setText(FILTER_NAMES[i] + " (" + String.format(java.util.Locale.forLanguageTag("vi-VN"), "%,d", count) + ")");
            chip.setChecked(i == filter);
        }
        String key = CatalogState.normalize(query);
        List<ManagedUser> items = new ArrayList<>();
        for (ManagedUser u : all) {
            if (!matches(u, filter)) continue;
            if (!key.isEmpty() && !CatalogState.normalize(u.name).contains(key) && !u.email.toLowerCase(java.util.Locale.ROOT).contains(key)) continue;
            items.add(u);
        }
        String state = admin.usersLoading() && all.isEmpty() ? "Đang tải tài khoản…"
                : !admin.usersError().isEmpty() ? admin.usersError()
                : items.isEmpty() ? "Không có tài khoản phù hợp." : null;
        binding.state.setVisibility(state == null ? View.GONE : View.VISIBLE);
        if (state != null) binding.state.setText(state);
        adapter.submit(items);
    }

    private void bind(ItemAdminUserBinding b, ManagedUser u) {
        boolean open = expanded.contains(u.id);
        ExUi.avatar(b.avatar.avatarPhoto, b.avatar.avatarInitials, u.avatar, u.name);
        b.name.setText(u.name);
        ExUi.pill(b.role, u.roleLabel(), ExUi.INFO);
        ExUi.pill(b.status, u.banned() ? "Tạm khóa" : "Hoạt động", u.banned() ? ExUi.WAIT : ExUi.OK);
        b.chevron.setImageResource(open ? R.drawable.ic_ex_chevron_up : R.drawable.ic_ex_chevron_down);
        b.summary.setContentDescription(u.name + ", " + u.roleLabel() + ", " + (u.banned() ? "tạm khóa" : "hoạt động"));
        b.summary.setOnClickListener(v -> { if (!expanded.remove(u.id)) expanded.add(u.id); adapter.notifyDataSetChanged(); });
        b.details.setVisibility(open ? View.VISIBLE : View.GONE);
        if (!open) return;
        b.email.setText("Email: " + (u.email.isEmpty() ? "—" : u.email));
        b.phone.setText("Số điện thoại: " + (u.phone.isEmpty() ? "—" : u.phone));
        b.created.setText("Ngày đăng ký: " + (u.createdAt > 0 ? ExUi.date(u.createdAt) : "—"));
        b.region.setText("Khu vực: " + (u.region.isEmpty() ? "—" : u.region));
        boolean self = u.id.equals(admin.myId());
        boolean protectedAccount = u.admin() || self;
        b.lock.setVisibility(protectedAccount ? View.GONE : View.VISIBLE);
        b.note.setVisibility(protectedAccount ? View.VISIBLE : View.GONE);
        b.note.setText(self ? "Đây là tài khoản của bạn." : "Tài khoản quản trị không thể khóa tại đây.");
        if (protectedAccount) return;
        // Đổi kiểu nút: khóa (viền đỏ) / mở khóa (viền xanh).
        int color = requireContext().getColor(u.banned() ? R.color.chat_blue : R.color.chat_red);
        b.lock.setText(u.banned() ? R.string.ex_unlock : R.string.ex_lock);
        b.lock.setIconResource(u.banned() ? R.drawable.ic_ex_unlock : R.drawable.ic_ex_lock);
        b.lock.setTextColor(color);
        b.lock.setIconTint(android.content.res.ColorStateList.valueOf(color));
        b.lock.setStrokeColor(android.content.res.ColorStateList.valueOf(color));
        b.lock.setEnabled(!admin.busy());
        b.lock.setOnClickListener(v -> confirm(u));
    }

    private void confirm(ManagedUser u) {
        boolean lock = !u.banned();
        EditText reason = new EditText(requireContext());
        reason.setHint(lock ? "Lý do khóa (bắt buộc)" : "Ghi chú (không bắt buộc)");
        reason.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(300)});
        LinearLayout box = new LinearLayout(requireContext());
        int pad = ExUi.dp(requireContext(), 20); box.setPadding(pad, 0, pad, 0); box.addView(reason, new LinearLayout.LayoutParams(-1, -2));
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle((lock ? "Khóa tài khoản " : "Mở khóa tài khoản ") + u.name + "?")
                .setMessage(lock ? "Người dùng sẽ bị đăng xuất ở lần tải hồ sơ tiếp theo và không thể gửi tin nhắn, hợp đồng hay báo cáo." : "Người dùng có thể đăng nhập và sử dụng lại ứng dụng.")
                .setView(box).setNegativeButton(R.string.cancel, null)
                .setPositiveButton(lock ? R.string.ex_lock : R.string.ex_unlock, null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (lock && ExUi.text(reason).length() < 3) { reason.setError("Nhập lý do khóa"); return; }
            admin.setLocked(u, lock, ExUi.text(reason)); dialog.dismiss();
        }));
        dialog.show();
    }

    @Override public void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("filter", filter); out.putString("query", query); out.putStringArrayList("expanded", new ArrayList<>(expanded));
    }
    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}
