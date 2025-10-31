package com.example.crossword.server;


import java.sql.*;

public class DAO {
    private static final String URL = "jdbc:mysql://localhost:3306/crossword";
    private static final String USER = "root";
    private static final String PASSWORD = "123456";

    protected Connection conn;

    public DAO() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            this.conn = DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        return conn;
    }
}
