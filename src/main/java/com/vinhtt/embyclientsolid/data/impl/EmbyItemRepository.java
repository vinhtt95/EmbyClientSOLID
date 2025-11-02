package com.vinhtt.embyclientsolid.data.impl;

import com.vinhtt.embyclientsolid.core.IEmbySessionService;
import com.vinhtt.embyclientsolid.data.IItemRepository;
import com.vinhtt.embyclientsolid.model.Tag;
import embyclient.ApiClient;
import embyclient.ApiException;
import embyclient.api.ImageServiceApi;
import embyclient.api.ItemsServiceApi;
import embyclient.api.UserLibraryServiceApi;
import embyclient.model.BaseItemDto;
import embyclient.model.ImageInfo;
import embyclient.model.QueryResultBaseItemDto;

import java.util.Collections;
import java.util.List;

/**
 * Triển khai (Implementation) của IItemRepository.
 * Phụ thuộc vào IEmbySessionService để lấy ApiClient.
 *
 * (ĐÃ SỬA LỖI LOGIC: Tách biệt logic getItemsByParentId (cho Tree)
 * khỏi getItemsPaginated (cho Grid) theo đúng yêu cầu.)
 */
public class EmbyItemRepository implements IItemRepository {

    private final IEmbySessionService sessionService;
    private final ApiClient apiClient;

    /**
     * Khởi tạo Repository với Session Service.
     * @param sessionService Service đã được tiêm (DI).
     */
    public EmbyItemRepository(IEmbySessionService sessionService) {
        this.sessionService = sessionService;
        this.apiClient = sessionService.getApiClient();
    }

    // --- Helpers để lấy API services ---
    private ItemsServiceApi getItemsService() {
        return new ItemsServiceApi(apiClient);
    }

    private UserLibraryServiceApi getUserLibraryServiceApi() {
        return new UserLibraryServiceApi(apiClient);
    }

    private ImageServiceApi getImageServiceApi() {
        return new ImageServiceApi(apiClient);
    }

    @Override
    public List<BaseItemDto> getRootViews() throws ApiException {
        String userId = sessionService.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("Chưa đăng nhập. Không thể lấy root views.");
        }

        // Logic này đúng, lấy các thư mục gốc của User
        QueryResultBaseItemDto result = getItemsService().getUsersByUseridItems(userId, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        if (result != null && result.getItems() != null) {
            return result.getItems();
        }
        return Collections.emptyList();
    }

    /**
     * (SỬA LỖI LOGIC TẠI ĐÂY)
     * Lấy các items con dựa trên parentId (dùng cho Cột 1 - Tree).
     * Hàm này KHÔNG đệ quy (recursive=null) và lấy TẤT CẢ các loại item
     * (includeItemTypes=null) để ViewModel có thể lọc ra các thư mục.
     * (UR-17).
     *
     * Logic này dựa trên RequestEmby.getQueryResultBaseItemDto từ dự án cũ.
     */
    @Override
    public List<BaseItemDto> getItemsByParentId(String parentId) throws ApiException {
        // Logic từ RequestEmby.getQueryResultBaseItemDto
        QueryResultBaseItemDto result = getItemsService().getItems(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null,
                null,
                "Ascending",
                parentId,
                null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                "SortName",
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        if (result != null && result.getItems() != null) {
            return result.getItems();
        }
        return Collections.emptyList();
    }

    /**
     * Lấy các items con (dùng cho Cột 2 - Grid).
     * Hàm này CÓ đệ quy (recursive=true) và chỉ lấy "Movie"
     * (includeItemTypes="Movie").
     */
    @Override
    public QueryResultBaseItemDto getItemsPaginated(String parentId, int startIndex, int limit, String sortOrder, String sortBy) throws ApiException {
        // Logic từ RequestEmby.getQueryResultFullBaseItemDto
        QueryResultBaseItemDto result = getItemsService().getItems(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, startIndex, limit, true, null, sortOrder, parentId, "OfficialRating,CriticRating", null, "Movie", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, sortBy, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        if (result != null) {
            return result;
        }
        // Trả về kết quả rỗng an toàn
        QueryResultBaseItemDto emptyResult = new QueryResultBaseItemDto();
        emptyResult.setItems(Collections.emptyList());
        emptyResult.setTotalRecordCount(0);
        return emptyResult;
    }

    @Override
    public QueryResultBaseItemDto searchItemsPaginated(String keywords, int startIndex, int limit, String sortOrder, String sortBy) throws ApiException {
        // Logic từ RequestEmby.searchBaseItemDto
        QueryResultBaseItemDto result = getItemsService().getItems(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                startIndex, // startIndex
                limit, // limit
                true, // recursive: true
                keywords, // searchTerm
                sortOrder, // sortOrder
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                sortBy, // sortBy
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        if (result != null) {
            return result;
        }
        QueryResultBaseItemDto emptyResult = new QueryResultBaseItemDto();
        emptyResult.setItems(Collections.emptyList());
        emptyResult.setTotalRecordCount(0);
        return emptyResult;
    }

    @Override
    public List<BaseItemDto> getItemsByChip(Tag chip, String chipType, Integer startIndex, Integer limit, boolean recursive) throws ApiException {
        // Logic này từ EmbyService.java cũ
        String apiParam;

        switch (chipType) {
            case "TAG":
                apiParam = chip.serialize(); // Tag dùng tên đã serialize
                return getItemsService().getItems(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, startIndex, limit, recursive, null, "Ascending", null, null, null, "Movie,Series,Video,Game", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, apiParam, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null).getItems();
            case "STUDIO":
                apiParam = chip.getId(); // Studio dùng ID
                if (apiParam == null) throw new ApiException("Studio ID is null for: " + chip.getDisplayName());
                return getItemsService().getItems(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, startIndex, limit, recursive, null, "Ascending", null, null, null, "Movie,Series,Video,Game", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, apiParam, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,null).getItems();
            case "PEOPLE":
                apiParam = chip.getId(); // People dùng ID
                if (apiParam == null) throw new ApiException("People ID is null for: ".concat(chip.getDisplayName()));
                return getItemsService().getItems(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, startIndex, limit, recursive, null, "Ascending", null, null, null, "Movie,Series,Video,Game", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, apiParam, null, null, apiParam, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,null,null).getItems();
            case "GENRE":
                apiParam = chip.getDisplayName(); // Genre dùng tên hiển thị
                return getItemsService().getItems(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, startIndex, limit, recursive, null, null, null, null, null, "Movie, Series, Video, Game, MusicAlbum", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,null).getItems();
            default:
                return Collections.emptyList();
        }
    }

    @Override
    public BaseItemDto getFullItemDetails(String itemId) throws ApiException {
        String userId = sessionService.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("Chưa đăng nhập. Không thể lấy chi tiết item.");
        }
        // Logic từ ItemRepository.getFullItemDetails
        return getUserLibraryServiceApi().getUsersByUseridItemsById(userId, itemId);
    }

    @Override
    public List<ImageInfo> getItemImages(String itemId) throws ApiException {
        // Logic từ ItemRepository.getItemImages
        List<ImageInfo> images = getImageServiceApi().getItemsByIdImages(itemId);
        if (images != null) {
            return images;
        }
        return Collections.emptyList();
    }
}