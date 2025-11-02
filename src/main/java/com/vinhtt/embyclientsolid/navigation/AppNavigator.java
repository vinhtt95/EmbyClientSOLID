package com.vinhtt.embyclientsolid.navigation;

import com.vinhtt.embyclientsolid.MainApp;
import com.vinhtt.embyclientsolid.core.IAppNavigator;
import com.vinhtt.embyclientsolid.core.IConfigurationService;
import com.vinhtt.embyclientsolid.core.IEmbySessionService;
import com.vinhtt.embyclientsolid.controller.LoginController;
import com.vinhtt.embyclientsolid.model.Tag;
import com.vinhtt.embyclientsolid.viewmodel.ILoginViewModel;
import com.vinhtt.embyclientsolid.viewmodel.impl.LoginViewModel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * Triển khai (Implementation) của IAppNavigator.
 * Chịu trách nhiệm tải FXML, cấu hình Controllers (thông qua DI) và quản lý Stages.
 *
 */
public class AppNavigator implements IAppNavigator {

    private Stage primaryStage;
    private Stage detailDialog; // Cho UR-50

    // Services được tiêm (DI) từ MainApp
    private final IEmbySessionService sessionService;
    private final IConfigurationService configService;
    // (Các services và factory khác sẽ được thêm vào đây khi cần)

    /**
     * Khởi tạo Navigator với các services cốt lõi.
     *
     * @param sessionService Service quản lý session.
     * @param configService  Service đọc config.
     */
    public AppNavigator(IEmbySessionService sessionService, IConfigurationService configService) {
        this.sessionService = sessionService;
        this.configService = configService;
    }

    @Override
    public void initialize(Stage primaryStage) {
        this.primaryStage = primaryStage;
        // (Logic tải/lưu kích thước cửa sổ sẽ ở Giai đoạn 7)
    }

    @Override
    public void showLogin() {
        try {
            // 1. Tạo ViewModel
            ILoginViewModel viewModel = new LoginViewModel(sessionService, this, configService);

            // 2. Lắng nghe tín hiệu đăng nhập thành công
            viewModel.loginSuccessProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    showMain(); // Điều hướng đến màn hình chính
                }
            });

            // 3. Tạo Controller và tiêm (inject) ViewModel
            LoginController controller = new LoginController(viewModel, configService);

            // 4. Tải FXML và tiêm Controller
            String fxmlFile = "LoginView.fxml";
            String title = configService.getString("mainApp", "loginTitle");
            loadScene(fxmlFile, title, controller);

        } catch (IOException e) {
            e.printStackTrace();
            // (Ở đây có thể gọi INotificationService để báo lỗi nghiêm trọng)
        }
    }

    @Override
    public void showMain() {
        // (Logic tải FXML, tiêm ViewModels, và hiển thị sẽ ở Giai đoạn 7)
        System.out.println("AppNavigator: Điều hướng đến MainView (Chưa triển khai FXML)");

        // --- Logic Placeholder cho Giai đoạn 6 ---
        // (Chúng ta sẽ thay thế bằng việc tải MainView.fxml ở Giai đoạn 7)
        try {
            Label placeholder = new Label("Đăng nhập thành công! MainView (Giai đoạn 7) sẽ ở đây.");
            StackPane root = new StackPane(placeholder);
            Scene scene = new Scene(root, 1280, 800);

            // Tải CSS
            URL cssUrl = MainApp.class.getResource("styles.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            primaryStage.setTitle(configService.getString("mainApp", "mainTitle"));
            primaryStage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // --- Kết thúc Placeholder ---
    }

    @Override
    public Tag showAddTagDialog(Stage ownerStage) {
        // (Logic tải FXML, tiêm ViewModel, và hiển thị dialog sẽ ở Giai đoạn 11)
        System.out.println("AppNavigator: Hiển thị AddTagDialog (Chưa triển khai FXML)");
        return null; // Trả về null
    }

    @Override
    public void showPopOutDetail() {
        // (Logic tải FXML, tiêm ViewModel, và hiển thị Stage mới sẽ ở Giai đoạn 12)
        System.out.println("AppNavigator: Hiển thị PopOutDetail (Chưa triển khai FXML)");
        if (detailDialog == null) {
            // Tạo Stage mới
            detailDialog = new Stage();
            // Cấu hình (tải FXML, Scene, v.v.)
            // ...
        }
        detailDialog.show();
        detailDialog.toFront();
    }

    @Override
    public void closePopOutDetail() {
        if (detailDialog != null) {
            detailDialog.close();
        }
    }

    /**
     * Helper nội bộ để tải FXML, tiêm Controller, và hiển thị Scene.
     */
    private void loadScene(String fxmlFile, String title, Object controllerInstance) throws IOException {
        // Giả định FXML nằm trong /com/vinhtt/embyclientsolid/view/fxml/
        URL fxmlUrl = MainApp.class.getResource("view/fxml/" + fxmlFile);
        if (fxmlUrl == null) {
            throw new IOException("Không tìm thấy FXML: " + fxmlFile);
        }

        FXMLLoader loader = new FXMLLoader(fxmlUrl);

        // Tiêm Controller (thay vì để FXML tự tạo)
        loader.setController(controllerInstance);

        Parent root = loader.load();

        Scene scene = new Scene(root);

        // Tải CSS
        URL cssUrl = MainApp.class.getResource("styles.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        } else {
            System.err.println("Không tìm thấy file styles.css");
        }

        primaryStage.setTitle(title);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}