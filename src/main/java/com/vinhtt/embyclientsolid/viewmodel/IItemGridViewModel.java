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
 * Interface cho ItemGridViewModel (Cột 2).
 * (UR-19 đến UR-29).
 *
 * (Cập nhật GĐ 9: Thêm getPrimaryImageUrl)
 */
public interface IItemGridViewModel {

    // --- Trạng thái (Binding) ---
    ReadOnlyBooleanProperty loadingProperty();
    ReadOnlyStringProperty statusMessageProperty();
    ReadOnlyBooleanProperty showStatusMessageProperty();
    ObservableList<BaseItemDto> getItems();
    ObjectProperty<BaseItemDto> selectedItemProperty();
    ReadOnlyObjectProperty<GridNavigationState.ScrollAction> scrollActionProperty();

    // --- Sắp xếp (Binding) ---
    void setSortBy(String sortBy);
    void setSortOrder(String sortOrder);

    // --- Phân trang (Binding) ---
    ReadOnlyBooleanProperty hasNextPageProperty();
    ReadOnlyBooleanProperty hasPreviousPageProperty();

    // --- Điều hướng (Binding) ---
    ReadOnlyBooleanProperty canGoBackProperty();
    ReadOnlyBooleanProperty canGoForwardProperty();

    // --- Hành động (Commands) ---
    void loadItemsByParentId(String parentId);
    void searchItems(String keywords);
    void loadItemsByChip(Tag chip, String chipType);
    void loadNextPage();
    void loadPreviousPage();
    void navigateBack();
    void navigateForward();
    void playItemCommand(BaseItemDto item);
    void selectNextItem();
    void selectPreviousItem();
    void selectAndPlayNextItem();
    void selectAndPlayPreviousItem();
    boolean isPlayAfterSelect();
    void clearPlayAfterSelect();
    String getPrimaryImageUrl(BaseItemDto item);
}