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
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
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

        // 7. (MỚI - GĐ 12/UR-13) Đăng ký Hotkeys
        registerHotkeys();

        // 8. Kích hoạt "Home" khi khởi động
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
            libraryTreeController = loadNestedFXML("LibraryTreeView.fxml", leftPaneContainer);
            if (libraryTreeController != null) {
                libraryTreeController.setViewModel(libraryTreeViewModel);
            }

            itemGridController = loadNestedFXML("ItemGridView.fxml", centerPaneContainer);
            if (itemGridController != null) {
                itemGridController.setViewModel(itemGridViewModel, notificationService, configService);
            }

            itemDetailController = loadNestedFXML("ItemDetailView.fxml", rightPaneContainer);
            if (itemDetailController != null) {
                itemDetailController.setViewModel(itemDetailViewModel, configService);
            }

        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText(configService.getString("mainView", "errorLoadUI"));
        }
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
     * (SỬA ĐỔI GĐ 11/12: Thêm logic trả focus).
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
            itemDetailViewModel.loadItem(newVal);
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
                if (libraryTreeController != null) {
                    libraryTreeController.clearSelection();
                }
                searchField.setText("");
                itemGridViewModel.loadItemsByChip(newEvent.model, newEvent.type);
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
                // Sau khi dialog (showAndWait) đóng, trả focus về rootPane
                // để phím Enter có thể hoạt động ngay lập tức.
                Platform.runLater(() -> rootPane.requestFocus());
                // --- KẾT THÚC SỬA LỖI ---

                // Gửi kết quả (hoặc null) trở lại ViewModel
                itemDetailViewModel.processAddTagResult(result, newCtx);

                itemDetailViewModel.clearAddChipCommand();
            }
        });
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
        // Chúng ta phải đợi Stage hiển thị để lấy Scene
        Platform.runLater(() -> {
            Scene scene = rootPane.getScene();
            if (scene == null) {
                System.err.println("Không thể lấy Scene để đăng ký hotkey!");
                return;
            }

            scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                // (UR-13) ENTER Hotkey to repeat last Add Tag
                if (event.getCode() == KeyCode.ENTER && !event.isShortcutDown() && !event.isAltDown() && !event.isShiftDown()) {

                    Node focusedNode = scene.getFocusOwner();

                    // Kiểm tra xem focus có đang ở trên một control "chặn" (như text field, button) không
                    boolean isBlockingControl = focusedNode instanceof javafx.scene.control.TextInputControl ||
                            focusedNode instanceof javafx.scene.control.Button ||
                            focusedNode instanceof javafx.scene.control.ToggleButton;

                    // Chỉ kích hoạt nếu focus không nằm trên các control đó
                    if (focusedNode == null || !isBlockingControl) {
                        if (itemDetailViewModel != null) {
                            // (Logic từ ItemDetailController cũ: handleRepeatAddTagDialog)
                            itemDetailViewModel.repeatAddChipCommand();
                            event.consume();
                        }
                    }
                }

                // (Các hotkey khác của GĐ 12 như Cmd+S, Cmd+N sẽ được thêm ở đây)

            });
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