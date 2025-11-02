package com.vinhtt.embyclientsolid;

/**
 * Lớp Launcher để tránh lỗi JavaFX runtime components khi tạo fat JAR.
 * Đây là Entry Point được định nghĩa trong pom.xml (maven-shade-plugin).
 */
public class Launcher {

    /**
     * Main method của Launcher, gọi main method của MainApp.
     * @param args Đối số dòng lệnh.
     */
    public static void main(String[] args) {
        // Trỏ đến MainApp của package mới
        MainApp.main(args);
    }
}