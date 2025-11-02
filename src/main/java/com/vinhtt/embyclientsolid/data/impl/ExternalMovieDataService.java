package com.vinhtt.embyclientsolid.data.impl;

import com.vinhtt.embyclientsolid.data.IExternalDataService;
import com.vinhtt.embyclientsolid.model.ReleaseInfo;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.OffsetDateTime;

/**
 * Triển khai (Implementation) của IExternalDataService.
 * Chịu trách nhiệm gọi API bên ngoài (localhost:8081).
 * Logic được chuyển từ RequestEmby.getDateRelease.
 * (UR-38).
 */
public class ExternalMovieDataService implements IExternalDataService {

    // API URL này có thể được chuyển ra IConfigurationService nếu muốn
    private static final String API_URL_TEMPLATE = "http://localhost:8081/movies/movie/date/?movieCode=";

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

                    // Logic parse từ RequestEmby
                    JSONObject dataObject = jsonResponse.optJSONObject("data");
                    if (dataObject != null) {
                        String dateValue = dataObject.optString("releaseDate", null);
                        String actressName = dataObject.optString("actressName", null);

                        OffsetDateTime odt = null;
                        if (dateValue != null && !dateValue.equals("null")) {
                            try {
                                odt = OffsetDateTime.parse(dateValue);
                            } catch (Exception e) {
                                System.err.println("Lỗi parse ngày: " + dateValue);
                            }
                        }
                        return new ReleaseInfo(odt, actressName); // Dùng Model mới

                    } else {
                        // Fallback cho cấu trúc cũ
                        String dataValue = jsonResponse.optString("data", null);
                        OffsetDateTime odt = null;
                        if (dataValue != null && !dataValue.equals("null")) {
                            try {
                                odt = OffsetDateTime.parse(dataValue);
                            } catch (Exception e) {
                                System.err.println("Lỗi parse ngày (fallback): " + dataValue);
                            }
                        }
                        return new ReleaseInfo(odt, null); // Dùng Model mới
                    }
                }
            } else {
                System.err.println("API bên ngoài thất bại (Code " + responseCode + ") cho code: " + itemCode);
                return null; // Lỗi API (ví dụ 404)
            }
        } catch (Exception e) {
            System.err.println("Lỗi kết nối API bên ngoài: " + e.getMessage());
            return null; // Lỗi kết nối hoặc lỗi parse JSON
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}