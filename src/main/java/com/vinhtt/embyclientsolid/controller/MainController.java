package com.vinhtt.embyclientsolid.controller;

import com.vinhtt.embyclientsolid.MainApp;
import com.vinhtt.embyclientsolid.core.IAppNavigator;
import com.vinhtt.embyclientsolid.core.IConfigurationService;
import com.vinhtt.embyclientsolid.core.INotificationService;
import com.vinhtt.embyclientsolid.core.IPreferenceService;
import com.vinhtt.embyclientsolid.model.SuggestionContext;
import com.vinhtt.embyclientsolid.viewmodel.AddTagResult;
import com.vinhtt.embyclientsolid.viewmodel.IItemDetailViewModel;
import com.vinhtt.embyclientsolid.viewmodel.IItemGridViewModel;
import com.vinhtt.embyclientsolid.viewmodel.ILibraryTreeViewModel;
import com.vinhtt.embyclientsolid.viewmodel.impl.MainViewModel;
import embyclient.model.BaseItemDto;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * Controller cho MainView.fxml (Giai đoạn 7).
 * (Cập nhật GĐ 11/12: Sửa lỗi mất focus sau khi đóng dialog).
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
    private final INotificationService notificationService;
    private final IAppNavigator appNavigator;

    // --- Sub-Controllers ---
    private LibraryTreeController libraryTreeController;
    private ItemGridController itemGridController;
    private ItemDetailController itemDetailController;
    private boolean isChipLoading = false;

    // --- Constants for Divider Positions (UR-8) ---
    private static final String KEY_DIVIDER_1 = "dividerPos1";
    private static final String KEY_DIVIDER_2 = "dividerPos2";

    /**
     * Khởi tạo MainController với tất cả các dependencies.
     */
    public MainController(
            MainViewModel viewModel,
            ILibraryTreeViewModel libraryTreeViewModel,
            IItemGridViewModel itemGridViewModel,
            IItemDetailViewModel itemDetailViewModel,
            IConfigurationService configService,
            IPreferenceService preferenceService,
            INotificationService notificationService,
            IAppNavigator appNavigator
    ) {
        this.viewModel = viewModel;
        this.libraryTreeViewModel = libraryTreeViewModel;
        this.itemGridViewModel = itemGridViewModel;
        this.itemDetailViewModel = itemDetailViewModel;
        this.configService = configService;
        this.preferenceService = preferenceService;
        this.notificationService = notificationService;
        this.appNavigator = appNavigator;
    }

    @FXML
    public void initialize() {
        setupLocalization();
        loadSubViews();
        bindCommonUI();
        setupViewModelCoordination();
        loadDividerPositions();
        if (libraryTreeController != null) {
            libraryTreeController.loadLibraries();
        }
        registerHotkeys();
        Platform.runLater(this::handleHomeButtonAction);
    }

    private void setupLocalization() {
        homeButton.setText(configService.getString("mainView", "homeButton"));
        logoutButton.setText(configService.getString("mainView", "logoutButton"));
        searchField.setPromptText(configService.getString("mainView", "searchPrompt"));
        searchButton.setText(configService.getString("mainView", "searchButton"));
    }

    private void loadSubViews() {
        try {
            // 1. LibraryTreeController
            libraryTreeController = new LibraryTreeController(); // <-- TẠO MỚI
            loadAndInjectFXML("LibraryTreeView.fxml", libraryTreeController, leftPaneContainer); // <-- TIÊM
            libraryTreeController.setViewModel(libraryTreeViewModel, configService);

            // 2. ItemGridController
            itemGridController = new ItemGridController(); // <-- TẠO MỚI
            loadAndInjectFXML("ItemGridView.fxml", itemGridController, centerPaneContainer); // <-- TIÊM
            itemGridController.setViewModel(itemGridViewModel, notificationService, configService); // <-- BIND

            // 3. ItemDetailController
            itemDetailController = new ItemDetailController(); // <-- TẠO MỚI
            loadAndInjectFXML("ItemDetailView.fxml", itemDetailController, rightPaneContainer); // <-- TIÊM
            itemDetailController.setViewModel(itemDetailViewModel, configService); // <-- BIND

        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText(configService.getString("mainView", "errorLoadUI"));
        }
    }

    /**
     * Helper mới để tải FXML và tiêm (inject) Controller đã được tạo.
     *
     * @param fxmlFile Tên file FXML (ví dụ: "LibraryTreeView.fxml")
     * @param controllerInstance Instance của Controller đã được tạo (ví dụ: new LibraryTreeController())
     * @param container AnchorPane (cột) để chứa nội dung
     * @throws IOException
     */
    private void loadAndInjectFXML(String fxmlFile, Object controllerInstance, AnchorPane container) throws IOException {
        URL fxmlUrl = MainApp.class.getResource("view/fxml/" + fxmlFile);
        if (fxmlUrl == null) {
            throw new IOException("Không tìm thấy FXML: " + fxmlFile);
        }

        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        loader.setController(controllerInstance); // <-- TIÊM CONTROLLER VÀO LOADER

        Node node = loader.load(); // <-- TẢI FXML (sẽ không tạo controller mới)

        // Gắn node vào AnchorPane (như code cũ)
        AnchorPane.setTopAnchor(node, 0.0);
        AnchorPane.setBottomAnchor(node, 0.0);
        AnchorPane.setLeftAnchor(node, 0.0);
        AnchorPane.setRightAnchor(node, 0.0);
        container.getChildren().add(node);
    }

    private void bindCommonUI() {
        statusLabel.textProperty().bind(viewModel.statusMessageProperty());
        BooleanBinding combinedLoading = libraryTreeViewModel.loadingProperty()
                .or(itemGridViewModel.loadingProperty())
                .or(itemDetailViewModel.loadingProperty());
        viewModel.loadingProperty().bind(combinedLoading);
        statusProgressIndicator.visibleProperty().bind(viewModel.loadingProperty());
        homeButton.setOnAction(e -> handleHomeButtonAction());
        logoutButton.setOnAction(e -> viewModel.logoutCommand());
        searchField.textProperty().bindBidirectional(viewModel.searchKeywordProperty());
        searchButton.setOnAction(e -> viewModel.searchCommand(searchField.getText()));
        searchField.setOnAction(e -> viewModel.searchCommand(searchField.getText()));
        bindSortingButtons();
        sortByButton.setOnAction(e -> viewModel.toggleSortByCommand());
        sortOrderButton.setOnAction(e -> viewModel.toggleSortOrderCommand());
    }

    /**
     * (SỬA ĐỔI GĐ 11/12: Sửa lỗi focus).
     */
    private void setupViewModelCoordination() {

        // 1. Cây -> Lưới
        libraryTreeViewModel.selectedTreeItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getValue() != null && !newVal.getValue().isLoadingNode()) {
                String parentId = newVal.getValue().getItemDto().getId();
                searchField.setText("");
                itemGridViewModel.loadItemsByParentId(parentId);
            }
        });

        // 2. Lưới -> Chi tiết
        itemGridViewModel.selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (!isChipLoading) {
                itemDetailViewModel.loadItem(newVal);
            }
        });

        // 3. MainVM (Tìm kiếm) -> Lưới
        viewModel.searchKeywordProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isEmpty()) {
                if (libraryTreeController != null) {
                    libraryTreeController.clearSelection();
                }
                itemGridViewModel.searchItems(newVal);
            }
        });

        // 4. MainVM (Sắp xếp) -> Lưới
        viewModel.sortByProperty().addListener((obs, oldVal, newVal) -> itemGridViewModel.setSortBy(newVal));
        viewModel.sortOrderProperty().addListener((obs, oldVal, newVal) -> itemGridViewModel.setSortOrder(newVal));

        // 5. Chi tiết (Click Chip) -> Lưới
        itemDetailViewModel.chipClickEventProperty().addListener((obs, oldEvent, newEvent) -> {
            if (newEvent != null) {
                // 1. Đặt cờ
                isChipLoading = true;

                // 2. (SỬA LỖI TIMING) Thêm một listener tạm thời vào GridVM
                // Listener này sẽ tự hủy sau khi reset cờ.
                itemGridViewModel.loadingProperty().addListener(new ChangeListener<Boolean>() {
                    @Override
                    public void changed(ObservableValue<? extends Boolean> obs, Boolean wasLoading, Boolean isLoading) {
                        if (wasLoading && !isLoading) { // Khi GridVM tải xong (từ true -> false)
                            isChipLoading = false; // Reset cờ
                            itemGridViewModel.loadingProperty().removeListener(this); // Tự hủy
                        }
                    }
                });

                // 3. Xóa chọn cây và text tìm kiếm
                if (libraryTreeController != null) {
                    libraryTreeController.clearSelection();
                }
                searchField.setText("");

                // 4. Kích hoạt tải lưới (việc này sẽ set loading=true, sau đó set selectedItem=null)
                // Listener (số 2) giờ sẽ thấy isChipLoading=true và bỏ qua việc xóa Cột 3
                itemGridViewModel.loadItemsByChip(newEvent.model, newEvent.type);

                // 5. Tiêu thụ sự kiện
                itemDetailViewModel.clearChipClickEvent();
            }
        });

        // 6. Chi tiết (Add Chip) -> MainController -> AppNavigator
        itemDetailViewModel.addChipCommandProperty().addListener((obs, oldCtx, newCtx) -> {
            if (newCtx != null) {
                Stage ownerStage = (Stage) rootPane.getScene().getWindow();

                // Gọi AppNavigator để hiển thị dialog (blocking)
                AddTagResult result = appNavigator.showAddTagDialog(ownerStage, newCtx);

                // --- SỬA LỖI FOCUS (UR-13) ---
                // Ngay sau khi dialog (showAndWait) đóng, trả focus về rootPane
                // để phím Enter có thể hoạt động ngay lập tức.
                Platform.runLater(() -> rootPane.requestFocus());
                // --- KẾT THÚC SỬA LỖI ---

                // Gửi kết quả (hoặc null) trở lại ViewModel
                itemDetailViewModel.processAddTagResult(result, newCtx);

                itemDetailViewModel.clearAddChipCommand();
            }
        });

        // 7. Chi tiết (Pop-out) -> AppNavigator
        itemDetailViewModel.popOutRequestProperty().addListener((obs, oldV, newV) -> {
            if (newV != null && newV) {
                BaseItemDto selectedItem = itemGridViewModel.selectedItemProperty().get();
                if (selectedItem != null) {
                    appNavigator.showPopOutDetail(selectedItem); // Truyền item
                }
                itemDetailViewModel.clearPopOutRequest(); // Reset cờ
            }
        });
    }

    public ItemGridController getItemGridController() {
        return itemGridController;
    }

    public ItemDetailController getItemDetailController() {
        return itemDetailController;
    }

    /**
     * Đăng ký Hotkeys (khi app được focus) trên Scene chính.
     * Được gọi từ MainApp.
     */
    public void registerGlobalHotkeys(Scene scene) {
        if (scene == null) return;

        // Gọi các helper (được sao chép từ EmbyClientJavaFX)
        appNavigator.registerHotkeys(
                scene,
                this,
                this.itemDetailController,
                this.itemGridController,
                this.itemGridViewModel
        );
    }

    @FXML
    private void handleHomeButtonAction() {
        if (libraryTreeController != null) {
            libraryTreeController.clearSelection();
        }
        viewModel.homeCommand(); // Đặt lại searchKeyword
        itemGridViewModel.loadItemsByParentId(null); // Tải gốc
    }

    private <T> T loadNestedFXML(String fxmlFile, AnchorPane container) throws IOException {
        URL fxmlUrl = MainApp.class.getResource("view/fxml/" + fxmlFile);
        if (fxmlUrl == null) {
            throw new IOException("Không tìm thấy FXML: " + fxmlFile);
        }
        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Node node = loader.load();
        AnchorPane.setTopAnchor(node, 0.0);
        AnchorPane.setBottomAnchor(node, 0.0);
        AnchorPane.setLeftAnchor(node, 0.0);
        AnchorPane.setRightAnchor(node, 0.0);
        container.getChildren().add(node);
        return loader.getController();
    }

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

        BooleanBinding isSearching = Bindings.createBooleanBinding(
                () -> !viewModel.searchKeywordProperty().get().isEmpty(),
                viewModel.searchKeywordProperty()
        );
        sortByButton.disableProperty().bind(isSearching);
        sortOrderButton.disableProperty().bind(isSearching);
    }

    /**
     * (MỚI - GĐ 12/UR-13) Đăng ký phím tắt cho Scene.
     */
    private void registerHotkeys() {
        Platform.runLater(() -> {
            Scene scene = rootPane.getScene();
            if (scene == null) {
                System.err.println("Không thể lấy Scene để đăng ký hotkey!");
                return;
            }

            if (notificationService != null) {
                notificationService.setOwnerWindow(scene.getWindow());
            }

            // Di chuyển logic từ registerGlobalHotkeys() vào đây
            // để đảm bảo nó được gọi.
            if (appNavigator != null) {
                appNavigator.registerHotkeys(
                        scene,
                        this,
                        this.itemDetailController,
                        this.itemGridController,
                        this.itemGridViewModel
                );
            }
        });
    }

    private void saveDividerPositions() {
        if (mainSplitPane != null && preferenceService != null && mainSplitPane.getDividers().size() >= 2) {
            double[] positions = mainSplitPane.getDividerPositions();
            preferenceService.putDouble(KEY_DIVIDER_1, positions[0]);
            preferenceService.putDouble(KEY_DIVIDER_2, positions[1]);
            preferenceService.flush();
        }
    }
    private void loadDividerPositions() {
        if (mainSplitPane != null && preferenceService != null && mainSplitPane.getDividers().size() >= 2) {
            double pos1 = preferenceService.getDouble(KEY_DIVIDER_1, 0.20);
            double pos2 = preferenceService.getDouble(KEY_DIVIDER_2, 0.60);
            Platform.runLater(() -> {
                mainSplitPane.setDividerPositions(pos1, pos2);
                mainSplitPane.getDividers().get(0).positionProperty().addListener((obs, o, n) -> saveDividerPositions());
                mainSplitPane.getDividers().get(1).positionProperty().addListener((obs, o, n) -> saveDividerPositions());
            });
        }
    }
}