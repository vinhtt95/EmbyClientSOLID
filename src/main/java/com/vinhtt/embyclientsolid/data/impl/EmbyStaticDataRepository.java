package com.vinhtt.embyclientsolid.data.impl;

import com.vinhtt.embyclientsolid.core.IEmbySessionService;
import com.vinhtt.embyclientsolid.data.IStaticDataRepository;
import com.vinhtt.embyclientsolid.model.SuggestionItem;
import com.vinhtt.embyclientsolid.model.Tag;
import embyclient.ApiClient;
import embyclient.ApiException;
import embyclient.api.GenresServiceApi;
import embyclient.api.PersonsServiceApi;
import embyclient.api.StudiosServiceApi;
import embyclient.api.TagServiceApi;
import embyclient.model.BaseItemDto;
import embyclient.model.QueryResultBaseItemDto;
import embyclient.model.QueryResultUserLibraryTagItem;
import embyclient.model.UserLibraryTagItem;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Triển khai (Implementation) của IStaticDataRepository.
 * Lớp này chịu trách nhiệm ĐỌC (Query) dữ liệu gợi ý (Suggestions)
 * như Tags, Genres, Studios, People từ Emby API.
 */
public class EmbyStaticDataRepository implements IStaticDataRepository {

    private final ApiClient apiClient;

    /**
     * Khởi tạo Repository.
     *
     * @param sessionService Service Session (DI) để lấy ApiClient.
     */
    public EmbyStaticDataRepository(IEmbySessionService sessionService) {
        this.apiClient = sessionService.getApiClient();
    }

    /**
     * Hàm helper chung để chuyển đổi một danh sách DTO (BaseItemDto)
     * sang danh sách Model (Tag), tự động parse Name (có thể là JSON) và giữ lại ID.
     *
     * @param dtoList Danh sách DTO từ API.
     * @return Danh sách Tag (POJO).
     */
    private List<Tag> parseDtoListToTagList(List<BaseItemDto> dtoList) {
        if (dtoList == null) return Collections.emptyList();

        return dtoList.stream()
                // Sử dụng Tag.parse để phân tích Name (có thể là JSON)
                // và truyền cả ID gốc từ DTO
                .map(dto -> Tag.parse(dto.getName(), dto.getId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Tag> getAllUsedTags() throws ApiException {
        TagServiceApi tagServiceApi = new TagServiceApi(apiClient);
        // Gọi API lấy tất cả các Tag (UserLibraryTagItem)
        QueryResultUserLibraryTagItem listTag = tagServiceApi.getTags(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        if (listTag != null && listTag.getItems() != null) {
            return listTag.getItems().stream()
                    .map(UserLibraryTagItem::getName) // Lấy chuỗi Name thô
                    .filter(name -> name != null && !name.isEmpty())
                    .distinct()
                    // Parse chuỗi Name thô (có thể là JSON) thành Tag (POJO)
                    .map(rawName -> Tag.parse(rawName, null))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Tag> getGenreSuggestions() throws ApiException {
        GenresServiceApi genresServiceApi = new GenresServiceApi(apiClient);
        // Gọi API lấy tất cả Genres (BaseItemDto)
        QueryResultBaseItemDto genreResult = genresServiceApi.getGenres(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        // Dùng helper để parse danh sách DTO sang danh sách Tag
        return parseDtoListToTagList(genreResult != null ? genreResult.getItems() : null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Tag> getStudioSuggestions() throws ApiException {
        StudiosServiceApi studiosServiceApi = new StudiosServiceApi(apiClient);
        // Gọi API lấy tất cả Studios (BaseItemDto)
        QueryResultBaseItemDto resultBaseItemDto = studiosServiceApi.getStudios(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        // Dùng helper để parse danh sách DTO sang danh sách Tag
        return parseDtoListToTagList(resultBaseItemDto != null ? resultBaseItemDto.getItems() : null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Tag> getPeopleSuggestions() throws ApiException {
        PersonsServiceApi personsServiceApi = new PersonsServiceApi(apiClient);
        // Gọi API lấy tất cả People (BaseItemDto)
        QueryResultBaseItemDto resultBaseItemDto = personsServiceApi.getPersons(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        // Dùng helper để parse danh sách DTO sang danh sách Tag
        return parseDtoListToTagList(resultBaseItemDto != null ? resultBaseItemDto.getItems() : null);
    }
}