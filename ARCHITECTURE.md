# Kiến trúc Dự án (SOLID & MVVM)

Dự án này là một bản tái cấu trúc (refactor) của `EmbyClientJavaFX` gốc, được thiết kế để tuân thủ chặt chẽ các nguyên tắc SOLID và mô hình MVVM.

## 1. Vấn đề của Kiến trúc Cũ

Kiến trúc gốc vi phạm các nguyên tắc thiết kế phần mềm cơ bản:

1.  **Vi phạm Single Responsibility Principle (SRP):**
    * `EmbyService` là một "God Class" xử lý mọi thứ: xác thực, quản lý session, truy vấn dữ liệu (tags, studios...), và lưu trữ preferences.
    * `ItemDetailViewModel` quá lớn, chứa logic nghiệp vụ nặng về tải, lưu, import, và theo dõi thay đổi, thay vì chỉ quản lý trạng thái UI.
    * `Controller` (View) chứa logic nghiệp vụ, tự ý gọi services và repositories (ví dụ: `ItemGridController.playItem`), vi phạm ranh giới của MVVM.
2.  **Vi phạm Dependency Inversion Principle (DIP):**
    * Các lớp cấp cao (ViewModels, Controllers) phụ thuộc trực tiếp vào các lớp triển khai cấp thấp (concrete class) như `EmbyService`, `ItemRepository`.
    * Việc lạm dụng Singleton (`.getInstance()`) tạo ra các phụ thuộc ẩn, khiến việc kiểm thử (testing) và bảo trì trở nên bất khả thi.

## 2. Kiến trúc MVVM-SOLID Mới

Kiến trúc mới tách biệt rõ ràng các mối quan tâm (concerns) bằng cách sử dụng **Dependency Injection (DI)** và **Interfaces** (Sự trừu tượng).

* **View (Controllers & FXML):** Tầng "ngu ngốc" (dumb), chỉ chịu trách nhiệm hiển thị UI, binding (liên kết) dữ liệu với ViewModel, và gọi các "Commands" (hành động) từ ViewModel.
* **ViewModel:** Tầng quản lý **Trạng thái (State)** của View. Nó không biết gì về FXML. Nó nhận lệnh từ View, gọi các Services/Repositories để thực thi logic, và cập nhật trạng thái (State). View sẽ tự động phản ứng với các thay đổi trạng thái này.
* **Model (Services & Data):** Tầng logic nghiệp vụ và truy cập dữ liệu. Được trừu tượng hóa hoàn toàn qua các Interfaces.

### Cấu trúc Thư mục
`com.example.embyapp
├── MainApp.java                    // (Config DI, khởi chạy App)
├── Launcher.java                   // (Hỗ trợ Fat JAR)
│
├── core                            // (Interfaces - Trừu tượng hóa)
│   ├── IAppNavigator.java
│   ├── IConfigurationService.java
│   ├── IEmbySessionService.java
│   ├── ILocalInteractionService.java
│   ├── INotificationService.java
│   └── IPreferenceService.java
│
├── data                            // (Data Layer - Abstractions & Impls)
│   ├── IExternalDataService.java   // (Interface cho API bên ngoài)
│   ├── IItemRepository.java
│   ├── IItemUpdateService.java
│   ├── IStaticDataRepository.java
│   ├── impl
│   │   ├── EmbyItemRepository.java // (Implementations)
│   │   ├── EmbyItemUpdateService.java
│   │   ├── EmbyStaticDataRepository.java
│   │   └── ExternalMovieDataService.java
│
├── model                           // (Domain Models - POJOs)
│   ├── GridNavigationState.java
│   ├── ReleaseInfo.java            // (Thay thế FetchDateResult)
│   └── Tag.java                    // (Thay thế TagModel)
│
├── navigation                      // (Quản lý Stage/Scene)
│   └── AppNavigator.java
│
├── services                        // (Implementations của Core Services)
│   ├── DesktopInteractionService.java
│   ├── JNativeHookHotkeyService.java
│   ├── JavaPreferenceService.java
│   ├── JsonConfigurationService.java
│   └── NotificationService.java    // (Quản lý status bar)
│
├── session                         // (Tách biệt logic session)
│   └── EmbySessionService.java
│
├── view                            // (FXML & Custom Controls)
│   ├── fxml
│   │   ├── AddTagDialog.fxml
│   │   ├── ItemDetailView.fxml
│   │   ├── ...
│   └── controls
│       ├── BackdropChip.java       // (Thay thế BackdropView)
│       └── TagChip.java            // (Thay thế TagView)
│
├── viewmodel                       // (ViewModels)
│   ├── IAddTagViewModel.java
│   ├── IItemDetailViewModel.java
│   ├── IItemGridViewModel.java
│   ├── ILibraryTreeViewModel.java
│   ├── ILoginViewModel.java
│   ├── impl
│   │   ├── AddTagViewModel.java    // (Implementations)
│   │   ├── ItemDetailViewModel.java
│   │   ├── ItemGridViewModel.java
│   │   ├── LibraryTreeViewModel.java
│   │   ├── LoginViewModel.java
│   │   └── MainViewModel.java      // (VM điều phối chính)
│
└── controller                      // (View Layer - Controllers)
├── AddTagDialogController.java
├── ItemDetailController.java
├── ItemGridController.java
├── LibraryTreeController.java
├── LoginController.java
└── MainController.java`

