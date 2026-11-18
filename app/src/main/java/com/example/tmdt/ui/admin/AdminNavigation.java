package com.example.tmdt.ui.admin;

import com.example.tmdt.R;

/** The admin's five destinations have their own menu IDs and never reuse chat/account badges. */
public final class AdminNavigation {
    private AdminNavigation() {}

    public static String destination(int item) {
        if (item == R.id.nav_admin_management) return "admin_management";
        if (item == R.id.nav_admin_contracts) return "admin_contracts";
        if (item == R.id.nav_admin_finance) return "admin_finance";
        if (item == R.id.nav_admin_account) return "account";
        return "admin_dashboard";
    }

    public static boolean mainScreen(String screen) {
        return "admin_dashboard".equals(screen) || "admin_management".equals(screen)
                || "admin_contracts".equals(screen) || "admin_finance".equals(screen)
                || "account".equals(screen);
    }

    public static int item(String screen) {
        switch (screen) {
            case "admin_dashboard": case "admin_stats":
                return R.id.nav_admin_home;
            case "admin_management": case "admin_users": case "admin_user":
            case "admin_tutors": case "admin_tutor_manage": case "admin_classes":
            case "admin_roles": case "report_admin": case "report_handle":
                return R.id.nav_admin_management;
            case "admin_contracts": case "admin_contract": case "admin_agreement": case "contract_templates":
                return R.id.nav_admin_contracts;
            case "admin_finance": case "admin_income": case "admin_tutor_income":
            case "admin_payroll": case "admin_withdrawals": case "withdrawal_detail":
            case "payments": case "payment_detail": case "payout_detail": case "payout_bank":
                return R.id.nav_admin_finance;
            default:
                return R.id.nav_admin_account;
        }
    }
}
