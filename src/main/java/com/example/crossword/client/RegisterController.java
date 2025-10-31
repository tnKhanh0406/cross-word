package com.example.crossword.client;

import com.example.crossword.model.Message;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;

import javafx.scene.control.*;
import java.io.IOException;


public class RegisterController {
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private TextField txtDisplayName;
    @FXML private Label lblStatus;

    private Client client;
    public void setClient(Client client) {
        this.client = client;
    }

    public Client getClient() {
        return client;
    }

    public void handleRegister() throws IOException {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();
        String displayName = txtDisplayName.getText().trim();

        if (username.isEmpty() || password.isEmpty() || displayName.isEmpty()) {
            lblStatus.setText("Vui lòng nhập đầy đủ thông tin.");
            return;
        }

        String[] register = {username, password, displayName};
        client.sendMessage(new Message("register", register));
    }

    public void handleBackToLogin() {
        client.showLoginUI();
    }

    public void showError(String error) {
        Platform.runLater(() -> {
            lblStatus.setText(error);
        });
    }
}
