package com.vinhtt.embyclientsolid.navigation;

import com.vinhtt.embyclientsolid.MainApp;
import com.vinhtt.embyclientsolid.controller.AddTagDialogController;
import com.vinhtt.embyclientsolid.core.*;
import com.vinhtt.embyclientsolid.controller.LoginController;
import com.vinhtt.embyclientsolid.controller.MainController;
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
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

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
                    notificationService
            );

            IItemGridViewModel itemGridViewModel = new ItemGridViewModel(
                    itemRepository,
                    notificationService,
                    localInteractionService,
                    sessionService
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
                    itemRepository
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
    public void showPopOutDetail() {
        // (Giai đoạn 12)
    }

    @Override
    public void closePopOutDetail() {
        // (Giai đoạn 12)
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