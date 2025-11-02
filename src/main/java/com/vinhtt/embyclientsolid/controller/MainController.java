package com.vinhtt.embyclientsolid.controller;

import com.vinhtt.embyclientsolid.MainApp;
import com.vinhtt.embyclientsolid.core.IConfigurationService;
import com.vinhtt.embyclientsolid.core.INotificationService;
import com.vinhtt.embyclientsolid.core.IPreferenceService;
import com.vinhtt.embyclientsolid.viewmodel.IItemDetailViewModel;
import com.vinhtt.embyclientsolid.viewmodel.IItemGridViewModel;
import com.vinhtt.embyclientsolid.viewmodel.ILibraryTreeViewModel;
import com.vinhtt.embyclientsolid.viewmodel.impl.MainViewModel;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty; // <-- SỬA LỖI 4: Thêm import
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;

import java.io.IOException;
import java.net.URL;

/**
 * Controller cho MainView.fxml (Giai đoạn 7).
 * (Đã sửa lỗi tiêm VM và logic lấy ID).
 */
public class MainController {

    // --- FXML UI Components ---
    @FXML private BorderPane rootPane;
    @FXML private Button homeButton;
    @FXML private Button logoutButton;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator statusProgressIndicator;
    @FXML private SplitPane mainSplitPane;
    @FXML private AnchorPane leftPaneContainer;
    @FXML private AnchorPane centerPaneContainer;
    @FXML private AnchorPane rightPaneContainer;
    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private Button sortByButton;
    @FXML private ToggleButton sortOrderButton;

    // --- Services & ViewModels (DI) ---
    private final MainViewModel viewModel;
    private final ILibraryTreeViewModel libraryTreeViewModel;
    private final IItemGridViewModel itemGridViewModel;
    private final IItemDetailViewModel itemDetailViewModel;
    private final IConfigurationService configService;
    private final IPreferenceService preferenceService;
    private final INotificationService notificationService; // <-- SỬA LỖI 2: Thêm service

    // --- Sub-Controllers ---
    private LibraryTreeController libraryTreeController;
    private ItemGridController itemGridController;
    private ItemDetailController itemDetailController;

    // --- Constants for Divider Positions (UR-8) ---
    private static final String KEY_DIVIDER_1 = "dividerPos1";
    private static final String KEY_DIVIDER_2 = "dividerPos2";

    /**
     * Khởi tạo MainController với tất cả các dependencies.
     * (SỬA LỖI 2: Nhận INotificationService).
     */
    public MainController(
            MainViewModel viewModel,
            ILibraryTreeViewModel libraryTreeViewModel,
            IItemGridViewModel itemGridViewModel,
            IItemDetailViewModel itemDetailViewModel,
            IConfigurationService configService,
            IPreferenceService preferenceService,
            INotificationService notificationService // <-- SỬA LỖI 2: Thêm tham số
    ) {
        this.viewModel = viewModel;
        this.libraryTreeViewModel = libraryTreeViewModel;
        this.itemGridViewModel = itemGridViewModel;
        this.itemDetailViewModel = itemDetailViewModel;
        this.configService = configService;
        this.preferenceService = preferenceService;
        this.notificationService = notificationService; // <-- SỬA LỖI 2: Lưu service
    }

    @FXML
    public void initialize() {
        // 1. Cài đặt I18n (UR-11)
        setupLocalization();

        // 2. Tải 3 cột con (UR-7)
        loadSubViews();

        // 3. Binding UI chung (Toolbar, Statusbar) vào MainViewModel
        bindCommonUI();

        // 4. Kết nối các ViewModels (Logic Điều phối)
        setupViewModelCoordination();

        // 5. Khôi phục vị trí thanh chia (UR-8)
        loadDividerPositions();

        // 6. Bắt đầu tải cây thư viện
        if (libraryTreeController != null) {
            libraryTreeController.loadLibraries();
        }

        // 7. Kích hoạt "Home" khi khởi động
        Platform.runLater(this::handleHomeButtonAction);
    }

    /**
     * Cài đặt các chuỗi văn bản (I18n).
     * (UR-11)
     */
    private void setupLocalization() {
        homeButton.setText(configService.getString("mainView", "homeButton"));
        logoutButton.setText(configService.getString("mainView", "logoutButton"));
        searchField.setPromptText(configService.getString("mainView", "searchPrompt"));
        searchButton.setText(configService.getString("mainView", "searchButton"));
    }

