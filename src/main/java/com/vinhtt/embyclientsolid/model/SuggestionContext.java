package com.vinhtt.embyclientsolid.model;

/**
 * Enum (kiểu liệt kê) định nghĩa bối cảnh (context) khi mở AddTagDialog (UR-35).
 * Điều này giúp dialog biết loại gợi ý nào cần tải và
 * thuộc tính nào của item cần được cập nhật
 * (ví dụ: đang thêm TAG hay thêm STUDIO).
 */
public enum SuggestionContext {
    TAG,
    STUDIO,
    PEOPLE,
    GENRE
}