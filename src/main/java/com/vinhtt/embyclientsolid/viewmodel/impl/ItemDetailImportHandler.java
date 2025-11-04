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
 * Lớp helper (trợ giúp) xử lý logic nghiệp vụ cho tính năng Import/Export JSON
 * (UR-45, 46, 47, 49).
 * Lớp này được ủy thác (delegate) bởi {@link ItemDetailViewModel}.
 * Nó chịu trách nhiệm:
 * 1. Lưu trạng thái UI (snapshot) trước khi import.
 * 2. Cập nhật UI với dữ liệu "preview" (xem trước) từ tệp JSON.
 * 3. Hiển thị/ẩn các nút Accept (✓) / Reject (✗) (UR-46).
 * 4. Theo dõi các trường đã được chấp nhận (✓) (UR-47).
 * 5. Khôi phục (revert) trạng thái UI khi nhấn (✗) (UR-47).
 */
public class ItemDetailImportHandler {

    private final IItemDetailViewModel viewModel;
    private final ItemDetailDirtyTracker dirtyTracker;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    /**
     * Lưu trữ "snapshot" (ảnh chụp) của các giá trị UI
     * ngay trước khi áp dụng dữ liệu import.
     * (Chỉ dùng khi nhấn Reject ✗).
     */
    private final Map<String, Object> preImportState = new HashMap<>();

    /**
     * DTO gốc (đã parse) từ tệp JSON.
     */
    private BaseItemDto importedDto = null;

    /**
     * Tập hợp (Set) chứa tên các trường đã được nhấn Accept (✓).
     * (UR-47).
     */
    private final Set<String> acceptedFields = new HashSet<>();

    /**
     * Các Properties (có thể quan sát) để BẬT/TẮT
     * các nút review (✓/✗) trên UI (UR-46).
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
     * Nhận DTO từ tệp JSON, cập nhật UI để hiển thị "preview" (xem trước),
     * và bật các nút (✓/✗) tương ứng (UR-45, UR-46).
     *
     * @param importedDto DTO đã được parse từ tệp JSON.
     */
    public void importAndPreview(BaseItemDto importedDto) {
        if (importedDto == null) return;

        this.importedDto = importedDto;
        dirtyTracker.startImport(); // Báo cho Tracker: Bắt đầu trạng thái import (vô hiệu hóa nút Save)

        try {
            clearStateExceptImportedDto(); // Xóa trạng thái import cũ (nếu có)
            hideAllReviewButtons(); // Ẩn tất cả nút (✓/✗)

            // 1. Title
            preImportState.put("title", viewModel.titleProperty().get()); // Lưu snapshot
            viewModel.titleProperty().set(importedDto.getName() != null ? importedDto.getName() : ""); // Cập nhật UI
            showTitleReview.set(true); // Hiển thị (✓/✗)

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
            preImportState.put("tags", new ArrayList<>(viewModel.getTagItems())); // Lưu snapshot (List)
            List<Tag> importedTags = new ArrayList<>();
            if (importedDto.getTagItems() != null) {
                for (NameLongIdPair tagPair : importedDto.getTagItems()) {
                    if (tagPair.getName() != null) {
                        // Chuyển đổi DTO (NameLongIdPair) sang Model (Tag)
                        importedTags.add(Tag.parse(tagPair.getName(), tagPair.getId() != null ? tagPair.getId().toString() : null));
                    }
                }
            }
            viewModel.getTagItems().setAll(importedTags); // Cập nhật UI
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

            // 9. Genres
            preImportState.put("genres", new ArrayList<>(viewModel.getGenreItems()));
            List<Tag> importedGenres = new ArrayList<>();
            if (importedDto.getGenres() != null) {
                // Genres trong DTO là List<String>, không phải NameLongIdPair
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
     * Xử lý hành động nhấn nút (✓) - Chấp nhận thay đổi (UR-47).
     *
     * @param fieldName Tên trường được chấp nhận (ví dụ: "title").
     */
    public void acceptImportField(String fieldName) {
        hideReviewButton(fieldName); // Ẩn nút (✓/✗)
        acceptedFields.add(fieldName); // Thêm vào danh sách chờ lưu
        // Báo cho ViewModel (ViewModel sẽ gọi forceDirty của Tracker
        // để kích hoạt nút Save (UR-48))
        viewModel.markAsDirtyByAccept();
    }

    /**
     * Xử lý hành động nhấn nút (✗) - Hủy bỏ thay đổi (UR-47).
     *
     * @param fieldName Tên trường bị từ chối (ví dụ: "title").
     */
    @SuppressWarnings("unchecked")
    public void rejectImportField(String fieldName) {
        acceptedFields.remove(fieldName); // Xóa khỏi danh sách chờ lưu
        dirtyTracker.pauseTracking(); // Tạm dừng tracker khi khôi phục UI

        try {
            // Khôi phục giá trị UI từ "snapshot" (preImportState)
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
                    // Khôi phục toàn bộ danh sách (List)
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
            hideReviewButton(fieldName); // Ẩn nút (✓/✗)
        } finally {
            dirtyTracker.resumeTracking(); // Bật lại tracker
        }
    }

    /**
     * Helper Ẩn một cặp nút (✓/✗) cụ thể.
     */
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

    /**
     * Ẩn TẤT CẢ các cặp nút (✓/✗).
     */
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
     * Xóa toàn bộ trạng thái import (snapshot, DTO, accepted fields).
     * Được gọi khi clear item hoặc sau khi lưu thành công.
     */
    public void clearState() {
        preImportState.clear();
        hideAllReviewButtons();
        importedDto = null;
        acceptedFields.clear();
    }

    /**
     * Xóa trạng thái, nhưng giữ lại importedDto (dùng khi bắt đầu import mới).
     */
    private void clearStateExceptImportedDto() {
        preImportState.clear();
        hideAllReviewButtons();
        acceptedFields.clear();
    }

    /**
     * Helper chuyển đổi OffsetDateTime sang chuỗi "dd/MM/yyyy".
     */
    private String dateToString(OffsetDateTime date) {
        if (date == null) return "";
        try {
            return dateFormat.format(Date.from(date.toInstant()));
        } catch (Exception e) { return ""; }
    }

    // --- Getters cho ViewModel ---

    /**
     * Kiểm tra xem có đang trong quá trình import hay không.
     */
    public boolean wasImportInProgress() {
        return importedDto != null;
    }

    /**
     * Lấy danh sách các trường đã nhấn Accept (✓).
     */
    public Set<String> getAcceptedFields() {
        return acceptedFields;
    }

    /**
     * Lấy DTO gốc từ tệp JSON.
     */
    public BaseItemDto getImportedDto() {
        return importedDto;
    }

    // --- Getters cho các BooleanProperty (hiển thị ✓/✗) ---
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