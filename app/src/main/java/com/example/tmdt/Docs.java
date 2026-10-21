package com.example.tmdt;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Đọc an toàn các trường Firestore và thông báo lỗi dùng chung cho phần của Thành viên 4. */
final class Docs {
    private Docs() {}
    static final DocumentSnapshot.ServerTimestampBehavior ESTIMATE = DocumentSnapshot.ServerTimestampBehavior.ESTIMATE;

    static String text(DocumentSnapshot doc, String field) { return text(doc.get(field)); }
    static String text(Object value) { return value instanceof String ? (String) value : ""; }
    static long number(DocumentSnapshot doc, String field) { Object v = doc.get(field); return v instanceof Number ? ((Number) v).longValue() : 0; }
    static boolean flag(DocumentSnapshot doc, String field) { return Boolean.TRUE.equals(doc.getBoolean(field)); }
    static long millis(DocumentSnapshot doc, String field) { return millis(doc.getTimestamp(field, ESTIMATE)); }
    static long millis(Object value) { return value instanceof Timestamp ? ((Timestamp) value).toDate().getTime() : 0; }
    static Map<?, ?> map(Object value) { return value instanceof Map ? (Map<?, ?>) value : Collections.emptyMap(); }
    static List<String> strings(Object value) {
        List<String> result = new ArrayList<>();
        if (value instanceof List) for (Object item : (List<?>) value) if (item instanceof String) result.add((String) item);
        return result;
    }
    /** Mã ngắn dễ đọc từ mã tài liệu, ví dụ HĐ8F3K2A. */
    static String code(String prefix, String id) {
        String clean = id.replaceAll("[^A-Za-z0-9]", "");
        return prefix + clean.substring(0, Math.min(6, clean.length())).toUpperCase(Locale.ROOT);
    }
    static String money(long value) { return String.format(Locale.forLanguageTag("vi-VN"), "%,dđ", value); }

    static String message(Exception e) {
        if (e instanceof FirebaseNetworkException) return "Không có kết nối mạng. Vui lòng thử lại.";
        if (e instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException.Code code = ((FirebaseFirestoreException) e).getCode();
            if (code == FirebaseFirestoreException.Code.PERMISSION_DENIED) return "Bạn không có quyền thực hiện thao tác này.";
            if (code == FirebaseFirestoreException.Code.UNAVAILABLE) return "Không kết nối được máy chủ. Vui lòng thử lại.";
            if (code == FirebaseFirestoreException.Code.UNAUTHENTICATED) return "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.";
            if (code == FirebaseFirestoreException.Code.FAILED_PRECONDITION) return "Thiếu chỉ mục Firestore cho truy vấn này.";
        }
        return "Không hoàn tất được yêu cầu. Vui lòng thử lại.";
    }
}
