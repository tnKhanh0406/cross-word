package com.example.crossword.client;

import com.example.crossword.model.*;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
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
    @FXML public Label lblYourRank;
    private Client client;
    private ObservableList<User> usersList = FXCollections.observableArrayList();
    // Tab 1
    @FXML private TableView<User> tblUsers;
    @FXML private TableColumn<User, String> colDisplayName;
    @FXML private TableColumn<User, Double> colTotalPoints;
    @FXML private TableColumn<User, Integer> colTotalWins;
    @FXML private TableColumn<User, String> colStatus;

    // Tab 2
    @FXML private TableView<HistoryDTO> tblGames;
    @FXML private TableColumn<HistoryDTO, Integer> colGameId;
    @FXML private TableColumn<HistoryDTO, String> colOpponent;
    @FXML private TableColumn<HistoryDTO, String> colResult;
    @FXML private TableColumn<HistoryDTO, String> colStartTime;

    @FXML private TableView<HistoryDetailDTO> tblGameDetails;
    @FXML private TableColumn<HistoryDetailDTO, Integer> colRound;
    @FXML private TableColumn<HistoryDetailDTO, String> colWord;
    @FXML private TableColumn<HistoryDetailDTO, String> colPlayerAnswer;
    @FXML private TableColumn<HistoryDetailDTO, String> colOpponentAnswer;

    private ObservableList<HistoryDTO> gamesList = FXCollections.observableArrayList();
    private ObservableList<HistoryDetailDTO> detailsList = FXCollections.observableArrayList();

    // Tab 3
    @FXML private ComboBox<String> cbSortType;
    @FXML private TableView<User> tblRanking;
    @FXML private TableColumn<User, Integer> colRank;
    @FXML private TableColumn<User, String> colRankDisplayName;
    @FXML private TableColumn<User, Double> colRankPoints;
    @FXML private TableColumn<User, Integer> colRankWins;

    @FXML private Label lblDisplayName;

    public void setDisplayName(String displayName) {
        lblDisplayName.setText(displayName);
    }

    @FXML
    public void initialize() {
        //tab 1
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

        //tab 2
        colGameId.setCellValueFactory(new PropertyValueFactory<>("game_id"));
        colOpponent.setCellValueFactory(new PropertyValueFactory<>("opponent_name"));
        colResult.setCellValueFactory(new PropertyValueFactory<>("result"));
        colStartTime.setCellValueFactory(new PropertyValueFactory<>("time"));

        tblGames.setItems(gamesList);

        colRound.setCellValueFactory(new PropertyValueFactory<>("round"));
        colWord.setCellValueFactory(new PropertyValueFactory<>("word"));
        colPlayerAnswer.setCellValueFactory(new PropertyValueFactory<>("myAnswer"));
        colOpponentAnswer.setCellValueFactory(new PropertyValueFactory<>("opponentAnswer"));
        tblGameDetails.setItems(detailsList);
        tblGames.setRowFactory(tv -> {
            TableRow<HistoryDTO> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getClickCount() == 1) {
                    HistoryDTO clickedHistory = row.getItem();
                    try {
                        client.sendMessage(new Message("get_history_details", clickedHistory.getGame_id()));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            return row;
        });

        //tab 3
        cbSortType.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                String sortType = newVal.equals("Tổng điểm") ? "points" : "wins";
                try {
                    client.sendMessage(new Message("get_leaderboard", sortType));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        // set các cột bảng
        colRankDisplayName.setCellValueFactory(new PropertyValueFactory<>("displayName"));
        colRankPoints.setCellValueFactory(new PropertyValueFactory<>("totalPoints"));
        colRankWins.setCellValueFactory(new PropertyValueFactory<>("totalWins"));
        colRank.setCellValueFactory(cellData ->
                new ReadOnlyObjectWrapper<>(tblRanking.getItems().indexOf(cellData.getValue()) + 1)
        );

    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) throws IOException {
        this.client = client;
        loadUsers();
        loadHistory();
        loadLeaderboard();
    }

    public void loadUsers() throws IOException {
        client.sendMessage(new Message("get_users", null));
    }

    public void loadHistory() throws IOException {
        client.sendMessage(new Message("get_history", null));
    }

    public void loadLeaderboard() throws IOException {
        client.sendMessage(new Message("get_leaderboard", "points"));
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

    public void updateGameList(List<HistoryDTO> histories) {
        gamesList = FXCollections.observableList(histories);
        tblGames.setItems(gamesList);
    }

    public void showGameDetails(List<HistoryDetailDTO> list) {
        detailsList = FXCollections.observableList(list);
        tblGameDetails.setItems(detailsList);
    }

    public void showLeaderboard(List<User> items) {
        ObservableList<User> leaderboardList = FXCollections.observableArrayList(items);
        tblRanking.setItems(leaderboardList);

        int rank = -1;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId() == client.getUser().getId()) {
                rank = i + 1; // vì index bắt đầu từ 0
                break;
            }
        }

        if (rank != -1) {
            lblYourRank.setText(
                    "Bạn đang xếp hạng #" + rank +
                            " — Điểm: " + client.getUser().getTotalPoints() +
                            " — Thắng: " + client.getUser().getTotalWins()
            );
        } else {
            lblYourRank.setText("Bạn chưa có trong bảng xếp hạng.");
        }
    }
}
