package com.vinhtt.embyclientsolid.viewmodel.impl;

import com.vinhtt.embyclientsolid.model.Tag;
import com.vinhtt.embyclientsolid.viewmodel.IItemDetailViewModel;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Helper class để theo dõi trạng thái "dirty" (đã thay đổi) của ItemDetailViewModel.
 * Hỗ trợ logic phức tạp của Import/Export (UR-48, UR-49).
 * Logic được port từ ItemDetailDirtyTracker.java (cũ).
 */
public class ItemDetailDirtyTracker {

    private final IItemDetailViewModel viewModel;
    private final BooleanProperty isDirty = new SimpleBooleanProperty(false);

    /**
     * Snapshot của dữ liệu gốc khi item được tải.
     */
    private String originalTitle, originalOverview, originalReleaseDate, originalOriginalTitle;
    private Float originalCriticRating;
    private List<Tag> originalTagItems;
    private List<Tag> originalStudioItems;
    private List<Tag> originalPeopleItems;
    private List<Tag> originalGenreItems;

    /**
     * Listeners để theo dõi thay đổi.
     */
    private final ChangeListener<String> stringListener = (obs, oldVal, newVal) -> checkForChanges();
    private final ChangeListener<Number> ratingListener = (obs, oldVal, newVal) -> checkForChanges();
    private final ListChangeListener<Tag> tagsListener = (c) -> checkForChanges();
    private final ListChangeListener<Tag> studioItemsListener = (c) -> checkForChanges();
    private final ListChangeListener<Tag> peopleItemsListener = (c) -> checkForChanges();
    private final ListChangeListener<Tag> genreItemsListener = (c) -> checkForChanges();

    /**
     * Cờ trạng thái.
     */
    private boolean paused = false;
    private boolean importAcceptancePending = false;

    public ItemDetailDirtyTracker(IItemDetailViewModel viewModel) {
        this.viewModel = viewModel;
    }

    /**
     * Bắt đầu theo dõi (sau khi tải item).
     */
    public void startTracking() {
        updateOriginalsFromCurrent(); // Lấy snapshot
        addListeners();
        paused = false;
        importAcceptancePending = false;
        isDirty.set(false);
    }

    /**
     * Dừng theo dõi (khi clear item).
     */
    public void stopTracking() {
        removeListeners();
        clearOriginals();
        paused = false;
        importAcceptancePending = false;
        isDirty.set(false);
    }

    /**
     * Được gọi bởi ImportHandler TRƯỚC KHI cập nhật UI.
     * (UR-46).
     */
    public void startImport() {
        if (!importAcceptancePending) {
            importAcceptancePending = true;
            isDirty.set(false); // Vô hiệu hóa nút Save
            pauseTracking(); // Dừng listener
        }
    }

    /**
     * Được gọi bởi ImportHandler SAU KHI cập nhật UI.
     */
    public void endImport() {
        if (importAcceptancePending) {
            resumeTracking(); // Bật lại listener (sẽ tự gọi checkForChanges)
        }
    }

    /**
     * Tạm dừng theo dõi.
     */
    public void pauseTracking() {
        if (!paused) {
            removeListeners();
            paused = true;
        }
    }

    /**
     * Tiếp tục theo dõi.
     */
    public void resumeTracking() {
        if (paused) {
            addListeners();
            paused = false;
            checkForChanges(); // Kiểm tra ngay
        }
    }

    private void addListeners() {
        viewModel.titleProperty().addListener(stringListener);
        // (SỬA LỖI: Không lắng nghe rating, vì nó tự lưu (UR-33))
        // viewModel.criticRatingProperty().addListener(ratingListener);
        viewModel.overviewProperty().addListener(stringListener);
        viewModel.releaseDateProperty().addListener(stringListener);
        viewModel.originalTitleProperty().addListener(stringListener);
        viewModel.getTagItems().addListener(tagsListener);
        viewModel.getStudioItems().addListener(studioItemsListener);
        viewModel.getPeopleItems().addListener(peopleItemsListener);
        viewModel.getGenreItems().addListener(genreItemsListener);
    }

