package com.example.tmdt;

/** The short name is an alias only; Firebase still authenticates the password. */
public final class LoginIdentity {
    public static final String ADMIN_EMAIL = "admin@tmdt.invalid";
    private LoginIdentity() {}
    public static String email(String value) {
        String input=value==null?"":value.trim();
        return "admin".equalsIgnoreCase(input)?ADMIN_EMAIL:AccountValidation.email(input);
    }
    public static boolean valid(String value) {
        return "admin".equalsIgnoreCase(value==null?"":value.trim()) || AccountValidation.validEmail(value==null?"":value.trim());
    }
}
