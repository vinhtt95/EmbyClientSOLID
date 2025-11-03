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
 * Triển khai của {@link ILocalInteractionService}.
 * Sử dụng {@code java.awt.Desktop} để tương tác với hệ thống file của người dùng.
 * Chịu trách nhiệm mở file/thư mục và chứa logic nghiệp vụ để tạo file subtitle (.srt)
 * theo yêu cầu (UR-40, UR-43).
 */
public class DesktopInteractionService implements ILocalInteractionService {

    /**
     * Khóa (key) để tra cứu đường dẫn cơ sở của thư mục screenshot
     * trong tệp {@code config.json} (UR-43).
     */
    private static final String KEY_SCREENSHOT_BASE_PATH = "appSettings.screenshotBasePath";

    private final IConfigurationService configService;

    /**
     * Khởi tạo dịch vụ DesktopInteraction.
     *
     * @param configService Dịch vụ Cấu hình (DI) để lấy các chuỗi I18n và đường dẫn
     * từ {@code config.json}.
     */
    public DesktopInteractionService(IConfigurationService configService) {
        this.configService = configService;
    }

    /**
     * Kiểm tra xem {@code java.awt.Desktop} có được hệ điều hành hiện tại hỗ trợ không.
     *
     * @throws UnsupportedOperationException Nếu Desktop API không được hỗ trợ.
     */
    private void checkDesktopSupport() throws UnsupportedOperationException {
        if (!Desktop.isDesktopSupported()) {
            // Ném lỗi nếu HĐH không hỗ trợ (ví dụ: môi trường server không đầu)
            throw new UnsupportedOperationException(
                    configService.getString("itemDetailView", "errorDesktopAPINotSupported")
            );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void openFileOrFolder(String path) throws IOException, UnsupportedOperationException {
        checkDesktopSupport();
        if (path == null || path.isEmpty()) {
            throw new FileNotFoundException(configService.getString("itemDetailView", "errorInvalidPath"));
        }

        File fileOrDir = new File(path);
        if (!fileOrDir.exists()) {
            // Ném lỗi nếu đường dẫn không tồn tại trên hệ thống
            throw new FileNotFoundException(
                    configService.getString("itemDetailView", "errorPathNotExist", path)
            );
        }

        // Ủy thác cho HĐH mở file/thư mục bằng ứng dụng mặc định
        Desktop.getDesktop().open(fileOrDir);
    }

    /**
     * {@inheritDoc}
     */
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

        // Lấy thư mục cha của file
        File parentDir = file.getParentFile();
        if (parentDir != null && parentDir.exists()) {
            // Mở thư mục cha
            Desktop.getDesktop().open(parentDir);
        } else {
            throw new IOException(configService.getString("exceptions", "parentFolderNotFound", path));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void openScreenshotFolder(String mediaFileName) throws IOException, IllegalStateException, UnsupportedOperationException {
        checkDesktopSupport();

        // 1. Lấy đường dẫn cơ sở (base path) từ config.json (UR-43)
        String basePath = configService.getString(KEY_SCREENSHOT_BASE_PATH);
        if (basePath == null || basePath.isEmpty() || basePath.equals(KEY_SCREENSHOT_BASE_PATH)) {
            // Ném lỗi nếu config chưa được thiết lập
            throw new IllegalStateException(configService.getString("itemDetailView", "errorScreenshotPathBase"));
        }

        // 2. Kiểm tra tên file media
        if (mediaFileName == null || mediaFileName.isEmpty()) {
            throw new FileNotFoundException(configService.getString("exceptions", "mediaFileNameEmpty"));
        }

        // 3. Xây dựng đường dẫn đầy đủ (basePath + mediaFileName) và mở
        File screenshotFolder = new File(basePath, mediaFileName);
        openFileOrFolder(screenshotFolder.getAbsolutePath());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File openSubtitleFile(String mediaPath, String srtTitleContent) throws IOException, UnsupportedOperationException {
        checkDesktopSupport();

        // 1. Xác định đường dẫn .srt (ví dụ: "video.mp4" -> "video.srt")
        String srtPath = getSrtPath(mediaPath);

        // 2. Logic tạo file nếu chưa tồn tại (UR-40)
        File srtFile = new File(srtPath);
        if (!srtFile.exists()) {
            try {
                // Gọi helper để tạo file .srt với nội dung tiêu đề
                createSrtFile(srtFile, srtTitleContent);
            } catch (IOException e) {
                // Bọc lỗi với thông báo I18n
                throw new IOException(
                        configService.getString("itemDetailView", "errorSubtitleCreate", e.getMessage()), e
                );
            }
        }

        // 3. Mở file .srt (bằng trình editor mặc định)
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
     * Xây dựng đường dẫn tệp {@code .srt} từ đường dẫn tệp media.
     *
     * @param mediaPath Đường dẫn tệp media (ví dụ: "/path/to/video.mp4").
     * @return Đường dẫn tệp subtitle tương ứng (ví dụ: "/path/to/video.srt").
     * @throws IOException Nếu đường dẫn media không hợp lệ (không có phần mở rộng).
     */
    private String getSrtPath(String mediaPath) throws IOException {
        int dotIndex = mediaPath.lastIndexOf('.');
        if (dotIndex == -1) {
            throw new IOException(configService.getString("exceptions", "mediaPathInvalid", mediaPath));
        }
        // Thay thế phần mở rộng (ví dụ: .mp4) bằng .srt
        return mediaPath.substring(0, dotIndex) + ".srt";
    }

    /**
     * Tạo một tệp {@code .srt} mới với nội dung tiêu đề (UR-40).
     * Tệp sẽ được mã hóa UTF-8 và bao gồm BOM (Byte Order Mark)
     * để đảm bảo tương thích ký tự.
     *
     * @param srtFile         File .srt để tạo.
     * @param srtTitleContent Nội dung tiêu đề (Title) của item để ghi vào phụ đề đầu tiên.
     * @throws IOException Nếu có lỗi khi ghi file.
     */
    private void createSrtFile(File srtFile, String srtTitleContent) throws IOException {
        // Sử dụng try-with-resources để đảm bảo writer được đóng
        try (FileOutputStream fos = new FileOutputStream(srtFile);
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
             BufferedWriter writer = new BufferedWriter(osw)) {

            // 1. Ghi BOM (Byte Order Mark) cho UTF-8 (UR-40)
            // Điều này cần thiết để nhiều trình phát media đọc đúng ký tự tiếng Việt
            writer.write('\uFEFF');

            // 2. Ghi nội dung subtitle đầu tiên (Tiêu đề)
            writer.write("1");
            writer.newLine();
            writer.write("00:00:03,000 --> 00:00:10,000"); // Mốc thời gian
            writer.newLine();
            writer.write(srtTitleContent != null ? srtTitleContent : ""); // Ghi Tiêu đề
            writer.newLine();
            writer.newLine();

            // 3. Ghi mốc thời gian trống thứ hai (theo code cũ)
            writer.write("2");
            writer.newLine();
            writer.write("00:30:00,000 --> 00:30:10,000");
            writer.newLine();
            writer.write(""); // Nội dung trống
            writer.newLine();
            writer.newLine();

            writer.flush();
        }
    }
}