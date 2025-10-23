package com.example.crossword;

import com.example.crossword.client.Client;
import com.example.crossword.model.Message;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javafx.scene.Parent;

import java.io.IOException;

public class HelloApplication extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            Client client = new Client(primaryStage);
            client.showLoginUI();

            new Thread(() -> {
                try {
                    // Thay "localhost" bằng địa chỉ IP của server nếu cần
                    client.startConnection("localhost", 12345);
                } catch (Exception e) {
                    e.printStackTrace();
                    client.showErrorAlert("Không thể kết nối tới server.");
                }
            }).start();

            // Thêm event handler cho việc đóng ứng dụng
            primaryStage.setOnCloseRequest(event -> {
                try {
                    if (client.getUser() != null) {
                        // Gửi yêu cầu đăng xuất trước khi đóng
                        Message logoutMessage = new Message("logout", client.getUser().getId());
                        client.sendMessage(logoutMessage);
                    }
                    client.closeConnection();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            Platform.exit();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}