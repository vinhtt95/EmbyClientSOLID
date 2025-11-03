package com.vinhtt.embyclientsolid.navigation;

import com.vinhtt.embyclientsolid.MainApp;
import com.vinhtt.embyclientsolid.controller.*;
import com.vinhtt.embyclientsolid.core.*;
import com.vinhtt.embyclientsolid.data.IExternalDataService;
import com.vinhtt.embyclientsolid.data.IItemRepository;
import com.vinhtt.embyclientsolid.data.IItemUpdateService;
import com.vinhtt.embyclientsolid.data.IStaticDataRepository;
import com.vinhtt.embyclientsolid.model.SuggestionContext;
import com.vinhtt.embyclientsolid.model.Tag;
import com.vinhtt.embyclientsolid.viewmodel.AddTagResult;
import com.vinhtt.embyclientsolid.viewmodel.IAddTagViewModel;
import com.vinhtt.embyclientsolid.viewmodel.IItemDetailViewModel;
import com.vinhtt.embyclientsolid.viewmodel.IItemGridViewModel;
import com.vinhtt.embyclientsolid.viewmodel.ILibraryTreeViewModel;
import com.vinhtt.embyclientsolid.viewmodel.ILoginViewModel;
import com.vinhtt.embyclientsolid.viewmodel.impl.AddTagViewModel;
import com.vinhtt.embyclientsolid.viewmodel.impl.ItemDetailViewModel;
import com.vinhtt.embyclientsolid.viewmodel.impl.ItemGridViewModel;
import com.vinhtt.embyclientsolid.viewmodel.impl.LibraryTreeViewModel;
import com.vinhtt.embyclientsolid.viewmodel.impl.LoginViewModel;
import com.vinhtt.embyclientsolid.viewmodel.impl.MainViewModel;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import com.vinhtt.embyclientsolid.controller.ItemDetailController;
import com.vinhtt.embyclientsolid.controller.ItemGridController;
import com.vinhtt.embyclientsolid.controller.MainController;
import com.vinhtt.embyclientsolid.model.SuggestionContext;
import com.vinhtt.embyclientsolid.model.Tag;
import com.vinhtt.embyclientsolid.viewmodel.AddTagResult;
import com.vinhtt.embyclientsolid.viewmodel.IItemGridViewModel;
import com.vinhtt.embyclientsolid.viewmodel.impl.ItemDetailViewModel;
import embyclient.model.BaseItemDto;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import java.io.IOException;
import java.net.URL;

/**
 * Triển khai của {@link IAppNavigator}.
 * Lớp này quản lý việc chuyển đổi giữa các Scene (Màn hình) và hiển thị các
 * cửa sổ (Stage) và dialog. Nó đóng vai trò trung tâm trong việc khởi tạo
 * các cặp View-ViewModel và tiêm (inject) các dependency.
 * (UR-5, UR-35, UR-50).
 */
public class AppNavigator implements IAppNavigator {

    private final Stage primaryStage;
    private final IEmbySessionService sessionService;
    private final IConfigurationService configService;
    private final IPreferenceService preferenceService;
    private final INotificationService notificationService;
    private final ILocalInteractionService localInteractionService;
    private final IItemRepository itemRepository;
    private final IStaticDataRepository staticDataRepository;
    private final IItemUpdateService itemUpdateService;
    private final IExternalDataService externalDataService;

    // Hằng số để lưu trữ vị trí và kích thước của dialog "Add Tag"
    private static final String KEY_ADD_TAG_DIALOG_X = "addTagDialogX";
    private static final String KEY_ADD_TAG_DIALOG_Y = "addTagDialogY";
    private static final String KEY_ADD_TAG_DIALOG_WIDTH = "addTagDialogWidth";
    private static final String KEY_ADD_TAG_DIALOG_HEIGHT = "addTagDialogHeight";

    // Hằng số để lưu trữ vị trí và kích thước của cửa sổ "Pop-out" (UR-50)
    private static final String KEY_DIALOG_WIDTH = "popOutDialogWidth";
    private static final String KEY_DIALOG_HEIGHT = "popOutDialogHeight";
    private static final String KEY_DIALOG_X = "popOutDialogX";
    private static final String KEY_DIALOG_Y = "popOutDialogY";

