package com.vinhtt.embyclientsolid.viewmodel;

import com.vinhtt.embyclientsolid.model.SuggestionContext;
import com.vinhtt.embyclientsolid.model.SuggestionItem;
import com.vinhtt.embyclientsolid.model.Tag;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.scene.input.KeyEvent;

/**
 * Interface (Hợp đồng) cho AddTagViewModel.
 * Định nghĩa trạng thái và hành vi của dialog "Add Tag" (UR-35).
 * Lớp View (Controller) sẽ tương tác với Interface này.
 */
public interface IAddTagViewModel {

    /**
     * Thiết lập bối cảnh (context) khi mở dialog.
     * Báo cho ViewModel biết loại dữ liệu đang được thêm (ví dụ: TAG, STUDIO, PEOPLE)
     * để tải đúng các gợi ý.
     *
     * @param context Bối cảnh (enum) của dialog.
     */
    void setContext(SuggestionContext context);

    /**
     * Lấy kết quả cuối cùng sau khi dialog đóng.
     *
     * @return {@link AddTagResult} chứa Tag mới hoặc ID sao chép,
     * hoặc {@code null} nếu người dùng hủy.
     */
    AddTagResult getResult();

    // --- Properties (Trạng thái UI) ---

    /**
     * Cung cấp tiêu đề động cho dialog (ví dụ: "Thêm Tag Mới", "Thêm Studio Mới").
     *
     * @return Property (chỉ đọc) chứa tiêu đề.
     */
    ReadOnlyStringProperty titleProperty();

    /**
     * Cung cấp nhãn động cho chế độ "Simple" (ví dụ: "Tag Đơn giản", "Tên Studio").
     *
     * @return Property (chỉ đọc) chứa nhãn.
     */
    ReadOnlyStringProperty simpleLabelProperty();

    /**
     * Cung cấp nhãn động cho khu vực gợi ý "Simple" (ví dụ: "Gợi ý Tag", "Gợi ý Studio").
     *
     * @return Property (chỉ đọc) chứa nhãn.
     */
    ReadOnlyStringProperty simpleSuggestionLabelProperty();

    /**
     * Quản lý trạng thái của RadioButton (Simple vs JSON).
     *
     * @return Property (hai chiều) cho biết chế độ Simple có đang được chọn hay không.
     */
    BooleanProperty simpleModeProperty();

    /**
     * Liên kết (bind) với trường text nhập liệu cho chế độ Simple.
     *
     * @return Property (hai chiều) chứa nội dung trường "Simple Name".
     */
    StringProperty simpleNameProperty();

    /**
     * Liên kết (bind) với trường text nhập liệu cho "Key" (chế độ JSON).
     *
     * @return Property (hai chiều) chứa nội dung trường "Key".
     */
    StringProperty keyProperty();

    /**
     * Liên kết (bind) với trường text nhập liệu cho "Value" (chế độ JSON).
     *
     * @return Property (hai chiều) chứa nội dung trường "Value".
     */
    StringProperty valueProperty();

    /**
     * Liên kết (bind) với trường text nhập liệu cho "Copy by ID".
     *
     * @return Property (hai chiều) chứa nội dung trường "Copy ID".
     */
    StringProperty copyIdProperty();

    /**
     * Cung cấp thông báo trạng thái cho hành động Copy by ID (ví dụ: "Đang tải...").
     *
     * @return Property (chỉ đọc) chứa thông báo trạng thái.
     */
    ReadOnlyStringProperty copyStatusProperty();

    /**
     * Bắn tín hiệu (event) yêu cầu Controller đóng dialog.
     *
     * @return Property (chỉ đọc) sẽ chuyển thành {@code true} khi dialog cần đóng.
     */
    ReadOnlyBooleanProperty closeDialogProperty();

    // --- Properties Điều hướng Phím (UR-35) ---

    /**
     * Cung cấp chỉ số (index) của chip "Key" đang được focus (dùng phím mũi tên).
     *
     * @return Property (chỉ đọc) chứa index.
     */
    ReadOnlyIntegerProperty focusedKeyIndexProperty();

    /**
     * Cung cấp chỉ số (index) của chip "Value" đang được focus (dùng phím mũi tên).
     *
     * @return Property (chỉ đọc) chứa index.
     */
    ReadOnlyIntegerProperty focusedValueIndexProperty();

    /**
     * Cung cấp chỉ số (index) của chip "Simple" đang được focus (dùng phím mũi tên).
     *
     * @return Property (chỉ đọc) chứa index.
     */
    ReadOnlyIntegerProperty focusedSimpleIndexProperty();

    // --- Danh sách Gợi ý (Binding) ---

    /**
     * Cung cấp danh sách các "Key" (JSON) đã lọc để hiển thị.
     *
     * @return Danh sách (có thể quan sát) các chuỗi Key.
     */
    ObservableList<String> getSuggestionKeys();

    /**
     * Cung cấp danh sách các "Value" (JSON) đã lọc để hiển thị.
     *
     * @return Danh sách (có thể quan sát) các đối tượng {@link Tag}.
     */
    ObservableList<Tag> getSuggestionValues();

    /**
     * Cung cấp danh sách các gợi ý "Simple" đã lọc để hiển thị.
     *
     * @return Danh sách (có thể quan sát) các đối tượng {@link SuggestionItem}.
     */
    ObservableList<SuggestionItem> getSuggestionSimple();

    // --- Commands (Hành động từ View) ---

    /**
     * Xử lý tất cả các sự kiện phím (Up, Down, Enter, Tab) từ các trường text
     * để điều hướng và tự động hoàn thành (UR-35).
     *
     * @param event       Sự kiện phím.
     * @param sourceField Tên của trường nguồn ("key", "value", "simple").
     */
    void handleFieldKeyEvent(KeyEvent event, String sourceField);

    /**
     * Xử lý khi một trường text mất focus (blur) để tự động hoàn thành (auto-complete).
     *
     * @param sourceField Tên của trường nguồn ("key", "value", "simple").
     */
    void handleFocusLost(String sourceField);

    /**
     * Xử lý hành động khi người dùng click vào một chip "Key" gợi ý.
     *
     * @param key Key được chọn.
     */
    void selectKeySuggestion(String key);

    /**
     * Xử lý hành động khi người dùng click vào một chip "Value" gợi ý.
     *
     * @param tag Tag (chứa Value) được chọn.
     */
    void selectValueSuggestion(Tag tag);

    /**
     * Xử lý hành động khi người dùng click vào một chip "Simple" gợi ý.
     *
     * @param item Item (chứa tên) được chọn.
     */
    void selectSimpleSuggestion(SuggestionItem item);

    /**
     * Xử lý hành động khi người dùng nhấn nút "Đồng ý".
     */
    void okCommand();

    /**
     * Xử lý hành động khi người dùng nhấn nút "Hủy".
     */
    void cancelCommand();

    /**
     * Xử lý hành động khi người dùng nhấn nút "Sao chép" (Copy by ID).
     */
    void copyCommand();
}