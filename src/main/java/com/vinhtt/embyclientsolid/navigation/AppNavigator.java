package com.vinhtt.embyclientsolid.navigation;

import com.vinhtt.embyclientsolid.core.IAppNavigator;
import com.vinhtt.embyclientsolid.model.Tag;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * Triển khai (Implementation) của IAppNavigator.
 * Chịu trách nhiệm tải FXML, cấu hình Controllers (thông qua DI) và quản lý Stages.
 *
 * LƯU Ý GIAI ĐOẠN 5:
 * Lớp này hiện tại chỉ là khung xương.
 * Ở các giai đoạn sau (6-12), nó sẽ cần được tiêm (inject)
 * các Factory hoặc Provider (ví dụ: `ILoginViewModelFactory`)
 * để có thể tiêm ViewModel vào Controller trước khi hiển thị Scene.
 */
public class AppNavigator implements IAppNavigator {

    private Stage primaryStage;
    private Stage detailDialog; // Cho UR-50

    // Các service khác (ví dụ: IConfigurationService, IPreferenceService,
    // các ViewModel Factory) sẽ được tiêm (inject) vào đây ở các giai đoạn sau
    // để cấu hình controllers.

    /**
     * Khởi tạo Navigator.
     * (Trong hệ thống DI hoàn chỉnh, các factory sẽ được tiêm ở đây).
     */
    public AppNavigator() {
        // Ví dụ: this.loginViewModelFactory = loginViewModelFactory;
    }

    @Override
    public void initialize(Stage primaryStage) {
        this.primaryStage = primaryStage;
        // Logic cấu hình primaryStage (ví dụ: lưu/tải kích thước từ IPreferenceService)
        // sẽ được triển khai ở Giai đoạn 6/7.
    }

    @Override
    public void showLogin() {
        // (Logic tải FXML, tiêm ViewModel, và hiển thị sẽ ở Giai đoạn 6)
        System.out.println("AppNavigator: Điều hướng đến LoginView (Chưa triển khai FXML)");
        // Ví dụ (sẽ lỗi ở Giai đoạn 5 vì FXML chưa tồn tại):
        // loadScene("LoginView.fxml", "Đăng nhập Emby");
    }

    @Override
    public void showMain() {
        // (Logic tải FXML, tiêm ViewModels, và hiển thị sẽ ở Giai đoạn 7)
        System.out.println("AppNavigator: Điều hướng đến MainView (Chưa triển khai FXML)");
        // Ví dụ (sẽ lỗi ở Giai đoạn 5 vì FXML chưa tồn tại):
        // loadScene("MainView.fxml", "Emby Client");
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
     * (Helper nội bộ - Sẽ được dùng ở các giai đoạn sau)
     * Tải một FXML và đặt nó làm Scene chính.
     */
    private void loadScene(String fxmlFile, String title) {
        try {
            // Logic tải FXML (giả định FXML nằm trong /view/fxml/)
            URL fxmlUrl = getClass().getResource("/com/vinhtt/embyclientsolid/view/fxml/" + fxmlFile);
            if (fxmlUrl == null) {
                throw new IOException("Không tìm thấy FXML: " + fxmlFile);
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);

            // *** PHẦN QUAN TRỌNG CỦA DI ***
            // (Ở đây, chúng ta sẽ tiêm ViewModel vào Controller)
            // Ví dụ:
            // if ("LoginView.fxml".equals(fxmlFile)) {
            //    ILoginViewModel vm = loginViewModelFactory.create();
            //    LoginController controller = new LoginController(vm, this, ...);
            //    loader.setController(controller); // Hoặc dùng setControllerFactory
            // }

            Parent root = loader.load();

            // Lấy Controller sau khi load (nếu FXML định nghĩa)
            // Object controller = loader.getController();
            // (Cấu hình controller nếu cần)

            Scene scene = new Scene(root);

            // Tải CSS (sẽ hoàn thiện ở Giai đoạn 13)
            // URL cssUrl = getClass().getResource("/com/vinhtt/embyclientsolid/styles.css");
            // if (cssUrl != null) {
            //     scene.getStylesheets().add(cssUrl.toExternalForm());
            // }

            primaryStage.setTitle(title);
            primaryStage.setScene(scene);

        } catch (IOException e) {
            e.printStackTrace();
            // (Nên gọi INotificationService ở đây để báo lỗi)
        }
    }
}