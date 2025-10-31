package com.example.crossword.server;

import com.example.crossword.model.Message;
import com.example.crossword.model.Word;

import java.util.*;

public class GameRoom {
    private int gameId;
    private ClientHandler player1;
    private ClientHandler player2;
    private int p1Score;
    private int p2Score;

    private boolean gameEnded = false;
    private final int MAX_ROUNDS = 10;
    private final int GAME_TIMEOUT = 20;
    private int remainingTime = GAME_TIMEOUT;

    private transient Timer timer;

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }

    public void setWords(List<Word> words) {
        this.words = words;
    }

    private List<Word> words;

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
            if (gameEnded) return;
            gameEnded = true;
            if (timer != null) {
                timer.cancel();
                timer = null;// dừng timer ngay khi kết thúc game
            }

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

            if(winnerId != 0) {
                endDataP1.put("loser_id", (winnerId == player1.getUser().getId())
                        ? player2.getUser().getId()
                        : player1.getUser().getId());

                endDataP2.put("loser_id", (winnerId == player1.getUser().getId())
                        ? player2.getUser().getId()
                        : player1.getUser().getId());
            } else {
                endDataP1.put("loser_id", 0);
                endDataP2.put("loser_id", 0);
            }
            endDataP1.put("opponent_id", player2.getUser().getId());
            endDataP2.put("opponent_id", player1.getUser().getId());

            player1.sendMessage(new Message("game_over", endDataP1));
            player2.sendMessage(new Message("game_over", endDataP2));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startGameTimer() {
        timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (gameEnded) { // nếu game đã kết thúc, dừng timer
                    cancel();
                    return;
                }
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

    public void handlePlayerQuit(ClientHandler quitter) {
        gameEnded = true;
        boolean isPlayer1 = quitter.equals(player1);
        ClientHandler winner = isPlayer1 ? player2 : player1;
        String result = isPlayer1 ? "p2_win" : "p1_win";
        gameDAO.finishGame(gameId, winner.getUser().getId(), result);
        userDAO.addTotalPoint(winner.getUser().getId(), 1);
        userDAO.addTotalWin(winner.getUser().getId(), 1);

        Map<String, Object> endDataP1 = new HashMap<>();
        Map<String, Object> endDataP2 = new HashMap<>();
        endDataP1.put("winner_id", winner.getUser().getId());
        endDataP2.put("winner_id", winner.getUser().getId());
        endDataP1.put("result", result);
        endDataP2.put("result", result);
        if(result.equals("p1_win")) {
            endDataP1.put("reason", "Đối thủ đã thoát trò chơi.");
            endDataP2.put("reason", "Bạn đã thoát khỏi trò chơi.");
        } else {
            endDataP2.put("reason", "Đối thủ đã thoát trò chơi.");
            endDataP1.put("reason", "Bạn đã thoát khỏi trò chơi.");
        }

        player1.sendMessage(new Message("game_over", endDataP1));
        player2.sendMessage(new Message("game_over", endDataP2));
    }

    public void handleSubmitWord(ClientHandler sender, int gameId, int wordId, String answer) {
        try {
            boolean isPlayer1 = sender.equals(player1);
            Word word = wordDAO.getWordById(wordId);
            boolean isCorrect = word != null && word.getWord().equalsIgnoreCase(answer.trim());

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

            // Gửi cập nhật điểm CHO CẢ 2
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
}
