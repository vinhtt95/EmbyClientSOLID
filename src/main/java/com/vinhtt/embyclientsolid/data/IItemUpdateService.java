package com.vinhtt.embyclientsolid.data;

import com.vinhtt.embyclientsolid.model.Tag;
import embyclient.ApiException;
import embyclient.model.BaseItemDto;
import embyclient.model.ImageType;
import java.io.File;
import java.util.List;

/**
 * Interface cho việc GHI (Command) dữ liệu Item.
 * Bao gồm: Cập nhật metadata, upload/xóa ảnh, clone thuộc tính.
 * (UR-31, UR-33, UR-34, UR-37, UR-41, UR-42, UR-49).
 */
public interface IItemUpdateService {

    /**
     * Cập nhật metadata cho một item.
     * (UR-31, UR-33, UR-34, UR-49).
     * @param itemId ID của item cần cập nhật.
     * @param item DTO chứa thông tin mới.
     * @throws ApiException Nếu API call thất bại.
     */
    void updateItem(String itemId, BaseItemDto item) throws ApiException;

    /**
     * Upload một file ảnh lên server Emby.
     * (UR-41, UR-42).
     * @param itemId ID của item.
     * @param imageType Loại ảnh (Primary, Backdrop, v.v.).
     * @param imageFile File ảnh trên máy.
     * @throws Exception Nếu upload thất bại.
     */
    void uploadImage(String itemId, ImageType imageType, File imageFile) throws Exception;

    /**
     * Xóa một ảnh khỏi item.
     * (UR-41).
     * @param itemId ID của item.
     * @param imageType Loại ảnh.
     * @param imageIndex Chỉ số (index) của ảnh (thường dùng cho Backdrop).
     * @throws ApiException Nếu API call thất bại.
     */
    void deleteImage(String itemId, ImageType imageType, Integer imageIndex) throws ApiException;

    /**
     * Nhân bản (merge) thuộc tính Tags từ item nguồn sang các item con của thư mục cha.
     * (UR-37).
     * @param sourceItemId ID của item nguồn (để lấy tags).
     * @param targetParentId ID của item cha (để tìm item đích).
     * @return Số lượng item con đã được cập nhật.
     * @throws ApiException Nếu API call thất bại.
     */
    int cloneTags(String sourceItemId, String targetParentId) throws ApiException;

    /**
     * Nhân bản (merge) thuộc tính Studios từ item nguồn sang các item con.
     * (UR-37).
     * @param sourceItemId ID của item nguồn.
     * @param targetParentId ID của item cha.
     * @return Số lượng item con đã được cập nhật.
     * @throws ApiException Nếu API call thất bại.
     */
    int cloneStudios(String sourceItemId, String targetParentId) throws ApiException;

    /**
     * Nhân bản (merge) thuộc tính People từ item nguồn sang các item con.
     * (UR-37).
     * @param sourceItemId ID của item nguồn.
     * @param targetParentId ID của item cha.
     * @return Số lượng item con đã được cập nhật.
     * @throws ApiException Nếu API call thất bại.
     */
    int clonePeople(String sourceItemId, String targetParentId) throws ApiException;

    /**
     * Nhân bản (merge) thuộc tính Genres từ item nguồn sang các item con.
     * (UR-37).
     * @param sourceItemId ID của item nguồn.
     * @param targetParentId ID của item cha.
     * @return Số lượng item con đã được cập nhật.
     * @throws ApiException Nếu API call thất bại.
     */
    int cloneGenres(String sourceItemId, String targetParentId) throws ApiException;
}