    /**
     * Tải FXML cho 3 cột và tiêm ViewModel cho chúng.
     * (SỬA LỖI 1 & 2: Truyền services vào setViewModel).
     */
    private void loadSubViews() {
        try {
            libraryTreeController = loadNestedFXML("LibraryTreeView.fxml", leftPaneContainer);
            if (libraryTreeController != null) {
                // Tiêm ViewModel cho Cột 1
                libraryTreeController.setViewModel(libraryTreeViewModel);
            }

            itemGridController = loadNestedFXML("ItemGridView.fxml", centerPaneContainer);
            if (itemGridController != null) {
                // Tiêm ViewModel cho Cột 2 (SỬA LỖI 1)
                itemGridController.setViewModel(itemGridViewModel, notificationService, configService);
            }

            itemDetailController = loadNestedFXML("ItemDetailView.fxml", rightPaneContainer);
            if (itemDetailController != null) {
                // Tiêm ViewModel cho Cột 3 (SỬA LỖI 2)
                itemDetailController.setViewModel(itemDetailViewModel, configService);
            }

        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText(configService.getString("mainView", "errorLoadUI"));
        }
    }

    /**
     * Binding các nút Toolbar và Statusbar vào MainViewModel.
     * (UR-5, UR-9, UR-10, UR-20, UR-21, UR-22).
     */
    private void bindCommonUI() {
        // Status Bar (UR-9)
        statusLabel.textProperty().bind(viewModel.statusMessageProperty());
        statusProgressIndicator.visibleProperty().bind(viewModel.loadingProperty());

        // Toolbar Buttons (UR-5, UR-10)
        homeButton.setOnAction(e -> handleHomeButtonAction());
        logoutButton.setOnAction(e -> viewModel.logoutCommand());

        // Search (UR-20)
        searchField.textProperty().bindBidirectional(viewModel.searchKeywordProperty());
        searchButton.setOnAction(e -> viewModel.searchCommand(searchField.getText()));
        searchField.setOnAction(e -> viewModel.searchCommand(searchField.getText()));

        // Sorting (UR-21, UR-22)
        bindSortingButtons();
        sortByButton.setOnAction(e -> viewModel.toggleSortByCommand());
        sortOrderButton.setOnAction(e -> viewModel.toggleSortOrderCommand());
    }