### 3. Vai trò của Từng Gói

* **`core` (Interfaces):** Trái tim của **Dependency Inversion (D)**. Định nghĩa các "hợp đồng" cho toàn bộ ứng dụng. ViewModels sẽ chỉ phụ thuộc vào các interface này.
    * `IAppNavigator`: Điều hướng giữa các Scene (ví dụ: `showLogin()`, `showMain()`).
    * `IEmbySessionService`: Chỉ quản lý session (ví dụ: `login()`, `logout()`, `getApiClient()`).
    * `IConfigurationService`: Chỉ đọc `config.json`.
    * `IPreferenceService`: Đọc/ghi `java.util.prefs` (cài đặt người dùng).
    * `ILocalInteractionService`: Tương tác cục bộ (ví dụ: `openFile()`, `openFolder()`).
    * `IGlobalHotkeyService`: Lắng nghe phím tắt toàn hệ thống.
    * `INotificationService`: Gửi thông báo đến Status Bar.

* **`model` (POJOs):** Các đối tượng dữ liệu (Data Transfer Objects) đơn giản, không chứa logic, dùng để truyền tải thông tin.

* **`services` & `session` (Implementations):** Các lớp triển khai (implement) các interface trong `core`. Chúng chứa logic "thật". Ví dụ:
    * `EmbySessionService` triển khai `IEmbySessionService`, quản lý `ApiClient` và `AuthResult`.
    * `DesktopInteractionService` triển khai `ILocalInteractionService`, chứa logic `Desktop.getDesktop().open()` và logic tạo file `.srt`.
    * Tuân thủ **Single Responsibility (S)**: Mỗi service chỉ làm một việc.

* **`data` (Data Layer):** Tuân thủ **Single Responsibility (S)** và **Interface Segregation (I)**. Gói này chịu trách nhiệm hoàn toàn về việc truy cập dữ liệu.
    * `IItemRepository` / `IStaticDataRepository` / `IExternalDataService`: Các interface chỉ dành cho việc **ĐỌC (Query)** dữ liệu.
    * `IItemUpdateService`: Interface chỉ dành cho việc **GHI (Command)** dữ liệu (ví dụ: `updateItem`, `uploadImage`).
    * `impl` (Package): Chứa các lớp triển khai, sử dụng `ApiClient` từ `IEmbySessionService` để gọi Emby API.

* **`viewmodel` (ViewModels):** Tầng logic của UI.
    * Chúng được *tiêm* (inject) các services (ví dụ: `IItemRepository`, `IItemUpdateService`, `ILocalInteractionService`).
    * Chúng phơi bày (expose) trạng thái qua `JavaFX Properties` (ví dụ: `StringProperty`, `ObservableList`).
    * Chúng phơi bày hành động qua các phương thức `...Command()` (ví dụ: `saveCommand()`).
    * Chúng không biết gì về `javafx.scene.control` (ví dụ: không biết `TextField` là gì).

* **`controller` (View):** Tầng UI "ngu ngốc".
    * Nhận ViewModel qua hàm `setViewModel(I...ViewModel vm)`.
    * Chỉ thực hiện 2 việc: **Binding** (ví dụ: `myTextField.textProperty().bind(vm.myTextProperty())`) và **Command** (ví dụ: `myButton.setOnAction(e -> vm.myCommand())`).
    * `MainController` đóng vai trò đặc biệt là **View-Coordinator**, chịu trách nhiệm kết nối các ViewModel con lại với nhau (ví dụ: khi `selectedItem` trong `GridVM` thay đổi, nó gọi `detailVM.loadItem(...)`).