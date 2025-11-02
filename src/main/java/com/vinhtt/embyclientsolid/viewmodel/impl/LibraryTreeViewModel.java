package com.vinhtt.embyclientsolid.viewmodel.impl;

import com.vinhtt.embyclientsolid.core.INotificationService;
import com.vinhtt.embyclientsolid.data.IItemRepository;
import com.vinhtt.embyclientsolid.model.LibraryTreeItem;
import com.vinhtt.embyclientsolid.viewmodel.ILibraryTreeViewModel;
import embyclient.ApiException;
import embyclient.model.BaseItemDto;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.scene.control.TreeItem;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Triển khai (Implementation) của ILibraryTreeViewModel.
 * Logic được chuyển từ LibraryTreeViewModel.java cũ.
 * (UR-15, UR-16, UR-17).
 *
 * (ĐÃ SỬA LỖI: Thêm quản lý trạng thái loading cho loadChildrenForItem)
 */
public class LibraryTreeViewModel implements ILibraryTreeViewModel {

    private final IItemRepository itemRepository;
    private final INotificationService notificationService;

    private final ReadOnlyBooleanWrapper loading = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyObjectWrapper<TreeItem<LibraryTreeItem>> rootItem = new ReadOnlyObjectWrapper<>();
    private final ObjectProperty<TreeItem<LibraryTreeItem>> selectedTreeItem = new SimpleObjectProperty<>();

    // "Loading..." node (dummy node)
    private static final TreeItem<LibraryTreeItem> DUMMY_NODE = new TreeItem<>(new LibraryTreeItem("Đang tải..."));

    /**
     * Khởi tạo ViewModel.
     * @param itemRepository      Repo Đọc Item (DI).
     * @param notificationService Service Thông báo (DI).
     */
    public LibraryTreeViewModel(IItemRepository itemRepository, INotificationService notificationService) {
        this.itemRepository = itemRepository;
        this.notificationService = notificationService;

        // Tạo root ảo (không hiển thị)
        TreeItem<LibraryTreeItem> root = new TreeItem<>();
        root.setExpanded(true);
        rootItem.set(root);
    }

    @Override
    public void loadLibraries() {
        loading.set(true);
        notificationService.showStatus("Đang tải thư viện...");

        new Thread(() -> {
            try {
                // 1. Lấy TẤT CẢ item gốc
                List<BaseItemDto> allRootItems = itemRepository.getRootViews();

                // 2. Lọc chỉ giữ lại FOLDER (UR-16)
                List<BaseItemDto> libraries = allRootItems.stream()
                        .filter(item -> Boolean.TRUE.equals(item.isIsFolder()))
                        .collect(Collectors.toList());

                Platform.runLater(() -> {
                    TreeItem<LibraryTreeItem> root = rootItem.get();
                    root.getChildren().clear();

                    // 3. Thêm các thư viện (đã lọc) làm con
                    for (BaseItemDto lib : libraries) {
                        TreeItem<LibraryTreeItem> libNode = new TreeItem<>(new LibraryTreeItem(lib));
                        // Thêm dummy node để kích hoạt lazy loading (UR-17)
                        libNode.getChildren().add(DUMMY_NODE);
                        root.getChildren().add(libNode);
                    }
                    loading.set(false);
                    notificationService.clearStatus();
                });

            } catch (Exception e) {
                System.err.println("Lỗi API khi tải thư viện: " + e.getMessage());
                Platform.runLater(() -> {
                    loading.set(false);
                    notificationService.showStatus("Lỗi tải thư viện: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * (SỬA LỖI TẠI ĐÂY)
     * Thêm logic `loading.set(true)` và `notificationService.showStatus`
     * để quản lý trạng thái loading khi tải con.
     */
    @Override
    public void loadChildrenForItem(TreeItem<LibraryTreeItem> item) {
        // Kiểm tra điều kiện (UR-17)
        if (item == null || item.getValue() == null || item.getValue().isLoadingNode()) {
            return;
        }
        // Kiểm tra xem đã load con chưa (nếu node con không phải là DUMMY_NODE)
        if (!item.isLeaf() && !item.getChildren().isEmpty() && item.getChildren().get(0) != DUMMY_NODE) {
            return; // Đã load rồi
        }

        String parentId = item.getValue().getItemDto().getId();

        // --- BẮT ĐẦU SỬA LỖI ---
        // Báo cáo trạng thái loading TRƯỚC KHI chạy luồng
        // (Chúng ta phải bọc trong Platform.runLater vì đây có thể được gọi từ một sự kiện UI)
        Platform.runLater(() -> {
            loading.set(true);
            notificationService.showStatus("Đang tải: " + item.getValue().toString());
        });
        // --- KẾT THÚC SỬA LỖI ---

        new Thread(() -> {
            try {
                List<BaseItemDto> allChildren = itemRepository.getItemsByParentId(parentId);
                System.out.println(allChildren.size());

                // Lọc chỉ FOLDER (UR-16)
                List<BaseItemDto> folderChildren = allChildren.stream()
                        .filter(child -> Boolean.TRUE.equals(child.isIsFolder()))
                        .collect(Collectors.toList());

                Platform.runLater(() -> {
                    item.getChildren().clear(); // Xóa dummy node

                    for (BaseItemDto childDto : folderChildren) {
                        TreeItem<LibraryTreeItem> childNode = new TreeItem<>(new LibraryTreeItem(childDto));
                        // Thêm dummy node cho các node con này
                        childNode.getChildren().add(DUMMY_NODE);
                        item.getChildren().add(childNode);
                    }
                    // Tự động expand
                    item.setExpanded(true);

                    // --- BẮT ĐẦU SỬA LỖI ---
                    // Báo cáo đã tải xong
                    loading.set(false);
                    notificationService.clearStatus();
                    // --- KẾT THÚC SỬA LỖI ---
                });

            } catch (ApiException e) {
                System.err.println("Lỗi API khi tải con cho cây: " + e.getMessage());
                Platform.runLater(() -> {
                    item.getChildren().clear(); // Xóa dummy node khi lỗi
                    item.setExpanded(false);

                    // --- BẮT ĐẦU SỬA LỖI ---
                    // Báo cáo lỗi
                    loading.set(false);
                    notificationService.showStatus("Lỗi tải cây con: " + e.getMessage());
                    // --- KẾT THÚC SỬA LỖI ---
                });
            }
        }).start();
    }

    @Override
    public void clearSelection() {
        this.selectedTreeItem.set(null);
    }

    // --- Getters cho Properties ---
    @Override public ReadOnlyBooleanProperty loadingProperty() { return loading.getReadOnlyProperty(); }
    @Override public ReadOnlyObjectProperty<TreeItem<LibraryTreeItem>> rootItemProperty() { return rootItem.getReadOnlyProperty(); }
    @Override public ObjectProperty<TreeItem<LibraryTreeItem>> selectedTreeItemProperty() { return selectedTreeItem; }
}