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
 * Triển khai (Implementation) của IAppNavigator.
 * (Cập nhật GĐ 11: Thêm logic lưu/khôi phục vị trí AddTagDialog).
 */
public class AppNavigator implements IAppNavigator {

    // ... (Các trường services giữ nguyên) ...
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


    // (MỚI) Các hằng số để lưu vị trí dialog,
    // (copy từ ItemDetailController.java cũ)
    private static final String KEY_ADD_TAG_DIALOG_X = "addTagDialogX";
    private static final String KEY_ADD_TAG_DIALOG_Y = "addTagDialogY";
    private static final String KEY_ADD_TAG_DIALOG_WIDTH = "addTagDialogWidth";
    private static final String KEY_ADD_TAG_DIALOG_HEIGHT = "addTagDialogHeight";

    private Stage detailDialog; // Cho UR-50
    private IItemDetailViewModel popOutDetailViewModel;
    private IItemGridViewModel mainGridVM;
    private IItemDetailViewModel mainDetailVM;
    private MainController mainControllerRef;

    // Các hằng số để lưu vị trí/kích thước cửa sổ pop-out
    private static final String KEY_DIALOG_WIDTH = "popOutDialogWidth";
    private static final String KEY_DIALOG_HEIGHT = "popOutDialogHeight";
    private static final String KEY_DIALOG_X = "popOutDialogX";
    private static final String KEY_DIALOG_Y = "popOutDialogY";

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

    // ... (hàm initialize, showLogin, showMain giữ nguyên) ...

    @Override
    public void initialize(Stage stage) {
        // (Không cần, Stage đã được inject)
    }

