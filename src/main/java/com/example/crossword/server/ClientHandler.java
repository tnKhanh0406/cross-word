package com.example.crossword.server;

import com.example.crossword.model.Message;
import com.example.crossword.model.User;
import com.example.crossword.model.Word;

import java.io.*;
import java.net.Socket;
import java.sql.SQLException;
import java.util.List;

public class ClientHandler implements Runnable {
    private Socket socket;
    private Server server;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private User user;
    private GameRoom gameRoom;
    private volatile boolean isRunning = true;

    private final UserDAO userDAO = new UserDAO();
    private final WordDAO wordDAO = new WordDAO();

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public User getUser() {
        return user;
    }

    @Override
    public void run() {
        try {
            while (isRunning) {
                Message message = (Message) in.readObject();
                if (message != null) {
                    handleMessage(message);
                }
            }
        } catch (IOException | ClassNotFoundException | SQLException e) {
            System.out.println("Kết nối với " + (user != null ? user.getUsername() : "client") + " bị ngắt.");
            isRunning = false; // Dừng vòng lặp
            if (gameRoom != null) {
                try {
                    gameRoom.handlePlayerDisconnect(this);
                } catch (IOException | SQLException ex) {
                    ex.printStackTrace();
                }
            }
        } finally {
            try {
                if (user != null) {
                    userDAO.updateUserStatus(user.getId(), "offline");
                    server.broadcast(new Message("status_update", user.getUsername() + " offline"));
                    server.removeClient(this);
                }
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleMessage(Message message) throws IOException, SQLException {
        System.out.println("Client to Server: " + message.getType() + " " + message.getContent());
        switch (message.getType()) {
            case "login":
                handleLogin(message);
                break;
            case "get_users":
                handleGetUsers();
                break;
            case "request_match":
                handleRequestMatch(message);
                break;
            case "match_response":
                handleMatchResponse(message);
                break;
            case "chat":
                handleChat(message);
                break;
            case "get_words":
                handleGetWords();
                break;
            case "quit_game":
                handleQuitGame();
                break;
        }
    }

    private void handleGetWords() {
        List<Word> words = wordDAO.getRandomWords(10);
        sendMessage(new Message("word_list", words));
    }

    private void handleQuitGame() {
        gameRoom.handlePlayerQuit(this);
    }

    private void handleChat(Message message) {
        server.broadcast(new Message("chat", user.getUsername() + ": " + message.getContent()));
    }

    private void handleMatchResponse(Message message) {
        Object[] data = (Object[]) message.getContent();
        int requesterId = (int) data[0];
        boolean accepted = (boolean) data[1];
        ClientHandler requester = server.getClientById(requesterId);
        if (requester != null) {
            if (accepted) {
                GameRoom newGameRoom = new GameRoom(this, requester);
                this.gameRoom = newGameRoom;
                requester.gameRoom = newGameRoom;
                this.user.setStatus("busy");
                requester.user.setStatus("busy");

                userDAO.updateUserStatus(user.getId(), "busy");
                requester.userDAO.updateUserStatus(user.getId(), "busy");

                server.broadcast(new Message("status_update", user.getUsername() + " busy"));
                server.broadcast(new Message("status_update", requester.user.getUsername() + " busy"));

                newGameRoom.startGame();
            } else {
                requester.sendMessage(new Message("match_response", "Yêu cầu trận đấu của bạn đã bị từ chối."));
            }
        }
    }

    private void handleRequestMatch(Message message) throws IOException, SQLException {
        int opponentId = (int) message.getContent();
        System.out.println("Received match request from user ID: " + user.getId() + " to opponent ID: " + opponentId);
        ClientHandler opponent = server.getClientById(opponentId);
        if (opponent != null) {
            System.out.println("Opponent found: " + opponent.getUser().getUsername() + " - Status: "
                    + opponent.getUser().getStatus());
            if (opponent.getUser().getStatus().equals("online")) {
                opponent.sendMessage(new Message("match_request", user.getId()));
                System.out.println("Match request sent to " + opponent.getUser().getUsername());
            } else {
                sendMessage(new Message("match_response", "Người chơi không sẵn sàng."));
                System.out.println("Opponent is not online.");
            }
        } else {
            sendMessage(new Message("match_response", "Người chơi không tồn tại hoặc không online."));
            System.out.println("Opponent not found.");
        }
    }

    private void handleGetUsers() {
        List<User> users = userDAO.getAllUsers();
        sendMessage(new Message("user_list", users));
    }

    private void handleLogin(Message message) {
        String[] credentials = (String[]) message.getContent();
        String username = credentials[0];
        String password = credentials[1];
        User user = userDAO.validate(username, password);
        if (user != null) {
            this.user = user;
            userDAO.updateUserStatus(user.getId(), "online");
            user.setStatus("online");
            sendMessage(new Message("login_success", user));
            server.broadcast(new Message("status_update", user.getUsername() + " online"));
            server.addClient(user.getId(), this);
        } else {
            sendMessage(new Message("login_fail", "Tài khoản hoặc mật khẩu không đúng"));
        }
    }

    public void sendMessage(Message message) {
        try {
            if (socket != null && !socket.isClosed()) {
                out.writeObject(message);
                out.flush();
            } else {
                System.out.println(
                        "Socket đã đóng, không thể gửi tin nhắn tới " + (user != null ? user.getUsername() : "client"));
            }
        } catch (IOException e) {
            System.out.println("Lỗi khi gửi tin nhắn tới " + (user != null ? user.getUsername() : "client") + ": "
                    + e.getMessage());
            // Không gọi lại handleLogout() ở đây để tránh đệ quy
            // Đánh dấu client là đã ngắt kết nối
            try {
                socket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void clearGameRoom() {
        this.gameRoom = null;
    }

    public Server getServer() {
        return server;
    }
}