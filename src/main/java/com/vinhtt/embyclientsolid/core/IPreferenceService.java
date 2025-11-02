package com.vinhtt.embyclientsolid.core;

/**
 * Interface trừu tượng hóa việc đọc và ghi
 * các cài đặt cố định của người dùng (ví dụ: java.util.prefs).
 * Dùng để lưu trữ session token, URL máy chủ, và vị trí/kích thước cửa sổ.
 * (UR-2, UR-4, UR-6, UR-8, UR-50).
 */
public interface IPreferenceService {

    /**
     * Lấy một giá trị chuỗi từ preferences.
     *
     * @param key          Khóa của giá trị.
     * @param defaultValue Giá trị trả về nếu không tìm thấy khóa.
     * @return Giá trị đã lưu hoặc giá trị mặc định.
     */
    String getString(String key, String defaultValue);

    /**
     * Lưu một giá trị chuỗi vào preferences.
     *
     * @param key   Khóa để lưu.
     * @param value Giá trị để lưu.
     */
    void putString(String key, String value);

    /**
     * Lấy một giá trị double từ preferences.
     *
     * @param key          Khóa của giá trị.
     * @param defaultValue Giá trị trả về nếu không tìm thấy khóa.
     * @return Giá trị đã lưu hoặc giá trị mặc định.
     */
    double getDouble(String key, double defaultValue);

    /**
     * Lưu một giá trị double vào preferences.
     *
     * @param key   Khóa để lưu.
     * @param value Giá trị để lưu.
     */
    void putDouble(String key, double value);

    /**
     * Xóa một khóa khỏi preferences.
     *
     * @param key Khóa cần xóa.
     */
    void remove(String key);

    /**
     * Đảm bảo tất cả các thay đổi được ghi vào bộ lưu trữ cố định.
     */
    void flush();
}