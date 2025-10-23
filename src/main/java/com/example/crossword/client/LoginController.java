package com.example.crossword.client;

import com.example.crossword.model.Message;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.*;

public class LoginController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblStatus;


    private Client client;
    public void setClient(Client client) {
        this.client = client;
    }

    public Client getClient() {
        return client;
    }

    @FXML
    private void handleLogin() throws IOException {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();
        if (username.isEmpty() || password.isEmpty()) {
            lblStatus.setText("Vui lòng nhập đầy đủ thông tin.");
            return;
        }
        String[] credentials = {username, password};
        client.sendMessage(new Message("login", credentials));
    }

    public void showError(String error) {
        Platform.runLater(() -> {
            lblStatus.setText(error);
        });
    }
}
