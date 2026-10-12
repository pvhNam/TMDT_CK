package com.example.tmdt;

/**
 * Một tin nhắn trong hội thoại (UC21). Lưu tại conversations/{id}/messages/{client_message_id}.
 * Mã tin do máy khách tạo nên gửi lại không bao giờ tạo bản sao.
 */
public final class ChatMessage {
    public static final int SENDING = 0, SENT = 1, READ = 2, FAILED = 3;

    public String id = "", senderId = "", content = "";
    /** Thời điểm gửi (ms). Tin đang chờ máy chủ dùng thời gian ước tính. */
    public long sentAt;
    public int status = SENT;
    /** Tin của người đang đăng nhập. */
    public boolean mine;

    public ChatMessage() {}
    public ChatMessage(String id, String senderId, String content, long sentAt, int status, boolean mine) {
        this.id = id; this.senderId = senderId; this.content = content; this.sentAt = sentAt; this.status = status; this.mine = mine;
    }
}
