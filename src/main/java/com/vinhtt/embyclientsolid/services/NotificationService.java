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
 * Triển khai của {@link INotificationService}.
 * Quản lý trạng thái thông báo chung (cho status bar UR-9) và
 * hiển thị các dialog xác nhận (UR-24).
 */
public class NotificationService implements INotificationService {

    private final IConfigurationService configService;
    // Tham chiếu đến cửa sổ chính để áp dụng CSS cho dialog
    private Window ownerWindow = null;
    private final String readyString;

    /**
     * Wrapper (có thể ghi) cho status message.
     * MainViewModel sẽ bind (liên kết) với property chỉ-đọc (read-only) của wrapper này.
     */
    private final ReadOnlyStringWrapper statusMessage = new ReadOnlyStringWrapper();

    /**
     * Khởi tạo service thông báo.
     *
     * @param configService Tiêm (inject) {@link IConfigurationService} để lấy chuỗi I18n
     * (ví dụ: "Sẵn sàng.").
     */
    public NotificationService(IConfigurationService configService) {
        this.configService = configService;
        this.readyString = configService.getString("mainView", "statusReady");
        this.statusMessage.set(this.readyString);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOwnerWindow(Window window) {
        this.ownerWindow = window;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showStatus(String message) {
        // Đảm bảo rằng việc cập nhật property (dùng cho UI)
        // luôn được thực hiện trên luồng JavaFX.
        if (Platform.isFxApplicationThread()) {
            statusMessage.set(message);
        } else {
            Platform.runLater(() -> statusMessage.set(message));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearStatus() {
        showStatus(readyString);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReadOnlyStringProperty statusMessageProperty() {
        return statusMessage.getReadOnlyProperty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean showConfirmation(String title, String content) {
        // Logic hiển thị dialog xác nhận (UR-24)
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);

        // Nếu đã set ownerWindow, áp dụng CSS của scene chính cho dialog
        if (ownerWindow != null) {
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.getStylesheets().addAll(ownerWindow.getScene().getStylesheets());
            alert.initOwner(ownerWindow);
        }

        // Chỉ hiển thị nút Yes và No
        alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        // Hiển thị dialog và chờ kết quả
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.YES;
    }
}