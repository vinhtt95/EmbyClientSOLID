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
 * Custom Control (Chip) để hiển thị một ảnh Backdrop.
 * (UR-41).
 */
public class BackdropChip extends StackPane {

    private static final double THUMBNAIL_HEIGHT = 100;

    // (Lưu ý: Chúng ta đang vi phạm DIP ở đây một chút vì
    // không thể inject IEmbySessionService vào một custom control.
    // Cách tốt hơn là ItemDetailViewModel sẽ cung cấp URL đầy đủ.)

    // (Cập nhật: Chúng ta sẽ truyền ItemID vào và tự build URL.
    // Vẫn cần IEmbySessionService...
    // -> Quyết định: Truyền thẳng URL vào (ViewModel sẽ build URL))

    /**
     * Khởi tạo BackdropChip.
     * @param imageInfo   Thông tin ảnh.
     * @param imageUrl    URL đầy đủ của ảnh (đã được ViewModel build).
     * @param onDelete    Hàm callback khi nhấn nút Xóa.
     */
    public BackdropChip(ImageInfo imageInfo, String imageUrl, Consumer<ImageInfo> onDelete) {
        setPrefHeight(THUMBNAIL_HEIGHT);
        getStyleClass().add("backdrop-view");

        // 1. Ảnh
        ImageView imageView = new ImageView();
        imageView.setFitHeight(THUMBNAIL_HEIGHT);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        // 2. Loading
        ProgressIndicator loading = new ProgressIndicator();
        loading.setMaxSize(40, 40);

        // 3. Nút Xóa (UR-41)
        Button deleteButton = new Button("✕");
        deleteButton.getStyleClass().add("backdrop-delete-button");
        deleteButton.setOnAction(e -> onDelete.accept(imageInfo));
        StackPane.setAlignment(deleteButton, Pos.TOP_RIGHT);

        getChildren().addAll(loading, imageView, deleteButton);

        // 4. Tải ảnh
        if (imageUrl != null) {
            Image image = new Image(imageUrl, true); // true = tải nền
            imageView.setImage(image);

            image.progressProperty().addListener((obs, oldVal, newVal) -> {
                loading.setVisible(newVal.doubleValue() < 1.0);
            });
            image.errorProperty().addListener((obs, wasError, isError) -> {
                if(isError) {
                    System.err.println("Lỗi tải backdrop: " + imageUrl);
                    loading.setVisible(false);
                }
            });
        } else {
            loading.setVisible(false);
            // (Có thể set ảnh placeholder)
        }
    }
}