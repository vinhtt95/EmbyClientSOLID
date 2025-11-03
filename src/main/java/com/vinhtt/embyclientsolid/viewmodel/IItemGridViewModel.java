package com.vinhtt.embyclientsolid.viewmodel;

import com.vinhtt.embyclientsolid.model.GridNavigationState;
import com.vinhtt.embyclientsolid.model.Tag;
import embyclient.model.BaseItemDto;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.collections.ObservableList;

/**
 * Interface (Hợp đồng) cho ItemGridViewModel (Cột 2 - Lưới Item).
 * Định nghĩa trạng thái và hành vi của Cột 2.
 * (UR-19 đến UR-29).
 */
public interface IItemGridViewModel {

    // --- Trạng thái (Binding) ---

    /**
     * Báo cáo trạng thái đang tải (loading) của Cột 2.
     *
     * @return Property (chỉ đọc) cho biết đang tải trang.
     */
    ReadOnlyBooleanProperty loadingProperty();

    /**
     * Cung cấp thông báo trạng thái (ví dụ: "Thư viện rỗng", "Đang tải trang 1/10...").
     *
     * @return Property (chỉ đọc) chứa thông báo.
     */
    ReadOnlyStringProperty statusMessageProperty();

    /**
     * Yêu cầu View hiển thị thông báo trạng thái (thay vì lưới item).
     *
     * @return Property (chỉ đọc) {@code true} nếu cần hiển thị status.
     */
    ReadOnlyBooleanProperty showStatusMessageProperty();

    /**
     * Cung cấp danh sách các item cho trang hiện tại.
     *
     * @return Danh sách (có thể quan sát) các {@link BaseItemDto}.
     */
    ObservableList<BaseItemDto> getItems();

    /**
     * Quản lý item hiện đang được chọn trong lưới (UR-26).
     *
     * @return Property (hai chiều) chứa item được chọn.
     */
    ObjectProperty<BaseItemDto> selectedItemProperty();

    /**
     * Bắn tín hiệu (event) yêu cầu Controller cuộn (scroll) lưới (UR-24).
     *
     * @return Property (chỉ đọc) chứa hành động cuộn {@link GridNavigationState.ScrollAction}.
     */
    ReadOnlyObjectProperty<GridNavigationState.ScrollAction> scrollActionProperty();

    // --- Sắp xếp (Binding) ---

    /**
     * (Được gọi bởi MainViewModel) Thiết lập tiêu chí sắp xếp mới (UR-21).
     *
     * @param sortBy Tiêu chí (ví dụ: "SortName", "DateCreated").
     */
    void setSortBy(String sortBy);

    /**
     * (Được gọi bởi MainViewModel) Thiết lập thứ tự sắp xếp mới (UR-22).
     *
     * @param sortOrder Thứ tự ("Ascending" hoặc "Descending").
     */
    void setSortOrder(String sortOrder);

    // --- Phân trang (Binding) ---

    /**
     * Báo cáo cho View (Controller) biết có trang tiếp theo (Next) hay không (UR-24).
     *
     * @return Property (chỉ đọc) {@code true} nếu có trang tiếp theo.
     */
    ReadOnlyBooleanProperty hasNextPageProperty();

    /**
     * Báo cáo cho View (Controller) biết có trang trước đó (Previous) hay không (UR-24).
     *
     * @return Property (chỉ đọc) {@code true} nếu có trang trước đó.
     */
    ReadOnlyBooleanProperty hasPreviousPageProperty();

    // --- Điều hướng (Binding) ---

    /**
     * Báo cáo cho View (Toolbar) biết có thể điều hướng Lùi (Back) hay không (UR-29).
     *
     * @return Property (chỉ đọc) {@code true} nếu có lịch sử (history).
     */
    ReadOnlyBooleanProperty canGoBackProperty();

    /**
     * Báo cáo cho View (Toolbar) biết có thể điều hướng Tiến (Forward) hay không (UR-29).
     *
     * @return Property (chỉ đọc) {@code true} nếu có lịch sử (forward history).
     */
    ReadOnlyBooleanProperty canGoForwardProperty();

    // --- Hành động (Commands) ---

    /**
     * Tải các item dựa trên thư mục cha (khi click Cột 1 hoặc nút Home) (UR-10, UR-19).
     *
     * @param parentId ID của thư mục cha (hoặc {@code null} cho Home).
     */
    void loadItemsByParentId(String parentId);

    /**
     * Tải các item dựa trên từ khóa tìm kiếm (UR-20).
     *
     * @param keywords Từ khóa.
     */
    void searchItems(String keywords);

    /**
     * Tải các item dựa trên một chip (Tag, Studio...) (UR-36).
     *
     * @param chip     Đối tượng {@link Tag} đã được click.
     * @param chipType Loại chip ("TAG", "STUDIO", "PEOPLE", "GENRE").
     */
    void loadItemsByChip(Tag chip, String chipType);

    /**
     * Tải trang tiếp theo (UR-24).
     */
    void loadNextPage();

    /**
     * Tải trang trước đó (UR-24).
     */
    void loadPreviousPage();

    /**
     * Điều hướng Lùi (Back) trong lịch sử (UR-13, UR-14, UR-29).
     */
    void navigateBack();

    /**
     * Điều hướng Tiến (Forward) trong lịch sử (UR-13, UR-14, UR-29).
     */
    void navigateForward();

    /**
     * Phát (play) một item (double-click hoặc hotkey) (UR-27, UR-13).
     *
     * @param item Item cần phát.
     */
    void playItemCommand(BaseItemDto item);

    /**
     * Chọn item tiếp theo trong lưới (Hotkey Cmd+N) (UR-13).
     */
    void selectNextItem();

    /**
     * Chọn item trước đó trong lưới (Hotkey Cmd+P) (UR-13).
     */
    void selectPreviousItem();

    /**
     * Chọn và tự động phát item tiếp theo (Hotkey Cmd+Shift+N) (UR-13).
     */
    void selectAndPlayNextItem();

    /**
     * Chọn và tự động phát item trước đó (Hotkey Cmd+Shift+P) (UR-13).
     */
    void selectAndPlayPreviousItem();

    /**
     * Kiểm tra cờ (flag) "phát sau khi chọn".
     *
     * @return {@code true} nếu hotkey (Cmd+Shift+N/P) vừa được nhấn.
     */
    boolean isPlayAfterSelect();

    /**
     * Xóa (reset) cờ "phát sau khi chọn".
     */
    void clearPlayAfterSelect();

    /**
     * Xây dựng URL cho ảnh thumbnail (Primary) của item (UR-25).
     *
     * @param item Item DTO.
     * @return Chuỗi URL đầy đủ.
     */
    String getPrimaryImageUrl(BaseItemDto item);
}