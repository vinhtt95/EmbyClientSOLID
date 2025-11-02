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
 * Controller cho ItemGridView.fxml (Cột 2).
 * Là một View "ngu ngốc".
 * (UR-19 đến UR-29).
 *
 * (Cập nhật GĐ 9: Sửa lỗi ảnh)
 */
public class ItemGridController {

    @FXML private StackPane rootPane;
    @FXML private ScrollPane gridScrollPane;
    @FXML private FlowPane itemFlowPane;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Label statusLabel;
    @FXML private VBox statusContainer; // (Thêm container)

    private IItemGridViewModel viewModel;
    private INotificationService notificationService;
    private IConfigurationService configService;

    private boolean ignoreNextScrollEvent = false;

    // Kích thước cell (UR-25)
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
        itemFlowPane.setPadding(new Insets(20));
        itemFlowPane.setHgap(20);
        itemFlowPane.setVgap(20);
        itemFlowPane.setAlignment(Pos.CENTER);
        gridScrollPane.setFitToWidth(true);

        // (Listener cuộn chuột sẽ được thêm trong setViewModel khi có notificationService)
    }

    /**
     * Được gọi bởi MainController (View-Coordinator) để tiêm ViewModel và Services.
     */
    public void setViewModel(IItemGridViewModel viewModel, INotificationService notificationService, IConfigurationService configService) {
        this.viewModel = viewModel;
        this.notificationService = notificationService;
        this.configService = configService;

        // 1. Binding UI
        loadingIndicator.visibleProperty().bind(viewModel.loadingProperty());
        statusLabel.textProperty().bind(viewModel.statusMessageProperty());

        // Hiển thị VBox (loading + status)
        statusContainer.visibleProperty().bind(viewModel.loadingProperty().or(viewModel.showStatusMessageProperty()));
        statusContainer.managedProperty().bind(statusContainer.visibleProperty());

        // Ẩn Grid (ScrollPane) khi status/loading hiển thị
        gridScrollPane.visibleProperty().bind(statusContainer.visibleProperty().not());
        gridScrollPane.managedProperty().bind(gridScrollPane.visibleProperty());

        // 2. Lắng nghe thay đổi danh sách items
        viewModel.getItems().addListener((ListChangeListener<BaseItemDto>) c -> {
            Platform.runLater(this::updateItemGrid);
        });

        // 3. Lắng nghe thay đổi item được chọn
        viewModel.selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            Platform.runLater(() -> updateCellSelection(oldItem, newItem));
        });

        // 4. Lắng nghe yêu cầu cuộn (Scroll Action)
        viewModel.scrollActionProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == GridNavigationState.ScrollAction.SCROLL_TO_TOP) {
                ignoreNextScrollEvent = true;
                Platform.runLater(() -> {
                    gridScrollPane.setVvalue(0.0);
                    Platform.runLater(() -> ignoreNextScrollEvent = false);
                });
            } else if (newVal == GridNavigationState.ScrollAction.SCROLL_TO_BOTTOM) {
                ignoreNextScrollEvent = true;
                Platform.runLater(() -> {
                    gridScrollPane.setVvalue(1.0);
                    Platform.runLater(() -> ignoreNextScrollEvent = false);
                });
            }
        });

        // 5. Cài đặt listener cuộn chuột (UR-24)
        gridScrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> {
            if (viewModel == null || viewModel.loadingProperty().get()) return;
            if (ignoreNextScrollEvent) {
                ignoreNextScrollEvent = false;
                return;
            }

            // (UR-24: Cuộn xuống)
            if (newVal.doubleValue() > 0.95 && oldVal.doubleValue() < 0.95) {
                if (viewModel.hasNextPageProperty().get()) {
                    if (showConfirmationDialog("Chuyển Trang", "Bạn có muốn chuyển sang trang tiếp theo?")) {
                        viewModel.loadNextPage();
                    }
                }
            }
            // (UR-24: Cuộn lên)
            else if (newVal.doubleValue() < 0.05 && oldVal.doubleValue() > 0.05) {
                if (viewModel.hasPreviousPageProperty().get()) {
                    if (showConfirmationDialog("Chuyển Trang", "Bạn có muốn chuyển về trang trước đó?")) {
                        viewModel.loadPreviousPage();
                    }
                }
            }
        });
    }

    /**
     * Helper hiển thị dialog xác nhận (UR-24).
     */
    private boolean showConfirmationDialog(String title, String content) {
        if (notificationService == null) {
            return true; // Nếu service lỗi, tự động đồng ý
        }
        return notificationService.showConfirmation(
                configService.getString("itemGridView", "confirmDialogTitle"),
                configService.getString("itemGridView", "confirmNextPage") // Cần cập nhật key
        );
    }

    /**
     * Cập nhật toàn bộ lưới item.
     */
    private void updateItemGrid() {
        itemFlowPane.getChildren().clear();
        String selectedId = (viewModel.selectedItemProperty().get() != null)
                ? viewModel.selectedItemProperty().get().getId() : null;

        for (BaseItemDto item : viewModel.getItems()) {
            StackPane cell = createItemCell(item);
            if (selectedId != null && item.getId() != null && item.getId().equals(selectedId)) {
                cell.getStyleClass().add("item-cell-selected");
            }
            itemFlowPane.getChildren().add(cell);
        }
    }

    /**
     * Cập nhật CSS cho cell được chọn/bỏ chọn.
     * (UR-26).
     */
    private void updateCellSelection(BaseItemDto oldItem, BaseItemDto newItem) {
        String oldId = (oldItem != null) ? oldItem.getId() : null;
        String newId = (newItem != null) ? newItem.getId() : null;

        for (Node node : itemFlowPane.getChildren()) {
            if (node.getUserData() instanceof BaseItemDto) {
                String cellItemId = ((BaseItemDto) node.getUserData()).getId();
                if (cellItemId == null) continue;

                if (cellItemId.equals(oldId)) {
                    node.getStyleClass().remove("item-cell-selected");
                }
                if (cellItemId.equals(newId)) {
                    if (!node.getStyleClass().contains("item-cell-selected")) {
                        node.getStyleClass().add("item-cell-selected");
                    }
                }
            }
        }
    }

    /**
     * Tạo một ô (cell) cho item.
     * (UR-25, UR-26, UR-27, UR-28).
     */
    private StackPane createItemCell(BaseItemDto item) {
        StackPane cellContainer = new StackPane();
        cellContainer.setPrefSize(CELL_WIDTH, CELL_HEIGHT);
        cellContainer.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        cellContainer.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        cellContainer.getStyleClass().add("item-cell");
        cellContainer.setUserData(item);

        ImageView imageView = new ImageView();
        imageView.setFitWidth(CELL_WIDTH);
        imageView.setFitHeight(IMAGE_HEIGHT);
        imageView.setPreserveRatio(false);
        imageView.setSmooth(true);
        imageView.getStyleClass().add("item-image");

        // (UR-25: Thumbnail)
        // --- SỬA LỖI 1: Gọi ViewModel để lấy URL ---
        String imageUrl = getImageUrl(item);
        // --- KẾT THÚC SỬA LỖI ---

        Image image = new Image(imageUrl, true); // true = tải nền
        imageView.setImage(image);

        // (Phần tiêu đề)
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

        // (UR-25: Badge)
        Float criticRating = item.getCriticRating();
        if (criticRating != null && criticRating > 0) {
            Label ratingLabel = new Label(String.format("%.0f", criticRating));
            ratingLabel.getStyleClass().add("parental-rating-badge");
            StackPane.setAlignment(ratingLabel, Pos.TOP_RIGHT);
            StackPane.setMargin(ratingLabel, new Insets(8, 8, 8, 8));
            cellContainer.getChildren().add(ratingLabel);
        }

        // (UR-26: Click chọn)
        cellContainer.setOnMouseClicked(event -> {
            viewModel.selectedItemProperty().set(item);
            // (UR-27: Double-click)
            if (event.getClickCount() == 2) {
                viewModel.playItemCommand(item);
            }
        });

        // (UR-28: Copy ID)
        final ContextMenu contextMenu = new ContextMenu();
        final MenuItem copyIdItem = new MenuItem("Sao chép ID");
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
     * (SỬA LỖI 1: Sửa hàm này)
     * Helper lấy URL ảnh.
     */
    private String getImageUrl(BaseItemDto item) {
        if (viewModel != null) {
            String url = viewModel.getPrimaryImageUrl(item);
            if (url != null) {
                return url;
            }
        }
        // Fallback (ảnh placeholder) nếu ViewModel bị null hoặc không trả về URL
        return "https://placehold.co/" + (int)CELL_WIDTH + "x" + (int)IMAGE_HEIGHT + "/333/999?text=" + (item.getType() != null ? item.getType() : "Item");
    }
}