    /**
     * Thiết lập luồng dữ liệu giữa các ViewModel con.
     * (SỬA LỖI 3: Sửa logic lấy ID).
     */
    private void setupViewModelCoordination() {

        // 1. Khi Cây (Cột 1) chọn 1 item -> Cột 2 tải item đó
        libraryTreeViewModel.selectedTreeItemProperty().addListener((obs, oldVal, newVal) -> {
            // SỬA LỖI 3: Kiểm tra dummy node và gọi getItemDto()
            if (newVal != null && newVal.getValue() != null && !newVal.getValue().isLoadingNode()) {
                String parentId = newVal.getValue().getItemDto().getId();
                searchField.setText(""); // Xóa tìm kiếm
                itemGridViewModel.loadItemsByParentId(parentId);
            }
            // (Nếu newVal là null, nút Home đã xử lý)
        });

        // 2. Khi Lưới (Cột 2) chọn 1 item -> Cột 3 tải chi tiết item đó
        itemGridViewModel.selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            itemDetailViewModel.loadItem(newVal);
        });

        // 3. Khi MainVM yêu cầu tìm kiếm -> Cột 2 tải kết quả tìm kiếm
        viewModel.searchKeywordProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isEmpty()) {
                if (libraryTreeController != null) {
                    libraryTreeController.clearSelection(); // Bỏ chọn cây
                }
                itemGridViewModel.searchItems(newVal);
            }
        });

        // 4. Khi MainVM thay đổi sắp xếp -> Cột 2 tải lại với sắp xếp mới
        viewModel.sortByProperty().addListener((obs, oldVal, newVal) -> itemGridViewModel.setSortBy(newVal));
        viewModel.sortOrderProperty().addListener((obs, oldVal, newVal) -> itemGridViewModel.setSortOrder(newVal));

        // 5. Khi Cột 3 click vào chip (UR-36) -> Cột 2 tải theo chip
        itemDetailViewModel.chipClickEventProperty().addListener((obs, oldEvent, newEvent) -> {
            if (newEvent != null) {
                if (libraryTreeController != null) {
                    libraryTreeController.clearSelection(); // Bỏ chọn cây
                }
                searchField.setText(""); // Xóa tìm kiếm

                itemGridViewModel.loadItemsByChip(newEvent.model, newEvent.type);

                itemDetailViewModel.clearChipClickEvent(); // Tiêu thụ sự kiện
            }
        });
    }

    /**
     * Xử lý khi nhấn nút Home.
     * (UR-10)
     */
    @FXML
    private void handleHomeButtonAction() {
        if (libraryTreeController != null) {
            libraryTreeController.clearSelection();
        }
        viewModel.homeCommand(); // Đặt lại searchKeyword
        itemGridViewModel.loadItemsByParentId(null); // Tải gốc
    }

    /**
     * Helper tải FXML con vào AnchorPane.
     */
    private <T> T loadNestedFXML(String fxmlFile, AnchorPane container) throws IOException {
        URL fxmlUrl = MainApp.class.getResource("view/fxml/" + fxmlFile);
        if (fxmlUrl == null) {
            throw new IOException("Không tìm thấy FXML: " + fxmlFile);
        }

        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        // Controller sẽ được tạo tự động bởi FXML (vì chúng ta không
        // inject controller factory vào AppNavigator)
        Node node = loader.load();

        AnchorPane.setTopAnchor(node, 0.0);
        AnchorPane.setBottomAnchor(node, 0.0);
        AnchorPane.setLeftAnchor(node, 0.0);
        AnchorPane.setRightAnchor(node, 0.0);

        container.getChildren().add(node);
        return loader.getController();
    }

    /**
     * Binding logic cho các nút sắp xếp.
     * (UR-21, UR-22).
     */
    private void bindSortingButtons() {
        sortByButton.textProperty().bind(
                Bindings.createStringBinding(() -> {
                    String currentSortBy = viewModel.sortByProperty().get();
                    if (MainViewModel.SORT_BY_NAME.equals(currentSortBy)) {
                        return configService.getString("mainView", "sortByName");
                    } else if (MainViewModel.SORT_BY_DATE_RELEASE.equals(currentSortBy)) {
                        return configService.getString("mainView", "sortByDateRelease");
                    } else if (MainViewModel.SORT_BY_DATE_CREATED.equals(currentSortBy)) {
                        return configService.getString("mainView", "sortByDateCreated");
                    }
                    return configService.getString("mainView", "sortByDefault");
                }, viewModel.sortByProperty())
        );

        sortOrderButton.textProperty().bind(
                Bindings.createStringBinding(() -> {
                    String currentOrder = viewModel.sortOrderProperty().get();
                    if (MainViewModel.SORT_ORDER_ASCENDING.equals(currentOrder)) {
                        return configService.getString("mainView", "orderAsc");
                    } else {
                        return configService.getString("mainView", "orderDesc");
                    }
                }, viewModel.sortOrderProperty())
        );
        sortOrderButton.setSelected(
                MainViewModel.SORT_ORDER_ASCENDING.equals(viewModel.sortOrderProperty().get())
        );
        viewModel.sortOrderProperty().addListener((obs, oldVal, newVal) -> {
            sortOrderButton.setSelected(MainViewModel.SORT_ORDER_ASCENDING.equals(newVal));
        });

        // Vô hiệu hóa nút khi đang tìm kiếm (UR-20)
        // SỬA LỖI 4: Thêm import cho BooleanProperty
        BooleanBinding isSearching = Bindings.createBooleanBinding(
                () -> !viewModel.searchKeywordProperty().get().isEmpty(),
                viewModel.searchKeywordProperty()
        );
        sortByButton.disableProperty().bind(isSearching);
        sortOrderButton.disableProperty().bind(isSearching);
    }

    /**
     * Lưu vị trí thanh chia.
     * (UR-8)
     */
    private void saveDividerPositions() {
        if (mainSplitPane != null && preferenceService != null && mainSplitPane.getDividers().size() >= 2) {
            double[] positions = mainSplitPane.getDividerPositions();
            preferenceService.putDouble(KEY_DIVIDER_1, positions[0]);
            preferenceService.putDouble(KEY_DIVIDER_2, positions[1]);
            preferenceService.flush();
        }
    }

    /**
     * Tải vị trí thanh chia.
     * (UR-8)
     */
    private void loadDividerPositions() {
        if (mainSplitPane != null && preferenceService != null && mainSplitPane.getDividers().size() >= 2) {
            double pos1 = preferenceService.getDouble(KEY_DIVIDER_1, 0.20); // Cột 1 nhỏ hơn
            double pos2 = preferenceService.getDouble(KEY_DIVIDER_2, 0.60); // Cột 2 lớn hơn
            Platform.runLater(() -> {
                mainSplitPane.setDividerPositions(pos1, pos2);
                // Thêm listener để lưu khi thay đổi (UR-8)
                mainSplitPane.getDividers().get(0).positionProperty().addListener((obs, o, n) -> saveDividerPositions());
                mainSplitPane.getDividers().get(1).positionProperty().addListener((obs, o, n) -> saveDividerPositions());
            });
        }
    }
}