package com.vinhtt.embyclientsolid.viewmodel.impl;

import com.vinhtt.embyclientsolid.core.*;
import com.vinhtt.embyclientsolid.data.*;
import com.vinhtt.embyclientsolid.model.ReleaseInfo;
import com.vinhtt.embyclientsolid.model.SuggestionItem;
import com.vinhtt.embyclientsolid.model.Tag;
import com.vinhtt.embyclientsolid.viewmodel.IItemDetailViewModel;
import embyclient.model.*;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import javafx.collections.ListChangeListener;

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

/**
 * Triển khai (Implementation) của IItemDetailViewModel (Cột 3).
 * (Cập nhật: Sửa lỗi 1 và 2).
 */
public class ItemDetailViewModel implements IItemDetailViewModel {

    // --- Services (DI) ---
    private final IItemRepository itemRepository;
    private final IItemUpdateService itemUpdateService;
    private final IStaticDataRepository staticDataRepository;
    private final IExternalDataService externalDataService;
    private final ILocalInteractionService localInteractionService;
    private final INotificationService notificationService;
    private final IEmbySessionService sessionService; // (Cần cho URL ảnh)

    // --- Trạng thái nội bộ ---
    private BaseItemDto originalItemDto;
    private String currentItemId;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    // --- Properties (Trạng thái UI) ---
    private final ReadOnlyBooleanWrapper loading = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyStringWrapper statusMessage = new ReadOnlyStringWrapper("Vui lòng chọn một item...");
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

    // --- Trạng thái Dirty/Import (UR-46, UR-48) ---
    private final BooleanProperty isDirty = new SimpleBooleanProperty(false);
    private final ObjectProperty<File> newPrimaryImageFile = new SimpleObjectProperty<>(null);
    private final ReadOnlyBooleanWrapper primaryImageDirty = new ReadOnlyBooleanWrapper(false);
    // (Các property cho review (v/x) sẽ được thêm sau)

    // --- Sự kiện (Event) ---
    private final ReadOnlyObjectWrapper<ChipClickEvent> chipClickEvent = new ReadOnlyObjectWrapper<>(null);

    public ItemDetailViewModel(IItemRepository itemRepository, IItemUpdateService itemUpdateService,
                               IStaticDataRepository staticDataRepository, IExternalDataService externalDataService,
                               ILocalInteractionService localInteractionService, INotificationService notificationService,
                               IEmbySessionService sessionService) {
        this.itemRepository = itemRepository;
        this.itemUpdateService = itemUpdateService;
        this.staticDataRepository = staticDataRepository;
        this.externalDataService = externalDataService;
        this.localInteractionService = localInteractionService;
        this.notificationService = notificationService;
        this.sessionService = sessionService;

        // (UR-48: Kích hoạt nút Save nếu có thay đổi)
        setupDirtyTracking();
        // (UR-42: Kích hoạt nút Lưu ảnh)
        newPrimaryImageFile.addListener((obs, oldVal, newVal) -> primaryImageDirty.set(newVal != null));
    }

    /**
     * Cài đặt theo dõi thay đổi (dirty tracking) cho UR-48.
     */
    private void setupDirtyTracking() {
        // (Đây là phiên bản đơn giản, logic phức tạp hơn (Import/Export) sẽ ở Giai đoạn 10)
        title.addListener((obs, oldVal, newVal) -> isDirty.set(true));
        originalTitle.addListener((obs, oldVal, newVal) -> isDirty.set(true));
        // (Rating được lưu ngay, không cần set dirty)
        overview.addListener((obs, oldVal, newVal) -> isDirty.set(true));
        releaseDate.addListener((obs, oldVal, newVal) -> isDirty.set(true));
        tagItems.addListener((ListChangeListener<Tag>) c -> isDirty.set(true));
        studioItems.addListener((ListChangeListener<Tag>) c -> isDirty.set(true));
        peopleItems.addListener((ListChangeListener<Tag>) c -> isDirty.set(true));
        genreItems.addListener((ListChangeListener<Tag>) c -> isDirty.set(true));
    }

