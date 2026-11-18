package com.example.tmdt.ui.exchange;

import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.tmdt.LearnerState;
import com.example.tmdt.ui.common.Ui;
import java.util.function.Consumer;

/**
 * Mục "Người học" khi đặt lịch 1-1 hoặc đăng ký lớp nhóm: "Bản thân" hoặc một người học của tài khoản,
 * kèm nút thêm người học. Giá trị chọn là id người học ("" = bản thân).
 */
public final class LearnerPicker {
    private LearnerPicker() {}

    public static void add(Ui ui, LinearLayout body, LearnerState state, String selected, Consumer<String> select, Runnable manage) {
        ui.section(body, "Người học", 18); ui.space(body, 4);
        ui.add(body, ui.text("Học cho bản thân hoặc cho con, người thân của bạn.", 13, Ui.MUTED, false)); ui.space(body, 8);
        HorizontalScrollView scroller = new HorizontalScrollView(ui.context); scroller.setHorizontalScrollBarEnabled(false);
        LinearLayout row = ui.row(); scroller.addView(row);
        chip(ui, row, "Bản thân", selected == null || selected.isEmpty(), () -> select.accept(""), false);
        for (LearnerState.Learner l : state.learners()) chip(ui, row, l.name, l.id.equals(selected), () -> select.accept(l.id), true);
        chip(ui, row, "+ Thêm người học", false, manage, true);
        body.addView(scroller, ui.lp(-1, -2));
    }
    private static void chip(Ui ui, LinearLayout row, String label, boolean on, Runnable click, boolean gap) {
        if (gap) ui.gap(row, 8);
        TextView chip = ui.choice(label, on, click); chip.setTextSize(14); chip.setPadding(ui.dp(14), 0, ui.dp(14), 0);
        row.addView(chip, ui.lp(-2, -2));
    }
}
