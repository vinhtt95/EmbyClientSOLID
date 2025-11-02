package com.vinhtt.embyclientsolid.viewmodel;

import com.vinhtt.embyclientsolid.model.LibraryTreeItem;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.scene.control.TreeItem;

/**
 * Interface cho LibraryTreeViewModel (Cột 1).
 * (UR-15, UR-16, UR-17).
 */
public interface ILibraryTreeViewModel {

    /**
     * @return Property (chỉ-đọc) cho biết Cây thư viện có đang tải hay không.
     */
    ReadOnlyBooleanProperty loadingProperty();

    /**
     * @return Property (chỉ-đọc) chứa TreeItem gốc (root) để binding với TreeView.
     */
    ReadOnlyObjectProperty<TreeItem<LibraryTreeItem>> rootItemProperty();

    /**
     * @return Property (hai chiều) cho item đang được chọn trong TreeView.
     * MainController sẽ lắng nghe property này.
     */
    ObjectProperty<TreeItem<LibraryTreeItem>> selectedTreeItemProperty();

    /**
     * Bắt đầu tải các thư viện gốc (root views) từ Emby.
     * (UR-15).
     */
    void loadLibraries();

    /**
     * Tải các thư mục con cho một TreeItem (Lazy Loading).
     * (UR-17).
     * @param item Item cần tải con.
     */
    void loadChildrenForItem(TreeItem<LibraryTreeItem> item);

    /**
     * Xóa lựa chọn hiện tại trên cây.
     * (Dùng cho nút Home - UR-10).
     */
    void clearSelection();
}