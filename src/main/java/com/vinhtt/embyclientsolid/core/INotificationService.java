package com.vinhtt.embyclientsolid.core;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.stage.Window;

/**
 * Interface trừu tượng hóa hệ thống thông báo trạng thái (status bar)
 * và hiển thị dialog xác nhận (confirmation dialog).
 */
public interface INotificationService {

    /**
     * Hiển thị một tin nhắn trên thanh trạng thái (status bar) chung của ứng dụng.
     *
     * @param message Tin nhắn cần hiển thị.
     */
    void showStatus(String message);

    /**
     * Xóa tin nhắn trên thanh trạng thái, quay về trạng thái "Sẵn sàng".
     */
    void clearStatus();

    /**
     * Lấy thuộc tính (Property) JavaFX (chỉ-đọc) chứa tin nhắn trạng thái hiện tại.
     * Dùng để binding (liên kết) với Label trên MainView.
     *
     * @return ReadOnlyStringProperty.
     */
    ReadOnlyStringProperty statusMessageProperty();

    /**
     * Hiển thị hộp thoại xác nhận (Yes/No) và chờ người dùng trả lời.
     *
     * @param title Tiêu đề của hộp thoại.
     * @param content Nội dung câu hỏi.
     * @return true nếu người dùng chọn Yes, false nếu chọn No/Cancel.
     */
    boolean showConfirmation(String title, String content);

    /**
     * Thiết lập cửa sổ (Window) chủ sở hữu cho các dialog (ví dụ: dialog xác nhận).
     * Điều này đảm bảo dialog hiển thị đúng và khóa (modal) cửa sổ cha.
     *
     * @param window Cửa sổ (Window) chính của ứng dụng.
     */
    void setOwnerWindow(Window window);
}