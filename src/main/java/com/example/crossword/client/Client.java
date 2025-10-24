package com.example.crossword.client;

import com.example.crossword.model.Message;
import com.example.crossword.model.User;
import com.example.crossword.model.Word;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.Map;

public class Client {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private User user;
    private Stage primaryStage;

    private LoginController loginController = new LoginController();
    private HomeController homeController = new HomeController();
    private GameController gameController = new GameController();

    private volatile boolean isRunning = true;

    public Client(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public User getUser() {
        return user;
    }

    public void showErrorAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public void startConnection(String address, int port) {
        try {
            socket = new Socket(address, port);
            in = new ObjectInputStream(socket.getInputStream());
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();

            isRunning = true; // Đặt lại isRunning thành true
            listenForMessages();
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Không thể kết nối tới server.");
        }
    }

    private void listenForMessages() {
        new Thread(() -> {
            try {
                while (isRunning) {
                    Message message = (Message) in.readObject();
                    if (message != null) {
                        handleMessage(message);
                    }
                }
            } catch (IOException | ClassNotFoundException ex) {
                if (isRunning) {
                    ex.printStackTrace();
                    try {
                        closeConnection(); // Đóng kết nối hiện tại
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Platform.runLater(() -> {
                        showErrorAlert("Kết nối tới server bị mất.");
                        try {
                            showLoginUI(); // Hiển thị giao diện đăng nhập và tái kết nối
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                } else {
                    System.out.println("Đã đóng kết nối, dừng luồng lắng nghe.");
                }
            }
        }).start();
    }

    public void sendMessage(Message message) throws IOException {
        out.writeObject(message);
        out.flush();
    }

    private void handleMessage(Message message) {
        System.out.println("📨 Received from server: " + message.getType());
        switch (message.getType()) {
            case "login_success":
                user = (User) message.getContent();
                Platform.runLater(this::showMainUI);
                break;
            case "login_fail":
                Platform.runLater(() -> {
                    loginController.showError((String) message.getContent());
                });
                break;
            case "status_update":
                Platform.runLater(() -> {
                   homeController.updateStatus((String) message.getContent());
                });
                break;
            case "user_list":
                System.out.println(message.getContent());
                List<User> users = (List<User>) message.getContent();
                Platform.runLater(() -> {
                    homeController.updateUserList(users);
                });
                break;
            case "match_request":
                Platform.runLater(() -> {
                    homeController.showMatchRequest((int) message.getContent());
                });
                break;
            case "match_response":
                Platform.runLater(() -> {
                    homeController.handleMatchResponse((String) message.getContent());
                });
                break;
            case "game_start": {
                Map<String, Object> data = (Map<String, Object>) message.getContent();
                int gameId = (int) data.get("game_id");
                List<Word> words = (List<Word>) data.get("words");
                Platform.runLater(() -> {
                    showGameUI();
                    gameController.setGameId(gameId);
                    gameController.setWords(words);
                });
                break;
            }
            case "chat":
                Platform.runLater(() -> {
                   gameController.updateChat((String) message.getContent());
                });
                break;
            case "word_list":
                List<Word> words2 = (List<Word>) message.getContent();
                Platform.runLater(() -> {
                    gameController.setWords(words2);
                });
//            case "word_result":
//                Map<String, Object> resultData = (Map<String, Object>) message.getContent();
//                Platform.runLater(() -> gameController.updateWordResult(resultData));
//                break;

            case "game_over":
                Map<String, Object> gameOverData = (Map<String, Object>) message.getContent();
                Platform.runLater(() -> gameController.showGameOver(gameOverData));
                break;
            case "answer_result":
                Map<String, Object> data = (Map<String, Object>) message.getContent();
                boolean correct = (boolean) data.get("correct");
                Platform.runLater(() -> gameController.updateLbl(correct));
                break;
            case "update_score":
                Map<String, Object> scoreData = (Map<String, Object>) message.getContent();
                int your = (int) scoreData.get("your_score");
                int opp = (int) scoreData.get("opponent_score");
                Platform.runLater(() -> gameController.updateScores(your, opp));
                break;
            case "time_update":
                Map<String, Object> dataTime = (Map<String, Object>) message.getContent();
                int remaining = (int) dataTime.get("remaining_time");
                Platform.runLater(() -> gameController.updateTime(remaining));
                break;
        }
    }

    public void showMainUI() {
        try {
            System.out.println("Loading home view");
            FXMLLoader loader = new FXMLLoader(HomeController.class.getResource("/com/example/crossword/HomeView.fxml"));
            Parent root = loader.load();
            homeController = loader.getController();
            homeController.setClient(this);
            homeController.setDisplayName(user.getDisplayName());
            Scene scene = new Scene(root);
            primaryStage.setScene(scene);
            primaryStage.setTitle("Cross Word - Main");
            primaryStage.setMinWidth(400);
            primaryStage.setMinHeight(300);
            primaryStage.show();
            sendMessage(new Message("get_users", null));
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("khong tai duoc giao dien chinh");
        }
    }

    public void showLoginUI() {
        try {
            System.out.println("Loading LoginUI.fxml...");
            FXMLLoader loader = new FXMLLoader(LoginController.class.getResource("/com/example/crossword/LoginView.fxml"));
            Parent root = loader.load();
            loginController = loader.getController();
            loginController.setClient(this);
            Scene scene = new Scene(root);

            primaryStage.setScene(scene);
            primaryStage.setTitle("Cross Word - Login");
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Không thể tải giao diện đăng nhập.");
        }
    }

    public void showGameUI() {
        try {
            System.out.println("Loading Game view...");
            FXMLLoader loader = new FXMLLoader(GameController.class.getResource("/com/example/crossword/GameView.fxml"));
            Parent root = loader.load();
            gameController = loader.getController();
            gameController.setClient(this);
            Scene scene = new Scene(root);

            primaryStage.setScene(scene);
            primaryStage.setTitle("Cross Word - Game Room");
            primaryStage.show();
            System.out.println("[CLIENT] GameView.fxml loaded successfully.");
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Không thể tải giao diện phòng chơi.");
        }
    }

    public void closeConnection() throws IOException {
        isRunning = false; // Dừng luồng lắng nghe
        if (in != null) {
            in.close();
        }
        if (out != null) {
            out.close();
        }
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}
