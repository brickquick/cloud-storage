import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.util.Objects;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent parent = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("mainPanel.fxml")));
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent t) {
                Platform.exit();
                System.exit(0);
            }
        });
        Scene scene = new Scene(parent);
        String css = Objects.requireNonNull(this.getClass().getResource("application.css")).toExternalForm();
        scene.getStylesheets().add(css);
//        primaryStage.setResizable(false);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

}
