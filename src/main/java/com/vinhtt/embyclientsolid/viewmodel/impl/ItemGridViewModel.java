package com.vinhtt.embyclientsolid.viewmodel.impl;

import com.vinhtt.embyclientsolid.core.IEmbySessionService; // <-- THÊM IMPORT
import com.vinhtt.embyclientsolid.core.ILocalInteractionService;
import com.vinhtt.embyclientsolid.core.INotificationService;
import com.vinhtt.embyclientsolid.data.IItemRepository;
import com.vinhtt.embyclientsolid.model.GridNavigationState;
import com.vinhtt.embyclientsolid.model.Tag;
import com.vinhtt.embyclientsolid.viewmodel.IItemGridViewModel;
import embyclient.ApiException;
import embyclient.model.BaseItemDto;
import embyclient.model.QueryResultBaseItemDto;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Stack;

/**
 * Triển khai (Implementation) của IItemGridViewModel (Cột 2).
 * (UR-19 đến UR-29).
 *
 * (Cập nhật GĐ 9: Sửa lỗi Status và Ảnh)
 */
public class ItemGridViewModel implements IItemGridViewModel {

    private static final int ITEMS_PER_LOAD = 50; // (UR-23)

    private static final double CELL_HEIGHT = 320;
    private static final double CELL_WIDTH = CELL_HEIGHT * 16 / 9;

    // --- Services (DI) ---
    private final IItemRepository itemRepository;
    private final INotificationService notificationService;
    private final ILocalInteractionService localInteractionService; // (Cho UR-27)
    private final IEmbySessionService sessionService; // <-- SỬA LỖI 1: Thêm service

    // --- Trạng thái nội bộ ---
    private int totalCount = 0;
    private int currentPageIndex = 0;
    private int totalPages = 0;
    private boolean isRestoringState = false; // Cờ ngăn push lịch sử khi back/forward

    // --- Trạng thái điều hướng hiện tại ---
    private GridNavigationState.StateType currentStateType = GridNavigationState.StateType.FOLDER;
    private String currentPrimaryParam; // Dùng cho ParentId (FOLDER) hoặc Keywords (SEARCH)
    private Tag currentChipModel = null;
    private String currentChipType = null;
    private String currentSortBy = MainViewModel.SORT_BY_DATE_CREATED;
    private String currentSortOrder = MainViewModel.SORT_ORDER_DESCENDING;

    // --- Stacks Điều hướng (UR-29) ---
    private final Stack<GridNavigationState> navigationHistory = new Stack<>();
    private final Stack<GridNavigationState> forwardHistory = new Stack<>();
    private final ReadOnlyBooleanWrapper canGoBack = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper canGoForward = new ReadOnlyBooleanWrapper(false);

    // --- Properties (Trạng thái UI) ---
    private final ReadOnlyBooleanWrapper loading = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyStringWrapper statusMessage = new ReadOnlyStringWrapper("");
    private final ReadOnlyBooleanWrapper showStatusMessage = new ReadOnlyBooleanWrapper(true);
    private final ObservableList<BaseItemDto> items = FXCollections.observableArrayList();
    private final ObjectProperty<BaseItemDto> selectedItem = new SimpleObjectProperty<>();
    private final ReadOnlyObjectWrapper<GridNavigationState.ScrollAction> scrollAction = new ReadOnlyObjectWrapper<>(GridNavigationState.ScrollAction.NONE);
    private final ReadOnlyBooleanWrapper hasNextPage = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper hasPreviousPage = new ReadOnlyBooleanWrapper(false);
    private final BooleanProperty playAfterSelect = new SimpleBooleanProperty(false);


