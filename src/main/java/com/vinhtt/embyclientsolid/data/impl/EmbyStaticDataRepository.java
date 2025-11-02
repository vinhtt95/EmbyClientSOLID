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
 * (Cập nhật: Sửa logic để parse Name (có thể là JSON) cho TẤT CẢ các loại).
 */
public class EmbyStaticDataRepository implements IStaticDataRepository {

    private final ApiClient apiClient;

    /**
     * Khởi tạo Repository với Session Service.
     * @param sessionService Service đã được tiêm (DI).
     */
    public EmbyStaticDataRepository(IEmbySessionService sessionService) {
        this.apiClient = sessionService.getApiClient();
    }

    /**
     * Helper chung để parse DTOs sang List<Tag>.
     * Đây là logic cốt lõi bạn yêu cầu.
     */
    private List<Tag> parseDtoListToTagList(List<BaseItemDto> dtoList) {
        if (dtoList == null) return Collections.emptyList();

        return dtoList.stream()
                // (SỬA LỖI: Lấy cả ID gốc từ DTO)
                .map(dto -> Tag.parse(dto.getName(), dto.getId())) // Parse Name (có thể là JSON) và giữ ID
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<Tag> getAllUsedTags() throws ApiException {
        TagServiceApi tagServiceApi = new TagServiceApi(apiClient);
        QueryResultUserLibraryTagItem listTag = tagServiceApi.getTags(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        if (listTag != null && listTag.getItems() != null) {
            return listTag.getItems().stream()
                    .map(UserLibraryTagItem::getName) // Lấy Name
                    .filter(name -> name != null && !name.isEmpty())
                    .distinct()
                    .map(rawName -> Tag.parse(rawName, null)) // Parse Name (có thể là JSON)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public List<Tag> getGenreSuggestions() throws ApiException {
        GenresServiceApi genresServiceApi = new GenresServiceApi(apiClient);
        QueryResultBaseItemDto genreResult = genresServiceApi.getGenres(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        return parseDtoListToTagList(genreResult != null ? genreResult.getItems() : null);
    }

    @Override
    public List<Tag> getStudioSuggestions() throws ApiException {
        StudiosServiceApi studiosServiceApi = new StudiosServiceApi(apiClient);
        QueryResultBaseItemDto resultBaseItemDto = studiosServiceApi.getStudios(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        return parseDtoListToTagList(resultBaseItemDto != null ? resultBaseItemDto.getItems() : null);
    }

    @Override
    public List<Tag> getPeopleSuggestions() throws ApiException {
        PersonsServiceApi personsServiceApi = new PersonsServiceApi(apiClient);
        QueryResultBaseItemDto resultBaseItemDto = personsServiceApi.getPersons(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        return parseDtoListToTagList(resultBaseItemDto != null ? resultBaseItemDto.getItems() : null);
    }
}