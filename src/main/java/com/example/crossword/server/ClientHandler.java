package com.example.crossword.server;

import com.example.crossword.model.*;

import java.io.*;
import java.net.Socket;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClientHandler implements Runnable {
    private Socket socket;
    private Server server;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private User user;
    private GameRoom gameRoom;
    private volatile boolean isRunning = true;

    private final UserDAO userDAO = new UserDAO();
    private final GameDAO gameDAO = new GameDAO();
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
            isRunning = false;
            if (gameRoom != null) {
//                gameRoom.handlePlayerQuit(this);
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
            case "logout":
                handleLogout();
                break;
            case "register":
                handleRegister(message);
                break;
            case "get_users":
                handleGetUsers();
                break;
            case "get_history":
                handleGetHistory();
                break;
            case "get_history_details":
                handleGetHistoryDetails(message);
                break;
            case "get_leaderboard":
                handleGetLeaderboard(message);
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
            case "quit_game":
                handleQuitGame();
                break;
            case "submit_word":
                handleSubmitWord(message);
                break;
            case "rematch_request":
                handleRematchRequest(this, (Map<String, Object>) message.getContent());
                break;

            case "rematch_response":
                handleRematchResponse(this, (Map<String, Object>) message.getContent());
                break;

            case "back_to_home":
                handleBackToHome(this);
                break;
        }
    }

    private void handleRegister(Message message) throws SQLException {
        String[] register = (String[]) message.getContent();
        String username = register[0];
        String password = register[1];
        String displayName = register[2];
        if (userDAO.existsByUsername(username)) {
            sendMessage(new Message("register_failed", "Tên đăng nhập đã tồn tại."));
        } else {
            userDAO.createUser(username, password, displayName);
            sendMessage(new Message("register_success", "Đăng ký thành công!"));
        }
    }

    private void handleLogout() {
        if (user != null) {
            userDAO.updateUserStatus(user.getId(), "offline");
            user.setStatus("offline");
            server.broadcast(new Message("status_update", user.getUsername() + " offline."));
            sendMessage(new Message("logout_success", "Đăng xuất thành công."));
            server.removeClient(this);
        }
    }

    private void handleGetLeaderboard(Message message) {
        String sortType = (String) message.getContent();

        String orderBy;
        if ("wins".equalsIgnoreCase(sortType)) {
            orderBy = "total_wins DESC, total_points DESC";
        } else {
            orderBy = "total_points DESC, total_wins DESC";
        }
        List<User> users = userDAO.ranking(orderBy);
        sendMessage(new Message("ranking_list", users));
    }

    private void handleGetHistoryDetails(Message message) {
        int gameId = (int) message.getContent();
        List<HistoryDetail> list = gameDAO.getGameDetail(gameId, user.getId());
        sendMessage(new Message("history_details", list));
    }

    private void handleRematchRequest(ClientHandler sender, Map<String, Object> data) {
        int opponentId = (int) data.get("opponent_id");

        ClientHandler opponent = server.getClientById(opponentId);
        if (opponent != null) {
            Map<String, Object> invite = new HashMap<>();
            invite.put("from_id", sender.getUser().getId());
            invite.put("from_name", sender.getUser().getDisplayName());
            opponent.sendMessage(new Message("rematch_invite", invite));
        }
    }

    private void handleRematchResponse(ClientHandler responder, Map<String, Object> data) {
        int fromId = (int) data.get("from_id");
        boolean accepted = (boolean) data.get("accepted");

        ClientHandler requester = server.getClientById(fromId);

        if (accepted && requester != null) {
            GameRoom newRoom = new GameRoom(requester, responder);

            int newGameId = gameDAO.createGame(
                    requester.getUser().getId(),
                    responder.getUser().getId()
            );
            newRoom.setGameId(newGameId);

            List<Word> newWords = wordDAO.getRandomWords(10);
            newRoom.setWords(newWords);

            Message startedMsg = new Message("rematch_started", null);
            requester.sendMessage(startedMsg);
            responder.sendMessage(startedMsg);

            newRoom.startGame();

            requester.getUser().setStatus("busy");
            responder.getUser().setStatus("busy");
            server.broadcast(new Message("status_update",
                    requester.getUser().getUsername() + " busy"));
            server.broadcast(new Message("status_update",
                    responder.getUser().getUsername() + " busy"));

        } else {
            if (requester != null) {
                requester.sendMessage(new Message("rematch_declined", null));
                requester.getUser().setStatus("online");
                server.broadcast(new Message("status_update",
                        requester.getUser().getUsername() + " online"));
            }
            responder.getUser().setStatus("online");
            server.broadcast(new Message("status_update",
                    responder.getUser().getUsername() + " online"));
        }
    }



    private void handleBackToHome(ClientHandler client) {
        client.getUser().setStatus("online");
        userDAO.updateUserStatus(client.getUser().getId(), "online");
        server.broadcast(new Message("status_update", client.getUser().getUsername() + " online"));
    }


    private void handleSubmitWord(Message message) {
        Map<String, Object> data = (Map<String, Object>) message.getContent();
        int gameId = (int) data.get("game_id");
        int wordId = (int) data.get("word_id");
        String answer = (String) data.get("answer");
        gameRoom.handleSubmitWord(this, gameId, wordId, answer);
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
        Set<Integer> senderIds = (Set<Integer>) data[2];
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
                for (int senderId : senderIds) {
                    ClientHandler sender = server.getClientById(senderId);
                    if (sender != null) {
                        sender.sendMessage(new Message("match_response", "Yêu cầu trận đấu của bạn đã bị từ chối."));
                    }
                }
            } else {
                requester.sendMessage(new Message("match_response", "Yêu cầu trận đấu của bạn đã bị từ chối."));
            }
        }
    }

    private void handleRequestMatch(Message message) {
        int opponentId = (int) message.getContent();
        System.out.println("Received match request from user ID: " + user.getId() + " to opponent ID: " + opponentId);
        ClientHandler opponent = server.getClientById(opponentId);
        if (opponent != null) {
            System.out.println("Opponent found: " + opponent.getUser().getUsername() + " - Status: "
                    + opponent.getUser().getStatus());
            if (opponent.getUser().getStatus().equals("online")) {
                Object[] o = {user.getId(), user.getDisplayName()};
                opponent.sendMessage(new Message("match_request", o));
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

    private void handleGetHistory() {
        List<History> histories = gameDAO.getGamesByUserId(user.getId());
        sendMessage(new Message("history", histories));
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
            try {
                socket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public Server getServer() {
        return server;
    }
}