    // Tham chiếu đến cửa sổ pop-out chi tiết (UR-50)
    private Stage detailDialog;
    // Tham chiếu đến các ViewModel chính, được chia sẻ giữa MainView và Pop-out
    private IItemGridViewModel mainGridVM;
    private IItemDetailViewModel mainDetailVM;
    // Tham chiếu đến MainController để gọi ẩn/hiện cột 3 khi pop-out
    private MainController mainControllerRef;


    /**
     * Khởi tạo AppNavigator với tất cả các dependencies của ứng dụng (DI).
     *
     * @param primaryStage         Stage chính của ứng dụng.
     * @param sessionService       Dịch vụ quản lý phiên đăng nhập.
     * @param configService        Dịch vụ đọc file cấu hình.
     * @param preferenceService    Dịch vụ lưu trữ cài đặt người dùng.
     * @param notificationService  Dịch vụ hiển thị thông báo.
     * @param localInteractionService Dịch vụ tương tác file cục bộ.
     * @param itemRepository       Dịch vụ ĐỌC dữ liệu item.
     * @param staticDataRepository Dịch vụ ĐỌC dữ liệu tĩnh (gợi ý).
     * @param itemUpdateService    Dịch vụ GHI dữ liệu item.
     * @param externalDataService  Dịch vụ gọi API bên ngoài.
     */
    public AppNavigator(
            Stage primaryStage,
            IEmbySessionService sessionService,
            IConfigurationService configService,
            IPreferenceService preferenceService,
            INotificationService notificationService,
            ILocalInteractionService localInteractionService,
            IItemRepository itemRepository,
            IStaticDataRepository staticDataRepository,
            IItemUpdateService itemUpdateService,
            IExternalDataService externalDataService
    ) {
        this.primaryStage = primaryStage;
        this.sessionService = sessionService;
        this.configService = configService;
        this.preferenceService = preferenceService;
        this.notificationService = notificationService;
        this.localInteractionService = localInteractionService;
        this.itemRepository = itemRepository;
        this.staticDataRepository = staticDataRepository;
        this.itemUpdateService = itemUpdateService;
        this.externalDataService = externalDataService;
    }

