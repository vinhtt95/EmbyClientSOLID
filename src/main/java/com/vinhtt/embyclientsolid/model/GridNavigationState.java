package com.vinhtt.embyclientsolid.model;

import java.util.Objects;

/**
 * Lớp POJO (Đối tượng Java cũ đơn giản) lưu trữ trạng thái của ItemGridView (Cột 2)
 * tại một thời điểm cụ thể.
 * Được sử dụng để triển khai chức năng "Back" (Lùi) và "Forward" (Tiến) (UR-29, UR-13, UR-14).
 */
public class GridNavigationState {

    /**
     * Enum xác định hành động cuộn (scroll) cần thực hiện
     * sau khi tải trang mới (ví dụ: cuộn lên đầu trang).
     */
    public enum ScrollAction {
        NONE, SCROLL_TO_TOP, SCROLL_TO_BOTTOM
    }

    /**
     * Enum định nghĩa các loại trạng thái điều hướng
     * (ví dụ: đang xem thư mục, tìm kiếm, hay lọc theo chip).
     */
    public enum StateType {
        /**
         * Đang xem một thư mục (dùng parentId).
         */
        FOLDER,
        /**
         * Đang xem kết quả tìm kiếm (dùng searchKeywords).
         */
        SEARCH,
        /**
         * Đang xem kết quả lọc theo chip (dùng đối tượng chip).
         */
        CHIP
    }

    // Kiểu trạng thái (FOLDER, SEARCH, hay CHIP)
    private final StateType type;

    // Tham số chính (dùng cho FOLDER (parentId) hoặc SEARCH (searchKeywords))
    private final String primaryParam;

    // Tham số chip (dùng cho CHIP (Tag, Studio, People...))
    private final Tag chip;
    private final String chipType;

    // Thông tin sắp xếp và phân trang
    private final String sortBy;
    private final String sortOrder;
    private final int pageIndex;

    // Item đang được chọn (để khôi phục)
    private final String selectedItemId;

    /**
     * Khởi tạo trạng thái cho điều hướng FOLDER (Thư mục) hoặc SEARCH (Tìm kiếm).
     *
     * @param type           Kiểu trạng thái (FOLDER hoặc SEARCH).
     * @param primaryParam   ID thư mục (nếu là FOLDER) hoặc từ khóa tìm kiếm (nếu là SEARCH).
     * @param sortBy         Tiêu chí sắp xếp.
     * @param sortOrder      Thứ tự sắp xếp.
     * @param pageIndex      Chỉ số trang (0-based).
     * @param selectedItemId ID của item được chọn (để khôi phục).
     */
    public GridNavigationState(StateType type, String primaryParam, String sortBy, String sortOrder, int pageIndex, String selectedItemId) {
        this.type = type;
        this.primaryParam = primaryParam;
        this.chip = null;
        this.chipType = null;
        this.sortBy = sortBy;
        this.sortOrder = sortOrder;
        this.pageIndex = pageIndex;
        this.selectedItemId = selectedItemId;
    }

    /**
     * Khởi tạo trạng thái cho điều hướng CHIP (lọc theo Tag, Studio, People, Genre) (UR-36).
     *
     * @param type           Kiểu trạng thái (luôn là CHIP).
     * @param chip           Đối tượng Tag (hoặc Studio, People, Genre) đã được click.
     * @param chipType       Loại chip (ví dụ: "TAG", "STUDIO").
     * @param sortBy         Tiêu chí sắp xếp.
     * @param sortOrder      Thứ tự sắp xếp.
     * @param pageIndex      Chỉ số trang (0-based).
     * @param selectedItemId ID của item được chọn (để khôi phục).
     */
    public GridNavigationState(StateType type, Tag chip, String chipType, String sortBy, String sortOrder, int pageIndex, String selectedItemId) {
        this.type = type;
        this.primaryParam = null;
        this.chip = chip; // Sử dụng Tag
        this.chipType = chipType;
        this.sortBy = sortBy;
        this.sortOrder = sortOrder;
        this.pageIndex = pageIndex;
        this.selectedItemId = selectedItemId;
    }

    // --- Getters ---
    public StateType getType() { return type; }
    public String getPrimaryParam() { return primaryParam; }
    public Tag getChip() { return chip; }
    public String getChipType() { return chipType; }
    public String getSortBy() { return sortBy; }
    public String getSortOrder() { return sortOrder; }
    public int getPageIndex() { return pageIndex; }
    public String getSelectedItemId() { return selectedItemId; }

    /**
     * Ghi đè (override) phương thức equals để so sánh hai trạng thái.
     * Hai trạng thái là giống hệt nhau nếu tất cả các trường đều giống nhau.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GridNavigationState that = (GridNavigationState) o;
        return pageIndex == that.pageIndex &&
                type == that.type &&
                Objects.equals(primaryParam, that.primaryParam) &&
                Objects.equals(chip, that.chip) && // So sánh Tag
                Objects.equals(chipType, that.chipType) &&
                Objects.equals(sortBy, that.sortBy) &&
                Objects.equals(sortOrder, that.sortOrder) &&
                Objects.equals(selectedItemId, that.selectedItemId);
    }

    /**
     * Ghi đè (override) phương thức hashCode.
     */
    @Override
    public int hashCode() {
        return Objects.hash(type, primaryParam, chip, chipType, sortBy, sortOrder, pageIndex, selectedItemId);
    }
}