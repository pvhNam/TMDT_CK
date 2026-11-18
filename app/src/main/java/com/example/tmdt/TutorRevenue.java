package com.example.tmdt;

import androidx.annotation.Nullable;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Tuition collected for the signed-in tutor, grouped by the date of payment. */
public final class TutorRevenue {

    public final List<Transaction> transactions;
    public final long collected;
    public final long refunded;

    /** A null month includes transactions whose payment date was not recorded. */
    public TutorRevenue(Iterable<PaymentState.Row> payments, String owner, @Nullable YearMonth month) {
        this(payments, owner, month == null ? ReportPeriod.all() : ReportPeriod.month(month));
    }

    public static TutorRevenue forPeriod(Iterable<PaymentState.Row> payments, String owner, ReportPeriod period) {
        return new TutorRevenue(payments, owner, period);
    }

    private TutorRevenue(Iterable<PaymentState.Row> payments, String owner, ReportPeriod period) {
        List<Transaction> matched = new ArrayList<>();
        long collected = 0;
        long refunded = 0;
        if (!owner.isEmpty()) {
            for (PaymentState.Row payment : payments) {
                if (!owner.equals(payment.text("tutor_id")) || !"PAID".equals(payment.text("status"))) continue;

                long paidAt = payment.time("paid_at");
                if (!period.includes(paidAt)) continue;

                Transaction transaction = new Transaction(payment, paidAt);
                matched.add(transaction);
                collected += transaction.collected;
                refunded += transaction.refunded;
            }
        }
        matched.sort(Comparator.comparingLong((Transaction row) -> row.paidAt).reversed()
                .thenComparing(row -> row.payment.id));
        this.transactions = Collections.unmodifiableList(matched);
        this.collected = collected;
        this.refunded = refunded;
    }

    public static YearMonth currentMonth() {
        return ReportPeriod.currentMonth();
    }

    public long net() {
        return collected - refunded;
    }

    public static final class Transaction {
        public final PaymentState.Row payment;
        public final long paidAt;
        public final long collected;
        public final long refunded;

        private Transaction(PaymentState.Row payment, long paidAt) {
            this.payment = payment;
            this.paidAt = paidAt;
            // Older paid records may only have the invoice amount. A recorded receipt of zero is respected.
            long receipt = payment.data.containsKey("received_amount")
                    ? payment.number("received_amount") : payment.number("amount");
            collected = Math.max(0, receipt);
            refunded = Math.min(collected, Math.max(0, payment.number("refund_total")));
        }

        public long net() {
            return collected - refunded;
        }
    }
}