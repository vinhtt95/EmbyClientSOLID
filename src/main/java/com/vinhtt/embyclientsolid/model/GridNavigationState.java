package com.vinhtt.embyclientsolid.model;

import java.util.Objects;

/**
 * POJO lưu trữ một trạng thái điều hướng của ItemGridView.
 * Dùng cho chức năng "Back" và "Forward" (UR-29, UR-13, UR-14).
 * Đã cập nhật để sử dụng 'Tag' thay vì 'TagModel'.
 *
 * (Cập nhật: Thêm ScrollAction enum).
 */
public class GridNavigationState {

    /**
     * Enum báo hiệu hành động cuộn cần thực hiện sau khi tải trang mới.
     * (Được chuyển từ ItemGridViewModel cũ).
     */
    public enum ScrollAction {
        NONE, SCROLL_TO_TOP, SCROLL_TO_BOTTOM
    }

    /**
     * Định nghĩa các loại trạng thái điều hướng.
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
         * Đang xem kết quả click chip (dùng chip, chipType).
         */
        CHIP
    }

    private final StateType type;
    private final String primaryParam; // Dùng cho parentId (FOLDER) hoặc searchKeywords (SEARCH)
    private final Tag chip;          // Dùng cho CHIP (thay thế TagModel)
    private final String chipType;   // Dùng cho CHIP
    private final String sortBy;
    private final String sortOrder;
    private final int pageIndex;
    private final String selectedItemId; // ID của item đang được chọn

    /**
     * Constructor cho FOLDER và SEARCH.
     *
     * @param type           FOLDER hoặc SEARCH.
     * @param primaryParam   ID thư mục hoặc từ khóa tìm kiếm.
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
     * Constructor cho CHIP (UR-36).
     *
     * @param type           CHIP.
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

    // Getters
    public StateType getType() { return type; }
    public String getPrimaryParam() { return primaryParam; }
    public Tag getChip() { return chip; } // Trả về Tag
    public String getChipType() { return chipType; }
    public String getSortBy() { return sortBy; }
    public String getSortOrder() { return sortOrder; }
    public int getPageIndex() { return pageIndex; }
    public String getSelectedItemId() { return selectedItemId; }

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

    @Override
    public int hashCode() {
        return Objects.hash(type, primaryParam, chip, chipType, sortBy, sortOrder, pageIndex, selectedItemId);
    }
}