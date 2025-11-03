package com.vinhtt.embyclientsolid.model;

import embyclient.model.BaseItemDto;

/**
 * Lớp Model (POJO) đại diện cho một mục (item) trong TreeView (Cột 1).
 * Lớp này đóng vai trò là một wrapper (lớp bọc) để phân biệt giữa một
 * `BaseItemDto` (dữ liệu thư mục thật) và một "dummy node" (ví dụ: "Đang tải...")
 * được sử dụng cho chức năng lazy loading (UR-17).
 */
public class LibraryTreeItem {

    // Chứa DTO thật nếu đây là node dữ liệu
    private final BaseItemDto itemDto;
    // Chứa thông điệp nếu đây là node "dummy"
    private final String loadingMessage;

    /**
     * Khởi tạo một item cây (node) chứa dữ liệu thật (thư mục).
     *
     * @param itemDto DTO (BaseItemDto) từ Emby.
     */
    public LibraryTreeItem(BaseItemDto itemDto) {
        this.itemDto = itemDto;
        this.loadingMessage = null;
    }

    /**
     * Khởi tạo một item cây (node) "dummy" (giả)
     * để hiển thị trạng thái (ví dụ: "Đang tải...").
     *
     * @param loadingMessage Thông điệp (ví dụ: "Đang tải...").
     */
    public LibraryTreeItem(String loadingMessage) {
        this.itemDto = null;
        this.loadingMessage = loadingMessage;
    }

    /**
     * Kiểm tra xem item này có phải là dummy node "Đang tải..." hay không.
     *
     * @return true nếu đây là dummy node, false nếu là node chứa dữ liệu thật.
     */
    public boolean isLoadingNode() {
        return this.itemDto == null;
    }

    /**
     * Lấy DTO dữ liệu thật.
     *
     * @return BaseItemDto (hoặc null nếu là dummy node).
     */
    public BaseItemDto getItemDto() {
        return itemDto;
    }

    /**
     * Trả về chuỗi hiển thị cho TreeCell (UI).
     *
     * @return Tên của item (nếu là node dữ liệu) hoặc thông điệp loading (nếu là dummy node).
     */
    @Override
    public String toString() {
        if (itemDto != null) {
            return itemDto.getName();
        }
        return loadingMessage;
    }
}