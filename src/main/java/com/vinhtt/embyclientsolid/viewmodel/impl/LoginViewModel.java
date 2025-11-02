package com.vinhtt.embyclientsolid.viewmodel.impl;

import com.vinhtt.embyclientsolid.core.IAppNavigator;
import com.vinhtt.embyclientsolid.core.IConfigurationService;
import com.vinhtt.embyclientsolid.core.IEmbySessionService;
import com.vinhtt.embyclientsolid.viewmodel.ILoginViewModel;
import embyclient.ApiException;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.concurrent.Task;

import java.io.IOException;

/**
 * Triển khai (Implementation) của ILoginViewModel.
 * Chứa logic xử lý đăng nhập (UR-1, UR-2, UR-3).
 * Phụ thuộc vào các interface (DIP).
 * Logic được chuyển từ LoginViewModel.java cũ.
 */
public class LoginViewModel implements ILoginViewModel {

    // Dependencies (được inject qua constructor)
    private final IEmbySessionService sessionService;
    private final IAppNavigator appNavigator;
    private final IConfigurationService configService;

    // Properties (Trạng thái của View)
    private final StringProperty serverUrl;
    private final StringProperty username = new SimpleStringProperty("admin"); // Giá trị mặc định
    private final StringProperty password = new SimpleStringProperty("123@123a"); // Giá trị mặc định
    private ReadOnlyStringWrapper statusMessage = new ReadOnlyStringWrapper("");
    private ReadOnlyBooleanWrapper loginInProgress = new ReadOnlyBooleanWrapper(false);
    private ReadOnlyBooleanWrapper loginSuccess = new ReadOnlyBooleanWrapper(false);

    /**
     * Khởi tạo ViewModel với các services đã được tiêm (DI).
     *
     * @param sessionService  Service quản lý session (từ Giai đoạn 2).
     * @param appNavigator    Service điều hướng (từ Giai đoạn 5).
     * @param configService   Service đọc config (từ Giai đoạn 2).
     */
    public LoginViewModel(IEmbySessionService sessionService, IAppNavigator appNavigator, IConfigurationService configService) {
        this.sessionService = sessionService;
        this.appNavigator = appNavigator;
        this.configService = configService;

        // Tự động điền URL máy chủ cuối cùng (UR-2)
        //
        this.serverUrl = new SimpleStringProperty(sessionService.getLastServerUrl());
    }

    @Override
    public void loginCommand() {
        if (loginInProgress.get()) {
            return; // Ngăn chặn spam click
        }

        String url = serverUrl.get().trim();
        String user = username.get().trim();
        String pass = password.get();

        // Validation (UR-3)
        if (url.isEmpty() || user.isEmpty() || pass.isEmpty()) {
            statusMessage.set(configService.getString("loginViewModel", "errorEmptyFields"));
            return;
        }

        loginInProgress.set(true);
        statusMessage.set(configService.getString("loginViewModel", "statusLoggingIn"));
        loginSuccess.set(false);

        // Chạy đăng nhập trong một luồng nền (Task)
        Task<Void> loginTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Ủy thác (delegate) logic đăng nhập cho sessionService
                //
                sessionService.login(url, user, pass);
                return null;
            }
        };

        // Xử lý khi Task thành công
        loginTask.setOnSucceeded(event -> {
            statusMessage.set(configService.getString("loginViewModel", "statusSuccess"));
            loginInProgress.set(false);
            loginSuccess.set(true); // Bắn tín hiệu thành công
            // ViewModel KHÔNG gọi appNavigator.showMain().
            // MainApp sẽ lắng nghe 'loginSuccess' và gọi navigator.
        });

        // Xử lý khi Task thất bại (UR-3)
        loginTask.setOnFailed(event -> {
            Throwable exception = loginTask.getException();
            String errorMessage = parseException(exception); // Phân tích lỗi
            statusMessage.set(errorMessage);
            loginInProgress.set(false);
        });

        new Thread(loginTask).start();
    }

    /**
     * Phân tích Exception để đưa ra thông báo lỗi thân thiện.
     * (Logic từ LoginViewModel.java cũ)
     */
    private String parseException(Throwable exception) {
        if (exception instanceof ApiException) {
            ApiException apiEx = (ApiException) exception;
            if (apiEx.getCode() == 401 || apiEx.getCode() == 403) {
                return configService.getString("loginViewModel", "errorUnauthorized");
            } else if (apiEx.getCode() == 400) {
                return configService.getString("loginViewModel", "errorBadRequest");
            } else if (apiEx.getCode() == 0) {
                return configService.getString("loginViewModel", "errorConnection");
            } else {
                return configService.getString("loginViewModel", "errorApiGeneric", apiEx.getCode());
            }
        } else if (exception instanceof IOException) {
            return configService.getString("loginViewModel", "errorNetwork");
        }
        return configService.getString("loginViewModel", "errorUnexpected");
    }

    // --- Getters cho Properties ---
    @Override public StringProperty serverUrlProperty() { return serverUrl; }
    @Override public StringProperty usernameProperty() { return username; }
    @Override public StringProperty passwordProperty() { return password; }
    @Override public ReadOnlyStringProperty statusMessageProperty() { return statusMessage.getReadOnlyProperty(); }
    @Override public ReadOnlyBooleanProperty loginInProgressProperty() { return loginInProgress.getReadOnlyProperty(); }
    @Override public ReadOnlyBooleanProperty loginSuccessProperty() { return loginSuccess.getReadOnlyProperty(); }
}