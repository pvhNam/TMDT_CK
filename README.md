# Ứng dụng thuê gia sư

Ứng dụng Android hỗ trợ người học tìm kiếm gia sư, xem thông tin giảng dạy, lựa chọn thời gian học và theo dõi lịch học trên điện thoại.

**Trạng thái hiện tại:** dự án đang ở giai đoạn xây dựng giao diện mẫu và mô phỏng luồng sử dụng dành cho học viên. Ứng dụng có 4 màn hình hoạt động với dữ liệu mẫu; chưa kết nối máy chủ và chưa cung cấp dịch vụ đặt gia sư thực tế.

## 1. Giới thiệu đề tài

Việc tìm gia sư thường đòi hỏi người học hoặc phụ huynh tham khảo nhiều nguồn thông tin, trao đổi về môn học, học phí và thời gian trước khi quyết định. Đề tài hướng đến việc tập hợp những thông tin này trong một ứng dụng, giúp quá trình tìm kiếm và đặt lịch học thuận tiện hơn.

Trong lĩnh vực thương mại điện tử, ứng dụng được định hướng là nền tảng kết nối người có nhu cầu học với người cung cấp dịch vụ gia sư. Dịch vụ được giới thiệu thông qua hồ sơ gia sư; người học lựa chọn và gửi yêu cầu đặt buổi học phù hợp với nhu cầu.

## 2. Mục tiêu

- Xây dựng giao diện tiếng Việt dễ sử dụng trên thiết bị Android.
- Giúp người học tìm gia sư theo tên hoặc môn học.
- Trình bày thông tin gia sư, kinh nghiệm, đánh giá và học phí để hỗ trợ lựa chọn.
- Cho phép lựa chọn ngày, giờ, thời lượng và hình thức học.
- Tập trung lịch học và yêu cầu đặt học vào một nơi để dễ theo dõi.
- Tạo nền tảng giao diện để tiếp tục phát triển các chức năng thực tế khi có máy chủ và cơ sở dữ liệu.

## 3. Đối tượng sử dụng

| Đối tượng | Nhu cầu | Phạm vi hiện tại |
| --- | --- | --- |
| Học viên, phụ huynh | Tìm gia sư, xem học phí, đặt và theo dõi buổi học | Đã có giao diện mẫu cho luồng học viên |
| Gia sư | Giới thiệu chuyên môn, quản lý thời gian và phản hồi yêu cầu đặt học | Định hướng phát triển, chưa có màn hình riêng |
| Quản trị viên | Quản lý người dùng, hồ sơ gia sư và hoạt động của nền tảng | Định hướng phát triển, chưa triển khai |

## 4. Mô tả bốn màn hình

### 4.1. Trang chủ

Trang đầu tiên giúp người dùng khám phá và tìm kiếm gia sư.

- Hiển thị lời chào, ô tìm kiếm và banner giới thiệu.
- Có các nhóm môn học: Toán, Tiếng Anh, Vật lý và Hóa học.
- Hiển thị gia sư nổi bật với ảnh, tên, môn dạy, đánh giá và học phí theo giờ.
- Hỗ trợ tìm kiếm không dấu, ví dụ nhập `nguyen minh anh` để tìm Nguyễn Minh Anh.
- Chạm vào thẻ gia sư để mở hồ sơ chi tiết.
- Có thanh điều hướng Trang chủ, Lịch học, Tin nhắn và Cá nhân.

Dữ liệu mẫu hiện có 2 gia sư thuộc môn Toán và Tiếng Anh. Khi chọn môn chưa có dữ liệu hoặc tìm kiếm không có kết quả, ứng dụng hiển thị trạng thái trống và nút xóa bộ lọc.

### 4.2. Hồ sơ gia sư

Màn hình cung cấp thông tin giúp người học cân nhắc trước khi đặt lịch.

- Ảnh đại diện, họ tên, môn học và cấp độ giảng dạy.
- Nhãn xác minh, điểm đánh giá, kinh nghiệm và số học viên trong dữ liệu mẫu.
- Giới thiệu phương pháp giảng dạy.
- Hình thức học trực tuyến hoặc tại nhà.
- Học phí theo giờ và nhận xét của học viên mẫu.
- Nút lưu gia sư yêu thích, nhắn tin và đặt lịch học.

Nhãn xác minh và các nhận xét chỉ phục vụ trình bày giao diện, chưa được kiểm chứng bởi một hệ thống quản trị thực tế.

### 4.3. Đặt lịch học

Màn hình cho phép người học thiết lập thông tin của một buổi học.

