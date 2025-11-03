package com.vinhtt.embyclientsolid.session;

import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.vinhtt.embyclientsolid.core.IConfigurationService;
import com.vinhtt.embyclientsolid.core.IEmbySessionService;
import com.vinhtt.embyclientsolid.core.IPreferenceService;
import embyclient.ApiClient;
import embyclient.ApiException;
import embyclient.api.SystemServiceApi;
import embyclient.api.UserServiceApi;
import embyclient.model.AuthenticateUserByName;
import embyclient.model.AuthenticationAuthenticationResult;
import embyclient.model.SystemInfo;
import embyclient.model.UserDto;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.io.IOException;
import java.util.UUID;

/**
 * Triển khai của {@link IEmbySessionService}.
 * Chịu trách nhiệm duy nhất về logic Xác thực và Quản lý Phiên (Session) Emby
 * (UR-1, UR-4, UR-5).
 * Lớp này ủy thác việc lưu trữ session cho {@link IPreferenceService}.
 */
public class EmbySessionService implements IEmbySessionService {

    // Các khóa (key) để lưu trữ session trong IPreferenceService
    private static final String KEY_SERVER_URL = "serverUrl";
    private static final String KEY_ACCESS_TOKEN = "accessToken";
    private static final String KEY_CLIENT_AUTH_HEADER = "clientAuthHeader";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_DEVICE_ID = "deviceId";

    private final IPreferenceService preferenceService;
    private final ApiClient apiClient;
    private final IConfigurationService configService;

    // Trạng thái session trong bộ nhớ
    private AuthenticationAuthenticationResult currentAuthResult;
    private String currentAccessToken;
    private final BooleanProperty loggedIn = new SimpleBooleanProperty(false);

    /**
     * Khởi tạo Dịch vụ Session.
     *
     * @param preferenceService Dịch vụ (DI) để đọc/ghi session token.
     * @param configService Dịch vụ (DI) để lấy chuỗi lỗi I18n.
     */
    public EmbySessionService(IPreferenceService preferenceService, IConfigurationService configService) {
        this.preferenceService = preferenceService;
        this.configService = configService;

        // Khởi tạo ApiClient (trung tâm của Emby SDK)
        this.apiClient = new ApiClient();

        // Cấu hình OkHttpClient (nằm bên trong ApiClient)
        // để thêm Interceptor (bộ chặn) tùy chỉnh của chúng ta
        OkHttpClient defaultClient = apiClient.getHttpClient();
        defaultClient.interceptors().add(new AuthHeaderInterceptor());
    }

    /**
     * Một {@link Interceptor} của OkHttpClient, tự động chèn header
     * {@code X-Emby-Token} (nếu có) vào MỌI request API.
     */
    private class AuthHeaderInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request originalRequest = chain.request();
            Request.Builder builder = originalRequest.newBuilder();

            // Lấy token hiện tại từ session service
            String localAccessToken = EmbySessionService.this.currentAccessToken;
            if (localAccessToken != null && !localAccessToken.isEmpty()) {
                // Xóa header cũ (nếu có) và thêm header mới nhất
                builder.removeHeader("X-Emby-Token");
                builder.header("X-Emby-Token", localAccessToken);
            }

