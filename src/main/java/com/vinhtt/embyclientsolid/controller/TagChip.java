package com.vinhtt.embyclientsolid.view.controls;

import com.vinhtt.embyclientsolid.model.Tag;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import java.util.function.Consumer;

/**
 * Custom Control (View Component) đại diện cho một "Chip"
 * (Tag, Studio, People, hoặc Genre) (UR-34, UR-36).
 * Lớp này xử lý logic hiển thị (JSON hoặc Simple) và cung cấp callbacks (onClick, onDelete).
 */
public class TagChip extends HBox {

    /**
     * Khởi tạo một Chip.
     *
     * @param tagModel    Đối tượng Tag (POJO) chứa dữ liệu.
     * @param onDelete    Hàm callback (sự kiện) được gọi khi nhấn nút Xóa (UR-34).
     * @param onClick     Hàm callback (sự kiện) được gọi khi nhấn vào chip (UR-36).
     */
    public TagChip(Tag tagModel, Consumer<Tag> onDelete, Consumer<Tag> onClick) {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(8);
        setPadding(new Insets(4, 6, 4, 10));
        getStyleClass().add("tag-view");

        // Kiểm tra xem Tag là JSON hay Simple
        if (tagModel.isJson()) {
            getStyleClass().add("tag-view-json");
            // Hiển thị dạng Key | Value (UR-34)
            Label keyLabel = new Label(tagModel.getKey());
            keyLabel.getStyleClass().add("tag-label-key");
            Separator separator = new Separator(Orientation.VERTICAL);
            separator.getStyleClass().add("tag-separator");
            Label valueLabel = new Label(tagModel.getValue());
            valueLabel.getStyleClass().add("tag-label-value");
            getChildren().addAll(keyLabel, separator, valueLabel);
        } else {
            getStyleClass().add("tag-view-simple");
            // Hiển thị dạng tên đơn giản
            Label label = new Label(tagModel.getDisplayName());
            label.getStyleClass().add("tag-label");
            getChildren().add(label);
        }

        // Nút Xóa (UR-34)
        Button deleteButton = new Button("✕");
        deleteButton.getStyleClass().add("tag-delete-button");
        deleteButton.setOnAction(e -> {
            // Ủy thác hành động xóa cho Consumer (callback)
            onDelete.accept(tagModel);
            e.consume(); // Ngăn sự kiện click lan ra HBox (tránh kích hoạt onClick)
        });
        getChildren().add(deleteButton);

        // Click vào chip (UR-36)
        this.setOnMouseClicked(e -> {
            if (onClick != null) {
                // Ủy thác hành động click cho Consumer (callback)
                onClick.accept(tagModel);
            }
        });
    }
}