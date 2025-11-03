package com.vinhtt.embyclientsolid.core;

/**
 * Interface trừu tượng hóa việc đọc file cấu hình
 * chỉ-đọc (read-only) của ứng dụng (ví dụ: config.json).
 * Dùng để tải các chuỗi I18n (Quốc tế hóa) và các đường dẫn cấu hình cố định.
 */
public interface IConfigurationService {

    /**
     * Lấy một chuỗi (string) từ file config, hỗ trợ format.
     *
     * @param key    Khóa chính (ví dụ: "appSettings").
     * @param subKey Khóa phụ (ví dụ: "screenshotBasePath").
     * @param args   Các đối số để format (ví dụ: cho "Hello {0}").
     * @return Chuỗi đã được định dạng, hoặc "key.subKey" nếu không tìm thấy.
     */
    String getString(String key, String subKey, Object... args);

    /**
     * Lấy một chuỗi (string) đơn giản từ file config (ví dụ: "appSettings.screenshotBasePath").
     *
     * @param key Khóa (ví dụ: "appSettings.screenshotBasePath").
     * @return Giá trị chuỗi, hoặc chính "key" nếu không tìm thấy.
     */
    String getString(String key);
}