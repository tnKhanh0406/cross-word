package com.example.crossword.server;


import java.sql.*;

public class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/crossword";
    private static final String USER = "root";
    private static final String PASSWORD = "Kieuo@nh2104";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
