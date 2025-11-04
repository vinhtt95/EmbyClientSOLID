package com.vinhtt.embyclientsolid.viewmodel;

import com.vinhtt.embyclientsolid.model.SuggestionContext;
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
 * Interface (Hợp đồng) cho ItemDetailViewModel (Cột 3 - Chi tiết).
 * Định nghĩa tất cả trạng thái (Properties) và hành vi (Commands)
 * mà {@link com.vinhtt.embyclientsolid.controller.ItemDetailController}
 * có thể tương tác.
 * (UR-30 đến UR-50).
 */
public interface IItemDetailViewModel {

    // --- Trạng thái (Binding) ---

    /**
     * Báo cáo trạng thái đang tải (loading) của Cột 3.
     *
     * @return Property (chỉ đọc) cho biết đang tải chi tiết.
     */
    ReadOnlyBooleanProperty loadingProperty();

    /**
     * Cung cấp thông báo trạng thái (ví dụ: "Đang tải...", "Lỗi...", "Vui lòng chọn item...").
     *
     * @return Property (chỉ đọc) chứa thông báo.
     */
    ReadOnlyStringProperty statusMessageProperty();

    /**
     * Yêu cầu View hiển thị thông báo trạng thái (thay vì nội dung chi tiết).
     *
     * @return Property (chỉ đọc) {@code true} nếu cần hiển thị status.
     */
    ReadOnlyBooleanProperty showStatusMessageProperty();

    /**
     * Liên kết (bind) với trường text "Tiêu đề" (Title) (UR-31).
     *
     * @return Property (hai chiều) chứa Tiêu đề.
     */
    StringProperty titleProperty();

    /**
     * Liên kết (bind) với trường text "Tiêu đề gốc" (Original Title) (UR-31, UR-38).
     *
     * @return Property (hai chiều) chứa Tiêu đề gốc.
     */
    StringProperty originalTitleProperty();

    /**
     * Liên kết (bind) với các nút Rating (1-10) (UR-32).
     *
     * @return Property (hai chiều) chứa điểm số (hoặc null).
     */
    ObjectProperty<Float> criticRatingProperty();

    /**
     * Liên kết (bind) với trường text "Mô tả" (Overview) (UR-31).
     *
     * @return Property (hai chiều) chứa Mô tả.
     */
    StringProperty overviewProperty();

    /**
     * Liên kết (bind) với trường text "Ngày phát hành" (Release Date) (UR-31).
     *
     * @return Property (hai chiều) chứa Ngày phát hành (dưới dạng chuỗi "dd/MM/yyyy").
     */
    StringProperty releaseDateProperty();

    /**
     * Cung cấp đường dẫn tệp cục bộ của item (UR-39).
     *
     * @return Property (chỉ đọc) chứa đường dẫn.
     */
    ReadOnlyStringProperty itemPathProperty();

    /**
     * Báo cáo item hiện tại có phải là thư mục hay không (để ẩn/hiện các nút) (UR-39).
     *
     * @return Property (chỉ đọc) {@code true} nếu là thư mục.
     */
    ReadOnlyBooleanProperty isFolderProperty();

    /**
     * Cung cấp ảnh Primary (chính) để hiển thị (UR-42).
     *
     * @return Property (chỉ đọc) chứa đối tượng {@link Image}.
     */
    ReadOnlyObjectProperty<Image> primaryImageProperty();

    /**
     * Cung cấp danh sách thông tin ảnh Backdrop (để xây dựng gallery) (UR-41).
     *
     * @return Danh sách (có thể quan sát) các {@link ImageInfo}.
     */
    ObservableList<ImageInfo> getBackdropImages();

    /**
     * Cung cấp danh sách Tags (UR-34).
     *
     * @return Danh sách (có thể quan sát) các {@link Tag}.
     */
    ObservableList<Tag> getTagItems();

    /**
     * Cung cấp danh sách Studios (UR-34).
     *
     * @return Danh sách (có thể quan sát) các {@link Tag}.
     */
    ObservableList<Tag> getStudioItems();

    /**
     * Cung cấp danh sách People (UR-34).
     *
     * @return Danh sách (có thể quan sát) các {@link Tag}.
     */
    ObservableList<Tag> getPeopleItems();

    /**
     * Cung cấp danh sách Genres (UR-34).
     *
     * @return Danh sách (có thể quan sát) các {@link Tag}.
     */
    ObservableList<Tag> getGenreItems();

    // --- Trạng thái Dirty/Import (Binding) ---

    /**
     * Báo cáo trạng thái "dirty" (đã thay đổi nhưng chưa lưu) (UR-48).
     *
     * @return Property (chỉ đọc) {@code true} nếu có thay đổi.
     */
    ReadOnlyBooleanProperty isDirtyProperty();

