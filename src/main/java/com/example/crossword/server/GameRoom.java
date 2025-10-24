package com.example.crossword.server;

import com.example.crossword.model.Message;
import com.example.crossword.model.Word;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameRoom {
    private int gameId;
    private ClientHandler player1;
    private ClientHandler player2;
    private int p1Score;
    private int p2Score;

    private Map<String, String> p1Answer;
    private Map<String, String> p2Answer;

    private Boolean p1WantsRematch = null;
    private Boolean p2WantsRematch = null;
    private final int WIN_SCORE = 10;
    private final int MAX_ROUNDS = 10;
    private int currentRound;
    private final int GAME_TIMEOUT = 300;

    private List<Word> words; // 10 từ được chọn ngẫu nhiên từ DB

    private GameDAO gameDAO = new GameDAO();
    private WordDAO wordDAO = new WordDAO();
    public GameRoom(ClientHandler p1, ClientHandler p2) {
        this.player1 = p1;
        this.player2 = p2;
        this.words = wordDAO.getRandomWords(10);
        this.currentRound = 1;
        this.p1Score = 0;
        this.p2Score = 0;
        this.gameId = gameDAO.createGame(this.player1.getUser().getId(), this.player2.getUser().getId());
        gameDAO.createGameDetails(this.gameId, this.words);
    }

    public void startGame() {
        player1.getUser().setStatus("busy");
        player2.getUser().setStatus("busy");

        player1.sendMessage(new Message("game_start", this.words));
        player2.sendMessage(new Message("game_start", this.words));
    }

    public void handlePlayerDisconnect(ClientHandler disconnectedPlayer) throws SQLException, IOException {
        //
    }

    public void handlePlayerQuit(ClientHandler clientHandler) {

    }

    public void handleSubmitWord(ClientHandler sender, int gameId, int wordId, String answer) {

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
