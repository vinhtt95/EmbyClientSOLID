package com.vinhtt.embyclientsolid.viewmodel;

import com.vinhtt.embyclientsolid.model.Tag;

/**
 * Một lớp POJO (Đối tượng Java cũ đơn giản) đóng gói kết quả trả về
 * từ {@link com.vinhtt.embyclientsolid.controller.AddTagDialogController}.
 * Dialog có thể trả về một trong hai:
 * 1. Một đối tượng {@link Tag} mới được tạo.
 * 2. Một {@link String} (ID) của item nguồn để sao chép thuộc tính (UR-35).
 */
public class AddTagResult {
    private final Tag tag;
    private final String copyId;

    /**
     * Khởi tạo kết quả khi người dùng tạo một Tag mới.
     *
     * @param tag Tag (Simple hoặc JSON) đã được tạo.
     */
    public AddTagResult(Tag tag) {
        this.tag = tag;
        this.copyId = null;
    }

    /**
     * Khởi tạo kết quả khi người dùng yêu cầu sao chép (Copy by ID).
     *
     * @param copyId ID của item nguồn để sao chép.
     */
    public AddTagResult(String copyId) {
        this.tag = null;
        this.copyId = copyId;
    }

    /**
     * Lấy đối tượng Tag (nếu có).
     *
     * @return {@link Tag} đã tạo, hoặc {@code null} nếu đây là kết quả sao chép.
     */
    public Tag getTag() {
        return tag;
    }

    /**
     * Lấy ID item nguồn để sao chép (nếu có).
     *
     * @return Chuỗi ID, hoặc {@code null} nếu đây là kết quả tạo Tag.
     */
    public String getCopyId() {
        return copyId;
    }

    /**
     * Kiểm tra xem kết quả này có phải là tạo Tag mới không.
     *
     * @return {@code true} nếu {@code tag} không phải null.
     */
    public boolean isTag() {
        return tag != null;
    }

    /**
     * Kiểm tra xem kết quả này có phải là sao chép theo ID không.
     *
     * @return {@code true} nếu {@code copyId} không phải null.
     */
    public boolean isCopy() {
        return copyId != null;
    }
}