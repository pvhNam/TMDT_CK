# Cấu hình thanh toán hiện tại

App chính dùng **VNPay TEST trực tiếp trong WebView như LTMobile**. Không có màn tiền giả, nút giả lập kết quả hoặc bản app offline riêng. Kết quả thành công phải là phản hồi có chữ ký hợp lệ do VNPay trả về.

## File cấu hình

`vnpay.properties` nằm ở gốc project, cùng cấp `local.properties` và `gradlew.bat`:

```properties
VNPAY_TMN_CODE=MA_WEBSITE_TEST
VNPAY_HASH_SECRET=KHOA_CHECKSUM_TEST
VNPAY_RETURN_URL=https://tmdt.example/vnpay/return
```

File trên máy đã được điền từ cấu hình VNPay đang có. Khi clone trên máy khác, sao chép `vnpay.properties.example` thành `vnpay.properties`, điền thông tin TEST của bạn. File chứa khóa được Git bỏ qua.

`VNPAY_RETURN_URL` được WebView bắt ngay trong app, không cần website hoạt động ở địa chỉ này. Luồng trực tiếp không đọc `payment-server/.env`, không yêu cầu `PUBLIC_BASE_URL`, Firebase Admin JSON hay máy tính chạy server. Không cần thêm `PAYMENT_API_BASE_URL` vào `local.properties` cho luồng này.

## Kiểm tra và chạy

```powershell
node scripts/check-vnpay-config.mjs
.\gradlew.bat :app:checkVnpayConfig :app:assembleDebug
```

Lệnh kiểm tra không in secret. Build debug tự kiểm tra cấu hình; nếu thiếu sẽ chỉ rõ trường cần điền. Đổi cấu hình cần build/cài lại app. APK: `build/outputs/TMDT-1.2.1-VNPay-TEST.apk` sau khi xuất, hoặc `app/build/outputs/apk/debug/app-debug.apk`.

Firestore Console → Rules: dán toàn bộ `FIRESTORE_RULES_FULL.txt` rồi Publish. Bộ rules trong project không đồng nghĩa đã Publish lên Firebase. Các collection `sandbox_` lưu giao dịch nhận từ cổng VNPay TEST, tách khỏi sổ tiền thật; không phải dữ liệu được sinh sẵn.

Luồng sử dụng: học sinh thanh toán đăng ký đã duyệt → VNPay trả kết quả → gia sư báo hoàn thành buổi → học sinh xác nhận → admin lập bảng chi trả tháng. Xem chi tiết trong `VNPAY_NATIVE_SANDBOX.md`.

Nếu VNPay trả **71 — The terminal (website) not approved**, cần VNPay duyệt/kích hoạt merchant TEST. Không tự đánh dấu đơn thành công khi gặp lỗi này.

Bản release không chứa secret TEST và tắt nhánh ký trực tiếp trong app. Luồng backend/IPN dành cho thanh toán thật được giữ ở `payment-server`, hướng dẫn riêng trong `VNPAY_SETUP.md`.
