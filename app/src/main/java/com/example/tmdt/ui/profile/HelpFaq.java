package com.example.tmdt.ui.profile;

import com.example.tmdt.CatalogState;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

final class HelpFaq {
    private static final Pattern WORD_SEPARATOR = Pattern.compile("[^a-z0-9]+");
    private static final String FALLBACK = "Mình chưa tìm thấy hướng dẫn cho câu hỏi này. "
            + "Bạn có thể hỏi lại ngắn hơn, hoặc bấm Chủ đề để chọn nội dung cần hỗ trợ.";

    static final List<Topic> TOPICS = Collections.unmodifiableList(Arrays.asList(
            new Topic("Tìm gia sư",
                    "Vào Trang chủ, nhập tên gia sư vào ô tìm kiếm hoặc chọn môn học. "
                            + "Chạm vào một gia sư để xem hồ sơ, học phí và hình thức dạy.",
                    "tìm giáo viên", "tìm người dạy", "kiếm gia sư", "xem gia sư", "tìm tutor"),
            new Topic("Đặt lịch học",
                    "Mở hồ sơ gia sư và chọn Đặt lịch học. Chọn ngày, giờ và điền mục tiêu học, "
                            + "sau đó bấm Gửi yêu cầu đặt lịch. Bạn có thể theo dõi yêu cầu trong Lịch học → Chờ xác nhận.",
                    "đặt lịch", "đặt buổi học", "đặt học riêng", "đặt lịch với gia sư", "gửi yêu cầu đặt lịch"),
            new Topic("Quên mật khẩu",
                    "Ở màn Đăng nhập, chọn Quên mật khẩu?, nhập email đăng ký rồi mở liên kết đặt lại mật khẩu "
                            + "trong hộp thư. Nếu đang đăng nhập, vào Cài đặt → Đổi mật khẩu để gửi liên kết. "
                            + "Nếu chưa thấy email, kiểm tra cả thư mục Thư rác.",
                    "lấy lại mật khẩu", "đặt lại mật khẩu", "đổi mật khẩu", "không nhớ mật khẩu",
                    "quên mk", "quên pass", "đổi mk", "reset password"),
            new Topic("Thanh toán học phí",
                    "Mở chi tiết buổi học, lớp nhóm hoặc hợp đồng đã được xác nhận. Khi có nút Thanh toán, "
                            + "chọn Tiếp tục với VNPay và làm theo hướng dẫn trên cổng thanh toán. "
                            + "Sau đó, mở lại chi tiết giao dịch để xem trạng thái.",
                    "thanh toán", "đóng học phí", "nộp học phí", "trả học phí", "vnpay", "vn pay"),
            new Topic("Xem lịch học",
                    "Ở chế độ học viên, mở mục Lịch học. Chọn Sắp tới, Chờ xác nhận hoặc Đã học "
                            + "rồi chạm Chi tiết để xem thông tin buổi học.",
                    "xem lịch", "lịch học", "lịch sắp tới", "xem buổi học", "lịch của em"),
            new Topic("Đổi lịch học",
                    "Học viên có thể nhắn tin với gia sư để thống nhất giờ mới. Khi gia sư gửi đề nghị đổi lịch, "
                            + "mở Lịch học → Chi tiết buổi học, chạm đề nghị và chọn phản hồi phù hợp.",
                    "đổi lịch", "dời lịch", "dời buổi học", "đổi giờ học", "thay đổi lịch"),
            new Topic("Hủy buổi học",
                    "Mở Lịch học → Chi tiết buổi học. Nếu có nút Hủy buổi học, bấm nút này, "
                            + "chọn lý do rồi bấm Xác nhận hủy.",
                    "hủy lịch", "hủy đặt lịch", "hủy buổi", "hủy lịch học", "hủy buổi dạy"),
            new Topic("Đăng ký lớp nhóm",
                    "Vào Trang chủ → Lớp học nhóm. Chọn lớp, bấm Xem lớp và gửi đăng ký. "
                            + "Khi gia sư chấp nhận, mở chi tiết đăng ký để thanh toán học phí.",
                    "đăng ký lớp học nhóm", "lớp học nhóm", "học nhóm", "tham gia lớp nhóm", "đăng ký khóa học"),
            new Topic("Đăng ký làm gia sư",
                    "Vào Cá nhân → Đăng ký làm gia sư. Điền thông tin giảng dạy, thêm ảnh minh chứng "
                            + "và gửi hồ sơ xét duyệt. Bạn có thể mở lại hồ sơ ở mục Cá nhân để xem kết quả.",
                    "đăng ký gia sư", "làm gia sư", "làm giáo viên", "đăng ký dạy", "hồ sơ gia sư chờ duyệt"),
            new Topic("Chỉnh sửa hồ sơ",
                    "Vào Cá nhân và chọn Chỉnh sửa hồ sơ. Cập nhật thông tin hoặc ảnh đại diện, "
                            + "sau đó bấm lưu thay đổi.",
                    "sửa hồ sơ", "sửa thông tin cá nhân", "đổi tên", "đổi ảnh đại diện", "thay ảnh đại diện", "đổi avatar"),
            new Topic("Nhắn tin với gia sư",
                    "Mở hồ sơ gia sư hoặc chi tiết buổi học và chọn Nhắn tin. "
                            + "Bạn có thể mở lại cuộc trò chuyện trong mục Tin nhắn.",
                    "nhắn tin", "chat với gia sư", "liên hệ gia sư", "gửi tin nhắn"),
            new Topic("Kiểm tra giao dịch",
                    "Ở chế độ học viên, vào Cá nhân → Thanh toán của tôi và chọn giao dịch cần xem. "
                            + "Chi tiết giao dịch hiển thị số tiền và trạng thái thanh toán.",
                    "xem giao dịch", "lịch sử thanh toán", "thanh toán của tôi", "thanh toán chưa xác nhận",
                    "thanh toán thành công chưa", "đã thanh toán chưa", "kiểm tra thanh toán"),
            new Topic("Đăng nhập bằng Google",
                    "Ở màn Đăng nhập, bấm Đăng nhập bằng Google và chọn tài khoản. "
                            + "Nếu app yêu cầu bổ sung hồ sơ lần đầu, điền các thông tin còn thiếu để tiếp tục.",
                    "đăng nhập google", "login google", "đăng nhập gmail", "đăng nhập bằng gmail"),
            new Topic("Đăng ký tài khoản",
                    "Ở màn Đăng nhập, chọn Đăng ký và hoàn thành thông tin yêu cầu. "
                            + "Mở email xác nhận Firebase gửi tới địa chỉ đã đăng ký, bấm liên kết xác thực rồi quay lại ứng dụng. "
                            + "Nếu chưa thấy thư, kiểm tra mục Thư rác.",
                    "tạo tài khoản", "xác thực email", "xác nhận gmail", "email đăng ký", "không nhận email xác nhận"),
            new Topic("Ví gia sư",
                    "Ở tài khoản gia sư, vào Cá nhân → Ví & thanh toán → Ví. "
                            + "Bạn có thể xem số dư khả dụng và lịch sử tiền lương, rút tiền. "
                            + "Chọn một khoản để mở chi tiết.",
                    "xem ví", "số dư ví", "ví giáo viên", "tiền trong ví"),
            new Topic("Rút tiền về ngân hàng",
                    "Vào Cá nhân → Ví & thanh toán → Ví, kiểm tra Ngân hàng nhận tiền và số dư khả dụng. "
                            + "Chọn Rút về ngân hàng, nhập số tiền và làm theo bước xác nhận. "
                            + "Nếu nút chưa bật, kiểm tra số dư và thông tin tài khoản nhận tiền.",
                    "rút tiền", "rút lương", "rút về ngân hàng", "không rút được tiền"),
            new Topic("Thu nhập gia sư",
                    "Ở tài khoản gia sư, vào Cá nhân → Ví & thanh toán → Thu nhập. "
                            + "Chọn Doanh thu hoặc Thu nhập và chi trả để xem khoản thu và tình trạng chi trả. "
                            + "Số dư đã được cộng vào ví được xem ở mục Ví.",
                    "xem thu nhập", "xem lương", "tiền lương", "lương gia sư", "doanh thu gia sư"),
            new Topic("Hợp đồng của tôi",
                    "Vào Cá nhân → Hợp đồng của tôi và chọn hợp đồng cần xem. "
                            + "Kiểm tra nội dung và trạng thái trong chi tiết hợp đồng; thực hiện các nút xác nhận khi được hiển thị.",
                    "xem hợp đồng", "ký hợp đồng", "hợp đồng cộng tác", "kiểm tra hợp đồng")
    ));

