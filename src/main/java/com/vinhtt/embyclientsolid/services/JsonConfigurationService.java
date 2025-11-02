package com.vinhtt.embyclientsolid.services;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vinhtt.embyclientsolid.core.IConfigurationService;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Triển khai (Implementation) của IConfigurationService.
 * Chịu trách nhiệm duy nhất cho việc tải và đọc file config.json.
 * (UR-11, UR-43).
 * Logic được chuyển từ I18nManager cũ.
 */
public class JsonConfigurationService implements IConfigurationService {

    /**
     * Đường dẫn đến file config. Phải sao chép file này vào
     * /src/main/resources/com/vinhtt/embyclientsolid/config.json
     *
     */
    private static final String CONFIG_PATH = "/com/vinhtt/embyclientsolid/config.json";

    private final Map<String, Object> config = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();

    /**
     * Khởi tạo service và tải file config ngay lập tức.
     */
    public JsonConfigurationService() {
        loadConfig();
    }

    private void loadConfig() {
        try (InputStream is = getClass().getResourceAsStream(CONFIG_PATH)) {
            if (is == null) {
                System.err.println("NGHIÊM TRỌNG: Không tìm thấy " + CONFIG_PATH + " trong resources!");
                return;
            }
            try (InputStreamReader reader = new InputStreamReader(is)) {
                Type type = new TypeToken<Map<String, Object>>() {}.getType();
                Map<String, Object> loadedMap = gson.fromJson(reader, type);
                if (loadedMap != null) {
                    config.putAll(loadedMap);
                }
            }
        } catch (Exception e) {
            System.err.println("Không thể tải config.json: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public String getString(String key, String subKey, Object... args) {
        try {
            Map<String, String> section = (Map<String, String>) config.get(key);
            if (section != null) {
                String pattern = section.get(subKey);
                if (pattern != null) {
                    if (args.length > 0) {
                        return MessageFormat.format(pattern, args);
                    }
                    return pattern;
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy chuỗi config: " + key + "." + subKey + " | " + e.getMessage());
        }
        // Fallback
        return key + "." + subKey;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String getString(String key) {
        // Hàm này dùng để lấy các giá trị ở cấp cao nhất (ví dụ: appSettings)
        try {
            // Logic này giả định key có dạng "appSettings.screenshotBasePath"
            String[] parts = key.split("\\.");
            if (parts.length == 2) {
                Map<String, String> section = (Map<String, String>) config.get(parts[0]);
                if (section != null) {
                    String value = section.get(parts[1]);
                    if (value != null) {
                        return value;
                    }
                }
            } else if (parts.length == 1) {
                // Thử lấy trực tiếp (mặc dù config.json có cấu trúc lồng)
                Object value = config.get(key);
                if (value instanceof String) {
                    return (String) value;
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy chuỗi config: " + key + " | " + e.getMessage());
        }
        return key; // Fallback
    }
}