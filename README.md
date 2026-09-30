# Ứng dụng thuê gia sư — Android và Firebase

Bản hiện tại triển khai **trang chủ, đăng nhập, đăng ký và thông tin cá nhân** theo `thiết kế giao diện mobile_1.docx`, với cấu trúc dữ liệu rút gọn từ `03_Thiet_ke_CSDL_v4.md` theo yêu cầu loại bỏ các luồng không sử dụng.

## Chức năng

- Trang chủ xanh–trắng, banner, tìm kiếm không dấu và bộ lọc môn. Danh sách gia sư đọc từ Firestore, tự cập nhật; không dùng danh sách giả khi database trống.
- Thông tin gia sư gồm giới thiệu, kinh nghiệm, hình thức, môn dạy và học phí cho buổi 90 phút.
- Đăng ký/đăng nhập Firebase Email/Password, kiểm tra đầu vào và mật khẩu xác nhận, lưu phiên, quên mật khẩu qua email.
- Trang cá nhân hiển thị và chỉnh sửa hồ sơ, địa chỉ, số điện thoại, ảnh đại diện, cấp học và mục tiêu; đăng xuất. Xác thực điện thoại là chức năng bổ sung tùy chọn.
- Firestore Rules bảo vệ hồ sơ theo UID, chặn nâng vai trò và chặn client sửa danh mục gia sư.

Đã bỏ khỏi điều hướng và dữ liệu của các màn hình mới: tin đăng/ứng tuyển/lời mời, minh chứng bằng cấp, hợp đồng chống dạy ngoài, học thử/điểm danh, đánh giá, ví/ký quỹ/thanh toán, tin nhắn và cấu hình hệ thống cùng các trường đã được yêu cầu bỏ. Chi tiết tại [schema rút gọn](FIRESTORE_SCHEMA.md).

## Chạy dự án

1. Mở thư mục này bằng Android Studio; sử dụng JDK 21 và Android SDK của dự án.
2. Hoàn tất [cấu hình Firebase](FIREBASE_SETUP.md), đặc biệt **Email/Password** và **Firestore Rules**.
3. Chọn thiết bị Android 10 trở lên rồi Run.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

Project Firebase: `tmdt-3e274`. Việc có `google-services.json` chỉ cấu hình ứng dụng; không đồng nghĩa rules đã được publish hoặc phương thức đăng nhập đã bật trên Console. Dữ liệu `users/{UID}` được tạo khi đăng ký thành công. Gia sư/môn dạy cần được quản trị nhập đúng [schema](FIRESTORE_SCHEMA.md).

## Mã nguồn chính

| File | Chức năng |
| --- | --- |
| MainActivity.java | Host Fragment, điều hướng và kiểm tra đăng nhập |
| ui/home/HomeFragment.java | Trang chủ dùng fragment_home.xml |
| CatalogState.java | Đọc/lắng nghe danh mục Firestore, nối môn và học phí |
| ui/auth/*.java | Fragment đăng nhập, đăng ký, quên mật khẩu, OTP |
| ui/profile/*.java | Fragment cá nhân và chỉnh sửa hồ sơ |
| AccountState.java | Firebase Auth, lưu/tải hồ sơ, xử lý lỗi |
| UserProfile.java | Ánh xạ trường dữ liệu tiếng Việt |
| AccountValidation.java | Kiểm tra và chuẩn hóa đầu vào |
| firestore.rules | Phân quyền và kiểm tra dữ liệu phía máy chủ |
| scripts/test-firebase.mjs | Kiểm thử Auth/rules trên localhost, fixture danh mục |

Mỗi màn hình hiện có Fragment và XML riêng theo từng chức năng. Xem [cấu trúc giao diện](SCREEN_STRUCTURE.md). Mã mô phỏng cũ được lưu ngoài source set tại `legacy/`.

## Kiểm thử Firebase

Xem lệnh chạy Emulator và kiểm thử Android trong [FIREBASE_SETUP.md](FIREBASE_SETUP.md). Script kiểm thử chỉ dùng localhost, không ghi dữ liệu kiểm thử lên Firebase thật. Chức năng OTP/SMS thật và email thật cần cấu hình Console và xác nhận riêng.
