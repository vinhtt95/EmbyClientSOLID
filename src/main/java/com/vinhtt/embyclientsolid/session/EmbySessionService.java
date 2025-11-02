package com.vinhtt.embyclientsolid.session;

import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
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
 * Triển khai (Implementation) của IEmbySessionService.
 * Tuân thủ Single Responsibility Principle (SRP):
 * Lớp này chỉ chịu trách nhiệm về Xác thực và Quản lý Phiên.
 * Nó ủy thác việc lưu trữ cho IPreferenceService.
 * Logic được chuyển từ EmbyService.java
 * và LoginViewModel.java cũ.
 */
public class EmbySessionService implements IEmbySessionService {

    // Các khóa để lưu preferences
    private static final String KEY_SERVER_URL = "serverUrl";
    private static final String KEY_ACCESS_TOKEN = "accessToken";
    private static final String KEY_CLIENT_AUTH_HEADER = "clientAuthHeader";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_DEVICE_ID = "deviceId";

    private final IPreferenceService preferenceService;
    private final ApiClient apiClient;

    private AuthenticationAuthenticationResult currentAuthResult;
    private String currentAccessToken;
    private final BooleanProperty loggedIn = new SimpleBooleanProperty(false);

    /**
     * Khởi tạo Session Service.
     *
     * @param preferenceService Service đã được tiêm (DI) để quản lý lưu trữ.
     */
    public EmbySessionService(IPreferenceService preferenceService) {
        this.preferenceService = preferenceService;

        this.apiClient = new ApiClient();
        // Cấu hình OkHttpClient với Interceptor
        OkHttpClient defaultClient = apiClient.getHttpClient();
        defaultClient.interceptors().add(new AuthHeaderInterceptor());
    }

    /**
     * Interceptor tùy chỉnh để tự động thêm X-Emby-Token.
     *
     */
    private class AuthHeaderInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request originalRequest = chain.request();
            Request.Builder builder = originalRequest.newBuilder();

            String localAccessToken = EmbySessionService.this.currentAccessToken;
            if (localAccessToken != null && !localAccessToken.isEmpty()) {
                builder.removeHeader("X-Emby-Token");
                builder.header("X-Emby-Token", localAccessToken);
            }