- Xem lại gia sư và đơn giá đã chọn.
- Chọn học trực tuyến hoặc tại nhà; nhập địa chỉ nếu học tại nhà.
- Chọn ngày bằng dãy ngày hoặc hộp thoại lịch.
- Chọn một trong các giờ mẫu: 18:00, 19:00 hoặc 20:00.
- Chọn thời lượng 60, 90 hoặc 120 phút.
- Nhập mục tiêu buổi học.
- Xem học phí và tổng cộng được cập nhật theo thời lượng.
- Gửi yêu cầu để lưu trên thiết bị và chuyển đến tab Chờ xác nhận.

Cách tính học phí:

```text
Học phí = Đơn giá theo giờ × Thời lượng học (phút) / 60

Ví dụ: 150.000đ/giờ × 90 phút / 60 = 225.000đ
```

Ứng dụng kiểm tra thời gian phải ở trong tương lai, mục tiêu không được để trống, địa chỉ bắt buộc khi học tại nhà và khung giờ không trùng với buổi học hoặc yêu cầu đã có trên thiết bị. Các khung giờ mẫu chưa phản ánh lịch rảnh thực tế của gia sư.

### 4.4. Lịch học

Màn hình tập hợp các buổi học theo ba tab:

- **Sắp tới:** hiển thị các buổi học mẫu đã xác nhận và chưa kết thúc.
- **Chờ xác nhận:** hiển thị yêu cầu đặt học được lưu trên thiết bị.
- **Đã học:** dành cho các buổi đã xác nhận có thời gian kết thúc trong quá khứ; ban đầu có thể chưa có dữ liệu.

Mỗi thẻ buổi học hiển thị môn học, gia sư, trạng thái, ngày giờ, hình thức học và các nút nhắn tin, xem chi tiết. Chi tiết buổi học gồm mục tiêu, thời lượng thông qua khung giờ, học phí và địa chỉ nếu có.

## 5. Luồng sử dụng mẫu

1. Mở ứng dụng và xem danh sách gia sư tại Trang chủ.
2. Tìm kiếm hoặc chọn môn học phù hợp.
3. Chọn một gia sư để xem hồ sơ.
4. Nhấn **Đặt lịch học**.
5. Chọn hình thức, ngày giờ, thời lượng và nhập mục tiêu buổi học.
6. Kiểm tra học phí rồi nhấn **Gửi yêu cầu đặt lịch**.
7. Xem yêu cầu vừa tạo tại **Lịch học → Chờ xác nhận**.

Yêu cầu trong bản mẫu chỉ được lưu cục bộ, chưa gửi đến gia sư và không tự chuyển thành đã xác nhận.

## 6. Phần đã thực hiện và giới hạn

| Hạng mục | Trạng thái |
| --- | --- |
| Bốn giao diện và điều hướng giữa các trang | Đã thực hiện |
| Tìm kiếm, lọc danh sách gia sư | Hoạt động với dữ liệu mẫu |
| Lưu gia sư yêu thích | Lưu trên thiết bị bằng SharedPreferences |
| Chọn lịch, đổi thời lượng và tính học phí | Đã thực hiện trong luồng mô phỏng |
| Lưu và xem lại yêu cầu đặt học | Lưu trên thiết bị bằng SharedPreferences |
| Khôi phục biểu mẫu đặt học khi Activity được tạo lại | Đã thực hiện |
| Đăng ký, đăng nhập và phân quyền | Chưa triển khai |
| Cơ sở dữ liệu dùng chung, đồng bộ giữa các thiết bị | Chưa triển khai |
| Gia sư xác nhận, từ chối hoặc đổi lịch | Chưa triển khai |
| Trò chuyện, thông báo và trang cá nhân thực tế | Chưa triển khai; các nút hiện mở thông báo mẫu |
| Thanh toán, hoàn tiền và đánh giá sau buổi học | Chưa triển khai |

Hai buổi học đã xác nhận ban đầu được tạo từ dữ liệu mẫu với ngày tính theo thời điểm mở ứng dụng. Đây không phải lịch đã đặt trên hệ thống thật. Dữ liệu lưu cục bộ không đồng bộ sang thiết bị khác và có thể mất khi xóa dữ liệu ứng dụng.

## 7. Công nghệ sử dụng

| Thành phần | Công nghệ |
| --- | --- |
| Nền tảng | Android native |
| Ngôn ngữ mã nguồn ứng dụng | Java, mức tương thích Java 11 |
| Môi trường phát triển | Android Studio |
| Xây dựng giao diện | Android Views, XML, AndroidX AppCompat và Material Components |
| Lưu trữ bản mẫu | SharedPreferences, dữ liệu yêu cầu được tuần tự hóa thành JSON |
| Công cụ build | Gradle, cấu hình Kotlin DSL |
| Kiểm thử | JUnit, AndroidX Test và Espresso |
| Kiểm tra mã nguồn | Android Lint |
| Android tối thiểu | Android 10, API 29 |
| SDK mục tiêu | API 36 |