    /**
     * Báo cáo trạng thái "dirty" của ảnh Primary (đã kéo thả ảnh mới) (UR-42).
     *
     * @return Property (chỉ đọc) {@code true} nếu có ảnh mới chờ lưu.
     */
    ReadOnlyBooleanProperty primaryImageDirtyProperty();

    // Các property (UR-46) báo cáo View hiển thị các nút (✓/✗) cho từng trường
    ReadOnlyBooleanProperty showTitleReviewProperty();
    ReadOnlyBooleanProperty showOverviewReviewProperty();
    ReadOnlyBooleanProperty showReleaseDateReviewProperty();
    ReadOnlyBooleanProperty showOriginalTitleReviewProperty();
    ReadOnlyBooleanProperty showStudiosReviewProperty();
    ReadOnlyBooleanProperty showPeopleReviewProperty();
    ReadOnlyBooleanProperty showGenresReviewProperty();
    ReadOnlyBooleanProperty showTagsReviewProperty();
    ReadOnlyBooleanProperty showCriticRatingReviewProperty();

    // --- Sự kiện (Events) ---

    /**
     * Cung cấp sự kiện khi người dùng click vào một chip (UR-36).
     * MainController sẽ lắng nghe sự kiện này để kích hoạt Cột 2.
     *
     * @return Property (chỉ đọc) chứa sự kiện {@link ChipClickEvent}.
     */
    ReadOnlyObjectProperty<ChipClickEvent> chipClickEventProperty();

    /**
     * Xóa (tiêu thụ) sự kiện click chip sau khi đã xử lý.
     */
    void clearChipClickEvent();

    /**
     * (Được gọi bởi Controller) Bắn (fire) một sự kiện click chip mới.
     *
     * @param model Đối tượng {@link Tag} được click.
     * @param type  Loại chip ("TAG", "STUDIO", "PEOPLE", "GENRE").
     */
    void fireChipClickEvent(Tag model, String type);

    /**
     * Lấy URL đã xây dựng (build) cho một thumbnail ảnh Backdrop (UR-41).
     *
     * @param info Thông tin ảnh từ {@code getBackdropImages()}.
     * @return Chuỗi URL đầy đủ.
     */
    String getBackdropUrl(ImageInfo info);

    /**
     * Cung cấp sự kiện khi người dùng nhấn nút "Add" (+).
     * MainController sẽ lắng nghe sự kiện này để mở {@code AddTagDialog}.
     *
     * @return Property (chỉ đọc) chứa {@link SuggestionContext} (loại chip cần thêm).
     */
    ReadOnlyObjectProperty<SuggestionContext> addChipCommandProperty();

    /**
     * Xóa (tiêu thụ) sự kiện "Add Chip" sau khi dialog đã đóng.
     */
    void clearAddChipCommand();

    /**
     * Cung cấp sự kiện khi ViewModel yêu cầu mở cửa sổ pop-out (UR-50).
     *
     * @return Property (chỉ đọc) sẽ chuyển thành {@code true} khi có yêu cầu.
     */
    ReadOnlyObjectProperty<Boolean> popOutRequestProperty();

    /**
     * Xóa (tiêu thụ) sự kiện yêu cầu pop-out sau khi đã xử lý.
     */
    void clearPopOutRequest();

    /**
     * Cung cấp sự kiện khi ViewModel yêu cầu Phát item Tiếp theo.
     */
    ReadOnlyBooleanProperty playNextRequestProperty();

    /**
     * Cung cấp sự kiện khi ViewModel yêu cầu Phát item Trước đó.
     */
    ReadOnlyBooleanProperty playPreviousRequestProperty();

    /**
     * Xóa (tiêu thụ) sự kiện yêu cầu phát tiếp theo.
     */
    void clearPlayNextRequest();

    /**
     * Xóa (tiêu thụ) sự kiện yêu cầu phát trước đó.
     */
    void clearPlayPreviousRequest();

    /**
     * (Command) Yêu cầu chọn và phát item tiếp theo.
     */
    void playNextItemCommand();

    /**
     * (Command) Yêu cầu chọn và phát item trước đó.
     */
    void playPreviousItemCommand();

    /**
     * Tải chi tiết item và tự động kích hoạt lệnh phát/pop-out sau khi tải xong.
     * Dùng cho hotkey (Cmd+Shift+N/P) và nút Next/Previous.
     *
     * @param item DTO (tóm tắt) của item được chọn từ Cột 2.
     */
    void loadItemAndPlay(BaseItemDto item);

    // --- Hành động (Commands từ View) ---

    /**
     * Tải chi tiết đầy đủ của một item vào ViewModel.
     *
     * @param item DTO (tóm tắt) của item được chọn từ Cột 2.
     */
    void loadItem(BaseItemDto item);

