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
 * Triển khai logic nghiệp vụ để hiển thị dialog Mở/Lưu tệp và đọc/ghi tệp JSON
 * chứa {@link BaseItemDto}. Chịu trách nhiệm cho UR-44 (Export) và UR-45 (Import).
 */
public class JsonFileHandler {

    private final IConfigurationService configService;
    private final Gson gson;

    /**
     * Khởi tạo service, tiêm (inject) {@link IConfigurationService}
     * và cấu hình {@link Gson} với bộ điều hợp (adapter) kiểu dữ liệu
     * {@link OffsetDateTime} của Emby SDK.
     *
     * @param configService Service cấu hình để lấy chuỗi I18n cho tiêu đề dialog.
     */
    public JsonFileHandler(IConfigurationService configService) {
        this.configService = configService;

        // Cấu hình Gson để xử lý đúng định dạng ngày tháng (OffsetDateTime)
        // mà Emby SDK sử dụng.
        this.gson = new GsonBuilder()
                .registerTypeAdapter(OffsetDateTime.class, new JSON.OffsetDateTimeTypeAdapter())
                .setPrettyPrinting() // Ghi JSON có định dạng (dễ đọc)
                .create();
    }

    /**
     * Hiển thị dialog 'Mở Tệp' (File Open) của hệ thống để người dùng
     * chọn tệp JSON (UR-45).
     *
     * @param ownerStage Cửa sổ (Stage) cha để khóa (modal).
     * @return {@link File} người dùng đã chọn, hoặc {@code null} nếu hủy.
     */
    public File showOpenJsonDialog(Stage ownerStage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(configService.getString("jsonFileHandler", "importTitle"));
        // Chỉ cho phép chọn file .json
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON files (*.json)", "*.json")
        );
        return fileChooser.showOpenDialog(ownerStage);
    }

    /**
     * Hiển thị dialog 'Lưu Tệp' (File Save) của hệ thống để người dùng
     * chọn vị trí export JSON (UR-44).
     *
     * @param ownerStage Cửa sổ (Stage) cha để khóa (modal).
     * @param initialFileName Tên tệp được gợi ý sẵn trong dialog.
     * @return {@link File} đích người dùng đã chọn, hoặc {@code null} nếu hủy.
     */
    public File showSaveJsonDialog(Stage ownerStage, String initialFileName) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(configService.getString("jsonFileHandler", "exportTitle"));
        fileChooser.setInitialFileName(initialFileName);
        // Đặt bộ lọc tệp .json
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON files (*.json)", "*.json")
        );
        return fileChooser.showSaveDialog(ownerStage);
    }

    /**
     * Đọc nội dung tệp JSON được chỉ định và giải tuần tự hóa (deserialize)
     * nó thành một đối tượng {@link BaseItemDto} (UR-45).
     *
     * @param file Tệp JSON nguồn.
     * @return Đối tượng {@link BaseItemDto} đã được parse.
     * @throws Exception Nếu có lỗi IO hoặc lỗi parse JSON.
     */
    public BaseItemDto readJsonFileToObject(File file) throws Exception {
        // Sử dụng try-with-resources để tự động đóng Reader
        try (Reader reader = new FileReader(file)) {
            return gson.fromJson(reader, BaseItemDto.class);
        }
    }

    /**
     * Tuần tự hóa (serialize) một đối tượng {@link BaseItemDto} thành chuỗi JSON
     * (đã định dạng) và ghi vào tệp đích (UR-44).
     *
     * @param object Đối tượng DTO cần export.
     * @param file Tệp JSON đích.
     * @throws Exception Nếu có lỗi IO khi ghi tệp.
     */
    public void writeObjectToJsonFile(BaseItemDto object, File file) throws Exception {
        // Sử dụng try-with-resources để tự động đóng Writer
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(object, writer);
        }
    }
}