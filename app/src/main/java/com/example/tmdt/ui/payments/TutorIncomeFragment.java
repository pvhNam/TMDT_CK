package com.example.tmdt.ui.payments;

import android.view.View;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import com.example.tmdt.PaymentState;
import com.example.tmdt.R;
import com.example.tmdt.TutorRevenue;
import com.example.tmdt.TutorWallet;
import com.example.tmdt.ui.admin.AdminMonthPicker;
import com.example.tmdt.ui.common.Ui;
import com.example.tmdt.ui.common.StableViews;
import com.google.android.material.tabs.TabLayout;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

public final class TutorIncomeFragment extends PaymentScreen {
    private LinearLayout body;
    @Override protected View build(){
        LinearLayout root=root("Thu nhập gia sư");body=ui.page(root);render();return root;
    }
    @Override protected boolean updateContent(){if(body==null)return false;render();return true;}
    @Override public void onDestroyView(){super.onDestroyView();body=null;}
    private void render() {
        StableViews rows = StableViews.at(body);
        rows.begin();
        PaymentState state = state();

        if (!state.tutor || state.owner.isEmpty()) {
            rows.add("income-access", true,
                    () -> ui.text("Tài khoản hiện chưa có quyền xem thu nhập gia sư.", 15, Ui.MUTED, false),
                    0);
            rows.commit();
            return;
        }

        if (!state.error.isEmpty() || !state.notice.isEmpty() || state.busy) {
            rows.add("feedback", Arrays.asList(state.error, state.notice, state.busy), () -> {
                LinearLayout box = ui.column();
                feedback(box);
                return box;
            }, 0);
        }
        if (state.sandbox()) {
            rows.add("sandbox", true, () -> ui.text("Dữ liệu VNPay thử nghiệm", 13, Ui.ORANGE, true),
                    ui.dp(12));
        }
        rows.add("income-tabs", state.tutorIncomeTab, () -> {
            LinearLayout tabs = ui.column();
            ui.tabs(tabs, new String[]{"Doanh thu", "Thu nhập và chi trả"}, state.tutorIncomeTab, tab -> {
                state.tutorIncomeTab = tab;
                scrollToTop();
                refresh();
            });
            return tabs;
        }, ui.dp(16));

        if (state.tutorIncomeTab == 0) renderRevenue(rows, state);
        else renderPayouts(rows);
        rows.commit();
    }

    private void renderRevenue(StableViews rows, PaymentState state) {
        String period = state.tutorRevenuePeriod;
        YearMonth month = period.isEmpty() ? null : YearMonth.parse(period);
        rows.add("revenue-period", Arrays.asList(period, state.busy), () -> {
            LinearLayout filters = ui.column();
            if (month == null) {
                action(filters, "Xem theo tháng", Ui.OUTLINE,
                        () -> selectRevenuePeriod(TutorRevenue.currentMonth().toString()));
            } else {
                AdminMonthPicker.add(ui, filters, period, this::selectRevenuePeriod);
                action(filters, "Xem tất cả các tháng", Ui.OUTLINE, () -> selectRevenuePeriod(""));
            }
            return filters;
        }, 0);

        if (state.tutorPaymentsStatus == PaymentState.TutorPaymentsStatus.LOADING) {
            revenueNotice(rows, "Đang tải giao dịch đã thanh toán…");
            return;
        }
        if (state.tutorPaymentsStatus == PaymentState.TutorPaymentsStatus.UNAVAILABLE) {
            revenueNotice(rows, "Chưa tải được doanh thu từ giao dịch.");
            return;
        }

        boolean cached = state.tutorPaymentsStatus == PaymentState.TutorPaymentsStatus.CACHED;
        if (cached) {
            rows.add("revenue-cache", true, () -> ui.text("Đang hiển thị dữ liệu đã lưu.", 13, Ui.MUTED, false),
                    ui.dp(10));
        }
        TutorRevenue revenue = new TutorRevenue(state.payments.values(), state.owner, month);
        if (cached && revenue.transactions.isEmpty()) {
            revenueNotice(rows, "Chưa có giao dịch trong dữ liệu đã lưu của kỳ này.");
            return;
        }

        rows.add("revenue-total", Arrays.asList(revenue.collected, revenue.refunded, revenue.transactions.size()),
                () -> revenueTotal(revenue), ui.dp(16));
        rows.add("revenue-heading", true, () -> ui.text("Giao dịch đã thanh toán", 18, Ui.INK, true), ui.dp(10));
        if (revenue.transactions.isEmpty()) {
            revenueNotice(rows, "Chưa có giao dịch đã thanh toán trong kỳ này.");
            return;
        }
        for (TutorRevenue.Transaction transaction : revenue.transactions) {
            rows.add("revenue-payment/" + transaction.payment.id, transaction.payment,
                    () -> transactionCard(transaction), ui.dp(12));
        }
    }

    private void revenueNotice(StableViews rows, String message) {
        rows.add("revenue-notice", message, () -> ui.text(message, 15, Ui.MUTED, false), 0);
    }

