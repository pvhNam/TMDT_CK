package com.example.tmdt.ui.payments;

import android.view.View;
import android.widget.LinearLayout;
import com.example.tmdt.PaymentState;
import com.example.tmdt.R;
import com.example.tmdt.ui.common.SectionHubFragment;
import com.example.tmdt.ui.common.StableViews;
import com.example.tmdt.ui.common.Ui;
import java.util.Arrays;

public final class PaymentHistoryFragment extends PaymentScreen {
    private LinearLayout body;

    @Override
    protected View build() {
        LinearLayout root = root(state().admin ? "Quản lý thanh toán" : "Thanh toán của tôi");
        body = ui.page(root);
        render();
        return root;
    }

    @Override
    protected boolean updateContent() {
        if (body == null) return false;
        render();
        return true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        body = null;
    }

    private void render() {
        StableViews rows = StableViews.at(body);
        rows.begin();

        if (!state().error.isEmpty() || !state().notice.isEmpty() || state().busy) {
            rows.add("feedback", Arrays.asList(state().error, state().notice, state().busy), () -> {
                LinearLayout box = ui.column();
                feedback(box);
                return box;
            }, 0);
        }

        if (state().admin) {
            rows.add("tabs", state().adminPaymentTab, () -> {
                LinearLayout box = ui.column();
                ui.tabs(box, new String[]{"Học phí", "Hoàn tiền"}, state().adminPaymentTab, i -> {
                    state().adminPaymentTab = i;
                    scrollToTop();
                    refresh();
                });
                return box;
            }, ui.dp(16));
        }

        if (!state().admin || state().adminPaymentTab == 0) {
            if (state().tutor && !SectionHubFragment.embedded(this)) {
                rows.add("income", state().busy, () -> {
                    LinearLayout box = ui.column();
                    action(box, "Doanh thu và chi trả", Ui.OUTLINE,
                            () -> host().show("tutor_income"));
                    return box;
                }, 0);
            }

            rows.add("heading", state().payments.size(), () -> {
                LinearLayout heading = ui.row();
                ui.weight(heading, ui.text("Lịch sử giao dịch", 16, Ui.INK, true));
                heading.addView(ui.pill(String.valueOf(state().payments.size()), 12, Ui.BLUE, Ui.PALE));
                return heading;
            }, ui.dp(14));

            for (PaymentState.Row payment : newest(state().payments.values())) {
                rows.add("payment/" + payment.id, payment, () -> {
                    LinearLayout card = ui.card();
                    LinearLayout top = ui.row();
                    top.addView(statusPill(payment.text("status")));
                    ui.add(card, top);
                    ui.space(card, 12);

                    String title = payment.text("title");
                    ui.add(card, ui.text(title.isEmpty() ? "Thanh toán học phí" : title, 16, Ui.INK, true));
                    ui.space(card, 6);

                    String tutor = payment.text("tutor_name");
                    String subtitle = (tutor.isEmpty() ? "" : tutor + " · ")
                            + payment.number("session_count") + " buổi";
                    ui.add(card, ui.text(subtitle, 13, Ui.MUTED, false));
                    ui.space(card, 14);
                    ui.line(card);
                    ui.space(card, 12);

                    LinearLayout bottom = ui.row();
                    LinearLayout detail = ui.column();
                    ui.add(detail, ui.text(PaymentState.money(payment.number("amount")), 20, Ui.BLUE, true));

                    long createdAt = payment.time("created_at");
                    if (createdAt > 0) {
                        ui.space(detail, 4);
                        ui.add(detail, ui.text(date(createdAt), 12, Ui.MUTED, false));
                    }

                    ui.weight(bottom, detail);
                    bottom.addView(ui.icon(R.drawable.ic_right, 18, Ui.MUTED));
                    ui.add(card, bottom);
                    card.setContentDescription("Xem giao dịch " + (title.isEmpty() ? payment.id : title));
                    ui.clickable(card, () -> state().openExisting(payment.id, "payments"));
                    return card;
                }, ui.dp(12));
            }
        }

        if (state().admin && state().adminPaymentTab == 1) {
            rows.add("refund-heading", true,
                    () -> ui.text("Yêu cầu hoàn tiền", 19, Ui.INK, true), ui.dp(10));

            for (PaymentState.Row refund : newest(state().refunds.values())) {
                rows.add("refund/" + refund.id, Arrays.asList(refund, state().busy), () -> {
                    LinearLayout box = ui.column();
                    String status = refund.text("status");
                    summary(box, refund.text("title"),
                            refund.text("reason") + "\n" + PaymentState.status(status),
                            PaymentState.money(refund.number("amount")));

                    if ("PENDING".equals(status)) {
                        action(box, "Xác nhận đã chuyển hoàn tiền", Ui.OUTLINE, () -> {
                            state().selectedRefund = refund.id;
                            prompt("Xác nhận đã hoàn tiền", "Mã giao dịch ngân hàng", state()::finishRefund);
                        });
                        action(box, "Từ chối yêu cầu hoàn tiền", Ui.OUTLINE, () -> {
                            state().selectedRefund = refund.id;
                            prompt("Từ chối hoàn tiền", "Lý do từ chối", state()::rejectRefund);
                        });
                    }

                    return box;
                }, 0);
            }
        }

        rows.commit();
    }
}