    @Override
    public void loadItem(BaseItemDto item) {
        if (item == null) {
            Platform.runLater(this::clearAllDetailsUI);
            return;
        }
        final String newItemId = item.getId();
        Platform.runLater(() -> {
            clearAllDetailsUI();
            statusMessage.set("Đang tải chi tiết cho: " + item.getName());
            showStatusMessage.set(true);
            loading.set(true);
        });

        new Thread(() -> {
            try {
                // (UR-30)
                BaseItemDto loadedDto = itemRepository.getFullItemDetails(newItemId);
                // (UR-41)
                List<ImageInfo> backdrops = itemRepository.getItemImages(newItemId).stream()
                        .filter(img -> ImageType.BACKDROP.equals(img.getImageType()))
                        .collect(Collectors.toList());

                Platform.runLater(() -> {
                    this.originalItemDto = loadedDto;
                    this.currentItemId = newItemId;

                    // Điền dữ liệu vào Properties
                    title.set(loadedDto.getName() != null ? loadedDto.getName() : "");
                    originalTitle.set(loadedDto.getOriginalTitle() != null ? loadedDto.getOriginalTitle() : "");
                    criticRating.set(loadedDto.getCriticRating()); // (UR-32)
                    overview.set(loadedDto.getOverview() != null ? loadedDto.getOverview() : "");
                    releaseDate.set(dateToString(loadedDto.getPremiereDate()));
                    itemPath.set(loadedDto.getPath() != null ? loadedDto.getPath() : "Không có đường dẫn");
                    isFolder.set(Boolean.TRUE.equals(loadedDto.isIsFolder()));

                    // (UR-34: Chuyển đổi DTOs sang Model 'Tag')
                    tagItems.setAll(parseNameLongIdPair(loadedDto.getTagItems()));
                    studioItems.setAll(parseNameLongIdPair(loadedDto.getStudios()));
                    genreItems.setAll(parseStringList(loadedDto.getGenres()));
                    peopleItems.setAll(parseBaseItemPerson(loadedDto.getPeople()));

                    // (UR-41, UR-42: Tải ảnh)
                    primaryImage.set(getPrimaryImageUrl(loadedDto));
                    backdropImages.setAll(backdrops);

                    loading.set(false);
                    showStatusMessage.set(false);
                    isDirty.set(false); // Reset trạng thái dirty
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    clearAllDetailsUI();
                    statusMessage.set("Lỗi khi tải chi tiết: " + e.getMessage());
                    showStatusMessage.set(true);
                    loading.set(false);
                });
            }
        }).start();
    }

    private void clearAllDetailsUI() {
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
        isDirty.set(false);
        newPrimaryImageFile.set(null);
        statusMessage.set("Vui lòng chọn một item...");
        showStatusMessage.set(true);
    }

    // --- Helpers Chuyển đổi DTO -> Model ---
    private List<Tag> parseNameLongIdPair(List<NameLongIdPair> list) {
        if (list == null) return new ArrayList<>();
        return list.stream()
                .map(pair -> Tag.parse(pair.getName(), pair.getId().toString()))
                .collect(Collectors.toList());
    }
    private List<Tag> parseBaseItemPerson(List<BaseItemPerson> list) {
        if (list == null) return new ArrayList<>();
        return list.stream()
                .map(person -> Tag.parse(person.getName(), person.getId()))
                .collect(Collectors.toList());
    }
    private List<Tag> parseStringList(List<String> list) {
        if (list == null) return new ArrayList<>();
        return list.stream()
                .map(name -> Tag.parse(name, null)) // Genres không có ID
                .collect(Collectors.toList());
    }
    private Image getPrimaryImageUrl(BaseItemDto dto) {
        if (dto.getImageTags() != null && dto.getImageTags().containsKey("Primary")) {
            String tag = dto.getImageTags().get("Primary");
            String serverUrl = sessionService.getApiClient().getBasePath();
            String url = String.format("%s/Items/%s/Images/Primary?tag=%s&maxWidth=%d&quality=90",
                    serverUrl, dto.getId(), tag, 600);
            return new Image(url, true);
        }
        return null;
    }
    private String dateToString(OffsetDateTime date) {
        if (date == null) return "";
        try {
            return dateFormat.format(new Date(date.toInstant().toEpochMilli()));
        } catch (Exception e) { return ""; }
    }

    /**
     * (SỬA LỖI 2: Triển khai getBackdropUrl).
     * Helper build URL cho ảnh backdrop (dùng trong Controller).
     */
    @Override
    public String getBackdropUrl(ImageInfo info) {
        if (info == null || info.getImageType() == null || currentItemId == null) {
            return null;
        }
        String serverUrl = sessionService.getApiClient().getBasePath();
        String imageType = info.getImageType().getValue();
        Integer index = info.getImageIndex();

        if (index != null) {
            return String.format("%s/Items/%s/Images/%s/%d?maxWidth=%d&quality=90",
                    serverUrl, currentItemId, imageType, index, 400);
        }
        return null;
    }

