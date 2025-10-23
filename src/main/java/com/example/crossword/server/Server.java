package com.example.crossword.server;

import com.example.crossword.model.Message;

import java.io.*;
import java.net.*;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

public class Server {

    private static final int PORT = 12345;
    private ServerSocket serverSocket;
    private ConcurrentHashMap<Integer, ClientHandler> clientMap = new ConcurrentHashMap<>();

    public Server() {
        try {
            serverSocket = new ServerSocket(PORT);
            listenForClients();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void addClient(int userId, ClientHandler clientHandler) {
        clientMap.put(userId, clientHandler);
    }

    public synchronized ClientHandler getClientById(int userId) {
        return clientMap.get(userId);
    }

    public synchronized void removeClient(ClientHandler clientHandler) {
        if (clientHandler.getUser() != null) {
            clientMap.remove(clientHandler.getUser().getId());
        }
    }

    // Gửi tin nhắn tới tất cả client
    public synchronized void broadcast(Message message) {
        for (ClientHandler client : clientMap.values()) {
            client.sendMessage(message);
        }
    }

    // Lắng nghe kết nối từ client
    private void listenForClients() {
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                System.out.println("Đã có kết nối từ " + socket.getInetAddress());
                ClientHandler clientHandler = new ClientHandler(socket, this);
                new Thread(clientHandler).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        new Server();
    }
}

