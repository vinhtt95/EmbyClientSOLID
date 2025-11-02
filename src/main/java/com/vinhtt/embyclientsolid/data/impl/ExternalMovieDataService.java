package com.vinhtt.embyclientsolid.data.impl;

import com.vinhtt.embyclientsolid.data.IExternalDataService;
import com.vinhtt.embyclientsolid.model.ReleaseInfo;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Triển khai (Implementation) của IExternalDataService.
 * Chịu trách nhiệm gọi API bên ngoài (localhost:8081).
 * Logic được chuyển từ RequestEmby.getDateRelease.
 * (UR-38).
 */
public class ExternalMovieDataService implements IExternalDataService {

    private static final String API_URL_TEMPLATE = "http://localhost:8081/movies/movie/date/?movieCode=";

    private static final DateTimeFormatter LOCAL_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S");

    /**
     * Parse một chuỗi ngày tháng (có thể là ISO OffsetDateTime hoặc
     * định dạng LocalDateTime tùy chỉnh) về OffsetDateTime.
     *
     * @param dateValue Chuỗi ngày tháng từ API.
     * @return OffsetDateTime đã parse, hoặc null nếu thất bại.
     */
    private OffsetDateTime parseFlexibleDate(String dateValue) {
        if (dateValue == null || dateValue.equals("null")) {
            return null;
        }

        // 1. Thử parse theo định dạng ISO chuẩn (ví dụ: 2025-06-06T00:00:00+07:00)
        try {
            return OffsetDateTime.parse(dateValue);
        } catch (Exception e) {
            // Không phải ISO, tiếp tục
        }

        // 2. Thử parse theo định dạng API tùy chỉnh (ví dụ: 2025-06-06 00:00:00.0)
        try {
            LocalDateTime ldt = LocalDateTime.parse(dateValue, LOCAL_DATE_TIME_FORMATTER);
            // Giả định ngày API trả về là giờ hệ thống
            return ldt.atZone(ZoneId.systemDefault()).toOffsetDateTime();
        } catch (Exception e) {
            System.err.println("Lỗi parse ngày không xác định: " + dateValue);
        }

        return null;
    }


    @Override
    public ReleaseInfo fetchReleaseInfoByCode(String itemCode) {
        String apiUrl = API_URL_TEMPLATE + itemCode;
        HttpURLConnection connection = null;
        try {
            URL url = new URL(apiUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    JSONObject jsonResponse = new JSONObject(response.toString());

                    JSONObject dataObject = jsonResponse.optJSONObject("data");
                    if (dataObject != null) {
                        String dateValue = dataObject.optString("releaseDate", null);
                        String actressName = dataObject.optString("actressName", null);

                        OffsetDateTime odt = parseFlexibleDate(dateValue);
                        return new ReleaseInfo(odt, actressName);

                    } else {
                        String dataValue = jsonResponse.optString("data", null);
                        OffsetDateTime odt = parseFlexibleDate(dataValue);
                        return new ReleaseInfo(odt, null);
                    }
                }
            } else {
                System.err.println("API bên ngoài thất bại (Code " + responseCode + ") cho code: " + itemCode);
                return null;
            }
        } catch (Exception e) {
            System.err.println("Lỗi kết nối API bên ngoài: " + e.getMessage());
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}