    /**
     * Khởi tạo ViewModel.
     * @param itemRepository Repo Đọc Item (DI).
     * @param notificationService Service Thông báo (DI).
     * @param localInteractionService Service Tương tác (DI).
     * @param sessionService Service Phiên (DI). (SỬA LỖI 1)
     */
    public ItemGridViewModel(
            IItemRepository itemRepository,
            INotificationService notificationService,
            ILocalInteractionService localInteractionService,
            IEmbySessionService sessionService // <-- SỬA LỖI 1: Thêm tham số
    ) {
        this.itemRepository = itemRepository;
        this.notificationService = notificationService;
        this.localInteractionService = localInteractionService;
        this.sessionService = sessionService; // <-- SỬA LỖI 1: Lưu service

        // --- SỬA LỖI 2: Xóa listener tại đây ---
        // Không lắng nghe global status, chỉ đặt status của riêng Grid
        this.statusMessage.set("Vui lòng chọn thư viện...");
    }

    /**
     * (SỬA LỖI 1: Thêm hàm mới)
     * Lấy URL ảnh Primary (UR-25).
     * Logic được sao chép từ ItemDetailViewModel.
     */
    @Override
    public String getPrimaryImageUrl(BaseItemDto item) {
        if (item != null && item.getImageTags() != null && item.getImageTags().containsKey("Primary")) {
            String tag = item.getImageTags().get("Primary");
            String serverUrl = sessionService.getApiClient().getBasePath();
            // (Kích thước ảnh thumbnail)
            int width = (int) (CELL_WIDTH * 1.5); // Lấy ảnh lớn hơn 1 chút cho đẹp

            return String.format("%s/Items/%s/Images/Primary?tag=%s&maxWidth=%d&quality=90",
                    serverUrl, item.getId(), tag, width);
        }
        return null; // Controller sẽ xử lý ảnh placeholder
    }


    /**
     * Tạo một snapshot của trạng thái hiện tại.
     */
    private GridNavigationState createCurrentState() {
        String selectedId = (selectedItem.get() != null) ? selectedItem.get().getId() : null;
        if (currentStateType == GridNavigationState.StateType.CHIP) {
            return new GridNavigationState(
                    GridNavigationState.StateType.CHIP, currentChipModel, currentChipType,
                    currentSortBy, currentSortOrder, currentPageIndex, selectedId
            );
        } else {
            // FOLDER và SEARCH dùng chung
            return new GridNavigationState(
                    currentStateType, currentPrimaryParam,
                    currentSortBy, currentSortOrder, currentPageIndex, selectedId
            );
        }
    }

    /**
     * Đẩy trạng thái hiện tại vào stack lịch sử (UR-29).
     */
    private void pushCurrentStateToHistory() {
        if (isRestoringState) return;

        GridNavigationState currentState = createCurrentState();
        if (navigationHistory.isEmpty() || !navigationHistory.peek().equals(currentState)) {
            navigationHistory.push(currentState);
            canGoBack.set(true);
            // Hành động mới sẽ xóa lịch sử forward
            forwardHistory.clear();
            canGoForward.set(false);
        }
    }

    /**
     * Khôi phục một trạng thái từ lịch sử (UR-29).
     */
    private void restoreState(GridNavigationState state) {
        isRestoringState = true;

        // Khôi phục các giá trị
        this.currentSortBy = state.getSortBy();
        this.currentSortOrder = state.getSortOrder();

        // Gọi hàm load internal tương ứng
        switch (state.getType()) {
            case FOLDER:
                loadPageInternal(state.getPageIndex(), state.getPrimaryParam(), state.getSelectedItemId());
                break;
            case SEARCH:
                loadSearchPageInternal(state.getPageIndex(), state.getPrimaryParam(), state.getSelectedItemId());
                break;
            case CHIP:
                loadItemsByChipInternal(state.getPageIndex(), state.getChip(), state.getChipType(), state.getSelectedItemId());
                break;
        }
    }

    @Override
    public void navigateBack() {
        if (navigationHistory.isEmpty()) return;
        forwardHistory.push(createCurrentState());
        canGoForward.set(true);
        restoreState(navigationHistory.pop());
        canGoBack.set(!navigationHistory.isEmpty());
    }

    @Override
    public void navigateForward() {
        if (forwardHistory.isEmpty()) return;
        navigationHistory.push(createCurrentState());
        canGoBack.set(true);
        restoreState(forwardHistory.pop());
        canGoForward.set(!forwardHistory.isEmpty());
    }

