package com.vinhtt.embyclientsolid.data.impl;

import com.vinhtt.embyclientsolid.core.IConfigurationService;
import com.vinhtt.embyclientsolid.core.IEmbySessionService;
import com.vinhtt.embyclientsolid.data.IItemRepository;
import com.vinhtt.embyclientsolid.model.Tag;
import embyclient.ApiClient;
import embyclient.ApiException;
import embyclient.api.ImageServiceApi;
import embyclient.api.ItemsServiceApi;
import embyclient.api.UserLibraryServiceApi;
import embyclient.model.BaseItemDto;
import embyclient.model.ImageInfo;
import embyclient.model.QueryResultBaseItemDto;

import java.util.Collections;
import java.util.List;

/**
 * Triển khai (Implementation) của IItemRepository.
 * Lớp này chịu trách nhiệm thực thi các lệnh ĐỌC (Query) dữ liệu Item từ Emby API,
 * sử dụng ApiClient được cung cấp bởi IEmbySessionService.
 */
public class EmbyItemRepository implements IItemRepository {

    private final IEmbySessionService sessionService;
    private final ApiClient apiClient;
    private final IConfigurationService configService;

    /**
     * Khởi tạo Repository.
     *
     * @param sessionService Service Session (DI) để lấy ApiClient và UserId.
     * @param configService Service Config (DI) để lấy chuỗi (ví dụ: lỗi).
     */
    public EmbyItemRepository(IEmbySessionService sessionService, IConfigurationService configService) {
        this.sessionService = sessionService;
        this.apiClient = sessionService.getApiClient();
        this.configService = configService;
    }

    // --- Helpers để lấy các API service cụ thể từ ApiClient ---
    private ItemsServiceApi getItemsService() {
        return new ItemsServiceApi(apiClient);
    }

    private UserLibraryServiceApi getUserLibraryServiceApi() {
        return new UserLibraryServiceApi(apiClient);
    }