    private HelpFaq() {}

    static String knowledge() {
        StringBuilder text = new StringBuilder();
        for (Topic topic : TOPICS) {
            text.append("\nCâu hỏi: ").append(topic.question)
                    .append("\nHướng dẫn: ").append(topic.answer).append('\n');
        }
        return text.toString();
    }

    static String answer(String question) {
        String normalized = normalize(question);
        String text = " " + normalized + " ";
        Topic best = null;
        int bestLength = 0;
        for (Topic topic : TOPICS) {
            for (String phrase : topic.phrases) {
                if (normalized.equals(phrase)) return topic.answer;
                if (phrase.length() > bestLength && text.contains(" " + phrase + " ")) {
                    best = topic;
                    bestLength = phrase.length();
                }
            }
        }
        return best == null ? FALLBACK : best.answer;
    }

    private static String normalize(String value) {
        return WORD_SEPARATOR.matcher(CatalogState.normalize(value)).replaceAll(" ").trim();
    }

    static final class Topic {
        final String question;
        private final String answer;
        private final String[] phrases;

        Topic(String question, String answer, String... aliases) {
            this.question = question;
            this.answer = answer;
            phrases = new String[aliases.length + 1];
            phrases[0] = normalize(question);
            for (int i = 0; i < aliases.length; i++) {
                phrases[i + 1] = normalize(aliases[i]);
            }
        }
    }
}
