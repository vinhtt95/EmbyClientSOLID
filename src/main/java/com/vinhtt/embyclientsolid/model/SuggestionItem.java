package com.vinhtt.embyclientsolid.model;

import embyclient.model.BaseItemDto;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Lớp POJO (Đối tượng Java cũ đơn giản) đại diện cho một mục gợi ý (suggestion)
 * (ví dụ: Genre, Studio, People) được hiển thị trong AddTagDialog.
 * Đây là phiên bản đơn giản hóa của BaseItemDto, chỉ chứa thông tin cần thiết cho UI.
 */
public class SuggestionItem {

    private final String id;
    private final String name;
    private final String type;
    // (Các trường này hiện không dùng nhưng để tương thích với DTO gốc)
    private final Map<String, String> imageTags;
    private final List<String> backdropImageTags;

    /**
     * Khởi tạo một SuggestionItem từ một BaseItemDto (lấy từ API Emby).
     *
     * @param dto DTO (BaseItemDto) từ Emby API.
     */
    public SuggestionItem(BaseItemDto dto) {
        this.id = dto.getId();
        this.name = dto.getName();
        this.type = dto.getType();
        this.imageTags = dto.getImageTags() != null ? dto.getImageTags() : Collections.emptyMap();
        this.backdropImageTags = dto.getBackdropImageTags() != null ? dto.getBackdropImageTags() : Collections.emptyList();
    }

    /**
     * Khởi tạo một SuggestionItem từ các giá trị thô
     * (ví dụ: khi chuyển đổi từ một `Tag` đơn giản không có DTO đầy đủ).
     *
     * @param name Tên hiển thị.
     * @param id ID (có thể null).
     * @param type Loại (ví dụ: "Tag", "Studio").
     */
    public SuggestionItem(String name, String id, String type) {
        this.name = name;
        this.id = id;
        this.type = type;
        this.imageTags = Collections.emptyMap(); // Không có thông tin ảnh khi tạo từ Tag
        this.backdropImageTags = Collections.emptyList();
    }


    /**
     * Hàm helper tĩnh để chuyển đổi một danh sách DTOs (từ API)
     * thành danh sách SuggestionItems (dùng cho UI).
     *
     * @param dtoList Danh sách DTO (BaseItemDto).
     * @return Danh sách SuggestionItem.
     */
    public static List<SuggestionItem> fromBaseItemDtoList(List<BaseItemDto> dtoList) {
        if (dtoList == null) return Collections.emptyList();
        return dtoList.stream()
                .filter(Objects::nonNull)
                .map(SuggestionItem::new)
                .collect(Collectors.toList());
    }

    // --- Getters ---
    public String getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }

    /**
     * Trả về chuỗi hiển thị cho UI (ví dụ: trong chip gợi ý).
     */
    @Override
    public String toString() {
        return name + " (" + type + ")";
    }

    /**
     * Ghi đè (override) phương thức equals để so sánh các item.
     * So sánh dựa trên Name, Type, và ID (nếu có).
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SuggestionItem that = (SuggestionItem) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(type, that.type) &&
                Objects.equals(id, that.id);
    }

    /**
     * Ghi đè (override) phương thức hashCode.
     */
    @Override
    public int hashCode() {
        return Objects.hash(name, type, id);
    }
}