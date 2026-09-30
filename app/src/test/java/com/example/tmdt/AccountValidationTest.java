package com.example.tmdt;
import org.junit.Test;
import static org.junit.Assert.*;
public class AccountValidationTest {
    @Test public void normalizesEmailAndVietnamesePhone() {
        assertEquals("nam@example.com",AccountValidation.email(" Nam@Example.com "));
        assertEquals("+84912345678",AccountValidation.phone("0912 345 678"));
        assertTrue(AccountValidation.validPhone("+84 912 345 678"));
        assertEquals("0912345678",AccountValidation.localPhone("+84 912 345 678"));
        assertFalse(AccountValidation.validPhone("091234567"));
        assertFalse(AccountValidation.validPhone("+84112345678"));
    }
    @Test public void rejectsInvalidRegistrationFields() {
        assertFalse(AccountValidation.validEmail("a b@example.com"));
        assertFalse(AccountValidation.validEmail("a@b"));
        assertTrue(AccountValidation.validEmail("nam+study@example.com"));
        assertFalse(AccountValidation.validName("  "));
        assertTrue(AccountValidation.validName("Phạm Văn Nam"));
        assertFalse(AccountValidation.validPassword("1234567"));
        assertTrue(AccountValidation.validPassword("HocTap123!"));
    }
}