    @Override
    public void setSortBy(String sortBy) {
        if (!this.currentSortBy.equals(sortBy)) {
            this.currentSortBy = sortBy;
            // Tải lại trang đầu tiên với sắp xếp mới
            loadItemsByParentId(this.currentPrimaryParam);
        }
    }

    @Override
    public void setSortOrder(String sortOrder) {
        if (!this.currentSortOrder.equals(sortOrder)) {
            this.currentSortOrder = sortOrder;
            // Tải lại trang đầu tiên với sắp xếp mới
            loadItemsByParentId(this.currentPrimaryParam);
        }
    }

    @Override
    public void loadItemsByParentId(String parentId) {
        // (UR-10, UR-19)
        pushCurrentStateToHistory();
        loadPageInternal(0, parentId, null);
    }

    @Override
    public void searchItems(String keywords) {
        // (UR-20)
        pushCurrentStateToHistory();
        loadSearchPageInternal(0, keywords, null);
    }

    @Override
    public void loadItemsByChip(Tag chip, String chipType) {
        // (UR-36)
        pushCurrentStateToHistory();
        loadItemsByChipInternal(0, chip, chipType, null);
    }

    @Override
    public void loadNextPage() {
        if (hasNextPage.get() && !loading.get()) {
            int nextPage = currentPageIndex + 1;
            scrollAction.set(GridNavigationState.ScrollAction.SCROLL_TO_TOP); // Yêu cầu cuộn

            if (currentStateType == GridNavigationState.StateType.SEARCH) {
                loadSearchPageInternal(nextPage, currentPrimaryParam, null);
            } else {
                loadPageInternal(nextPage, currentPrimaryParam, null);
            }
        }
    }

    @Override
    public void loadPreviousPage() {
        if (hasPreviousPage.get() && !loading.get()) {
            int prevPage = currentPageIndex - 1;
            scrollAction.set(GridNavigationState.ScrollAction.SCROLL_TO_BOTTOM); // Yêu cầu cuộn

            if (currentStateType == GridNavigationState.StateType.SEARCH) {
                loadSearchPageInternal(prevPage, currentPrimaryParam, null);
            } else {
                loadPageInternal(prevPage, currentPrimaryParam, null);
            }
        }
    }

    /**
     * Logic tải trang chính (cho FOLDER và HOME).
     */
    private void loadPageInternal(int pageIndex, String parentId, String itemIdToSelect) {
        loading.set(true);
        showStatusMessage.set(true); // Hiển thị status (của Cột 2)
        // --- SỬA LỖI 2: Gọi global status ---
        notificationService.showStatus("Đang tải items...");

        new Thread(() -> {
            try {
                QueryResultBaseItemDto result = itemRepository.getItemsPaginated(
                        parentId, (pageIndex * ITEMS_PER_LOAD), ITEMS_PER_LOAD, currentSortOrder, currentSortBy
                );

                Platform.runLater(() -> {
                    // Cập nhật trạng thái
                    if (!isRestoringState) {
                        currentStateType = GridNavigationState.StateType.FOLDER;
                        currentPrimaryParam = parentId;
                        currentChipModel = null;
                        currentChipType = null;
                    }
                    updateStateFromQueryResult(result, pageIndex, itemIdToSelect);
                    // --- SỬA LỖI 2: Clear global status ---
                    notificationService.clearStatus();
                });

            } catch (Exception e) {
                handleApiError(e);
            }
        }).start();
    }

