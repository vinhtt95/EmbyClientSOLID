package com.vinhtt.embyclientsolid.model;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.Map;
import java.util.Objects;

/**
 * Lớp Model (POJO) cốt lõi đại diện cho một Tag (hoặc Studio, People, Genre).
 * Lớp này xử lý logic phân tích (parse) và tuần tự hóa (serialize)
 * giữa chuỗi thô (String) nhận từ API Emby và đối tượng Tag.
 *
 * Một Tag có thể là:
 * 1. Dạng chuỗi đơn giản (ví dụ: "Beautiful Girl").
 * 2. Dạng Key-Value (JSON) (ví dụ: chuỗi "{\"Body\":\"Slim\"}").
 *
 * Lớp này cũng lưu trữ 'id' gốc từ Emby (nếu có).
 */
public class Tag {

    // Đối tượng Gson tĩnh để parse JSON
    private static final Gson gson = new Gson();

    private final boolean isJson;
    private final String simpleName; // Chỉ dùng nếu isJson = false
    private final String key;        // Chỉ dùng nếu isJson = true
    private final String value;      // Chỉ dùng nếu isJson = true
    private final String id;         // ID gốc từ Emby (có thể là null)

    /**
     * Khởi tạo một Tag dạng chuỗi đơn giản (Simple).
     *
     * @param simpleName Tên hiển thị (ví dụ: "Beautiful Girl").
     * @param id         ID gốc từ Emby (có thể là null).
     */
    public Tag(String simpleName, String id) {
        this.isJson = false;
        this.simpleName = simpleName;
        this.key = null;
        this.value = null;
        this.id = id;
    }

    /**
     * Khởi tạo một Tag dạng Key-Value (JSON).
     *
     * @param key   Key (ví dụ: "Body").
     * @param value Value (ví dụ: "Slim").
     * @param id    ID gốc từ Emby (có thể là null).
     */
    public Tag(String key, String value, String id) {
        this.isJson = true;
        this.simpleName = null;
        this.key = key;
        this.value = value;
        this.id = id;
    }

    /**
     * Phân tích (parse) một chuỗi 'Name' (từ API) thành một đối tượng Tag,
     * tự động phát hiện JSON.
     *
     * @param rawName Chuỗi thô từ API (ví dụ: "Beautiful Girl" hoặc "{\"Body\":\"Slim\"}").
     * @return Một đối tượng Tag.
     */
    public static Tag parse(String rawName) {
        return parse(rawName, null);
    }

    /**
     * Phân tích (parse) một chuỗi 'Name' (từ API) và gán ID (nếu có)
     * vào đối tượng Tag.
     *
     * @param rawName Chuỗi thô từ API.
     * @param id      ID gốc từ Emby (có thể là null).
     * @return Một đối tượng Tag.
     */
    public static Tag parse(String rawName, String id) {
        if (rawName == null || rawName.isEmpty()) {
            // Trả về giá trị mặc định nếu chuỗi rỗng
            return new Tag("Trống", id);
        }

        // Kiểm tra xem có phải là chuỗi JSON thô hay không
        if (rawName.startsWith("{") && rawName.endsWith("}")) {
            try {
                // Thử parse JSON
                JsonObject jsonObject = gson.fromJson(rawName, JsonObject.class);

                // Lấy entry (cặp key-value) ĐẦU TIÊN
                Map.Entry<String, com.google.gson.JsonElement> firstEntry = jsonObject.entrySet().stream().findFirst().orElse(null);

                if (firstEntry != null) {
                    // Trả về dạng Key-Value
                    return new Tag(firstEntry.getKey(), firstEntry.getValue().getAsString(), id);
                }
            } catch (JsonSyntaxException | IllegalStateException e) {
                // Không phải JSON hợp lệ (ví dụ: chuỗi chứa "{"),
                // coi như chuỗi thường và đi xuống dưới.
            }
        }

        // Mặc định là chuỗi thường
        return new Tag(rawName, id);
    }


    /**
     * Chuyển đổi đối tượng Tag này TRỞ LẠI thành chuỗi String (đã tuần tự hóa)
     * để LƯU vào DTO (ví dụ: `BaseItemDto.TagItems`).
     *
     * @return Chuỗi đã được serialize (ví dụ: "Beautiful Girl" hoặc "{\"Body\":\"Slim\"}").
     */
    public String serialize() {
        if (isJson) {
            // Nếu là JSON, tạo lại đối tượng JSON
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty(key, value);
            return gson.toJson(jsonObject);
        } else {
            // Nếu là Simple, trả về tên
            return simpleName;
        }
    }

    /**
     * Lấy chuỗi hiển thị cho UI (ví dụ: trong TagChip).
     *
     * @return Chuỗi để hiển thị (ví dụ: "Body | Slim" hoặc "Beautiful Girl").
     */
    public String getDisplayName() {
        if (isJson) {
            return String.format("%s | %s", key, value);
        } else {
            return simpleName;
        }
    }

    /**
     * Kiểm tra xem Tag này có phải là dạng Key-Value (JSON) không.
     *
     * @return true nếu tag này là dạng Key-Value (JSON).
     */
    public boolean isJson() {
        return isJson;
    }

    /**
     * Lấy Key (chỉ cho tag JSON).
     *
     * @return Key (hoặc null nếu là tag Simple).
     */
    public String getKey() {
        return key;
    }

    /**
     * Lấy Value (chỉ cho tag JSON).
     *
     * @return Value (hoặc null nếu là tag Simple).
     */
    public String getValue() {
        return value;
    }

    /**
     * Lấy ID gốc từ Emby (nếu có).
     *
     * @return ID (hoặc null).
     */
    public String getId() {
        return id;
    }

    /**
     * Ghi đè (override) phương thức equals để so sánh các Tag.
     * Hai Tag là giống hệt nhau nếu tất cả các trường (bao gồm cả ID) đều giống nhau.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tag tag = (Tag) o;
        return isJson == tag.isJson &&
                Objects.equals(simpleName, tag.simpleName) &&
                Objects.equals(key, tag.key) &&
                Objects.equals(value, tag.value) &&
                Objects.equals(id, tag.id); // Thêm ID vào so sánh
    }

    /**
     * Ghi đè (override) phương thức hashCode.
     */
    @Override
    public int hashCode() {
        return Objects.hash(isJson, simpleName, key, value, id); // Thêm ID vào hash
    }

    /**
     * Ghi đè (override) phương thức toString (dùng chủ yếu để debug).
     */
    @Override
    public String toString() {
        return "Tag{" + getDisplayName() + (id != null ? ", id=" + id : "") + "}";
    }
}