module algo.vetplusapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.prefs;
    requires java.desktop;
    requires org.mariadb.jdbc;

    opens algo.vetplusapp to javafx.fxml;
    opens algo.vetplusapp.ctrl to javafx.fxml;
    exports algo.vetplusapp;
    exports algo.vetplusapp.ctrl;
    exports algo.vetplusapp.db;
    exports algo.vetplusapp.util;
    exports algo.vetplusapp.repo;
}
