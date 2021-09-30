import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import lombok.extern.slf4j.Slf4j;
import qbrick.FileMessage;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Slf4j
public class Controller implements Initializable {

    @FXML
    AnchorPane leftPanel, rightPanel;
    @FXML
    public ListView<String> listView;
    @FXML
    public TextField input;

    private static String ROOT_DIR = "client/root";
    private ObjectDecoderInputStream is;
    private ObjectEncoderOutputStream os;
    private Net net;

    public void send(ActionEvent actionEvent) throws Exception {
        String fileName = input.getText();
//        sendFile(fileName);
        net.sendMessage(fileName);
        input.clear();
    }

    private void sendFile(String fileName) throws IOException {
        Path file = Paths.get(ROOT_DIR, fileName);
        os.writeObject(new FileMessage(file));
        os.flush();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        net = Net.getInstance(s -> Platform.runLater(() ->
                listView.getItems().add(s))
        );

    }

    private void fillFilesInCurrentDir() throws IOException {
        listView.getItems().clear();
        listView.getItems().addAll(
                Files.list(Paths.get(ROOT_DIR))
                        .map(p -> p.getFileName().toString())
                        .collect(Collectors.toList())
        );
        listView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String item = listView.getSelectionModel().getSelectedItem();
                input.setText(item);
            }
        });
    }

    private static byte[] buffer = new byte[1024];

    public void copyBtnAction(ActionEvent actionEvent) {
    }

    public void btnExitAction(ActionEvent actionEvent) {
        Platform.exit();
        System.exit(0);
    }

}