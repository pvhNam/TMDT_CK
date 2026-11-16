package com.example.tmdt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** UC27, UC29: trạng thái và nhãn của báo cáo vi phạm. */
public class ReportTest {
    @Test public void reportFinishedOnlyWhenDecided() {
        Report r = new Report();
        for (String open : new String[]{Report.RECEIVED, Report.REVIEWING, Report.NEED_INFO}) { r.status = open; assertFalse(r.finished()); }
        r.status = Report.RESOLVED; assertTrue(r.finished());
        r.status = Report.DISMISSED; assertTrue(r.finished());
    }
    @Test public void labels() {
        Report r = new Report(); r.status = Report.NEED_INFO;
        assertEquals("Cần bổ sung", r.statusLabel());
        assertEquals("Khóa tài khoản người bị báo cáo", Report.outcomeLabel(Report.OUTCOME_BAN));
        assertEquals("Gia sư", Report.roleLabel(Review.TUTOR));
        assertEquals("Học viên", Report.roleLabel(Review.STUDENT));
        assertEquals("LESSON", Report.SUBJECT_LESSON);
    }
}
