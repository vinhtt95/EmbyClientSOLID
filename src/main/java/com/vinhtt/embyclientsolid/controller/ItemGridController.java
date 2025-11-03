package com.vinhtt.embyclientsolid.controller;

import com.vinhtt.embyclientsolid.core.IConfigurationService;
import com.vinhtt.embyclientsolid.core.INotificationService;
import com.vinhtt.embyclientsolid.model.GridNavigationState;
import com.vinhtt.embyclientsolid.viewmodel.IItemGridViewModel;
import embyclient.model.BaseItemDto;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

import java.util.Map;

/**
 * Controller (View) cho ItemGridView.fxml (Cột 2).
 * Lớp này chỉ chịu trách nhiệm binding UI components với ItemGridViewModel,
 * tạo các cell (ô) item, và ủy thác (delegate) các hành động (cuộn, click) cho ViewModel.
 */
public class ItemGridController {

    // --- FXML UI Components ---
    @FXML private StackPane rootPane;
    @FXML private ScrollPane gridScrollPane;
    @FXML private FlowPane itemFlowPane;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Label statusLabel;
    @FXML private VBox statusContainer; // Container cho (Loading + Status Label)

    private IItemGridViewModel viewModel;
    private INotificationService notificationService;
    private IConfigurationService configService;

    // Cờ để bỏ qua sự kiện cuộn do code gây ra (ví dụ: khi chuyển trang)
    private boolean ignoreNextScrollEvent = false;

    // Kích thước cố định cho cell (UR-25)
    private static final double CELL_HEIGHT = 320;
    private static final double CELL_WIDTH = CELL_HEIGHT * 16 / 9;
    private static final double IMAGE_HEIGHT = CELL_HEIGHT;

    /**
     * Khởi tạo Controller.
     */
    public ItemGridController() {
    }

    @FXML
    public void initialize() {
        // Cấu hình layout cơ bản cho FlowPane
        itemFlowPane.setPadding(new Insets(20));
        itemFlowPane.setHgap(20);
        itemFlowPane.setVgap(20);
        itemFlowPane.setAlignment(Pos.CENTER);
        gridScrollPane.setFitToWidth(true);
    }

