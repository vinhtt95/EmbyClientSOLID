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
 * Interface cho AddTagViewModel (Dialog).
 * (Cập nhật: Hoàn thiện logic điều hướng phím và focus lost).
 */
public interface IAddTagViewModel {

    /**
     * Thiết lập bối cảnh cho dialog (ví dụ: đang thêm TAG hay STUDIO).
     * @param context Bối cảnh.
     */
    void setContext(SuggestionContext context);

    /**
     * @return Kết quả (Tag hoặc CopyId) sau khi dialog đóng.
     */
    AddTagResult getResult();

    // --- Properties (Trạng thái UI) ---
    ReadOnlyStringProperty titleProperty();
    ReadOnlyStringProperty simpleLabelProperty();
    ReadOnlyStringProperty simpleSuggestionLabelProperty();
    BooleanProperty simpleModeProperty();
    StringProperty simpleNameProperty();
    StringProperty keyProperty();
    StringProperty valueProperty();
    StringProperty copyIdProperty();
    ReadOnlyStringProperty copyStatusProperty();
    ReadOnlyBooleanProperty closeDialogProperty();

    // --- Properties Điều hướng Phím ---
    ReadOnlyIntegerProperty focusedKeyIndexProperty();
    ReadOnlyIntegerProperty focusedValueIndexProperty();
    ReadOnlyIntegerProperty focusedSimpleIndexProperty();

    // --- Suggestions (Binding) ---
    ObservableList<String> getSuggestionKeys();
    ObservableList<Tag> getSuggestionValues();
    ObservableList<SuggestionItem> getSuggestionSimple();

    // --- Commands (Hành động) ---

    /**
     * Xử lý tất cả các sự kiện phím (Up, Down, Enter, Tab) từ các trường text.
     * @param event Sự kiện phím.
     * @param sourceField Tên của trường nguồn ("key", "value", "simple").
     */
    void handleFieldKeyEvent(KeyEvent event, String sourceField);

    /**
     * (MỚI) Xử lý khi một trường text mất focus (blur).
     * @param sourceField Tên của trường nguồn ("key", "value", "simple").
     */
    void handleFocusLost(String sourceField);

    /**
     * Xử lý click vào một Key suggestion.
     * @param key Key được chọn.
     */
    void selectKeySuggestion(String key);

    /**
     * Xử lý click vào một Value suggestion.
     * @param tag Tag được chọn.
     */
    void selectValueSuggestion(Tag tag);

    /**
     * Xử lý click vào một Simple suggestion.
     * @param item Item được chọn.
     */
    void selectSimpleSuggestion(SuggestionItem item);

    /**
     * Xử lý nhấn nút OK.
     */
    void okCommand();

    /**
     * Xử lý nhấn nút Cancel.
     */
    void cancelCommand();

    /**
     * Xử lý nhấn nút Copy (Copy by ID).
     */
    void copyCommand();
}