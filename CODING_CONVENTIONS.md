# Quy ước Code (Coding Conventions)

Tài liệu này định nghĩa các tiêu chuẩn và quy ước code cho dự án EmbyClientJavaFX (Refactored).

## 1. Quy tắc Đặt tên (Naming)

### 1.1. Packages (Gói)
* Tên gói dùng chữ thường, không có gạch dưới. (Ví dụ: `com.example.embyapp.viewmodel`).
* Gói `impl` được dùng để chứa các lớp triển khai (implementation) của các interface.
* Gói `core` chỉ chứa các interface cốt lõi.

### 1.2. Classes (Lớp)
* Dùng `PascalCase` (ví dụ: `ItemDetailViewModel`).
* **Interfaces:** Phải bắt đầu bằng chữ `I` (ví dụ: `IItemRepository`).
* **Implementations:** Tên lớp triển khai nên mô tả cách nó thực hiện interface (ví dụ: `IItemRepository` -> `EmbyItemRepository`).
* **ViewModels:** Tên phải kết thúc bằng `ViewModel` (ví dụ: `LoginViewModel`).
* **Controllers:** Tên phải kết thúc bằng `Controller` (ví dụ: `LoginController`).
* **Services/Repositories:** Tên phải mô tả rõ trách nhiệm (ví dụ: `EmbySessionService`, `IItemUpdateService`).

### 1.3. Methods (Phương thức)
* Dùng `camelCase` (ví dụ: `loadChildrenForItem`).
* **ViewModel Actions/Commands:** Các phương thức được gọi từ View (Controller) nên kết thúc bằng `Command` để rõ ràng (ví dụ: `saveCommand()`, `loginCommand()`).
* **Event Handlers (Controller):** Các phương thức xử lý sự kiện FXML nên bắt đầu bằng `handle...` (ví dụ: `handleSaveButtonAction()`). (Quy ước này ít quan trọng hơn khi dùng MVVM, vì logic đã chuyển sang `...Command`).

### 1.4. Variables (Biến)
* Dùng `camelCase` (ví dụ: `currentAuthResult`).
* **Constants:** Dùng `UPPER_SNAKE_CASE` (ví dụ: `PREF_NODE_PATH`).
* **FXML Fields:** Các biến được FXML tiêm (@FXML) phải trùng tên với `fx:id` trong FXML (ví dụ: `fx:id="saveButton"` -> `@FXML private Button saveButton;`).
* **JavaFX Properties:** Các biến là JavaFX Property trong ViewModel phải trùng tên với đối tượng nó đại diện (ví dụ: `title` cho `StringProperty`, `items` cho `ObservableList`).

## 2. Nguyên tắc MVVM

1.  **View (Controller & FXML):**
    * **"Ngu ngốc" (Dumb):** View *không* được chứa logic nghiệp vụ.
    * **Không tự ý khởi tạo:** View *không* được tự tạo ViewModel, Service, hay Repository. Chúng phải được tiêm (inject) vào (ví dụ: qua hàm `setViewModel(I...ViewModel vm)`).
    * **Chỉ Binding & Command:** View chỉ được phép:
        1.  **Binding:** `myTextField.textProperty().bind(vm.myTextProperty())`.
        2.  **Command:** `myButton.setOnAction(e -> vm.myCommand())`.

2.  **ViewModel:**
    * **Quản lý Trạng thái:** Chịu trách nhiệm quản lý trạng thái của View thông qua JavaFX Properties.
    * **Không biết về View:** ViewModel *không* được import bất kỳ lớp `javafx.scene.control.*` nào (ví dụ: `TextField`, `Button`). Nó chỉ biết về `StringProperty`, `BooleanProperty`, `ObservableList`.
    * **Dependency Injection:** ViewModel nhận các Services/Repositories (ví dụ: `IItemRepository`) qua constructor của nó.
    * **Không logic nghiệp vụ nặng:** Nếu một hành động (Command) quá phức tạp (ví dụ: `saveCommand()` cần parse, gọi API, xử lý lỗi), ViewModel nên *ủy thác* (delegate) hành động đó cho một Service (ví dụ: `IItemUpdateService.updateItem(...)`).

## 3. Nguyên tắc SOLID

1.  **Single Responsibility (S):** Mỗi lớp chỉ làm một việc.
    * `EmbySessionService`: Chỉ lo việc login/logout/session.
    * `EmbyItemRepository`: Chỉ lo việc ĐỌC item.
    * `EmbyItemUpdateService`: Chỉ lo việc GHI item.
    * `DesktopInteractionService`: Chỉ lo việc tương tác với Desktop.
2.  **Dependency Inversion (D):** Luôn code dựa trên **Interfaces (Trừu tượng)**, không phải **Implementations (Cụ thể)**.
    * **ĐÚNG (ViewModel):** `private final IItemRepository itemRepository;`
    * **SAI (ViewModel):** `private final EmbyItemRepository itemRepository;`

## 4. JavaFX & FXML

* **`fx:id`:** Tất cả các thành phần FXML cần tương tác trong Controller phải có `fx:id`.
* **Binding > Set:** Ưu tiên dùng `myLabel.visibleProperty().bind(vm.isLoadingProperty())` hơn là dùng listener để gọi `myLabel.setVisible(value)`. Binding giúp code sạch sẽ và tự động hơn.
* **`Platform.runLater()`:** Chỉ sử dụng trong các tác vụ nền (ví dụ: `new Thread(...)`) khi cần cập nhật UI. Không lạm dụng nó trong các luồng JavaFX thông thường.