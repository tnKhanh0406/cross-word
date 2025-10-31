package com.example.crossword.server;

import com.example.crossword.model.History;
import com.example.crossword.model.HistoryDetail;
import com.example.crossword.model.Word;

import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class GameDAO extends DAO{
    public GameDAO() {
        super();
    }
    public int createGame(int p1Id, int p2Id) {
        String sql = "INSERT INTO game (player1_id, player2_id) VALUES (?, ?)";
        try {
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, p1Id);
            ps.setInt(2, p2Id);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public void createGameDetails(int gameId, List<Word> words) {
        String sql = "INSERT INTO gamedetail (game_id, round_number, word_id, player1_answer, player2_answer, player1_correct, player2_correct) VALUES (?, ?, ?, NULL, NULL, 0, 0)";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            int round = 1;
            for (Word w : words) {
                ps.setInt(1, gameId);
                ps.setInt(2, round++);
                ps.setInt(3, w.getId());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateGameDetailAnswer(int gameId, int wordId, boolean isPlayer1, String answer, boolean correct){
        System.out.println("CAp nhat thanh cong game detail");
        String columnAnswer = isPlayer1 ? "player1_answer" : "player2_answer";
        String columnCorrect = isPlayer1 ? "player1_correct" : "player2_correct";

        String sql = "UPDATE GameDetail SET " + columnAnswer + " = ?, " + columnCorrect + " = ? WHERE game_id = ? AND word_id = ?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, answer);
            ps.setInt(2, correct ? 1 : 0);
            ps.setInt(3, gameId);
            ps.setInt(4, wordId);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                System.err.println("[WARN] Không có dòng nào được cập nhật! gameId=" + gameId + ", wordId=" + wordId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Kết thúc ván game
    public void finishGame(int gameId, int winnerId, String result) {
        String sql = "UPDATE Game SET status='finished', end_time=NOW(), winner_id=?, result=? WHERE game_id=?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);

            if (winnerId == 0) {
                ps.setNull(1, java.sql.Types.INTEGER); // nếu hòa thì set null
            } else {
                ps.setInt(1, winnerId);
            }

            ps.setString(2, result);
            ps.setInt(3, gameId);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public List<History> getGamesByUserId(int userId) {
        List<History> games = new ArrayList<>();
        String sql = """
                SELECT\s
                    g.*,
                    CASE\s
                        WHEN g.player1_id = ? THEN u2.display_name
                        ELSE u1.display_name\s
                    END AS opponent_name,
                
                    CASE\s
                        WHEN g.result = 'draw' THEN 'draw'
                        WHEN g.player1_id = ? AND g.result = 'p1_win' THEN 'win'
                        WHEN g.player1_id = ? AND g.result = 'p2_win' THEN 'loss'
                        WHEN g.player2_id = ? AND g.result = 'p2_win' THEN 'win'
                        WHEN g.player2_id = ? AND g.result = 'p1_win' THEN 'loss'
                        ELSE 'unknown'
                    END AS final_result
                
                FROM Game g
                LEFT JOIN User u1 ON g.player1_id = u1.user_id
                LEFT JOIN User u2 ON g.player2_id = u2.user_id
                WHERE g.player1_id = ? OR g.player2_id = ?
                ORDER BY g.start_time DESC;
                
                """;
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            ps.setInt(3, userId);
            ps.setInt(4, userId);
            ps.setInt(5, userId);
            ps.setInt(6, userId);
            ps.setInt(7, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                History game = new History();
                game.setGame_id(rs.getInt("game_id"));
                game.setOpponent_name(rs.getString("opponent_name"));
                game.setResult(rs.getString("final_result"));
                Timestamp ts = rs.getTimestamp("start_time");
                String time = ts.toLocalDateTime()
                        .format(DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy"));
                game.setTime(time);
                games.add(game);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return games;
    }

    public List<HistoryDetail> getGameDetail(int gameId, int userId) {
        String sql = """
                SELECT\s
                    gd.round_number,
                    w.word_text,
                    CASE\s
                        WHEN g.player1_id = ? THEN gd.player1_answer
                        ELSE gd.player2_answer
                    END AS player_answer,
                    CASE\s
                        WHEN g.player1_id = ? THEN gd.player2_answer
                        ELSE gd.player1_answer
                    END AS opponent_answer
                FROM GameDetail gd
                JOIN Word w ON gd.word_id = w.word_id
                JOIN Game g ON gd.game_id = g.game_id
                WHERE gd.game_id = ?
                ORDER BY gd.round_number;
                """;
        List<HistoryDetail> gameDetails = new ArrayList<>();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            ps.setInt(3, gameId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                HistoryDetail detail = new HistoryDetail();
                detail.setRound(rs.getInt("round_number"));
                detail.setWord(rs.getString("word_text"));
                detail.setMyAnswer(rs.getString("player_answer"));
                detail.setOpponentAnswer(rs.getString("opponent_answer"));
                gameDetails.add(detail);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return gameDetails;
    }

}