    // --- Commands (Hành động) ---

    @Override
    public void saveChangesCommand() {
        // (UR-31, UR-34, UR-49)
        if (originalItemDto == null || currentItemId == null) return;

        notificationService.showStatus("Đang lưu thay đổi...");
        loading.set(true);

        // Tạo DTO mới dựa trên DTO gốc
        BaseItemDto dtoToSave = new BaseItemDto();

        // (Logic này sẽ được cải tiến ở Giai đoạn 10 để xử lý Import/Export)
        dtoToSave.setName(title.get());
        dtoToSave.setOriginalTitle(originalTitle.get());
        dtoToSave.setOverview(overview.get());

        // (UR-33: Critic Rating được lưu riêng, nhưng chúng ta vẫn gửi nó)
        dtoToSave.setCriticRating(criticRating.get());

        try {
            Date parsedDate = dateFormat.parse(releaseDate.get());
            Instant instant = Instant.ofEpochMilli(parsedDate.getTime());
            dtoToSave.setPremiereDate(OffsetDateTime.ofInstant(instant, ZoneId.systemDefault()));
        } catch (ParseException e) {
            dtoToSave.setPremiereDate(originalItemDto.getPremiereDate()); // Giữ ngày cũ nếu parse lỗi
        }

        // Chuyển đổi Model 'Tag' ngược lại DTO
        dtoToSave.setTagItems(tagItems.stream().map(tag -> new NameLongIdPair().name(tag.serialize())).collect(Collectors.toList()));
        dtoToSave.setStudios(studioItems.stream().map(tag -> new NameLongIdPair().name(tag.serialize())).collect(Collectors.toList()));
        dtoToSave.setGenres(genreItems.stream().map(Tag::getDisplayName).collect(Collectors.toList()));
        dtoToSave.setPeople(peopleItems.stream().map(tag -> new BaseItemPerson().name(tag.serialize()).type(PersonType.ACTOR)).collect(Collectors.toList()));

        new Thread(() -> {
            try {
                itemUpdateService.updateItem(currentItemId, dtoToSave);
                Platform.runLater(() -> {
                    notificationService.showStatus("Đã lưu thành công!");
                    isDirty.set(false);
                    loading.set(false);
                    // Tải lại item để cập nhật originalItemDto
                    loadItem(originalItemDto);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    notificationService.showStatus("Lỗi khi lưu: " + e.getMessage());
                    loading.set(false);
                });
            }
        }).start();
    }

