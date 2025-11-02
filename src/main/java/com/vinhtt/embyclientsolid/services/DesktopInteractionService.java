package com.vinhtt.embyclientsolid.services;

import com.vinhtt.embyclientsolid.core.IConfigurationService;
import com.vinhtt.embyclientsolid.core.ILocalInteractionService;

import java.awt.Desktop;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

/**
 * Triển khai (Implementation) của ILocalInteractionService.
 * Sử dụng java.awt.Desktop để tương tác với hệ thống file.
 * Chứa logic nghiệp vụ tạo file .srt (UR-40).
 */
public class DesktopInteractionService implements ILocalInteractionService {

    /**
     * Key để lấy đường dẫn screenshot trong config.json (UR-43).
     *
     */
    private static final String KEY_SCREENSHOT_BASE_PATH = "appSettings.screenshotBasePath";

    private final IConfigurationService configService;

    /**
     * Khởi tạo service.
     * @param configService Service Config (DI) để lấy I18n strings và đường dẫn.
     */
    public DesktopInteractionService(IConfigurationService configService) {
        this.configService = configService;
    }

    /**
     * Kiểm tra xem Desktop API có được hỗ trợ không.
     */
    private void checkDesktopSupport() throws UnsupportedOperationException {
        if (!Desktop.isDesktopSupported()) {
            throw new UnsupportedOperationException(
                    configService.getString("itemDetailView", "errorDesktopAPINotSupported")
            );
        }
    }

    @Override
    public void openFileOrFolder(String path) throws IOException, UnsupportedOperationException {
        checkDesktopSupport();
        if (path == null || path.isEmpty()) {
            throw new FileNotFoundException(configService.getString("itemDetailView", "errorInvalidPath"));
        }

        File fileOrDir = new File(path);
        if (!fileOrDir.exists()) {
            throw new FileNotFoundException(
                    configService.getString("itemDetailView", "errorPathNotExist", path)
            );
        }

        Desktop.getDesktop().open(fileOrDir);
    }

    @Override
    public void openContainingFolder(String path) throws IOException, UnsupportedOperationException {
        checkDesktopSupport();
        if (path == null || path.isEmpty()) {
            throw new FileNotFoundException(configService.getString("itemDetailView", "errorInvalidPath"));
        }

        File file = new File(path);
        if (!file.exists()) {
            throw new FileNotFoundException(
                    configService.getString("itemDetailView", "errorPathNotExist", path)
            );
        }

        File parentDir = file.getParentFile();
        if (parentDir != null && parentDir.exists()) {
            Desktop.getDesktop().open(parentDir);
        } else {
            throw new IOException("Không thể tìm thấy thư mục cha cho: " + path);
        }
    }

    @Override
    public void openScreenshotFolder(String mediaFileName) throws IOException, IllegalStateException, UnsupportedOperationException {
        checkDesktopSupport();

        // 1. Lấy đường dẫn base từ config (UR-43)
        //
        String basePath = configService.getString(KEY_SCREENSHOT_BASE_PATH);
        if (basePath == null || basePath.isEmpty() || basePath.equals(KEY_SCREENSHOT_BASE_PATH)) {
            throw new IllegalStateException(configService.getString("itemDetailView", "errorScreenshotPathBase"));
        }

        // 2. Lấy tên file
        if (mediaFileName == null || mediaFileName.isEmpty()) {
            throw new FileNotFoundException("Tên file media rỗng.");
        }

        // 3. Xây dựng đường dẫn và mở
        File screenshotFolder = new File(basePath, mediaFileName);
        openFileOrFolder(screenshotFolder.getAbsolutePath());
    }

    @Override
    public File openSubtitleFile(String mediaPath, String srtTitleContent) throws IOException, UnsupportedOperationException {
        checkDesktopSupport();

        // 1. Tạo đường dẫn .srt
        String srtPath = getSrtPath(mediaPath);

        // 2. Tạo file nếu chưa tồn tại (Logic UR-40)
        //
        File srtFile = new File(srtPath);
        if (!srtFile.exists()) {
            try {
                createSrtFile(srtFile, srtTitleContent);
            } catch (IOException e) {
                throw new IOException(
                        configService.getString("itemDetailView", "errorSubtitleCreate", e.getMessage()), e
                );
            }
        }

        // 3. Mở file .srt
        try {
            Desktop.getDesktop().open(srtFile);
        } catch (Exception openEx) {
            throw new IOException(
                    configService.getString("itemDetailView", "errorSubtitleOpen", openEx.getMessage()), openEx
            );
        }

        return srtFile;
    }

    /**
     * Helper tạo đường dẫn .srt từ đường dẫn .mp4.
     */
    private String getSrtPath(String mediaPath) throws IOException {
        int dotIndex = mediaPath.lastIndexOf('.');
        if (dotIndex == -1) {
            throw new IOException("Đường dẫn media không hợp lệ (không có phần mở rộng): " + mediaPath);
        }
        return mediaPath.substring(0, dotIndex) + ".srt";
    }

    /**
     * Helper tạo file .srt mới với UTF-8 BOM và nội dung.
     * (Logic từ ItemDetailController.handleOpenSubtitleAction)
     */
    private void createSrtFile(File srtFile, String srtTitleContent) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(srtFile);
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
             BufferedWriter writer = new BufferedWriter(osw)) {

            // 1. Ghi BOM (Byte Order Mark) cho UTF-8 (UR-40)
            writer.write('\uFEFF');

            // 2. Ghi nội dung (UR-40)
            writer.write("1");
            writer.newLine();
            writer.write("00:00:03,000 --> 00:00:10,000");
            writer.newLine();
            writer.write(srtTitleContent != null ? srtTitleContent : ""); // Ghi Tiêu đề
            writer.newLine();
            writer.newLine();
            // (Thêm các mốc thời gian trống khác nếu muốn, như trong code cũ)
            writer.write("2");
            writer.newLine();
            writer.write("00:30:00,000 --> 00:30:10,000");
            writer.newLine();
            writer.write("");
            writer.newLine();
            writer.newLine();

            writer.flush();
        }
    }
}