package com.vinhtt.embyclientsolid;

import com.vinhtt.embyclientsolid.core.*;
import com.vinhtt.embyclientsolid.navigation.AppNavigator;
import com.vinhtt.embyclientsolid.services.DesktopInteractionService;
import com.vinhtt.embyclientsolid.services.JavaPreferenceService;
import com.vinhtt.embyclientsolid.services.JsonConfigurationService;
import com.vinhtt.embyclientsolid.services.NotificationService;
import com.vinhtt.embyclientsolid.session.EmbySessionService;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Lớp chính của ứng dụng EmbyClientSOLID.
 * Đóng vai trò là "Composition Root" - nơi khởi tạo và
 * tiêm (inject) các dependencies (services).
 */
public class MainApp extends Application {

    private IAppNavigator appNavigator;
    private IEmbySessionService sessionService;
    // (Các service khác có thể được giữ ở đây nếu cần shutdown, ví dụ: global hotkey)

    /**
     * Phương thức start, entry point cho JavaFX.
     *
     * @param primaryStage Stage chính của ứng dụng.
     * @throws Exception
     */
    @Override
    public void start(Stage primaryStage) throws Exception {

        // --- KHỞI TẠO DEPENDENCY INJECTION (COMPOSITION ROOT) ---

        // 1. Khởi tạo các Service Cốt lõi (Giai đoạn 2 & 4)
        IPreferenceService preferenceService = new JavaPreferenceService();
        IConfigurationService configService = new JsonConfigurationService();
        this.sessionService = new EmbySessionService(preferenceService);

        // (Khởi tạo các service Giai đoạn 4 - chưa dùng đến ở Giai đoạn 6 nhưng
        // AppNavigator sẽ cần chúng ở các giai đoạn sau)
        ILocalInteractionService localInteractionService = new DesktopInteractionService(configService);
        INotificationService notificationService = new NotificationService(configService);

        // (Khởi tạo các repo Giai đoạn 3 - chưa dùng đến ở Giai đoạn 6)
        // IItemRepository itemRepository = new EmbyItemRepository(sessionService);
        // ...

        // 2. Khởi tạo Navigator (Giai đoạn 5) và tiêm services
        // (Tạm thời chỉ tiêm các service cần cho Giai đoạn 6)
        this.appNavigator = new AppNavigator(sessionService, configService);
        this.appNavigator.initialize(primaryStage);

        // --- KẾT THÚC DI ---


        // 3. Khởi chạy ứng dụng
        // Cố gắng khôi phục session (UR-4)
        if (sessionService.tryRestoreSession()) {
            appNavigator.showMain();
        } else {
            appNavigator.showLogin();
        }
    }

    /**
     * Main method, entry point của ứng dụng.
     *
     * @param args Đối số dòng lệnh.
     */
    public static void main(String[] args) {
        launch(args);
    }
}