    /**
     * Logic tải trang (cho SEARCH).
     */
    private void loadSearchPageInternal(int pageIndex, String keywords, String itemIdToSelect) {
        loading.set(true);
        showStatusMessage.set(true);
        // --- SỬA LỖI 2: Gọi global status ---
        notificationService.showStatus("Đang tìm kiếm...");

        new Thread(() -> {
            try {
                QueryResultBaseItemDto result = itemRepository.searchItemsPaginated(
                        keywords, (pageIndex * ITEMS_PER_LOAD), ITEMS_PER_LOAD, currentSortOrder, currentSortBy
                );

                Platform.runLater(() -> {
                    if (!isRestoringState) {
                        currentStateType = GridNavigationState.StateType.SEARCH;
                        currentPrimaryParam = keywords;
                        currentChipModel = null;
                        currentChipType = null;
                    }
                    updateStateFromQueryResult(result, pageIndex, itemIdToSelect);
                    // --- SỬA LỖI 2: Clear global status ---
                    notificationService.clearStatus();
                });

            } catch (Exception e) {
                handleApiError(e);
            }
        }).start();
    }

    /**
     * Logic tải trang (cho CHIP).
     */
    private void loadItemsByChipInternal(int pageIndex, Tag chip, String chipType, String itemIdToSelect) {
        loading.set(true);
        showStatusMessage.set(true);
        // --- SỬA LỖI 2: Gọi global status ---
        notificationService.showStatus("Đang tải theo chip...");

        new Thread(() -> {
            try {
                // (Giả định API trả về tất cả, không phân trang)
                List<BaseItemDto> chipItems = itemRepository.getItemsByChip(chip, chipType, 0, 50, true);

                QueryResultBaseItemDto result = new QueryResultBaseItemDto();
                result.setItems(chipItems);
                result.setTotalRecordCount(chipItems.size());

                Platform.runLater(() -> {
                    if (!isRestoringState) {
                        currentStateType = GridNavigationState.StateType.CHIP;
                        currentPrimaryParam = null;
                        currentChipModel = chip;
                        currentChipType = chipType;
                    }
                    updateStateFromQueryResult(result, 0, itemIdToSelect); // Luôn là trang 0
                    // --- SỬA LỖI 2: Clear global status ---
                    notificationService.clearStatus();
                });

            } catch (Exception e) {
                handleApiError(e);
            }
        }).start();
    }

    /**
     * Helper chung để cập nhật trạng thái UI sau khi có kết quả.
     */
    private void updateStateFromQueryResult(QueryResultBaseItemDto result, int pageIndex, String itemIdToSelect) {
        totalCount = result.getTotalRecordCount() != null ? result.getTotalRecordCount() : 0;
        totalPages = (int) Math.ceil((double) totalCount / ITEMS_PER_LOAD);
        currentPageIndex = pageIndex;

        hasNextPage.set(currentPageIndex < totalPages - 1);
        hasPreviousPage.set(currentPageIndex > 0);

        List<BaseItemDto> pageItems = result.getItems();
        items.setAll(pageItems);

        if (pageItems.isEmpty()) {
            if (totalCount > 0) {
                statusMessage.set("Trang này rỗng.");
            } else {
                statusMessage.set("Không tìm thấy item nào.");
            }
            showStatusMessage.set(true);
        } else {
            // Cập nhật status Cục bộ (của Cột 2)
            statusMessage.set(String.format("Đang hiển thị: %d/%d (%d items)", (pageIndex + 1), totalPages, totalCount));
            showStatusMessage.set(false); // Ẩn status Cục bộ, hiện grid
        }

        // Chọn item
        selectItem(itemIdToSelect, pageItems);

        loading.set(false); // Tắt loading Cục bộ (của Cột 2)
        isRestoringState = false; // Luôn reset cờ
        scrollAction.set(GridNavigationState.ScrollAction.NONE); // Reset cờ cuộn
    }

    /**
     * Logic chọn item sau khi tải.
     */
    private void selectItem(String itemIdToSelect, List<BaseItemDto> pageItems) {
        BaseItemDto itemToSelect = null;
        if (itemIdToSelect != null) {
            itemToSelect = pageItems.stream()
                    .filter(i -> i.getId() != null && i.getId().equals(itemIdToSelect))
                    .findFirst().orElse(null);
        }

        if (itemToSelect == null && !pageItems.isEmpty()) {
            itemToSelect = pageItems.get(0); // Mặc định chọn item đầu tiên
        }

        selectedItem.set(itemToSelect);
    }