            Request newRequest = builder.build();
            return chain.proceed(newRequest);
        }
    }

    @Override
    public Interceptor getAuthHeaderInterceptor() {
        return new AuthHeaderInterceptor();
    }

    @Override
    public void login(String url, String username, String password) throws Exception {
        // Logic từ LoginViewModel.java cũ

        // 1. Cấu hình ApiClient với URL mới
        if (!url.equals(apiClient.getBasePath())) {
            apiClient.setBasePath(url);
        }

        // 2. Chuẩn bị request
        AuthenticateUserByName authRequest = new AuthenticateUserByName();
        authRequest.setUsername(username);
        authRequest.setPw(password);

        // 3. Chuẩn bị header (dùng preferenceService)
        String clientAuthHeader = generateClientAuthHeader();

        // 4. Tạo API service tạm thời (chỉ dùng cho login)
        UserServiceApi userService = new UserServiceApi(apiClient);

        // 5. Gọi API (ném lỗi nếu thất bại)
        AuthenticationAuthenticationResult authResult = userService.postUsersAuthenticatebyname(authRequest, clientAuthHeader);

        // 6. Lưu kết quả nếu thành công
        if (authResult != null && authResult.getAccessToken() != null) {
            setCurrentAuthResult(authResult, url, clientAuthHeader);
        } else {
            throw new ApiException("Phản hồi không hợp lệ từ máy chủ.");
        }
    }

    @Override
    public void logout() {
        //
        setCurrentAuthResult(null, null, null);
        clearSession();
    }

    @Override
    public boolean tryRestoreSession() {
        // Logic từ EmbyService.java cũ
        String[] sessionData = loadSession();
        if (sessionData != null) {
            String serverUrl = sessionData[0];
            String accessToken = sessionData[1];
            String loadedClientHeader = sessionData[2];
            String loadedUserId = sessionData[3];

            apiClient.setBasePath(serverUrl);
            this.currentAccessToken = accessToken;

            try {
                // Tạo các service tạm thời để xác thực
                SystemServiceApi systemService = new SystemServiceApi(apiClient);
                SystemInfo systemInfo = systemService.getSystemInfo();

                if (systemInfo != null) {
                    UserServiceApi userService = new UserServiceApi(apiClient);
                    UserDto currentUser = userService.getUsersById(loadedUserId);

                    if (currentUser != null) {
                        // Khôi phục thành công
                        AuthenticationAuthenticationResult restoredAuth = new AuthenticationAuthenticationResult();
                        restoredAuth.setAccessToken(accessToken);
                        restoredAuth.setUser(currentUser);
                        setCurrentAuthResult(restoredAuth, serverUrl, loadedClientHeader);
                        return true;
                    }
                }
            } catch (ApiException e) {
                System.err.println("API Exception khi khôi phục phiên (Code: " + e.getCode() + "): " + e.getMessage());
                if (e.getCode() != 0) { // Nếu lỗi không phải là do mạng
                    clearSession(); // Token/session hỏng, xóa nó đi
                }
            } catch (Exception e) {
                System.err.println("Lỗi không xác định khi khôi phục phiên: " + e.getMessage());
            }
        }

        // Thất bại
        logout(); // Đảm bảo dọn dẹp
        return false;
    }

    @Override
    public ApiClient getApiClient() {
        return apiClient;
    }

    @Override
    public ReadOnlyBooleanProperty loggedInProperty() {
        return loggedIn;
    }

    @Override
    public boolean isLoggedIn() {
        return loggedIn.get();
    }

    @Override
    public String getCurrentUserId() {
        if (currentAuthResult != null && currentAuthResult.getUser() != null) {
            return currentAuthResult.getUser().getId();
        }
        return null;
    }

    @Override
    public String getLastServerUrl() {
        return preferenceService.getString(KEY_SERVER_URL, "");
    }

    // --- Các hàm Helper nội bộ (chuyển từ EmbyService) ---

    /**
     * Cập nhật trạng thái dịch vụ với kết quả xác thực.
     *
     */
    private void setCurrentAuthResult(AuthenticationAuthenticationResult authResult, String serverUrl, String clientHeader) {
        this.currentAuthResult = authResult;
        String userIdToSave = null;

        if (authResult != null && authResult.getAccessToken() != null && serverUrl != null && !serverUrl.isEmpty()) {
            this.currentAccessToken = authResult.getAccessToken();
            apiClient.setBasePath(serverUrl);
            loggedIn.set(true);

            if (authResult.getUser() != null && authResult.getUser().getId() != null) {
                userIdToSave = authResult.getUser().getId();
            }

            // Lưu phiên (dùng IPreferenceService)
            saveSession(serverUrl, this.currentAccessToken, clientHeader, userIdToSave);
        } else {
            // Đăng xuất
            this.currentAccessToken = null;
            loggedIn.set(false);
        }
    }

    /**
     * Tạo X-Emby-Authorization header.
     *
     */
    private String generateClientAuthHeader() {
        String appName = "EmbyClientSOLID"; // Tên app mới
        String appVersion = "1.0.0";
        String deviceName = System.getProperty("os.name", "Desktop");

        // Lấy hoặc tạo DeviceId
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
     * Lưu thông tin session vào preferences.
     *
     */
    private void saveSession(String serverUrl, String accessToken, String clientHeader, String userId) {
        if (serverUrl != null && accessToken != null && clientHeader != null && userId != null) {
            preferenceService.putString(KEY_SERVER_URL, serverUrl);
            preferenceService.putString(KEY_ACCESS_TOKEN, accessToken);
            preferenceService.putString(KEY_CLIENT_AUTH_HEADER, clientHeader);
            preferenceService.putString(KEY_USER_ID, userId);
            preferenceService.flush();
        }
    }

    /**
     * Tải thông tin session từ preferences.
     *
     * @return String array chứa [serverUrl, accessToken, clientHeader, userId], hoặc null nếu thiếu.
     */
    private String[] loadSession() {
        String serverUrl = preferenceService.getString(KEY_SERVER_URL, null);
        String accessToken = preferenceService.getString(KEY_ACCESS_TOKEN, null);
        String clientHeader = preferenceService.getString(KEY_CLIENT_AUTH_HEADER, null);
        String userId = preferenceService.getString(KEY_USER_ID, null);

        if (serverUrl != null && accessToken != null && clientHeader != null && userId != null) {
            return new String[]{serverUrl, accessToken, clientHeader, userId};
        }
        return null;
    }

    /**
     * Xóa thông tin session (trừ server URL).
     *
     */
    private void clearSession() {
        preferenceService.remove(KEY_ACCESS_TOKEN);
        preferenceService.remove(KEY_CLIENT_AUTH_HEADER);
        preferenceService.remove(KEY_USER_ID);
        preferenceService.flush();
    }
}