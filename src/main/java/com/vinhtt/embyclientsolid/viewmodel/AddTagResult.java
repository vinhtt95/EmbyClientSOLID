package com.vinhtt.embyclientsolid.viewmodel;

import com.vinhtt.embyclientsolid.model.Tag;

/**
 * Lớp POJO chứa kết quả trả về từ AddTagDialog.
 * (UR-35).
 */
public class AddTagResult {
    private final Tag tag;
    private final String copyId;

    /**
     * Khởi tạo kết quả với một Tag mới.
     * @param tag Tag được tạo.
     */
    public AddTagResult(Tag tag) {
        this.tag = tag;
        this.copyId = null;
    }

    /**
     * Khởi tạo kết quả với một ID để sao chép.
     * @param copyId ID của item nguồn.
     */
    public AddTagResult(String copyId) {
        this.tag = null;
        this.copyId = copyId;
    }

    public Tag getTag() {
        return tag;
    }

    public String getCopyId() {
        return copyId;
    }

    public boolean isTag() {
        return tag != null;
    }

    public boolean isCopy() {
        return copyId != null;
    }
}