package com.vinhtt.embyclientsolid.core;

import javafx.beans.property.ReadOnlyStringProperty;

/**
 * Interface trừu tượng hóa hệ thống thông báo trạng thái và xác nhận.
 * (UR-9, UR-24).
 */
public interface INotificationService {

    /**
     * Hiển thị một tin nhắn trên thanh trạng thái (status bar) chung.
     * (UR-9).
     *
     * @param message Tin nhắn cần hiển thị.
     */
    void showStatus(String message);

    /**
     * Xóa tin nhắn trên thanh trạng thái, quay về "Sẵn sàng".
     */
    void clearStatus();

    /**
     * Lấy Property (chỉ-đọc) chứa tin nhắn trạng thái hiện tại.
     * Dùng để binding với Label trên MainView.
     *
     * @return ReadOnlyStringProperty.
     */
    ReadOnlyStringProperty statusMessageProperty();

    /**
     * Hiển thị hộp thoại xác nhận (Yes/No).
     * (UR-24).
     *
     * @param title Tiêu đề của hộp thoại.
     * @param content Nội dung câu hỏi.
     * @return true nếu người dùng chọn Yes, false nếu chọn No/Cancel.
     */
    boolean showConfirmation(String title, String content);
}