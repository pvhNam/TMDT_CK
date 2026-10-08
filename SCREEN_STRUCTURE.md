# Cấu trúc giao diện Android

Mỗi màn hình có Fragment Java và layout XML riêng. Mở XML trong Android Studio rồi chọn **Split** hoặc **Design** để sửa bố cục; mở Fragment tương ứng để sửa hành vi nút bấm. Không dựng toàn bộ biểu mẫu bằng Java và không gom nhiều màn hình vào `AccountScreen` nữa.

Bốn màn hình Đăng nhập, Đăng ký, Quên mật khẩu và Chỉnh sửa hồ sơ dùng bố cục theo ảnh tham khảo Tài khoản và hồ sơ: TextInputLayout có biểu tượng, nút hiện/ẩn mật khẩu trong ô, nút xanh và thẻ nền xanh nhạt. `account_reference_styles.xml`, `account_reference_colors.xml` và `account_reference_strings.xml` quản lý kiểu và nội dung. `illustration_learning.xml` và `illustration_reset.xml` là minh họa vector; `ic_field_*.xml` là các biểu tượng trong ô nhập. Ảnh đại diện lấy từ hồ sơ thật, hiển thị chữ viết tắt khi chưa có ảnh. Khu vực có gợi ý và cho phép nhập tự do.

## Các màn hình

Trang Thông tin cá nhân dùng cùng `ReferenceToolbar`, `ReferenceTitle`, `ReferenceLabel`, avatar 88dp và nút xanh với trang Chỉnh sửa hồ sơ. Các giá trị chỉ đọc dùng `ReferenceProfileValue`: icon 24dp, viền 1dp, góc bo 9dp, chữ 14sp và ô email nền xanh nhạt. Người dùng có thể chọn/sao chép giá trị; chỉnh sửa qua nút Chỉnh sửa hồ sơ. Thứ tự thông tin là email, điện thoại, cấp học, khu vực, mục tiêu và địa chỉ.

Xác nhận 08/10/2026: `assembleDebug`, `assembleDebugAndroidTest` và `lintDebug` thành công, lint không có lỗi. Chưa xác nhận luồng hồ sơ trên thiết bị trong lượt này: máy ảo API 36.1 bị treo System UI/DeadSystemException và tiến trình instrumentation không chạy xong. Không dùng các ảnh hồ sơ cũ trong `build/ui-previews/` làm ảnh xác nhận giao diện mới.

| Chức năng | Java dưới app/src/main/java/com/example/tmdt/ | XML dưới app/src/main/res/layout/ |
| --- | --- | --- |
| Trang chủ | ui/home/HomeFragment.java | fragment_home.xml |
| Lịch học (màn hình chờ) | ui/schedule/ScheduleFragment.java | fragment_schedule.xml |
| Tin nhắn (màn hình chờ) | ui/messages/MessagesFragment.java | fragment_messages.xml |
| Chi tiết gia sư | ui/home/TutorDetailsDialog.java | dialog_tutor.xml |
| Đăng nhập | ui/auth/LoginFragment.java | fragment_login.xml |
| Đăng ký | ui/auth/RegisterFragment.java | fragment_register.xml |
| Quên mật khẩu | ui/auth/ForgotPasswordFragment.java | fragment_forgot_password.xml |
| Xác thực điện thoại tùy chọn | ui/auth/PhoneVerificationFragment.java | fragment_phone_verification.xml |
| Thông tin cá nhân | ui/profile/AccountFragment.java | fragment_account.xml |
| Chỉnh sửa hồ sơ | ui/profile/EditAccountFragment.java | fragment_edit_account.xml |
| Đăng ký gia sư | ui/profile/RegisterTutorFragment.java | fragment_register_tutor.xml |
| Kiểm tra và gửi hồ sơ gia sư | ui/profile/ReviewTutorFragment.java | fragment_review_tutor.xml |

`item_tutor.xml` là một thẻ gia sư. `item_offering.xml` là một dòng môn dạy/học phí. `view_avatar.xml` và `view_account_feedback.xml` là các thành phần dùng chung.

## Phân trách nhiệm

