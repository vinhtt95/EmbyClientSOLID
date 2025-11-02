package com.vinhtt.embyclientsolid.viewmodel;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Interface cho LoginViewModel.
 * Định nghĩa "hợp đồng" (contract) mà LoginController (View) sẽ tương tác.
 *
 */
public interface ILoginViewModel {

    /**
     * @return Property cho Server URL, có thể binding hai chiều.
     */
    StringProperty serverUrlProperty();

    /**
     * @return Property cho Username, có thể binding hai chiều.
     */
    StringProperty usernameProperty();

    /**
     * @return Property cho Password, có thể binding hai chiều.
     */
    StringProperty passwordProperty();

    /**
     * @return Property chỉ-đọc cho thông báo trạng thái (ví dụ: "Lỗi: Sai mật khẩu").
     */
    ReadOnlyStringProperty statusMessageProperty();

    /**
     * @return Property chỉ-đọc cho biết quá trình đăng nhập đang diễn ra.
     */
    ReadOnlyBooleanProperty loginInProgressProperty();

    /**
     * @return Property chỉ-đọc, sẽ chuyển thành true khi đăng nhập thành công.
     * Dùng để kích hoạt điều hướng (navigation).
     */
    ReadOnlyBooleanProperty loginSuccessProperty();

    /**
     * Hành động (Command) được gọi khi người dùng nhấn nút Đăng nhập.
     *
     */
    void loginCommand();
}