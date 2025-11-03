package com.vinhtt.embyclientsolid.view.controls;

import com.vinhtt.embyclientsolid.core.IEmbySessionService;
import embyclient.model.ImageInfo;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import java.util.function.Consumer;

/**
 * Custom Control (View Component) đại diện cho một ảnh Backdrop (ảnh nền)
 * trong gallery (UR-41).
 * Lớp này hiển thị ảnh, indicator loading, và nút xóa.
 */
public class BackdropChip extends StackPane {

    private static final double THUMBNAIL_HEIGHT = 100;

    /**
     * Khởi tạo một BackdropChip (ảnh thumbnail trong gallery).
     *
     * @param imageInfo   Thông tin ảnh (ImageInfo) từ Emby, chứa index.
     * @param imageUrl    URL đầy đủ của ảnh (đã được ViewModel build).
     * @param onDelete    Hàm callback (sự kiện) được gọi khi nhấn nút Xóa.
     */
    public BackdropChip(ImageInfo imageInfo, String imageUrl, Consumer<ImageInfo> onDelete) {
        setPrefHeight(THUMBNAIL_HEIGHT);
        getStyleClass().add("backdrop-view");

        // 1. ImageView để hiển thị ảnh
        ImageView imageView = new ImageView();
        imageView.setFitHeight(THUMBNAIL_HEIGHT);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        // 2. ProgressIndicator (vòng xoay loading)
        ProgressIndicator loading = new ProgressIndicator();
        loading.setMaxSize(40, 40);

        // 3. Nút Xóa (UR-41)
        Button deleteButton = new Button("✕");
        deleteButton.getStyleClass().add("backdrop-delete-button");
        // Ủy thác hành động xóa cho Consumer (callback) đã được tiêm vào
        deleteButton.setOnAction(e -> onDelete.accept(imageInfo));
        StackPane.setAlignment(deleteButton, Pos.TOP_RIGHT);

        getChildren().addAll(loading, imageView, deleteButton);

        // 4. Tải ảnh (nền)
        if (imageUrl != null) {
            Image image = new Image(imageUrl, true); // true = tải nền
            imageView.setImage(image);

            // Ẩn/hiện vòng xoay loading dựa trên tiến trình tải ảnh
            image.progressProperty().addListener((obs, oldVal, newVal) -> {
                loading.setVisible(newVal.doubleValue() < 1.0);
            });

            // Xử lý lỗi tải ảnh
            image.errorProperty().addListener((obs, wasError, isError) -> {
                if(isError) {
                    System.err.println("Lỗi tải backdrop: " + imageUrl);
                    loading.setVisible(false);
                }
            });
        } else {
            // Không có URL, ẩn loading
            loading.setVisible(false);
        }
    }
}