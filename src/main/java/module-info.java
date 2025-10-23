module com.example.crossword {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;


    opens com.example.crossword to javafx.fxml;
    opens com.example.crossword.client to javafx.fxml;
    opens com.example.crossword.server to javafx.fxml;
    exports com.example.crossword.client;
    exports com.example.crossword.model;
    exports com.example.crossword;

}