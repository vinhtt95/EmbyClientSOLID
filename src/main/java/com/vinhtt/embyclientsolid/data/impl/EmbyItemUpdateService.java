package com.vinhtt.embyclientsolid.data.impl;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.vinhtt.embyclientsolid.core.IEmbySessionService;
import com.vinhtt.embyclientsolid.data.IItemUpdateService;
import com.vinhtt.embyclientsolid.data.IItemRepository; // Cần để đọc item
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
 * Chịu trách nhiệm GHI dữ liệu: update, upload, delete, clone.
 * Logic được chuyển từ ItemDetailSaver,
 * ItemImageUpdater,
 * và RequestEmby.
 */
public class EmbyItemUpdateService implements IItemUpdateService {

    private final IEmbySessionService sessionService;
    private final ApiClient apiClient;
    // Cần một IItemRepository để đọc DTOs khi clone
    private final IItemRepository itemRepository;

    /**
     * Khởi tạo Service.
     * @param sessionService Service Session đã được tiêm (DI).
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

    @Override
    public void updateItem(String itemId, BaseItemDto item) throws ApiException {
        // Logic từ ItemDetailSaver.saveChanges
        getItemUpdateServiceApi().postItemsByItemid(item, itemId);
    }

    @Override
    public void uploadImage(String itemId, ImageType imageType, File imageFile) throws Exception {
        // Logic từ ItemImageUpdater.uploadImage

        // 1. Lấy OkHttpClient (client SẠCH, chỉ có auth)
        OkHttpClient client = new OkHttpClient();
        client.interceptors().add(sessionService.getAuthHeaderInterceptor());

        // 2. Xác định URL
        String serverUrl = apiClient.getBasePath();
        if (serverUrl.endsWith("/")) {
            serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
        }
        String url = String.format("%s/Items/%s/Images/%s",
                serverUrl,
                itemId,
                imageType.getValue());

        // 3. Đọc byte và mã hóa Base64
        byte[] fileBytes = Files.readAllBytes(imageFile.toPath());
        String base64String = Base64.getEncoder().encodeToString(fileBytes);

        // 4. Xác định MediaType gốc
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
     *
     */
    private MediaType getMediaType(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".png")) return MediaType.parse("image/png");
        if (name.endsWith(".jpg")) return MediaType.parse("image/jpeg");
        if (name.endsWith(".jpeg")) return MediaType.parse("image/jpeg");
        if (name.endsWith(".webp")) return MediaType.parse("image/webp");
        return null; // Không hỗ trợ
    }

    @Override
    public void deleteImage(String itemId, ImageType imageType, Integer imageIndex) throws ApiException {
        // Logic từ ItemImageUpdater.deleteImage
        Integer index = (imageIndex == null) ? 0 : imageIndex;
        getImageServiceApi().deleteItemsByIdImagesByTypeByIndex(itemId, index, imageType);
    }

    // --- CLONE METHODS ---
    // Logic được chuyển từ RequestEmby.java
    // và ItemService.java

    @Override
    public int cloneTags(String sourceItemId, String targetParentId) throws ApiException {
        BaseItemDto itemCopy = itemRepository.getFullItemDetails(sourceItemId);
        if (itemCopy == null) return 0;
        List<NameLongIdPair> listTagsItemCopy = itemCopy.getTagItems();

        List<BaseItemDto> listItemPaste = itemRepository.getItemsPaginated(targetParentId, 0, 9999, "Ascending", "SortName").getItems();
        if (listItemPaste == null || listItemPaste.isEmpty()) return 0;

        int updateCount = 0;
        for (BaseItemDto eachItemPaste : listItemPaste) {
            BaseItemDto itemPaste = itemRepository.getFullItemDetails(eachItemPaste.getId());
            if (itemPaste == null) continue;

            // Logic Merge
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

            itemPaste.setTagItems(new ArrayList<>(mergedTagsMap.values()));
            updateItem(itemPaste.getId(), itemPaste);
            updateCount++;
        }
        return updateCount;
    }

    @Override
    public int cloneStudios(String sourceItemId, String targetParentId) throws ApiException {
        BaseItemDto itemCopy = itemRepository.getFullItemDetails(sourceItemId);
        if (itemCopy == null) return 0;
        List<NameLongIdPair> listStudoItemCopy = itemCopy.getStudios();

        List<BaseItemDto> listItemPaste = itemRepository.getItemsPaginated(targetParentId, 0, 9999, "Ascending", "SortName").getItems();
        if (listItemPaste == null || listItemPaste.isEmpty()) return 0;

        int updateCount = 0;
        for (BaseItemDto eachItemPaste : listItemPaste) {
            BaseItemDto itemPaste = itemRepository.getFullItemDetails(eachItemPaste.getId());
            if (itemPaste == null) continue;

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

            itemPaste.setStudios(new ArrayList<>(mergedStudiosMap.values()));
            updateItem(itemPaste.getId(), itemPaste);
            updateCount++;
        }
        return updateCount;
    }

    @Override
    public int clonePeople(String sourceItemId, String targetParentId) throws ApiException {
        BaseItemDto itemCopy = itemRepository.getFullItemDetails(sourceItemId);
        if (itemCopy == null) return 0;
        List<BaseItemPerson> listPeopleItemCopy = itemCopy.getPeople();

        List<BaseItemDto> listItemPaste = itemRepository.getItemsPaginated(targetParentId, 0, 9999, "Ascending", "SortName").getItems();
        if (listItemPaste == null || listItemPaste.isEmpty()) return 0;

        int updateCount = 0;
        for (BaseItemDto eachItemPaste : listItemPaste) {
            if (eachItemPaste.getId().equals(sourceItemId)) continue; // Bỏ qua chính nó

            BaseItemDto itemPaste = itemRepository.getFullItemDetails(eachItemPaste.getId());
            if (itemPaste == null) continue;

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

            itemPaste.setPeople(new ArrayList<>(mergedPeopleMap.values()));
            updateItem(itemPaste.getId(), itemPaste);
            updateCount++;
        }
        return updateCount;
    }

    @Override
    public int cloneGenres(String sourceItemId, String targetParentId) throws ApiException {
        BaseItemDto itemCopy = itemRepository.getFullItemDetails(sourceItemId);
        if (itemCopy == null) return 0;
        List<NameLongIdPair> listGenresItemCopy = itemCopy.getGenreItems();

        List<BaseItemDto> listItemPaste = itemRepository.getItemsPaginated(targetParentId, 0, 9999, "Ascending", "SortName").getItems();
        if (listItemPaste == null || listItemPaste.isEmpty()) return 0;

        int updateCount = 0;
        for (BaseItemDto eachItemPaste : listItemPaste) {
            BaseItemDto itemPaste = itemRepository.getFullItemDetails(eachItemPaste.getId());
            if (itemPaste == null) continue;

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

            itemPaste.setGenreItems(new ArrayList<>(mergedGenresMap.values()));
            updateItem(itemPaste.getId(), itemPaste);
            updateCount++;
        }
        return updateCount;
    }
}