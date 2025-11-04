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
 * Lớp helper (trợ giúp) để theo dõi trạng thái "dirty" (đã thay đổi) của
 * {@link ItemDetailViewModel}.
 * Lớp này chụp một "snapshot" (ảnh chụp) dữ liệu gốc khi item được tải
 * và so sánh nó với dữ liệu UI hiện tại để xác định xem nút "Lưu" (Save)
 * có nên được kích hoạt hay không (UR-48).
 * Nó cũng quản lý logic trạng thái phức tạp khi import (UR-49).
 */
public class ItemDetailDirtyTracker {

    private final IItemDetailViewModel viewModel;
    private final BooleanProperty isDirty = new SimpleBooleanProperty(false);

    // --- Snapshot (Ảnh chụp) ---
    // Lưu trữ trạng thái của item ngay sau khi tải xong
    private String originalTitle, originalOverview, originalReleaseDate, originalOriginalTitle;
    private Float originalCriticRating;
    private List<Tag> originalTagItems;
    private List<Tag> originalStudioItems;
    private List<Tag> originalPeopleItems;
    private List<Tag> originalGenreItems;

    // --- Listeners ---
    // Các listener này sẽ được gắn vào các Property của ViewModel
    // để gọi `checkForChanges()` mỗi khi có thay đổi
    private final ChangeListener<String> stringListener = (obs, oldVal, newVal) -> checkForChanges();
    private final ChangeListener<Number> ratingListener = (obs, oldVal, newVal) -> checkForChanges();
    private final ListChangeListener<Tag> tagsListener = (c) -> checkForChanges();
    private final ListChangeListener<Tag> studioItemsListener = (c) -> checkForChanges();
    private final ListChangeListener<Tag> peopleItemsListener = (c) -> checkForChanges();
    private final ListChangeListener<Tag> genreItemsListener = (c) -> checkForChanges();

    // --- Cờ trạng thái ---
    // Cờ `paused`: Dừng theo dõi (ví dụ: khi đang import JSON)
    private boolean paused = false;
    // Cờ `importAcceptancePending`: Đang ở trạng thái chờ chấp nhận (✓)
    // Nút "Lưu" sẽ bị vô hiệu hóa ngay cả khi có thay đổi (UR-48)
    private boolean importAcceptancePending = false;

    public ItemDetailDirtyTracker(IItemDetailViewModel viewModel) {
        this.viewModel = viewModel;
    }

    /**
     * Bắt đầu theo dõi thay đổi.
     * Được gọi bởi ViewModel sau khi tải xong item.
     */
    public void startTracking() {
        updateOriginalsFromCurrent(); // Chụp snapshot
        addListeners(); // Gắn listener
        paused = false;
        importAcceptancePending = false;
        isDirty.set(false); // Reset trạng thái
    }

    /**
     * Dừng theo dõi thay đổi.
     * Được gọi bởi ViewModel khi xóa chi tiết (clear) item.
     */
    public void stopTracking() {
        removeListeners();
        clearOriginals();
        paused = false;
        importAcceptancePending = false;
        isDirty.set(false);
    }

    /**
     * Được gọi bởi ImportHandler TRƯỚC KHI cập nhật UI (UR-46).
     * Đặt trạng thái "chờ chấp nhận" (✓) và vô hiệu hóa nút Lưu.
     */
    public void startImport() {
        if (!importAcceptancePending) {
            importAcceptancePending = true;
            isDirty.set(false); // Vô hiệu hóa nút Save
            pauseTracking(); // Tạm dừng listener
        }
    }

    /**
     * Được gọi bởi ImportHandler SAU KHI cập nhật UI.
     */
    public void endImport() {
        if (importAcceptancePending) {
            resumeTracking(); // Bật lại listener (sẽ tự động gọi checkForChanges)
        }
    }

    /**
     * Tạm dừng theo dõi (xóa listener).
     */
    public void pauseTracking() {
        if (!paused) {
            removeListeners();
            paused = true;
        }
    }

    /**
     * Tiếp tục theo dõi (gắn lại listener).
     */
    public void resumeTracking() {
        if (paused) {
            addListeners();
            paused = false;
            checkForChanges(); // Kiểm tra ngay lập tức
        }
    }

    /**
     * Gắn listener vào tất cả các Property của ViewModel.
     */
    private void addListeners() {
        viewModel.titleProperty().addListener(stringListener);
        // (UR-33) KHÔNG lắng nghe CriticRating, vì nó tự lưu
        // và không kích hoạt trạng thái "dirty" của nút Save chính (UR-48).
        viewModel.overviewProperty().addListener(stringListener);
        viewModel.releaseDateProperty().addListener(stringListener);
        viewModel.originalTitleProperty().addListener(stringListener);
        viewModel.getTagItems().addListener(tagsListener);
        viewModel.getStudioItems().addListener(studioItemsListener);
        viewModel.getPeopleItems().addListener(peopleItemsListener);
        viewModel.getGenreItems().addListener(genreItemsListener);
    }

