package com.vinhtt.embyclientsolid.viewmodel.impl;

import com.vinhtt.embyclientsolid.core.IAppNavigator;
import com.vinhtt.embyclientsolid.core.IConfigurationService;
import com.vinhtt.embyclientsolid.core.IEmbySessionService;
import com.vinhtt.embyclientsolid.core.INotificationService;
import embyclient.model.AuthenticationAuthenticationResult;
import embyclient.model.UserDto;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.*;

/**
 * ViewModel cho MainView (Giai đoạn 7).
 * ViewModel này đóng vai trò điều phối, quản lý trạng thái chung của ứng dụng
 * như thanh công cụ (sắp xếp, tìm kiếm) và thanh trạng thái.
 * (UR-5, UR-9, UR-10, UR-20, UR-21, UR-22).
 */
public class MainViewModel {

    /**
     * Định nghĩa các hằng số cho việc sắp xếp.
     * (UR-21)
     */
    public static final String SORT_BY_DATE_RELEASE = "ProductionYear,PremiereDate,SortName";
    public static final String SORT_BY_NAME = "SortName";
    public static final String SORT_BY_DATE_CREATED = "DateCreated";
    public static final String SORT_ORDER_DESCENDING = "Descending";
    public static final String SORT_ORDER_ASCENDING = "Ascending";

    // --- Services (DI) ---
    private final IEmbySessionService sessionService;
    private final IAppNavigator appNavigator;
    private final IConfigurationService configService;
    private final INotificationService notificationService;
    private final BooleanProperty loading = new SimpleBooleanProperty(false);

    // --- Properties (Trạng thái UI) ---
    private final StringProperty statusMessage;
    private final StringProperty searchKeyword = new SimpleStringProperty("");

    // (UR-21, UR-22)
    private final SimpleObjectProperty<String> sortBy = new SimpleObjectProperty<>(SORT_BY_DATE_CREATED);
    private final SimpleObjectProperty<String> sortOrder = new SimpleObjectProperty<>(SORT_ORDER_DESCENDING);

    /**
     * Khởi tạo MainViewModel.
     *
     * @param sessionService      Service phiên (DI).
     * @param appNavigator        Service điều hướng (DI).
     * @param notificationService Service thông báo (DI).
     * @param configService       Service cấu hình (DI).
     */
    public MainViewModel(
            IEmbySessionService sessionService,
            IAppNavigator appNavigator,
            INotificationService notificationService,
            IConfigurationService configService
    ) {
        this.sessionService = sessionService;
        this.appNavigator = appNavigator;
        this.notificationService = notificationService;
        this.configService = configService;

        // Kết nối statusMessage với notificationService (UR-9)
        this.statusMessage = new SimpleStringProperty();
        this.statusMessage.bind(notificationService.statusMessageProperty());
    }

    /**
     * Xử lý hành động nhấn nút "Home".
     * (UR-10)
     */
    public void homeCommand() {
        searchKeyword.set("");
        // Logic thực tế (clear tree selection, load grid)
        // sẽ được kích hoạt bởi MainController khi thấy searchKeyword rỗng.
    }

    /**
     * Xử lý hành động nhấn nút "Logout".
     * (UR-5)
     */
    public void logoutCommand() {
        sessionService.logout();
        appNavigator.showLogin();
    }

    /**
     * Xử lý hành động nhấn nút "Search".
     * (UR-20)
     */
    public void searchCommand(String keywords) {
        if (keywords == null || keywords.trim().isEmpty()) {
            homeCommand();
        } else {
            searchKeyword.set(keywords.trim());
        }
    }

    /**
     * Xử lý hành động nhấn nút "Sort By".
     * (UR-21)
     */
    public void toggleSortByCommand() {
        String current = sortBy.get();
        if (SORT_BY_DATE_RELEASE.equals(current)) {
            sortBy.set(SORT_BY_NAME);
        } else if (SORT_BY_NAME.equals(current)) {
            sortBy.set(SORT_BY_DATE_CREATED);
        } else {
            sortBy.set(SORT_BY_DATE_RELEASE);
        }
    }

    /**
     * Xử lý hành động nhấn nút "Sort Order".
     * (UR-22)
     */
    public void toggleSortOrderCommand() {
        if (SORT_ORDER_DESCENDING.equals(sortOrder.get())) {
            sortOrder.set(SORT_ORDER_ASCENDING);
        } else {
            sortOrder.set(SORT_ORDER_DESCENDING);
        }
    }

    // --- Getters cho Properties (Để Controller binding) ---

    public ReadOnlyStringProperty statusMessageProperty() {
        return statusMessage;
    }

    public BooleanProperty loadingProperty() {
        return loading;
    }

    public StringProperty searchKeywordProperty() {
        return searchKeyword;
    }

    public SimpleObjectProperty<String> sortByProperty() {
        return sortBy;
    }

    public SimpleObjectProperty<String> sortOrderProperty() {
        return sortOrder;
    }

    public IConfigurationService getConfigService() {
        return configService;
    }
}