    private ImageServiceApi getImageServiceApi() {
        return new ImageServiceApi(apiClient);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<BaseItemDto> getRootViews() throws ApiException {
        String userId = sessionService.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException(configService.getString("exceptions", "notLoggedIn"));
        }

        // Gọi API lấy các thư mục gốc (Views) của User
        QueryResultBaseItemDto result = getItemsService().getUsersByUseridItems(userId, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        if (result != null && result.getItems() != null) {
            return result.getItems();
        }
        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<BaseItemDto> getItemsByParentId(String parentId) throws ApiException {
        // Gọi API lấy item con, không đệ quy (recursive=null/false)
        // và sắp xếp theo SortName (dùng cho Cột 1 - Tree).
        QueryResultBaseItemDto result = getItemsService().getItems(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null,
                null,
                "Ascending",
                parentId,
                null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                "SortName",
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        if (result != null && result.getItems() != null) {
            return result.getItems();
        }
        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public QueryResultBaseItemDto getItemsPaginated(String parentId, int startIndex, int limit, String sortOrder, String sortBy) throws ApiException {
        // Gọi API lấy item con, CÓ đệ quy (recursive=true)
        // và chỉ lấy loại "Movie" (dùng cho Cột 2 - Grid).
        QueryResultBaseItemDto result = getItemsService().getItems(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, startIndex, limit, true, null, sortOrder, parentId, "OfficialRating,CriticRating", null, "Movie", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, sortBy, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        if (result != null) {
            return result;
        }
        // Trả về kết quả rỗng an toàn nếu API trả về null
        QueryResultBaseItemDto emptyResult = new QueryResultBaseItemDto();
        emptyResult.setItems(Collections.emptyList());
        emptyResult.setTotalRecordCount(0);
        return emptyResult;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public QueryResultBaseItemDto searchItemsPaginated(String keywords, int startIndex, int limit, String sortOrder, String sortBy) throws ApiException {
        // Gọi API tìm kiếm
        QueryResultBaseItemDto result = getItemsService().getItems(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                startIndex, // startIndex
                limit, // limit
                true, // recursive: true
                keywords, // searchTerm
                sortOrder, // sortOrder
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                sortBy, // sortBy
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        if (result != null) {
            return result;
        }
        QueryResultBaseItemDto emptyResult = new QueryResultBaseItemDto();
        emptyResult.setItems(Collections.emptyList());
        emptyResult.setTotalRecordCount(0);
        return emptyResult;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public QueryResultBaseItemDto getItemsByChip(Tag chip, String chipType, Integer startIndex, Integer limit, boolean recursive, String sortOrder, String sortBy) throws ApiException {
        String apiParam;
        QueryResultBaseItemDto listItems = null;

        // Tùy thuộc vào loại chip, chúng ta truyền tham số API khác nhau
        switch (chipType) {
            case "TAG":
                // Tag dùng 'tags' (truyền chuỗi đã serialize)
                String tagsName = chip.serialize();
                try {
                    listItems = getItemsService().getItems(
                            null,    //artistType
                            null,    //maxOfficialRating
                            null,    //hasThemeSong
                            null,    //hasThemeVideo
                            null,    //hasSubtitles
                            null,    //hasSpecialFeature
                            null,    //hasTrailer
                            null,    //isSpecialSeason
                            null,    //adjacentTo
                            null,    //startItemId
                            null,    //minIndexNumber
                            null,    //minStartDate
                            null,    //maxStartDate
                            null,    //minEndDate
                            null,    //maxEndDate
                            null,    //minPlayers
                            null,    //maxPlayers
                            null,    //parentIndexNumber
                            null,    //hasParentalRating
                            null,    //isHD
                            null,    //isUnaired
                            null,    //minCommunityRating
                            null,    //minCriticRating
                            null,    //airedDuringSeason
                            null,    //minPremiereDate
                            null,    //minDateLastSaved
                            null,    //minDateLastSavedForUser
                            null,    //maxPremiereDate
                            null,    //hasOverview
                            null,    //hasImdbId
                            null,    //hasTmdbId
                            null,    //hasTvdbId
                            null,    //excludeItemIds
                            startIndex,    //startIndex
                            limit,    //limit
                            recursive,    //recursive
                            null,    //searchTerm
                            sortOrder,    //sortOrder
                            null,    //parentId
                            null,    //fields
                            null,    //excludeItemTypes
                            "Movie,Series,Video,Game",    //includeItemTypes
                            null,    //anyProviderIdEquals
                            null,    //filters
                            null,    //isFavorite
                            null,    //isMovie
                            null,    //isSeries
                            null,    //isFolder
                            null,    //isNews
                            null,    //isKids
                            null,    //isSports
                            null,    //isNew
                            null,    //isPremiere
                            null,    //isNewOrPremiere
                            null,    //isRepeat
                            null,    //projectToMedia
                            null,    //mediaTypes
                            null,    //imageTypes
                            sortBy,    //sortBy
                            null,    //isPlayed
                            null,    //genres
                            null,    //officialRatings
                            tagsName,    //tags
                            null,    //excludeTags
                            null,    //years
                            null,    //enableImages
                            null,    //enableUserData
                            null,    //imageTypeLimit
                            null,    //enableImageTypes
                            null,    //person
                            null,    //personIds
                            null,    //personTypes
                            null,    //studios
                            null,    //studioIds
                            null,    //artists
                            null,    //artistIds
                            null,    //albums
                            null,    //ids
                            null,    //videoTypes
                            null,    //containers
                            null,    //audioCodecs
                            null,    //audioLayouts
                            null,    //videoCodecs
                            null,    //extendedVideoTypes
                            null,    //subtitleCodecs
                            null,    //path
                            null,    //userId
                            null,    //minOfficialRating
                            null,    //isLocked
                            null,    //isPlaceHolder
                            null,    //hasOfficialRating
                            null,    //groupItemsIntoCollections
                            null,    //is3D
                            null,    //seriesStatus
                            null,    //nameStartsWithOrGreater
                            null,    //artistStartsWithOrGreater
                            null,    //albumArtistStartsWithOrGreater
                            null,    //nameStartsWith
                            null    //nameLessThan
                    );
                } catch (ApiException e) {
                    System.err.println("Error fetching tags: " + e.getMessage());
                }
                break;

            case "STUDIO":
                // Studio dùng 'studioIds' (truyền ID)
                String studioId = chip.getId();
                if (studioId == null) throw new ApiException("Studio ID is null for: " + chip.getDisplayName());
                try {
                    listItems = getItemsService().getItems(
                            null,    //artistType
                            null,    //maxOfficialRating
                            null,    //hasThemeSong
                            null,    //hasThemeVideo
                            null,    //hasSubtitles
                            null,    //hasSpecialFeature
                            null,    //hasTrailer
                            null,    //isSpecialSeason
                            null,    //adjacentTo
                            null,    //startItemId
                            null,    //minIndexNumber
                            null,    //minStartDate
                            null,    //maxStartDate
                            null,    //minEndDate
                            null,    //maxEndDate
                            null,    //minPlayers
                            null,    //maxPlayers
                            null,    //parentIndexNumber
                            null,    //hasParentalRating
                            null,    //isHD
                            null,    //isUnaired
                            null,    //minCommunityRating
                            null,    //minCriticRating
                            null,    //airedDuringSeason
                            null,    //minPremiereDate
                            null,    //minDateLastSaved
                            null,    //minDateLastSavedForUser
                            null,    //maxPremiereDate
                            null,    //hasOverview
                            null,    //hasImdbId
                            null,    //hasTmdbId
                            null,    //hasTvdbId
                            null,    //excludeItemIds
                            startIndex,    //startIndex
                            limit,    //limit
                            recursive,    //recursive
                            null,    //searchTerm
                            sortOrder,    //sortOrder
                            null,    //parentId
                            null,    //fields
                            null,    //excludeItemTypes
                            "Movie,Series,Video,Game",    //includeItemTypes
                            null,    //anyProviderIdEquals
                            null,    //filters
                            null,    //isFavorite
                            null,    //isMovie
                            null,    //isSeries
                            null,    //isFolder
                            null,    //isNews
                            null,    //isKids
                            null,    //isSports
                            null,    //isNew
                            null,    //isPremiere
                            null,    //isNewOrPremiere
                            null,    //isRepeat
                            null,    //projectToMedia
                            null,    //mediaTypes
                            null,    //imageTypes
                            sortBy,    //sortBy
                            null,    //isPlayed
                            null,    //genres
                            null,    //officialRatings
                            null,    //tags
                            null,    //excludeTags
                            null,    //years
                            null,    //enableImages
                            null,    //enableUserData
                            null,    //imageTypeLimit
                            null,    //enableImageTypes
                            null,    //person
                            null,    //personIds
                            null,    //personTypes
                            null,    //studios
                            studioId,    //studioIds
                            null,    //artists
                            null,    //artistIds
                            null,    //albums
                            null,    //ids
                            null,    //videoTypes
                            null,    //containers
                            null,    //audioCodecs
                            null,    //audioLayouts
                            null,    //videoCodecs
                            null,    //extendedVideoTypes
                            null,    //subtitleCodecs
                            null,    //path
                            null,    //userId
                            null,    //minOfficialRating
                            null,    //isLocked
                            null,    //isPlaceHolder
                            null,    //hasOfficialRating
                            null,    //groupItemsIntoCollections
                            null,    //is3D
                            null,    //seriesStatus
                            null,    //nameStartsWithOrGreater
                            null,    //artistStartsWithOrGreater
                            null,    //albumArtistStartsWithOrGreater
                            null,    //nameStartsWith
                            null    //nameLessThan
                    );
                } catch (ApiException e) {
                    System.err.println("Error fetching studios: " + e.getMessage());
                }
                break;

            case "PEOPLE":
                // People dùng 'personIds' (truyền ID)
                apiParam = chip.getId();
                if (apiParam == null) throw new ApiException("People ID is null for: ".concat(chip.getDisplayName()));
                String peopleID = chip.getId();
                try {
                    listItems = getItemsService().getItems(
                            null,    //artistType
                            null,    //maxOfficialRating
                            null,    //hasThemeSong
                            null,    //hasThemeVideo
                            null,    //hasSubtitles
                            null,    //hasSpecialFeature
                            null,    //hasTrailer
                            null,    //isSpecialSeason
                            null,    //adjacentTo
                            null,    //startItemId
                            null,    //minIndexNumber
                            null,    //minStartDate
                            null,    //maxStartDate
                            null,    //minEndDate
                            null,    //maxEndDate
                            null,    //minPlayers
                            null,    //maxPlayers
                            null,    //parentIndexNumber
                            null,    //hasParentalRating
                            null,    //isHD
                            null,    //isUnaired
                            null,    //minCommunityRating
                            null,    //minCriticRating
                            null,    //airedDuringSeason
                            null,    //minPremiereDate
                            null,    //minDateLastSaved
                            null,    //minDateLastSavedForUser
                            null,    //maxPremiereDate
                            null,    //hasOverview
                            null,    //hasImdbId
                            null,    //hasTmdbId
                            null,    //hasTvdbId
                            null,    //excludeItemIds
                            startIndex,    //startIndex
                            limit,    //limit
                            recursive,    //recursive
                            null,    //searchTerm
                            sortOrder,    //sortOrder
                            null,    //parentId
                            null,    //fields
                            null,    //excludeItemTypes
                            "Movie,Series,Video,Game",    //includeItemTypes
                            null,    //anyProviderIdEquals
                            null,    //filters
                            null,    //isFavorite
                            null,    //isMovie
                            null,    //isSeries
                            null,    //isFolder
                            null,    //isNews
                            null,    //isKids
                            null,    //isSports
                            null,    //isNew
                            null,    //isPremiere
                            null,    //isNewOrPremiere
                            null,    //isRepeat
                            null,    //projectToMedia
                            null,    //mediaTypes
                            null,    //imageTypes
                            sortBy,    //sortBy
                            null,    //isPlayed
                            null,    //genres
                            null,    //officialRatings
                            null,    //tags
                            null,    //excludeTags
                            null,    //years
                            null,    //enableImages
                            null,    //enableUserData
                            null,    //imageTypeLimit
                            null,    //enableImageTypes
                            null,    //person
                            peopleID,    //personIds
                            null,    //personTypes
                            null,    //studios
                            null,    //studioIds
                            null,    //artists
                            null,    //artistIds
                            null,    //albums
                            null,    //ids
                            null,    //videoTypes
                            null,    //containers
                            null,    //audioCodecs
                            null,    //audioLayouts
                            null,    //videoCodecs
                            null,    //extendedVideoTypes
                            null,    //subtitleCodecs
                            null,    //path
                            null,    //userId
                            null,    //minOfficialRating
                            null,    //isLocked
                            null,    //isPlaceHolder
                            null,    //hasOfficialRating
                            null,    //groupItemsIntoCollections
                            null,    //is3D
                            null,    //seriesStatus
                            null,    //nameStartsWithOrGreater
                            null,    //artistStartsWithOrGreater
                            null,    //albumArtistStartsWithOrGreater
                            null,    //nameStartsWith
                            null    //nameLessThan
                    );
                } catch (ApiException e) {
                    System.err.println("Error fetching people: " + e.getMessage());
                }
                break;

            case "GENRE":
                // Genre dùng 'genres' (truyền chuỗi đã serialize)
                String nameGenres = chip.serialize();
                try {
                    listItems = getItemsService().getItems(
                            null,    //artistType
                            null,    //maxOfficialRating
                            null,    //hasThemeSong
                            null,    //hasThemeVideo
                            null,    //hasSubtitles
                            null,    //hasSpecialFeature
                            null,    //hasTrailer
                            null,    //isSpecialSeason
                            null,    //adjacentTo
                            null,    //startItemId
                            null,    //minIndexNumber
                            null,    //minStartDate
                            null,    //maxStartDate
                            null,    //minEndDate
                            null,    //maxEndDate
                            null,    //minPlayers
                            null,    //maxPlayers
                            null,    //parentIndexNumber
                            null,    //hasParentalRating
                            null,    //isHD
                            null,    //isUnaired
                            null,    //minCommunityRating
                            null,    //minCriticRating
                            null,    //airedDuringSeason
                            null,    //minPremiereDate
                            null,    //minDateLastSaved
                            null,    //minDateLastSavedForUser
                            null,    //maxPremiereDate
                            null,    //hasOverview
                            null,    //hasImdbId
                            null,    //hasTmdbId
                            null,    //hasTvdbId
                            null,    //excludeItemIds
                            startIndex,    //startIndex
                            limit,    //limit
                            recursive,    //recursive
                            null,    //searchTerm
                            sortOrder,    //sortOrder
                            null,    //parentId
                            null,    //fields
                            null,    //excludeItemTypes
                            "Movie, Series, Video, Game, MusicAlbum",    //includeItemTypes
                            null,    //anyProviderIdEquals
                            null,    //filters
                            null,    //isFavorite
                            null,    //isMovie
                            null,    //isSeries
                            null,    //isFolder
                            null,    //isNews
                            null,    //isKids
                            null,    //isSports
                            null,    //isNew
                            null,    //isPremiere
                            null,    //isNewOrPremiere
                            null,    //isRepeat
                            null,    //projectToMedia
                            null,    //mediaTypes
                            null,    //imageTypes
                            sortBy,    //sortBy
                            null,    //isPlayed
                            nameGenres,    //genres
                            null,    //officialRatings
                            null,    //tags
                            null,    //excludeTags
                            null,    //years
                            null,    //enableImages
                            null,    //enableUserData
                            null,    //imageTypeLimit
                            null,    //enableImageTypes
                            null,    //person
                            null,    //personIds
                            null,    //personTypes
                            null,    //studios
                            null,    //studioIds
                            null,    //artists
                            null,    //artistIds
                            null,    //albums
                            null,    //ids
                            null,    //videoTypes
                            null,    //containers
                            null,    //audioCodecs
                            null,    //audioLayouts
                            null,    //videoCodecs
                            null,    //extendedVideoTypes
                            null,    //subtitleCodecs
                            null,    //path
                            null,    //userId
                            null,    //minOfficialRating
                            null,    //isLocked
                            null,    //isPlaceHolder
                            null,    //hasOfficialRating
                            null,    //groupItemsIntoCollections
                            null,    //is3D
                            null,    //seriesStatus
                            null,    //nameStartsWithOrGreater
                            null,    //artistStartsWithOrGreater
                            null,    //albumArtistStartsWithOrGreater
                            null,    //nameStartsWith
                            null    //nameLessThan
                    );
                } catch (ApiException e) {
                    System.err.println("Error fetching Genres: " + e.getMessage());
                }
                break;
        }

        // Trả về kết quả (có thể là null) hoặc một kết quả rỗng
        if (listItems != null) {
            return listItems;
        }
        QueryResultBaseItemDto emptyResult = new QueryResultBaseItemDto();
        emptyResult.setItems(Collections.emptyList());
        emptyResult.setTotalRecordCount(0);
        return emptyResult;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BaseItemDto getFullItemDetails(String itemId) throws ApiException {
        String userId = sessionService.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException(configService.getString("exceptions", "notLoggedIn"));
        }
        // Gọi API lấy chi tiết item của user (để lấy được UserData)
        return getUserLibraryServiceApi().getUsersByUseridItemsById(userId, itemId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ImageInfo> getItemImages(String itemId) throws ApiException {
        // Gọi API lấy danh sách ảnh của item
        List<ImageInfo> images = getImageServiceApi().getItemsByIdImages(itemId);
        if (images != null) {
            return images;
        }
        return Collections.emptyList();
    }
}