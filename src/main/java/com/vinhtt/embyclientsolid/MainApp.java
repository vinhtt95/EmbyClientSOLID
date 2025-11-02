package com.vinhtt.embyclientsolid;

import com.vinhtt.embyclientsolid.core.*;
import com.vinhtt.embyclientsolid.data.IExternalDataService;
import com.vinhtt.embyclientsolid.data.IItemRepository;
import com.vinhtt.embyclientsolid.data.IItemUpdateService;
import com.vinhtt.embyclientsolid.data.IStaticDataRepository;
import com.vinhtt.embyclientsolid.data.impl.EmbyItemRepository;
import com.vinhtt.embyclientsolid.data.impl.EmbyItemUpdateService;
import com.vinhtt.embyclientsolid.data.impl.EmbyStaticDataRepository;
import com.vinhtt.embyclientsolid.data.impl.ExternalMovieDataService;
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
 *
 * (Cập nhật Giai đoạn 7):
 * - Khởi tạo tất cả các services và repositories.
 * - Tiêm (inject) tất cả vào AppNavigator.
 * - Thêm logic lưu/khôi phục vị trí cửa sổ (UR-6).
 */
public class MainApp extends Application {

    private IAppNavigator appNavigator;
    private IEmbySessionService sessionService;
    private IPreferenceService preferenceService;

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
        this.preferenceService = new JavaPreferenceService();
        IConfigurationService configService = new JsonConfigurationService();
        this.sessionService = new EmbySessionService(preferenceService);
        ILocalInteractionService localInteractionService = new DesktopInteractionService(configService);
        INotificationService notificationService = new NotificationService(configService);

        // 2. Khởi tạo các Repository (Giai đoạn 3)
        // (Lưu ý: Các repo cần sessionService)
        IItemRepository itemRepository = new EmbyItemRepository(sessionService);
        IStaticDataRepository staticDataRepository = new EmbyStaticDataRepository(sessionService);
        IExternalDataService externalDataService = new ExternalMovieDataService();
        // (Lưu ý: EmbyItemUpdateService cần IItemRepository để đọc DTOs khi clone)
        IItemUpdateService itemUpdateService = new EmbyItemUpdateService(sessionService, itemRepository);

        // 3. Khởi tạo Navigator (Giai đoạn 5) và tiêm MỌI THỨ
        this.appNavigator = new AppNavigator(
                primaryStage,
                sessionService,
                configService,
                preferenceService,
                notificationService,
                localInteractionService,
                itemRepository,
                staticDataRepository,
                itemUpdateService,
                externalDataService
        );

        // --- KẾT THÚC DI ---

        // 4. Cấu hình Stage chính (UR-6)
        setupPrimaryStage(primaryStage, preferenceService);

        // 5. Khởi chạy ứng dụng
        // Cố gắng khôi phục session (UR-4)
        if (sessionService.tryRestoreSession()) {
            appNavigator.showMain();
        } else {
            appNavigator.showLogin();
        }
    }

    /**
     * Cấu hình Stage chính, bao gồm khôi phục và lưu vị trí/kích thước.
     * (UR-6)
     *
     * @param primaryStage      Stage chính.
     * @param preferenceService Service để đọc/ghi cài đặt.
     */
    private void setupPrimaryStage(Stage primaryStage, IPreferenceService preferenceService) {
        // Khôi phục vị trí (UR-6)
        primaryStage.setX(preferenceService.getDouble("windowX", 100));
        primaryStage.setY(preferenceService.getDouble("windowY", 100));
        // Khôi phục kích thước (UR-6)
        primaryStage.setWidth(preferenceService.getDouble("windowWidth", 2000));
        primaryStage.setHeight(preferenceService.getDouble("windowHeight", 1400));

        // Thêm listener để lưu vị trí/kích thước khi đóng (UR-6)
        primaryStage.setOnCloseRequest(e -> {
            preferenceService.putDouble("windowX", primaryStage.getX());
            preferenceService.putDouble("windowY", primaryStage.getY());
            preferenceService.putDouble("windowWidth", primaryStage.getWidth());
            preferenceService.putDouble("windowHeight", primaryStage.getHeight());
            preferenceService.flush();
            // (Chúng ta có thể thêm logic shutdown hook cho các service ở đây nếu cần)
        });
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