    /**
     * Tiêm (inject) ViewModel và Services vào Controller.
     * Đây là phương thức khởi tạo chính, được gọi bởi MainController.
     *
     * @param viewModel ViewModel chứa logic và trạng thái của Cột 2.
     * @param notificationService Service thông báo (dùng cho dialog xác nhận).
     * @param configService Service để lấy chuỗi I18n.
     */
    public void setViewModel(IItemGridViewModel viewModel, INotificationService notificationService, IConfigurationService configService) {
        this.viewModel = viewModel;
        this.notificationService = notificationService;
        this.configService = configService;

        // 1. Binding UI
        // Liên kết indicator loading
        loadingIndicator.visibleProperty().bind(viewModel.loadingProperty());
        // Liên kết label trạng thái (ví dụ: "Thư viện rỗng")
        statusLabel.textProperty().bind(viewModel.statusMessageProperty());

        // Hiển thị VBox (loading + status) khi đang tải HOẶC khi VM yêu cầu (ví dụ: thư viện rỗng)
        statusContainer.visibleProperty().bind(viewModel.loadingProperty().or(viewModel.showStatusMessageProperty()));
        statusContainer.managedProperty().bind(statusContainer.visibleProperty());

        // Ẩn Grid (ScrollPane) khi status/loading hiển thị
        gridScrollPane.visibleProperty().bind(statusContainer.visibleProperty().not());
        gridScrollPane.managedProperty().bind(gridScrollPane.visibleProperty());

        // 2. Lắng nghe thay đổi danh sách items từ VM
        // Khi danh sách items trong VM thay đổi, vẽ lại toàn bộ lưới
        viewModel.getItems().addListener((ListChangeListener<BaseItemDto>) c -> {
            Platform.runLater(this::updateItemGrid);
        });

        // 3. Lắng nghe thay đổi item được chọn từ VM
        // Khi item được chọn thay đổi, cập nhật style CSS
        viewModel.selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            Platform.runLater(() -> updateCellSelection(oldItem, newItem));
        });

        // 4. Lắng nghe yêu cầu cuộn (Scroll Action) từ VM
        // Khi VM load trang mới (LoadNext/Prev), nó sẽ bắn sự kiện yêu cầu cuộn
        viewModel.scrollActionProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == GridNavigationState.ScrollAction.SCROLL_TO_TOP) {
                // Đặt cờ để bỏ qua sự kiện vvalueProperty()
                ignoreNextScrollEvent = true;
                Platform.runLater(() -> {
                    gridScrollPane.setVvalue(0.0); // Cuộn lên đầu
                    // Reset cờ sau khi cuộn
                    Platform.runLater(() -> ignoreNextScrollEvent = false);
                });
            } else if (newVal == GridNavigationState.ScrollAction.SCROLL_TO_BOTTOM) {
                ignoreNextScrollEvent = true;
                Platform.runLater(() -> {
                    gridScrollPane.setVvalue(1.0); // Cuộn xuống cuối
                    Platform.runLater(() -> ignoreNextScrollEvent = false);
                });
            }
        });

        // 5. Cài đặt listener cuộn chuột (UR-24)
        gridScrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> {
            if (viewModel == null || viewModel.loadingProperty().get()) return;
            if (ignoreNextScrollEvent) {
                // Bỏ qua sự kiện này nếu nó được kích hoạt bởi code (mục 4)
                ignoreNextScrollEvent = false;
                return;
            }

            // (UR-24: Cuộn xuống) Khi cuộn đến gần cuối (95%)
            if (newVal.doubleValue() > 0.95 && oldVal.doubleValue() < 0.95) {
                if (viewModel.hasNextPageProperty().get()) {
                    // Hiển thị dialog xác nhận
                    if (showConfirmationDialog(
                            configService.getString("itemGridView", "confirmDialogTitle"),
                            configService.getString("itemGridView", "confirmNextPage")
                    )) {
                        // Nếu đồng ý, gọi command trong VM
                        viewModel.loadNextPage();
                    }
                }
            }
            // (UR-24: Cuộn lên) Khi cuộn đến gần đầu (5%)
            else if (newVal.doubleValue() < 0.05 && oldVal.doubleValue() > 0.05) {
                if (viewModel.hasPreviousPageProperty().get()) {
                    if (showConfirmationDialog(
                            configService.getString("itemGridView", "confirmDialogTitle"),
                            configService.getString("itemGridView", "confirmPrevPage")
                    )) {
                        viewModel.loadPreviousPage();
                    }
                }
            }
        });
    }

    /**
     * Hiển thị dialog xác nhận (Yes/No) (UR-24).
     *
     * @param title Tiêu đề dialog.
     * @param content Nội dung câu hỏi.
     * @return true nếu người dùng chọn Yes, false nếu ngược lại.
     */
    private boolean showConfirmationDialog(String title, String content) {
        if (notificationService == null) {
            return true; // Tự động đồng ý nếu service lỗi
        }
        // Ủy thác cho NotificationService
        return notificationService.showConfirmation(
                configService.getString("itemGridView", "confirmDialogTitle"),
                configService.getString("itemGridView", "confirmNextPage")
        );
    }

    /**
     * Vẽ lại toàn bộ lưới item.
     * Được gọi khi viewModel.getItems() thay đổi.
     */
    private void updateItemGrid() {
        itemFlowPane.getChildren().clear();
        String selectedId = (viewModel.selectedItemProperty().get() != null)
                ? viewModel.selectedItemProperty().get().getId() : null;

        // Tạo lại tất cả các cell
        for (BaseItemDto item : viewModel.getItems()) {
            StackPane cell = createItemCell(item);
            // Áp dụng style "selected" nếu item này đang được chọn
            if (selectedId != null && item.getId() != null && item.getId().equals(selectedId)) {
                cell.getStyleClass().add("item-cell-selected");
            }
            itemFlowPane.getChildren().add(cell);
        }
    }

    /**
     * Cập nhật CSS cho cell được chọn/bỏ chọn (UR-26).
     * Tối ưu hơn updateItemGrid(), chỉ thay đổi CSS thay vì vẽ lại.
     *
     * @param oldItem Item cũ (cần xóa style).
     * @param newItem Item mới (cần thêm style).
     */
    private void updateCellSelection(BaseItemDto oldItem, BaseItemDto newItem) {
        String oldId = (oldItem != null) ? oldItem.getId() : null;
        String newId = (newItem != null) ? newItem.getId() : null;

        // Duyệt qua các cell đang hiển thị
        for (Node node : itemFlowPane.getChildren()) {
            if (node.getUserData() instanceof BaseItemDto) {
                String cellItemId = ((BaseItemDto) node.getUserData()).getId();
                if (cellItemId == null) continue;

                // Xóa style khỏi cell cũ
                if (cellItemId.equals(oldId)) {
                    node.getStyleClass().remove("item-cell-selected");
                }
                // Thêm style cho cell mới
                if (cellItemId.equals(newId)) {
                    if (!node.getStyleClass().contains("item-cell-selected")) {
                        node.getStyleClass().add("item-cell-selected");
                    }
                }
            }
        }
    }

    /**
     * Tạo một ô (cell) đại diện cho item (UR-25, 26, 27, 28).
     *
     * @param item DTO của item cần hiển thị.
     * @return Một StackPane chứa ảnh, tiêu đề, và badge.
     */
    private StackPane createItemCell(BaseItemDto item) {
        // Container chính của cell
        StackPane cellContainer = new StackPane();
        cellContainer.setPrefSize(CELL_WIDTH, CELL_HEIGHT);
        cellContainer.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        cellContainer.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        cellContainer.getStyleClass().add("item-cell");
        // Lưu DTO vào cell để dùng khi click hoặc update style
        cellContainer.setUserData(item);

        // Ảnh thumbnail
        ImageView imageView = new ImageView();
        imageView.setFitWidth(CELL_WIDTH);
        imageView.setFitHeight(IMAGE_HEIGHT);
        imageView.setPreserveRatio(false);
        imageView.setSmooth(true);
        imageView.getStyleClass().add("item-image");

        // (UR-25: Thumbnail) Lấy URL ảnh từ ViewModel
        String imageUrl = getImageUrl(item);

        Image image = new Image(imageUrl, true); // true = tải nền
        imageView.setImage(image);

        // Lớp phủ (overlay) chứa tiêu đề
        VBox overlay = new VBox();
        overlay.setAlignment(Pos.BOTTOM_LEFT);
        overlay.setPadding(new Insets(8));
        overlay.getStyleClass().add("item-title-overlay");
        Label titleLabel = new Label(item.getName());
        titleLabel.setWrapText(true);
        titleLabel.setTextAlignment(TextAlignment.LEFT);
        titleLabel.getStyleClass().add("item-title-label");
        overlay.getChildren().add(titleLabel);

        cellContainer.getChildren().addAll(imageView, overlay);

        // (UR-25: Badge) Hiển thị Critic Rating
        Float criticRating = item.getCriticRating();
        if (criticRating != null && criticRating > 0) {
            Label ratingLabel = new Label(String.format("%.0f", criticRating));
            ratingLabel.getStyleClass().add("parental-rating-badge"); // Style CSS
            StackPane.setAlignment(ratingLabel, Pos.TOP_RIGHT);
            StackPane.setMargin(ratingLabel, new Insets(8, 8, 8, 8));
            cellContainer.getChildren().add(ratingLabel);
        }

        // (UR-26: Click chọn)
        cellContainer.setOnMouseClicked(event -> {
            // Báo cho VM biết item nào được chọn
            viewModel.selectedItemProperty().set(item);
            // (UR-27: Double-click)
            if (event.getClickCount() == 2) {
                // Gọi command Play trong VM
                viewModel.playItemCommand(item);
            }
        });

        // (UR-28: Copy ID)
        final ContextMenu contextMenu = new ContextMenu();
        final MenuItem copyIdItem = new MenuItem(configService.getString("contextMenu", "copyId"));
        copyIdItem.setOnAction(e -> {
            if (item.getId() != null) {
                final Clipboard clipboard = Clipboard.getSystemClipboard();
                final ClipboardContent content = new ClipboardContent();
                content.putString(item.getId());
                clipboard.setContent(content);
            }
        });
        contextMenu.getItems().add(copyIdItem);
        cellContainer.setOnContextMenuRequested(event -> {
            contextMenu.show(cellContainer, event.getScreenX(), event.getScreenY());
            event.consume();
        });

        return cellContainer;
    }

    /**
     * Helper lấy URL ảnh, gọi vào ViewModel.
     * Nếu VM không trả về URL, dùng ảnh placeholder.
     *
     * @param item DTO của item.
     * @return URL ảnh (Emby hoặc placeholder).
     */
    private String getImageUrl(BaseItemDto item) {
        if (viewModel != null) {
            String url = viewModel.getPrimaryImageUrl(item);
            if (url != null) {
                return url;
            }
        }
        // Fallback (ảnh placeholder)
        String placeholderText = (item.getType() != null ? item.getType() : configService.getString("itemGridView", "placeholderItem"));
        return "https://placehold.co/" + (int)CELL_WIDTH + "x" + (int)IMAGE_HEIGHT + "/333/999?text=" + placeholderText;
    }

    /**
     * Xử lý hotkey CMD+ENTER (UR-13) từ AppNavigator/MainController.
     * Ủy thác cho ViewModel.
     */
    public void playSelectedItem() {
        if (viewModel != null) {
            BaseItemDto selectedItem = viewModel.selectedItemProperty().get();
            if (selectedItem != null) {
                viewModel.playItemCommand(selectedItem);
            }
        }
    }
}