package com.vinhtt.embyclientsolid.data;

import com.vinhtt.embyclientsolid.model.SuggestionItem;
import com.vinhtt.embyclientsolid.model.Tag;
import embyclient.ApiException;

import java.util.List;

/**
 * Interface cho việc ĐỌC (Query) dữ liệu tĩnh hoặc dữ liệu gợi ý.
 * (UR-35).
 */
public interface IStaticDataRepository {

    /**
     * Lấy danh sách tất cả các tag đã dùng trong thư viện.
     * (UR-35).
     * @return Danh sách các Tag.
     * @throws ApiException Nếu API call thất bại.
     */
    List<Tag> getAllUsedTags() throws ApiException;

    /**
     * Lấy danh sách Genres gợi ý.
     * (UR-35).
     * @return Danh sách SuggestionItem (Type=Genre).
     * @throws ApiException Nếu API call thất bại.
     */
    List<SuggestionItem> getGenreSuggestions() throws ApiException;

    /**
     * Lấy danh sách Studios gợi ý.
     * (UR-35).
     * @return Danh sách SuggestionItem (Type=Studio).
     * @throws ApiException Nếu API call thất bại.
     */
    List<SuggestionItem> getStudioSuggestions() throws ApiException;

    /**
     * Lấy danh sách People gợi ý.
     * (UR-35).
     * @return Danh sách SuggestionItem (Type=Person).
     * @throws ApiException Nếu API call thất bại.
     */
    List<SuggestionItem> getPeopleSuggestions() throws ApiException;
}