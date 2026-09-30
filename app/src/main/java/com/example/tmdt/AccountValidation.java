package com.example.tmdt;

import java.util.Locale;

public final class AccountValidation {
    private AccountValidation() {}
    public static String email(String value) { return value.trim().toLowerCase(Locale.ROOT); }
    public static boolean validEmail(String value) {
        return value.length() <= 254 && value.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+");
    }
    public static String phone(String value) {
        String number = value.replaceAll("[\\s().-]", "");
        if (number.startsWith("0")) number = "+84" + number.substring(1);
        return number;
    }
    public static boolean validPhone(String value) { return phone(value).matches("\\+84[35789][0-9]{8}"); }
    public static String localPhone(String value) { return phone(value).replaceFirst("^\\+84", "0"); }
    public static boolean validName(String value) { return value.trim().length() >= 2 && value.trim().length() <= 100; }
    public static boolean validPassword(String value) { return value.length() >= 8 && value.length() <= 128; }
}

