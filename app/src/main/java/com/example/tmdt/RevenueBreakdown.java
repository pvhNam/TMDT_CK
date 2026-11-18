package com.example.tmdt;

/** Mutually exclusive receipts by teaching format; pending invoice amounts are not revenue. */
public final class RevenueBreakdown {
    public static final String[] LABELS={"Buổi học riêng","Hợp đồng","Lớp nhóm","Khoản khác"};
    private final long[] amounts=new long[4];
    public final long received,refunded;
    public final int transactions;
    public RevenueBreakdown(Iterable<PaymentState.Row> payments){
        this(payments,ReportPeriod.all());
    }
    public RevenueBreakdown(Iterable<PaymentState.Row> payments,ReportPeriod period){
        long total=0,refunds=0;int count=0;
        for(PaymentState.Row row:payments){
            long receipt=Math.max(0,row.number("received_amount"));
            long date=row.time("paid_at")>0?row.time("paid_at"):row.time("created_at");
            if(receipt==0||!period.includes(date))continue;
            int index="LESSON".equals(row.text("type"))?0:"CONTRACT".equals(row.text("type"))?1:"GROUP".equals(row.text("type"))?2:3;
            amounts[index]+=receipt;total+=receipt;count++;
            refunds+=Math.min(receipt,Math.max(0,row.number("refund_total")));
        }
        received=total;refunded=refunds;transactions=count;
    }
    public long amount(int index){return amounts[index];}
    public double percent(int index){return received==0?0:100d*amounts[index]/received;}
    public Object signature(){return java.util.Arrays.asList(received,refunded,transactions,amounts[0],amounts[1],amounts[2],amounts[3]);}
}
