package com.example.tmdt.ui.exchange;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.tmdt.AgreementState;
import com.example.tmdt.Exchange;
import com.example.tmdt.MainActivity;
import com.example.tmdt.R;
import com.example.tmdt.ui.common.ScreenFragment;
import com.example.tmdt.ui.common.Ui;

/**
 * Hợp đồng cộng tác giữa gia sư và hệ thống (UC23). Gia sư đã được duyệt đọc mẫu GIA_SU_HE_THONG của quản trị viên
 * rồi ký; phải ký trước khi gửi hợp đồng dạy cho học viên. Mẫu có phiên bản mới thì ký lại.
 */
public final class AgreementFragment extends ScreenFragment {
    private boolean agreed;

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        Exchange exchange = ((MainActivity) requireActivity()).exchange;
        exchange.agreements.changes().observe(getViewLifecycleOwner(), ignored -> {
            String notice = exchange.agreements.takeNotice();
            if (!notice.isEmpty()) classroom.notice(notice);
            refresh();
        });
        exchange.agreements.start();
    }

    @Override protected View build() {
        Exchange exchange = ((MainActivity) requireActivity()).exchange;
        AgreementState state = exchange.agreements;
        LinearLayout root = ui.column(); ui.header(root, "Hợp đồng cộng tác", exchange::back, null);
        LinearLayout body = ui.page(root);

        AgreementState.Template template = state.template(AgreementState.TUTOR_PLATFORM);
        AgreementState.Agreement signed = state.agreement();
        if (!exchange.isTutor()) {
            ui.add(body, ui.note(R.drawable.ic_info, "Chỉ gia sư đã được duyệt hồ sơ mới ký hợp đồng cộng tác với hệ thống.", Ui.PALE, Ui.BLUE, Ui.INK));
            return root;
        }
        if (!state.templateLoaded(AgreementState.TUTOR_PLATFORM) || !state.agreementLoaded()) {
            ui.add(body, ui.note(R.drawable.ic_clock, state.loadError().isEmpty() ? "Đang tải hợp đồng…" : state.loadError(), Ui.PALE, Ui.BLUE, Ui.INK));
            return root;
        }
        if (template == null && signed == null) {
            ui.add(body, ui.note(R.drawable.ic_info, "Quản trị viên chưa soạn mẫu hợp đồng cộng tác. Vui lòng quay lại sau.", Ui.ORANGE_BG, Ui.ORANGE, Ui.INK));
            return root;
        }

        // Trạng thái
        if (signed != null && !state.outdated()) {
            ui.add(body, ui.note(R.drawable.ic_info, "Bạn đã ký ngày " + ExUi.date(signed.signedAt) + " · phiên bản " + signed.version
                    + ". Bạn có thể gửi hợp đồng dạy cho học viên.", Ui.GREEN_BG, Ui.GREEN, Ui.INK));
        } else if (signed != null) {
            ui.add(body, ui.note(R.drawable.ic_clock, "Mẫu hợp đồng đã cập nhật lên phiên bản " + template.version
                    + " (bạn đã ký phiên bản " + signed.version + "). Vui lòng đọc và ký lại.", Ui.ORANGE_BG, Ui.ORANGE, Ui.INK));
        } else {
            ui.add(body, ui.note(R.drawable.ic_info, "Ký hợp đồng cộng tác để bắt đầu gửi hợp đồng dạy cho học viên sau khi chấp nhận lịch học.", Ui.PALE, Ui.BLUE, Ui.INK));
        }
        ui.space(body, 14);

        // Các bên
        boolean showCurrent = signed == null || state.outdated();
        String title = showCurrent ? template.title : signed.title;
        String content = showCurrent ? template.content : signed.content;
        ui.section(body, title, 20); ui.space(body, 10);
        LinearLayout parties = ui.table();
        ui.tableRow(parties, R.drawable.ic_file, "Bên A", "Hệ thống TMĐT kết nối gia sư");
        ui.tableRow(parties, R.drawable.ic_user, "Bên B (gia sư)", signed != null && !showCurrent ? signed.name : exchange.myName());
        ui.tableRow(parties, R.drawable.ic_calendar, "Phiên bản", String.valueOf(showCurrent ? template.version : signed.version));
        ui.add(body, parties); ui.space(body, 12);

        TextView text = ui.text(content, 15, Ui.INK, false);
        text.setPadding(ui.dp(14), ui.dp(12), ui.dp(14), ui.dp(12)); text.setTextIsSelectable(true);
        ui.cardSurface(text, Ui.PALE, Ui.BORDER); ui.add(body, text); ui.space(body, 12);

        if (showCurrent) {
            ui.add(body, ui.check("Tôi đã đọc và đồng ý với hợp đồng cộng tác", agreed, (b, checked) -> { agreed = checked; refresh(); }));
            ui.space(body, 8);
            boolean ready = agreed && !state.busy();
            ui.addAction(body, ui.action(state.busy() ? "Đang ký…" : "Ký hợp đồng cộng tác", 0, ready ? Ui.PRIMARY : Ui.DISABLED,
                    ready ? () -> state.sign(exchange.myName()) : null), 48);
        }
        return root;
    }
}
