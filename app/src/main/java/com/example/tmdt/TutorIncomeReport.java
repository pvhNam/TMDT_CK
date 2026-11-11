package com.example.tmdt;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A tutor's monthly earnings and salary history, derived without changing the ledger. */
public final class TutorIncomeReport {
    public final String tutorId,period,name;
    public final long total,paid,waiting,unprepared;
    public final int sessions;
    public final List<PaymentState.Row> earnings=new ArrayList<>(),payouts=new ArrayList<>();

    public TutorIncomeReport(String tutorId,String period,Map<String,PaymentState.Row> allEarnings,Map<String,PaymentState.Row> allPayouts){
        this.tutorId=tutorId;this.period=period;
        Map<String,PaymentState.Row> earnedRows=new LinkedHashMap<>(),salaryRows=new LinkedHashMap<>();String tutorName="";
        for(PaymentState.Row row:allEarnings.values())if(tutorId.equals(row.text("tutor_id"))&&period.equals(row.text("period"))){
            earnedRows.put(row.id,row);earnings.add(row);if(!row.text("tutor_name").isEmpty())tutorName=row.text("tutor_name");
        }
        for(PaymentState.Row row:allPayouts.values())if(tutorId.equals(row.text("tutor_id"))&&period.equals(row.text("period"))){
            salaryRows.put(row.id,row);payouts.add(row);if(tutorName.isEmpty())tutorName=row.text("tutor_name");
        }
        MonthlyPayroll payroll=new MonthlyPayroll(period,earnedRows,salaryRows);
        name=tutorName;total=payroll.total;paid=payroll.paidAmount;waiting=Math.max(0,total-paid);unprepared=payroll.unpreparedAmount;sessions=payroll.sessions;
        earnings.sort(Comparator.comparingLong((PaymentState.Row row)->row.time("created_at")).reversed());
        payouts.sort(Comparator.comparingLong((PaymentState.Row row)->row.time("created_at")).reversed());
    }
    public static List<TutorIncomeReport> month(String period,Map<String,PaymentState.Row> earnings,Map<String,PaymentState.Row> payouts){
        Map<String,Map<String,PaymentState.Row>> earnedByTutor=new LinkedHashMap<>(),paidByTutor=new LinkedHashMap<>();
        for(PaymentState.Row row:earnings.values())if(period.equals(row.text("period")))earnedByTutor.computeIfAbsent(row.text("tutor_id"),key->new LinkedHashMap<>()).put(row.id,row);
        for(PaymentState.Row row:payouts.values())if(period.equals(row.text("period")))paidByTutor.computeIfAbsent(row.text("tutor_id"),key->new LinkedHashMap<>()).put(row.id,row);
        java.util.Set<String> ids=new java.util.LinkedHashSet<>(earnedByTutor.keySet());ids.addAll(paidByTutor.keySet());
        List<TutorIncomeReport> result=new ArrayList<>();
        for(String id:ids)result.add(new TutorIncomeReport(id,period,earnedByTutor.getOrDefault(id,java.util.Collections.emptyMap()),paidByTutor.getOrDefault(id,java.util.Collections.emptyMap())));
        result.sort(Comparator.comparingLong((TutorIncomeReport row)->row.total).reversed().thenComparing(row->row.name,String.CASE_INSENSITIVE_ORDER));return result;
    }
}
