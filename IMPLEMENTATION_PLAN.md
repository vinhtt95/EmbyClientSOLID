# Kế hoạch Triển khai (Implementation Plan)
- [x] Giai đoạn 0: Thiết lập Nền tảng (Maven & Project Setup)
- [x] Giai đoạn 1: Lõi Trừu tượng (Interfaces & Models)
- [x] Giai đoạn 2: Triển khai Dịch vụ Cốt lõi & Phiên (Services & Session)
- [x] Giai đoạn 3: Trừu tượng hóa & Triển khai Tầng Dữ liệu (Data Layer)
- [x] Giai đoạn 4: Dịch vụ Tương tác & Thông báo (Core Services Pt. 2)
- [x] Giai đoạn 5: Dịch vụ Điều hướng & Hotkey (Core Services Pt. 3)
- [ ] Giai đoạn 6: Màn hình Đăng nhập (View + ViewModel)
- [ ] Giai đoạn 7: Màn hình Chính (Main View + ViewModel)
- [ ] Giai đoạn 8: Cột Thư viện (Tree View + VM)
- [ ] Giai đoạn 9: Cột Lưới Item (Grid View + VM)
- [ ] Giai đoạn 10: Cột Chi tiết (Detail View + VM)
- [ ] Giai đoạn 11: Dialog Thêm Tag (Dialog + VM)
- [ ] Giai đoạn 12: Hoàn thiện Phím tắt & Pop-out
- [ ] Giai đoạn 13: Hoàn thiện (Styling & I18n)

Dự án sẽ được xây dựng theo từng giai đoạn ("code lan"), đi từ Nền tảng (Core) -> Dữ liệu (Data) -> Trạng thái (ViewModel) -> Giao diện (View).

---
### Giai đoạn 0: Thiết lập Nền tảng (Maven & Project Setup)
* **Mục tiêu:** Tạo cấu trúc dự án Maven và định nghĩa tất cả các phụ thuộc.
* **File:** `pom.xml`, `Launcher.java`, `MainApp.java` (khung xương).
* **Kiểm tra:** Chạy `mvn clean package` và khởi chạy được một cửa sổ trống.

---
### Giai đoạn 1: Lõi Trừu tượng (Interfaces & Models)
* **Mục tiêu:** Định nghĩa các "hợp đồng" (interfaces) cốt lõi và các POJO (models) theo nguyên tắc **Dependency Inversion (D)**.
* **File (Package `model`):**
    * `Tag.java` (thay thế `TagModel`)
    * `ReleaseInfo.java` (thay thế `FetchDateResult`)
    * `GridNavigationState.java`
* **File (Package `core`):**
    * `IPreferenceService.java`
    * `IConfigurationService.java`
* **Kiểm tra:** Dự án biên dịch (compile) thành công.

---
### Giai đoạn 2: Triển khai Dịch vụ Cốt lõi & Phiên (Services & Session)
* **Mục tiêu:** Triển khai các dịch vụ cơ sở (đọc/ghi file) và logic quản lý Session, tách biệt khỏi "God Class" `EmbyService` cũ.
* **File (Package `services`):**
    * `JavaPreferenceService.java` (Implement `IPreferenceService`)
    * `JsonConfigurationService.java` (Implement `IConfigurationService`)
* **File (Package `core`):**
    * `IEmbySessionService.java` (Interface mới)
* **File (Package `session`):**
    * `EmbySessionService.java` (Implement `IEmbySessionService`, chứa logic login/logout/restore từ `EmbyService` cũ).
* **Kiểm tra:** Viết Unit Test cho `EmbySessionService` (dùng Mock `IPreferenceService`).

---
### Giai đoạn 3: Trừu tượng hóa & Triển khai Tầng Dữ liệu (Data Layer)
* **Mục tiêu:** Định nghĩa và triển khai toàn bộ logic truy cập dữ liệu (Đọc/Ghi) theo nguyên tắc **Single Responsibility (S)** và **CQRS**.
* **File (Package `data` - Interfaces):**
    * `IItemRepository.java` (Đọc Item)
    * `IStaticDataRepository.java` (Đọc gợi ý Tags/Studios...)
    * `IExternalDataService.java` (Đọc API ngoài cho UR-38)
    * `IItemUpdateService.java` (Ghi Item, Upload Ảnh, Clone)
