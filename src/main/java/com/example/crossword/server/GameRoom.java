package com.example.crossword.server;

import com.example.crossword.model.Message;
import com.example.crossword.model.Word;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public class GameRoom {
    private int gameId;
    private ClientHandler player1;
    private ClientHandler player2;
    private int p1Score;
    private int p2Score;

    private boolean p1WantsRematch = false;
    private boolean p2WantsRematch = false;
    private final int MAX_ROUNDS = 10;
    private final int GAME_TIMEOUT = 150;
    private int remainingTime = GAME_TIMEOUT;

    private transient Timer timer;

    public Timer getTimer() {
        return timer;
    }

    public void setTimer(Timer timer) {
        this.timer = timer;
    }

    private List<Word> words; // 10 từ được chọn ngẫu nhiên từ DB

    private UserDAO userDAO = new UserDAO();
    private GameDAO gameDAO = new GameDAO();
    private WordDAO wordDAO = new WordDAO();
    public GameRoom(ClientHandler p1, ClientHandler p2) {
        this.player1 = p1;
        this.player2 = p2;
        this.words = wordDAO.getRandomWords(MAX_ROUNDS);
        this.p1Score = 0;
        this.p2Score = 0;
        this.gameId = gameDAO.createGame(this.player1.getUser().getId(), this.player2.getUser().getId());
        gameDAO.createGameDetails(this.gameId, this.words);
        System.out.println("Game created with ID = " + this.gameId);
    }

    public void startGame() {
        player1.getUser().setStatus("busy");
        player2.getUser().setStatus("busy");

        player1.sendMessage(new Message("game_start", Map.of(
                "game_id", this.gameId,
                "words", this.words
        )));
        player2.sendMessage(new Message("game_start", Map.of(
                "game_id", this.gameId,
                "words", this.words
        )));
        startGameTimer();
    }

    public void endGame() {
        try {
//            if (isGameFinished) return; // tránh gọi 2 lần
//            isGameFinished = true;

            int winnerId = 0;
            String resultType;

            if (p1Score == p2Score) {
                resultType = "draw";
            } else if (p1Score > p2Score) {
                resultType = "p1_win";
                winnerId = player1.getUser().getId();
            } else {
                resultType = "p2_win";
                winnerId = player2.getUser().getId();
            }

            gameDAO.finishGame(gameId, winnerId, resultType);

            // Cập nhật điểm tích lũy
            if (resultType.equals("p1_win")) {
                userDAO.addTotalPoint(player1.getUser().getId(), 1);
                userDAO.addTotalWin(player1.getUser().getId(), 1);
            } else if (resultType.equals("p2_win")) {
                userDAO.addTotalPoint(player2.getUser().getId(), 1);
                userDAO.addTotalWin(player2.getUser().getId(), 1);
            } else {
                userDAO.addTotalPoint(player1.getUser().getId(), 0.5);
                userDAO.addTotalPoint(player2.getUser().getId(), 0.5);
            }

            // Gửi thông tin kết thúc game riêng biệt
            Map<String, Object> endDataP1 = new HashMap<>();
            Map<String, Object> endDataP2 = new HashMap<>();

            endDataP1.put("winner_id", winnerId);
            endDataP2.put("winner_id", winnerId);

            endDataP1.put("your_score", p1Score);
            endDataP1.put("opponent_score", p2Score);
            endDataP2.put("your_score", p2Score);
            endDataP2.put("opponent_score", p1Score);

            endDataP1.put("result", resultType);
            endDataP2.put("result", resultType);

            player1.sendMessage(new Message("game_over", endDataP1));
            player2.sendMessage(new Message("game_over", endDataP2));

            // Đánh dấu game đã kết thúc
            System.out.println("[SERVER] Game " + gameId + " ended: " + resultType);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startGameTimer() {
        timer = new Timer(true); // true = daemon thread, tự dừng khi server tắt
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                remainingTime--;

                // Gửi thời gian còn lại cho cả 2 người chơi
                Map<String, Object> data = new HashMap<>();
                data.put("remaining_time", remainingTime);
                Message msg = new Message("time_update", data);
                broadcast(msg);

                // Khi hết thời gian thì kết thúc game
                if (remainingTime <= 0) {
                    timer.cancel();
                    endGame();
                }
            }
        }, 1000, 1000);
    }


    public void handlePlayerDisconnect(ClientHandler disconnectedPlayer) throws SQLException, IOException {
        //
    }

    public void handlePlayerQuit(ClientHandler clientHandler) {

    }

    public void handleSubmitWord(ClientHandler sender, int gameId, int wordId, String answer) {
        try {
            boolean isPlayer1 = sender.equals(player1);
            Word word = wordDAO.getWordById(wordId);
            boolean isCorrect = word != null && word.getWord().equalsIgnoreCase(answer.trim());

            // Cập nhật vào DB
            gameDAO.updateGameDetailAnswer(gameId, wordId, isPlayer1, answer, isCorrect);

            // Cập nhật điểm trong GameRoom
            if (isPlayer1 && isCorrect) {
                p1Score++;
            } else if (!isPlayer1 && isCorrect) {
                p2Score++;
            }

            // Gửi kết quả RIÊNG cho người gửi
            Map<String, Object> personalResult = new HashMap<>();
            personalResult.put("word_id", wordId);
            personalResult.put("correct", isCorrect);
            sender.sendMessage(new Message("answer_result", personalResult));

            // Gửi cập nhật điểm CHO CẢ 2 (theo hướng hiển thị riêng)
            Map<String, Object> scoreP1 = new HashMap<>();
            scoreP1.put("your_score", p1Score);
            scoreP1.put("opponent_score", p2Score);

            Map<String, Object> scoreP2 = new HashMap<>();
            scoreP2.put("your_score", p2Score);
            scoreP2.put("opponent_score", p1Score);

            player1.sendMessage(new Message("update_score", scoreP1));
            player2.sendMessage(new Message("update_score", scoreP2));

            // Kiểm tra kết thúc game
            if (p1Score >= MAX_ROUNDS || p2Score >= MAX_ROUNDS) {
                endGame();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void broadcast(Message message) {
        if (player1 != null) player1.sendMessage(message);
        if (player2 != null) player2.sendMessage(message);
    }


//    public void handlePlayerQuit(ClientHandler clientHandler) {
//        try {
//            // Xác định ai là người thoát
//            ClientHandler quitter = clientHandler;
//            ClientHandler opponent = (quitter == player1) ? player2 : player1;
//
//            // Thông báo cho đối thủ biết
//            if (opponent != null) {
//                opponent.sendMessage(new Message("opponent_quit", quitter.getUser().getUsername() + " đã thoát trò chơi."));
//                opponent.getUser().setStatus("available");
//            }
//
//            // Cập nhật trạng thái người thoát
//            quitter.getUser().setStatus("available");
//
//            // Lưu kết quả hoặc ghi log (tuỳ bạn muốn)
//            System.out.println("Người chơi " + quitter.getUser().getUsername() + " đã thoát game " + gameId);
//
//            // Gửi message kết thúc cho cả 2 (nếu muốn)
//            quitter.sendMessage(new Message("game_ended", "Bạn đã thoát trò chơi."));
//            if (opponent != null) {
//                opponent.sendMessage(new Message("game_ended", "Trò chơi đã kết thúc vì đối thủ rời đi."));
//            }
//
//            // Ngắt kết nối với người thoát nếu cần
//            // quitter.closeConnection(); // nếu bạn có hàm này
//
//            // Xoá game khỏi danh sách phòng đang chơi (tuỳ cấu trúc server)
//            GameManager.getInstance().removeGame(this);
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

//    case "opponent_quit":
//    String msg = (String) message.getContent();
//    Platform.runLater(() -> {
//        Alert alert = new Alert(Alert.AlertType.INFORMATION);
//        alert.setTitle("Đối thủ rời game");
//        alert.setHeaderText(null);
//        alert.setContentText(msg);
//        alert.showAndWait();
//        showMainUI();
//    });
//    break;
//
//case "game_ended":
//    String reason = (String) message.getContent();
//    Platform.runLater(() -> {
//        Alert alert = new Alert(Alert.AlertType.INFORMATION);
//        alert.setTitle("Kết thúc trò chơi");
//        alert.setHeaderText(null);
//        alert.setContentText(reason);
//        alert.showAndWait();
//        showMainUI();
//    });
//    break;

}
