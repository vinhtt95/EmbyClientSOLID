package com.vinhtt.embyclientsolid.model;

import embyclient.model.BaseItemDto;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Model POJO đơn giản hóa cho các đối tượng gợi ý như Genre, Studio, People.
 * Được chuyển đổi từ 'SuggestionItemModel' cũ.
 */
public class SuggestionItem {

    private final String id;
    private final String name;
    private final String type;
    private final Map<String, String> imageTags;
    private final List<String> backdropImageTags;

    /**
     * Khởi tạo từ một DTO.
     * @param dto DTO từ Emby API.
     */
    public SuggestionItem(BaseItemDto dto) {
        this.id = dto.getId();
        this.name = dto.getName();
        this.type = dto.getType();
        this.imageTags = dto.getImageTags() != null ? dto.getImageTags() : Collections.emptyMap();
        this.backdropImageTags = dto.getBackdropImageTags() != null ? dto.getBackdropImageTags() : Collections.emptyList();
    }

    /**
     * Chuyển đổi một danh sách DTOs thành danh sách SuggestionItems.
     * @param dtoList Danh sách DTO.
     * @return Danh sách SuggestionItem.
     */
    public static List<SuggestionItem> fromBaseItemDtoList(List<BaseItemDto> dtoList) {
        if (dtoList == null) return Collections.emptyList();
        return dtoList.stream()
                .filter(Objects::nonNull)
                .map(SuggestionItem::new)
                .collect(Collectors.toList());
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }

    @Override
    public String toString() {
        return name + " (" + type + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SuggestionItem that = (SuggestionItem) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, type);
    }
}