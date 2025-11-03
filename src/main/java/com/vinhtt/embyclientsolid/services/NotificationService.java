package com.vinhtt.embyclientsolid.services;

import com.vinhtt.embyclientsolid.core.IConfigurationService;
import com.vinhtt.embyclientsolid.core.INotificationService;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;
import javafx.scene.control.DialogPane;

import java.util.Optional;

/**
 * Triển khai (Implementation) của INotificationService.
 * Quản lý trạng thái cho status bar (UR-9) và hiển thị hộp thoại xác nhận (UR-24).
 */
public class NotificationService implements INotificationService {

    private final IConfigurationService configService;
    private Window ownerWindow = null;
    private final String readyString;

    /**
     * Wrapper (có thể ghi) cho status message.
     */
    private final ReadOnlyStringWrapper statusMessage = new ReadOnlyStringWrapper();

    /**
     * Khởi tạo service.
     * @param configService Service Config (DI) để lấy I18n strings.
     */
    public NotificationService(IConfigurationService configService) {
        this.configService = configService;
        this.readyString = configService.getString("mainView", "statusReady");
        this.statusMessage.set(this.readyString);
    }

    @Override
    public void setOwnerWindow(Window window) {
        this.ownerWindow = window;
    }

    @Override
    public void showStatus(String message) {
        // Đảm bảo chạy trên luồng JavaFX
        if (Platform.isFxApplicationThread()) {
            statusMessage.set(message);
        } else {
            Platform.runLater(() -> statusMessage.set(message));
        }
    }

    @Override
    public void clearStatus() {
        showStatus(readyString);
    }

    @Override
    public ReadOnlyStringProperty statusMessageProperty() {
        return statusMessage.getReadOnlyProperty();
    }

    @Override
    public boolean showConfirmation(String title, String content) {
        // Logic từ ItemGridController.showConfirmationDialog
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);

        if (ownerWindow != null) {
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.getStylesheets().addAll(ownerWindow.getScene().getStylesheets());
            alert.initOwner(ownerWindow);
        }

        // (Lưu ý: CSS styling cho Alert sẽ cần được xử lý ở Giai đoạn 13
        // hoặc khi Stage/Scene chính được tạo)

        alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.YES;
    }
}