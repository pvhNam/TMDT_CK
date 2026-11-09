package com.example.tmdt.ui.admin;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import com.example.tmdt.ManageState;
import com.example.tmdt.R;
import com.example.tmdt.ui.common.RenderSnapshot;
import com.example.tmdt.ui.common.Ui;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Phần chung của các màn thống kê và quản lý bổ sung cho quản trị viên.
 * Dùng khung trình bày của trang quản trị ({@link AdminScreen}); dữ liệu lấy từ {@link ManageState}.
 */
abstract class ManageScreen extends AdminScreen {
    static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    static final ZoneId ZONE = ZoneId.systemDefault();

    protected ManageState manage() { return new ViewModelProvider(requireActivity()).get(ManageState.class); }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        ManageState manage = manage();
        manage.changes().observe(getViewLifecycleOwner(), ignored -> {
            String notice = manage.takeNotice();
            if (!notice.isEmpty()) classroom.notice(notice);
            refreshData();
        });
        manage.start();
    }
    @Override protected Object dataSnapshot() { return RenderSnapshot.of(super.dataSnapshot(), manage().version()); }

    /** Đang tải hoặc lỗi thì hiện ghi chú và trả về true (màn dừng dựng phần còn lại). */
    protected boolean waiting(LinearLayout body, String... keys) {
        ManageState manage = manage();
        for (String key : keys) {
            String error = manage.error(key);
            if (!error.isEmpty()) {
                LinearLayout note = ui.note(R.drawable.ic_info, error + " Chạm để thử lại.", Ui.ORANGE_BG, Ui.ORANGE, Ui.INK);
                ui.clickable(note, manage::retry); ui.add(body, note); return true;
            }
        }
        for (String key : keys) if (!manage.loaded(key)) { ui.add(body, ui.note(R.drawable.ic_clock, "Đang tải dữ liệu…", Ui.PALE, Ui.BLUE, Ui.INK)); return true; }
        return false;
    }

    static String date(long millis) { return millis <= 0 ? "—" : Instant.ofEpochMilli(millis).atZone(ZONE).format(DATE); }

    static String vnd(long amount) { return java.text.NumberFormat.getInstance(java.util.Locale.forLanguageTag("vi-VN")).format(amount) + "đ"; }

    /** Nhãn trạng thái màu: xanh lá (ổn), cam (chờ), đỏ (khóa/hủy/từ chối), xanh dương (quản trị), xám (khác). */
    protected TextView pill(String label, String key) {
        int fg, bg;
        switch (key == null ? "" : key) {
            case "ACTIVE": case "CONFIRMED": case "VERIFIED": case "RUNNING": case "FINISHED": case "VISIBLE": fg = Ui.GREEN; bg = Ui.GREEN_BG; break;
            case "PENDING": case "OPEN": case "IN_REVIEW": case "NEEDS_REVISION": fg = Ui.ORANGE; bg = Ui.ORANGE_BG; break;
            case "BANNED": case "CANCELLED": case "REJECTED": case "HIDDEN": fg = Ui.RED; bg = 0xFFFFE9EC; break;
            case "ADMIN": fg = Ui.BLUE; bg = Ui.PALE; break;
            default: fg = Ui.MUTED; bg = 0xFFF0F3F8;
        }
        return ui.pill(label, 12, fg, bg);
    }

    /** Một dòng danh sách: ảnh/chữ cái đầu, tiêu đề, dòng phụ, các nhãn; chạm để mở chi tiết. */
    protected View row(String avatarId, String title, String subtitle, Runnable open, TextView... pills) {
        LinearLayout card = ui.card(); ui.pad(card, 12, 11);
        LinearLayout top = ui.row(); top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(ui.avatar(avatarId, title, 42)); ui.gap(top, 12);
        LinearLayout info = ui.column();
        TextView name = ui.text(title.isEmpty() ? "(Chưa có tên)" : title, 16, Ui.INK, true); name.setMaxLines(1);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END); ui.add(info, name);
        if (!subtitle.isEmpty()) {
            ui.space(info, 3);
            TextView sub = ui.text(subtitle, 13, Ui.MUTED, false); sub.setMaxLines(2);
            sub.setEllipsize(android.text.TextUtils.TruncateAt.END); ui.add(info, sub);
        }
        if (pills.length > 0) {
            ui.space(info, 6);
            LinearLayout tags = ui.row();
            for (int i = 0; i < pills.length; i++) { if (i > 0) ui.gap(tags, 6); tags.addView(pills[i], ui.lp(-2, -2)); }
            ui.add(info, tags);
        }
        ui.weight(top, info); ui.gap(top, 6); top.addView(ui.text("›", 22, Ui.MUTED, false));
        ui.add(card, top);
        card.setContentDescription(title + ". " + subtitle);
        if (open != null) ui.clickable(card, open);
        return card;
    }

    /** So khớp không dấu, không phân biệt hoa thường ("nguyen" khớp "Nguyễn"). */
    static boolean matches(String query, String... fields) {
        String q = fold(query);
        if (q.isEmpty()) return true;
        for (String f : fields) if (f != null && fold(f).contains(q)) return true;
        return false;
    }
    static String fold(String text) {
        String plain = java.text.Normalizer.normalize(text == null ? "" : text, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replace('đ', 'd').replace('Đ', 'D');
        return plain.toLowerCase(java.util.Locale.ROOT).trim();
    }

    /** Hộp chi tiết cuộn được, có nút Đóng. */
    protected AlertDialog detail(String title, LinearLayout content) {
        android.widget.ScrollView scroll = new android.widget.ScrollView(requireContext());
        content.setPadding(ui.dp(20), ui.dp(4), ui.dp(20), ui.dp(8)); scroll.addView(content);
        AlertDialog dialog = ui.dialog().setTitle(title).setView(scroll).setPositiveButton("Đóng", null).create();
        dialog.show();
        return dialog;
    }
    /** Chip lọc trong hàng cuộn ngang. */
    protected void chips(LinearLayout body, String[] labels, int selected, java.util.function.IntConsumer select) {
        android.widget.HorizontalScrollView scroller = new android.widget.HorizontalScrollView(requireContext());
        scroller.setHorizontalScrollBarEnabled(false);
        LinearLayout row = ui.row(); scroller.addView(row);
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            TextView chip = ui.choice(labels[i], i == selected, () -> select.accept(index));
            chip.setTextSize(13); chip.setPadding(ui.dp(12), 0, ui.dp(12), 0);
            if (i > 0) ui.gap(row, 6);
            row.addView(chip, ui.lp(-2, -2));
        }
        body.addView(scroller, ui.lp(-1, -2)); ui.space(body, 10);
    }
}
