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
 * Triển khai (Implementation) của IItemDetailViewModel (Cột 3).
 * (Cập nhật: Thêm logic lưu lastAddContext cho hotkey UR-13).
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

    // --- Helpers ---
    private final ItemDetailDirtyTracker dirtyTracker;
    private final ItemDetailImportHandler importHandler;
    private final JsonFileHandler jsonFileHandler;
    private final Gson gson;

    // --- Trạng thái nội bộ ---
    private BaseItemDto originalItemDto;
    private String currentItemId;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    /**
     * (MỚI - GĐ 12/UR-13) Lưu context cuối cùng được sử dụng.
     */
    private SuggestionContext lastAddContext = SuggestionContext.TAG;


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

    private final ObjectProperty<File> newPrimaryImageFile = new SimpleObjectProperty<>(null);
    private final ReadOnlyBooleanWrapper primaryImageDirty = new ReadOnlyBooleanWrapper(false);

    // --- Properties Sự kiện (Event) ---
    private final ReadOnlyObjectWrapper<ChipClickEvent> chipClickEvent = new ReadOnlyObjectWrapper<>(null);
    private final ReadOnlyObjectWrapper<SuggestionContext> addChipCommand = new ReadOnlyObjectWrapper<>(null);


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

        this.dirtyTracker = new ItemDetailDirtyTracker(this);
        this.importHandler = new ItemDetailImportHandler(this, this.dirtyTracker);
        this.jsonFileHandler = new JsonFileHandler(configService);
        this.gson = new GsonBuilder()
                .registerTypeAdapter(OffsetDateTime.class, new JSON.OffsetDateTimeTypeAdapter())
                .create();

        newPrimaryImageFile.addListener((obs, oldVal, newVal) -> primaryImageDirty.set(newVal != null));
    }

    @Override
    public void loadItem(BaseItemDto item) {
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
                    clearAllDetailsUI();
                    this.originalItemDto = loadedDto;
                    this.currentItemId = newItemId;

                    title.set(loadedDto.getName() != null ? loadedDto.getName() : "");
                    originalTitle.set(loadedDto.getOriginalTitle() != null ? loadedDto.getOriginalTitle() : "");
                    criticRating.set(loadedDto.getCriticRating());
                    overview.set(loadedDto.getOverview() != null ? loadedDto.getOverview() : "");
                    releaseDate.set(dateToString(loadedDto.getPremiereDate()));
                    itemPath.set(loadedDto.getPath() != null ? loadedDto.getPath() : configService.getString("itemDetailLoader", "noPath"));
                    isFolder.set(Boolean.TRUE.equals(loadedDto.isIsFolder()));

                    tagItems.setAll(parseNameLongIdPair(loadedDto.getTagItems()));
                    studioItems.setAll(parseNameLongIdPair(loadedDto.getStudios()));
                    genreItems.setAll(parseStringList(loadedDto.getGenres()));
                    peopleItems.setAll(parseBaseItemPerson(loadedDto.getPeople()));

                    primaryImage.set(getPrimaryImageUrl(loadedDto));
                    backdropImages.setAll(backdrops);

                    loading.set(false);
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

    private void clearAllDetailsUI() {
        dirtyTracker.stopTracking();
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
        newPrimaryImageFile.set(null);
        importHandler.clearState();
        statusMessage.set(configService.getString("itemDetailViewModel", "statusDefault"));
        showStatusMessage.set(true);
    }

    private List<Tag> parseNameLongIdPair(List<NameLongIdPair> list) {
        if (list == null) return new ArrayList<>();
        return list.stream()
                .map(pair -> Tag.parse(pair.getName(), pair.getId() != null ? pair.getId().toString() : null))
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
                .map(name -> Tag.parse(name, null))
                .collect(Collectors.toList());
    }
    private Image getPrimaryImageUrl(BaseItemDto dto) {
        if (dto.getId() != null && dto.getImageTags() != null && dto.getImageTags().containsKey("Primary")) {
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
            return dateFormat.format(Date.from(date.toInstant()));
        } catch (Exception e) { return ""; }
    }

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

    @Override
    public void saveChangesCommand() {
        final BaseItemDto dtoAtSaveTime = this.originalItemDto;
        final String idAtSaveTime = this.currentItemId;

        if (dtoAtSaveTime == null || idAtSaveTime == null) {
            notificationService.showStatus(configService.getString("itemDetailViewModel", "errorSave"));
            return;
        }

        notificationService.showStatus(configService.getString("itemDetailViewModel", "statusSaving"));
        loading.set(true);
        importHandler.hideAllReviewButtons();

        final boolean isSavingAfterImport = importHandler.wasImportInProgress();
        final Set<String> acceptedFields = isSavingAfterImport ? importHandler.getAcceptedFields() : null;
        final BaseItemDto importedDto = isSavingAfterImport ? importHandler.getImportedDto() : null;

        final String finalTitle = this.title.get();
        final String finalOriginalTitle = this.originalTitle.get();
        final Float finalCriticRating = this.criticRating.get();
        final String finalOverview = this.overview.get();
        final String finalReleaseDate = this.releaseDate.get();
        final List<Tag> finalTagItems = List.copyOf(this.tagItems);
        final List<Tag> finalStudiosItems = List.copyOf(this.studioItems);
        final List<Tag> finalPeopleItems = List.copyOf(this.peopleItems);
        final List<Tag> finalGenresItems = List.copyOf(this.genreItems);

        new Thread(() -> {
            try {
                BaseItemDto dtoToSendToApi;

                if (isSavingAfterImport) {
                    notificationService.showStatus(configService.getString("itemDetailViewModel", "statusSavingImport"));
                    dtoToSendToApi = createDtoWithAcceptedChanges(
                            dtoAtSaveTime, importedDto, acceptedFields
                    );
                } else {
                    notificationService.showStatus(configService.getString("itemDetailViewModel", "statusSavingManual"));
                    dtoToSendToApi = gson.fromJson(gson.toJson(dtoAtSaveTime), BaseItemDto.class);
                    dtoToSendToApi.setName(finalTitle);
                    dtoToSendToApi.setOriginalTitle(finalOriginalTitle);
                    dtoToSendToApi.setOverview(finalOverview);
                    dtoToSendToApi.setCriticRating(finalCriticRating);
                    dtoToSendToApi.setPremiereDate(parseDateString(finalReleaseDate, dtoAtSaveTime.getPremiereDate()));
                    dtoToSendToApi.setTagItems(convertTagsToNameLongIdPair(finalTagItems));
                    dtoToSendToApi.setStudios(convertTagsToNameLongIdPair(finalStudiosItems));
                    dtoToSendToApi.setPeople(convertTagsToPeopleList(finalPeopleItems));
                    dtoToSendToApi.setGenres(finalGenresItems.stream().map(Tag::getDisplayName).collect(Collectors.toList()));
                    dtoToSendToApi.setGenreItems(new ArrayList<>());
                }

                itemUpdateService.updateItem(idAtSaveTime, dtoToSendToApi);

                Platform.runLater(() -> {
                    notificationService.showStatus(configService.getString("itemDetailViewModel", "statusSaveSuccess"));
                    loading.set(false);
                    loadItem(dtoToSendToApi);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    notificationService.showStatus(configService.getString("itemDetailViewModel", "errorApiSave", e.getClass().getSimpleName(), e.getMessage()));
                    loading.set(false);
                });
            }
        }).start();
    }

    private BaseItemDto createDtoWithAcceptedChanges(BaseItemDto originalDto, BaseItemDto importedDto, Set<String> acceptedFields) {
        BaseItemDto dtoCopy = gson.fromJson(gson.toJson(originalDto), BaseItemDto.class);

        if (acceptedFields.contains("title")) { dtoCopy.setName(importedDto.getName()); }
        if (acceptedFields.contains("originalTitle")) { dtoCopy.setOriginalTitle(importedDto.getOriginalTitle()); }
        if (acceptedFields.contains("criticRating")) { dtoCopy.setCriticRating(importedDto.getCriticRating()); }
        if (acceptedFields.contains("overview")) { dtoCopy.setOverview(importedDto.getOverview()); }
        if (acceptedFields.contains("releaseDate")) { dtoCopy.setPremiereDate(importedDto.getPremiereDate()); }
        if (acceptedFields.contains("tags")) { dtoCopy.setTagItems(importedDto.getTagItems()); }
        if (acceptedFields.contains("studios")) { dtoCopy.setStudios(importedDto.getStudios()); }
        if (acceptedFields.contains("people")) { dtoCopy.setPeople(importedDto.getPeople()); }
        if (acceptedFields.contains("genres")) {
            dtoCopy.setGenres(importedDto.getGenres());
            dtoCopy.setGenreItems(new ArrayList<>());
        }

        return dtoCopy;
    }

    private OffsetDateTime parseDateString(String dateStr, OffsetDateTime fallback) {
        try {
            Date parsedDate = dateFormat.parse(dateStr);
            Instant instant = Instant.ofEpochMilli(parsedDate.getTime());
            return OffsetDateTime.ofInstant(instant, ZoneId.systemDefault());
        } catch (ParseException e) {
            return fallback;
        }
    }

    private List<NameLongIdPair> convertTagsToNameLongIdPair(List<Tag> tags) {
        return tags.stream()
                .map(tag -> new NameLongIdPair().name(tag.serialize()))
                .collect(Collectors.toList());
    }
    private List<BaseItemPerson> convertTagsToPeopleList(List<Tag> tags) {
        return tags.stream()
                .map(tag -> new BaseItemPerson().name(tag.serialize()).type(PersonType.ACTOR))
                .collect(Collectors.toList());
    }

    @Override
    public void saveCriticRatingImmediately(Float newRating) {
        if (originalItemDto == null || currentItemId == null) return;
        BaseItemDto ratingDto = gson.fromJson(gson.toJson(originalItemDto), BaseItemDto.class);
        ratingDto.setCriticRating(newRating);
        loading.set(true);
        notificationService.showStatus(configService.getString("itemDetailViewModel", "statusSavingRating"));
        new Thread(() -> {
            try {
                itemUpdateService.updateItem(currentItemId, ratingDto);
                Platform.runLater(() -> {
                    notificationService.showStatus(configService.getString("itemDetailViewModel", "statusSaveRatingSuccess"));
                    if (originalItemDto != null) {
                        originalItemDto.setCriticRating(newRating);
                    }
                    dirtyTracker.updateOriginalRating(newRating);
                    loading.set(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    notificationService.showStatus(configService.getString("itemDetailViewModel", "errorSaveRating", e.getMessage()));
                    criticRating.set(originalItemDto.getCriticRating());
                    loading.set(false);
                });
            }
        }).start();
    }

    @Override
    public void fetchReleaseDateCommand() {
        String code = originalTitle.get();
        if (code == null || code.trim().isEmpty()) {
            notificationService.showStatus(configService.getString("itemDetailView", "originalTitlePrompt"));
            return;
        }

        notificationService.showStatus(configService.getString("itemDetailView", "statusFetchingDate", code));
        loading.set(true);

        new Thread(() -> {
            try {
                ReleaseInfo info = externalDataService.fetchReleaseInfoByCode(code);
                if (info == null) {
                    throw new Exception(configService.getString("itemDetailView", "statusFetchDateNotFound"));
                }

                List<Tag> tagsFromPerson = new ArrayList<>();
                Tag personTag = null;

                if (info.getActressName() != null && !info.getActressName().isEmpty()) {
                    List<SuggestionItem> people = staticDataRepository.getPeopleSuggestions();
                    SuggestionItem person = people.stream()
                            .filter(p -> info.getActressName().equalsIgnoreCase(p.getName()))
                            .findFirst().orElse(null);

                    if (person != null) {
                        BaseItemDto personDto = itemRepository.getFullItemDetails(person.getId());
                        if (personDto != null && personDto.getTagItems() != null) {
                            tagsFromPerson.addAll(parseNameLongIdPair(personDto.getTagItems()));
                        }
                        personTag = Tag.parse(person.getName(), person.getId());
                    }
                }

                final Tag finalPersonTag = personTag;
                Platform.runLater(() -> {
                    if (info.getReleaseDate() != null) {
                        releaseDate.set(dateToString(info.getReleaseDate()));
                    }
                    if (!tagsFromPerson.isEmpty()) {
                        Set<Tag> existingTags = new HashSet<>(tagItems);
                        tagItems.addAll(tagsFromPerson.stream()
                                .filter(t -> !existingTags.contains(t))
                                .collect(Collectors.toList()));
                    }
                    if (finalPersonTag != null && !peopleItems.contains(finalPersonTag)) {
                        peopleItems.add(finalPersonTag);
                    }
                    notificationService.showStatus(configService.getString("itemDetailView", "statusFetchDateSuccess"));
                    loading.set(false);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    notificationService.showStatus(configService.getString("itemDetailView", "errorFetchDate", e.getMessage()));
                    loading.set(false);
                });
            }
        }).start();
    }

    @Override
    public void clonePropertiesCommand(String propertyType) {
        TreeItem<LibraryTreeItem> selectedFolder = libraryTreeViewModel.selectedTreeItemProperty().get();
        if (selectedFolder == null || selectedFolder.getValue() == null || selectedFolder.getValue().isLoadingNode()) {
            notificationService.showStatus(configService.getString("itemDetailView", "cloneErrorNoParent"));
            return;
        }
        if (currentItemId == null) {
            notificationService.showStatus(configService.getString("itemDetailView", "cloneErrorNoSource"));
            return;
        }

        final String targetParentId = selectedFolder.getValue().getItemDto().getId();
        final String sourceItemId = this.currentItemId;

        notificationService.showStatus(configService.getString("itemDetailView", "cloneStatusStart", propertyType));
        loading.set(true);

        new Thread(() -> {
            try {
                int count = 0;
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
                final int finalCount = count;
                Platform.runLater(() -> {
                    notificationService.showStatus(configService.getString("itemDetailView", "cloneStatusSuccess", propertyType, finalCount));
                    loading.set(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    notificationService.showStatus(configService.getString("itemDetailView", "cloneStatusError", e.getMessage()));
                    loading.set(false);
                });
            }
        }).start();
    }

    @Override
    public void openFileOrFolderCommand() {
        String path = itemPath.get();
        try {
            localInteractionService.openFileOrFolder(path);
        } catch (Exception e) {
            notificationService.showStatus(configService.getString("itemDetailView", "errorOpenPath", e.getMessage()));
        }
    }
    @Override
    public void openSubtitleCommand() {
        String path = itemPath.get();
        String title = titleProperty().get();
        try {
            File srtFile = localInteractionService.openSubtitleFile(path, title);
            localInteractionService.openContainingFolder(srtFile.getAbsolutePath());
            notificationService.showStatus(configService.getString("itemDetailView", "statusSubtitleCreated", srtFile.getName()));
        } catch (Exception e) {
            notificationService.showStatus(configService.getString("itemDetailView", "errorSubtitleOpen", e.getMessage()));
        }
    }
    @Override
    public void openScreenshotFolderCommand() {
        String path = itemPath.get();
        try {
            String mediaFileName = new File(path).getName();
            localInteractionService.openScreenshotFolder(mediaFileName);
        } catch (Exception e) {
            notificationService.showStatus(configService.getString("itemDetailView", "errorScreenshotPathOpen", e.getMessage()));
        }
    }

    @Override
    public void setDroppedPrimaryImage(File imageFile) {
        if (imageFile == null || currentItemId == null) return;
        newPrimaryImageFile.set(imageFile);
        try {
            Image localImage = new Image(imageFile.toURI().toString());
            primaryImage.set(localImage);
        } catch (Exception e) {
            notificationService.showStatus(configService.getString("itemDetailViewModel", "errorImagePreview"));
        }
    }
    @Override
    public void saveNewPrimaryImageCommand() {
        File fileToSave = newPrimaryImageFile.get();
        if (fileToSave == null || currentItemId == null) return;

        notificationService.showStatus(configService.getString("itemDetailViewModel", "statusUploadingPrimary"));
        loading.set(true);

        new Thread(() -> {
            try {
                itemUpdateService.uploadImage(currentItemId, ImageType.PRIMARY, fileToSave);
                Platform.runLater(() -> {
                    notificationService.showStatus(configService.getString("itemDetailViewModel", "statusUploadPrimarySuccess"));
                    newPrimaryImageFile.set(null);
                    loading.set(false);
                    loadItem(originalItemDto);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    notificationService.showStatus(configService.getString("itemDetailViewModel", "errorUploadPrimary", e.getMessage()));
                    loading.set(false);
                });
            }
        }).start();
    }
    @Override
    public void deleteBackdropCommand(ImageInfo backdrop) {
        if (backdrop == null || currentItemId == null) return;
        notificationService.showStatus(configService.getString("itemDetailViewModel", "statusDeletingBackdrop", backdrop.getImageIndex()));
        new Thread(() -> {
            try {
                itemUpdateService.deleteImage(currentItemId, backdrop.getImageType(), backdrop.getImageIndex());
                Platform.runLater(() -> {
                    notificationService.showStatus(configService.getString("itemDetailViewModel", "statusDeleteBackdropSuccess"));
                    backdropImages.remove(backdrop);
                });
            } catch (Exception e) {
                Platform.runLater(() -> notificationService.showStatus(configService.getString("itemDetailViewModel", "errorDeleteBackdrop", e.getMessage())));
            }
        }).start();
    }
    @Override
    public void uploadDroppedBackdropFiles(List<File> files) {
        if (files == null || files.isEmpty() || currentItemId == null) return;
        notificationService.showStatus(configService.getString("itemDetailViewModel", "statusUploadingBackdrops", files.size()));
        loading.set(true);

        new Thread(() -> {
            try {
                for (int i = 0; i < files.size(); i++) {
                    File file = files.get(i);
                    final int current = i + 1;
                    Platform.runLater(() -> notificationService.showStatus(
                            configService.getString("itemDetailViewModel", "statusUploadingBackdropProgress", current, files.size(), file.getName())
                    ));
                    itemUpdateService.uploadImage(currentItemId, ImageType.BACKDROP, file);
                }
                Platform.runLater(() -> {
                    notificationService.showStatus(configService.getString("itemDetailViewModel", "statusUploadBackdropSuccess", files.size()));
                    loading.set(false);
                    loadItem(originalItemDto);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    notificationService.showStatus(configService.getString("itemDetailViewModel", "errorUploadBackdrop", e.getMessage()));
                    loading.set(false);
                });
            }
        }).start();
    }

    // --- (SỬA ĐỔI GĐ 11 / UR-13) ---
    @Override public void addTagCommand() {
        this.lastAddContext = SuggestionContext.TAG; // Lưu context
        addChipCommand.set(SuggestionContext.TAG);
    }
    @Override public void addStudioCommand() {
        this.lastAddContext = SuggestionContext.STUDIO; // Lưu context
        addChipCommand.set(SuggestionContext.STUDIO);
    }
    @Override public void addGenreCommand() {
        this.lastAddContext = SuggestionContext.GENRE; // Lưu context
        addChipCommand.set(SuggestionContext.GENRE);
    }
    @Override public void addPeopleCommand() {
        this.lastAddContext = SuggestionContext.PEOPLE; // Lưu context
        addChipCommand.set(SuggestionContext.PEOPLE);
    }

    @Override public ReadOnlyObjectProperty<SuggestionContext> addChipCommandProperty() { return addChipCommand.getReadOnlyProperty(); }
    @Override public void clearAddChipCommand() { addChipCommand.set(null); }

    @Override public void removeTag(Tag tag) { tagItems.remove(tag); }
    @Override public void removeStudio(Tag tag) { studioItems.remove(tag); }
    @Override public void removeGenre(Tag tag) { genreItems.remove(tag); }
    @Override public void removePeople(Tag tag) { peopleItems.remove(tag); }

    /**
     * (MỚI - GĐ 12/UR-13)
     * Kích hoạt lại dialog "Add Chip" cuối cùng đã được sử dụng.
     */
    @Override
    public void repeatAddChipCommand() {
        if (currentItemId == null) {
            notificationService.showStatus(configService.getString("itemDetailView", "errorSave")); // "Lỗi: Không có item nào đang được chọn..."
            return;
        }
        // Kích hoạt sự kiện với context đã lưu
        addChipCommand.set(this.lastAddContext);
    }


    @Override
    public void processAddTagResult(AddTagResult result, SuggestionContext context) {
        if (result == null) return; // Người dùng hủy

        // (MỚI - GĐ 12/UR-13) Lưu lại context vừa dùng thành công
        this.lastAddContext = context;

        if (result.isTag()) {
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
            copyPropertiesFromItemCommand(result.getCopyId(), context);
        }
    }

    private void copyPropertiesFromItemCommand(String sourceItemId, SuggestionContext context) {
        if (currentItemId == null) {
            notificationService.showStatus(configService.getString("itemDetailViewModel", "errorSave"));
            return;
        }
        notificationService.showStatus(configService.getString("addTagDialog", "copyStatusLoading", sourceItemId));

        new Thread(() -> {
            try {
                BaseItemDto sourceDto = itemRepository.getFullItemDetails(sourceItemId);
                if (sourceDto == null) {
                    throw new Exception(configService.getString("addTagDialog", "copyErrorNotFound"));
                }

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

                final List<Tag> finalSourceTags = sourceTagsToCopy;
                Platform.runLater(() -> {
                    ObservableList<Tag> destinationList;
                    switch (context) {
                        case TAG: destinationList = tagItems; break;
                        case STUDIO: destinationList = studioItems; break;
                        case PEOPLE: destinationList = peopleItems; break;
                        case GENRE: destinationList = genreItems; break;
                        default: return;
                    }

                    Set<Tag> existingTags = new HashSet<>(destinationList);
                    int addedCount = 0;
                    for (Tag newTag : finalSourceTags) {
                        if (existingTags.add(newTag)) { // (HashSet.add trả về true nếu thêm mới)
                            destinationList.add(newTag);
                            addedCount++;
                        }
                    }
                    notificationService.showStatus(configService.getString("addTagDialog", "copySuccessStatus", addedCount, sourceItemId));
                });

            } catch (Exception e) {
                Platform.runLater(() -> notificationService.showStatus(configService.getString("addTagDialog", "copyErrorStatus", sourceItemId, e.getMessage())));
            }
        }).start();
    }


    @Override
    public void importAndPreview(File file) {
        if (originalItemDto == null) return;
        notificationService.showStatus(configService.getString("itemDetailView", "errorImportRead"));
        loading.set(true);

        new Thread(() -> {
            try {
                BaseItemDto importedDto = jsonFileHandler.readJsonFileToObject(file);
                if (importedDto != null) {
                    Platform.runLater(() -> {
                        importHandler.importAndPreview(importedDto);
                        loading.set(false);
                        notificationService.clearStatus();
                    });
                } else {
                    throw new Exception(configService.getString("itemDetailView", "errorImportInvalid"));
                }
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    notificationService.showStatus(configService.getString("itemDetailView", "errorImportReadThread", ex.getMessage()));
                    loading.set(false);
                });
            }
        }).start();
    }
    @Override
    public void exportCommand(File file) {
        if (originalItemDto == null) {
            notificationService.showStatus(configService.getString("itemDetailView", "errorExportNoData"));
            return;
        }
        final BaseItemDto dtoToExport = this.originalItemDto;

        new Thread(() -> {
            try {
                jsonFileHandler.writeObjectToJsonFile(dtoToExport, file);
                Platform.runLater(() -> notificationService.showStatus(
                        configService.getString("itemDetailView", "exportSuccess", file.getName())
                ));
            } catch (Exception ex) {
                Platform.runLater(() -> notificationService.showStatus(
                        configService.getString("itemDetailView", "errorExportWriteThread", ex.getMessage())
                ));
            }
        }).start();
    }

    @Override public void acceptImportField(String fieldName) { importHandler.acceptImportField(fieldName); }
    @Override public void rejectImportField(String fieldName) { importHandler.rejectImportField(fieldName); }
    @Override public void markAsDirtyByAccept() { dirtyTracker.forceDirty(); }
    @Override public String getExportFileName() {
        String name = originalTitle.get();
        if (name == null || name.trim().isEmpty()) {
            name = title.get();
        }
        if (name == null || name.trim().isEmpty()) {
            return "item";
        }
        return name;
    }

    @Override public void clearChipClickEvent() { chipClickEvent.set(null); }
    @Override public void fireChipClickEvent(Tag model, String type) { chipClickEvent.set(new ChipClickEvent(model, type)); }

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