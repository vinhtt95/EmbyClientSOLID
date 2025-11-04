package com.vinhtt.embyclientsolid.viewmodel.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vinhtt.embyclientsolid.core.*;
import com.vinhtt.embyclientsolid.data.*;
import com.vinhtt.embyclientsolid.model.LibraryTreeItem;
import com.vinhtt.embyclientsolid.model.ReleaseInfo;
import com.vinhtt.embyclientsolid.model.SuggestionContext;
import com.vinhtt.embyclientsolid.model.SuggestionItem;
import com.vinhtt.embyclientsolid.model.Tag;
import com.vinhtt.embyclientsolid.services.JsonFileHandler;
import com.vinhtt.embyclientsolid.viewmodel.AddTagResult;
import com.vinhtt.embyclientsolid.viewmodel.IItemDetailViewModel;
import com.vinhtt.embyclientsolid.viewmodel.ILibraryTreeViewModel;
import embyclient.JSON;
import embyclient.model.*;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.collections.ListChangeListener;
import javafx.stage.Stage;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.io.File;

/**
 * Triển khai của {@link IItemDetailViewModel}.
 * Đây là ViewModel trung tâm cho Cột 3 (Chi tiết Item). Nó quản lý trạng thái
 * của tất cả các trường metadata (Title, Overview, Tags, v.v.),
 * và điều phối các hành động (Commands) như Tải, Lưu, Import, Export,
 * và các tương tác người dùng khác, bằng cách ủy thác logic nghiệp vụ
 * cho các Dịch vụ (Services) đã được tiêm (inject).
 */
public class ItemDetailViewModel implements IItemDetailViewModel {

    // --- Services (DI) ---
    private final IItemRepository itemRepository;
    private final IItemUpdateService itemUpdateService;
    private final IStaticDataRepository staticDataRepository;
    private final IExternalDataService externalDataService;
    private final ILocalInteractionService localInteractionService;
    private final INotificationService notificationService;
    private final IEmbySessionService sessionService;
    private final ILibraryTreeViewModel libraryTreeViewModel;
    private final IConfigurationService configService;

    /**
     * Helper theo dõi trạng thái "dirty" (thay đổi chưa lưu) của các trường,
     * hỗ trợ logic bật/tắt nút Save (UR-48).
     */
    private final ItemDetailDirtyTracker dirtyTracker;

    /**
     * Helper xử lý logic phức tạp của việc Import và Preview JSON
     * (UR-45, UR-46, UR-47, UR-49).
     */
    private final ItemDetailImportHandler importHandler;

    /**
     * Helper xử lý việc hiển thị dialog Mở/Lưu file .json (UR-44, UR-45).
     */
    private final JsonFileHandler jsonFileHandler;
    private final Gson gson;
    private boolean playAfterLoad = false;

    // --- Trạng thái nội bộ ---
    /**
     * Lưu trữ bản gốc DTO được tải từ server. Dùng để so sánh thay đổi
     * và làm cơ sở cho việc lưu (save) và import.
     */
    private BaseItemDto originalItemDto;
    private String currentItemId;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    /**
     * Lưu lại bối cảnh (Context) của lần mở dialog "Add Chip" cuối cùng,
     * dùng cho hotkey "Lặp lại Add Tag" (UR-13).
     */
    private SuggestionContext lastAddContext = SuggestionContext.TAG;


    // --- Properties (Trạng thái UI) ---
    private final ReadOnlyBooleanWrapper loading = new ReadOnlyBooleanWrapper(false);
    private ReadOnlyStringWrapper statusMessage = new ReadOnlyStringWrapper("Vui lòng chọn một item...");
    private final ReadOnlyBooleanWrapper showStatusMessage = new ReadOnlyBooleanWrapper(true);

    private final StringProperty title = new SimpleStringProperty("");
    private final StringProperty originalTitle = new SimpleStringProperty("");
    private final ObjectProperty<Float> criticRating = new SimpleObjectProperty<>(null);
    private final StringProperty overview = new SimpleStringProperty("");
    private final StringProperty releaseDate = new SimpleStringProperty("");
    private final ReadOnlyStringWrapper itemPath = new ReadOnlyStringWrapper("");
    private final ReadOnlyBooleanWrapper isFolder = new ReadOnlyBooleanWrapper(false);

    private final ReadOnlyObjectWrapper<Image> primaryImage = new ReadOnlyObjectWrapper<>(null);
    private final ObservableList<ImageInfo> backdropImages = FXCollections.observableArrayList();
    private final ObservableList<Tag> tagItems = FXCollections.observableArrayList();
    private final ObservableList<Tag> studioItems = FXCollections.observableArrayList();
    private final ObservableList<Tag> peopleItems = FXCollections.observableArrayList();
    private final ObservableList<Tag> genreItems = FXCollections.observableArrayList();

    /**
     * Lưu trữ file ảnh mới (local) khi người dùng kéo-thả vào ảnh Primary.
     * Sẽ bị xóa (về null) sau khi lưu thành công.
     */
    private final ObjectProperty<File> newPrimaryImageFile = new SimpleObjectProperty<>(null);
    /**
     * Cờ (flag) báo cho UI biết có ảnh Primary mới đang chờ lưu (UR-42).
     */
    private final ReadOnlyBooleanWrapper primaryImageDirty = new ReadOnlyBooleanWrapper(false);

    // --- Properties Sự kiện (Event) ---
    /**
     * Bắn (fire) sự kiện khi người dùng click vào một chip (UR-36).
     * MainController sẽ lắng nghe và kích hoạt Cột 2 (Grid).
     */
    private final ReadOnlyObjectWrapper<ChipClickEvent> chipClickEvent = new ReadOnlyObjectWrapper<>(null);
    /**
     * Bắn (fire) sự kiện khi người dùng nhấn nút Add (+) (UR-35).
     * MainController sẽ lắng nghe và mở AddTagDialog.
     */
    private final ReadOnlyObjectWrapper<SuggestionContext> addChipCommand = new ReadOnlyObjectWrapper<>(null);
    /**
     * Bắn (fire) sự kiện khi người dùng nhấn Play (UR-50).
     * MainController sẽ lắng nghe và mở cửa sổ Pop-out.
     */
    private final ReadOnlyObjectWrapper<Boolean> popOutRequest = new ReadOnlyObjectWrapper<>(null);

    private final ReadOnlyBooleanWrapper playNextRequest = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper playPreviousRequest = new ReadOnlyBooleanWrapper(false);


