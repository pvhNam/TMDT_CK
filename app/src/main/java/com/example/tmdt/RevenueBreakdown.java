package com.example.tmdt;

/** Mutually exclusive receipts by teaching format; pending invoice amounts are not revenue. */
public final class RevenueBreakdown {
    public static final String[] LABELS={"Buổi học riêng","Hợp đồng","Lớp nhóm","Khoản khác"};
    private final long[] amounts=new long[4];
    public final long received,refunded;
    public RevenueBreakdown(Iterable<PaymentState.Row> payments){
        long total=0,refunds=0;
        for(PaymentState.Row row:payments){
            long receipt=Math.max(0,row.number("received_amount"));
            int index="LESSON".equals(row.text("type"))?0:"CONTRACT".equals(row.text("type"))?1:"GROUP".equals(row.text("type"))?2:3;
            amounts[index]+=receipt;total+=receipt;
            refunds+=Math.min(receipt,Math.max(0,row.number("refund_total")));
        }
        received=total;refunded=refunds;
    }
    public long amount(int index){return amounts[index];}
    public double percent(int index){return received==0?0:100d*amounts[index]/received;}
}
