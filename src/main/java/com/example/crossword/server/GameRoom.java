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
    }

    public void startGame() {
        player1.getUser().setStatus("busy");
        player2.getUser().setStatus("busy");

        player1.sendMessage(new Message("game_start", null));
        player2.sendMessage(new Message("game_start", null));
    }

    public void handlePlayerDisconnect(ClientHandler disconnectedPlayer) throws SQLException, IOException {
        //
    }

    public void handlePlayerQuit(ClientHandler clientHandler) {
    }
}
