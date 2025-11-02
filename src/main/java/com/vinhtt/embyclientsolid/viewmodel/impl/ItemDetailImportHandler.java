package com.vinhtt.embyclientsolid.viewmodel.impl;

import com.vinhtt.embyclientsolid.model.Tag;
import com.vinhtt.embyclientsolid.viewmodel.IItemDetailViewModel;
import embyclient.model.BaseItemDto;
import embyclient.model.BaseItemPerson;
import embyclient.model.NameLongIdPair;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;

import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Helper class xử lý logic Import/Export (UR-45, 46, 47, 49).
 * Logic được port từ ItemDetailImportHandler.java (cũ)
 * và cập nhật để dùng 'Tag' model thay vì 'TagModel'.
 */
public class ItemDetailImportHandler {

    private final IItemDetailViewModel viewModel;
    private final ItemDetailDirtyTracker dirtyTracker;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    /**
     * Lưu trạng thái của UI ngay trước khi import.
     */
    private final Map<String, Object> preImportState = new HashMap<>();

    /**
     * DTO gốc từ file JSON.
     */
    private BaseItemDto importedDto = null;
    /**
     * Tên các trường đã nhấn (✓).
     */
    private final Set<String> acceptedFields = new HashSet<>();

    /**
     * Properties để BẬT/TẮT các nút review (✓/✗).
     * (UR-46).
     */
    private final ReadOnlyBooleanWrapper showTitleReview = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper showOverviewReview = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper showReleaseDateReview = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper showOriginalTitleReview = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper showStudiosReview = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper showPeopleReview = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper showGenresReview = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper showCriticRatingReview = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper showTagsReview = new ReadOnlyBooleanWrapper(false);

    public ItemDetailImportHandler(IItemDetailViewModel viewModel, ItemDetailDirtyTracker dirtyTracker) {
        this.viewModel = viewModel;
        this.dirtyTracker = dirtyTracker;
    }

    /**
     * Nhận DTO, cập nhật UI, báo cho DirtyTracker.
     * (UR-45, UR-46).
     */
    public void importAndPreview(BaseItemDto importedDto) {
        if (importedDto == null) return;

        this.importedDto = importedDto;
        dirtyTracker.startImport(); // Báo cho Tracker: Bắt đầu trạng thái import

        try {
            clearStateExceptImportedDto(); // Xóa state cũ
            hideAllReviewButtons();

            // 1. Title
            preImportState.put("title", viewModel.titleProperty().get());
            viewModel.titleProperty().set(importedDto.getName() != null ? importedDto.getName() : "");
            showTitleReview.set(true);

            // 2. Critic Rating
            preImportState.put("criticRating", viewModel.criticRatingProperty().get());
            viewModel.criticRatingProperty().set(importedDto.getCriticRating());
            showCriticRatingReview.set(true);

            // 3. Overview
            preImportState.put("overview", viewModel.overviewProperty().get());
            viewModel.overviewProperty().set(importedDto.getOverview() != null ? importedDto.getOverview() : "");
            showOverviewReview.set(true);

            // 4. Original Title
            preImportState.put("originalTitle", viewModel.originalTitleProperty().get());
            viewModel.originalTitleProperty().set(importedDto.getOriginalTitle() != null ? importedDto.getOriginalTitle() : "");
            showOriginalTitleReview.set(true);

            // 5. Tags
            preImportState.put("tags", new ArrayList<>(viewModel.getTagItems()));
            List<Tag> importedTags = new ArrayList<>();
            if (importedDto.getTagItems() != null) {
                for (NameLongIdPair tagPair : importedDto.getTagItems()) {
                    if (tagPair.getName() != null) {
                        // Dùng Tag model mới
                        importedTags.add(Tag.parse(tagPair.getName(), tagPair.getId() != null ? tagPair.getId().toString() : null));
                    }
                }
            }
            viewModel.getTagItems().setAll(importedTags);
            showTagsReview.set(true);

            // 6. Release Date
            preImportState.put("releaseDate", viewModel.releaseDateProperty().get());
            viewModel.releaseDateProperty().set(dateToString(importedDto.getPremiereDate()));
            showReleaseDateReview.set(true);

            // 7. Studios
            preImportState.put("studios", new ArrayList<>(viewModel.getStudioItems()));
            List<Tag> importedStudios = new ArrayList<>();
            if (importedDto.getStudios() != null) {
                importedStudios = importedDto.getStudios().stream()
                        .map(pair -> Tag.parse(pair.getName(), pair.getId() != null ? pair.getId().toString() : null))
                        .collect(Collectors.toList());
            }
            viewModel.getStudioItems().setAll(importedStudios);
            showStudiosReview.set(true);

            // 8. People
            preImportState.put("people", new ArrayList<>(viewModel.getPeopleItems()));
            List<Tag> importedPeople = new ArrayList<>();
            if (importedDto.getPeople() != null) {
                importedPeople = importedDto.getPeople().stream()
                        .map(person -> Tag.parse(person.getName(), person.getId()))
                        .collect(Collectors.toList());
            }
            viewModel.getPeopleItems().setAll(importedPeople);
            showPeopleReview.set(true);

            // 9. Genres (Dùng 'Genres' thay vì 'GenreItems' để tương thích JSON)
            preImportState.put("genres", new ArrayList<>(viewModel.getGenreItems()));
            List<Tag> importedGenres = new ArrayList<>();
            if (importedDto.getGenres() != null) {
                importedGenres = importedDto.getGenres().stream()
                        .filter(Objects::nonNull)
                        .map(name -> Tag.parse(name, null)) // Genres không có ID
                        .collect(Collectors.toList());
            }
            viewModel.getGenreItems().setAll(importedGenres);
            showGenresReview.set(true);

        } finally {
            dirtyTracker.endImport(); // Báo cho Tracker: Kết thúc cập nhật UI
        }
    }