    /**
     * Xóa tất cả listener.
     */
    private void removeListeners() {
        viewModel.titleProperty().removeListener(stringListener);
        viewModel.overviewProperty().removeListener(stringListener);
        viewModel.releaseDateProperty().removeListener(stringListener);
        viewModel.originalTitleProperty().removeListener(stringListener);
        viewModel.getTagItems().removeListener(tagsListener);
        viewModel.getStudioItems().removeListener(studioItemsListener);
        viewModel.getPeopleItems().removeListener(peopleItemsListener);
        viewModel.getGenreItems().removeListener(genreItemsListener);
    }

    /**
     * Logic cốt lõi: So sánh trạng thái UI hiện tại với "snapshot" gốc
     * để xác định xem có thay đổi hay không (UR-48).
     */
    private void checkForChanges() {
        if (paused) return; // Không kiểm tra nếu đang tạm dừng

        if (originalTitle == null && originalTagItems == null) {
            isDirty.set(false); // Chưa bắt đầu theo dõi
            return;
        }

        // So sánh các trường String
        boolean stringChanges = !Objects.equals(viewModel.titleProperty().get(), originalTitle) ||
                !Objects.equals(viewModel.overviewProperty().get(), originalOverview) ||
                !Objects.equals(viewModel.releaseDateProperty().get(), originalReleaseDate) ||
                !Objects.equals(viewModel.originalTitleProperty().get(), originalOriginalTitle);

        // So sánh nội dung của các danh sách (List)
        // (Phải tạo new ArrayList để so sánh nội dung, không phải tham chiếu)
        boolean tagChanges = !Objects.equals(new ArrayList<>(viewModel.getTagItems()), originalTagItems);
        boolean studioChanges = !Objects.equals(new ArrayList<>(viewModel.getStudioItems()), originalStudioItems);
        boolean peopleChanges = !Objects.equals(new ArrayList<>(viewModel.getPeopleItems()), originalPeopleItems);
        boolean genreChanges = !Objects.equals(new ArrayList<>(viewModel.getGenreItems()), originalGenreItems);

        boolean changes = stringChanges || tagChanges || studioChanges || peopleChanges || genreChanges;

        // (UR-48) Nếu đang ở chế độ import (chờ chấp nhận ✓),
        // nút Save VẪN BỊ vô hiệu hóa, ngay cả khi có thay đổi.
        if (importAcceptancePending) {
            isDirty.set(false);
        } else {
            isDirty.set(changes);
        }
    }

    /**
     * Kích hoạt 'isDirty' (bật nút Save) một cách cưỡng bức.
     * Được gọi khi người dùng nhấn Accept (✓) (UR-47, UR-48)
     * hoặc khi người dùng bắt đầu chỉnh sửa thủ công.
     */
    public void forceDirty() {
        if (paused) return;

        if (importAcceptancePending) {
            // Nếu đang import, việc nhấn (✓) sẽ thoát khỏi chế độ import
            // và kích hoạt nút Save.
            importAcceptancePending = false;
            isDirty.set(true);
        } else if (!isDirty.get()) {
            // Nếu không import, chỉ cần kích hoạt nút Save
            isDirty.set(true);
        }
    }

    /**
     * Cập nhật "snapshot" gốc bằng dữ liệu UI hiện tại.
     * Được gọi sau khi Tải item thành công hoặc Lưu item thành công.
     */
    public void updateOriginalsFromCurrent() {
        this.originalTitle = viewModel.titleProperty().get();
        this.originalOriginalTitle = viewModel.originalTitleProperty().get();
        this.originalOverview = viewModel.overviewProperty().get();
        this.originalReleaseDate = viewModel.releaseDateProperty().get();
        this.originalCriticRating = viewModel.criticRatingProperty().get();
        // (Phải tạo new ArrayList để sao chép giá trị, không phải tham chiếu)
        this.originalTagItems = new ArrayList<>(viewModel.getTagItems());
        this.originalStudioItems = new ArrayList<>(viewModel.getStudioItems());
        this.originalPeopleItems = new ArrayList<>(viewModel.getPeopleItems());
        this.originalGenreItems = new ArrayList<>(viewModel.getGenreItems());

        importAcceptancePending = false; // Reset cờ import
        checkForChanges(); // Kiểm tra lại (thường sẽ set isDirty=false)
    }

    /**
     * Cập nhật "snapshot" chỉ cho trường rating (dùng cho UR-33).
     *
     * @param newRating Điểm số mới.
     */
    public void updateOriginalRating(Float newRating) {
        if (paused) return;
        this.originalCriticRating = newRating;
        if (importAcceptancePending) {
            // (UR-47) Nếu người dùng đang import VÀ nhấn nút rating,
            // coi như họ đã chấp nhận (✓) thay đổi.
            importAcceptancePending = false;
        }
        checkForChanges(); // Kiểm tra lại
    }

    /**
     * Xóa "snapshot" gốc.
     */
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

    /**
     * Cung cấp Property 'isDirty' cho ViewModel để bind (liên kết)
     * với nút Save trong Controller.
     */
    public BooleanProperty isDirtyProperty() {
        return isDirty;
    }
}