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
 * Controller (View) cho MainView.fxml.
 * Đóng vai trò là View-Coordinator (Điều phối View), chịu trách nhiệm:
 * 1. Tải 3 Controller con (Tree, Grid, Detail).
 * 2. Tiêm (inject) ViewModels cho 3 Controller con.
 * 3. Kết nối và điều phối logic giữa các ViewModel (ví dụ: Cây -> Lưới, Lưới -> Chi tiết, Chi tiết -> Lưới).
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

    // Cờ trạng thái ngăn Cột 3 (Detail) bị xóa khi Cột 2 (Grid) đang tải do click chip
    private boolean isChipLoading = false;

    // --- Constants for Divider Positions (UR-8) ---
    private static final String KEY_DIVIDER_1 = "dividerPos1";
    private static final String KEY_DIVIDER_2 = "dividerPos2";

    // Biến lưu vị trí divider trước khi ẩn cột 3
    private double[] lastDividerPositions = new double[]{0.20, 0.60}; // Giá trị mặc định
    /**
     * Khởi tạo MainController với tất cả các dependencies (DI).
     *
     * @param viewModel VM điều phối chung (Toolbar, Status bar).
     * @param libraryTreeViewModel VM cho Cột 1.
     * @param itemGridViewModel VM cho Cột 2.
     * @param itemDetailViewModel VM cho Cột 3.
     * @param configService Service I18n.
     * @param preferenceService Service lưu/đọc cài đặt (vị trí divider).
     * @param notificationService Service thông báo (cho Cột 2 và hotkey).
     * @param appNavigator Service điều hướng (cho dialog và hotkey).
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

    /**
     * Được gọi bởi FXMLLoader sau khi các trường @FXML đã được tiêm.
     * Khởi tạo UI, tải các view con, và kết nối các ViewModel.
     */
    @FXML
    public void initialize() {
        setupLocalization();
        loadSubViews();
        bindCommonUI();
        setupViewModelCoordination();
        loadDividerPositions();

        // Bắt đầu tải Cột 1
        if (libraryTreeController != null) {
            libraryTreeController.loadLibraries();
        }

        // Đăng ký hotkey và tải Cột 2 (Home)
        registerHotkeys();
        Platform.runLater(this::handleHomeButtonAction);
    }

    /**
     * Cài đặt các chuỗi văn bản (I18n) cho UI (Toolbar).
     */
    private void setupLocalization() {
        homeButton.setText(configService.getString("mainView", "homeButton"));
        logoutButton.setText(configService.getString("mainView", "logoutButton"));
        searchField.setPromptText(configService.getString("mainView", "searchPrompt"));
        searchButton.setText(configService.getString("mainView", "searchButton"));
    }

    /**
     * Tải FXML cho 3 cột con (Tree, Grid, Detail),
     * tiêm (inject) Controller tương ứng cho mỗi FXML,
     * và tiêm ViewModel cho mỗi Controller.
     */
    private void loadSubViews() {
        try {
            // 1. Cột 1: LibraryTreeController
            libraryTreeController = new LibraryTreeController();
            loadAndInjectFXML("LibraryTreeView.fxml", libraryTreeController, leftPaneContainer);
            libraryTreeController.setViewModel(libraryTreeViewModel, configService);

            // 2. Cột 2: ItemGridController
            itemGridController = new ItemGridController();
            loadAndInjectFXML("ItemGridView.fxml", itemGridController, centerPaneContainer);
            itemGridController.setViewModel(itemGridViewModel, notificationService, configService);

            // 3. Cột 3: ItemDetailController
            itemDetailController = new ItemDetailController();
            loadAndInjectFXML("ItemDetailView.fxml", itemDetailController, rightPaneContainer);
            itemDetailController.setViewModel(itemDetailViewModel, configService);

        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText(configService.getString("mainView", "errorLoadUI"));
        }
    }

    /**
     * Helper tải FXML, tiêm (inject) một Controller đã được khởi tạo
     * vào FXML Loader, và gắn vào AnchorPane container.
     *
     * @param fxmlFile Tên file FXML (ví dụ: "LibraryTreeView.fxml").
     * @param controllerInstance Instance của Controller (ví dụ: libraryTreeController).
     * @param container AnchorPane (cột) để chứa FXML.
     * @throws IOException
     */
    private void loadAndInjectFXML(String fxmlFile, Object controllerInstance, AnchorPane container) throws IOException {
        URL fxmlUrl = MainApp.class.getResource("view/fxml/" + fxmlFile);
        if (fxmlUrl == null) {
            throw new IOException("Không tìm thấy FXML: " + fxmlFile);
        }

        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        // Tiêm Controller (thay vì để FXML tạo mới)
        loader.setController(controllerInstance);

        Node node = loader.load();

        // Gắn node con vào container (AnchorPane)
        AnchorPane.setTopAnchor(node, 0.0);
        AnchorPane.setBottomAnchor(node, 0.0);
        AnchorPane.setLeftAnchor(node, 0.0);
        AnchorPane.setRightAnchor(node, 0.0);
        container.getChildren().add(node);
    }

    /**
     * Liên kết (bind) các UI chung (Toolbar, Status bar) với MainViewModel.
     */
    private void bindCommonUI() {
        // Liên kết Status Bar
        statusLabel.textProperty().bind(viewModel.statusMessageProperty());

        // Indicator loading chung sẽ bật nếu 1 trong 3 cột đang loading
        BooleanBinding combinedLoading = libraryTreeViewModel.loadingProperty()
                .or(itemGridViewModel.loadingProperty())
                .or(itemDetailViewModel.loadingProperty());
        viewModel.loadingProperty().bind(combinedLoading);
        statusProgressIndicator.visibleProperty().bind(viewModel.loadingProperty());

        // Liên kết các nút Toolbar
        homeButton.setOnAction(e -> handleHomeButtonAction());
        logoutButton.setOnAction(e -> viewModel.logoutCommand());
        searchField.textProperty().bindBidirectional(viewModel.searchKeywordProperty());
        searchButton.setOnAction(e -> viewModel.searchCommand(searchField.getText()));
        searchField.setOnAction(e -> viewModel.searchCommand(searchField.getText())); // Enter trên search field

        // Liên kết các nút Sắp xếp
        bindSortingButtons();
        sortByButton.setOnAction(e -> viewModel.toggleSortByCommand());
        sortOrderButton.setOnAction(e -> viewModel.toggleSortOrderCommand());
    }

    /**
     * Cài đặt logic điều phối (coordination) giữa các ViewModel.
     * Đây là logic cốt lõi của MainController.
     */
    private void setupViewModelCoordination() {

        // 1. Cây -> Lưới: Khi chọn item trên Cây (Cột 1)...
        libraryTreeViewModel.selectedTreeItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getValue() != null && !newVal.getValue().isLoadingNode()) {
                // ...tải item con của nó vào Lưới (Cột 2)
                String parentId = newVal.getValue().getItemDto().getId();
                searchField.setText("");
                itemGridViewModel.loadItemsByParentId(parentId);
            }
        });

        // 2. Lưới -> Chi tiết: Khi chọn item trên Lưới (Cột 2)...
        itemGridViewModel.selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            // Bỏ qua nếu Cột 2 đang tải do click chip (logic ở mục 5)
            if (!isChipLoading) {
                // ...tải chi tiết của nó vào Cột 3
                itemDetailViewModel.loadItem(newVal);

                // Xử lý hotkey Cmd+Shift+N/P (Tự động Play sau khi chọn)
                if (itemGridViewModel.isPlayAfterSelect()) {
                    if (newVal != null && Boolean.FALSE.equals(newVal.isIsFolder())) {
                        // 1. Yêu cầu Cột 2 phát file
                        itemGridViewModel.playItemCommand(newVal);
                        // 2. Yêu cầu Cột 3 kích hoạt cờ Pop-out
                        itemDetailViewModel.openFileOrFolderCommand();
                    }
                    itemGridViewModel.clearPlayAfterSelect(); // Xóa cờ
                }
            }
        });

        // 3. MainVM (Tìm kiếm) -> Lưới: Khi tìm kiếm trên Toolbar...
        viewModel.searchKeywordProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isEmpty()) {
                // ...xóa chọn Cột 1 và tải kết quả vào Cột 2
                if (libraryTreeController != null) {
                    libraryTreeController.clearSelection();
                }
                itemGridViewModel.searchItems(newVal);
            }
        });

        // 4. MainVM (Sắp xếp) -> Lưới: Khi thay đổi sắp xếp trên Toolbar...
        viewModel.sortByProperty().addListener((obs, oldVal, newVal) -> itemGridViewModel.setSortBy(newVal));
        viewModel.sortOrderProperty().addListener((obs, oldVal, newVal) -> itemGridViewModel.setSortOrder(newVal));

        // 5. Chi tiết (Click Chip) -> Lưới: Khi click vào chip (Tag, Studio...) ở Cột 3...
        itemDetailViewModel.chipClickEventProperty().addListener((obs, oldEvent, newEvent) -> {
            if (newEvent != null) {
                // 1. Đặt cờ: Báo cho (mục 2) biết Cột 2 sắp tải
                isChipLoading = true;

                // 2. Thêm listener tạm thời vào Cột 2
                // Khi Cột 2 tải xong (loading: true -> false), reset cờ
                itemGridViewModel.loadingProperty().addListener(new ChangeListener<Boolean>() {
                    @Override
                    public void changed(ObservableValue<? extends Boolean> obs, Boolean wasLoading, Boolean isLoading) {
                        if (wasLoading && !isLoading) { // Tải xong
                            isChipLoading = false; // Reset cờ
                            itemGridViewModel.loadingProperty().removeListener(this); // Tự hủy listener
                        }
                    }
                });

                // 3. Xóa chọn Cột 1 và Toolbar
                if (libraryTreeController != null) {
                    libraryTreeController.clearSelection();
                }
                searchField.setText("");

                // 4. Kích hoạt tải Cột 2 theo chip
                itemGridViewModel.loadItemsByChip(newEvent.model, newEvent.type);

                // 5. Tiêu thụ sự kiện
                itemDetailViewModel.clearChipClickEvent();
            }
        });

        // 6. Chi tiết (Add Chip) -> MainController -> AppNavigator (UR-35, UR-13)
        itemDetailViewModel.addChipCommandProperty().addListener((obs, oldCtx, newCtx) -> {
            Stage ownerStage = (Stage) rootPane.getScene().getWindow();

            // Chỉ mở dialog nếu cửa sổ chính đang được focus
            if (newCtx != null && ownerStage.isFocused()) {
                // Gọi AppNavigator để hiển thị dialog (chạy blocking)
                AddTagResult result = appNavigator.showAddTagDialog(ownerStage, newCtx);

                // Sửa lỗi mất focus hotkey (UR-13):
                // Ngay sau khi dialog đóng, trả focus về rootPane
                Platform.runLater(() -> rootPane.requestFocus());

                // Gửi kết quả (hoặc null) trở lại Cột 3
                itemDetailViewModel.processAddTagResult(result, newCtx);
                itemDetailViewModel.clearAddChipCommand();
            }
        });

        // 7. Chi tiết (Pop-out) -> AppNavigator (UR-50)
        itemDetailViewModel.popOutRequestProperty().addListener((obs, oldV, newV) -> {
            if (newV != null && newV) {
                BaseItemDto selectedItem = itemGridViewModel.selectedItemProperty().get();
                if (selectedItem != null) {
                    // Yêu cầu AppNavigator mở cửa sổ pop-out VÀ TRUYỀN 'THIS'
                    appNavigator.showPopOutDetail(selectedItem, this);
                }
                itemDetailViewModel.clearPopOutRequest(); // Reset cờ
            }
        });
    }

    /**
     * Lấy instance của ItemGridController (dùng cho AppNavigator).
     * @return ItemGridController.
     */
    public ItemGridController getItemGridController() {
        return itemGridController;
    }

    /**
     * Lấy instance của ItemDetailController (dùng cho AppNavigator).
     * @return ItemDetailController.
     */
    public ItemDetailController getItemDetailController() {
        return itemDetailController;
    }

    /**
     * Đăng ký Hotkeys (khi app được focus) trên Scene chính.
     * Được gọi bởi AppNavigator.
     *
     * @param scene Scene chính của ứng dụng.
     */
    public void registerGlobalHotkeys(Scene scene) {
        if (scene == null) return;

        // Ủy thác cho AppNavigator đăng ký các hotkey
        appNavigator.registerHotkeys(
                scene,
                this,
                this.itemDetailController,
                this.itemGridController,
                this.itemGridViewModel
        );
    }

    /**
     * Xử lý sự kiện onAction của nút Home (UR-10).
     * Được gọi từ FXML hoặc initialize().
     */
    @FXML
    private void handleHomeButtonAction() {
        if (libraryTreeController != null) {
            libraryTreeController.clearSelection();
        }
        viewModel.homeCommand(); // Xóa text tìm kiếm
        itemGridViewModel.loadItemsByParentId(null); // Tải item gốc (Home)
    }

    /**
     * Helper binding text và trạng thái cho các nút Sắp xếp (UR-21, UR-22).
     */
    private void bindSortingButtons() {
        // Thay đổi Text của nút SortBy
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

        // Thay đổi Text của nút SortOrder (ToggleButton)
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

        // Cập nhật trạng thái "selected" của ToggleButton
        sortOrderButton.setSelected(
                MainViewModel.SORT_ORDER_ASCENDING.equals(viewModel.sortOrderProperty().get())
        );
        viewModel.sortOrderProperty().addListener((obs, oldVal, newVal) -> {
            sortOrderButton.setSelected(MainViewModel.SORT_ORDER_ASCENDING.equals(newVal));
        });

        // Vô hiệu hóa các nút Sắp xếp nếu đang Tìm kiếm
        BooleanBinding isSearching = Bindings.createBooleanBinding(
                () -> !viewModel.searchKeywordProperty().get().isEmpty(),
                viewModel.searchKeywordProperty()
        );
        sortByButton.disableProperty().bind(isSearching);
        sortOrderButton.disableProperty().bind(isSearching);
    }

    /**
     * Đăng ký phím tắt (hotkey) cho Scene chính (UR-13, UR-14).
     * Được gọi sau khi UI đã được khởi tạo.
     */
    private void registerHotkeys() {
        Platform.runLater(() -> {
            Scene scene = rootPane.getScene();
            if (scene == null) {
                System.err.println("Không thể lấy Scene để đăng ký hotkey!");
                return;
            }

            // Cung cấp Window (Stage) cho NotificationService (để hiển thị dialog xác nhận)
            if (notificationService != null) {
                notificationService.setOwnerWindow(scene.getWindow());
            }

            // Ủy thác logic đăng ký hotkey cho AppNavigator
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

    /**
     * Lưu vị trí các thanh chia (divider) (UR-8).
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
     * Tải (khôi phục) vị trí các thanh chia (divider) (UR-8).
     */
    private void loadDividerPositions() {
        if (mainSplitPane != null && preferenceService != null && mainSplitPane.getDividers().size() >= 2) {
            double pos1 = preferenceService.getDouble(KEY_DIVIDER_1, 0.20); // Mặc định 20%
            double pos2 = preferenceService.getDouble(KEY_DIVIDER_2, 0.60); // Mặc định 60%

            // Lưu vị trí khôi phục vào biến instance
            lastDividerPositions = new double[]{pos1, pos2};

            Platform.runLater(() -> {
                // Đặt vị trí
                mainSplitPane.setDividerPositions(pos1, pos2);

                // Thêm listener để tự động lưu khi người dùng kéo
                mainSplitPane.getDividers().get(0).positionProperty().addListener((obs, o, n) -> saveDividerPositions());
                mainSplitPane.getDividers().get(1).positionProperty().addListener((obs, o, n) -> saveDividerPositions());
            });
        }
    }

    /**
     * Ẩn cột chi tiết (cột 3) và cho phép cột 2 (grid) mở rộng.
     * Được gọi bởi AppNavigator khi mở pop-out.
     */
    public void hideDetailColumn() {
        if (rightPaneContainer != null && mainSplitPane.getItems().contains(rightPaneContainer)) {
            // 1. Lưu vị trí divider hiện tại (nếu có 2 dividers)
            if (mainSplitPane.getDividerPositions().length >= 2) {
                lastDividerPositions = mainSplitPane.getDividerPositions();
            }

            // 2. Ẩn và xóa cột 3 khỏi SplitPane
            // Điều này sẽ khiến cột 2 tự động mở rộng
            rightPaneContainer.setVisible(false);
            rightPaneContainer.setManaged(false);
            mainSplitPane.getItems().remove(rightPaneContainer);
        }
    }

    /**
     * Hiện lại cột chi tiết (cột 3) và khôi phục vị trí divider.
     * Được gọi bởi AppNavigator khi đóng pop-out.
     */
    public void showDetailColumn() {
        if (rightPaneContainer != null && !mainSplitPane.getItems().contains(rightPaneContainer)) {
            // 1. Thêm cột 3 trở lại SplitPane
            rightPaneContainer.setVisible(true);
            rightPaneContainer.setManaged(true);
            mainSplitPane.getItems().add(rightPaneContainer);

            // 2. Khôi phục vị trí divider
            // Phải bọc trong Platform.runLater để đảm bảo layout đã cập nhật
            Platform.runLater(() -> {
                mainSplitPane.setDividerPositions(lastDividerPositions[0], lastDividerPositions[1]);
            });
        }
    }
}