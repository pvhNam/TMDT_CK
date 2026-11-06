package com.example.tmdt;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

/** A monthly view of the existing earning/payout ledger; never writes or pays funds. */
public final class MonthlyPayroll {
    public static final class Unprepared {
        public final String tutorId;
        public String tutorName;
        public long amount;
        public int sessions;
        Unprepared(String tutorId,String name){this.tutorId=tutorId;this.tutorName=name;}
    }
    public final String period;
    public final YearMonth month;
    public final List<PaymentState.Row> ready=new ArrayList<>(),paid=new ArrayList<>(),cancelled=new ArrayList<>();
    public final Map<String,Unprepared> unprepared=new LinkedHashMap<>();
    public final long total,waitingAmount,paidAmount,unpreparedAmount;
    public final int sessions,tutors;
    public final long dueAt;

    public MonthlyPayroll(String period,Map<String,PaymentState.Row> earnings,Map<String,PaymentState.Row> payouts){
        month=YearMonth.parse(period);this.period=month.toString();
        dueAt=month.plusMonths(1).atDay(5).atStartOfDay(ReportPeriod.ZONE).toInstant().toEpochMilli();
        Set<String> teacherIds=new HashSet<>();
        Set<String> covered=new HashSet<>();
        long waiting=0,received=0,unassigned=0,earned=0;int count=0,earningCount=0;
        for(PaymentState.Row payout:payouts.values())if(this.period.equals(payout.text("period"))){
            String status=payout.text("status");
            if("CANCELLED".equals(status)){cancelled.add(payout);continue;}
            if("READY".equals(status)){ready.add(payout);waiting+=payout.number("amount");}
            else if(TutorWallet.salaryPaid(status)){paid.add(payout);received+=payout.number("amount");}
            else continue;
            count+=payout.number("session_count");teacherIds.add(payout.text("tutor_id"));
            Object ids=payout.data.get("earning_ids");
            if(ids instanceof Iterable)for(Object id:(Iterable<?>)ids)if(id instanceof String)covered.add((String)id);
        }
        for(PaymentState.Row earning:earnings.values())if(this.period.equals(earning.text("period"))){
            earned+=earning.number("amount");earningCount++;teacherIds.add(earning.text("tutor_id"));
            // The payout may arrive before the earning update, so check both sides of the link.
            if(covered.contains(earning.id))continue;
            PaymentState.Row payout=payouts.get(earning.text("payout_id"));
            if(!earning.text("payout_id").isEmpty()&&(payout==null||!"CANCELLED".equals(payout.text("status"))))continue;
            Unprepared row=unprepared.computeIfAbsent(earning.text("tutor_id"),id->new Unprepared(id,earning.text("tutor_name")));
            row.amount+=earning.number("amount");row.sessions++;
            unassigned+=earning.number("amount");count++;teacherIds.add(row.tutorId);
        }
        Comparator<PaymentState.Row> byName=Comparator.comparing(p->p.text("tutor_name"),String.CASE_INSENSITIVE_ORDER);
        ready.sort(byName);paid.sort(byName);cancelled.sort((a,b)->Long.compare(b.time("updated_at"),a.time("updated_at")));
        waitingAmount=waiting;paidAmount=received;unpreparedAmount=unassigned;
        // Each collection updates separately; keep the total from the complete side of the ledger.
        total=Math.max(earned,waiting+received+unassigned);sessions=Math.max(earningCount,count);tutors=teacherIds.size();
    }
    public boolean ended(){return month.isBefore(ReportPeriod.currentMonth());}
}
