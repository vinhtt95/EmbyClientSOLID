package com.vinhtt.embyclientsolid.core;

import java.io.File;
import java.io.IOException;

/**
 * Interface trừu tượng hóa các tương tác với hệ thống file cục bộ của người dùng.
 * Chịu trách nhiệm mở file (phát media), mở thư mục, và logic tạo file phụ đề.
 */
public interface ILocalInteractionService {

    /**
     * Mở một file (để phát) hoặc một thư mục (để duyệt) bằng ứng dụng mặc định của HĐH.
     *
     * @param path Đường dẫn đến file hoặc thư mục.
     * @throws IOException Nếu đường dẫn không tồn tại hoặc không thể mở.
     * @throws UnsupportedOperationException Nếu HĐH không hỗ trợ Desktop API.
     */
    void openFileOrFolder(String path) throws IOException, UnsupportedOperationException;

    /**
     * Mở thư mục chứa file được chỉ định.
     *
     * @param path Đường dẫn đến một file (service sẽ tìm thư mục cha của file này).
     * @throws IOException Nếu đường dẫn không tồn tại hoặc không thể mở.
     * @throws UnsupportedOperationException Nếu HĐH không hỗ trợ Desktop API.
     */
    void openContainingFolder(String path) throws IOException, UnsupportedOperationException;

    /**
     * Mở thư mục screenshot cho một item media cụ thể.
     *
     * @param mediaFileName Tên file của media (ví dụ: "ABC-123.mp4").
     * @throws IOException Nếu đường dẫn không tồn tại hoặc không thể mở.
     * @throws IllegalStateException Nếu 'screenshotBasePath' chưa được cấu hình trong config.json.
     * @throws UnsupportedOperationException Nếu HĐH không hỗ trợ Desktop API.
     */
    void openScreenshotFolder(String mediaFileName) throws IOException, IllegalStateException, UnsupportedOperationException;

    /**
     * Xử lý logic "Mở Subtitle":
     * 1. Kiểm tra xem file .srt (cùng tên với media) có tồn tại không.
     * 2. Nếu không tồn tại, tạo mới file .srt với mã hóa UTF-8 BOM và nội dung tiêu đề.
     * 3. Mở file .srt (bằng trình chỉnh sửa mặc định).
     *
     * @param mediaPath Đường dẫn đầy đủ đến file media (ví dụ: ".../video.mp4").
     * @param srtTitleContent Nội dung tiêu đề (Title) để ghi vào file .srt mới (nếu tạo mới).
     * @return File .srt đã được mở (hoặc tạo).
     * @throws IOException Nếu không thể đọc/ghi file.
     * @throws UnsupportedOperationException Nếu HĐH không hỗ trợ Desktop API.
     */
    File openSubtitleFile(String mediaPath, String srtTitleContent) throws IOException, UnsupportedOperationException;
}