    private void removeListeners() {
        viewModel.titleProperty().removeListener(stringListener);
        // viewModel.criticRatingProperty().removeListener(ratingListener);
        viewModel.overviewProperty().removeListener(stringListener);
        viewModel.releaseDateProperty().removeListener(stringListener);
        viewModel.originalTitleProperty().removeListener(stringListener);
        viewModel.getTagItems().removeListener(tagsListener);
        viewModel.getStudioItems().removeListener(studioItemsListener);
        viewModel.getPeopleItems().removeListener(peopleItemsListener);
        viewModel.getGenreItems().removeListener(genreItemsListener);
    }

    /**
     * Kiểm tra xem UI có khác gì so với snapshot gốc không.
     * (UR-48).
     */
    private void checkForChanges() {
        if (paused) return;

        if (originalTitle == null && originalTagItems == null) {
            isDirty.set(false); // Chưa start
            return;
        }

        boolean stringChanges = !Objects.equals(viewModel.titleProperty().get(), originalTitle) ||
                !Objects.equals(viewModel.overviewProperty().get(), originalOverview) ||
                !Objects.equals(viewModel.releaseDateProperty().get(), originalReleaseDate) ||
                !Objects.equals(viewModel.originalTitleProperty().get(), originalOriginalTitle);

        // (SỬA LỖI: UR-33 (Rating) không kích hoạt nút Save (UR-48))
        boolean ratingChanges = false;

        // (SỬA LỖI: So sánh nội dung List, không phải bản thân List)
        boolean tagChanges = !Objects.equals(new ArrayList<>(viewModel.getTagItems()), originalTagItems);
        boolean studioChanges = !Objects.equals(new ArrayList<>(viewModel.getStudioItems()), originalStudioItems);
        boolean peopleChanges = !Objects.equals(new ArrayList<>(viewModel.getPeopleItems()), originalPeopleItems);
        boolean genreChanges = !Objects.equals(new ArrayList<>(viewModel.getGenreItems()), originalGenreItems);

        boolean changes = stringChanges || ratingChanges || tagChanges || studioChanges || peopleChanges || genreChanges;

        // (SỬA LỖI (UR-48): Chuyển check này xuống cuối)
        if (importAcceptancePending) {
            isDirty.set(false);
        } else {
            isDirty.set(changes);
        }
    }

    /**
     * Kích hoạt 'isDirty' (thường do nhấn Accept ✓ hoặc sửa thủ công).
     * (UR-47, UR-48).
     */
    public void forceDirty() {
        if (paused) return;

        if (importAcceptancePending) {
            importAcceptancePending = false;
            isDirty.set(true);
        } else if (!isDirty.get()) {
            isDirty.set(true);
        }
    }

    /**
     * Cập nhật snapshot gốc (sau khi Tải hoặc Lưu thành công).
     */
    public void updateOriginalsFromCurrent() {
        this.originalTitle = viewModel.titleProperty().get();
        this.originalOriginalTitle = viewModel.originalTitleProperty().get();
        this.originalOverview = viewModel.overviewProperty().get();
        this.originalReleaseDate = viewModel.releaseDateProperty().get();
        this.originalCriticRating = viewModel.criticRatingProperty().get();
        this.originalTagItems = new ArrayList<>(viewModel.getTagItems());
        this.originalStudioItems = new ArrayList<>(viewModel.getStudioItems());
        this.originalPeopleItems = new ArrayList<>(viewModel.getPeopleItems());
        this.originalGenreItems = new ArrayList<>(viewModel.getGenreItems());
        importAcceptancePending = false;
        checkForChanges();
    }

    /**
     * Cập nhật snapshot chỉ cho rating (dùng cho UR-33).
     */
    public void updateOriginalRating(Float newRating) {
        if (paused) return;
        this.originalCriticRating = newRating;
        if (importAcceptancePending) {
            // Nếu người dùng đang import VÀ nhấn nút rating,
            // chúng ta coi như họ đã chấp nhận (UR-47)
            importAcceptancePending = false;
        }
        checkForChanges();
    }

    private void clearOriginals() {
        this.originalTitle = null;
        this.originalOriginalTitle = null;
        this.originalOverview = null;
        this.originalReleaseDate = null;
        this.originalCriticRating = null;
        this.originalTagItems = null;
        this.originalStudioItems = null;
        this.originalPeopleItems = null;
        this.originalGenreItems = null;
    }

    public BooleanProperty isDirtyProperty() {
        return isDirty;
    }
}