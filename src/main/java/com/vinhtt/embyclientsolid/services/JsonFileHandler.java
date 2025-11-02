package com.vinhtt.embyclientsolid.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vinhtt.embyclientsolid.core.IConfigurationService;
import embyclient.JSON; // SDK của Emby
import embyclient.model.BaseItemDto;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.time.OffsetDateTime;

/**
 * Service xử lý việc đọc và ghi file JSON cho chức năng Import/Export.
 * (UR-44, UR-45).
 * Logic được chuyển từ JsonFileHandler.java (cũ).
 */
public class JsonFileHandler {

    private final IConfigurationService configService;
    private final Gson gson;

    /**
     * Khởi tạo service.
     * @param configService Service cấu hình để lấy chuỗi I18n.
     */
    public JsonFileHandler(IConfigurationService configService) {
        this.configService = configService;

        // Cấu hình Gson để xử lý đúng định dạng ngày tháng của Emby SDK
        this.gson = new GsonBuilder()
                .registerTypeAdapter(OffsetDateTime.class, new JSON.OffsetDateTimeTypeAdapter())
                .setPrettyPrinting()
                .create();
    }

    /**
     * Hiển thị cửa sổ MỞ file JSON.
     * (UR-45).
     * @param ownerStage Stage sở hữu.
     * @return File đã chọn, hoặc null.
     */
    public File showOpenJsonDialog(Stage ownerStage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(configService.getString("jsonFileHandler", "importTitle"));
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON files (*.json)", "*.json")
        );
        return fileChooser.showOpenDialog(ownerStage);
    }

    /**
     * Hiển thị cửa sổ LƯU file JSON.
     * (UR-44).
     * @param ownerStage Stage sở hữu.
     * @param initialFileName Tên file gợi ý.
     * @return File đích, hoặc null.
     */
    public File showSaveJsonDialog(Stage ownerStage, String initialFileName) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(configService.getString("jsonFileHandler", "exportTitle"));
        fileChooser.setInitialFileName(initialFileName);
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON files (*.json)", "*.json")
        );
        return fileChooser.showSaveDialog(ownerStage);
    }

    /**
     * Đọc file JSON và chuyển đổi thành BaseItemDto.
     * (UR-45).
     */
    public BaseItemDto readJsonFileToObject(File file) throws Exception {
        try (Reader reader = new FileReader(file)) {
            return gson.fromJson(reader, BaseItemDto.class);
        }
    }

    /**
     * Ghi đối tượng BaseItemDto ra file JSON.
     * (UR-44).
     */
    public void writeObjectToJsonFile(BaseItemDto object, File file) throws Exception {
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(object, writer);
        }
    }
}