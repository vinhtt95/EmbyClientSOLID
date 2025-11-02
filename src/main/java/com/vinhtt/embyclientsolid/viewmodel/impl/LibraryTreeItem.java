package com.vinhtt.embyclientsolid.model;

import embyclient.model.BaseItemDto;

/**
 * Lớp wrapper cho item trong TreeView (Cột 1).
 * Dùng để phân biệt DTO thật và item "Đang tải..." (dummy node).
 * (UR-17).
 */
public class LibraryTreeItem {

    private final BaseItemDto itemDto;
    private final String loadingMessage;

    /**
     * Tạo một item cây thật.
     * @param itemDto DTO từ Emby.
     */
    public LibraryTreeItem(BaseItemDto itemDto) {
        this.itemDto = itemDto;
        this.loadingMessage = null;
    }

    /**
     * Tạo một item "Đang tải..." (dummy node).
     * @param loadingMessage Thông điệp (ví dụ: "Đang tải...").
     */
    public LibraryTreeItem(String loadingMessage) {
        this.itemDto = null;
        this.loadingMessage = loadingMessage;
    }

    /**
     * @return true nếu đây là dummy node "Đang tải...".
     */
    public boolean isLoadingNode() {
        return this.itemDto == null;
    }

    public BaseItemDto getItemDto() {
        return itemDto;
    }

    @Override
    public String toString() {
        if (itemDto != null) {
            return itemDto.getName();
        }
        return loadingMessage;
    }
}