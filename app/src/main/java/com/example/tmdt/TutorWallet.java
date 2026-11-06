package com.example.tmdt;

public final class TutorWallet {
    public static final long MIN_WITHDRAWAL=50000,MAX_WITHDRAWAL=100000000;
    private TutorWallet(){}
    public static boolean salaryPaid(String status){return "PAID".equals(status)||"CREDITED".equals(status);}
    public static String withdrawalStatus(String status){
        switch(status){case "PENDING":return "Chờ chuyển khoản";case "PROCESSING":return "Đang chuyển khoản";case "PAID":return "Đã chuyển khoản";
            case "REJECTED":return "Đã từ chối · Hoàn về ví";case "CANCELLED":return "Đã hủy · Hoàn về ví";default:return status;}
    }
    public static long available(PaymentState.Row wallet){return wallet==null?0:wallet.number("available_balance");}
    public static long reserved(PaymentState.Row wallet){return wallet==null?0:wallet.number("reserved_balance");}
    public static boolean withdrawing(String status){return "PENDING".equals(status)||"PROCESSING".equals(status);}
}
