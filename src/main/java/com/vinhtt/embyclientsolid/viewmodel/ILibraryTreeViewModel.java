package com.vinhtt.embyclientsolid.viewmodel;

import com.vinhtt.embyclientsolid.model.LibraryTreeItem;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.scene.control.TreeItem;

/**
 * Interface (Hợp đồng) cho LibraryTreeViewModel (Cột 1 - Cây thư viện).
 * Định nghĩa trạng thái và hành vi của Cột 1.
 * (UR-15, UR-16, UR-17).
 */
public interface ILibraryTreeViewModel {

    /**
     * Báo cáo trạng thái đang tải (loading) của Cột 1.
     *
     * @return Property (chỉ đọc) {@code true} nếu đang tải thư viện
     * hoặc tải các mục con.
     */
    ReadOnlyBooleanProperty loadingProperty();

    /**
     * Cung cấp {@link TreeItem} gốc (root) để liên kết (bind) với TreeView.
     * Root này là một root "ảo" (không hiển thị).
     *
     * @return Property (chỉ đọc) chứa TreeItem gốc.
     */
    ReadOnlyObjectProperty<TreeItem<LibraryTreeItem>> rootItemProperty();

    /**
     * Quản lý item hiện đang được chọn trong cây (UR-15).
     * MainController sẽ lắng nghe (listen) sự thay đổi của property này
     * để kích hoạt tải Cột 2.
     *
     * @return Property (hai chiều) chứa TreeItem được chọn.
     */
    ObjectProperty<TreeItem<LibraryTreeItem>> selectedTreeItemProperty();

    /**
     * Bắt đầu hành động (Command) tải các thư viện gốc (root views)
     * từ Emby (UR-15).
     */
    void loadLibraries();

    /**
     * Bắt đầu hành động (Command) tải các thư mục con cho một TreeItem
     * (Lazy Loading - UR-17).
     *
     * @param item Item cha cần tải các mục con.
     */
    void loadChildrenForItem(TreeItem<LibraryTreeItem> item);

    /**
     * Xóa lựa chọn hiện tại trên cây.
     * (Được gọi bởi MainController khi nhấn nút Home - UR-10).
     */
    void clearSelection();
}