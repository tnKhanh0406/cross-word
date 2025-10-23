package com.example.crossword.client;

import com.example.crossword.model.Message;
import com.example.crossword.model.Word;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.application.Platform;
import javafx.scene.text.Text;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import javafx.animation.*;

import javafx.util.Duration;

public class GameController {
    @FXML private Label lblTimer;
    @FXML private Label lblMyScore;
    @FXML private Label lblOpponentScore;

    @FXML private TabPane tabWords;
    @FXML private TextArea lvChatMessages;

    @FXML private TextField txtChatInput;
    @FXML private Button btnSendChat;

    @FXML private Label lblStatus;
    @FXML private Button quitButton;

    private int myScore = 0;
    private int opponentScore = 0;
    private int totalQuestions = 10;
    private int remainingSeconds = 300; // 5 phút = 300s

    private Timer gameTimer;

    private List<Word> words = new ArrayList<>();

    private String myUsername;
    private String opponentUsername;

    private int gameId;

    private Client client;

    public void setClient(Client client) {
        this.client = client;
    }

    public void setWords(List<Word> words) {
        this.words = words;
    }

    @FXML
    public void initialize() {
        lvChatMessages.setEditable(false);
        lblTimer.setText(formatTime(remainingSeconds));
        initTabs();
        startTimer();
    }

    private void initTabs() {
        for (int i = 0; i < words.size(); i++) {
            Word w = words.get(i);
            Tab tab = new Tab("Từ " + (i + 1));

            VBox content = new VBox(10);
            content.setStyle("-fx-padding: 12;");

            Label hintLabel = new Label("Gợi ý: " + w.getHint());
            hintLabel.setWrapText(true);

            // Placeholder cho ô chữ (controller khác sẽ sinh ô vuông theo số chữ)
            Label blankLabel = new Label("Số chữ: " + w.getWord().length());
            blankLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #555;");

            // Hiển thị các chữ cái có thể dùng
            String letters = String.join(", ", getSortedUniqueLetters(w.getWord()));
            Label lettersLabel = new Label("Chữ cái: " + letters);

            // TextField nhập câu trả lời
            TextField txtAnswer = new TextField();
            txtAnswer.setPromptText("Nhập đáp án của bạn...");

            Button btnSubmit = new Button("Gửi");
            btnSubmit.setOnAction(e -> handleSubmitWord(w, txtAnswer.getText()));

            HBox inputBox = new HBox(8, txtAnswer, btnSubmit);
            content.getChildren().addAll(hintLabel, blankLabel, lettersLabel, inputBox);

            tab.setContent(content);
            tabWords.getTabs().add(tab);
        }
    }

    private String formatTime(int seconds) {
        int min = seconds / 60;
        int sec = seconds % 60;
        return String.format("%02d:%02d", min, sec);
    }

    @FXML
    public void handleSendChat() throws IOException {
        String message = txtChatInput.getText();
        if (!message.isEmpty()) {
            Message chatMessage = new Message("chat", message);
            client.sendMessage(chatMessage);
            txtChatInput.clear();
        }
    }

    public void updateChat(String message) {
        Platform.runLater(() -> {
            lvChatMessages.appendText(message + "\n");
        });
    }

    @FXML
    public void handleQuitGame() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Thoát Trò Chơi");
            alert.setHeaderText(null);
            alert.setContentText("Bạn có chắc chắn muốn thoát trò chơi không?");
            ButtonType yesButton = new ButtonType("Có", ButtonBar.ButtonData.YES);
            ButtonType noButton = new ButtonType("Không", ButtonBar.ButtonData.NO);
            alert.getButtonTypes().setAll(yesButton, noButton);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == yesButton) {
                Message quitMessage = new Message("quit_game", null);
                try {
                    client.sendMessage(quitMessage);
                    client.showMainUI();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void handleTimeOut() throws IOException {
        //
    }

    private void handleSubmitWord(Word w, String answer) {
        if (answer == null || answer.trim().isEmpty()) {
            lblStatus.setText("Hãy nhập đáp án trước khi gửi!");
            return;
        }

        // Gửi lên server để kiểm tra
//        client.sendMessage(new Message("submit_word", gameId, w.getId(), answer.trim()));

        lblStatus.setText("Đã gửi đáp án cho từ \"" + w.getWord() + "\"");
    }

    private void startTimer() {
        gameTimer = new Timer();
        gameTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    remainingSeconds--;
                    lblTimer.setText(formatTime(remainingSeconds));
                    if (remainingSeconds <= 0) {
                        gameTimer.cancel();
                        lblStatus.setText("Hết thời gian!");
                        endGame();
                    }
                });
            }
        }, 1000, 1000);
    }

    private List<String> getSortedUniqueLetters(String word) {
        Set<Character> set = new HashSet<>();
        for (char c : word.toUpperCase().toCharArray()) set.add(c);
        List<String> result = new ArrayList<>();
        for (char c : set) result.add(String.valueOf(c));
        Collections.sort(result);
        return result;
    }

    private void endGame() {
        // Xử lý khi hết thời gian hoặc kết thúc
        lblStatus.setText("Trò chơi kết thúc!");
        if (gameTimer != null) gameTimer.cancel();
    }

    public void updateScores(int my, int opponent) {
        Platform.runLater(() -> {
            myScore = my;
            opponentScore = opponent;
            lblMyScore.setText(my + " / " + totalQuestions);
            lblOpponentScore.setText(opponent + " / " + totalQuestions);
        });
    }
}

