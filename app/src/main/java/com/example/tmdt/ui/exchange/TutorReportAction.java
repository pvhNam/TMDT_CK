package com.example.tmdt.ui.exchange;

import android.view.View;
import android.widget.ImageButton;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import com.example.tmdt.MainActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * UC27 – Nút ⋮ trên trang hồ sơ gia sư: "Báo cáo gia sư". Ẩn khi chưa đăng nhập hoặc đang xem hồ sơ của chính mình.
 * Hồ sơ gia sư chỉ cần gọi {@link #bind} một dòng.
 */
public final class TutorReportAction {
    private TutorReportAction() {}

    public static void bind(Fragment fragment, ImageButton button, String tutorUid, String tutorName) {
        FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
        boolean show = me != null && tutorUid != null && !tutorUid.isEmpty() && !tutorUid.equals(me.getUid());
        button.setVisibility(show ? View.VISIBLE : View.GONE);
        if (!show) { button.setOnClickListener(null); return; }
        button.setContentDescription("Thêm thao tác");
        button.setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(fragment.requireContext(), v);
            menu.getMenu().add("Báo cáo gia sư");
            menu.setOnMenuItemClickListener(item -> {
                ((MainActivity) fragment.requireActivity()).exchange.openTutorReport(tutorUid, tutorName);
                return true;
            });
            menu.show();
        });
    }
}
