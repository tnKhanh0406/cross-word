package com.example.crossword.client;

import com.example.crossword.model.Game;
import com.example.crossword.model.GameDetail;
import com.example.crossword.model.Message;
import com.example.crossword.model.User;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HomeController {
    private Client client;
    private ObservableList<User> usersList = FXCollections.observableArrayList();
    // Tab 1
    @FXML private TableView<User> tblUsers;
    @FXML private TableColumn<User, String> colDisplayName;
    @FXML private TableColumn<User, Double> colTotalPoints;
    @FXML private TableColumn<User, Integer> colTotalWins;
    @FXML private TableColumn<User, String> colStatus;

    // Tab 2
    @FXML private TableView<Game> tblGames;
    @FXML private TableColumn<Game, Integer> colGameId;
    @FXML private TableColumn<Game, String> colOpponent;
    @FXML private TableColumn<Game, String> colResult;
    @FXML private TableColumn<Game, String> colStartTime;

    @FXML private TableView<GameDetail> tblGameDetails;
    @FXML private TableColumn<GameDetail, Integer> colRound;
    @FXML private TableColumn<GameDetail, String> colWord;
    @FXML private TableColumn<GameDetail, String> colPlayerAnswer;
    @FXML private TableColumn<GameDetail, String> colOpponentAnswer;

    // Tab 3
    @FXML private ComboBox<String> cbSortType;
    @FXML private TableView<User> tblRanking;
    @FXML private TableColumn<User, String> colRankDisplayName;
    @FXML private TableColumn<User, Double> colRankPoints;
    @FXML private TableColumn<User, Integer> colRankWins;

    @FXML private Label lblDisplayName;

    public void setDisplayName(String displayName) {
        lblDisplayName.setText(displayName);
    }

    @FXML
    public void initialize() {
        cbSortType.getSelectionModel().selectFirst();

        colDisplayName.setCellValueFactory(new PropertyValueFactory<>("displayName"));
        colTotalPoints.setCellValueFactory(new PropertyValueFactory<>("totalPoints"));
        colTotalWins.setCellValueFactory(new PropertyValueFactory<>("totalWins"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        tblUsers.setItems(usersList);

        tblUsers.setRowFactory(tf -> {
            TableRow<User> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    User clickedUser = row.getItem();
                    if (clickedUser.getId() != client.getUser().getId()) {
                        Message matchRequest = new Message("request_match", clickedUser.getId());
                        try {
                            client.sendMessage(matchRequest);
                        } catch (IOException ex) {
                            Logger.getLogger(HomeController.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            });
            return row;
        });
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public void updateStatus(String statusUpdate) {
        String[] parts = statusUpdate.split(" ");
        if (parts.length >= 2) {
            String username = parts[0];
            String status = parts[1];
            for (User user : usersList) {
                if (user.getUsername().equalsIgnoreCase(username)) {
                    user.setStatus(status);
                    tblUsers.refresh();
                    break;
                }
            }
        }
    }

    public void updateUserList(List<User> users) {
        Platform.runLater(() -> {
            usersList.setAll(users);
            tblUsers.setItems(usersList);
            tblUsers.refresh();
        });
    }

    public void showMatchRequest(int requesterId) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Yêu Cầu Trận Đấu");
        alert.setHeaderText("Bạn nhận được yêu cầu trận đấu từ người chơi ID: " + requesterId);
        alert.setContentText("Bạn có muốn đồng ý?");

        alert.showAndWait().ifPresent(response -> {
            boolean accepted = response == ButtonType.OK;
            Object[] data = { requesterId, accepted };
            Message responseMessage = new Message("match_response", data);
            try {
                client.sendMessage(responseMessage);
            } catch (IOException ex) {
                Logger.getLogger(HomeController.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
    }

    public void handleMatchResponse(String response) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Trận Đấu");
        alert.setHeaderText(null);
        alert.setContentText(response);
        alert.showAndWait();
    }
}
