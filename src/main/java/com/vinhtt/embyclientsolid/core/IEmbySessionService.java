package com.vinhtt.embyclientsolid.core;

import com.squareup.okhttp.Interceptor;
import embyclient.ApiClient;
import embyclient.ApiException;
import javafx.beans.property.ReadOnlyBooleanProperty;

/**
 * Interface trừu tượng hóa việc quản lý Phiên (Session) Emby.
 * Chịu trách nhiệm cho: Đăng nhập, Đăng xuất, Khôi phục phiên,
 * và cung cấp ApiClient đã được xác thực.
 * (UR-1, UR-3, UR-4, UR-5).
 */
public interface IEmbySessionService {

    /**
     * Cố gắng đăng nhập vào máy chủ Emby.
     *
     * @param url      URL máy chủ (ví dụ: "http://localhost:8096").
     * @param username Tên đăng nhập.
     * @param password Mật khẩu.
     * @throws ApiException Nếu API Emby trả về lỗi (ví dụ: 401 Sai mật khẩu).
     * @throws Exception    Nếu có lỗi mạng hoặc lỗi khác.
     */
    void login(String url, String username, String password) throws Exception;

    /**
     * Đăng xuất khỏi phiên hiện tại.
     * Xóa token đã lưu và reset ApiClient.
     * (UR-5).
     */
    void logout();

    /**
     * Cố gắng khôi phục phiên làm việc từ preferences.
     * (UR-4).
     *
     * @return true nếu khôi phục thành công, false nếu thất bại.
     */
    boolean tryRestoreSession();

    /**
     * Lấy ApiClient đã được cấu hình (với BasePath và Interceptor).
     * Các service khác (ví dụ: Repository) sẽ dùng client này.
     *
     * @return ApiClient đã cấu hình.
     */
    ApiClient getApiClient();

    /**
     * Lấy Interceptor xác thực (Auth Header Interceptor).
     * Dùng cho các yêu cầu tùy chỉnh (ví dụ: upload ảnh).
     *
     * @return Interceptor chứa X-Emby-Token.
     */
    Interceptor getAuthHeaderInterceptor();

    /**
     * @return Property JavaFX cho biết trạng thái đăng nhập.
     */
    ReadOnlyBooleanProperty loggedInProperty();

    /**
     * @return Giá trị boolean cho biết trạng thái đăng nhập.
     */
    boolean isLoggedIn();

    /**
     * @return ID của người dùng hiện tại, hoặc null nếu chưa đăng nhập.
     */
    String getCurrentUserId();

    /**
     * @return URL máy chủ cuối cùng đã đăng nhập thành công, hoặc "" nếu chưa có.
     * (Hỗ trợ UR-2).
     */
    String getLastServerUrl();
}