package com.vinhtt.embyclientsolid.data;

import com.vinhtt.embyclientsolid.model.Tag;
import embyclient.ApiException;
import embyclient.model.BaseItemDto;
import embyclient.model.ImageType;
import java.io.File;
import java.util.List;

/**
 * Interface trừu tượng hóa các hành động GHI (Command) dữ liệu liên quan đến Item.
 * Bao gồm: Cập nhật metadata, upload/xóa ảnh, clone thuộc tính.
 * (Tách biệt với IItemRepository theo nguyên tắc CQRS).
 */
public interface IItemUpdateService {

    /**
     * Cập nhật (ghi đè) metadata cho một item cụ thể trên server.
     *
     * @param itemId ID của item cần cập nhật.
     * @param item DTO chứa thông tin mới.
     * @throws ApiException Nếu API call thất bại.
     */
    void updateItem(String itemId, BaseItemDto item) throws ApiException;

    /**
     * Tải (upload) một file ảnh (từ máy local) lên server cho một item.
     *
     * @param itemId ID của item.
     * @param imageType Loại ảnh (Primary, Backdrop, v.v.).
     * @param imageFile File ảnh trên máy.
     * @throws Exception Nếu upload thất bại (ví dụ: lỗi IO, lỗi API).
     */
    void uploadImage(String itemId, ImageType imageType, File imageFile) throws Exception;

    /**
     * Xóa một ảnh (dựa trên loại và chỉ số index) khỏi một item.
     *
     * @param itemId ID của item.
     * @param imageType Loại ảnh.
     * @param imageIndex Chỉ số (index) của ảnh (thường dùng cho Backdrop, Primary mặc định là 0).
     * @throws ApiException Nếu API call thất bại.
     */
    void deleteImage(String itemId, ImageType imageType, Integer imageIndex) throws ApiException;

    /**
     * Nhân bản (sao chép và merge) thuộc tính Tags từ item nguồn
     * sang tất cả item con của thư mục đích.
     *
     * @param sourceItemId ID của item nguồn (để lấy tags).
     * @param targetParentId ID của item cha (để tìm các item đích).
     * @return Số lượng item con đã được cập nhật.
     * @throws ApiException Nếu API call thất bại.
     */
    int cloneTags(String sourceItemId, String targetParentId) throws ApiException;

    /**
     * Nhân bản (sao chép và merge) thuộc tính Studios từ item nguồn
     * sang tất cả item con của thư mục đích.
     *
     * @param sourceItemId ID của item nguồn.
     * @param targetParentId ID của item cha.
     * @return Số lượng item con đã được cập nhật.
     * @throws ApiException Nếu API call thất bại.
     */
    int cloneStudios(String sourceItemId, String targetParentId) throws ApiException;

    /**
     * Nhân bản (sao chép và merge) thuộc tính People từ item nguồn
     * sang tất cả item con của thư mục đích.
     *
     * @param sourceItemId ID của item nguồn.
     * @param targetParentId ID của item cha.
     * @return Số lượng item con đã được cập nhật.
     * @throws ApiException Nếu API call thất bại.
     */
    int clonePeople(String sourceItemId, String targetParentId) throws ApiException;

    /**
     * Nhân bản (sao chép và merge) thuộc tính Genres từ item nguồn
     * sang tất cả item con của thư mục đích.
     *
     * @param sourceItemId ID của item nguồn.
     * @param targetParentId ID của item cha.
     * @return Số lượng item con đã được cập nhật.
     * @throws ApiException Nếu API call thất bại.
     */
    int cloneGenres(String sourceItemId, String targetParentId) throws ApiException;
}