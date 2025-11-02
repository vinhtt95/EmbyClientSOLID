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
import java.util.stream.Collectors;

/**
 * Triển khai (Implementation) của IStaticDataRepository.
 * Chịu trách nhiệm đọc dữ liệu tĩnh/gợi ý.
 * Logic được chuyển từ RequestEmby.java
 * và ItemRepository.java cũ.
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

    @Override
    public List<Tag> getAllUsedTags() throws ApiException {
        TagServiceApi tagServiceApi = new TagServiceApi(apiClient);
        // Logic từ RequestEmby.getListTagsItem
        QueryResultUserLibraryTagItem listTag = tagServiceApi.getTags(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        if (listTag != null && listTag.getItems() != null) {
            return listTag.getItems().stream()
                    .map(UserLibraryTagItem::getName)
                    .filter(name -> name != null && !name.isEmpty())
                    .distinct()
                    .map(rawName -> Tag.parse(rawName, null)) // Chuyển đổi sang Model mới
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public List<SuggestionItem> getGenreSuggestions() throws ApiException {
        GenresServiceApi genresServiceApi = new GenresServiceApi(apiClient);
        // Logic từ RequestEmby.getListGenres
        QueryResultBaseItemDto genreResult = genresServiceApi.getGenres(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        List<BaseItemDto> dtoList = (genreResult != null) ? genreResult.getItems() : null;
        return SuggestionItem.fromBaseItemDtoList(dtoList); // Dùng model mới
    }

    @Override
    public List<SuggestionItem> getStudioSuggestions() throws ApiException {
        StudiosServiceApi studiosServiceApi = new StudiosServiceApi(apiClient);
        // Logic từ RequestEmby.getListStudio
        QueryResultBaseItemDto resultBaseItemDto = studiosServiceApi.getStudios(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        List<BaseItemDto> dtoList = (resultBaseItemDto != null) ? resultBaseItemDto.getItems() : null;
        return SuggestionItem.fromBaseItemDtoList(dtoList); // Dùng model mới
    }

    @Override
    public List<SuggestionItem> getPeopleSuggestions() throws ApiException {
        PersonsServiceApi personsServiceApi = new PersonsServiceApi(apiClient);
        // Logic từ RequestEmby.getListPeoples
        QueryResultBaseItemDto resultBaseItemDto = personsServiceApi.getPersons(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        List<BaseItemDto> dtoList = (resultBaseItemDto != null) ? resultBaseItemDto.getItems() : null;
        return SuggestionItem.fromBaseItemDtoList(dtoList); // Dùng model mới
    }
}