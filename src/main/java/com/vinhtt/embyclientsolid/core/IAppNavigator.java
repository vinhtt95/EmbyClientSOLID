package com.vinhtt.embyclientsolid.core;

import com.vinhtt.embyclientsolid.model.Tag;
import javafx.stage.Stage;

/**
 * Interface trừu tượng hóa việc điều hướng giữa các Scene (Màn hình)
 * và quản lý việc hiển thị các cửa sổ (Stage).
 * (Hỗ trợ UR-5, UR-35, UR-50).
 */
public interface IAppNavigator {

    /**
     * Khởi tạo Navigator với Stage chính của ứng dụng.
     * Đây phải là hàm được gọi đầu tiên trong MainApp.start().
     *
     * @param primaryStage Stage chính.
     */
    void initialize(Stage primaryStage);

    /**
     * Hiển thị màn hình đăng nhập (LoginView) trên Stage chính.
     * (Hỗ trợ UR-5).
     */
    void showLogin();

    /**
     * Hiển thị màn hình chính (MainView) trên Stage chính.
     */
    void showMain();

    /**
     * Hiển thị dialog "Add Tag" (UR-35).
     *
     * @param ownerStage Stage sở hữu (để khóa).
     * @return Đối tượng Tag mới được tạo, hoặc null nếu người dùng hủy.
     * (Kiểu trả về sẽ được hoàn thiện ở Giai đoạn 11).
     */
    Tag showAddTagDialog(Stage ownerStage);

    /**
     * Hiển thị cửa sổ chi tiết "pop-out" (UR-50).
     */
    void showPopOutDetail();

    /**
     * Đóng cửa sổ chi tiết "pop-out" (nếu đang mở).
     */
    void closePopOutDetail();
}