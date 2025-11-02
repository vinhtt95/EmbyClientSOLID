package com.vinhtt.embyclientsolid.viewmodel;

import com.vinhtt.embyclientsolid.model.Tag;
import embyclient.model.BaseItemDto;
import embyclient.model.ImageInfo;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;

import java.io.File;
import java.util.List;

/**
 * Interface cho ItemDetailViewModel (Cột 3).
 * (Cập nhật GĐ 10: Thêm các hàm/props cho Import/Export).
 */
public interface IItemDetailViewModel {

    // --- Trạng thái (Binding) ---
    ReadOnlyBooleanProperty loadingProperty();
    ReadOnlyStringProperty statusMessageProperty();
    ReadOnlyBooleanProperty showStatusMessageProperty();
    StringProperty titleProperty();
    StringProperty originalTitleProperty();
    ObjectProperty<Float> criticRatingProperty();
    StringProperty overviewProperty();
    StringProperty releaseDateProperty();
    ReadOnlyStringProperty itemPathProperty();
    ReadOnlyBooleanProperty isFolderProperty();
    ReadOnlyObjectProperty<Image> primaryImageProperty();
    ObservableList<ImageInfo> getBackdropImages();
    ObservableList<Tag> getTagItems();
    ObservableList<Tag> getStudioItems();
    ObservableList<Tag> getPeopleItems();
    ObservableList<Tag> getGenreItems();

    // --- Trạng thái Dirty/Import (Binding) ---
    ReadOnlyBooleanProperty isDirtyProperty();
    ReadOnlyBooleanProperty primaryImageDirtyProperty();
    /**
     * (MỚI) Các property để hiển thị nút (✓/✗) (UR-46).
     */
    ReadOnlyBooleanProperty showTitleReviewProperty();
    ReadOnlyBooleanProperty showOverviewReviewProperty();
    ReadOnlyBooleanProperty showReleaseDateReviewProperty();
    ReadOnlyBooleanProperty showOriginalTitleReviewProperty();
    ReadOnlyBooleanProperty showStudiosReviewProperty();
    ReadOnlyBooleanProperty showPeopleReviewProperty();
    ReadOnlyBooleanProperty showGenresReviewProperty();
    ReadOnlyBooleanProperty showTagsReviewProperty();
    ReadOnlyBooleanProperty showCriticRatingReviewProperty();

    // --- Sự kiện (Event) ---
    ReadOnlyObjectProperty<ChipClickEvent> chipClickEventProperty();
    void clearChipClickEvent();
    void fireChipClickEvent(Tag model, String type);
    String getBackdropUrl(ImageInfo info);

    // --- Hành động (Commands) ---
    void loadItem(BaseItemDto item);
    void saveChangesCommand();
    void saveCriticRatingImmediately(Float newRating);
    void fetchReleaseDateCommand();
    void clonePropertiesCommand(String propertyType);

    // Commands Tương tác Local
    void openFileOrFolderCommand();
    void openSubtitleCommand();
    void openScreenshotFolderCommand();

    // Commands Ảnh
    void setDroppedPrimaryImage(File imageFile);
    void saveNewPrimaryImageCommand();
    void deleteBackdropCommand(ImageInfo imageInfo);
    void uploadDroppedBackdropFiles(List<File> files);

    // Commands Chip
    void addTagCommand();
    void addStudioCommand();
    void addGenreCommand();
    void addPeopleCommand();
    void removeTag(Tag tag);
    void removeStudio(Tag tag);
    void removeGenre(Tag tag);
    void removePeople(Tag tag);

    // --- (MỚI) Commands Import/Export (UR-44, 45, 47) ---
    void importAndPreview(File file);
    void exportCommand(File file);
    void acceptImportField(String fieldName);
    void rejectImportField(String fieldName);
    /**
     * (MỚI) Được gọi bởi ImportHandler khi nhấn Accept (✓).
     * (UR-47).
     */
    void markAsDirtyByAccept();

    /**
     * Lớp POJO cho sự kiện click chip (UR-36).
     */
    class ChipClickEvent {
        public final Tag model;
        public final String type; // "TAG", "STUDIO", "PEOPLE", "GENRE"
        public ChipClickEvent(Tag model, String type) {
            this.model = model;
            this.type = type;
        }
    }
}