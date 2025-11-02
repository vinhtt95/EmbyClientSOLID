package com.vinhtt.embyclientsolid.core;

import java.io.File;
import java.io.IOException;

/**
 * Interface trừu tượng hóa các tương tác với hệ thống file cục bộ của người dùng.
 * Chịu trách nhiệm mở file, mở thư mục, và logic tạo file phụ đề.
 * (UR-27, UR-39, UR-40, UR-43).
 */
public interface ILocalInteractionService {

    /**
     * Mở một file (để phát) hoặc một thư mục (để duyệt).
     * (UR-27, UR-39).
     *
     * @param path Đường dẫn đến file hoặc thư mục.
     * @throws IOException Nếu đường dẫn không tồn tại hoặc không thể mở.
     * @throws UnsupportedOperationException Nếu HĐH không hỗ trợ Desktop API.
     */
    void openFileOrFolder(String path) throws IOException, UnsupportedOperationException;

    /**
     * Mở thư mục chứa file.
     * (UR-40).
     *
     * @param path Đường dẫn đến một file (service sẽ tìm thư mục cha).
     * @throws IOException Nếu đường dẫn không tồn tại hoặc không thể mở.
     * @throws UnsupportedOperationException Nếu HĐH không hỗ trợ Desktop API.
     */
    void openContainingFolder(String path) throws IOException, UnsupportedOperationException;

    /**
     * Mở thư mục screenshot cho một item.
     * (UR-43).
     *
     * @param mediaFileName Tên file của media (ví dụ: "ABC-123.mp4").
     * @throws IOException Nếu đường dẫn không tồn tại hoặc không thể mở.
     * @throws IllegalStateException Nếu 'screenshotBasePath' chưa được cấu hình.
     * @throws UnsupportedOperationException Nếu HĐH không hỗ trợ Desktop API.
     */
    void openScreenshotFolder(String mediaFileName) throws IOException, IllegalStateException, UnsupportedOperationException;

    /**
     * Xử lý logic "Mở Subtitle" (UR-40).
     * 1. Kiểm tra file .srt.
     * 2. Nếu không tồn tại, tạo mới với UTF-8 BOM và nội dung tiêu đề.
     * 3. Mở file .srt.
     *
     * @param mediaPath Đường dẫn đầy đủ đến file media (ví dụ: ".../video.mp4").
     * @param srtTitleContent Nội dung tiêu đề (Title) để ghi vào file .srt mới.
     * @return File .srt đã được mở (hoặc tạo).
     * @throws IOException Nếu không thể đọc/ghi file.
     * @throws UnsupportedOperationException Nếu HĐH không hỗ trợ Desktop API.
     */
    File openSubtitleFile(String mediaPath, String srtTitleContent) throws IOException, UnsupportedOperationException;
}