    /**
     * Nhấn (v) - Chấp nhận.
     * (UR-47).
     */
    public void acceptImportField(String fieldName) {
        hideReviewButton(fieldName);
        acceptedFields.add(fieldName);
        // Báo cho ViewModel (ViewModel sẽ gọi forceDirty của Tracker)
        viewModel.markAsDirtyByAccept();
    }

    /**
     * Nhấn (x) - Hủy bỏ.
     * (UR-47).
     */
    @SuppressWarnings("unchecked")
    public void rejectImportField(String fieldName) {
        acceptedFields.remove(fieldName);
        dirtyTracker.pauseTracking(); // Tạm dừng tracker khi revert UI

        try {
            // Khôi phục giá trị UI từ snapshot
            switch (fieldName) {
                case "title":
                    viewModel.titleProperty().set((String) preImportState.get("title"));
                    break;
                case "criticRating":
                    viewModel.criticRatingProperty().set((Float) preImportState.get("criticRating"));
                    break;
                case "overview":
                    viewModel.overviewProperty().set((String) preImportState.get("overview"));
                    break;
                case "originalTitle":
                    viewModel.originalTitleProperty().set((String) preImportState.get("originalTitle"));
                    break;
                case "tags":
                    viewModel.getTagItems().setAll((List<Tag>) preImportState.get("tags"));
                    break;
                case "releaseDate":
                    viewModel.releaseDateProperty().set((String) preImportState.get("releaseDate"));
                    break;
                case "studios":
                    viewModel.getStudioItems().setAll((List<Tag>) preImportState.get("studios"));
                    break;
                case "people":
                    viewModel.getPeopleItems().setAll((List<Tag>) preImportState.get("people"));
                    break;
                case "genres":
                    viewModel.getGenreItems().setAll((List<Tag>) preImportState.get("genres"));
                    break;
            }
            hideReviewButton(fieldName);
        } finally {
            dirtyTracker.resumeTracking(); // Bật lại tracker
        }
    }

    private void hideReviewButton(String fieldName) {
        switch (fieldName) {
            case "title": showTitleReview.set(false); break;
            case "criticRating": showCriticRatingReview.set(false); break;
            case "overview": showOverviewReview.set(false); break;
            case "originalTitle": showOriginalTitleReview.set(false); break;
            case "tags": showTagsReview.set(false); break;
            case "releaseDate": showReleaseDateReview.set(false); break;
            case "studios": showStudiosReview.set(false); break;
            case "people": showPeopleReview.set(false); break;
            case "genres": showGenresReview.set(false); break;
        }
    }

    public void hideAllReviewButtons() {
        showTitleReview.set(false);
        showCriticRatingReview.set(false);
        showOverviewReview.set(false);
        showReleaseDateReview.set(false);
        showOriginalTitleReview.set(false);
        showStudiosReview.set(false);
        showPeopleReview.set(false);
        showGenresReview.set(false);
        showTagsReview.set(false);
    }

    /**
     * Xóa state, reset cờ import, xóa accepted fields.
     */
    public void clearState() {
        preImportState.clear();
        hideAllReviewButtons();
        importedDto = null;
        acceptedFields.clear();
    }

    private void clearStateExceptImportedDto() {
        preImportState.clear();
        hideAllReviewButtons();
        acceptedFields.clear();
    }

    private String dateToString(OffsetDateTime date) {
        if (date == null) return "";
        try {
            return dateFormat.format(Date.from(date.toInstant()));
        } catch (Exception e) { return ""; }
    }

    // --- Getters cho ViewModel ---
    public boolean wasImportInProgress() {
        return importedDto != null;
    }

    public Set<String> getAcceptedFields() {
        return acceptedFields;
    }

    public BaseItemDto getImportedDto() {
        return importedDto;
    }

    // --- Getters cho các BooleanProperty (v/x) ---
    public ReadOnlyBooleanProperty showTitleReviewProperty() { return showTitleReview.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty showCriticRatingReviewProperty() { return showCriticRatingReview.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty showOverviewReviewProperty() { return showOverviewReview.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty showReleaseDateReviewProperty() { return showReleaseDateReview.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty showOriginalTitleReviewProperty() { return showOriginalTitleReview.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty showStudiosReviewProperty() { return showStudiosReview.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty showPeopleReviewProperty() { return showPeopleReview.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty showGenresReviewProperty() { return showGenresReview.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty showTagsReviewProperty() { return showTagsReview.getReadOnlyProperty(); }
}