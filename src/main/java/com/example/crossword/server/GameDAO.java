package com.example.crossword.server;

import com.example.crossword.model.Word;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

public class GameDAO {
    public int createGame(int p1Id, int p2Id) {
        String sql = "INSERT INTO game (player1_id, player2_id) VALUES (?, ?)";
        try {
            Connection conn = DBConnection.getConnection();
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
        try (Connection conn = DBConnection.getConnection()) {
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
}
