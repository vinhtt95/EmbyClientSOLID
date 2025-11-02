package com.vinhtt.embyclientsolid.model;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.Map;
import java.util.Objects;

/**
 * Lớp Model đại diện cho một Tag đã được phân tích.
 * Thay thế cho 'TagModel' trong kiến trúc cũ.
 * Lớp này là một POJO thuần túy, không chứa logic nghiệp vụ.
 * Nó có thể là một chuỗi đơn giản, hoặc một cặp Key-Value từ JSON.
 * Bao gồm trường 'id' để lưu ID gốc từ Emby (nếu có).
 */
public class Tag {

    private static final Gson gson = new Gson();

    private final boolean isJson;
    private final String simpleName;
    private final String key;
    private final String value;
    private final String id;

    /**
     * Constructor cho tag chuỗi đơn giản.
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
     * Constructor cho tag Key-Value (JSON).
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
     * Phân tích một chuỗi 'Name' (từ NameLongIdPair) thành một Tag object.
     * Tự động phát hiện JSON.
     *
     * @param rawName Chuỗi thô từ API (ví dụ: "Beautiful Girl" hoặc "{\"Body\":\"Slim\"}").
     * @return Một đối tượng Tag.
     */
    public static Tag parse(String rawName) {
        return parse(rawName, null);
    }

    /**
     * Phân tích một chuỗi 'Name' và lưu trữ ID.
     *
     * @param rawName Chuỗi thô từ API.
     * @param id      ID gốc từ Emby.
     * @return Một đối tượng Tag.
     */
    public static Tag parse(String rawName, String id) {
        if (rawName == null || rawName.isEmpty()) {
            return new Tag("Trống", id); // Giá trị mặc định nếu rỗng
        }

        // Kiểm tra xem có phải là chuỗi JSON thô hay không
        if (rawName.startsWith("{") && rawName.endsWith("}")) {
            try {
                // Thử parse JSON
                JsonObject jsonObject = gson.fromJson(rawName, JsonObject.class);

                // Lấy entry ĐẦU TIÊN
                Map.Entry<String, com.google.gson.JsonElement> firstEntry = jsonObject.entrySet().stream().findFirst().orElse(null);

                if (firstEntry != null) {
                    // Trả về dạng Key-Value
                    return new Tag(firstEntry.getKey(), firstEntry.getValue().getAsString(), id);
                }
            } catch (JsonSyntaxException | IllegalStateException e) {
                // Không phải JSON hợp lệ, coi như chuỗi thường
            }
        }

        // Mặc định là chuỗi thường
        return new Tag(rawName, id);
    }


    /**
     * Chuyển đổi Tag này TRỞ LẠI thành chuỗi String để LƯU vào DTO (trường TagItems).
     *
     * @return Chuỗi đã được serialize.
     */
    public String serialize() {
        if (isJson) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty(key, value);
            return gson.toJson(jsonObject);
        } else {
            return simpleName;
        }
    }

    /**
     * Lấy chuỗi hiển thị cho UI (ví dụ: "Body | Slim" hoặc "Beautiful Girl").
     *
     * @return Chuỗi để hiển thị.
     */
    public String getDisplayName() {
        if (isJson) {
            return String.format("%s | %s", key, value);
        } else {
            return simpleName;
        }
    }

    /**
     * @return True nếu tag này là dạng Key-Value (JSON).
     */
    public boolean isJson() {
        return isJson;
    }

    /**
     * @return Key (chỉ cho tag JSON).
     */
    public String getKey() {
        return key;
    }

    /**
     * @return Value (chỉ cho tag JSON).
     */
    public String getValue() {
        return value;
    }

    /**
     * @return ID gốc từ Emby (nếu có).
     */
    public String getId() {
        return id;
    }

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

    @Override
    public int hashCode() {
        return Objects.hash(isJson, simpleName, key, value, id); // Thêm ID vào hash
    }

    @Override
    public String toString() {
        return "Tag{" + getDisplayName() + (id != null ? ", id=" + id : "") + "}";
    }
}