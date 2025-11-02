package com.vinhtt.embyclientsolid.data;

import com.vinhtt.embyclientsolid.model.SuggestionItem;
import com.vinhtt.embyclientsolid.model.Tag;
import embyclient.ApiException;

import java.util.List;

/**
 * Interface cho việc ĐỌC (Query) dữ liệu tĩnh hoặc dữ liệu gợi ý.
 * (UR-35).
 * (Cập nhật: Sửa kiểu trả về để hỗ trợ JSON cho tất cả các loại).
 */
public interface IStaticDataRepository {

    /**
     * Lấy danh sách tất cả các tag đã dùng trong thư viện.
     * (UR-35).
     * @return Danh sách các Tag (đã parse, có thể là JSON hoặc simple).
     * @throws ApiException Nếu API call thất bại.
     */
    List<Tag> getAllUsedTags() throws ApiException;

    /**
     * Lấy danh sách Genres gợi ý.
     * (UR-35).
     * @return Danh sách các Tag (đã parse, có thể là JSON hoặc simple).
     * @throws ApiException Nếu API call thất bại.
     */
    List<Tag> getGenreSuggestions() throws ApiException;

    /**
     * Lấy danh sách Studios gợi ý.
     * (UR-35).
     * @return Danh sách các Tag (đã parse, có thể là JSON hoặc simple).
     * @throws ApiException Nếu API call thất bại.
     */
    List<Tag> getStudioSuggestions() throws ApiException;

    /**
     * Lấy danh sách People gợi ý.
     * (UR-35).
     * @return Danh sách các Tag (đã parse, có thể là JSON hoặc simple).
     * @throws ApiException Nếu API call thất bại.
     */
    List<Tag> getPeopleSuggestions() throws ApiException;
}