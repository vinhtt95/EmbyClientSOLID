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
 * Controller cho LibraryTreeView.fxml (Cột 1).
 * Là một View "ngu ngốc", chỉ binding và gọi command.
 * (UR-15, UR-16, UR-17, UR-18).
 */
public class LibraryTreeController {

    @FXML private TreeView<LibraryTreeItem> treeView;
    @FXML private ProgressIndicator progressIndicator;

    private ILibraryTreeViewModel viewModel;
    private IConfigurationService configService;

    /**
     * Khởi tạo Controller.
     * (Không nhận DI qua constructor, nhận qua setViewModel).
     */
    public LibraryTreeController() {
    }

    /**
     * Được gọi bởi FXMLLoader sau khi các trường @FXML được tiêm.
     */
    @FXML
    public void initialize() {
        // (Không làm gì ở đây, chờ setViewModel)
    }

    /**
     * Được gọi bởi MainController (View-Coordinator) để tiêm ViewModel.
     *
     * @param viewModel ViewModel đã được khởi tạo.
     */
    public void setViewModel(ILibraryTreeViewModel viewModel, IConfigurationService configService) {
        this.viewModel = viewModel;
        this.configService = configService;

        // 1. Binding UI
        progressIndicator.visibleProperty().bind(viewModel.loadingProperty());
        treeView.rootProperty().bind(viewModel.rootItemProperty());
        treeView.setShowRoot(false); // Không hiển thị root ảo

        // 2. Cài đặt CellFactory (UR-17, UR-18)
        treeView.setCellFactory(tv -> new LibraryTreeCell());

        // 3. Binding Selection (Hai chiều)
        // (ViewModel -> View)
        viewModel.selectedTreeItemProperty().addListener((obs, oldVal, newVal) -> {
            SelectionModel<TreeItem<LibraryTreeItem>> selectionModel = treeView.getSelectionModel();
            if (newVal == null) {
                selectionModel.clearSelection();
            } else {
                selectionModel.select(newVal);
            }
        });
        // (View -> ViewModel)
        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            // Chỉ cập nhật ViewModel nếu giá trị thay đổi thực sự
            if (viewModel.selectedTreeItemProperty().get() != newVal) {
                viewModel.selectedTreeItemProperty().set(newVal);
            }
        });
    }

    /**
     * Xóa lựa chọn hiện tại trên TreeView.
     * (Được gọi bởi MainController - UR-10).
     */
    public void clearSelection() {
        if (treeView != null) {
            treeView.getSelectionModel().clearSelection();
        }
    }

    /**
     * Bắt đầu tải dữ liệu.
     * (Được gọi bởi MainController).
     */
    public void loadLibraries() {
        viewModel.loadLibraries();
    }

    /**
     * Lớp nội bộ cho TreeCell tùy chỉnh.
     */
    private class LibraryTreeCell extends TreeCell<LibraryTreeItem> {

        private final ContextMenu contextMenu = new ContextMenu();
        private final MenuItem copyIdItem;

        public LibraryTreeCell() {
            copyIdItem = new MenuItem(configService.getString("contextMenu", "copyId"));
            // (UR-18)
            copyIdItem.setOnAction(e -> {
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

        // Listener cho sự kiện Mở rộng (Expand) (UR-17)
        private final ChangeListener<Boolean> expansionListener = (obs, wasExpanded, isNowExpanded) -> {
            if (isNowExpanded && getTreeItem() != null) {
                viewModel.loadChildrenForItem(getTreeItem());
            }
        };

        private TreeItem<LibraryTreeItem> currentItem = null;

        @Override
        protected void updateItem(LibraryTreeItem item, boolean empty) {
            super.updateItem(item, empty);

            // Xóa listener cũ
            if (currentItem != null) {
                currentItem.expandedProperty().removeListener(expansionListener);
            }

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                setContextMenu(null);
                currentItem = null;
            } else {
                setText(item.toString()); // Dùng toString() của LibraryTreeItem
                currentItem = getTreeItem();

                if (item.isLoadingNode()) {
                    setContextMenu(null); // Không có context menu cho "Đang tải..."
                } else {
                    setContextMenu(contextMenu); // (UR-18)
                    // Gắn listener lazy loading (UR-17)
                    currentItem.expandedProperty().addListener(expansionListener);
                }
            }
        }
    }
}