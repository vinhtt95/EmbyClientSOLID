package com.vinhtt.embyclientsolid.data;

import com.vinhtt.embyclientsolid.model.Tag;
import embyclient.ApiException;
import embyclient.model.BaseItemDto;
import embyclient.model.ImageInfo;
import embyclient.model.QueryResultBaseItemDto;

import java.util.List;

/**
 * Interface cho việc ĐỌC (Query) dữ liệu Item chính từ Emby.
 * Bao gồm: Lấy thư viện, lấy item con, chi tiết item, ảnh, và tìm kiếm.
 * (UR-15, UR-17, UR-19, UR-20, UR-23, UR-30, UR-36, UR-41).
 */
public interface IItemRepository {

    /**
     * Lấy các thư viện gốc (Views) của user hiện tại.
     * (UR-15).
     * @return Danh sách BaseItemDto là các thư mục gốc.
     * @throws ApiException Nếu API call thất bại.
     */
    List<BaseItemDto> getRootViews() throws ApiException;

    /**
     * Lấy các items con dựa trên parentId (thường dùng cho lazy loading cây).
     * (UR-17).
     * @param parentId ID của thư mục cha.
     * @return Danh sách BaseItemDto là con.
     * @throws ApiException Nếu API call thất bại.
     */
    List<BaseItemDto> getItemsByParentId(String parentId) throws ApiException;

    /**
     * Lấy các items con đầy đủ dựa trên parentId VỚI PHÂN TRANG và SẮP XẾP.
     * (UR-19, UR-21, UR-22, UR-23).
     * @param parentId ID của thư mục cha (null nếu là "Home").
     * @param startIndex Vị trí bắt đầu.
     * @param limit Số lượng item muốn lấy.
     * @param sortOrder Thứ tự sắp xếp ("Ascending" hoặc "Descending").
     * @param sortBy Tiêu chí sắp xếp (ví dụ: "SortName").
     * @return QueryResultBaseItemDto chứa danh sách items và tổng số.
     * @throws ApiException Nếu API call thất bại.
     */
    QueryResultBaseItemDto getItemsPaginated(String parentId, int startIndex, int limit, String sortOrder, String sortBy) throws ApiException;

    /**
     * Tìm kiếm items dựa trên từ khóa với phân trang.
     * (UR-20).
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
     * Lấy danh sách item dựa trên một chip (Tag, Studio, People, Genre).
     * (UR-36).
     * @param chip      Đối tượng Tag đã được click.
     * @param chipType  Loại chip ("TAG", "STUDIO", "PEOPLE", "GENRE").
     * @param startIndex Vị trí bắt đầu.
     * @param limit Số lượng.
     * @param recursive Đệ quy.
     * @param sortOrder Thứ tự sắp xếp ("Ascending" hoặc "Descending").
     * @param sortBy Tiêu chí sắp xếp (ví dụ: "SortName").
     * @return QueryResultBaseItemDto chứa danh sách items và tổng số.
     * @throws ApiException Nếu API call thất bại.
     */
    QueryResultBaseItemDto getItemsByChip(Tag chip, String chipType, Integer startIndex, Integer limit, boolean recursive,String sortOrder, String sortBy) throws ApiException;

    /**
     * Lấy thông tin chi tiết đầy đủ của một item.
     * (UR-30).
     * @param itemId ID của item.
     * @return BaseItemDto với đầy đủ thông tin chi tiết.
     * @throws ApiException Nếu API call thất bại.
     */
    BaseItemDto getFullItemDetails(String itemId) throws ApiException;

    /**
     * Lấy danh sách ảnh (Backdrop, Primary, v.v.) của một item.
     * (UR-41).
     * @param itemId ID của item.
     * @return Danh sách ImageInfo.
     * @throws ApiException Nếu API call thất bại.
     */
    List<ImageInfo> getItemImages(String itemId) throws ApiException;
}