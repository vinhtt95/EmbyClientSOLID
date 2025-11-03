package com.vinhtt.embyclientsolid.services;

import com.vinhtt.embyclientsolid.core.IPreferenceService;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Triển khai của {@link IPreferenceService} sử dụng {@code java.util.prefs}.
 * Lớp này chịu trách nhiệm duy nhất cho việc đọc và ghi
 * các cài đặt cố định của người dùng (ví dụ: session token, URL máy chủ,
 * vị trí/kích thước cửa sổ).
 * (UR-2, UR-4, UR-6, UR-8, UR-50).
 */
public class JavaPreferenceService implements IPreferenceService {

    /**
     * Đường dẫn node trong hệ thống preferences của Java.
     * Tất cả cài đặt sẽ được lưu dưới node này.
     */
    private static final String PREF_NODE_PATH = "/com/vinhtt/embyclientsolid";

    private final Preferences prefs;

    /**
     * Khởi tạo dịch vụ và lấy đối tượng {@link Preferences} gốc
     * cho node cụ thể của ứng dụng này.
     */
    public JavaPreferenceService() {
        this.prefs = Preferences.userRoot().node(PREF_NODE_PATH);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getString(String key, String defaultValue) {
        return prefs.get(key, defaultValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putString(String key, String value) {
        if (value == null) {
            // Nếu giá trị là null, hành vi chuẩn là xóa key đó
            prefs.remove(key);
        } else {
            prefs.put(key, value);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getDouble(String key, double defaultValue) {
        return prefs.getDouble(key, defaultValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putDouble(String key, double value) {
        prefs.putDouble(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(String key) {
        prefs.remove(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flush() {
        try {
            // Đẩy các thay đổi trong bộ nhớ xuống bộ lưu trữ cố định (backing store)
            prefs.flush();
        } catch (BackingStoreException e) {
            System.err.println("Lỗi khi flush preferences: " + e.getMessage());
            e.printStackTrace();
        }
    }
}