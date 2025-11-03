package com.vinhtt.embyclientsolid.data;

import com.vinhtt.embyclientsolid.model.ReleaseInfo;

/**
 * Interface trừu tượng hóa việc truy vấn (Query) dữ liệu
 * từ các API bên ngoài (không phải Emby).
 */
public interface IExternalDataService {

    /**
     * Lấy thông tin phát hành (ngày phát hành, tên diễn viên) cho một item,
     * dựa trên mã (code) của item (thường là Original Title).
     *
     * @param itemCode Mã của item (ví dụ: "ABC-123").
     * @return Đối tượng ReleaseInfo chứa thông tin tìm được, hoặc null nếu không tìm thấy.
     */
    ReleaseInfo fetchReleaseInfoByCode(String itemCode);
}