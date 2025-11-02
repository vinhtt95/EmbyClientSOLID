package com.vinhtt.embyclientsolid.data;

import com.vinhtt.embyclientsolid.model.ReleaseInfo;

/**
 * Interface cho việc ĐỌC (Query) dữ liệu từ các API bên ngoài (không phải Emby).
 * (UR-38).
 */
public interface IExternalDataService {

    /**
     * Lấy ngày phát hành (và tên diễn viên) từ một API bên ngoài.
     * (UR-38).
     * @param itemCode Mã của item (thường là Original Title).
     * @return ReleaseInfo chứa ngày và tên, hoặc null nếu không tìm thấy.
     */
    ReleaseInfo fetchReleaseInfoByCode(String itemCode);
}