package com.vinhtt.embyclientsolid.core;

import com.squareup.okhttp.Interceptor;
import embyclient.ApiClient;
import embyclient.ApiException;
import javafx.beans.property.ReadOnlyBooleanProperty;

/**
 * Interface trừu tượng hóa việc quản lý Phiên (Session) Emby.
 * Chịu trách nhiệm cho: Đăng nhập, Đăng xuất, Khôi phục phiên,
 * và cung cấp ApiClient đã được xác thực cho các service khác.
 */
public interface IEmbySessionService {

    /**
     * Cố gắng đăng nhập vào máy chủ Emby bằng thông tin đăng nhập.
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
     */
    void logout();

    /**
     * Cố gắng khôi phục phiên làm việc (tự động đăng nhập) từ bộ nhớ cố định (preferences).
     *
     * @return true nếu khôi phục thành công, false nếu thất bại.
     */
    boolean tryRestoreSession();

    /**
     * Lấy ApiClient đã được cấu hình (với BasePath và Interceptor xác thực).
     * Các service khác (ví dụ: Repository) sẽ dùng client này để gọi API.
     *
     * @return ApiClient đã cấu hình.
     */
    ApiClient getApiClient();

    /**
     * Lấy Interceptor xác thực (Auth Header Interceptor).
     * Dùng cho các yêu cầu tùy chỉnh (ví dụ: upload ảnh)
     * mà không thể dùng ApiClient mặc định.
     *
     * @return Interceptor chứa X-Emby-Token.
     */
    Interceptor getAuthHeaderInterceptor();

    /**
     * Lấy thuộc tính (Property) JavaFX cho biết trạng thái đăng nhập.
     * Dùng để binding (liên kết) trong UI.
     *
     * @return Thuộc tính (Property) chỉ-đọc (ReadOnlyBooleanProperty).
     */
    ReadOnlyBooleanProperty loggedInProperty();

    /**
     * Kiểm tra trạng thái đăng nhập hiện tại.
     *
     * @return true nếu đã đăng nhập, false nếu chưa.
     */
    boolean isLoggedIn();

    /**
     * Lấy ID của người dùng (User) hiện tại đang đăng nhập.
     *
     * @return ID của người dùng, hoặc null nếu chưa đăng nhập.
     */
    String getCurrentUserId();

    /**
     * Lấy URL máy chủ cuối cùng đã đăng nhập thành công.
     *
     * @return URL máy chủ, hoặc chuỗi rỗng "" nếu chưa có.
     */
    String getLastServerUrl();
}