    /**
     * Lưu tất cả các thay đổi (đã "dirty") lên server (UR-31, UR-49).
     */
    void saveChangesCommand();

    /**
     * Lưu thay đổi Rating ngay lập tức (UR-33).
     *
     * @param newRating Điểm số mới (hoặc null).
     */
    void saveCriticRatingImmediately(Float newRating);

    /**
     * Gọi API bên ngoài để lấy ngày phát hành và thông tin diễn viên (UR-38).
     */
    void fetchReleaseDateCommand();

    /**
     * Nhân bản (clone) thuộc tính từ item hiện tại sang các item con
     * của thư mục đang chọn ở Cột 1 (UR-37).
     *
     * @param propertyType Loại thuộc tính ("Tags", "Studios", "People", "Genres").
     */
    void clonePropertiesCommand(String propertyType);

    // --- Commands Tương tác Local ---

    /**
     * Mở file media (để phát) hoặc thư mục (để duyệt) (UR-39).
     * Cũng kích hoạt {@code popOutRequestProperty} (UR-50).
     */
    void openFileOrFolderCommand();

    /**
     * Mở (hoặc tạo) file phụ đề (.srt) (UR-40).
     */
    void openSubtitleCommand();

    /**
     * Mở thư mục screenshot cục bộ tương ứng với item (UR-43).
     */
    void openScreenshotFolderCommand();

    // --- Commands Ảnh ---

    /**
     * Xử lý khi người dùng kéo-thả file ảnh vào ảnh Primary (UR-42).
     *
     * @param imageFile File ảnh đã thả.
     */
    void setDroppedPrimaryImage(File imageFile);

    /**
     * Lưu ảnh Primary đã được kéo-thả lên server (UR-42).
     */
    void saveNewPrimaryImageCommand();

    /**
     * Xóa một ảnh Backdrop (UR-41).
     *
     * @param imageInfo Thông tin ảnh cần xóa.
     */
    void deleteBackdropCommand(ImageInfo imageInfo);

    /**
     * Xử lý khi người dùng kéo-thả (nhiều) file ảnh vào gallery Backdrop (UR-41).
     *
     * @param files Danh sách các file ảnh đã thả.
     */
    void uploadDroppedBackdropFiles(List<File> files);

    // --- Commands Chip (UR-34) ---

    /**
     * Kích hoạt sự kiện {@code addChipCommandProperty} với bối cảnh "TAG".
     */
    void addTagCommand();
    void addStudioCommand();
    void addGenreCommand();
    void addPeopleCommand();

    /**
     * Xóa một chip khỏi danh sách trong bộ nhớ (sẽ kích hoạt "dirty").
     *
     * @param tag Đối tượng {@link Tag} cần xóa.
     */
    void removeTag(Tag tag);
    void removeStudio(Tag tag);
    void removeGenre(Tag tag);
    void removePeople(Tag tag);

    /**
     * Kích hoạt lại dialog "Add Chip" cuối cùng đã được sử dụng (Hotkey ENTER - UR-13).
     */
    void repeatAddChipCommand();

    /**
     * Xử lý kết quả trả về từ {@code AddTagDialog} (do MainController gọi).
     *
     * @param result  Kết quả (Tag mới hoặc Copy ID).
     * @param context Bối cảnh (TAG, STUDIO...) của dialog vừa đóng.
     */
    void processAddTagResult(AddTagResult result, SuggestionContext context);

    // --- Commands Import/Export ---

    /**
     * Bắt đầu quá trình đọc và xem trước (preview) tệp JSON (UR-45, UR-46).
     *
     * @param file Tệp JSON đã chọn.
     */
    void importAndPreview(File file);

    /**
     * Bắt đầu quá trình export item hiện tại ra tệp JSON (UR-44).
     *
     * @param file Tệp JSON đích.
     */
    void exportCommand(File file);

    /**
     * Chấp nhận (Accept ✓) một trường đã thay đổi từ JSON (UR-47).
     *
     * @param fieldName Tên của trường (ví dụ: "title", "tags").
     */
    void acceptImportField(String fieldName);

    /**
     * Từ chối (Reject ✗) một trường đã thay đổi từ JSON (UR-47).
     *
     * @param fieldName Tên của trường (ví dụ: "title", "tags").
     */
    void rejectImportField(String fieldName);

    /**
     * (Được gọi bởi ImportHandler) Kích hoạt trạng thái "dirty" khi
     * một trường import được chấp nhận (UR-48).
     */
    void markAsDirtyByAccept();

    /**
     * Lấy tên tệp gợi ý cho việc export (UR-44).
     *
     * @return Tên tệp (ví dụ: "OriginalTitle.json").
     */
    String getExportFileName();


    /**
     * Lớp POJO (nội bộ) đóng gói sự kiện click chip (UR-36).
     * Được sử dụng bởi {@code chipClickEventProperty}.
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