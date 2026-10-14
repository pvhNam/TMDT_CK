package com.example.tmdt;

import java.util.HashMap;
import java.util.Map;

/**
 * Hội thoại giữa hai thành viên (UC21). Lưu tại conversations/{uidA__uidB} với uid đã sắp xếp,
 * nên mỗi cặp học viên – gia sư chỉ có đúng một hội thoại.
 */
public final class Conversation {
    public static final String SEPARATOR = "__";

    public String id = "", peerId = "", peerName = "", peerRole = "", myRole = "";
    public String lastMessage = "", lastSenderId = "", createdBy = "";
    public long lastMessageAt, createdAt;
    public int unread;
    /** Lần cuối người kia đã đọc hội thoại (ms), dùng cho dấu "đã xem". */
    public long peerReadAt, myReadAt;
    /** Buổi học liên quan hiển thị trên đầu màn hình trò chuyện; có thể trống. */
    public String lessonTitle = "", lessonTime = "", lessonStatus = "";

    public boolean hasLesson() { return !lessonTitle.isEmpty(); }
    public long sortTime() { return lastMessageAt > 0 ? lastMessageAt : createdAt; }

    /** Mã hội thoại xác định từ hai uid, không phụ thuộc ai mở trước. */
    public static String idFor(String a, String b) {
        return a.compareTo(b) < 0 ? a + SEPARATOR + b : b + SEPARATOR + a;
    }

    /** Thông tin buổi học gửi lên Firestore (map lesson). */
    public static Map<String, Object> lesson(String title, String time, String status) {
        Map<String, Object> data = new HashMap<>();
        data.put("title", title); data.put("time", time); data.put("status", status);
        return data;
    }
}
