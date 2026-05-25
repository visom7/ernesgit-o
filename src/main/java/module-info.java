module com.ernesgito {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.desktop;

    opens com.ernesgito to javafx.graphics;
    opens com.ernesgito.ui to javafx.graphics;
    opens com.ernesgito.ui.panels to javafx.graphics;
    exports com.ernesgito.model to javafx.base;
    opens com.ernesgito.model to javafx.base;
}
