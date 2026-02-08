module jp.el {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;

    opens jp.el to javafx.fxml, com.fasterxml.jackson.databind;
    exports jp.el;
}