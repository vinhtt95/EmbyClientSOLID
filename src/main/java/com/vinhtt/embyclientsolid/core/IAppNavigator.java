package com.vinhtt.embyclientsolid.core;

import com.vinhtt.embyclientsolid.controller.ItemDetailController;
import com.vinhtt.embyclientsolid.controller.ItemGridController;
import com.vinhtt.embyclientsolid.controller.MainController;
import com.vinhtt.embyclientsolid.model.SuggestionContext;
import com.vinhtt.embyclientsolid.model.Tag;
import com.vinhtt.embyclientsolid.viewmodel.AddTagResult;
import com.vinhtt.embyclientsolid.viewmodel.IItemGridViewModel;
import embyclient.model.BaseItemDto;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Interface trừu tượng hóa việc điều hướng (navigation) giữa các Scene (Màn hình)
 * và quản lý việc hiển thị các cửa sổ (Stage) và dialogs.
 */
public interface IAppNavigator {

    /**
     * Khởi tạo Navigator với Stage chính (cửa sổ chính) của ứng dụng.
     *
     * @param primaryStage Stage chính.
     */
    void initialize(Stage primaryStage);

    /**
     * Hiển thị màn hình đăng nhập (LoginView) trên Stage chính.
     */
    void showLogin();

    /**
     * Hiển thị màn hình chính (MainView) trên Stage chính.
     */
    void showMain();

    /**
     * Hiển thị dialog "Add Tag" (Thêm Tag/Studio/People/Genre).
     *
     * @param ownerStage Stage sở hữu (để khóa).
     * @param context    Loại chip đang thêm (TAG, STUDIO, v.v.).
     * @return Đối tượng AddTagResult chứa Tag mới hoặc CopyId, hoặc null nếu người dùng hủy.
     */
    AddTagResult showAddTagDialog(Stage ownerStage, SuggestionContext context);

    /**
     * Đóng cửa sổ chi tiết "pop-out" (nếu đang mở).
     */
    void closePopOutDetail();

    /**
     * Hiển thị cửa sổ chi tiết (Cột 3) dưới dạng "pop-out" (cửa sổ riêng).
     *
     * @param item Item để hiển thị trong cửa sổ pop-out.
     * @param mainController Tham chiếu đến MainController để gọi ẩn/hiện cột 3.
     */
    void showPopOutDetail(BaseItemDto item, Object mainController);

    /**
     * Đăng ký các phím tắt (hotkeys) và sự kiện điều hướng (chuột)
     * cho một Scene cụ thể (Scene chính hoặc Scene pop-out).
     *
     * @param scene Scene cần đăng ký hotkey.
     * @param mainController Controller chính (nếu có).
     * @param detailController Controller chi tiết (nếu có).
     * @param gridController Controller lưới (nếu có).
     * @param gridViewModel VM lưới (dùng cho điều hướng Lùi/Tiến).
     */
    void registerHotkeys(Scene scene,
                         MainController mainController,
                         ItemDetailController detailController,
                         ItemGridController gridController,
                         IItemGridViewModel gridViewModel);
}