    @Override
    public void saveCriticRatingImmediately(Float newRating) {
        // (UR-33)
        if (originalItemDto == null || currentItemId == null) return;

        // Tạo DTO *chỉ* chứa rating
        BaseItemDto ratingDto = new BaseItemDto();
        ratingDto.setCriticRating(newRating);

        notificationService.showStatus("Đang lưu điểm số...");

        new Thread(() -> {
            try {
                itemUpdateService.updateItem(currentItemId, ratingDto);
                Platform.runLater(() -> {
                    notificationService.showStatus("Đã lưu điểm số!");
                    if (originalItemDto != null) {
                        originalItemDto.setCriticRating(newRating); // Cập nhật DTO gốc
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    notificationService.showStatus("Lỗi lưu điểm: " + e.getMessage());
                    // Rollback UI
                    criticRating.set(originalItemDto.getCriticRating());
                });
            }
        }).start();
    }

    @Override
    public void fetchReleaseDateCommand() {
        // (UR-38)
        String code = originalTitle.get();
        if (code == null || code.trim().isEmpty()) {
            notificationService.showStatus("Tiêu đề gốc rỗng, không thể tìm ngày.");
            return;
        }

        notificationService.showStatus("Đang tìm ngày phát hành cho: " + code);
        loading.set(true);

        new Thread(() -> {
            try {
                ReleaseInfo info = externalDataService.fetchReleaseInfoByCode(code);

                if (info == null) {
                    throw new Exception("Không tìm thấy thông tin.");
                }

                // (UR-38: Logic phức tạp - Lấy Tag từ Diễn viên)
                if (info.getActressName() != null && !info.getActressName().isEmpty()) {
                    // 1. Tìm diễn viên
                    List<SuggestionItem> people = staticDataRepository.getPeopleSuggestions();
                    SuggestionItem person = people.stream()
                            .filter(p -> info.getActressName().equalsIgnoreCase(p.getName()))
                            .findFirst().orElse(null);

                    if (person != null) {
                        // 2. Lấy chi tiết diễn viên
                        BaseItemDto personDto = itemRepository.getFullItemDetails(person.getId());
                        if (personDto != null && personDto.getTagItems() != null) {
                            List<Tag> tagsFromPerson = parseNameLongIdPair(personDto.getTagItems());

                            Platform.runLater(() -> {
                                // 3. Merge Tags
                                Set<Tag> existingTags = new HashSet<>(tagItems);
                                tagItems.addAll(tagsFromPerson.stream()
                                        .filter(t -> !existingTags.contains(t))
                                        .collect(Collectors.toList()));

                                // 4. Add Diễn viên
                                Tag personTag = Tag.parse(person.getName(), person.getId());
                                if (!peopleItems.contains(personTag)) {
                                    peopleItems.add(personTag);
                                }
                            });
                        }
                    }
                }

                // Cập nhật ngày phát hành
                Platform.runLater(() -> {
                    if (info.getReleaseDate() != null) {
                        releaseDate.set(dateToString(info.getReleaseDate()));
                    }
                    notificationService.showStatus("Lấy ngày P.H thành công!");
                    loading.set(false);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    notificationService.showStatus("Lỗi khi lấy ngày: " + e.getMessage());
                    loading.set(false);
                });
            }
        }).start();
    }

    @Override
    public void clonePropertiesCommand(String propertyType) {
        // (UR-37)
        // (Cần truy cập Cột 1, MainController sẽ phải tiêm ILibraryTreeViewModel vào đây)
        // (Tạm thời bỏ qua, logic này cần sự phối hợp của MainController)
        notificationService.showStatus("Chức năng Clone (Giai đoạn 10) cần được hoàn thiện.");
    }

    @Override
    public void openFileOrFolderCommand() {
        // (UR-39)
        String path = itemPath.get();
        try {
            localInteractionService.openFileOrFolder(path);
        } catch (Exception e) {
            notificationService.showStatus("Lỗi mở file/thư mục: " + e.getMessage());
        }
    }

    @Override
    public void openSubtitleCommand() {
        // (UR-40)
        String path = itemPath.get();
        String title = titleProperty().get();
        try {
            File srtFile = localInteractionService.openSubtitleFile(path, title);
            // Mở thư mục chứa (UR-40)
            localInteractionService.openContainingFolder(srtFile.getAbsolutePath());
            notificationService.showStatus("Đã mở file Subtitle.");
        } catch (Exception e) {
            notificationService.showStatus("Lỗi mở Subtitle: " + e.getMessage());
        }
    }

    @Override
    public void openScreenshotFolderCommand() {
        // (UR-43)
        String path = itemPath.get();
        try {
            String mediaFileName = new File(path).getName();
            localInteractionService.openScreenshotFolder(mediaFileName);
        } catch (Exception e) {
            notificationService.showStatus("Lỗi mở thư mục Screenshot: " + e.getMessage());
        }
    }

    @Override
    public void setDroppedPrimaryImage(File imageFile) {
        // (UR-42)
        if (imageFile == null || currentItemId == null) return;
        newPrimaryImageFile.set(imageFile);
        try {
            Image localImage = new Image(imageFile.toURI().toString());
            primaryImage.set(localImage);
        } catch (Exception e) {
            notificationService.showStatus("Lỗi preview ảnh local.");
        }
    }

    @Override
    public void saveNewPrimaryImageCommand() {
        // (UR-42)
        File fileToSave = newPrimaryImageFile.get();
        if (fileToSave == null || currentItemId == null) return;

        notificationService.showStatus("Đang upload ảnh Primary...");
        loading.set(true);

        new Thread(() -> {
            try {
                itemUpdateService.uploadImage(currentItemId, ImageType.PRIMARY, fileToSave);
                Platform.runLater(() -> {
                    notificationService.showStatus("Upload ảnh Primary thành công!");
                    newPrimaryImageFile.set(null); // Reset
                    loading.set(false);
                    // Tải lại item để lấy tag ảnh mới
                    loadItem(originalItemDto);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    notificationService.showStatus("Lỗi upload: " + e.getMessage());
                    loading.set(false);
                });
            }
        }).start();
    }

    @Override
    public void deleteBackdropCommand(ImageInfo backdrop) {
        // (UR-41)
        if (backdrop == null || currentItemId == null) return;
        notificationService.showStatus("Đang xóa backdrop...");
        new Thread(() -> {
            try {
                itemUpdateService.deleteImage(currentItemId, backdrop.getImageType(), backdrop.getImageIndex());
                Platform.runLater(() -> {
                    notificationService.showStatus("Xóa backdrop thành công.");
                    backdropImages.remove(backdrop); // Xóa khỏi UI
                });
            } catch (Exception e) {
                Platform.runLater(() -> notificationService.showStatus("Lỗi xóa backdrop: " + e.getMessage()));
            }
        }).start();
    }

    @Override
    public void uploadDroppedBackdropFiles(List<File> files) {
        // (UR-41)
        if (files == null || files.isEmpty() || currentItemId == null) return;
        notificationService.showStatus("Đang upload " + files.size() + " ảnh backdrop...");
        loading.set(true);

        new Thread(() -> {
            try {
                for (File file : files) {
                    itemUpdateService.uploadImage(currentItemId, ImageType.BACKDROP, file);
                }
                Platform.runLater(() -> {
                    notificationService.showStatus("Upload thành công " + files.size() + " backdrop.");
                    loading.set(false);
                    // Tải lại (cách đơn giản nhất)
                    loadItem(originalItemDto);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    notificationService.showStatus("Lỗi upload backdrop: " + e.getMessage());
                    loading.set(false);
                });
            }
        }).start();
    }

    // --- (Tạm thời cho Giai đoạn 10, Giai đoạn 11 sẽ phức tạp hơn) ---
    @Override public void addTagCommand() { tagItems.add(Tag.parse("New Tag", null)); }
    @Override public void addStudioCommand() { studioItems.add(Tag.parse("New Studio", null)); }
    @Override public void addGenreCommand() { genreItems.add(Tag.parse("New Genre", null)); }
    @Override public void addPeopleCommand() { peopleItems.add(Tag.parse("New Person", null)); }

    @Override public void removeTag(Tag tag) { tagItems.remove(tag); }
    @Override public void removeStudio(Tag tag) { studioItems.remove(tag); }
    @Override public void removeGenre(Tag tag) { genreItems.remove(tag); }
    @Override public void removePeople(Tag tag) { peopleItems.remove(tag); }

    // --- (Tạm thời cho Giai đoạn 10) ---
    @Override public void importAndPreview(File file) { notificationService.showStatus("Import (GĐ 10) chưa hoàn thiện."); }
    @Override public void exportCommand(File file) { notificationService.showStatus("Export (GĐ 10) chưa hoàn thiện."); }
    @Override public void acceptImportField(String fieldName) {}
    @Override public void rejectImportField(String fieldName) {}

    // --- Event (UR-36) ---
    @Override
    public void clearChipClickEvent() {
        chipClickEvent.set(null);
    }

    /**
     * (SỬA LỖI 1: Triển khai phương thức).
     * Helper nội bộ để kích hoạt sự kiện.
     */
    @Override
    public void fireChipClickEvent(Tag model, String type) {
        chipClickEvent.set(new ChipClickEvent(model, type));
    }

    // --- Getters cho Properties (Binding) ---
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
    @Override public ReadOnlyBooleanProperty isDirtyProperty() { return isDirty; }
    @Override public ReadOnlyBooleanProperty primaryImageDirtyProperty() { return primaryImageDirty.getReadOnlyProperty(); }
    @Override public ReadOnlyObjectProperty<ChipClickEvent> chipClickEventProperty() { return chipClickEvent.getReadOnlyProperty(); }

    // (Tạm thời trả về false cho các nút review)
    @Override public ReadOnlyBooleanProperty showTitleReviewProperty() { return new SimpleBooleanProperty(false); }
    @Override public ReadOnlyBooleanProperty showOverviewReviewProperty() { return new SimpleBooleanProperty(false); }
    @Override public ReadOnlyBooleanProperty showReleaseDateReviewProperty() { return new SimpleBooleanProperty(false); }
    @Override public ReadOnlyBooleanProperty showOriginalTitleReviewProperty() { return new SimpleBooleanProperty(false); }
    @Override public ReadOnlyBooleanProperty showStudiosReviewProperty() { return new SimpleBooleanProperty(false); }
    @Override public ReadOnlyBooleanProperty showPeopleReviewProperty() { return new SimpleBooleanProperty(false); }
    @Override public ReadOnlyBooleanProperty showGenresReviewProperty() { return new SimpleBooleanProperty(false); }
    @Override public ReadOnlyBooleanProperty showTagsReviewProperty() { return new SimpleBooleanProperty(false); }
    @Override public ReadOnlyBooleanProperty showCriticRatingReviewProperty() { return new SimpleBooleanProperty(false); }
}