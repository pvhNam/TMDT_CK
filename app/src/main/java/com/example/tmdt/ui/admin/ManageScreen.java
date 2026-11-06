package com.example.tmdt.ui.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
}
