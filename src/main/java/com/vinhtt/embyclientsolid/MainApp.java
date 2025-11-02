package com.vinhtt.embyclientsolid;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Lớp chính của ứng dụng EmbyClientSOLID.
 * Khởi tạo ứng dụng JavaFX và sẽ thiết lập Dependency Injection (ở các giai đoạn sau).
 * Giai đoạn 0: Chỉ hiển thị một cửa sổ trống để xác nhận việc thiết lập thành công.
 */
public class MainApp extends Application {

    /**
     * Phương thức start, entry point cho JavaFX.
     *
     * @param primaryStage Stage chính của ứng dụng.
     * @throws Exception
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Tạm thời tạo một Scene trống để kiểm tra Giai đoạn 0
        StackPane root = new StackPane();
        root.getChildren().add(new Label("Giai đoạn 0: Project SOLID đã khởi chạy!"));
        Scene scene = new Scene(root, 640, 480);

        primaryStage.setTitle("EmbyClientSOLID (Giai đoạn 0)");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Main method, entry point của ứng dụng.
     *
     * @param args Đối số dòng lệnh.
     */
    public static void main(String[] args) {
        launch(args); //
    }
}