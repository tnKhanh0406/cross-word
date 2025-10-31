package com.example.crossword.client;

import com.example.crossword.model.Message;
import com.example.crossword.model.Word;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.application.Platform;
import javafx.util.Pair;

import java.io.*;
import java.util.*;

public class GameController {
    @FXML private Label lblMyName;
    @FXML private Label lblOpponentName;
    @FXML private Label lblTimer;
    @FXML private Label lblMyScore;
    @FXML private Label lblOpponentScore;

    @FXML private TabPane tabWords;
    @FXML private TextArea lvChatMessages;

    @FXML private TextField txtChatInput;
    @FXML private Button btnSendChat;

    @FXML private Label lblStatus;
    @FXML private Button quitButton;
    @FXML private VBox vboxCrosswordGrid;

    private int myScore = 0;
    private int opponentScore = 0;
    private int totalQuestions = 10;
    private int remainingSeconds = 300; // 5 min = 300s

    private Timer gameTimer;

    private List<Word> words = new ArrayList<>();

    private int gameId;

    private Client client;

    private Alert gameOverAlert;

    private Map<Integer, Pair<TextField, Button>> wordInputMap = new HashMap<>();
    private Map<Integer, Tab> wordTabMap = new HashMap<>();


    public Alert getGameOverAlert() {
        return gameOverAlert;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public void setWords(List<Word> words) {
        this.words = words;
        if (tabWords != null) {
            Platform.runLater(() -> {
                tabWords.getTabs().clear();
                initTabs();
                initCrosswordGrid();
            });
        }
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }

    public void setLblMyName(String lblMyName) {
        this.lblMyName.setText(lblMyName);
    }

    public void setLblOpponentName(String lblOpponentName) {
        this.lblOpponentName.setText(lblOpponentName);
    }

    @FXML
    public void initialize() {
        lvChatMessages.setEditable(false);
        lblTimer.setText(formatTime(remainingSeconds));
        initTabs();
        initCrosswordGrid();
    }

    private void initCrosswordGrid() {
        vboxCrosswordGrid.getChildren().clear();

        for (int i = 0; i < words.size(); i++) {
            Word w = words.get(i);

            // HBox cho từng hàng
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(2, 0, 2, 0));

            // Thêm nhãn số thứ tự
            Label indexLabel = new Label((i + 1) + ".");
            indexLabel.setPrefWidth(25);
            indexLabel.setAlignment(Pos.CENTER_RIGHT);
            indexLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #444;");

            row.getChildren().add(indexLabel);

            // Thêm các ô chữ
            int wordLength = w.getWord().length();

            for (int j = 0; j < wordLength; j++) {
                Label cell = new Label();
                cell.setPrefSize(32, 32);
                cell.setAlignment(Pos.CENTER);
                cell.setStyle(
                        "-fx-border-color: #bbb;" +
                                "-fx-border-radius: 4;" +
                                "-fx-background-radius: 4;" +
                                "-fx-background-color: #ffffff;" +
                                "-fx-font-size: 14px;"
                );
                row.getChildren().add(cell);
            }

            vboxCrosswordGrid.getChildren().add(row);
        }
    }

