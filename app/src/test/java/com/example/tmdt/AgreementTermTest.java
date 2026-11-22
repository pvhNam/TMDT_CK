package com.example.tmdt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.ZoneId;
import org.junit.Test;

public class AgreementTermTest {
    private static final long DAY = AgreementTerm.DAY;
    private static final long T0 = 1_780_000_000_000L;
    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private static AgreementTerm.Phase phase(long now, long start, long end) {
        return AgreementTerm.phase(now, true, AgreementTerm.SIGNED, start, end, 0);
    }

    @Test public void oneAndTwoYearTerms() {
        assertEquals(T0 + 365 * DAY, AgreementTerm.end(T0, 1));
        assertEquals(T0 + 730 * DAY, AgreementTerm.end(T0, 2));
    }

    @Test public void activeUntilLastThirtyDaysThenExpiringThenExpired() {
        long end = AgreementTerm.end(T0, 1);
        assertEquals(AgreementTerm.Phase.ACTIVE, phase(T0, T0, end));
        assertEquals(AgreementTerm.Phase.ACTIVE, phase(end - 31 * DAY, T0, end));
        assertEquals(AgreementTerm.Phase.EXPIRING, phase(end - 30 * DAY, T0, end));
        assertEquals(AgreementTerm.Phase.EXPIRING, phase(end - 1, T0, end));
        assertEquals(AgreementTerm.Phase.EXPIRED, phase(end, T0, end));
    }

    @Test public void cannotResignWhileActiveSoTermStaysFixed() {
        long end = AgreementTerm.end(T0, 2);
        assertFalse(AgreementTerm.canSign(phase(T0 + 100 * DAY, T0, end)));
        assertTrue(AgreementTerm.active(phase(T0 + 100 * DAY, T0, end)));
    }

    @Test public void earlyRenewalStartsWhenOldTermEnds() {
        long end = AgreementTerm.end(T0, 1), now = end - 10 * DAY;
        AgreementTerm.Phase p = phase(now, T0, end);
        assertTrue(AgreementTerm.canSign(p));
        long next = AgreementTerm.newStart(p, now, end);
        assertEquals(end, next);
        AgreementTerm.Phase after = phase(now, next, AgreementTerm.end(next, 2));
        assertEquals(AgreementTerm.Phase.RENEWED, after);
        assertTrue(AgreementTerm.active(after));
        assertFalse(AgreementTerm.canSign(after));
    }

    @Test public void expiredSignsFromNowAndCannotSendContracts() {
        long end = AgreementTerm.end(T0, 1), now = end + 5 * DAY;
        AgreementTerm.Phase p = phase(now, T0, end);
        assertFalse(AgreementTerm.active(p));
        assertEquals(now, AgreementTerm.newStart(p, now, end));
    }

    @Test public void terminatedWaitsThirtyDays() {
        long cut = T0 + 50 * DAY;
        assertEquals(AgreementTerm.Phase.TERMINATED_WAIT,
                AgreementTerm.phase(cut + 29 * DAY, true, AgreementTerm.TERMINATED, T0, AgreementTerm.end(T0, 1), cut));
        AgreementTerm.Phase later = AgreementTerm.phase(cut + 30 * DAY, true, AgreementTerm.TERMINATED, T0, AgreementTerm.end(T0, 1), cut);
        assertEquals(AgreementTerm.Phase.TERMINATED_CAN_SIGN, later);
        assertFalse(AgreementTerm.active(later));
    }

    @Test public void legacyAgreementMustBeResigned() {
        AgreementTerm.Phase p = AgreementTerm.phase(T0, true, AgreementTerm.SIGNED, 0, 0, 0);
        assertEquals(AgreementTerm.Phase.LEGACY, p);
        assertFalse(AgreementTerm.active(p));
        assertTrue(AgreementTerm.canSign(p));
        assertEquals(AgreementTerm.Phase.NONE, AgreementTerm.phase(T0, false, "", 0, 0, 0));
    }

    @Test public void clausesNameTermAndLastDay() {
        long start = java.time.LocalDate.of(2026, 11, 19).atStartOfDay(VN).toInstant().toEpochMilli();
        String text = AgreementTerm.clauses(1, start, AgreementTerm.end(start, 1), VN);
        assertTrue(text.contains("1 năm, có hiệu lực từ 19/11/2026 đến hết 18/11/2027"));
        assertTrue(text.contains("Vi phạm hợp đồng"));
        assertTrue(text.contains("Gia hạn"));
        assertEquals(3, AgreementTerm.daysLeft(start, start + 2 * DAY + 1));
    }
}