    @Override
    public void showLogin() {
        try {
            ILoginViewModel viewModel = new LoginViewModel(sessionService, this, configService);
            viewModel.loginSuccessProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) showMain();
            });
            LoginController controller = new LoginController(viewModel, configService);
            loadScene("LoginView.fxml", configService.getString("mainApp", "loginTitle"), controller, primaryStage, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void showMain() {
        try {
            // 1. Tạo MainViewModel
            MainViewModel mainViewModel = new MainViewModel(
                    sessionService,
                    this,
                    notificationService,
                    configService
            );

            // 2. Tạo các ViewModel con
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

            this.mainGridVM = itemGridViewModel;
            this.mainDetailVM = itemDetailViewModel;

            // 3. Tạo MainController và tiêm
            MainController controller = new MainController(
                    mainViewModel,
                    libraryTreeViewModel,
                    itemGridViewModel,
                    itemDetailViewModel,
                    configService,
                    preferenceService,
                    notificationService,
                    this // Tiêm IAppNavigator
            );

            // 4. Tải FXML
            loadScene("MainView.fxml", configService.getString("mainApp", "mainTitle"), controller, primaryStage, false);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * (SỬA ĐỔI GĐ 11) Triển khai hiển thị dialog Add Tag (UR-35).
     */
    @Override
    public AddTagResult showAddTagDialog(Stage ownerStage, SuggestionContext context) {
        try {
            URL fxmlUrl = MainApp.class.getResource("view/fxml/AddTagDialog.fxml");
            FXMLLoader loader = new FXMLLoader(fxmlUrl);

            // 1. Tạo ViewModel
            IAddTagViewModel viewModel = new AddTagViewModel(
                    staticDataRepository,
                    itemRepository,
                    configService
            );
            viewModel.setContext(context);

            // 2. Tải FXML
            Parent root = loader.load();
            AddTagDialogController controller = loader.getController();

            // 3. Cấu hình Stage (Cửa sổ)
            Stage dialogStage = new Stage();
            dialogStage.titleProperty().bind(viewModel.titleProperty());
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(ownerStage);

            // --- (MỚI) KHÔI PHỤC VỊ TRÍ/KÍCH THƯỚC (Logic từ ItemDetailController.java cũ) ---
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

            // --- (MỚI) LƯU VỊ TRÍ/KÍCH THƯỚC KHI ĐÓNG (Logic từ ItemDetailController.java cũ) ---
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
            // --- KẾT THÚC THÊM MỚI ---

            Scene scene = new Scene(root);
            scene.getStylesheets().addAll(ownerStage.getScene().getStylesheets());
            dialogStage.setScene(scene);

            // 4. Tiêm ViewModel, Stage, và ConfigService vào Controller
            controller.setViewModel(viewModel, dialogStage, configService);

            // 5. Hiển thị và chờ
            dialogStage.showAndWait();

            // 6. Trả về kết quả từ ViewModel
            return viewModel.getResult();

        } catch (IOException e) {
            e.printStackTrace();
            notificationService.showStatus("Lỗi: Không thể mở dialog. " + e.getMessage());
            return null;
        }
    }

    @Override
    public void showPopOutDetail(BaseItemDto item, Object mainController) {
        if (item == null) return;

        try {
            // Lưu tham chiếu đến MainController
            if (mainController instanceof MainController) {
                this.mainControllerRef = (MainController) mainController;
            }

            // Chỉ tạo dialog nếu nó chưa tồn tại
            if (detailDialog == null) {

                // 2. Tải FXML
                URL fxmlUrl = MainApp.class.getResource("view/fxml/ItemDetailView.fxml");
                FXMLLoader loader = new FXMLLoader(fxmlUrl);

                // 3. Tạo Controller MỚI
                ItemDetailController controller = new ItemDetailController();
                loader.setController(controller);
                Parent root = loader.load();

                // 4. Tiêm VM vào Controller MỚI
                if (this.mainDetailVM == null) {
                    notificationService.showStatus("Lỗi: VM chi tiết chính chưa được khởi tạo.");
                    return;
                }
                controller.setViewModel(this.mainDetailVM, configService);

                // 5. Cấu hình Stage
                detailDialog = new Stage();
                detailDialog.setTitle(configService.getString("itemDetailView", "popOutTitle"));

                // Đặt Owner cho dialog
                // Điều này tự động đóng dialog khi cửa sổ chính (primaryStage) đóng.
                detailDialog.initOwner(primaryStage);
                detailDialog.initModality(Modality.NONE);

                // 6. Lấy kích thước/vị trí đã lưu (UR-50)
                double defaultWidth = 1000, defaultHeight = 800;
                double savedWidth = preferenceService.getDouble(KEY_DIALOG_WIDTH, defaultWidth);
                double savedHeight = preferenceService.getDouble(KEY_DIALOG_HEIGHT, defaultHeight);
                double savedX = preferenceService.getDouble(KEY_DIALOG_X, -1);
                double savedY = preferenceService.getDouble(KEY_DIALOG_Y, -1);

                detailDialog.setWidth(savedWidth);
                detailDialog.setHeight(savedHeight);
                if (savedX != -1 && savedY != -1) detailDialog.setX(savedX);

                // 7. Cài đặt Scene
                Scene scene = new Scene(root);
                URL cssUrl = MainApp.class.getResource("styles.css");
                if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());
                detailDialog.setScene(scene);

                registerHotkeysForScene(
                        scene,
                        null, // Không có MainController
                        controller,     // Controller CỘT 3 của pop-out
                        null, // Không có GridController
                        this.mainGridVM // VM CỘT 2 (chung)
                );

                // 8. Đăng ký hotkeys cho scene MỚI này (UR-13)
                // (Chỉ truyền vào các thành phần mà pop-out có)
                final IItemDetailViewModel sharedVM = this.mainDetailVM;
                final Stage stage = detailDialog;

                sharedVM.addChipCommandProperty().addListener((obs, oldCtx, newCtx) -> {
                    // (MỚI) Chỉ kích hoạt nếu cửa sổ POP-UP (stage) đang focus
                    if (newCtx != null && stage.isFocused()) {
                        // Gọi hàm showAddTagDialog của chính AppNavigator
                        AddTagResult result = showAddTagDialog(stage, newCtx);

                        // (SỬA LỖI FOCUS: Yêu cầu focus vào Nút gốc (root node) của Scene)
                        Platform.runLater(root::requestFocus);

                        // Gửi kết quả (hoặc null) trở lại ViewModel
                        sharedVM.processAddTagResult(result, newCtx);
                        sharedVM.clearAddChipCommand();
                    }
                });

                // 9. Lưu kích thước khi đóng (UR-50)
                detailDialog.setOnCloseRequest(e -> {
                    // YÊU CẦU 1: Hiện lại cột detail khi đóng dialog
                    if (this.mainControllerRef != null) {
                        this.mainControllerRef.showDetailColumn();
                        this.mainControllerRef = null; // Xóa tham chiếu
                    }
                    preferenceService.putDouble(KEY_DIALOG_WIDTH, detailDialog.getWidth());
                    preferenceService.putDouble(KEY_DIALOG_HEIGHT, detailDialog.getHeight());
                    preferenceService.putDouble(KEY_DIALOG_X, detailDialog.getX());
                    preferenceService.putDouble(KEY_DIALOG_Y, detailDialog.getY());
                    preferenceService.flush();
                    detailDialog = null; // Hủy stage
                });
            }

            // Hiện lại cột detail khi đóng dialog
            if (this.mainControllerRef != null) {
                this.mainControllerRef.showDetailColumn();
                this.mainControllerRef = null; // Xóa tham chiếu
            }

            detailDialog.show();
            detailDialog.toFront();

        } catch (IOException e) {
            e.printStackTrace();
            notificationService.showStatus("Lỗi mở pop-out: " + e.getMessage());
        }
    }

    @Override
    public void closePopOutDetail() {
        if (detailDialog != null) {
            detailDialog.close();
            // (Listener OnCloseRequest sẽ tự dọn dẹp)
        }
    }

    @Override
    public void registerHotkeys(Scene scene, MainController mainController, ItemDetailController detailController, ItemGridController gridController, IItemGridViewModel gridViewModel) {
        if (scene == null) return;

        // (UR-14) Chỉ đăng ký điều hướng chuột nếu có GridVM (tức là scene chính)
        if (gridViewModel != null) {
            registerMouseNavigation(scene, gridViewModel);
        }

        // (UR-13) Đăng ký phím tắt chung
        registerHotkeysForScene(scene, mainController, detailController, gridController, gridViewModel);
    }

    private void registerMouseNavigation(Scene scene, IItemGridViewModel gridViewModel) {
        scene.setOnMouseClicked((MouseEvent event) -> {
            if (event.getButton() == MouseButton.BACK) {
                event.consume();
                if (gridViewModel.canGoBackProperty().get()) {
                    gridViewModel.navigateBack();
                }
            }
            if (event.getButton() == MouseButton.FORWARD) {
                event.consume();
                if (gridViewModel.canGoForwardProperty().get()) {
                    gridViewModel.navigateForward();
                }
            }
        });
    }

    private void registerHotkeysForScene(Scene scene,
                                         MainController mainController,
                                         ItemDetailController detailController,
                                         ItemGridController gridController,
                                         IItemGridViewModel gridViewModel) // gridViewModel dùng cho Mouse Nav
    {
        if (scene == null) return;

        // --- ENTER (Repeat Add Tag) ---
        // Luôn gọi mainDetailVM (vì nó được chia sẻ)
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER && !event.isShortcutDown() && !event.isAltDown() && !event.isShiftDown()) {
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

        // --- CMD+S (Save) ---
        // Luôn gọi mainDetailVM (vì nó được chia sẻ)
        final KeyCombination saveShortcut = new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN);
        scene.getAccelerators().put(saveShortcut, () -> {
            if (this.mainDetailVM != null) {
                // Kiểm tra isDirty() trước khi lưu
                if (this.mainDetailVM.isDirtyProperty().get()) {
                    this.mainDetailVM.saveChangesCommand();
                }
            }
        });

        // --- CMD+ENTER (Play) ---
        // Phím này CẦN biết ngữ cảnh (Context-Aware)
        final KeyCombination playShortcut = new KeyCodeCombination(KeyCode.ENTER, KeyCombination.SHORTCUT_DOWN);
        if (gridController != null) {
            // Scene chính: Hotkey "Play" gọi Grid Controller (Cột 2)
            scene.getAccelerators().put(playShortcut, gridController::playSelectedItem);
        } else if (detailController != null) {
            // Scene Pop-up: Hotkey "Play" gọi Detail Controller (Cột 3)
            scene.getAccelerators().put(playShortcut, detailController::handlePlayHotkey);
        }

        // --- Phím tắt CỘT 2 (Grid) ---
        // Luôn gọi mainGridVM (Toàn cục)
        if (this.mainGridVM != null) {
            // CMD+N (Next)
            final KeyCombination nextShortcut = new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN);
            scene.getAccelerators().put(nextShortcut, this.mainGridVM::selectNextItem);

            // CMD+P (Previous)
            final KeyCombination prevShortcut = new KeyCodeCombination(KeyCode.P, KeyCombination.SHORTCUT_DOWN);
            scene.getAccelerators().put(prevShortcut, this.mainGridVM::selectPreviousItem);

            // CMD+SHIFT+N (Next and Play)
            final KeyCombination nextAndPlayShortcut = new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN);
            scene.getAccelerators().put(nextAndPlayShortcut, this.mainGridVM::selectAndPlayNextItem);

            // CMD+SHIFT+P (Previous and Play)
            final KeyCombination prevAndPlayShortcut = new KeyCodeCombination(KeyCode.P, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN);
            scene.getAccelerators().put(prevAndPlayShortcut, this.mainGridVM::selectAndPlayPreviousItem);

            // BACK/FORWARD (ALT+LEFT/RIGHT)
            final KeyCombination backShortcutWin = new KeyCodeCombination(KeyCode.LEFT, KeyCombination.ALT_DOWN);
            scene.getAccelerators().put(backShortcutWin, () -> {
                if (this.mainGridVM.canGoBackProperty().get()) this.mainGridVM.navigateBack();
            });
            final KeyCombination forwardShortcutWin = new KeyCodeCombination(KeyCode.RIGHT, KeyCombination.ALT_DOWN);
            scene.getAccelerators().put(forwardShortcutWin, () -> {
                if (this.mainGridVM.canGoForwardProperty().get()) this.mainGridVM.navigateForward();
            });

            // BACK/FORWARD (CMD+[ / ])
            final KeyCombination backShortcutMac = new KeyCodeCombination(KeyCode.OPEN_BRACKET, KeyCombination.SHORTCUT_DOWN);
            scene.getAccelerators().put(backShortcutMac, () -> {
                if (this.mainGridVM.canGoBackProperty().get()) this.mainGridVM.navigateBack();
            });
            final KeyCombination forwardShortcutMac = new KeyCodeCombination(KeyCode.CLOSE_BRACKET, KeyCombination.SHORTCUT_DOWN);
            scene.getAccelerators().put(forwardShortcutMac, () -> {
                if (this.mainGridVM.canGoForwardProperty().get()) this.mainGridVM.navigateForward();
            });
        }
    }


    /**
     * Helper nội bộ để tải FXML, tiêm Controller, và hiển thị Scene.
     */
    private void loadScene(String fxmlFile, String title, Object controllerInstance, Stage stage, boolean isDialog) throws IOException {
        URL fxmlUrl = MainApp.class.getResource("view/fxml/" + fxmlFile);
        if (fxmlUrl == null) {
            throw new IOException("Không tìm thấy FXML: " + fxmlFile);
        }

        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        if (controllerInstance != null) {
            loader.setController(controllerInstance);
        }
        Parent root = loader.load();

        Scene scene = stage.getScene();
        if (scene == null) {
            double width = preferenceService.getDouble("windowWidth", 2000);
            double height = preferenceService.getDouble("windowHeight", 1400);
            scene = new Scene(root, width, height);
        } else {
            scene.setRoot(root);
        }

        URL cssUrl = MainApp.class.getResource("styles.css");
        if (cssUrl != null) {
            if (scene.getStylesheets().isEmpty() || isDialog) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }
        } else {
            System.err.println("Không tìm thấy file styles.css");
        }

        stage.setTitle(title);
        stage.setScene(scene);
        stage.show();
    }
}