    /**
     * Khởi tạo ItemDetailViewModel và tiêm (inject) tất cả các dịch vụ (DI).
     *
     * @param itemRepository       Repo Đọc Item.
     * @param itemUpdateService    Repo Ghi Item.
     * @param staticDataRepository Repo Đọc gợi ý.
     * @param externalDataService  Repo gọi API bên ngoài.
     * @param localInteractionService Dịch vụ tương tác file local.
     * @param notificationService  Dịch vụ thông báo.
     * @param sessionService       Dịch vụ session.
     * @param libraryTreeViewModel VM Cột 1 (để lấy thư mục cha khi Clone).
     * @param configService        Dịch vụ đọc config.
     */
    public ItemDetailViewModel(IItemRepository itemRepository, IItemUpdateService itemUpdateService,
                               IStaticDataRepository staticDataRepository, IExternalDataService externalDataService,
                               ILocalInteractionService localInteractionService, INotificationService notificationService,
                               IEmbySessionService sessionService, ILibraryTreeViewModel libraryTreeViewModel,
                               IConfigurationService configService) {
        this.itemRepository = itemRepository;
        this.itemUpdateService = itemUpdateService;
        this.staticDataRepository = staticDataRepository;
        this.externalDataService = externalDataService;
        this.localInteractionService = localInteractionService;
        this.notificationService = notificationService;
        this.sessionService = sessionService;
        this.libraryTreeViewModel = libraryTreeViewModel;
        this.configService = configService;

        // Khởi tạo các helper
        this.statusMessage = new ReadOnlyStringWrapper(configService.getString("itemDetailView", "statusDefault"));
        this.dirtyTracker = new ItemDetailDirtyTracker(this);
        this.importHandler = new ItemDetailImportHandler(this, this.dirtyTracker);
        this.jsonFileHandler = new JsonFileHandler(configService);

        // Cấu hình Gson để parse/serialize DTO khi import/export
        this.gson = new GsonBuilder()
                .registerTypeAdapter(OffsetDateTime.class, new JSON.OffsetDateTimeTypeAdapter())
                .create();

        // Lắng nghe thay đổi của newPrimaryImageFile để cập nhật cờ primaryImageDirty (UR-42)
        newPrimaryImageFile.addListener((obs, oldVal, newVal) -> primaryImageDirty.set(newVal != null));

        // Lắng nghe khi loading xong
        loading.addListener((obs, wasLoading, isLoading) -> {
            if (wasLoading && !isLoading) {
                // Nếu cờ playAfterLoad được đặt, và đã tải xong
                if (playAfterLoad) {
                    playAfterLoad = false; // Xóa cờ
                    // Kiểm tra xem có phải là file không
                    if (Boolean.FALSE.equals(isFolder.get())) {
                        openFileOrFolderCommand(); // Kích hoạt phát VÀ pop-out
                    }
                }
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadItem(BaseItemDto item) {
        // 1. Đặt cờ: Đây là lệnh tải thông thường (KHÔNG phát)
        this.playAfterLoad = false;

        // 2. Gọi hàm tải nội bộ
        internalLoadItem(item);
    }

    @Override
    public void loadItemAndPlay(BaseItemDto item) {
        // 1. Đặt cờ: Đây là lệnh tải VÀ PHÁT
        this.playAfterLoad = true;

        // 2. Gọi hàm tải nội bộ
        internalLoadItem(item);
    }

    /**
     * Hàm private chứa logic tải item, được gọi bởi cả loadItem và loadItemAndPlay.
     * (Đây là nội dung của hàm loadItem() cũ của bạn,
     * chỉ cần XÓA dòng "this.playAfterLoad = false;" ở đầu hàm)
     */
    private void internalLoadItem(BaseItemDto item) {
        // XÓA DÒNG "this.playAfterLoad = false;" NẾU NÓ CÒN Ở ĐÂY

        if (item == null) {
            Platform.runLater(this::clearAllDetailsUI);
            return;
        }
        final String newItemId = item.getId();

        Platform.runLater(() -> {
            statusMessage.set(configService.getString("itemDetailViewModel", "statusLoading", item.getName()));
            showStatusMessage.set(true);
            loading.set(true);
        });

        new Thread(() -> {
            try {
                BaseItemDto loadedDto = itemRepository.getFullItemDetails(newItemId);
                List<ImageInfo> backdrops = itemRepository.getItemImages(newItemId).stream()
                        .filter(img -> ImageType.BACKDROP.equals(img.getImageType()))
                        .collect(Collectors.toList());

                Platform.runLater(() -> {
                    // clearAllDetailsUI() sẽ tự động reset cờ playAfterLoad về false,
                    // nhưng nó sẽ được gọi TRƯỚC KHI listener 'loading' kích hoạt,
                    // nên chúng ta cần đảm bảo cờ playAfterLoad được giữ nguyên nếu cần.

                    // --- SỬA ĐỔI QUAN TRỌNG ---
                    // Lưu lại cờ playAfterLoad TRƯỚC KHI gọi clearAllDetailsUI
                    boolean intendedToPlay = this.playAfterLoad;

                    clearAllDetailsUI(); // Hàm này sẽ đặt playAfterLoad = false

                    // Đặt lại cờ nếu nó được dự định
                    this.playAfterLoad = intendedToPlay;
                    // --- KẾT THÚC SỬA ĐỔI ---

                    this.originalItemDto = loadedDto;
                    this.currentItemId = newItemId;

                    title.set(loadedDto.getName() != null ? loadedDto.getName() : "");
                    String originalTitleFromDto = loadedDto.getOriginalTitle();
                    // ... (giữ nguyên phần còn lại của logic tải dữ liệu) ...
                    // ... (ví dụ: overview.set, releaseDate.set, ...)

                    loading.set(false); // Dòng này sẽ kích hoạt listener trong constructor
                    showStatusMessage.set(false);
                    dirtyTracker.startTracking();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    clearAllDetailsUI();
                    statusMessage.set(configService.getString("itemDetailViewModel", "errorLoad", e.getMessage()));
                    showStatusMessage.set(true);
                    loading.set(false);
                });
            }
        }).start();
    }

    /**
     * Reset tất cả trạng thái của Cột 3 về giá trị mặc định.
     */
    private void clearAllDetailsUI() {
        playAfterLoad = false;
        dirtyTracker.stopTracking(); // Dừng theo dõi thay đổi
        originalItemDto = null;
        currentItemId = null;
        title.set("");
        originalTitle.set("");
        criticRating.set(null);
        overview.set("");
        releaseDate.set("");
        itemPath.set("");
        isFolder.set(false);
        primaryImage.set(null);
        backdropImages.clear();
        tagItems.clear();
        studioItems.clear();
        peopleItems.clear();
        genreItems.clear();
        newPrimaryImageFile.set(null); // Xóa file ảnh chờ upload
        importHandler.clearState(); // Reset trạng thái import
        statusMessage.set(configService.getString("itemDetailViewModel", "statusDefault"));
        showStatusMessage.set(true);
    }

    /**
     * Helper: Chuyển đổi danh sách {@code List<NameLongIdPair>} (DTO)
     * thành danh sách {@code List<Tag>} (Model).
     */
    private List<Tag> parseNameLongIdPair(List<NameLongIdPair> list) {
        if (list == null) return new ArrayList<>();
        return list.stream()
                .map(pair -> Tag.parse(pair.getName(), pair.getId() != null ? pair.getId().toString() : null))
                .collect(Collectors.toList());
    }

    /**
     * Helper: Chuyển đổi danh sách {@code List<BaseItemPerson>} (DTO)
     * thành danh sách {@code List<Tag>} (Model).
     */
    private List<Tag> parseBaseItemPerson(List<BaseItemPerson> list) {
        if (list == null) return new ArrayList<>();
        return list.stream()
                .map(person -> Tag.parse(person.getName(), person.getId()))
                .collect(Collectors.toList());
    }

    /**
     * Helper: Chuyển đổi danh sách {@code List<String>} (DTO của Genres)
     * thành danh sách {@code List<Tag>} (Model).
     */
    private List<Tag> parseStringList(List<String> list) {
        if (list == null) return new ArrayList<>();
        return list.stream()
                .map(name -> Tag.parse(name, null)) // Genres không có ID riêng
                .collect(Collectors.toList());
    }

    /**
     * Helper: Xây dựng URL đầy đủ cho ảnh Primary (UR-42).
     */
    private Image getPrimaryImageUrl(BaseItemDto dto) {
        if (dto.getId() != null && dto.getImageTags() != null && dto.getImageTags().containsKey("Primary")) {
            String tag = dto.getImageTags().get("Primary");
            String serverUrl = sessionService.getApiClient().getBasePath();
            // Yêu cầu ảnh với chiều rộng 600px
            String url = String.format("%s/Items/%s/Images/Primary?tag=%s&maxWidth=%d&quality=90",
                    serverUrl, dto.getId(), tag, 600);
            return new Image(url, true); // true = tải nền
        }
        return null;
    }
    private String dateToString(OffsetDateTime date) {
        if (date == null) return "";
        try {
            return dateFormat.format(Date.from(date.toInstant()));
        } catch (Exception e) { return ""; }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getBackdropUrl(ImageInfo info) {
        if (info == null || info.getImageType() == null || currentItemId == null) {
            return null;
        }
        String serverUrl = sessionService.getApiClient().getBasePath();
        String imageType = info.getImageType().getValue();
        Integer index = info.getImageIndex();

        // Ảnh backdrop cần cả ID, Loại, và Index
        if (index != null) {
            return String.format("%s/Items/%s/Images/%s/%d?maxWidth=%d&quality=90",
                    serverUrl, currentItemId, imageType, index, 400); // Yêu cầu ảnh thumbnail rộng 400px
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveChangesCommand() {
        // Lấy tham chiếu đến DTO gốc tại thời điểm nhấn Lưu
        final BaseItemDto dtoAtSaveTime = this.originalItemDto;
        final String idAtSaveTime = this.currentItemId;

        if (dtoAtSaveTime == null || idAtSaveTime == null) {
            notificationService.showStatus(configService.getString("itemDetailViewModel", "errorSave"));
            return;
        }

        // Báo trạng thái
        notificationService.showStatus(configService.getString("itemDetailViewModel", "statusSaving"));
        loading.set(true);
        importHandler.hideAllReviewButtons(); // Ẩn các nút (✓/✗)

        // Kiểm tra xem đây là lưu sau khi Import hay lưu thủ công
        final boolean isSavingAfterImport = importHandler.wasImportInProgress();
        final Set<String> acceptedFields = isSavingAfterImport ? importHandler.getAcceptedFields() : null;
        final BaseItemDto importedDto = isSavingAfterImport ? importHandler.getImportedDto() : null;

        // Lấy giá trị *hiện tại* từ các JavaFX Properties
        // (Đây là các giá trị đã được edit hoặc chấp nhận (✓))
        final String finalTitle = this.title.get();
        final String finalOriginalTitle = this.originalTitle.get();
        final Float finalCriticRating = this.criticRating.get();
        final String finalOverview = this.overview.get();
        final String finalReleaseDate = this.releaseDate.get();
        final List<Tag> finalTagItems = List.copyOf(this.tagItems);
        final List<Tag> finalStudiosItems = List.copyOf(this.studioItems);
        final List<Tag> finalPeopleItems = List.copyOf(this.peopleItems);
        final List<Tag> finalGenresItems = List.copyOf(this.genreItems);

        // Chạy tác vụ lưu trên luồng nền
        new Thread(() -> {
            try {
                BaseItemDto dtoToSendToApi;

                if (isSavingAfterImport) {
                    // KỊCH BẢN 1: LƯU SAU KHI IMPORT (UR-49)
                    notificationService.showStatus(configService.getString("itemDetailViewModel", "statusSavingImport"));
                    // Chỉ tạo DTO với các trường đã được chấp nhận (✓)
                    dtoToSendToApi = createDtoWithAcceptedChanges(
                            dtoAtSaveTime, importedDto, acceptedFields
                    );
                } else {
                    // KỊCH BẢN 2: LƯU THỦ CÔNG (UR-31)
                    notificationService.showStatus(configService.getString("itemDetailViewModel", "statusSavingManual"));
                    // Tạo DTO mới dựa trên DTO gốc, nhưng cập nhật các trường từ UI
                    dtoToSendToApi = gson.fromJson(gson.toJson(dtoAtSaveTime), BaseItemDto.class);
                    dtoToSendToApi.setName(finalTitle);
                    dtoToSendToApi.setOriginalTitle(finalOriginalTitle);
                    dtoToSendToApi.setOverview(finalOverview);
                    dtoToSendToApi.setCriticRating(finalCriticRating);
                    dtoToSendToApi.setPremiereDate(parseDateString(finalReleaseDate, dtoAtSaveTime.getPremiereDate()));
                    // Chuyển đổi Model (Tag) trở lại DTO (NameLongIdPair/BaseItemPerson)
                    dtoToSendToApi.setTagItems(convertTagsToNameLongIdPair(finalTagItems));
                    dtoToSendToApi.setStudios(convertTagsToNameLongIdPair(finalStudiosItems));
                    dtoToSendToApi.setPeople(convertTagsToPeopleList(finalPeopleItems));
                    List<NameLongIdPair> genreItemsToSave = convertTagsToNameLongIdPair(finalGenresItems);
                    dtoToSendToApi.setGenreItems(genreItemsToSave);
                    dtoToSendToApi.setGenres(new ArrayList<>()); // Xóa trường Genres (List<String>) cũ
                }

                // Gọi service GHI
                itemUpdateService.updateItem(idAtSaveTime, dtoToSendToApi);

                // Cập nhật UI (trên luồng FX)
                Platform.runLater(() -> {
                    notificationService.showStatus(configService.getString("itemDetailViewModel", "statusSaveSuccess"));
                    loading.set(false);
                    // Tải lại item để lấy dữ liệu mới nhất từ server và reset trạng thái 'dirty'
                    loadItem(dtoToSendToApi);
                });
            } catch (Exception e) {
                // Xử lý lỗi
                Platform.runLater(() -> {
                    notificationService.showStatus(configService.getString("itemDetailViewModel", "errorApiSave", e.getClass().getSimpleName(), e.getMessage()));
                    loading.set(false);
                });
            }
        }).start();
    }

    /**
     * Helper: Xây dựng DTO để gửi đi khi lưu sau khi import (UR-49).
     * Chỉ áp dụng các thay đổi (từ {@code importedDto}) vào {@code originalDto}
     * nếu trường đó có trong {@code acceptedFields}.
     *
     * @param originalDto  DTO gốc trước khi import.
     * @param importedDto  DTO đọc từ file JSON.
     * @param acceptedFields Set chứa tên các trường đã nhấn (✓).
     * @return DTO đã merge.
     */
    private BaseItemDto createDtoWithAcceptedChanges(BaseItemDto originalDto, BaseItemDto importedDto, Set<String> acceptedFields) {
        // Tạo bản sao của DTO gốc
        BaseItemDto dtoCopy = gson.fromJson(gson.toJson(originalDto), BaseItemDto.class);

        // Áp dụng các thay đổi đã chấp nhận
        if (acceptedFields.contains("title")) { dtoCopy.setName(importedDto.getName()); }
        if (acceptedFields.contains("originalTitle")) { dtoCopy.setOriginalTitle(importedDto.getOriginalTitle()); }
        if (acceptedFields.contains("criticRating")) { dtoCopy.setCriticRating(importedDto.getCriticRating()); }
        if (acceptedFields.contains("overview")) { dtoCopy.setOverview(importedDto.getOverview()); }
        if (acceptedFields.contains("releaseDate")) { dtoCopy.setPremiereDate(importedDto.getPremiereDate()); }
        if (acceptedFields.contains("tags")) { dtoCopy.setTagItems(importedDto.getTagItems()); }
        if (acceptedFields.contains("studios")) { dtoCopy.setStudios(importedDto.getStudios()); }
        if (acceptedFields.contains("people")) { dtoCopy.setPeople(importedDto.getPeople()); }
        if (acceptedFields.contains("genres")) {
            dtoCopy.setGenres(importedDto.getGenres()); // Dùng trường Genres (List<String>)
            dtoCopy.setGenreItems(new ArrayList<>()); // Xóa trường GenreItems (List<NameLongIdPair>)
        }

        return dtoCopy;
    }

    /**
     * Helper: Chuyển đổi chuỗi "dd/MM/yyyy" (từ UI) sang {@link OffsetDateTime} (cho API).
     */
    private OffsetDateTime parseDateString(String dateStr, OffsetDateTime fallback) {
        try {
            Date parsedDate = dateFormat.parse(dateStr);
            Instant instant = Instant.ofEpochMilli(parsedDate.getTime());
            return OffsetDateTime.ofInstant(instant, ZoneId.systemDefault());
        } catch (ParseException e) {
            // Nếu parse lỗi, trả về giá trị gốc
            return fallback;
        }
    }

    /**
     * Helper: Chuyển đổi {@code List<Tag>} (Model) sang {@code List<NameLongIdPair>} (DTO).
     */
    private List<NameLongIdPair> convertTagsToNameLongIdPair(List<Tag> tags) {
        return tags.stream()
                .map(tag -> new NameLongIdPair().name(tag.serialize()).id(null)) // ID được bỏ qua, server tự xử lý
                .collect(Collectors.toList());
    }

    /**
     * Helper: Chuyển đổi {@code List<Tag>} (Model) sang {@code List<BaseItemPerson>} (DTO).
     */
    private List<BaseItemPerson> convertTagsToPeopleList(List<Tag> tags) {
        return tags.stream()
                .map(tag -> new BaseItemPerson().name(tag.serialize()).type(PersonType.ACTOR)) // Mặc định là Actor
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveCriticRatingImmediately(Float newRating) {
        if (originalItemDto == null || currentItemId == null) return;

        // Tạo DTO chỉ chứa thay đổi rating
        BaseItemDto ratingDto = gson.fromJson(gson.toJson(originalItemDto), BaseItemDto.class);
        ratingDto.setCriticRating(newRating);

        // Báo trạng thái
        loading.set(true);
        notificationService.showStatus(configService.getString("itemDetailViewModel", "statusSavingRating"));

        // Chạy trên luồng nền
        new Thread(() -> {
            try {
                // Gọi service GHI
                itemUpdateService.updateItem(currentItemId, ratingDto);

                // Cập nhật UI (trên luồng FX)
                Platform.runLater(() -> {
                    notificationService.showStatus(configService.getString("itemDetailViewModel", "statusSaveRatingSuccess"));
                    // Cập nhật DTO gốc trong bộ nhớ
                    if (originalItemDto != null) {
                        originalItemDto.setCriticRating(newRating);
                    }
                    // Báo cho DirtyTracker biết giá trị "gốc" mới của rating
                    // để tránh kích hoạt "dirty" cho các trường khác (UR-33)
                    dirtyTracker.updateOriginalRating(newRating);
                    loading.set(false);
                });
            } catch (Exception e) {
                // Xử lý lỗi (ví dụ: mất mạng)
                Platform.runLater(() -> {
                    notificationService.showStatus(configService.getString("itemDetailViewModel", "errorSaveRating", e.getMessage()));
                    // Hoàn tác thay đổi trên UI
                    criticRating.set(originalItemDto.getCriticRating());
                    loading.set(false);
                });
            }
        }).start();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fetchReleaseDateCommand() {
        String code = originalTitle.get();
        if (code == null || code.trim().isEmpty()) {
            notificationService.showStatus(configService.getString("itemDetailView", "originalTitlePrompt"));
            return;
        }

        // Báo trạng thái
        notificationService.showStatus(configService.getString("itemDetailView", "statusFetchingDate", code));
        loading.set(true);

        // Chạy trên luồng nền
        new Thread(() -> {
            try {
                // 1. Gọi API bên ngoài (UR-38)
                ReleaseInfo info = externalDataService.fetchReleaseInfoByCode(code);
                if (info == null) {
                    throw new Exception(configService.getString("itemDetailView", "statusFetchDateNotFound"));
                }

                List<Tag> tagsFromPerson = new ArrayList<>();
                Tag actorTagToAlwaysAdd = null; // Tag diễn viên (People) sẽ được thêm

                if (info.getActressName() != null && !info.getActressName().isEmpty()) {
                    String actorName = info.getActressName();

                    // 2. Tìm kiếm diễn viên trong kho (Repo) để lấy ID và Tags
                    List<Tag> peopleTags = staticDataRepository.getPeopleSuggestions();
                    Tag personTagResultFromRepo = peopleTags.stream()
                            .filter(p -> actorName.equalsIgnoreCase(p.getDisplayName()))
                            .findFirst().orElse(null);

                    if (personTagResultFromRepo != null) {
                        // 2a. Nếu tìm thấy: Lấy DTO đầy đủ để lấy Tags của họ
                        BaseItemDto personDto = itemRepository.getFullItemDetails(personTagResultFromRepo.getId());
                        if (personDto != null && personDto.getTagItems() != null) {
                            tagsFromPerson.addAll(parseNameLongIdPair(personDto.getTagItems()));
                        }
                        actorTagToAlwaysAdd = personTagResultFromRepo; // Dùng Tag có ID
                    } else {
                        // 2b. Nếu không tìm thấy: Tạo Tag mới (không có ID)
                        actorTagToAlwaysAdd = Tag.parse(actorName, null);
                    }
                }

                // 3. Cập nhật UI (trên luồng FX)
                final Tag finalActorTagToAlwaysAdd = actorTagToAlwaysAdd;
                Platform.runLater(() -> {
                    // 3a. Cập nhật Ngày phát hành
                    if (info.getReleaseDate() != null) {
                        releaseDate.set(dateToString(info.getReleaseDate()));
                    }

                    // 3b. Merge (hợp nhất) Tags của diễn viên vào item (UR-38)
                    if (!tagsFromPerson.isEmpty()) {
                        Set<Tag> existingTags = new HashSet<>(tagItems);
                        tagItems.addAll(tagsFromPerson.stream()
                                .filter(t -> !existingTags.contains(t)) // Chỉ thêm nếu chưa có
                                .collect(Collectors.toList()));
                    }

                    // 3c. Thêm diễn viên vào danh sách People (UR-38)
                    if (finalActorTagToAlwaysAdd != null && !peopleItems.contains(finalActorTagToAlwaysAdd)) {
                        peopleItems.add(finalActorTagToAlwaysAdd);
                    }

                    notificationService.showStatus(configService.getString("itemDetailView", "statusFetchDateSuccess"));
                    loading.set(false);
                });

            } catch (Exception e) {
                // Xử lý lỗi
                Platform.runLater(() -> {
                    notificationService.showStatus(configService.getString("itemDetailView", "errorFetchDate", e.getMessage()));
                    loading.set(false);
                });
            }
        }).start();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clonePropertiesCommand(String propertyType) {
        // 1. Lấy thư mục cha (đích) từ Cột 1
        TreeItem<LibraryTreeItem> selectedFolder = libraryTreeViewModel.selectedTreeItemProperty().get();
        if (selectedFolder == null || selectedFolder.getValue() == null || selectedFolder.getValue().isLoadingNode()) {
            notificationService.showStatus(configService.getString("itemDetailView", "cloneErrorNoParent"));
            return;
        }
        // 2. Lấy item hiện tại (nguồn) từ Cột 3
        if (currentItemId == null) {
            notificationService.showStatus(configService.getString("itemDetailView", "cloneErrorNoSource"));
            return;
        }

        final String targetParentId = selectedFolder.getValue().getItemDto().getId();
        final String sourceItemId = this.currentItemId;

        // Báo trạng thái
        notificationService.showStatus(configService.getString("itemDetailView", "cloneStatusStart", propertyType));
        loading.set(true);

        // Chạy trên luồng nền
        new Thread(() -> {
            try {
                int count = 0;
                // 3. Gọi service GHI tương ứng (UR-37)
                switch (propertyType) {
                    case "Tags":
                        count = itemUpdateService.cloneTags(sourceItemId, targetParentId);
                        break;
                    case "Studios":
                        count = itemUpdateService.cloneStudios(sourceItemId, targetParentId);
                        break;
                    case "Genres":
                        count = itemUpdateService.cloneGenres(sourceItemId, targetParentId);
                        break;
                    case "People":
                        count = itemUpdateService.clonePeople(sourceItemId, targetParentId);
                        break;
                }

                // 4. Cập nhật UI (trên luồng FX)
                final int finalCount = count;
                Platform.runLater(() -> {
                    notificationService.showStatus(configService.getString("itemDetailView", "cloneStatusSuccess", propertyType, finalCount));
                    loading.set(false);
                });
            } catch (Exception e) {
                // Xử lý lỗi
                Platform.runLater(() -> {
                    notificationService.showStatus(configService.getString("itemDetailView", "cloneStatusError", e.getMessage()));
                    loading.set(false);
                });
            }
        }).start();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void openFileOrFolderCommand() {
        String path = itemPath.get();
        try {
            // 1. Ủy thác cho service tương tác local (UR-39)
            localInteractionService.openFileOrFolder(path);

            // 2. Nếu là file (không phải folder), kích hoạt sự kiện Pop-out (UR-50)
            if (!isFolder.get()) {
                Platform.runLater(() -> popOutRequest.set(true));
            }
        } catch (Exception e) {
            notificationService.showStatus(configService.getString("itemDetailView", "errorOpenPath", e.getMessage()));
        }
    }

    @Override
    public void playNextItemCommand() {
        playNextRequest.set(true);
    }

    @Override
    public void playPreviousItemCommand() {
        playPreviousRequest.set(true);
    }

    @Override
    public ReadOnlyBooleanProperty playNextRequestProperty() {
        return playNextRequest.getReadOnlyProperty();
    }

    @Override
    public ReadOnlyBooleanProperty playPreviousRequestProperty() {
        return playPreviousRequest.getReadOnlyProperty();
    }

    @Override
    public void clearPlayNextRequest() {
        playNextRequest.set(false);
    }

    @Override
    public void clearPlayPreviousRequest() {
        playPreviousRequest.set(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void openSubtitleCommand() {
        String path = itemPath.get();
        String title = titleProperty().get();
        try {
            // Ủy thác cho service (UR-40)
            File srtFile = localInteractionService.openSubtitleFile(path, title);
            // Mở thư mục chứa file .srt (UR-40)
            localInteractionService.openContainingFolder(srtFile.getAbsolutePath());
            notificationService.showStatus(configService.getString("itemDetailView", "statusSubtitleCreated", srtFile.getName()));
        } catch (Exception e) {
            notificationService.showStatus(configService.getString("itemDetailView", "errorSubtitleOpen", e.getMessage()));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void openScreenshotFolderCommand() {
        String path = itemPath.get();
        try {
            String mediaFileName = new File(path).getName();
            // Ủy thác cho service (UR-43)
            localInteractionService.openScreenshotFolder(mediaFileName);
        } catch (Exception e) {
            notificationService.showStatus(configService.getString("itemDetailView", "errorScreenshotPathOpen", e.getMessage()));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDroppedPrimaryImage(File imageFile) {
        if (imageFile == null || currentItemId == null) return;
        // 1. Lưu file local vào state
        newPrimaryImageFile.set(imageFile);
        try {
            // 2. Tạo ảnh preview (từ file local) và hiển thị
            Image localImage = new Image(imageFile.toURI().toString());
            primaryImage.set(localImage);
            // 3. (primaryImageDirty sẽ tự động set = true nhờ listener)
        } catch (Exception e) {
            notificationService.showStatus(configService.getString("itemDetailViewModel", "errorImagePreview"));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveNewPrimaryImageCommand() {
        File fileToSave = newPrimaryImageFile.get();
        if (fileToSave == null || currentItemId == null) return;

        // Báo trạng thái
        notificationService.showStatus(configService.getString("itemDetailViewModel", "statusUploadingPrimary"));
        loading.set(true);

        // Chạy trên luồng nền
        new Thread(() -> {
            try {
                // Gọi service GHI để upload (UR-42)
                itemUpdateService.uploadImage(currentItemId, ImageType.PRIMARY, fileToSave);

                // Cập nhật UI (trên luồng FX)
                Platform.runLater(() -> {
                    notificationService.showStatus(configService.getString("itemDetailViewModel", "statusUploadPrimarySuccess"));
                    newPrimaryImageFile.set(null); // Xóa file chờ
                    loading.set(false);
                    loadItem(originalItemDto); // Tải lại để lấy ảnh mới từ server
                });
            } catch (Exception e) {
                // Xử lý lỗi
                Platform.runLater(() -> {
                    notificationService.showStatus(configService.getString("itemDetailViewModel", "errorUploadPrimary", e.getMessage()));
                    loading.set(false);
                });
            }
        }).start();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteBackdropCommand(ImageInfo backdrop) {
        if (backdrop == null || currentItemId == null) return;

        notificationService.showStatus(configService.getString("itemDetailViewModel", "statusDeletingBackdrop", backdrop.getImageIndex()));

        // Chạy trên luồng nền
        new Thread(() -> {
            try {
                // Gọi service GHI (UR-41)
                itemUpdateService.deleteImage(currentItemId, backdrop.getImageType(), backdrop.getImageIndex());

                // Cập nhật UI (trên luồng FX)
                Platform.runLater(() -> {
                    notificationService.showStatus(configService.getString("itemDetailViewModel", "statusDeleteBackdropSuccess"));
                    backdropImages.remove(backdrop); // Xóa khỏi danh sách
                });
            } catch (Exception e) {
                // Xử lý lỗi
                Platform.runLater(() -> notificationService.showStatus(configService.getString("itemDetailViewModel", "errorDeleteBackdrop", e.getMessage())));
            }
        }).start();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void uploadDroppedBackdropFiles(List<File> files) {
        if (files == null || files.isEmpty() || currentItemId == null) return;

        notificationService.showStatus(configService.getString("itemDetailViewModel", "statusUploadingBackdrops", files.size()));
        loading.set(true);

        // Chạy trên luồng nền
        new Thread(() -> {
            try {
                // Lặp qua từng file và upload (UR-41)
                for (int i = 0; i < files.size(); i++) {
                    File file = files.get(i);
                    final int current = i + 1;
                    // Cập nhật trạng thái tiến trình (phải chạy trên luồng FX)
                    Platform.runLater(() -> notificationService.showStatus(
                            configService.getString("itemDetailViewModel", "statusUploadingBackdropProgress", current, files.size(), file.getName())
                    ));
                    // Gọi service GHI
                    itemUpdateService.uploadImage(currentItemId, ImageType.BACKDROP, file);
                }

                // Hoàn tất (trên luồng FX)
                Platform.runLater(() -> {
                    notificationService.showStatus(configService.getString("itemDetailViewModel", "statusUploadBackdropSuccess", files.size()));
                    loading.set(false);
                    loadItem(originalItemDto); // Tải lại để lấy gallery mới
                });
            } catch (Exception e) {
                // Xử lý lỗi
                Platform.runLater(() -> {
                    notificationService.showStatus(configService.getString("itemDetailViewModel", "errorUploadBackdrop", e.getMessage()));
                    loading.set(false);
                });
            }
        }).start();
    }


    /**
     * {@inheritDoc}
     */
    @Override public void addTagCommand() {
        this.lastAddContext = SuggestionContext.TAG; // Lưu context (UR-13)
        addChipCommand.set(SuggestionContext.TAG); // Bắn sự kiện (UR-35)
    }
    /**
     * {@inheritDoc}
     */
    @Override public void addStudioCommand() {
        this.lastAddContext = SuggestionContext.STUDIO;
        addChipCommand.set(SuggestionContext.STUDIO);
    }
    /**
     * {@inheritDoc}
     */
    @Override public void addGenreCommand() {
        this.lastAddContext = SuggestionContext.GENRE;
        addChipCommand.set(SuggestionContext.GENRE);
    }
    /**
     * {@inheritDoc}
     */
    @Override public void addPeopleCommand() {
        this.lastAddContext = SuggestionContext.PEOPLE;
        addChipCommand.set(SuggestionContext.PEOPLE);
    }

    /**
     * {@inheritDoc}
     */
    @Override public ReadOnlyObjectProperty<SuggestionContext> addChipCommandProperty() { return addChipCommand.getReadOnlyProperty(); }
    /**
     * {@inheritDoc}
     */
    @Override public void clearAddChipCommand() { addChipCommand.set(null); }

    /**
     * {@inheritDoc}
     */
    @Override public void removeTag(Tag tag) { tagItems.remove(tag); }
    /**
     * {@inheritDoc}
     */
    @Override public void removeStudio(Tag tag) { studioItems.remove(tag); }
    /**
     * {@inheritDoc}
     */
    @Override public void removeGenre(Tag tag) { genreItems.remove(tag); }
    /**
     * {@inheritDoc}
     */
    @Override public void removePeople(Tag tag) { peopleItems.remove(tag); }

    /**
     * {@inheritDoc}
     */
    @Override
    public void repeatAddChipCommand() {
        if (currentItemId == null) {
            notificationService.showStatus(configService.getString("itemDetailViewModel", "errorSave"));
            return;
        }
        // Kích hoạt lại sự kiện "Add Chip" với bối cảnh cuối cùng (UR-13)
        addChipCommand.set(this.lastAddContext);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void processAddTagResult(AddTagResult result, SuggestionContext context) {
        if (result == null) return; // Người dùng hủy

        // Lưu lại bối cảnh (context) vừa dùng thành công (UR-13)
        this.lastAddContext = context;

        if (result.isTag()) {
            // KỊCH BẢN 1: Người dùng tạo Tag mới
            Tag newTag = result.getTag();
            switch (context) {
                case TAG:
                    if (!tagItems.contains(newTag)) tagItems.add(newTag);
                    break;
                case STUDIO:
                    if (!studioItems.contains(newTag)) studioItems.add(newTag);
                    break;
                case PEOPLE:
                    if (!peopleItems.contains(newTag)) peopleItems.add(newTag);
                    break;
                case GENRE:
                    if (!genreItems.contains(newTag)) genreItems.add(newTag);
                    break;
            }
        } else if (result.isCopy()) {
            // KỊCH BẢN 2: Người dùng Copy by ID (UR-35)
            copyPropertiesFromItemCommand(result.getCopyId(), context);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReadOnlyObjectProperty<Boolean> popOutRequestProperty() {
        return popOutRequest.getReadOnlyProperty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearPopOutRequest() {
        popOutRequest.set(null);
    }

    /**
     * Xử lý logic Copy by ID (UR-35) (sau khi dialog đóng).
     *
     * @param sourceItemId ID item nguồn (từ AddTagResult).
     * @param context      Loại thuộc tính cần sao chép (từ AddTagResult).
     */
    private void copyPropertiesFromItemCommand(String sourceItemId, SuggestionContext context) {
        if (currentItemId == null) {
            notificationService.showStatus(configService.getString("itemDetailViewModel", "errorSave"));
            return;
        }
        notificationService.showStatus(configService.getString("addTagDialog", "copyStatusLoading", sourceItemId));

        // Chạy trên luồng nền
        new Thread(() -> {
            try {
                // 1. Lấy DTO đầy đủ của item NGUỒN
                BaseItemDto sourceDto = itemRepository.getFullItemDetails(sourceItemId);
                if (sourceDto == null) {
                    throw new Exception(configService.getString("addTagDialog", "copyErrorNotFound"));
                }

                // 2. Trích xuất danh sách thuộc tính cần sao chép
                List<Tag> sourceTagsToCopy = new ArrayList<>();
                switch (context) {
                    case TAG:
                        sourceTagsToCopy.addAll(parseNameLongIdPair(sourceDto.getTagItems()));
                        break;
                    case STUDIO:
                        sourceTagsToCopy.addAll(parseNameLongIdPair(sourceDto.getStudios()));
                        break;
                    case PEOPLE:
                        sourceTagsToCopy.addAll(parseBaseItemPerson(sourceDto.getPeople()));
                        break;
                    case GENRE:
                        sourceTagsToCopy.addAll(parseStringList(sourceDto.getGenres()));
                        break;
                }

                // 3. Cập nhật UI (trên luồng FX)
                final List<Tag> finalSourceTags = sourceTagsToCopy;
                Platform.runLater(() -> {
                    // 3a. Xác định danh sách ĐÍCH (trong ViewModel)
                    ObservableList<Tag> destinationList;
                    switch (context) {
                        case TAG: destinationList = tagItems; break;
                        case STUDIO: destinationList = studioItems; break;
                        case PEOPLE: destinationList = peopleItems; break;
                        case GENRE: destinationList = genreItems; break;
                        default: return;
                    }

                    // 3b. Merge (hợp nhất) danh sách
                    Set<Tag> existingTags = new HashSet<>(destinationList);
                    int addedCount = 0;
                    for (Tag newTag : finalSourceTags) {
                        if (existingTags.add(newTag)) { // HashSet.add trả về true nếu thêm mới
                            destinationList.add(newTag);
                            addedCount++;
                        }
                    }
                    notificationService.showStatus(configService.getString("addTagDialog", "copySuccessStatus", addedCount, sourceItemId));
                });

            } catch (Exception e) {
                // Xử lý lỗi
                Platform.runLater(() -> notificationService.showStatus(configService.getString("addTagDialog", "copyErrorStatus", sourceItemId, e.getMessage())));
            }
        }).start();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void importAndPreview(File file) {
        if (originalItemDto == null) return;
        notificationService.showStatus(configService.getString("itemDetailView", "errorImportRead"));
        loading.set(true);

        // Chạy trên luồng nền
        new Thread(() -> {
            try {
                // 1. Dùng JsonFileHandler để đọc và parse file
                BaseItemDto importedDto = jsonFileHandler.readJsonFileToObject(file);
                if (importedDto != null) {
                    // 2. Cập nhật UI (trên luồng FX)
                    Platform.runLater(() -> {
                        // 3. Ủy thác cho ImportHandler xử lý preview (UR-46)
                        importHandler.importAndPreview(importedDto);
                        loading.set(false);
                        notificationService.clearStatus();
                    });
                } else {
                    throw new Exception(configService.getString("itemDetailView", "errorImportInvalid"));
                }
            } catch (Exception ex) {
                // Xử lý lỗi
                Platform.runLater(() -> {
                    notificationService.showStatus(configService.getString("itemDetailView", "errorImportReadThread", ex.getMessage()));
                    loading.set(false);
                });
            }
        }).start();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void exportCommand(File file) {
        if (originalItemDto == null) {
            notificationService.showStatus(configService.getString("itemDetailView", "errorExportNoData"));
            return;
        }
        // Dùng DTO gốc (chưa chỉnh sửa) để export
        final BaseItemDto dtoToExport = this.originalItemDto;

        // Chạy trên luồng nền
        new Thread(() -> {
            try {
                // Ủy thác cho JsonFileHandler (UR-44)
                jsonFileHandler.writeObjectToJsonFile(dtoToExport, file);
                Platform.runLater(() -> notificationService.showStatus(
                        configService.getString("itemDetailView", "exportSuccess", file.getName())
                ));
            } catch (Exception ex) {
                // Xử lý lỗi
                Platform.runLater(() -> notificationService.showStatus(
                        configService.getString("itemDetailView", "errorExportWriteThread", ex.getMessage())
                ));
            }
        }).start();
    }

    /**
     * Gợi ý Tên gốc (Original Title) từ đường dẫn tệp media.
     * (Logic nghiệp vụ tùy chỉnh).
     *
     * @param path Đường dẫn tệp media.
     * @return Tên gốc đã được chuẩn hóa (ví dụ: "ABC-123").
     */
    private String suggestOriginalTitleFromPath(String path) {
        if (path == null || path.isEmpty()) return null;
        File file = new File(path);
        if (file.isDirectory()) return null; // Không gợi ý cho thư mục

        String fileName = file.getName();
        if (fileName.isEmpty()) return null;

        // 1. Loại bỏ phần mở rộng (ví dụ: .mp4, .mkv)
        int lastDot = fileName.lastIndexOf('.');
        String baseName = (lastDot == -1) ? fileName : fileName.substring(0, lastDot);

        // 2. Loại bỏ domain (ví dụ: "abc.com@XYZ-123" -> "XYZ-123")
        String[] parts = baseName.split("@", 2);
        if (parts.length > 1) {
            baseName = parts[1];
        }

        // 3. Chuẩn hóa tên (xóa rác, chuẩn hóa ký tự)
        baseName = normalizeFileName(baseName);
        return baseName;
    }

    /**
     * Helper chuẩn hóa tên file (logic nghiệp vụ tùy chỉnh).
     *
     * @param input Tên file (không có mở rộng).
     * @return Tên đã chuẩn hóa.
     */
    public static String normalizeFileName(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        // 1. Loại bỏ các hậu tố chất lượng (HD, 4K, 1080P)
        String cleanedInput = input
                .replaceAll("(?i)[._-]?[\\p{Punct}&&[^.-_]]*4k[\\p{Punct}&&[^.-_]]*$", "")
                .replaceAll("(?i)[._-][\\p{Punct}&&[^.-_]]*(HD|1080P|720P)[\\p{Punct}&&[^.-_]]*$", "")
                .replaceAll("(?i)[._-](4k|hd|1080p|720p)$", "");

        // 2. Chèn dấu '-' giữa chữ và số (ví dụ: ABC317 -> ABC-317)
        String normalized = cleanedInput
                .replaceAll("([a-zA-Z])(\\d)", "$1-$2")
                .replaceAll("(\\d)([a-zA-Z])", "$1-$2");

        // 3. Thay thế các ký tự đặc biệt (không phải chữ/số/gạch ngang) bằng '-'
        normalized = normalized
                .replaceAll("[^a-zA-Z0-9-]", "-")
                .replaceAll("-+", "-") // Gộp nhiều dấu '-'
                .replaceAll("^-|-$", ""); // Xóa '-' ở đầu/cuối

        // 4. Tách các phần và viết hoa
        String[] parts = normalized.split("-");
        StringBuilder result = new StringBuilder();
        boolean firstPart = true;

        for (String part : parts) {
            if (part.isEmpty()) continue;

            // 5. Loại bỏ các hậu tố chất lượng còn sót lại
            if (part.matches("(?i)\\d*K|HD|1080P|720P|MP4|MKV|COM|NET|ORG")) {
                continue;
            }

            if (!firstPart) {
                result.append("-");
            }

            result.append(part.toUpperCase());
            firstPart = false;
        }

        return result.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override public void acceptImportField(String fieldName) { importHandler.acceptImportField(fieldName); }
    /**
     * {@inheritDoc}
     */
    @Override public void rejectImportField(String fieldName) { importHandler.rejectImportField(fieldName); }
    /**
     * {@inheritDoc}
     */
    @Override public void markAsDirtyByAccept() { dirtyTracker.forceDirty(); }
    /**
     * {@inheritDoc}
     */
    @Override public String getExportFileName() {
        String name = originalTitle.get();
        // Nếu OriginalTitle rỗng, dùng Title
        if (name == null || name.trim().isEmpty()) {
            name = title.get();
        }
        if (name == null || name.trim().isEmpty()) {
            return configService.getString("itemDetailView", "defaultExportName");
        }
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override public void clearChipClickEvent() { chipClickEvent.set(null); }
    /**
     * {@inheritDoc}
     */
    @Override public void fireChipClickEvent(Tag model, String type) { chipClickEvent.set(new ChipClickEvent(model, type)); }

    // --- Getters cho Properties (Binding) ---
    // (Các getter chuẩn cho JavaFX Properties)
    @Override public ReadOnlyBooleanProperty loadingProperty() { return loading.getReadOnlyProperty(); }
    @Override public ReadOnlyStringProperty statusMessageProperty() { return statusMessage.getReadOnlyProperty(); }
    @Override public ReadOnlyBooleanProperty showStatusMessageProperty() { return showStatusMessage.getReadOnlyProperty(); }
    @Override public StringProperty titleProperty() { return title; }
    @Override public StringProperty originalTitleProperty() { return originalTitle; }
    @Override public ObjectProperty<Float> criticRatingProperty() { return criticRating; }
    @Override public StringProperty overviewProperty() { return overview; }
    @Override public StringProperty releaseDateProperty() { return releaseDate; }
    @Override public ReadOnlyStringProperty itemPathProperty() { return itemPath.getReadOnlyProperty(); }
    @Override public ReadOnlyBooleanProperty isFolderProperty() { return isFolder.getReadOnlyProperty(); }
    @Override public ReadOnlyObjectProperty<Image> primaryImageProperty() { return primaryImage.getReadOnlyProperty(); }
    @Override public ObservableList<ImageInfo> getBackdropImages() { return backdropImages; }
    @Override public ObservableList<Tag> getTagItems() { return tagItems; }
    @Override public ObservableList<Tag> getStudioItems() { return studioItems; }
    @Override public ObservableList<Tag> getPeopleItems() { return peopleItems; }
    @Override public ObservableList<Tag> getGenreItems() { return genreItems; }
    @Override public ReadOnlyBooleanProperty isDirtyProperty() { return dirtyTracker.isDirtyProperty(); }
    @Override public ReadOnlyBooleanProperty primaryImageDirtyProperty() { return primaryImageDirty.getReadOnlyProperty(); }
    @Override public ReadOnlyObjectProperty<ChipClickEvent> chipClickEventProperty() { return chipClickEvent.getReadOnlyProperty(); }

    @Override public ReadOnlyBooleanProperty showTitleReviewProperty() { return importHandler.showTitleReviewProperty(); }
    @Override public ReadOnlyBooleanProperty showOverviewReviewProperty() { return importHandler.showOverviewReviewProperty(); }
    @Override public ReadOnlyBooleanProperty showReleaseDateReviewProperty() { return importHandler.showReleaseDateReviewProperty(); }
    @Override public ReadOnlyBooleanProperty showOriginalTitleReviewProperty() { return importHandler.showOriginalTitleReviewProperty(); }
    @Override public ReadOnlyBooleanProperty showStudiosReviewProperty() { return importHandler.showStudiosReviewProperty(); }
    @Override public ReadOnlyBooleanProperty showPeopleReviewProperty() { return importHandler.showPeopleReviewProperty(); }
    @Override public ReadOnlyBooleanProperty showGenresReviewProperty() { return importHandler.showGenresReviewProperty(); }
    @Override public ReadOnlyBooleanProperty showTagsReviewProperty() { return importHandler.showTagsReviewProperty(); }
    @Override public ReadOnlyBooleanProperty showCriticRatingReviewProperty() { return importHandler.showCriticRatingReviewProperty(); }
}