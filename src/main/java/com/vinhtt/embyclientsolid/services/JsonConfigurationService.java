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
 * Triển khai của {@link IConfigurationService}.
 * Chịu trách nhiệm duy nhất cho việc tải và đọc tệp cấu hình
 * {@code config.json} từ thư mục resources.
 * Dịch vụ này được dùng để lấy chuỗi I18n (Quốc tế hóa) và các đường dẫn cố định.
 * (UR-11, UR-43).
 */
public class JsonConfigurationService implements IConfigurationService {

    /**
     * Đường dẫn tuyệt đối trong 'resources' đến tệp cấu hình.
     */
    private static final String CONFIG_PATH = "/com/vinhtt/embyclientsolid/config.json";

    // Sử dụng ConcurrentHashMap để đảm bảo an toàn luồng nếu cần
    private final Map<String, Object> config = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();

    /**
     * Khởi tạo dịch vụ và thực hiện tải tệp {@code config.json}
     * ngay lập tức vào bộ nhớ.
     */
    public JsonConfigurationService() {
        loadConfig();
    }

    /**
     * Tải tệp {@code config.json} từ class path (resources)
     * và parse nó vào {@code config} Map.
     */
    private void loadConfig() {
        // Sử dụng getResourceAsStream để đọc tệp từ bên trong JAR/resources
        try (InputStream is = getClass().getResourceAsStream(CONFIG_PATH)) {
            if (is == null) {
                System.err.println("LỖI NGHIÊM TRỌNG: Không tìm thấy " + CONFIG_PATH + " trong resources!");
                return;
            }
            // Sử dụng InputStreamReader để đọc tệp với mã hóa (mặc định)
            try (InputStreamReader reader = new InputStreamReader(is)) {
                // Định nghĩa kiểu dữ liệu cho Gson (Map lồng nhau)
                Type type = new TypeToken<Map<String, Object>>() {}.getType();
                Map<String, Object> loadedMap = gson.fromJson(reader, type);
                if (loadedMap != null) {
                    // Tải dữ liệu vào Map an toàn luồng
                    config.putAll(loadedMap);
                }
            }
        } catch (Exception e) {
            System.err.println("Không thể tải config.json: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public String getString(String key, String subKey, Object... args) {
        try {
            // Giả định cấu trúc 2 cấp: config.get("mainView").get("statusReady")
            Map<String, String> section = (Map<String, String>) config.get(key);
            if (section != null) {
                String pattern = section.get(subKey);
                if (pattern != null) {
                    // Nếu có tham số (args), định dạng chuỗi (ví dụ: "Lỗi: {0}")
                    if (args.length > 0) {
                        return MessageFormat.format(pattern, args);
                    }
                    // Nếu không, trả về chuỗi gốc
                    return pattern;
                }
            }
        } catch (Exception e) {
            // Xử lý lỗi nếu cast (Map<String, String>) thất bại hoặc key không tồn tại
            System.err.println("Lỗi khi lấy chuỗi config: " + key + "." + subKey + " | " + e.getMessage());
        }
        // Trả về key nếu không tìm thấy để dễ dàng debug
        return key + "." + subKey;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public String getString(String key) {
        try {
            // Xử lý key dạng "key.subKey" (ví dụ: "appSettings.screenshotBasePath")
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
                // Xử lý key dạng "key" (mặc dù config.json có cấu trúc lồng)
                Object value = config.get(key);
                if (value instanceof String) {
                    return (String) value;
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy chuỗi config: " + key + " | " + e.getMessage());
        }
        // Trả về key nếu không tìm thấy để dễ dàng debug
        return key;
    }
}