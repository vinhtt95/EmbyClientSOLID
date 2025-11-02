package com.vinhtt.embyclientsolid.navigation;

import com.vinhtt.embyclientsolid.MainApp;
import com.vinhtt.embyclientsolid.core.*;
import com.vinhtt.embyclientsolid.controller.LoginController;
import com.vinhtt.embyclientsolid.controller.MainController;
import com.vinhtt.embyclientsolid.data.IExternalDataService;
import com.vinhtt.embyclientsolid.data.IItemRepository;
import com.vinhtt.embyclientsolid.data.IItemUpdateService;
import com.vinhtt.embyclientsolid.data.IStaticDataRepository;
import com.vinhtt.embyclientsolid.model.Tag;
import com.vinhtt.embyclientsolid.viewmodel.IItemDetailViewModel;
import com.vinhtt.embyclientsolid.viewmodel.IItemGridViewModel;
import com.vinhtt.embyclientsolid.viewmodel.ILibraryTreeViewModel;
import com.vinhtt.embyclientsolid.viewmodel.ILoginViewModel;
import com.vinhtt.embyclientsolid.viewmodel.impl.ItemDetailViewModel;
import com.vinhtt.embyclientsolid.viewmodel.impl.ItemGridViewModel;
import com.vinhtt.embyclientsolid.viewmodel.impl.LibraryTreeViewModel;
import com.vinhtt.embyclientsolid.viewmodel.impl.LoginViewModel;
import com.vinhtt.embyclientsolid.viewmodel.impl.MainViewModel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * Triển khai (Implementation) của IAppNavigator.
 * (Cập nhật Giai đoạn 8, 9, 10: Tiêm (inject) đầy đủ service vào các ViewModel).
 * (Sửa lỗi: Tiêm INotificationService vào MainController).
 */
public class AppNavigator implements IAppNavigator {

    private final Stage primaryStage;

    // --- Services (DI từ MainApp) ---
    private final IEmbySessionService sessionService;
    private final IConfigurationService configService;
    private final IPreferenceService preferenceService;
    private final INotificationService notificationService;
    private final ILocalInteractionService localInteractionService;
    private final IItemRepository itemRepository;
    private final IStaticDataRepository staticDataRepository;
    private final IItemUpdateService itemUpdateService;
    private final IExternalDataService externalDataService;

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
            loadScene("LoginView.fxml", configService.getString("mainApp", "loginTitle"), controller, primaryStage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void showMain() {
        try {
            // 1. Tạo MainViewModel (Điều phối chính)
            MainViewModel mainViewModel = new MainViewModel(
                    sessionService,
                    this,
                    notificationService,
                    configService
            );

            // 2. Tạo các ViewModel con (Tiêm đầy đủ dependencies)
            ILibraryTreeViewModel libraryTreeViewModel = new LibraryTreeViewModel(
                    itemRepository,
                    notificationService
            );

            IItemGridViewModel itemGridViewModel = new ItemGridViewModel(
                    itemRepository,
                    notificationService,
                    localInteractionService // Cần cho PlayCommand (UR-27)
            );

            IItemDetailViewModel itemDetailViewModel = new ItemDetailViewModel(
                    itemRepository,
                    itemUpdateService,
                    staticDataRepository,
                    externalDataService,
                    localInteractionService,
                    notificationService,
                    sessionService // Cần cho build Image URL
            );

            // 3. Tạo MainController (View-Coordinator) và tiêm MỌI THỨ
            MainController controller = new MainController(
                    mainViewModel,
                    libraryTreeViewModel,
                    itemGridViewModel,
                    itemDetailViewModel,
                    configService,
                    preferenceService,
                    notificationService // <-- SỬA LỖI 1: Tiêm INotificationService
            );

            // 4. Tải FXML và tiêm Controller
            loadScene("MainView.fxml", configService.getString("mainApp", "mainTitle"), controller, primaryStage);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Tag showAddTagDialog(Stage ownerStage) {
        // (Giai đoạn 11)
        return null;
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
    private void loadScene(String fxmlFile, String title, Object controllerInstance, Stage stage) throws IOException {
        URL fxmlUrl = MainApp.class.getResource("view/fxml/" + fxmlFile);
        if (fxmlUrl == null) {
            throw new IOException("Không tìm thấy FXML: " + fxmlFile);
        }

        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        loader.setController(controllerInstance);
        Parent root = loader.load();

        Scene scene = stage.getScene();
        if (scene == null) {
            double width = preferenceService.getDouble("windowWidth", 2000);
            double height = preferenceService.getDouble("windowHeight", 1400);
            scene = new Scene(root, width, height);
        } else {
            scene.setRoot(root);
        }

        // Tải CSS
        URL cssUrl = MainApp.class.getResource("styles.css");
        if (cssUrl != null) {
            // (Đảm bảo chỉ add 1 lần)
            if (scene.getStylesheets().isEmpty()) {
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