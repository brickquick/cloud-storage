import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import lombok.extern.slf4j.Slf4j;
import qbrick.*;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Slf4j
public class Controller implements Initializable {

    @FXML
    AnchorPane clientPanel;

    @FXML
    TableView<FileInfo> filesTable;
    @FXML
    ComboBox<String> disksBox;
    @FXML
    TextField pathField;

    @FXML
    public ListView<String> listView;
    @FXML
    public TextField input;

    private static byte[] buffer = new byte[1024];

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
//        net = Net.getInstance(s -> Platform.runLater(() ->
//                listView.getItems().add(String.valueOf(s))));
//        net = new Net(args -> listView.getItems().add((String) args[0]));

        net = new Net(command -> {
            switch (command.getType()) {
                case LIST_RESPONSE:
                    ListResponse files = (ListResponse) command;

                    Platform.runLater(() -> {
                        listView.getItems().clear();
                        listView.getItems().addAll(files.getFileInfos().toString());


                        TableColumn<FileInfo, String> fileTypeColumn = new TableColumn<>();
                        fileTypeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getType().getName()));
                        fileTypeColumn.setPrefWidth(24);

                        TableColumn<FileInfo, String> filenameColumn = new TableColumn<>("Имя");
                        filenameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFilename()));
                        filenameColumn.setPrefWidth(240);

                        TableColumn<FileInfo, Long> fileSizeColumn = new TableColumn<>("Размер");
                        fileSizeColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getSize()));
                        fileSizeColumn.setCellFactory(column -> {
                            return new TableCell<FileInfo, Long>() {
                                @Override
                                protected void updateItem(Long item, boolean empty) {
                                    super.updateItem(item, empty);
                                    if (item == null || empty) {
                                        setText(null);
                                        setStyle("");
                                    } else {
                                        String text = String.format("%,d bytes", item);
                                        if (item == -1L) {
                                            text = "[DIR]";
                                        }
                                        setText(text);
                                    }
                                }
                            };
                        });
                        fileSizeColumn.setPrefWidth(120);

                        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        TableColumn<FileInfo, String> fileDateColumn = new TableColumn<>("Дата изменения");
                        fileDateColumn.setCellValueFactory(param -> new SimpleStringProperty(
                                param.getValue().getLastModified().format(dtf)));
                        fileDateColumn.setPrefWidth(120);

                        filesTable.getColumns().clear();
                        filesTable.getColumns().addAll(fileTypeColumn, filenameColumn, fileSizeColumn, fileDateColumn);
                        filesTable.getSortOrder().add(fileTypeColumn);

                        filesTable.setOnMouseClicked(new EventHandler<MouseEvent>() {
                            @Override
                            public void handle(MouseEvent event) {
                                if (event.getClickCount() == 2) {
                                    net.sendCmd(new PathInRequest(filesTable.getSelectionModel().getSelectedItem().getFilename()));
                                    updateList(files);
                                }
                            }
                        });

                        updateList(files);
                    });
                    break;
                case PATH_RESPONSE:
                    PathResponse pathResponse = (PathResponse) command;
                    updatePath(pathResponse.getPath());
                    break;
            }
        });


    }

    public void updatePath(String path) {
        pathField.setText(path);
    }

    public void updateList(ListResponse files) {
        filesTable.getItems().clear();
        filesTable.getItems().addAll(new ArrayList<>(files.getFileInfos()));
        filesTable.sort();
    }

    public void copyBtnAction(ActionEvent actionEvent) {
        ClientPanelController cpc = (ClientPanelController) clientPanel.getProperties().get("ctrl");


    }

    public void btnExitAction(ActionEvent actionEvent) {
        Platform.exit();
        System.exit(0);
    }

    public void selectDiskAction(ActionEvent actionEvent) {
    }

    public void btnPathUpAction(ActionEvent actionEvent) {
        net.sendCmd(new PathUpRequest());
    }
}