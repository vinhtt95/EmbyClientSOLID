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
     * (Cập nhật GĐ 11).
     *
     * @param ownerStage Stage sở hữu (để khóa).
     * @param context    Loại chip đang thêm (TAG, STUDIO, v.v.).
     * @return Đối tượng AddTagResult chứa Tag mới hoặc CopyId, hoặc null nếu hủy.
     */
    AddTagResult showAddTagDialog(Stage ownerStage, SuggestionContext context);

    /**
     * Đóng cửa sổ chi tiết "pop-out" (nếu đang mở).
     */
    void closePopOutDetail();

    /**
     * Hiển thị cửa sổ chi tiết "pop-out".
     * @param item Item để hiển thị trong cửa sổ pop-out.
     */
    void showPopOutDetail(BaseItemDto item);

    /**
     * &#x110;&#x103;ng k&yacute; c&aacute;c ph&iacute;m t&#x1EAF;t v&agrave; &#x111;i&#x1EC1;u h&#x1B0;&#x1EDB;ng chu&#x1ED9;t cho m&#x1ED9;t Scene.
     * @param scene Scene (c&#x1EED;a s&#x1ED5; ch&iacute;nh ho&#x1EB7;c pop-out).
     * @param mainController (T&ugrave;y ch&#x1ECD;n) Controller ch&iacute;nh (d&ugrave;ng cho Enter).
     * @param detailController Controller chi ti&#x1EBF;t (d&ugrave;ng cho Cmd+S, Enter).
     * @param gridController Controller l&#x1B0;&#x1EDB;i (d&ugrave;ng cho Cmd+Enter Play).
     * @param gridViewModel ViewModel l&#x1B0;&#x1EDB;i (d&ugrave;ng cho &#x111;i&#x1EC1;u h&#x1B0;&#x1EDB;ng N/P, Back/Forward).
     */
    void registerHotkeys(Scene scene,
                         MainController mainController,
                         ItemDetailController detailController,
                         ItemGridController gridController,
                         IItemGridViewModel gridViewModel);
}