    /**
     * Helper xử lý lỗi API.
     */
    private void handleApiError(Exception e) {
        System.err.println("Lỗi API khi tải Grid: " + e.getMessage());
        Platform.runLater(() -> {
            loading.set(false);
            // --- SỬA LỖI 2: Cập nhật cả 2 status ---
            String errorMessage = "Lỗi tải items: " + e.getMessage();
            statusMessage.set(errorMessage); // Status Cục bộ
            notificationService.showStatus(errorMessage); // Status Toàn cục
            showStatusMessage.set(true);
            isRestoringState = false;
        });
    }

    @Override
    public void playItemCommand(BaseItemDto item) {
        if (item == null) return;

        notificationService.showStatus("Đang lấy đường dẫn để phát...");

        new Thread(() -> {
            try {
                // (UR-27) Cần lấy DTO đầy đủ để có 'Path'
                BaseItemDto fullDetails = itemRepository.getFullItemDetails(item.getId());
                if (fullDetails.isIsFolder() != null && fullDetails.isIsFolder()) {
                    throw new IOException("Không thể phát thư mục.");
                }
                String path = fullDetails.getPath();
                if (path == null || path.isEmpty()) {
                    throw new FileNotFoundException("Không tìm thấy đường dẫn file media.");
                }

                localInteractionService.openFileOrFolder(path);
                Platform.runLater(notificationService::clearStatus);

            } catch (Exception e) {
                System.err.println("Lỗi khi Phát từ Grid: " + e.getMessage());
                Platform.runLater(() -> notificationService.showStatus("Lỗi phát file: " + e.getMessage()));
            }
        }).start();
    }

    // (UR-13)
    @Override
    public void selectNextItem() {
        if (items.isEmpty() || loading.get()) return;
        int currentIndex = items.indexOf(selectedItem.get());
        if (currentIndex < items.size() - 1) {
            selectedItem.set(items.get(currentIndex + 1));
        }
    }

    // (UR-13)
    @Override
    public void selectPreviousItem() {
        if (items.isEmpty() || loading.get()) return;
        int currentIndex = items.indexOf(selectedItem.get());
        if (currentIndex > 0) {
            selectedItem.set(items.get(currentIndex - 1));
        }
    }

    @Override public void selectAndPlayNextItem() { playAfterSelect.set(true); selectNextItem(); }
    @Override public void selectAndPlayPreviousItem() { playAfterSelect.set(true); selectPreviousItem(); }
    @Override public boolean isPlayAfterSelect() { return playAfterSelect.get(); }
    @Override public void clearPlayAfterSelect() { playAfterSelect.set(false); }

    // --- Getters cho Properties (Binding) ---
    @Override public ReadOnlyBooleanProperty loadingProperty() { return loading.getReadOnlyProperty(); }
    @Override public ReadOnlyStringProperty statusMessageProperty() { return statusMessage.getReadOnlyProperty(); }
    @Override public ReadOnlyBooleanProperty showStatusMessageProperty() { return showStatusMessage.getReadOnlyProperty(); }
    @Override public ObservableList<BaseItemDto> getItems() { return items; }
    @Override public ObjectProperty<BaseItemDto> selectedItemProperty() { return selectedItem; }
    @Override public ReadOnlyObjectProperty<GridNavigationState.ScrollAction> scrollActionProperty() { return scrollAction.getReadOnlyProperty(); }
    @Override public ReadOnlyBooleanProperty hasNextPageProperty() { return hasNextPage.getReadOnlyProperty(); }
    @Override public ReadOnlyBooleanProperty hasPreviousPageProperty() { return hasPreviousPage.getReadOnlyProperty(); }
    @Override public ReadOnlyBooleanProperty canGoBackProperty() { return canGoBack.getReadOnlyProperty(); }
    @Override public ReadOnlyBooleanProperty canGoForwardProperty() { return canGoForward.getReadOnlyProperty(); }
}