* **File (Package `data.impl` - Implementations):**
    * `EmbyItemRepository.java` (Logic từ `ItemRepository` cũ)
    * `EmbyStaticDataRepository.java` (Logic từ `RequestEmby` cũ)
    * `ExternalMovieDataService.java` (Logic `getDateRelease` từ `RequestEmby` cũ)
    * `EmbyItemUpdateService.java` (Logic `copyTags`, `ItemDetailSaver`, `ItemImageUpdater` cũ)
* **Kiểm tra:** Viết Unit Test cho các lớp Impl (dùng Mock `IEmbySessionService`).

---
### Giai đoạn 4: Dịch vụ Tương tác & Thông báo (Core Services Pt. 2)
* **Mục tiêu:** Trừu tượng hóa các hành động tương tác với máy người dùng và hệ thống thông báo.
* **File (Package `core`):**
    * `ILocalInteractionService.java` (Interface: `openFile`, `openFolder`, `openSubtitleFile`)
    * `INotificationService.java` (Interface: `showStatus`, `showConfirmation`)
* **File (Package `services`):**
    * `DesktopInteractionService.java` (Implement `ILocalInteractionService`, chứa logic tạo file `.srt` từ `ItemDetailController` cũ).
    * `NotificationService.java` (Implement `INotificationService`, chứa `StringProperty` cho status bar).
* **Kiểm tra:** Viết Unit Test cho logic tạo file `.srt` (UR-40).

---
### Giai đoạn 5: Dịch vụ Điều hướng & Hotkey (Core Services Pt. 3)
* **Mục tiêu:** Hoàn thiện hạ tầng services (Quản lý `Stage`/`Scene`). (Đã loại bỏ Global Hotkey theo yêu cầu).
* **File (Package `core`):**
    * `IAppNavigator.java` (Interface: `showLogin`, `showMain`, `showAddTagDialog`)
* **File (Package `navigation`):**
    * `AppNavigator.java` (Implement `IAppNavigator`, chứa logic `FXMLLoader.load()`).
* **Kiểm tra:** Hoàn tất toàn bộ tầng Services.

---
### Giai đoạn 6: Màn hình Đăng nhập (View + ViewModel)
* **Mục tiêu:** Xây dựng hoàn chỉnh chức năng đăng nhập (UR-1, UR-2, UR-3).
* **File:**
    * `viewmodel.ILoginViewModel.java` (Interface)
    * `viewmodel.impl.LoginViewModel.java` (Logic từ `LoginViewModel` cũ, gọi `IEmbySessionService` và `IAppNavigator`).
    * `controller.LoginController.java` (Chỉ binding và gọi command).
    * `MainApp.java` (Cập nhật để thiết lập "Dependency Injection" cho `LoginViewModel`).
* **Kiểm tra:** Khởi chạy app, đăng nhập thành công.

---
### Giai đoạn 7: Màn hình Chính (Main View + ViewModel)
* **Mục tiêu:** Xây dựng bộ khung 3 cột, status bar, toolbar, và logic điều phối (coordinator) chính.
* **File:**
    * `viewmodel.impl.MainViewModel.java` (VM điều phối: Quản lý trạng thái chung như `searchKeyword`, `sortBy`, `sortOrder`).
    * `controller.MainController.java` (View-Coordinator: Tải 3 FXML con, tiêm ViewModel cho chúng, và kết nối các VM con với nhau, ví dụ: `GridVM.selectedItem -> DetailVM.loadItem`).
    * `MainApp.java` (Cập nhật `showMain` để tiêm (inject) tất cả các Services và ViewModels vào `MainController`).
* **Kiểm tra:** Khởi chạy app, đăng nhập -> Màn hình chính 3 cột (trống) xuất hiện, status bar hoạt động, nút Logout (UR-5), Home (UR-10) hoạt động.

---
### Giai đoạn 8: Cột Thư viện (Tree View + VM)
* **Mục tiêu:** Hoàn thiện Cột 1 (UR-15, 16, 17, 18).
* **File:**
    * `viewmodel.ILibraryTreeViewModel.java` (Interface)
    * `viewmodel.impl.LibraryTreeViewModel.java` (Logic từ `LibraryTreeViewModel` cũ, gọi `IItemRepository`).
    * `controller.LibraryTreeController.java` (Chỉ binding `treeView` và Context Menu (UR-18)).
* **Kiểm tra:** Cây thư viện hiển thị, lazy loading hoạt động. Nhấp vào một thư mục (chưa có gì xảy ra ở Cột 2).

