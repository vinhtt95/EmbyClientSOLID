package com.vinhtt.embyclientsolid.services;

import com.vinhtt.embyclientsolid.core.IPreferenceService;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Triển khai (Implementation) của IPreferenceService sử dụng java.util.prefs.
 * Chịu trách nhiệm duy nhất cho việc đọc/ghi cài đặt của người dùng.
 * (UR-2, UR-4, UR-6, UR-8, UR-50).
 */
public class JavaPreferenceService implements IPreferenceService {

    /**
     * Đường dẫn node trong preferences, lấy từ EmbyService cũ.
     *
     */
    private static final String PREF_NODE_PATH = "/com/vinhtt/embyclientsolid";

    private final Preferences prefs;

    /**
     * Khởi tạo service và lấy đối tượng Preferences gốc.
     */
    public JavaPreferenceService() {
        this.prefs = Preferences.userRoot().node(PREF_NODE_PATH);
    }

    @Override
    public String getString(String key, String defaultValue) {
        return prefs.get(key, defaultValue);
    }

    @Override
    public void putString(String key, String value) {
        if (value == null) {
            prefs.remove(key);
        } else {
            prefs.put(key, value);
        }
    }

    @Override
    public double getDouble(String key, double defaultValue) {
        return prefs.getDouble(key, defaultValue);
    }

    @Override
    public void putDouble(String key, double value) {
        prefs.putDouble(key, value);
    }

    @Override
    public void remove(String key) {
        prefs.remove(key);
    }

    @Override
    public void flush() {
        try {
            prefs.flush();
        } catch (BackingStoreException e) {
            System.err.println("Lỗi khi flush preferences: " + e.getMessage());
            e.printStackTrace();
        }
    }
}