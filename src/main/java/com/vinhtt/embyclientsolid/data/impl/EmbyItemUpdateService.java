package com.vinhtt.embyclientsolid.data.impl;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.vinhtt.embyclientsolid.core.IEmbySessionService;
import com.vinhtt.embyclientsolid.data.IItemUpdateService;
import com.vinhtt.embyclientsolid.data.IItemRepository;
import embyclient.ApiClient;
import embyclient.ApiException;
import embyclient.api.ImageServiceApi;
import embyclient.api.ItemUpdateServiceApi;
import embyclient.model.BaseItemDto;
import embyclient.model.BaseItemPerson;
import embyclient.model.ImageType;
import embyclient.model.NameLongIdPair;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Triển khai (Implementation) của IItemUpdateService.
 * Chịu trách nhiệm thực thi các lệnh GHI (Command) dữ liệu: update, upload, delete, clone.
 * Phụ thuộc vào IEmbySessionService (để ghi) và IItemRepository (để đọc khi clone).
 */
public class EmbyItemUpdateService implements IItemUpdateService {

    private final IEmbySessionService sessionService;
    private final ApiClient apiClient;
    // Cần IItemRepository để ĐỌC DTOs khi thực hiện logic CLONE
    private final IItemRepository itemRepository;

    /**
     * Khởi tạo Service.
     *
     * @param sessionService Service Session (DI) để lấy ApiClient.
     * @param itemRepository Repository Đọc (DI) để lấy item khi clone.
     */
    public EmbyItemUpdateService(IEmbySessionService sessionService, IItemRepository itemRepository) {
        this.sessionService = sessionService;
        this.apiClient = sessionService.getApiClient();
        this.itemRepository = itemRepository;
    }

    // --- Helpers để lấy API services ---
    private ItemUpdateServiceApi getItemUpdateServiceApi() {
        return new ItemUpdateServiceApi(apiClient);
    }

    private ImageServiceApi getImageServiceApi() {
        return new ImageServiceApi(apiClient);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateItem(String itemId, BaseItemDto item) throws ApiException {
        // Gọi API để POST (cập nhật) DTO mới
        getItemUpdateServiceApi().postItemsByItemid(item, itemId);
    }

    /**
     * {@inheritDoc}
     *
     * Phương thức này phải sử dụng OkHttpClient tùy chỉnh vì Emby API (Swagger)
     * không hỗ trợ upload ảnh dạng Base64.
     */
    @Override
    public void uploadImage(String itemId, ImageType imageType, File imageFile) throws Exception {

        // 1. Lấy OkHttpClient (client SẠCH, chỉ có auth)
        // Chúng ta cần client này để thêm Interceptor xác thực
        OkHttpClient client = new OkHttpClient();
        client.interceptors().add(sessionService.getAuthHeaderInterceptor());

        // 2. Xây dựng URL API upload thủ công
        String serverUrl = apiClient.getBasePath();
        if (serverUrl.endsWith("/")) {
            serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
        }
        String url = String.format("%s/Items/%s/Images/%s",
                serverUrl,
                itemId,
                imageType.getValue());

        // 3. Đọc byte ảnh và mã hóa Base64
        byte[] fileBytes = Files.readAllBytes(imageFile.toPath());
        String base64String = Base64.getEncoder().encodeToString(fileBytes);

        // 4. Xác định MediaType (ví dụ: "image/jpeg")
        MediaType originalMediaType = getMediaType(imageFile);
        if (originalMediaType == null) {
            throw new IOException("Không hỗ trợ định dạng file ảnh: " + imageFile.getName());
        }

        // 5. Tạo RequestBody từ chuỗi Base64
        RequestBody body = RequestBody.create(originalMediaType, base64String);

        // 6. Build Request
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        // 7. Thực thi
        Response response = client.newCall(request).execute();

        // 8. Kiểm tra kết quả
        if (!response.isSuccessful()) {
            String responseBody = response.body() != null ? response.body().string() : "No response body";
            response.body().close(); // Đóng body
            throw new IOException("Upload thất bại (Code " + response.code() + "): " + responseBody);
        } else {
            response.body().close(); // Luôn đóng body
        }
    }

    /**
     * Helper lấy MediaType từ tên file.
     */
    private MediaType getMediaType(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".png")) return MediaType.parse("image/png");
        if (name.endsWith(".jpg")) return MediaType.parse("image/jpeg");
        if (name.endsWith(".jpeg")) return MediaType.parse("image/jpeg");
        if (name.endsWith(".webp")) return MediaType.parse("image/webp");
        return null; // Không hỗ trợ
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteImage(String itemId, ImageType imageType, Integer imageIndex) throws ApiException {
        // Gọi API xóa ảnh
        Integer index = (imageIndex == null) ? 0 : imageIndex;
        getImageServiceApi().deleteItemsByIdImagesByTypeByIndex(itemId, index, imageType);
    }