---
### Giai đoạn 9: Cột Lưới Item (Grid View + VM)
* **Mục tiêu:** Hoàn thiện Cột 2 (UR-19 đến UR-29).
* **File:**
    * `viewmodel.IItemGridViewModel.java` (Interface)
    * `viewmodel.impl.ItemGridViewModel.java` (Logic từ `ItemGridViewModel` cũ, quản lý `itemsProperty`, stack Lịch sử (UR-29), và logic phân trang).
    * `controller.ItemGridController.java` (Chỉ binding, tạo `ItemCell`, xử lý cuộn chuột (UR-24) và nhấp đúp (UR-27)).
* **Kiểm tra:** Nhấp vào thư mục (Cột 1) -> Cột 2 hiển thị lưới. Sắp xếp, Tìm kiếm, Lùi/Tiến (Back/Forward) hoạt động.

---
### Giai đoạn 10: Cột Chi tiết (Detail View + VM)
* **Mục tiêu:** Hoàn thiện Cột 3 (UR-30 đến UR-50). Đây là giai đoạn lớn nhất.
* **File:**
    * `viewmodel.IItemDetailViewModel.java` (Interface: định nghĩa tất cả Trạng thái và Hành động).
    * `viewmodel.impl.ItemDetailViewModel.java` (Logic chính từ `ItemDetailViewModel` cũ, nhưng *ủy thác* (delegate) logic nghiệp vụ cho các Services đã được tiêm (inject)).
        * `saveCommand()` -> gọi `IItemUpdateService.updateItem()`.
        * `fetchReleaseDateCommand()` -> gọi `IExternalDataService.fetchReleaseInfo()` và `IItemUpdateService.updateItem()`.
        * `openSubtitleCommand()` -> gọi `ILocalInteractionService.openSubtitleFile()`.
        * `cloneCommand()` -> gọi `IItemUpdateService.cloneProperties()`.
    * `controller.ItemDetailController.java` (Lớp View "ngu ngốc", chỉ binding và gọi command).
    * `view.controls.TagChip.java` & `BackdropChip.java` (Các custom control cho UI).
* **Kiểm tra:** Chọn item (Cột 2) -> Cột 3 hiển thị chi tiết. Tất cả các nút (Lưu, Import, Export, Fetch, Clone, Add/Delete Tag, Play, Subtitle, Screenshot...) hoạt động.

---
### Giai đoạn 11: Dialog Thêm Tag (Dialog + VM)
* **Mục tiêu:** Hoàn thiện dialog Add Tag/Studio/People/Genre (UR-35).
* **File:**
    * `viewmodel.IAddTagViewModel.java` (Interface)
    * `viewmodel.impl.AddTagViewModel.java` (Quản lý trạng thái phức tạp của dialog: JSON/Simple, gợi ý, lọc).
    * `controller.AddTagDialogController.java` (Binding vào `AddTagViewModel`).
    * `navigation.AppNavigator.java` (Cập nhật `showAddTagDialog` để khởi tạo và hiển thị).
* **Kiểm tra:** Nhấn nút "+" ở Cột 3 -> Dialog xuất hiện, gợi ý hoạt động, điều hướng phím hoạt động, copy-by-id hoạt động.

---
### Giai đoạn 12: Hoàn thiện Phím tắt & Pop-out
* **Mục tiêu:** Kết nối các phím tắt *trong ứng dụng* và cửa sổ pop-out (UR-13, UR-14, UR-50). (Đã loại bỏ UR-12).
* **File:**
    * `MainApp.java` (Cập nhật: Đăng ký phím tắt *trong ứng dụng*).
    * `controller.MainController.java` (Cập nhật: Đăng ký phím tắt (Cmd+S, Cmd+N...) và điều hướng chuột Back/Forward (UR-14)).
    * `navigation.AppNavigator.java` (Cập nhật: Triển khai `showPopOutDetail()`).
* **Kiểm tra:** Tất cả các phím tắt trong ứng dụng và cửa sổ pop-out hoạt động như mong đợi.

---
### Giai đoạn 13: Hoàn thiện (Styling & I18n)
* **Mục tiêu:** Sao chép và rà soát lại các tài nguyên tĩnh.
* **File:**
    * `resources/com/example/embyapp/styles.css`
    * `resources/com/example/embyapp/config.json`
* **Kiểm tra:** Ứng dụng có giao diện hoàn chỉnh và hỗ trợ đa ngôn ngữ (I18n) đầy đủ.