package com.vinhtt.embyclientsolid.controller;

import com.vinhtt.embyclientsolid.core.IConfigurationService;
import com.vinhtt.embyclientsolid.viewmodel.ILoginViewModel;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Controller (View) cho LoginView.fxml.
 * Lớp này chỉ chịu trách nhiệm binding UI components với ILoginViewModel
 * và ủy thác (delegate) các hành động của người dùng cho ViewModel.
 */
public class LoginController {

    // --- FXML UI Components ---
    @FXML private Label titleLabel;
    @FXML private TextField serverUrlField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label statusLabel;

    private final ILoginViewModel viewModel;
    private final IConfigurationService configService;

    /**
     * Khởi tạo Controller với các dependencies cần thiết (DI).
     *
     * @param viewModel     ViewModel chứa logic và trạng thái của màn hình Login.
     * @param configService Service để lấy chuỗi I18n.
     */
    public LoginController(ILoginViewModel viewModel, IConfigurationService configService) {
        this.viewModel = viewModel;
        this.configService = configService;
    }

    /**
     * Được gọi bởi FXMLLoader sau khi các trường @FXML đã được tiêm (inject).
     * Thực hiện việc cài đặt I18n và binding dữ liệu.
     */
    @FXML
    public void initialize() {
        // 1. Cài đặt I18n
        setupLocalization();

        // 2. Binding (Liên kết) UI với ViewModel

        // Liên kết 2 chiều (bindBidirectional) cho các trường text
        serverUrlField.textProperty().bindBidirectional(viewModel.serverUrlProperty());
        usernameField.textProperty().bindBidirectional(viewModel.usernameProperty());
        passwordField.textProperty().bindBidirectional(viewModel.passwordProperty());

        // Liên kết 1 chiều (bind) cho label trạng thái
        statusLabel.textProperty().bind(viewModel.statusMessageProperty());

        // Liên kết trạng thái loading cho nút Login
        // Vô hiệu hóa nút nếu đang đăng nhập
        loginButton.disableProperty().bind(viewModel.loginInProgressProperty());
        // Thay đổi text của nút (ví dụ: "Login" -> "Logging in...")
        loginButton.textProperty().bind(
                Bindings.when(viewModel.loginInProgressProperty())
                        .then(configService.getString("loginView", "loginInProgress"))
                        .otherwise(configService.getString("loginView", "loginButton"))
        );
    }

    /**
     * Cài đặt các chuỗi văn bản (I18n) cho UI từ config service.
     */
    private void setupLocalization() {
        titleLabel.setText(configService.getString("loginView", "title"));
        serverUrlField.setPromptText(configService.getString("loginView", "serverUrlPrompt"));
        usernameField.setPromptText(configService.getString("loginView", "usernamePrompt"));
        passwordField.setPromptText(configService.getString("loginView", "passwordPrompt"));
    }

    /**
     * Xử lý sự kiện onAction của nút Đăng nhập (được gọi từ FXML).
     * Ủy thác hành động cho ViewModel.
     */
    @FXML
    private void handleLoginButtonAction() {
        viewModel.loginCommand();
    }

    /**
     * Xử lý sự kiện nhấn Enter trên trường Mật khẩu (được gọi từ FXML).
     * Ủy thác hành động cho ViewModel.
     */
    @FXML
    private void handlePasswordKeyPress() {
        viewModel.loginCommand();
    }
}