            Request newRequest = builder.build();
            return chain.proceed(newRequest);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Interceptor getAuthHeaderInterceptor() {
        return new AuthHeaderInterceptor();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void login(String url, String username, String password) throws Exception {
        // 1. Cấu hình ApiClient với URL máy chủ mới
        if (!url.equals(apiClient.getBasePath())) {
            apiClient.setBasePath(url);
        }

        // 2. Chuẩn bị đối tượng request body
        AuthenticateUserByName authRequest = new AuthenticateUserByName();
        authRequest.setUsername(username);
        authRequest.setPw(password);

        // 3. Chuẩn bị header "X-Emby-Authorization" (UR-1)
        String clientAuthHeader = generateClientAuthHeader();

        // 4. Tạo một instance API service tạm thời (chỉ dùng cho đăng nhập)
        UserServiceApi userService = new UserServiceApi(apiClient);

        // 5. Gọi API đăng nhập (sẽ ném Exception nếu thất bại)
        AuthenticationAuthenticationResult authResult = userService.postUsersAuthenticatebyname(authRequest, clientAuthHeader);

        // 6. Lưu kết quả session nếu đăng nhập thành công
        if (authResult != null && authResult.getAccessToken() != null) {
            setCurrentAuthResult(authResult, url, clientAuthHeader);
        } else {
            // Ném lỗi nếu server trả về 200 OK nhưng không có token
            throw new ApiException(configService.getString("exceptions", "invalidServerResponse"));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void logout() {
        // Xóa session trong bộ nhớ
        setCurrentAuthResult(null, null, null);
        // Xóa session đã lưu
        clearSession();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean tryRestoreSession() {
        // 1. Tải thông tin session đã lưu từ IPreferenceService
        String[] sessionData = loadSession();
        if (sessionData != null) {
            String serverUrl = sessionData[0];
            String accessToken = sessionData[1];
            String loadedClientHeader = sessionData[2];
            String loadedUserId = sessionData[3];

            // 2. Cấu hình ApiClient với thông tin đã lưu
            apiClient.setBasePath(serverUrl);
            this.currentAccessToken = accessToken;

            try {
                // 3. Thực hiện một cuộc gọi API đơn giản (ví dụ: lấy thông tin hệ thống)
                // để xác thực token còn hợp lệ hay không.
                SystemServiceApi systemService = new SystemServiceApi(apiClient);
                SystemInfo systemInfo = systemService.getSystemInfo();

                if (systemInfo != null) {
                    // 4. Nếu token hợp lệ, lấy thông tin User đầy đủ
                    UserServiceApi userService = new UserServiceApi(apiClient);
                    UserDto currentUser = userService.getUsersById(loadedUserId);

                    if (currentUser != null) {
                        // 5. Khôi phục thành công
                        AuthenticationAuthenticationResult restoredAuth = new AuthenticationAuthenticationResult();
                        restoredAuth.setAccessToken(accessToken);
                        restoredAuth.setUser(currentUser);
                        setCurrentAuthResult(restoredAuth, serverUrl, loadedClientHeader);
                        return true;
                    }
                }
            } catch (ApiException e) {
                System.err.println("API Exception khi khôi phục phiên (Code: " + e.getCode() + "): " + e.getMessage());
                // Nếu lỗi không phải là do mạng (code 0),
                // nghĩa là token/session đã hỏng -> xóa nó đi.
                if (e.getCode() != 0) {
                    clearSession();
                }
            } catch (Exception e) {
                System.err.println("Lỗi không xác định khi khôi phục phiên: " + e.getMessage());
            }
        }

        // Khôi phục thất bại
        logout(); // Đảm bảo dọn dẹp trạng thái
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ApiClient getApiClient() {
        return apiClient;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReadOnlyBooleanProperty loggedInProperty() {
        return loggedIn;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isLoggedIn() {
        return loggedIn.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCurrentUserId() {
        if (currentAuthResult != null && currentAuthResult.getUser() != null) {
            return currentAuthResult.getUser().getId();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLastServerUrl() {
        // (UR-2)
        return preferenceService.getString(KEY_SERVER_URL, "");
    }

    /**
     * Helper nội bộ để cập nhật trạng thái session (cả trong bộ nhớ và lưu trữ cố định)
     * sau khi đăng nhập/đăng xuất thành công.
     *
     * @param authResult     Kết quả xác thực từ API (hoặc null khi đăng xuất).
     * @param serverUrl      URL máy chủ (hoặc null khi đăng xuất).
     * @param clientHeader   Header "X-Emby-Authorization" (hoặc null khi đăng xuất).
     */
    private void setCurrentAuthResult(AuthenticationAuthenticationResult authResult, String serverUrl, String clientHeader) {
        this.currentAuthResult = authResult;
        String userIdToSave = null;

        if (authResult != null && authResult.getAccessToken() != null && serverUrl != null && !serverUrl.isEmpty()) {
            // Cập nhật trạng thái trong bộ nhớ
            this.currentAccessToken = authResult.getAccessToken();
            apiClient.setBasePath(serverUrl);
            loggedIn.set(true);

            if (authResult.getUser() != null && authResult.getUser().getId() != null) {
                userIdToSave = authResult.getUser().getId();
            }

            // Lưu session vào lưu trữ cố định
            saveSession(serverUrl, this.currentAccessToken, clientHeader, userIdToSave);
        } else {
            // Đăng xuất: xóa trạng thái trong bộ nhớ
            this.currentAccessToken = null;
            loggedIn.set(false);
        }
    }

    /**
     * Tạo header {@code X-Emby-Authorization} cần thiết cho việc xác thực.
     * Header này định danh ứng dụng (UR-1).
     *
     * @return Chuỗi header "X-Emby-Authorization".
     */
    private String generateClientAuthHeader() {
        String appName = "EmbyClientSOLID";
        String appVersion = "1.0.0";
        String deviceName = System.getProperty("os.name", configService.getString("appSettings", "defaultDeviceName"));

        // Lấy hoặc tạo DeviceId (để Emby nhận diện client này)
        String deviceId = preferenceService.getString(KEY_DEVICE_ID, null);
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString();
            preferenceService.putString(KEY_DEVICE_ID, deviceId);
            preferenceService.flush();
        }

        return String.format("Emby Client=\"%s\", Device=\"%s\", DeviceId=\"%s\", Version=\"%s\"",
                appName, deviceName, deviceId, appVersion);
    }

    /**
     * Lưu trữ các chi tiết session (URL, Token, UserID) vào {@link IPreferenceService}.
     *
     * @param serverUrl    URL máy chủ.
     * @param accessToken  Token truy cập.
     * @param clientHeader Header "X-Emby-Authorization".
     * @param userId       ID người dùng.
     */
    private void saveSession(String serverUrl, String accessToken, String clientHeader, String userId) {
        if (serverUrl != null && accessToken != null && clientHeader != null && userId != null) {
            preferenceService.putString(KEY_SERVER_URL, serverUrl);
            preferenceService.putString(KEY_ACCESS_TOKEN, accessToken);
            preferenceService.putString(KEY_CLIENT_AUTH_HEADER, clientHeader);
            preferenceService.putString(KEY_USER_ID, userId);
            preferenceService.flush(); // Đảm bảo lưu ngay
        }
    }

    /**
     * Tải thông tin session từ {@link IPreferenceService}.
     *
     * @return Một mảng String chứa [serverUrl, accessToken, clientHeader, userId],
     * hoặc {@code null} nếu thiếu thông tin.
     */
    private String[] loadSession() {
        String serverUrl = preferenceService.getString(KEY_SERVER_URL, null);
        String accessToken = preferenceService.getString(KEY_ACCESS_TOKEN, null);
        String clientHeader = preferenceService.getString(KEY_CLIENT_AUTH_HEADER, null);
        String userId = preferenceService.getString(KEY_USER_ID, null);

        // Đảm bảo tất cả các trường đều tồn tại
        if (serverUrl != null && accessToken != null && clientHeader != null && userId != null) {
            return new String[]{serverUrl, accessToken, clientHeader, userId};
        }
        return null;
    }

    /**
     * Xóa thông tin session (Token, UserID) khỏi {@link IPreferenceService},
     * giữ lại Server URL (UR-2).
     */
    private void clearSession() {
        preferenceService.remove(KEY_ACCESS_TOKEN);
        preferenceService.remove(KEY_CLIENT_AUTH_HEADER);
        preferenceService.remove(KEY_USER_ID);
        preferenceService.flush(); // Đảm bảo lưu ngay
    }
}