package com.vinhtt.embyclientsolid.core;

/**
 * Interface trừu tượng hóa việc đọc file cấu hình
 * chỉ-đọc của ứng dụng (ví dụ: config.json).
 * Dùng để tải các chuỗi I18n và các đường dẫn cố định.
 * (UR-11, UR-43).
 */
public interface IConfigurationService {

    /**
     * Lấy một chuỗi string từ file config.
     *
     * @param key    Khóa chính (ví dụ: "appSettings").
     * @param subKey Khóa phụ (ví dụ: "screenshotBasePath").
     * @param args   Các đối số để format (ví dụ: cho "Hello {0}").
     * @return Chuỗi đã được định dạng, hoặc key nếu không tìm thấy.
     */
    String getString(String key, String subKey, Object... args);

    /**
     * Lấy một chuỗi string đơn giản từ file config (chỉ có 1 cấp).
     *
     * @param key Khóa (ví dụ: "screenshotBasePath").
     * @return Giá trị chuỗi, hoặc key nếu không tìm thấy.
     */
    void StringgetString(String key);
}