    // --- CLONE METHODS ---

    /**
     * {@inheritDoc}
     */
    @Override
    public int cloneTags(String sourceItemId, String targetParentId) throws ApiException {
        // Lấy item nguồn (để copy)
        BaseItemDto itemCopy = itemRepository.getFullItemDetails(sourceItemId);
        if (itemCopy == null) return 0;
        List<NameLongIdPair> listTagsItemCopy = itemCopy.getTagItems();

        // Lấy danh sách item đích (để paste)
        List<BaseItemDto> listItemPaste = itemRepository.getItemsPaginated(targetParentId, 0, 9999, "Ascending", "SortName").getItems();
        if (listItemPaste == null || listItemPaste.isEmpty()) return 0;

        int updateCount = 0;
        for (BaseItemDto eachItemPaste : listItemPaste) {
            BaseItemDto itemPaste = itemRepository.getFullItemDetails(eachItemPaste.getId());
            if (itemPaste == null) continue;

            // Logic Merge: Dùng Map để merge (ghi đè) tag cũ bằng tag mới nếu trùng tên
            Map<String, NameLongIdPair> mergedTagsMap = new HashMap<>();
            if (itemPaste.getTagItems() != null) {
                for (NameLongIdPair existingTag : itemPaste.getTagItems()) {
                    if (existingTag.getName() != null) mergedTagsMap.put(existingTag.getName(), existingTag);
                }
            }
            if (listTagsItemCopy != null) {
                for (NameLongIdPair newTag : listTagsItemCopy) {
                    if (newTag.getName() != null) mergedTagsMap.put(newTag.getName(), newTag);
                }
            }

            // Ghi đè danh sách tags và gọi API Update
            itemPaste.setTagItems(new ArrayList<>(mergedTagsMap.values()));
            updateItem(itemPaste.getId(), itemPaste);
            updateCount++;
        }
        return updateCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int cloneStudios(String sourceItemId, String targetParentId) throws ApiException {
        // Lấy item nguồn
        BaseItemDto itemCopy = itemRepository.getFullItemDetails(sourceItemId);
        if (itemCopy == null) return 0;
        List<NameLongIdPair> listStudoItemCopy = itemCopy.getStudios();

        // Lấy item đích
        List<BaseItemDto> listItemPaste = itemRepository.getItemsPaginated(targetParentId, 0, 9999, "Ascending", "SortName").getItems();
        if (listItemPaste == null || listItemPaste.isEmpty()) return 0;

        int updateCount = 0;
        for (BaseItemDto eachItemPaste : listItemPaste) {
            BaseItemDto itemPaste = itemRepository.getFullItemDetails(eachItemPaste.getId());
            if (itemPaste == null) continue;

            // Logic Merge (dùng Map)
            Map<String, NameLongIdPair> mergedStudiosMap = new HashMap<>();
            if (itemPaste.getStudios() != null) {
                for (NameLongIdPair existing : itemPaste.getStudios()) {
                    if (existing.getName() != null) mergedStudiosMap.put(existing.getName(), existing);
                }
            }
            if (listStudoItemCopy != null) {
                for (NameLongIdPair newStudio : listStudoItemCopy) {
                    if (newStudio.getName() != null) mergedStudiosMap.put(newStudio.getName(), newStudio);
                }
            }

            // Cập nhật
            itemPaste.setStudios(new ArrayList<>(mergedStudiosMap.values()));
            updateItem(itemPaste.getId(), itemPaste);
            updateCount++;
        }
        return updateCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int clonePeople(String sourceItemId, String targetParentId) throws ApiException {
        // Lấy item nguồn
        BaseItemDto itemCopy = itemRepository.getFullItemDetails(sourceItemId);
        if (itemCopy == null) return 0;
        List<BaseItemPerson> listPeopleItemCopy = itemCopy.getPeople();

        // Lấy item đích
        List<BaseItemDto> listItemPaste = itemRepository.getItemsPaginated(targetParentId, 0, 9999, "Ascending", "SortName").getItems();
        if (listItemPaste == null || listItemPaste.isEmpty()) return 0;

        int updateCount = 0;
        for (BaseItemDto eachItemPaste : listItemPaste) {
            if (eachItemPaste.getId().equals(sourceItemId)) continue; // Bỏ qua chính nó

            BaseItemDto itemPaste = itemRepository.getFullItemDetails(eachItemPaste.getId());
            if (itemPaste == null) continue;

            // Logic Merge (dùng Map)
            Map<String, BaseItemPerson> mergedPeopleMap = new HashMap<>();
            if (itemPaste.getPeople() != null) {
                for (BaseItemPerson existing : itemPaste.getPeople()) {
                    if (existing.getName() != null) mergedPeopleMap.put(existing.getName(), existing);
                }
            }
            if (listPeopleItemCopy != null) {
                for (BaseItemPerson newPerson : listPeopleItemCopy) {
                    if (newPerson.getName() != null) mergedPeopleMap.put(newPerson.getName(), newPerson);
                }
            }

            // Cập nhật
            itemPaste.setPeople(new ArrayList<>(mergedPeopleMap.values()));
            updateItem(itemPaste.getId(), itemPaste);
            updateCount++;
        }
        return updateCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int cloneGenres(String sourceItemId, String targetParentId) throws ApiException {
        // Lấy item nguồn
        BaseItemDto itemCopy = itemRepository.getFullItemDetails(sourceItemId);
        if (itemCopy == null) return 0;
        List<NameLongIdPair> listGenresItemCopy = itemCopy.getGenreItems();

        // Lấy item đích
        List<BaseItemDto> listItemPaste = itemRepository.getItemsPaginated(targetParentId, 0, 9999, "Ascending", "SortName").getItems();
        if (listItemPaste == null || listItemPaste.isEmpty()) return 0;

        int updateCount = 0;
        for (BaseItemDto eachItemPaste : listItemPaste) {
            BaseItemDto itemPaste = itemRepository.getFullItemDetails(eachItemPaste.getId());
            if (itemPaste == null) continue;

            // Logic Merge (dùng Map)
            Map<String, NameLongIdPair> mergedGenresMap = new HashMap<>();
            if (itemPaste.getGenreItems() != null) {
                for (NameLongIdPair existing : itemPaste.getGenreItems()) {
                    if (existing.getName() != null) mergedGenresMap.put(existing.getName(), existing);
                }
            }
            if (listGenresItemCopy != null) {
                for (NameLongIdPair newGenre : listGenresItemCopy) {
                    if (newGenre.getName() != null) mergedGenresMap.put(newGenre.getName(), newGenre);
                }
            }

            // Cập nhật
            itemPaste.setGenreItems(new ArrayList<>(mergedGenresMap.values()));
            updateItem(itemPaste.getId(), itemPaste);
            updateCount++;
        }
        return updateCount;
    }
}