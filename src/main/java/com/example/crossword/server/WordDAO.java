package com.example.crossword.server;

import com.example.crossword.model.Word;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class WordDAO {
    public List<Word> getRandomWords(int limit) {
        List<Word> words = new ArrayList<>();
        String sql = "SELECT word_id, word_text, hint FROM word ORDER BY RAND() LIMIT ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Word w = new Word();
                w.setId(rs.getInt("word_id"));
                w.setWord(rs.getString("word_text"));
                w.setHint(rs.getString("hint"));
                words.add(w);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return words;
    }

}
