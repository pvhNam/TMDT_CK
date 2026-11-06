package com.example.tmdt;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/** Inclusive calendar dates in the app's time zone. */
public final class ReportPeriod {
    public enum Mode { MONTH, MONTHS, DATES, ALL }
    public static final ZoneId ZONE=ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter MONTH=DateTimeFormatter.ofPattern("MM/yyyy");
    private static final DateTimeFormatter DAY=DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public final Mode mode;
    public final LocalDate from,to;

    private ReportPeriod(Mode mode,LocalDate from,LocalDate to){
        if(mode!=Mode.ALL&&(from==null||to==null||to.isBefore(from)))
            throw new IllegalArgumentException("Ngày kết thúc phải từ ngày bắt đầu trở đi.");
        this.mode=mode;this.from=from;this.to=to;
    }
    public static ReportPeriod month(YearMonth month){return new ReportPeriod(Mode.MONTH,month.atDay(1),month.atEndOfMonth());}
    public static ReportPeriod months(YearMonth from,YearMonth to){return new ReportPeriod(Mode.MONTHS,from.atDay(1),to.atEndOfMonth());}
    public static ReportPeriod dates(LocalDate from,LocalDate to){return new ReportPeriod(Mode.DATES,from,to);}
    public static ReportPeriod all(){return new ReportPeriod(Mode.ALL,null,null);}
    public static YearMonth currentMonth(){return YearMonth.now(ZONE);}
    public boolean includes(long timestamp){
        if(mode==Mode.ALL)return true;
        if(timestamp<=0)return false;
        LocalDate date=Instant.ofEpochMilli(timestamp).atZone(ZONE).toLocalDate();
        return !date.isBefore(from)&&!date.isAfter(to);
    }
    public String label(){
        if(mode==Mode.ALL)return "Toàn thời gian";
        if(mode==Mode.MONTH)return "Tháng "+from.format(MONTH);
        if(mode==Mode.MONTHS)return from.format(MONTH)+" – "+to.format(MONTH);
        return from.format(DAY)+" – "+to.format(DAY);
    }
    @Override public boolean equals(Object other){
        if(!(other instanceof ReportPeriod))return false;
        ReportPeriod p=(ReportPeriod)other;
        return mode==p.mode&&Objects.equals(from,p.from)&&Objects.equals(to,p.to);
    }
    @Override public int hashCode(){return Objects.hash(mode,from,to);}
}
