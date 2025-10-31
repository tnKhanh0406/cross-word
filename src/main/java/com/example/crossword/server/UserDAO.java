package com.example.crossword.server;

import com.example.crossword.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserDAO extends DAO {

    public UserDAO() {
        super();
    }

    public User validate(String username, String password) {
        String query = "SELECT * FROM user WHERE username = ? AND password = ?";
        try {
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            System.out.println(username + " " + password);

            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("user_id"));
                user.setUsername(rs.getString("username"));
                user.setPassword(rs.getString("password"));
                user.setDisplayName(rs.getString("display_name"));
                user.setStatus(rs.getString("status"));
                user.setTotalPoints(rs.getInt("total_points"));
                user.setTotalWins(rs.getInt("total_wins"));
                return user;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void updateUserStatus(int id, String status) {
        String sql = "UPDATE user SET status = ? WHERE user_id = ?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, status);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM user";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("user_id"));
                user.setUsername(rs.getString("username"));
                user.setPassword(rs.getString("password"));
                user.setDisplayName(rs.getString("display_name"));
                user.setStatus(rs.getString("status"));
                user.setTotalPoints(rs.getDouble("total_points"));
                user.setTotalWins(rs.getInt("total_wins"));
                users.add(user);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return users;
    }

    public void addTotalPoint(int user_id, double points) {
        String sql = "UPDATE user SET total_points = total_points + ? WHERE user_id = ?";
        try{
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setDouble(1, points);
            ps.setInt(2, user_id);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void addTotalWin(int user_id, int win) {
        String sql = "UPDATE user SET total_wins = total_wins + ? WHERE user_id = ?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, win);
            ps.setInt(2, user_id);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<User> ranking(String sortType) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM User ORDER BY " + sortType;
        try{
            PreparedStatement ps = conn.prepareStatement(sql);
            System.out.println(sortType);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("user_id"));
                user.setUsername(rs.getString("username"));
                user.setPassword(rs.getString("password"));
                user.setDisplayName(rs.getString("display_name"));
                user.setStatus(rs.getString("status"));
                user.setTotalPoints(rs.getDouble("total_points"));
                user.setTotalWins(rs.getInt("total_wins"));
                users.add(user);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return users;
    }

    public boolean existsByUsername(String username) {
        String sql = "SELECT COUNT(*) FROM user WHERE username = ?";
        try{
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void createUser(String username, String password, String displayName) throws SQLException {
        String sql = "INSERT INTO user(username, password, display_name, status, total_points, total_wins) VALUES(?, ?, ?, 'offline', 0, 0)";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, displayName);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
