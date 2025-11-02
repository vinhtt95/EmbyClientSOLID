package com.vinhtt.embyclientsolid.data.impl;

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
 * Phụ thuộc vào IEmbySessionService để lấy ApiClient.
 * <p>
 * (ĐÃ SỬA LỖI LOGIC: Tách biệt logic getItemsByParentId (cho Tree)
 * khỏi getItemsPaginated (cho Grid) theo đúng yêu cầu.)
 */
public class EmbyItemRepository implements IItemRepository {

    private final IEmbySessionService sessionService;
    private final ApiClient apiClient;

    /**
     * Khởi tạo Repository với Session Service.
     *
     * @param sessionService Service đã được tiêm (DI).
     */
    public EmbyItemRepository(IEmbySessionService sessionService) {
        this.sessionService = sessionService;
        this.apiClient = sessionService.getApiClient();
    }

    // --- Helpers để lấy API services ---
    private ItemsServiceApi getItemsService() {
        return new ItemsServiceApi(apiClient);
    }

    private UserLibraryServiceApi getUserLibraryServiceApi() {
        return new UserLibraryServiceApi(apiClient);
    }

    private ImageServiceApi getImageServiceApi() {
        return new ImageServiceApi(apiClient);
    }

    @Override
    public List<BaseItemDto> getRootViews() throws ApiException {
        String userId = sessionService.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("Chưa đăng nhập. Không thể lấy root views.");
        }

        // Logic này đúng, lấy các thư mục gốc của User
        QueryResultBaseItemDto result = getItemsService().getUsersByUseridItems(userId, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        if (result != null && result.getItems() != null) {
            return result.getItems();
        }
        return Collections.emptyList();
    }

    /**
     * (SỬA LỖI LOGIC TẠI ĐÂY)
     * Lấy các items con dựa trên parentId (dùng cho Cột 1 - Tree).
     * Hàm này KHÔNG đệ quy (recursive=null) và lấy TẤT CẢ các loại item
     * (includeItemTypes=null) để ViewModel có thể lọc ra các thư mục.
     * (UR-17).
     * <p>
     * Logic này dựa trên RequestEmby.getQueryResultBaseItemDto từ dự án cũ.
     */
    @Override
    public List<BaseItemDto> getItemsByParentId(String parentId) throws ApiException {
        // Logic từ RequestEmby.getQueryResultBaseItemDto
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
     * Lấy các items con (dùng cho Cột 2 - Grid).
     * Hàm này CÓ đệ quy (recursive=true) và chỉ lấy "Movie"
     * (includeItemTypes="Movie").
     */
    @Override
    public QueryResultBaseItemDto getItemsPaginated(String parentId, int startIndex, int limit, String sortOrder, String sortBy) throws ApiException {
        // Logic từ RequestEmby.getQueryResultFullBaseItemDto
        QueryResultBaseItemDto result = getItemsService().getItems(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, startIndex, limit, true, null, sortOrder, parentId, "OfficialRating,CriticRating", null, "Movie", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, sortBy, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        if (result != null) {
            return result;
        }
        // Trả về kết quả rỗng an toàn
        QueryResultBaseItemDto emptyResult = new QueryResultBaseItemDto();
        emptyResult.setItems(Collections.emptyList());
        emptyResult.setTotalRecordCount(0);
        return emptyResult;
    }

    @Override
    public QueryResultBaseItemDto searchItemsPaginated(String keywords, int startIndex, int limit, String sortOrder, String sortBy) throws ApiException {
        // Logic từ RequestEmby.searchBaseItemDto
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

    @Override
    public QueryResultBaseItemDto getItemsByChip(Tag chip, String chipType, Integer startIndex, Integer limit, boolean recursive, String sortOrder, String sortBy) throws ApiException {
        String apiParam;
        QueryResultBaseItemDto listItems = null;

        switch (chipType) {
            case "TAG":
                String tagsName;
                tagsName = chip.serialize(); // Tag dùng tên đã serialize (JSON hoặc text)
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
                    if (listItems.getItems().isEmpty()) {
                        System.out.println("Empty Item Tags");
                    }

                    if (!listItems.getItems().isEmpty()) {

                        return listItems;
                    }

                } catch (ApiException e) {
                    System.out.println("Error fetching tags: " + e.getMessage());
                }
                break;

            case "STUDIO":
                String studioId;
                studioId = chip.getId(); // Studio dùng TÊN (hoặc JSON string)
                if (studioId == null) throw new ApiException("Studio Name is null for: " + chip.getDisplayName());
                System.out.println("studioId: " + studioId);
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

                    if (listItems.getItems().isEmpty()) {
                        System.out.println("Empty Studios");
                    }

                    if (!listItems.getItems().isEmpty()) {

                        return listItems;
                    }

                } catch (ApiException e) {
                    System.out.println("Error fetching studios: " + e.getMessage());
                }
                break;
            case "PEOPLE":
                apiParam = chip.getId(); // People dùng ID
                if (apiParam == null) throw new ApiException("People ID is null for: ".concat(chip.getDisplayName()));
                String peopleID = chip.getId(); // Genre dùng tên hiển thị
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

                    if (listItems.getItems().isEmpty()) {
                        System.out.println("Empty People");
                    }

                    if (!listItems.getItems().isEmpty()) {

                        return listItems;
                    }
                } catch (ApiException e) {
                    System.out.println("Error fetching people: " + e.getMessage());
                }
                break;

            case "GENRE":
                String nameGenres = chip.serialize(); // Genre dùng tên hiển thị

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
                    if (listItems.getItems().isEmpty()) {
                        System.out.println("Empty Genres");
                    }

                    if (!listItems.getItems().isEmpty()) {

                        return listItems;
                    }
                } catch (ApiException e) {
                    System.out.println("Error fetching Genres: " + e.getMessage());
                }
                break;
        }

        QueryResultBaseItemDto emptyResult = new QueryResultBaseItemDto();
        emptyResult.setItems(Collections.emptyList());
        emptyResult.setTotalRecordCount(0);
        return emptyResult;

    }


    @Override
    public BaseItemDto getFullItemDetails(String itemId) throws ApiException {
        String userId = sessionService.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("Chưa đăng nhập. Không thể lấy chi tiết item.");
        }
        // Logic từ ItemRepository.getFullItemDetails
        return getUserLibraryServiceApi().getUsersByUseridItemsById(userId, itemId);
    }

    @Override
    public List<ImageInfo> getItemImages(String itemId) throws ApiException {
        // Logic từ ItemRepository.getItemImages
        List<ImageInfo> images = getImageServiceApi().getItemsByIdImages(itemId);
        if (images != null) {
            return images;
        }
        return Collections.emptyList();
    }
}