    private void selectRevenuePeriod(String period) {
        state().tutorRevenuePeriod = period;
        scrollToTop();
        refresh();
    }

    private LinearLayout revenueTotal(TutorRevenue revenue) {
        LinearLayout card = ui.card();
        String label = revenue.refunded > 0 ? "Học phí sau hoàn tiền" : "Học phí đã thu";
        ui.add(card, ui.text(label, 17, Ui.INK, true));
        ui.space(card, 8);
        ui.add(card, ui.text(PaymentState.money(revenue.net()), 25, Ui.BLUE, true));
        ui.space(card, 6);
        ui.add(card, ui.text(revenue.transactions.size() + " giao dịch đã thanh toán", 14, Ui.MUTED, false));
        if (revenue.refunded > 0) {
            ui.space(card, 10);
            fact(card, "Học phí đã thu", PaymentState.money(revenue.collected));
            fact(card, "Đã hoàn tiền", PaymentState.money(revenue.refunded));
        }
        return card;
    }

    private LinearLayout transactionCard(TutorRevenue.Transaction transaction) {
        PaymentState.Row payment = transaction.payment;
        String title = payment.text("title");
        if (title.isEmpty()) title = "Thanh toán học phí";
        LinearLayout card = ui.card();
        LinearLayout top = ui.row();
        ui.weight(top, ui.text(title, 17, Ui.INK, true));
        ui.gap(top, 8);
        top.addView(ui.text("›", 25, Ui.BLUE, false));
        ui.add(card, top);
        ui.space(card, 6);
        String paidDate = transaction.paidAt > 0 ? date(transaction.paidAt) : "Chưa có ngày thanh toán";
        ui.add(card, ui.text(paymentType(payment.text("type")) + " · " + paidDate, 13, Ui.MUTED, false));
        ui.space(card, 8);
        ui.add(card, ui.text(PaymentState.money(transaction.net()), 20, Ui.BLUE, true));
        if (transaction.refunded > 0) {
            ui.space(card, 6);
            fact(card, "Đã hoàn tiền", PaymentState.money(transaction.refunded));
        }
        ui.clickable(card, () -> state().openExisting(payment.id, "tutor_income"));
        card.setContentDescription("Xem giao dịch " + title);
        return card;
    }

    private static String paymentType(String type) {
        switch (type) {
            case "LESSON": return "Buổi học riêng";
            case "CONTRACT": return "Hợp đồng";
            case "GROUP": return "Lớp nhóm";
            default: return "Học phí";
        }
    }

    private void renderPayouts(StableViews rows) {
        rows.add("period",state().incomePeriod,this::monthSelector,ui.dp(14));
        long total=0,paid=0;int count=0;
        for(PaymentState.Row earning:state().earnings.values())if(earning.text("tutor_id").equals(state().owner)&&earning.text("period").equals(state().incomePeriod)){
            total+=earning.number("amount");count++;PaymentState.Row payout=state().payouts.get(earning.text("payout_id"));if(payout!=null&&state().owner.equals(payout.text("tutor_id"))&&TutorWallet.salaryPaid(payout.text("status")))paid+=earning.number("amount");
        }
        final long earned=total,received=paid;final int sessions=count;
        rows.add("totals",Arrays.asList(state().incomePeriod,total,paid,count),()->{
            LinearLayout card=ui.card(),heading=ui.row();ui.cardSurface(card,Ui.PALE,Ui.BORDER);
            ui.weight(heading,ui.text("Tổng thu nhập",14,Ui.MUTED,false));
            heading.addView(ui.pill(sessions+" buổi",12,Ui.BLUE,Ui.WHITE));ui.add(card,heading);ui.space(card,12);
            ui.add(card,ui.text(PaymentState.money(earned),26,Ui.INK,true));ui.space(card,18);ui.line(card);ui.space(card,16);
            LinearLayout stats=ui.row();
            ui.weight(stats,metric("Đã nhận",received,Ui.GREEN));ui.gap(stats,16);
            ui.weight(stats,metric("Chờ chi trả",earned-received,Ui.INK));ui.add(card,stats);return card;
        },ui.dp(12));
        rows.add("bank",true,()->{
            LinearLayout card=ui.row();ui.pad(card,14,8);ui.surface(card,Ui.WHITE,16,Ui.BORDER);
            card.addView(ui.icon(R.drawable.ic_wallet,22,Ui.BLUE));ui.gap(card,12);
            TextView label=ui.text("Ví nhận lương",14,Ui.INK,true);label.setMinHeight(ui.dp(40));label.setGravity(Gravity.CENTER_VERTICAL);
            ui.weight(card,label);card.addView(ui.icon(R.drawable.ic_right,18,Ui.MUTED));
            ui.clickable(card,this::openWallet);return card;
        },ui.dp(20));
        List<PaymentState.Row> payouts=new ArrayList<>();
        for(PaymentState.Row payout:newest(state().payouts.values()))
            if(payout.text("tutor_id").equals(state().owner)&&payout.text("period").equals(state().incomePeriod))payouts.add(payout);
        if(!payouts.isEmpty())rows.add("payout-heading",payouts.size(),()->{
            LinearLayout heading=ui.row();ui.weight(heading,ui.text("Chi trả kỳ này",16,Ui.INK,true));
            heading.addView(ui.pill(String.valueOf(payouts.size()),12,Ui.BLUE,Ui.PALE));return heading;
        },ui.dp(12));
        if (payouts.isEmpty()) {
            rows.add("payout-empty", true,
                    () -> ui.text("Chưa có khoản chi trả trong kỳ này.", 15, Ui.MUTED, false), 0);
        }
        for(PaymentState.Row payout:payouts){
            rows.add("payout/"+payout.id,payout,()->{
            LinearLayout card=ui.card(),top=ui.row();boolean completed=TutorWallet.salaryPaid(payout.text("status"));
            ui.weight(top,ui.text(payout.number("session_count")+" buổi học",13,Ui.MUTED,false));
            top.addView(ui.pill(PaymentState.payoutStatus(payout.text("status")),11,completed?Ui.GREEN:Ui.ORANGE,completed?Ui.GREEN_BG:Ui.ORANGE_BG));
            ui.add(card,top);ui.space(card,12);
            LinearLayout amount=ui.row();ui.weight(amount,ui.text(PaymentState.money(payout.number("amount")),20,Ui.INK,true));
            amount.addView(ui.icon(R.drawable.ic_right,18,Ui.BLUE));ui.add(card,amount);
            card.setContentDescription("Xem khoản chi trả "+payout.text("period")+", "+PaymentState.money(payout.number("amount")));
            ui.clickable(card,()->{state().selectedPayout=payout.id;state().origin="tutor_income";host().show("payout_detail");});return card;
            },ui.dp(12));
        }
    }

