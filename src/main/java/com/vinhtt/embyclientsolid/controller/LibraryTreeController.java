package com.vinhtt.embyclientsolid.controller;

import com.vinhtt.embyclientsolid.model.LibraryTreeItem;
import com.vinhtt.embyclientsolid.viewmodel.ILibraryTreeViewModel;
import com.vinhtt.embyclientsolid.core.IConfigurationService;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SelectionModel;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

/**
 * Controller (View) cho LibraryTreeView.fxml (Cột 1).
 * Lớp này chỉ chịu trách nhiệm binding TreeView với ILibraryTreeViewModel
 * và cài đặt CellFactory tùy chỉnh cho việc lazy loading và context menu.
 */
public class LibraryTreeController {

    // --- FXML UI Components ---
    @FXML private TreeView<LibraryTreeItem> treeView;
    @FXML private ProgressIndicator progressIndicator;

    private ILibraryTreeViewModel viewModel;
    private IConfigurationService configService;

    /**
     * Khởi tạo Controller.
     */
    public LibraryTreeController() {
    }

    @FXML
    public void initialize() {
        // Chờ setViewModel được gọi.
    }

    /**
     * Tiêm (inject) ViewModel và ConfigService vào Controller.
     * Đây là phương thức khởi tạo chính, được gọi bởi MainController.
     *
     * @param viewModel ViewModel chứa logic và trạng thái của Cột 1.
     * @param configService Service để lấy chuỗi I18n.
     */
    public void setViewModel(ILibraryTreeViewModel viewModel, IConfigurationService configService) {
        this.viewModel = viewModel;
        this.configService = configService;

        // 1. Binding UI
        // Liên kết indicator loading
        progressIndicator.visibleProperty().bind(viewModel.loadingProperty());
        // Liên kết root của TreeView với root trong VM
        treeView.rootProperty().bind(viewModel.rootItemProperty());
        // Ẩn root ảo (theo yêu cầu thiết kế)
        treeView.setShowRoot(false);

        // 2. Cài đặt CellFactory (UR-17, UR-18)
        // Sử dụng một TreeCell tùy chỉnh (định nghĩa bên dưới)
        treeView.setCellFactory(tv -> new LibraryTreeCell());

        // 3. Binding Selection (Hai chiều)

        // (ViewModel -> View)
        // Khi VM thay đổi lựa chọn (ví dụ: clearSelection), cập nhật TreeView
        viewModel.selectedTreeItemProperty().addListener((obs, oldVal, newVal) -> {
            SelectionModel<TreeItem<LibraryTreeItem>> selectionModel = treeView.getSelectionModel();
            if (newVal == null) {
                selectionModel.clearSelection();
            } else {
                selectionModel.select(newVal);
            }
        });

        // (View -> ViewModel)
        // Khi người dùng click chọn item trên TreeView, cập nhật VM
        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            // Chỉ cập nhật ViewModel nếu giá trị thực sự thay đổi
            if (viewModel.selectedTreeItemProperty().get() != newVal) {
                viewModel.selectedTreeItemProperty().set(newVal);
            }
        });
    }

    /**
     * Xóa lựa chọn hiện tại trên TreeView.
     * Được gọi bởi MainController khi nhấn nút Home (UR-10).
     */
    public void clearSelection() {
        if (treeView != null) {
            treeView.getSelectionModel().clearSelection();
        }
    }

    /**
     * Bắt đầu tải dữ liệu thư viện gốc.
     * Được gọi bởi MainController khi khởi chạy.
     */
    public void loadLibraries() {
        viewModel.loadLibraries();
    }

    /**
     * Lớp nội bộ cho TreeCell tùy chỉnh.
     * Xử lý hiển thị item, Context Menu (UR-18) và Lazy Loading (UR-17).
     */
    private class LibraryTreeCell extends TreeCell<LibraryTreeItem> {

        private final ContextMenu contextMenu = new ContextMenu();
        private final MenuItem copyIdItem;

        /**
         * Khởi tạo ContextMenu (UR-18) cho cell.
         */
        public LibraryTreeCell() {
            copyIdItem = new MenuItem(configService.getString("contextMenu", "copyId"));
            copyIdItem.setOnAction(e -> {
                // Chỉ thực hiện nếu item là item dữ liệu (không phải "Đang tải...")
                if (getItem() != null && !getItem().isLoadingNode()) {
                    String id = getItem().getItemDto().getId();
                    final Clipboard clipboard = Clipboard.getSystemClipboard();
                    final ClipboardContent content = new ClipboardContent();
                    content.putString(id);
                    clipboard.setContent(content);
                }
            });
            contextMenu.getItems().add(copyIdItem);
        }

        // (UR-17) Listener cho sự kiện Mở rộng (Expand)
        // Khi người dùng mở rộng một node, gọi command trong VM để tải con
        private final ChangeListener<Boolean> expansionListener = (obs, wasExpanded, isNowExpanded) -> {
            if (isNowExpanded && getTreeItem() != null) {
                viewModel.loadChildrenForItem(getTreeItem());
            }
        };

        // Biến tạm lưu TreeItem hiện tại để quản lý listener
        private TreeItem<LibraryTreeItem> currentItem = null;

        /**
         * Được JavaFX gọi khi cell cần được cập nhật (ví dụ: cuộn, thay đổi dữ liệu).
         *
         * @param item Đối tượng LibraryTreeItem để hiển thị.
         * @param empty true nếu cell rỗng, false nếu chứa dữ liệu.
         */
        @Override
        protected void updateItem(LibraryTreeItem item, boolean empty) {
            super.updateItem(item, empty);

            // Xóa listener cũ khỏi item cũ (nếu có) để tránh rò rỉ bộ nhớ
            if (currentItem != null) {
                currentItem.expandedProperty().removeListener(expansionListener);
            }

            if (empty || item == null) {
                // Cell rỗng
                setText(null);
                setGraphic(null);
                setContextMenu(null);
                currentItem = null;
            } else {
                // Cell chứa dữ liệu
                // Hiển thị text (dùng toString() của LibraryTreeItem)
                setText(item.toString());
                currentItem = getTreeItem();

                if (item.isLoadingNode()) {
                    // Nếu là node "Đang tải...", không hiển thị context menu
                    setContextMenu(null);
                } else {
                    // Nếu là node dữ liệu thật
                    setContextMenu(contextMenu); // (UR-18)
                    // Gắn listener lazy loading (UR-17)
                    currentItem.expandedProperty().addListener(expansionListener);
                }
            }
        }
    }
}