- `MainActivity`: chứa FragmentContainerView, thanh điều hướng, xử lý quay lại và kiểm tra đăng nhập trước khi vào màn hình cá nhân. Activity không dựng biểu mẫu và không truy vấn Firebase.
- `ui/auth`, `ui/profile`, `ui/home`: View Binding, nhận thao tác của người dùng và hiển thị trạng thái. Không import Firebase SDK trong các lớp này.
- `ui/common/AccountFragmentBase`: hành vi chung của biểu mẫu (thông báo lỗi, trạng thái chờ, kiểm tra đầu vào, giữ bản nháp). Không chứa nhánh lựa chọn giao diện đăng nhập/đăng ký/cá nhân.
- `AccountState`: ViewModel dùng chung, gọi Firebase Auth/Firestore, giữ phiên và các yêu cầu đang chạy khi xoay máy. Cung cấp LiveData chỉ đọc cho màn hình; bản nháp mật khẩu chỉ tồn tại trong bộ nhớ.
- `CatalogState`: ViewModel danh mục, quản lý listener Firestore, bộ lọc và kết quả tìm kiếm.
- `UserProfile`: ánh xạ dữ liệu người dùng. `AccountValidation`: quy tắc kiểm tra và chuẩn hóa đầu vào.
- `screen_strings.xml`, `screen_styles.xml`, `drawable/bg_*.xml`: nội dung, kiểu chữ, khoảng cách và nền dùng chung. `menu_bottom_navigation.xml`: các mục điều hướng.

Thanh menu theo Figma hiển thị **Trang chủ / Lịch học / Tin nhắn / Cá nhân**, nền trắng và mục đang chọn màu xanh. Bấm Cá nhân khi chưa đăng nhập mở biểu mẫu đăng nhập. Menu chỉ hiện trên bốn màn hình chính, ẩn trên biểu mẫu và khi bàn phím mở, giữ đúng mục sau khi xoay máy. Màu và chữ ở `color/navigation_item.xml`, `values/navigation_styles.xml`; tám SVG gốc trong `assets/figma/navigation/` được `NavigationAssets` đọc nguyên vẹn. Xem `FIGMA_MENU.md` để đối chiếu nguồn thiết kế. Lịch học và Tin nhắn mới có màn hình chờ, chưa kết nối nghiệp vụ.

Mỗi Fragment bỏ tham chiếu binding ở `onDestroyView`. Observer gắn với `getViewLifecycleOwner()` nên không cập nhật vào giao diện đã bị hủy. Đổi trạng thái đang xử lý không tạo lại màn hình và không xóa ô đang nhập. Fragment chỉnh sửa hồ sơ sở hữu trình chọn ảnh của chính nó.

## Mã tham khảo trước khi tách

`legacy/java/`, `legacy/tests/`, `legacy/res/` giữ lại bản giao diện dựng bằng Java, luồng đặt lịch mô phỏng và template Android ban đầu. Các thư mục này nằm ngoài source set của app nên không được biên dịch hay xuất hiện trong ứng dụng. Dữ liệu Firebase/rules không bị xóa hoặc thay đổi do việc tách giao diện.

## Kiểm tra

### Giao diện bo mềm

`res/values/soft_ui.xml` tập trung bán kính dùng chung: ô nhập 18dp, nút/thẻ 24dp, hộp thoại/menu 28dp. XML tài khoản và đăng ký gia sư dùng các kích thước này; `Ui.cardSurface`/`Ui.fieldSurface` áp dụng cho lịch học, lớp học và không gian gia sư. Viền xanh xám nhẹ, nền ô nhập và các thẻ ghi chú xanh nhạt. Hiệu ứng chạm giữ đúng đường bo của thành phần.

Thanh menu có nền xanh nhạt bo tròn, cách mép màn hình 12dp, vẫn giữ các mục và icon hiện có. Đây là điều chỉnh hình thức theo yêu cầu bo mềm sau bản Figma; không thay đổi luồng điều hướng hay quyền Firebase.

```powershell
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintDebug
```

Các kiểm thử Android liên quan: `ScreenNavigationTest` kiểm tra mỗi đích điều hướng là một Fragment riêng, quyền khách và quay lại; `AccountUiTest` kiểm tra đầu vào và bản nháp qua xoay máy; `AccountFirebaseTest` kiểm tra tài khoản và thao tác sửa/lưu qua XML; `CatalogFirebaseTest` kiểm tra trang chủ với dữ liệu Firestore Emulator.

Cấu hình Firebase thật vẫn theo `FIREBASE_SETUP.md`; việc tách giao diện không tự publish rules hoặc bật phương thức đăng nhập.