    private void initTabs() {
        for (int i = 0; i < words.size(); i++) {
            Word w = words.get(i);
            Tab tab = new Tab("Từ " + (i + 1));

            VBox content = new VBox(10);
            content.setStyle("-fx-padding: 12;");

            Label hintLabel = new Label("Gợi ý: " + w.getHint());
            hintLabel.setWrapText(true);

            Label blankLabel = new Label("Số chữ: " + w.getWord().length());
            blankLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #555;");

            // Hiển thị các chữ cái có thể dùng
            String letters = String.join(", ", getSortedUniqueLetters(w.getWord()));
            Label lettersLabel = new Label("Chữ cái: " + letters);

            // TextField nhập câu trả lời
            TextField txtAnswer = new TextField();
            txtAnswer.setPromptText("Nhập đáp án của bạn...");

            Button btnSubmit = new Button("Gửi");
            btnSubmit.setOnAction(e -> {
                try {
                    handleSubmitWord(w, txtAnswer.getText());
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });

            HBox inputBox = new HBox(8, txtAnswer, btnSubmit);
            content.getChildren().addAll(hintLabel, blankLabel, lettersLabel, inputBox);

            tab.setContent(content);
            tabWords.getTabs().add(tab);
            wordInputMap.put(w.getId(), new Pair<>(txtAnswer, btnSubmit));
            wordTabMap.put(w.getId(), tab);
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

    private void handleSubmitWord(Word w, String answer) throws IOException {
        if (answer == null || answer.trim().isEmpty()) {
            lblStatus.setText("Hãy nhập đáp án trước khi gửi!");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("game_id", gameId);
        data.put("word_id", w.getId());
        data.put("answer", answer.trim());

        client.sendMessage(new Message("submit_word", data));
        lblStatus.setText("Đã gửi đáp án cho từ \"" + w.getHint() + "\"");
    }

    public void showGameOver(Map<String, Object> data) throws IOException {
        int winnerId = (int) data.get("winner_id");
        int myId = client.getUser().getId();
        String result = (String) data.get("result");

        String message;
        if ("draw".equals(result)) {
            message = "Trận đấu hòa!";
        } else if (winnerId == myId) {
            message = "Bạn đã chiến thắng!";
        } else {
            message = "Bạn đã thua!";
        }
        String reason = (String) data.get("reason");

        gameOverAlert = new Alert(Alert.AlertType.CONFIRMATION);
        gameOverAlert.setTitle("Kết thúc trận đấu");
        gameOverAlert.setHeaderText(message);
        StringBuilder content = new StringBuilder();
        if (reason != null && !reason.isEmpty()) {
            content.append(reason).append("\n\n");
        }
        content.append("Điểm của bạn: ").append(lblMyScore.getText())
                .append("\nĐiểm đối thủ: ").append(lblOpponentScore.getText())
                .append("\n\nBạn muốn làm gì tiếp theo?");

        gameOverAlert.setContentText(content.toString());

//        ButtonType rematchBtn = new ButtonType("Mời chơi lại");
        ButtonType homeBtn = new ButtonType("Về trang chính");
//        gameOverAlert.getButtonTypes().setAll(rematchBtn, homeBtn);
        gameOverAlert.getButtonTypes().setAll(homeBtn);

        gameOverAlert.resultProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null) return;
            try {
//                if (newValue == rematchBtn) {
//                    Map<String, Object> dataOut = new HashMap<>();
//                    dataOut.put("opponent_id", data.get("opponent_id"));
//                    client.sendMessage(new Message("rematch_request", dataOut));
//                } else {
//                    client.sendMessage(new Message("back_to_home", null));
//                    client.showMainUI();
//                }
                client.sendMessage(new Message("back_to_home", null));
                client.showMainUI();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        gameOverAlert.show();
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

    public void updateLbl(boolean correct, int wordId) {
        Platform.runLater(() -> {
            lblStatus.setText(correct ? "Bạn trả lời đúng!" : "Sai mất rồi!");
            Pair<TextField, Button> pair = wordInputMap.get(wordId);
            Tab tab = wordTabMap.get(wordId);
            if(correct) {
                if (pair != null && tab != null) {
                    TextField txt = pair.getKey();
                    Button btn = pair.getValue();

                    txt.setDisable(true);
                    btn.setDisable(true);

                    // Đổi màu tab sang xanh lá cây
                    tab.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
                }
            } else {
                tab.setStyle("-fx-background-color: red; -fx-text-fill: white;");
            }
        });
    }

    public void updateTime(int remaining) {
        Platform.runLater(() -> {
            lblTimer.setText(formatTime(remaining));
            if (remaining <= 0) {
                lblStatus.setText("Hết thời gian!");
                endGame();
            }
        });
    }
}