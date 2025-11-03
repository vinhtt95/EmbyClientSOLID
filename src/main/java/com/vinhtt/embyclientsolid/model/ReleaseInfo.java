package com.vinhtt.embyclientsolid.model;

import java.time.OffsetDateTime;

/**
 * Lớp POJO (Đối tượng Java cũ đơn giản) chứa kết quả trả về
 * từ `IExternalDataService` (API bên ngoài lấy ngày phát hành) (UR-38).
 */
public class ReleaseInfo {

    private final OffsetDateTime releaseDate;
    private final String actressName;

    /**
     * Khởi tạo đối tượng thông tin phát hành.
     *
     * @param releaseDate Ngày phát hành (có thể là null).
     * @param actressName Tên diễn viên (có thể là null).
     */
    public ReleaseInfo(OffsetDateTime releaseDate, String actressName) {
        this.releaseDate = releaseDate;
        this.actressName = actressName;
    }

    /**
     * Lấy ngày phát hành.
     *
     * @return Ngày phát hành (OffsetDateTime), hoặc null.
     */
    public OffsetDateTime getReleaseDate() {
        return releaseDate;
    }

    /**
     * Lấy tên diễn viên.
     *
     * @return Tên diễn viên (String), hoặc null.
     */
    public String getActressName() {
        return actressName;
    }
}