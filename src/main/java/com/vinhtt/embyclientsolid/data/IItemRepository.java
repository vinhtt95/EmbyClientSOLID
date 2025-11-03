package com.vinhtt.embyclientsolid.data;

import com.vinhtt.embyclientsolid.model.Tag;
import embyclient.ApiException;
import embyclient.model.BaseItemDto;
import embyclient.model.ImageInfo;
import embyclient.model.QueryResultBaseItemDto;

import java.util.List;

/**
 * Interface trừu tượng hóa các hành động ĐỌC (Query) dữ liệu Item chính từ Emby.
 * (Tách biệt với IItemUpdateService theo nguyên tắc CQRS).
 */
public interface IItemRepository {

    /**
     * Lấy danh sách các thư viện gốc (root views) mà user hiện tại có thể truy cập.
     *
     * @return Danh sách BaseItemDto là các thư mục gốc.
     * @throws ApiException Nếu API call thất bại.
     */
    List<BaseItemDto> getRootViews() throws ApiException;

    /**
     * Lấy danh sách các item con trực tiếp (không đệ quy) của một thư mục cha.
     * Thường dùng để lazy-loading Cột 1 (Library Tree).
     *
     * @param parentId ID của thư mục cha.
     * @return Danh sách BaseItemDto là con.
     * @throws ApiException Nếu API call thất bại.
     */
    List<BaseItemDto> getItemsByParentId(String parentId) throws ApiException;

    /**
     * Lấy một trang (page) các item con (có đệ quy) từ một thư mục cha.
     * Hỗ trợ sắp xếp và phân trang (dùng cho Cột 2 - Item Grid).
     *
     * @param parentId ID của thư mục cha (null nếu là "Home").
     * @param startIndex Vị trí bắt đầu (offset).
     * @param limit Số lượng item muốn lấy.
     * @param sortOrder Thứ tự sắp xếp ("Ascending" hoặc "Descending").
     * @param sortBy Tiêu chí sắp xếp (ví dụ: "SortName").
     * @return QueryResultBaseItemDto chứa danh sách items và tổng số.
     * @throws ApiException Nếu API call thất bại.
     */
    QueryResultBaseItemDto getItemsPaginated(String parentId, int startIndex, int limit, String sortOrder, String sortBy) throws ApiException;

    /**
     * Tìm kiếm item trong toàn bộ thư viện dựa trên từ khóa.
     * Hỗ trợ sắp xếp và phân trang.
     *
     * @param keywords Từ khóa tìm kiếm.
     * @param startIndex Vị trí bắt đầu.
     * @param limit Số lượng item muốn lấy.
     * @param sortOrder Thứ tự sắp xếp.
     * @param sortBy Tiêu chí sắp xếp.
     * @return QueryResultBaseItemDto chứa danh sách items và tổng số.
     * @throws ApiException Nếu API call thất bại.
     */
    QueryResultBaseItemDto searchItemsPaginated(String keywords, int startIndex, int limit, String sortOrder, String sortBy) throws ApiException;

    /**
     * Lấy một trang (page) các item được lọc theo một thuộc tính cụ thể
     * (Tag, Studio, People, Genre) khi người dùng click vào "chip".
     *
     * @param chip      Đối tượng Tag (POJO) đã được click.
     * @param chipType  Loại chip ("TAG", "STUDIO", "PEOPLE", "GENRE").
     * @param startIndex Vị trí bắt đầu.
     * @param limit Số lượng.
     * @param recursive Đệ quy.
     * @param sortOrder Thứ tự sắp xếp.
     * @param sortBy Tiêu chí sắp xếp.
     * @return QueryResultBaseItemDto chứa danh sách items và tổng số.
     * @throws ApiException Nếu API call thất bại.
     */
    QueryResultBaseItemDto getItemsByChip(Tag chip, String chipType, Integer startIndex, Integer limit, boolean recursive,String sortOrder, String sortBy) throws ApiException;

    /**
     * Lấy thông tin chi tiết đầy đủ của một item cụ thể
     * (bao gồm cả các trường metadata như Tags, People, Studios...).
     *
     * @param itemId ID của item.
     * @return BaseItemDto với đầy đủ thông tin chi tiết.
     * @throws ApiException Nếu API call thất bại.
     */
    BaseItemDto getFullItemDetails(String itemId) throws ApiException;

    /**
     * Lấy danh sách thông tin (ImageInfo) về các ảnh (Backdrop, Primary, v.v.)
     * của một item.
     *
     * @param itemId ID của item.
     * @return Danh sách ImageInfo.
     * @throws ApiException Nếu API call thất bại.
     */
    List<ImageInfo> getItemImages(String itemId) throws ApiException;
}