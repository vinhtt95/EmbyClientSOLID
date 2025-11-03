package com.vinhtt.embyclientsolid.data;

import com.vinhtt.embyclientsolid.model.SuggestionItem;
import com.vinhtt.embyclientsolid.model.Tag;
import embyclient.ApiException;

import java.util.List;

/**
 * Interface trừu tượng hóa việc ĐỌC (Query) dữ liệu tĩnh
 * hoặc dữ liệu gợi ý (suggestions) từ Emby.
 * Dùng để điền dữ liệu cho dialog AddTag.
 */
public interface IStaticDataRepository {

    /**
     * Lấy danh sách tất cả các tag (đã parse, có thể là JSON hoặc simple)
     * đã được sử dụng trong thư viện (dùng cho gợi ý).
     *
     * @return Danh sách các Tag (đã parse).
     * @throws ApiException Nếu API call thất bại.
     */
    List<Tag> getAllUsedTags() throws ApiException;

    /**
     * Lấy danh sách tất cả các Genres (đã parse)
     * đã được sử dụng trong thư viện (dùng cho gợi ý).
     *
     * @return Danh sách các Tag (đã parse).
     * @throws ApiException Nếu API call thất bại.
     */
    List<Tag> getGenreSuggestions() throws ApiException;

    /**
     * Lấy danh sách tất cả các Studios (đã parse)
     * đã được sử dụng trong thư viện (dùng cho gợi ý).
     *
     * @return Danh sách các Tag (đã parse).
     * @throws ApiException Nếu API call thất bại.
     */
    List<Tag> getStudioSuggestions() throws ApiException;

    /**
     * Lấy danh sách tất cả các People (đã parse)
     * đã được sử dụng trong thư viện (dùng cho gợi ý).
     *
     * @return Danh sách các Tag (đã parse).
     * @throws ApiException Nếu API call thất bại.
     */
    List<Tag> getPeopleSuggestions() throws ApiException;
}