    @Override
    public void initialize(Stage stage) {
        // Phương thức này không được sử dụng vì Stage được tiêm qua constructor.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showLogin() {
        try {
            // 1. Khởi tạo ViewModel cho Login
            ILoginViewModel viewModel = new LoginViewModel(sessionService, this, configService);

            // 2. Lắng nghe kết quả đăng nhập thành công từ ViewModel
            viewModel.loginSuccessProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) showMain(); // Nếu thành công, gọi showMain()
            });

            // 3. Khởi tạo Controller và tiêm ViewModel
            LoginController controller = new LoginController(viewModel, configService);

            // 4. Tải FXML và hiển thị Scene
            loadScene("LoginView.fxml", configService.getString("mainApp", "loginTitle"), controller, primaryStage, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showMain() {
        try {
            // Đây là "Composition Root" cho màn hình chính.

            // 1. Khởi tạo MainViewModel (điều phối chung)
            MainViewModel mainViewModel = new MainViewModel(
                    sessionService,
                    this,
                    notificationService,
                    configService
            );

            // 2. Khởi tạo các ViewModel con cho 3 cột
            ILibraryTreeViewModel libraryTreeViewModel = new LibraryTreeViewModel(
                    itemRepository,
                    notificationService,
                    configService
            );

            IItemGridViewModel itemGridViewModel = new ItemGridViewModel(
                    itemRepository,
                    notificationService,
                    localInteractionService,
                    sessionService,
                    configService
            );

            IItemDetailViewModel itemDetailViewModel = new ItemDetailViewModel(
                    itemRepository,
                    itemUpdateService,
                    staticDataRepository,
                    externalDataService,
                    localInteractionService,
                    notificationService,
                    sessionService,
                    libraryTreeViewModel,
                    configService
            );

            // 3. Lưu lại tham chiếu đến các VM sẽ được chia sẻ (cho pop-out)
            this.mainGridVM = itemGridViewModel;
            this.mainDetailVM = itemDetailViewModel;

            // 4. Khởi tạo MainController và tiêm tất cả các VM
            MainController controller = new MainController(
                    mainViewModel,
                    libraryTreeViewModel,
                    itemGridViewModel,
                    itemDetailViewModel,
                    configService,
                    preferenceService,
                    notificationService,
                    this // Tiêm chính AppNavigator vào MainController
            );

            // 5. Tải FXML và hiển thị Scene
            loadScene("MainView.fxml", configService.getString("mainApp", "mainTitle"), controller, primaryStage, false);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AddTagResult showAddTagDialog(Stage ownerStage, SuggestionContext context) {
        try {
            URL fxmlUrl = MainApp.class.getResource("view/fxml/AddTagDialog.fxml");
            FXMLLoader loader = new FXMLLoader(fxmlUrl);

            // 1. Khởi tạo ViewModel cho Dialog
            IAddTagViewModel viewModel = new AddTagViewModel(
                    staticDataRepository,
                    itemRepository,
                    configService
            );
            viewModel.setContext(context); // Thiết lập bối cảnh (VD: đang thêm TAG hay STUDIO)

            // 2. Tải FXML
            Parent root = loader.load();
            AddTagDialogController controller = loader.getController();

            // 3. Cấu hình Stage (Cửa sổ)
            Stage dialogStage = new Stage();
            dialogStage.titleProperty().bind(viewModel.titleProperty()); // Title động
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(ownerStage);

            // 4. Khôi phục vị trí/kích thước dialog đã lưu
            double defaultWidth = 550.0;
            double defaultHeight = 700.0;
            double savedX = preferenceService.getDouble(KEY_ADD_TAG_DIALOG_X, -1);
            double savedY = preferenceService.getDouble(KEY_ADD_TAG_DIALOG_Y, -1);
            double savedWidth = preferenceService.getDouble(KEY_ADD_TAG_DIALOG_WIDTH, defaultWidth);
            double savedHeight = preferenceService.getDouble(KEY_ADD_TAG_DIALOG_HEIGHT, defaultHeight);

            if (savedX != -1 && savedY != -1) {
                dialogStage.setX(savedX);
                dialogStage.setY(savedY);
            }
            dialogStage.setWidth(savedWidth);
            dialogStage.setHeight(savedHeight);

            // 5. Lắng nghe sự kiện đóng để lưu vị trí/kích thước
            dialogStage.setOnCloseRequest(e -> {
                try {
                    preferenceService.putDouble(KEY_ADD_TAG_DIALOG_X, dialogStage.getX());
                    preferenceService.putDouble(KEY_ADD_TAG_DIALOG_Y, dialogStage.getY());
                    preferenceService.putDouble(KEY_ADD_TAG_DIALOG_WIDTH, dialogStage.getWidth());
                    preferenceService.putDouble(KEY_ADD_TAG_DIALOG_HEIGHT, dialogStage.getHeight());
                    preferenceService.flush();
                } catch (Exception ex) {
                    System.err.println("Lỗi khi lưu vị trí/kích thước AddTagDialog: " + ex.getMessage());
                }
            });

            // 6. Cài đặt Scene và CSS
            Scene scene = new Scene(root);
            scene.getStylesheets().addAll(ownerStage.getScene().getStylesheets());
            dialogStage.setScene(scene);

            // 7. Tiêm ViewModel, Stage, và ConfigService vào Controller
            controller.setViewModel(viewModel, dialogStage, configService);

            // 8. Hiển thị dialog và chờ người dùng nhập
            dialogStage.showAndWait();

            // 9. Trả về kết quả (AddTagResult) từ ViewModel
            return viewModel.getResult();

        } catch (IOException e) {
            e.printStackTrace();
            notificationService.showStatus(configService.getString("exceptions", "dialogError", e.getMessage()));
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showPopOutDetail(BaseItemDto item, Object mainController) {
        if (item == null) return;

        try {
            // Lưu tham chiếu đến MainController để ẩn/hiện cột 3
            if (mainController instanceof MainController) {
                this.mainControllerRef = (MainController) mainController;
            }

            // Chỉ tạo cửa sổ pop-out nếu nó chưa tồn tại
            if (detailDialog == null) {

                // 1. Tải FXML
                URL fxmlUrl = MainApp.class.getResource("view/fxml/ItemDetailView.fxml");
                FXMLLoader loader = new FXMLLoader(fxmlUrl);

                // 2. Tạo một Controller MỚI cho cửa sổ pop-out
                ItemDetailController controller = new ItemDetailController();
                loader.setController(controller);
                Parent root = loader.load();

                // 3. Tiêm ViewModel CHI TIẾT (đã được chia sẻ) vào Controller MỚI
                if (this.mainDetailVM == null) {
                    notificationService.showStatus(configService.getString("exceptions", "vmNotInitialized"));
                    return;
                }
                controller.setViewModel(this.mainDetailVM, configService);

                // 4. Cấu hình Stage
                detailDialog = new Stage();
                detailDialog.setTitle(configService.getString("itemDetailView", "popOutTitle"));
                detailDialog.initModality(Modality.NONE); // Không khóa cửa sổ chính

                // 5. Khôi phục kích thước/vị trí đã lưu (UR-50)
                double defaultWidth = 1000, defaultHeight = 800;
                double savedWidth = preferenceService.getDouble(KEY_DIALOG_WIDTH, defaultWidth);
                double savedHeight = preferenceService.getDouble(KEY_DIALOG_HEIGHT, defaultHeight);
                double savedX = preferenceService.getDouble(KEY_DIALOG_X, -1);
                double savedY = preferenceService.getDouble(KEY_DIALOG_Y, -1);

                detailDialog.setWidth(savedWidth);
                detailDialog.setHeight(savedHeight);
                if (savedX != -1 && savedY != -1) detailDialog.setX(savedX);

                // 6. Cài đặt Scene và CSS
                Scene scene = new Scene(root);
                URL cssUrl = MainApp.class.getResource("styles.css");
                if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());
                detailDialog.setScene(scene);

                // 7. Đăng ký phím tắt cho Scene MỚI này
                registerHotkeysForScene(
                        scene,
                        null, // Không có MainController
                        controller,     // Controller CỘT 3 của pop-out
                        null, // Không có GridController
                        this.mainGridVM // VM CỘT 2 (chung, để điều hướng Back/Fwd)
                );

                // 8. Xử lý sự kiện "Add Tag" (Enter) cho cửa sổ pop-out (UR-13)
                final IItemDetailViewModel sharedVM = this.mainDetailVM;
                final Stage stage = detailDialog;

                sharedVM.addChipCommandProperty().addListener((obs, oldCtx, newCtx) -> {
                    // Chỉ kích hoạt nếu cửa sổ pop-up đang focus
                    if (newCtx != null && stage.isFocused()) {
                        AddTagResult result = showAddTagDialog(stage, newCtx);

                        // Trả focus về cửa sổ pop-up sau khi dialog đóng
                        Platform.runLater(root::requestFocus);

                        sharedVM.processAddTagResult(result, newCtx);
                        sharedVM.clearAddChipCommand();
                    }
                });

                // 9. Lưu kích thước/vị trí khi đóng (UR-50)
                detailDialog.setOnCloseRequest(e -> {
                    // Hiện lại cột 3 ở cửa sổ chính
                    if (this.mainControllerRef != null) {
                        this.mainControllerRef.showDetailColumn();
                        this.mainControllerRef = null;
                    }
                    // Lưu cài đặt cửa sổ pop-out
                    preferenceService.putDouble(KEY_DIALOG_WIDTH, detailDialog.getWidth());
                    preferenceService.putDouble(KEY_DIALOG_HEIGHT, detailDialog.getHeight());
                    preferenceService.putDouble(KEY_DIALOG_X, detailDialog.getX());
                    preferenceService.putDouble(KEY_DIALOG_Y, detailDialog.getY());
                    preferenceService.flush();
                    detailDialog = null; // Hủy stage để lần sau tạo lại
                });
            }

            // Yêu cầu MainController ẩn cột 3
            if (this.mainControllerRef != null) {
                this.mainControllerRef.hideDetailColumn();
            }

            // Hiển thị cửa sổ pop-out
            detailDialog.show();
            detailDialog.toFront();

        } catch (IOException e) {
            e.printStackTrace();
            notificationService.showStatus(configService.getString("exceptions", "popupError", e.getMessage()));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void closePopOutDetail() {
        if (detailDialog != null) {
            detailDialog.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerHotkeys(Scene scene, MainController mainController, ItemDetailController detailController, ItemGridController gridController, IItemGridViewModel gridViewModel) {
        if (scene == null) return;

        // (UR-14) Đăng ký điều hướng bằng nút Back/Forward của chuột
        if (gridViewModel != null) {
            registerMouseNavigation(scene, gridViewModel);
        }

        // (UR-13) Đăng ký các phím tắt
        registerHotkeysForScene(scene, mainController, detailController, gridController, gridViewModel);
    }

    /**
     * Đăng ký sự kiện click chuột cho điều hướng Back/Forward (UR-14).
     *
     * @param scene         Scene để lắng nghe.
     * @param gridViewModel ViewModel của lưới (chứa logic Back/Forward).
     */
    private void registerMouseNavigation(Scene scene, IItemGridViewModel gridViewModel) {
        scene.setOnMouseClicked((MouseEvent event) -> {
            // Xử lý nút Back của chuột
            if (event.getButton() == MouseButton.BACK) {
                event.consume();
                if (gridViewModel.canGoBackProperty().get()) {
                    gridViewModel.navigateBack();
                }
            }
            // Xử lý nút Forward của chuột
            if (event.getButton() == MouseButton.FORWARD) {
                event.consume();
                if (gridViewModel.canGoForwardProperty().get()) {
                    gridViewModel.navigateForward();
                }
            }
        });
    }

    /**
     * Đăng ký các phím tắt (UR-13) cho một Scene cụ thể.
     *
     * @param scene            Scene (của MainView hoặc Pop-out)
     * @param mainController   Controller chính (chỉ có ở MainView)
     * @param detailController Controller chi tiết (có ở cả 2)
     * @param gridController   Controller lưới (chỉ có ở MainView)
     * @param gridViewModel    ViewModel lưới (luôn được chia sẻ)
     */
    private void registerHotkeysForScene(Scene scene,
                                         MainController mainController,
                                         ItemDetailController detailController,
                                         ItemGridController gridController,
                                         IItemGridViewModel gridViewModel)
    {
        if (scene == null) return;

        // Phím ENTER (Lặp lại Add Tag - UR-13)
        // Luôn gọi VM chi tiết (vì nó được chia sẻ)
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER && !event.isShortcutDown() && !event.isAltDown() && !event.isShiftDown()) {
                // Chỉ kích hoạt nếu focus không nằm trên một control (như Button, TextField)
                Node focusedNode = scene.getFocusOwner();
                boolean isBlockingControl = focusedNode instanceof javafx.scene.control.TextInputControl ||
                        focusedNode instanceof javafx.scene.control.Button ||
                        focusedNode instanceof javafx.scene.control.ToggleButton;

                if (focusedNode == null || !isBlockingControl) {
                    if (this.mainDetailVM != null) {
                        this.mainDetailVM.repeatAddChipCommand();
                        event.consume();
                    }
                }
            }
        });

        // Phím CMD+S (Lưu - UR-13)
        // Luôn gọi VM chi tiết
        final KeyCombination saveShortcut = new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN);
        scene.getAccelerators().put(saveShortcut, () -> {
            if (this.mainDetailVM != null) {
                // Chỉ lưu nếu có thay đổi (dirty)
                if (this.mainDetailVM.isDirtyProperty().get()) {
                    this.mainDetailVM.saveChangesCommand();
                }
            }
        });

        // Phím CMD+ENTER (Phát video - UR-13)
        // Phím này phụ thuộc vào ngữ cảnh (scene nào đang active)
        final KeyCombination playShortcut = new KeyCodeCombination(KeyCode.ENTER, KeyCombination.SHORTCUT_DOWN);
        if (gridController != null) {
            // Nếu ở Scene chính -> gọi GridController (phát item Cột 2)
            scene.getAccelerators().put(playShortcut, gridController::playSelectedItem);
        } else if (detailController != null) {
            // Nếu ở Scene Pop-out -> gọi DetailController (phát item Cột 3)
            scene.getAccelerators().put(playShortcut, detailController::handlePlayHotkey);
        }

        // Phím tắt của Cột 2 (Grid)
        // Luôn gọi VM Lưới (vì nó được chia sẻ)
        if (this.mainGridVM != null) {
            // CMD+N (Chọn item tiếp theo)
            final KeyCombination nextShortcut = new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN);
            scene.getAccelerators().put(nextShortcut, this.mainGridVM::selectNextItem);

            // CMD+P (Chọn item trước đó)
            final KeyCombination prevShortcut = new KeyCodeCombination(KeyCode.P, KeyCombination.SHORTCUT_DOWN);
            scene.getAccelerators().put(prevShortcut, this.mainGridVM::selectPreviousItem);

            // CMD+SHIFT+N (Chọn và Phát item tiếp theo)
            final KeyCombination nextAndPlayShortcut = new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN);
            scene.getAccelerators().put(nextAndPlayShortcut, this.mainGridVM::selectAndPlayNextItem);

            // CMD+SHIFT+P (Chọn và Phát item trước đó)
            final KeyCombination prevAndPlayShortcut = new KeyCodeCombination(KeyCode.P, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN);
            scene.getAccelerators().put(prevAndPlayShortcut, this.mainGridVM::selectAndPlayPreviousItem);

            // Phím Back (ALT+LEFT / CMD+[)
            final KeyCombination backShortcutWin = new KeyCodeCombination(KeyCode.LEFT, KeyCombination.ALT_DOWN);
            scene.getAccelerators().put(backShortcutWin, () -> {
                if (this.mainGridVM.canGoBackProperty().get()) this.mainGridVM.navigateBack();
            });
            final KeyCombination backShortcutMac = new KeyCodeCombination(KeyCode.OPEN_BRACKET, KeyCombination.SHORTCUT_DOWN);
            scene.getAccelerators().put(backShortcutMac, () -> {
                if (this.mainGridVM.canGoBackProperty().get()) this.mainGridVM.navigateBack();
            });

            // Phím Forward (ALT+RIGHT / CMD+])
            final KeyCombination forwardShortcutWin = new KeyCodeCombination(KeyCode.RIGHT, KeyCombination.ALT_DOWN);
            scene.getAccelerators().put(forwardShortcutWin, () -> {
                if (this.mainGridVM.canGoForwardProperty().get()) this.mainGridVM.navigateForward();
            });
            final KeyCombination forwardShortcutMac = new KeyCodeCombination(KeyCode.CLOSE_BRACKET, KeyCombination.SHORTCUT_DOWN);
            scene.getAccelerators().put(forwardShortcutMac, () -> {
                if (this.mainGridVM.canGoForwardProperty().get()) this.mainGridVM.navigateForward();
            });
        }
    }


    /**
     * Tải FXML, tiêm Controller, và hiển thị Scene lên Stage.
     *
     * @param fxmlFile         Tên tệp FXML (ví dụ: "LoginView.fxml").
     * @param title            Tiêu đề cửa sổ.
     * @param controllerInstance Instance của Controller đã được khởi tạo (với VM).
     * @param stage            Stage (cửa sổ) để hiển thị Scene.
     * @param isDialog         Cờ cho biết đây có phải là dialog (để load lại CSS).
     * @throws IOException Nếu không tìm thấy FXML.
     */
    private void loadScene(String fxmlFile, String title, Object controllerInstance, Stage stage, boolean isDialog) throws IOException {
        URL fxmlUrl = MainApp.class.getResource("view/fxml/" + fxmlFile);
        if (fxmlUrl == null) {
            throw new IOException(configService.getString("exceptions", "fxmlNotFound", fxmlFile));
        }

        // 1. Khởi tạo FXMLLoader
        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        if (controllerInstance != null) {
            // 2. Tiêm Controller (thay vì để FXML tự tạo)
            loader.setController(controllerInstance);
        }
        Parent root = loader.load();

        // 3. Cấu hình Scene
        Scene scene = stage.getScene();
        if (scene == null) {
            // Nếu Scene chưa tồn tại (lần đầu khởi chạy), tạo Scene mới với kích thước đã lưu
            double width = preferenceService.getDouble("windowWidth", 2000);
            double height = preferenceService.getDouble("windowHeight", 1400);
            scene = new Scene(root, width, height);
        } else {
            // Nếu Scene đã tồn tại, chỉ thay đổi nội dung (root)
            scene.setRoot(root);
        }

        // 4. Áp dụng CSS
        URL cssUrl = MainApp.class.getResource("styles.css");
        if (cssUrl != null) {
            if (scene.getStylesheets().isEmpty() || isDialog) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }
        } else {
            System.err.println("Không tìm thấy file styles.css");
        }

        // 5. Hiển thị
        stage.setTitle(title);
        stage.setScene(scene);
        stage.show();
    }
}