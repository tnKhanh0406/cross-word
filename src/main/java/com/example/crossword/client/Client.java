package com.example.crossword.client;

import com.example.crossword.model.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Client {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private User user;
    private Stage primaryStage;

    private LoginController loginController = new LoginController();
    private HomeController homeController = new HomeController();
    private GameController gameController = new GameController();
    private RegisterController registerController = new RegisterController();

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

            isRunning = true;
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
                        closeConnection();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Platform.runLater(() -> {
                        showErrorAlert("Kết nối tới server bị mất.");
                        try {
                            showLoginUI();
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
        System.out.println("Nhan mes tu server: " + message.getType());
        switch (message.getType()) {
            case "login_success":
                user = (User) message.getContent();
                Platform.runLater(this::showMainUI);
                break;
            case "login_fail", "logout_success":
                Platform.runLater(() -> {
                    loginController.showError((String) message.getContent());
                });
                break;
            case "register_success":
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Đăng ký thành công! Hãy đăng nhập.");
                    alert.showAndWait();
                    showLoginUI();
                });
                break;
            case "register_fail":
                Platform.runLater(() -> {
                    registerController.showError((String) message.getContent());
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
            case "history":
                List<History> histories =(List<History>) message.getContent();
                Platform.runLater(() -> {
                    homeController.updateGameList(histories);
                });
                break;
            case "history_details":
                List<HistoryDetail> list = (List<HistoryDetail>) message.getContent();
                Platform.runLater(() -> {
                    homeController.showGameDetails(list);
                });
                break;
            case "ranking_list":
                List<User> items = (List<User>) message.getContent();
                Platform.runLater(() -> {
                   homeController.showLeaderboard(items);
                });
                break;
            case "match_request":
                System.out.println(message.getContent());
                Object[] o = (Object[]) message.getContent();
                Platform.runLater(() -> {
                    homeController.showMatchRequest((Integer) o[0], (String) o[1]);
                });
                break;
            case "match_response":
                Platform.runLater(() -> {
                    homeController.handleMatchResponse((String) message.getContent());
                });
                break;
            case "cancel_match_request":
                int requesterId = (int) message.getContent();
                Platform.runLater(() -> homeController.closeMatchRequest(requesterId));
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
            case "game_over":
                Map<String, Object> gameOverData = (Map<String, Object>) message.getContent();
                Platform.runLater(() -> {
                    try {
                        gameController.showGameOver(gameOverData);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                break;
            case "answer_result":
                Map<String, Object> data = (Map<String, Object>) message.getContent();
                boolean correct = (boolean) data.get("correct");
                int wordId = (int) data.get("word_id");
                Platform.runLater(() -> gameController.updateLbl(correct, wordId));
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
            case "rematch_invite": {
                Map<String, Object> inviteData = (Map<String, Object>) message.getContent();
                int fromId = (int) inviteData.get("from_id");
                String fromName = (String) inviteData.get("from_name");

                Platform.runLater(() -> {
                    if (gameController != null && gameController.getGameOverAlert() != null) {
                        Alert oldAlert = gameController.getGameOverAlert();
                        if (oldAlert.isShowing()) oldAlert.close();
                    }

                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Lời mời chơi lại");
                    alert.setHeaderText("Người chơi " + fromName + " mời bạn chơi lại!");
                    alert.setContentText("Bạn có muốn tham gia không?");
                    ButtonType acceptBtn = new ButtonType("Đồng ý");
                    ButtonType declineBtn = new ButtonType("Từ chối");
                    alert.getButtonTypes().setAll(acceptBtn, declineBtn);

                    Optional<ButtonType> res = alert.showAndWait();
                    Map<String, Object> response = new HashMap<>();
                    response.put("from_id", fromId);
                    response.put("accepted", res.isPresent() && res.get() == acceptBtn);

                    try {
                        sendMessage(new Message("rematch_response", response));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                break;
            }
            case "rematch_started":
                Platform.runLater(() -> {
                    if (gameController != null && gameController.getGameOverAlert() != null) {
                        Alert alert = gameController.getGameOverAlert();
                        if (alert.isShowing()) alert.close();
                    }

                    try {
                        showGameUI();
                    } catch (Exception e) {
                        e.printStackTrace();
                        showErrorAlert("Không thể khởi tạo lại ván mới!");
                    }
                });
                break;
            case "rematch_declined":
                Platform.runLater(() -> {
                    if (gameController != null && gameController.getGameOverAlert() != null) {
                        Alert alert = gameController.getGameOverAlert();
                        if (alert.isShowing()) alert.close();
                    }

                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Mời chơi lại");
                    alert.setHeaderText("Người chơi kia đã từ chối lời mời!");
                    alert.showAndWait();

                    showMainUI();
                });
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
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("khong tai duoc giao dien chinh");
        }
    }

    public void showLoginUI() {
        try {
            System.out.println("Loading login view");
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
            System.out.println("Loading Game view");
            FXMLLoader loader = new FXMLLoader(GameController.class.getResource("/com/example/crossword/GameView.fxml"));
            Parent root = loader.load();
            gameController = loader.getController();
            gameController.setClient(this);
            gameController.setLblMyName(user.getDisplayName());
            gameController.setLblOpponentName("Opponent");
            Scene scene = new Scene(root);

            primaryStage.setScene(scene);
            primaryStage.setTitle("Cross Word - Game Room");
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Không thể tải giao diện phòng chơi.");
        }
    }

    public void showRegisterUI(ActionEvent actionEvent) throws IOException {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/crossword/RegisterView.fxml"));
            Scene scene = new Scene(loader.load());
            registerController = loader.getController();
            registerController.setClient(this);
            Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Đăng ký tài khoản");
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Khong the tai giao dien dang ky");
        }
    }

    public void closeConnection() throws IOException {
        isRunning = false;
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
