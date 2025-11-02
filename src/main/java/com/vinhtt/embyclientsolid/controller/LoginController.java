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
 * Controller cho LoginView.fxml (View).
 * Lớp này "ngu ngốc" (dumb) và tuân thủ MVVM.
 * Nó nhận ViewModel qua constructor và chỉ thực hiện binding dữ liệu.
 * Logic được chuyển từ LoginController.java cũ.
 */
public class LoginController {

    // @FXML fields khớp với LoginView.fxml
    @FXML private Label titleLabel;
    @FXML private TextField serverUrlField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label statusLabel;

    private final ILoginViewModel viewModel;
    private final IConfigurationService configService;

    /**
     * Khởi tạo Controller với ViewModel và Services cần thiết (DI).
     *
     * @param viewModel     ViewModel cho màn hình này.
     * @param configService Service để lấy chuỗi I18n.
     */
    public LoginController(ILoginViewModel viewModel, IConfigurationService configService) {
        this.viewModel = viewModel;
        this.configService = configService;
    }

    /**
     * Được gọi bởi FXMLLoader sau khi tiêm (inject) các trường @FXML.
     */
    @FXML
    public void initialize() {
        // 1. Cài đặt I18n
        setupLocalization();

        // 2. Binding (Liên kết) UI với ViewModel
        serverUrlField.textProperty().bindBidirectional(viewModel.serverUrlProperty());
        usernameField.textProperty().bindBidirectional(viewModel.usernameProperty());
        passwordField.textProperty().bindBidirectional(viewModel.passwordProperty());
        statusLabel.textProperty().bind(viewModel.statusMessageProperty());

        // Binding trạng thái loading cho nút
        loginButton.disableProperty().bind(viewModel.loginInProgressProperty());
        loginButton.textProperty().bind(
                Bindings.when(viewModel.loginInProgressProperty())
                        .then(configService.getString("loginView", "loginInProgress"))
                        .otherwise(configService.getString("loginView", "loginButton"))
        );
    }

    /**
     * Cài đặt các chuỗi văn bản (I18n) từ config service.
     */
    private void setupLocalization() {
        titleLabel.setText(configService.getString("loginView", "title"));
        serverUrlField.setPromptText(configService.getString("loginView", "serverUrlPrompt"));
        usernameField.setPromptText(configService.getString("loginView", "usernamePrompt"));
        passwordField.setPromptText(configService.getString("loginView", "passwordPrompt"));
    }

    /**
     * Xử lý sự kiện nhấn nút Đăng nhập (từ FXML).
     * Chỉ gọi command trên ViewModel.
     */
    @FXML
    private void handleLoginButtonAction() {
        viewModel.loginCommand();
    }

    /**
     * Xử lý sự kiện nhấn Enter trên trường Mật khẩu (từ FXML).
     * Chỉ gọi command trên ViewModel.
     */
    @FXML
    private void handlePasswordKeyPress() {
        viewModel.loginCommand();
    }
}