    private void openWallet() {
        Fragment parent = getParentFragment();
        if (parent instanceof TutorFinanceFragment && parent.getView() != null) {
            TabLayout tabs = parent.getView().findViewById(R.id.section_tabs);
            TabLayout.Tab wallet = tabs == null ? null : tabs.getTabAt(0);
            if (wallet != null) {
                wallet.select();
                return;
            }
        }
        host().show("tutor_wallet");
    }

    private LinearLayout metric(String label,long amount,int color){
        LinearLayout box=ui.column();ui.add(box,ui.text(label,12,Ui.MUTED,false));ui.space(box,6);
        ui.add(box,ui.text(PaymentState.money(amount),16,color,true));return box;
    }
    private YearMonth selectedMonth(){
        try{return YearMonth.parse(state().incomePeriod);}catch(java.time.format.DateTimeParseException e){return YearMonth.now(ZoneId.of("Asia/Ho_Chi_Minh"));}
    }
    private View monthSelector(){
        LinearLayout bar=ui.row();ui.surface(bar,Ui.WHITE,16,Ui.BORDER);
        bar.addView(ui.iconButton(R.drawable.ic_back,Ui.BLUE,"Tháng trước",()->selectMonth(selectedMonth().minusMonths(1))));
        TextView month=ui.link("Tháng "+selectedMonth().format(DateTimeFormatter.ofPattern("MM/yyyy")),15,this::chooseMonth);
        month.setGravity(Gravity.CENTER);month.setMinHeight(ui.dp(48));ui.weight(bar,month);
        bar.addView(ui.iconButton(R.drawable.ic_right,Ui.BLUE,"Tháng sau",()->selectMonth(selectedMonth().plusMonths(1))));return bar;
    }
    private void selectMonth(YearMonth month){state().incomePeriod=month.toString();state().changed();}
    private void chooseMonth(){
        TreeSet<YearMonth> months=new TreeSet<>(Collections.reverseOrder());months.add(selectedMonth());
        YearMonth current=YearMonth.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        for(int i=0;i<12;i++)months.add(current.minusMonths(i));
        for(PaymentState.Row earning:state().earnings.values())addMonth(months,earning);
        for(PaymentState.Row payout:state().payouts.values())addMonth(months,payout);
        List<YearMonth> choices=new ArrayList<>(months);String[] labels=new String[choices.size()];
        for(int i=0;i<choices.size();i++)labels[i]="Tháng "+choices.get(i).format(DateTimeFormatter.ofPattern("MM/yyyy"));
        ui.dialog().setTitle("Chọn tháng").setSingleChoiceItems(labels,choices.indexOf(selectedMonth()),(dialog,which)->{
            selectMonth(choices.get(which));dialog.dismiss();
        }).setNegativeButton("Đóng",null).show();
    }
    private void addMonth(TreeSet<YearMonth> months,PaymentState.Row row){
        if(!row.text("tutor_id").equals(state().owner))return;
        try{months.add(YearMonth.parse(row.text("period")));}catch(java.time.format.DateTimeParseException ignored){}
    }
}