Khung chính của ứng dụng được khai báo bằng XML. Nội dung bốn trang được dựng bằng các lớp Android Views trong Java, có cuộn và xử lý khoảng trống cho thanh hệ thống, bàn phím.

## 8. Cấu trúc mã nguồn chính

```text
app/src/main/
├── java/com/example/tmdt/
│   ├── MainActivity.java       # Điều hướng, khôi phục trạng thái và lưu yêu cầu
│   ├── HomeScreen.java         # Trang chủ
│   ├── ProfileScreen.java      # Hồ sơ gia sư
│   ├── BookingScreen.java      # Đặt lịch học
│   ├── ScheduleScreen.java     # Lịch học
│   ├── Ui.java                 # Thành phần giao diện và định dạng dùng chung
│   ├── LineIcon.java           # Vẽ biểu tượng
│   ├── DesignImage.java        # Hiển thị ảnh từ thiết kế tham chiếu
│   ├── Tutor.java              # Dữ liệu gia sư mẫu
│   └── Lesson.java             # Dữ liệu buổi học
├── res/
│   ├── layout/activity_main.xml
│   ├── drawable-nodpi/design_reference.png
│   └── values/                 # Tài nguyên và giao diện chủ đề
└── AndroidManifest.xml
```

Ảnh thiết kế tham chiếu nằm tại thư mục gốc dự án:

![Thiết kế bốn màn hình](ChatGPT%20Image%2011_41_39%2011%20thg%209,%202026.png)

Giao diện sử dụng tông xanh–trắng, thẻ bo góc và biểu tượng nét mảnh theo ảnh mẫu. Ảnh chân dung và banner hiện được hiển thị từ các vùng của ảnh thiết kế đóng gói trong ứng dụng, không cần tải từ mạng. Khi triển khai sản phẩm thật có thể thay bằng các tài nguyên riêng và ảnh hồ sơ từ hệ thống.

## 9. Hướng dẫn chạy

### Chạy trong Android Studio

1. Mở thư mục dự án `TMDT` bằng Android Studio.
2. Chờ Gradle đồng bộ và cài SDK theo yêu cầu của dự án nếu máy chưa có.
3. Chọn máy ảo hoặc thiết bị thật chạy Android 10 trở lên.
4. Chọn cấu hình `app` rồi nhấn **Run**.

Không cần tài khoản hoặc cấu hình máy chủ để thử giao diện. Lần build đầu tiên có thể cần kết nối Internet để tải Gradle và các thư viện.

### Tạo APK trên Windows

Chạy tại thư mục gốc dự án:

```powershell
.\gradlew.bat :app:assembleDebug
```

File APK được tạo tại:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 10. Kiểm thử

Biên dịch và chạy Android Lint:

```powershell
.\gradlew.bat :app:assembleDebug :app:lintDebug
```

Chạy kiểm thử giao diện khi đã kết nối thiết bị hoặc mở máy ảo:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

Các tình huống kiểm tra gồm tìm kiếm không dấu, trạng thái không có kết quả, đặt lịch, đổi thời lượng và tính phí, khôi phục Activity, lưu yêu cầu qua lần mở lại, địa chỉ bắt buộc khi học tại nhà và từ chối khung giờ trùng.

Bản giao diện hiện tại đã build thành công và vượt qua 6 kiểm thử trên máy ảo Android API 36.1. Android Lint không có lỗi; vẫn còn cảnh báo, chủ yếu liên quan đến tài nguyên, thư viện và cách khai báo giao diện.

Ảnh chụp bốn màn hình sau khi chạy thử được lưu tại `build/ui-previews/`. Đây là thư mục kết quả cục bộ, có thể bị xóa khi dọn build.

## 11. Hướng phát triển

- Xây dựng máy chủ và cơ sở dữ liệu để quản lý người dùng, gia sư và buổi học.
- Bổ sung đăng ký, đăng nhập và phân quyền học viên, gia sư, quản trị viên.
- Cho phép gia sư cập nhật hồ sơ, môn dạy, học phí và lịch rảnh.
- Hoàn thiện quy trình gửi, xác nhận, từ chối, hủy và đổi lịch học.
- Xử lý trùng lịch trên máy chủ khi nhiều người đặt cùng một khung giờ.
- Phát triển nhắn tin và thông báo nhắc lịch.
- Bổ sung thanh toán, lịch sử giao dịch và chính sách hủy/hoàn tiền.
- Cho phép đánh giá gia sư sau buổi học và quản trị nội dung đánh giá.
- Thay thế dữ liệu, ảnh mẫu và kiểm thử trên nhiều kích thước màn hình.

Giao diện hiện tại có thể tiếp tục được sử dụng và điều chỉnh khi phát triển các chức năng này, không cần xây dựng lại toàn bộ từ đầu.
