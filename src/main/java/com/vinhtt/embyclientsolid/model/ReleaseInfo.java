package com.vinhtt.embyclientsolid.model;

import java.time.OffsetDateTime;

/**
 * POJO chứa kết quả trả về từ API lấy ngày phát hành (UR-38).
 * Thay thế cho inner class 'FetchDateResult' trong RequestEmby cũ.
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
     * @return Ngày phát hành.
     */
    public OffsetDateTime getReleaseDate() {
        return releaseDate;
    }

    /**
     * @return Tên diễn viên.
     */
    public String getActressName() {
        return actressName;
    }
}