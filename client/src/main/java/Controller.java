import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.layout.VBox;
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

@Slf4j
public class Controller implements Initializable {

    @FXML
    VBox clientPanel;

    @FXML
    TableView<FileInfo> filesTable;
    @FXML
    TextField pathField;

    @FXML
    public ListView<String> listView;
    @FXML
    public TextField input;

    private static String ROOT_DIR = "client/root";
    private Net net;

    public void send(ActionEvent actionEvent) {
        net.sendCmd(new ConsoleMessage(input.getText()));
        input.clear();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        net = new Net(cmd -> {
            switch (cmd.getType()) {
                case CONSOLE_MESSAGE:
                    ConsoleMessage consoleMessage = (ConsoleMessage) cmd;
                    Platform.runLater(() -> {
                        listView.getItems().addAll(consoleMessage.getMsg());
                    });
                    break;
                case PATH_RESPONSE:
                    PathResponse pathResponse = (PathResponse) cmd;
                    updatePath(pathResponse.getPath());
                    break;
                case FILE_MESSAGE:
                    FileMessage message = (FileMessage) cmd;
                    ClientPanelController cpc = (ClientPanelController) clientPanel.getProperties().get("ctrl");
                    Path currentDir = Paths.get(cpc.pathField.getText());
                    Files.write(currentDir.resolve(message.getName()), message.getBytes());
                    cpc.updateList(currentDir);
                    break;
                case LIST_RESPONSE:
                    ListResponse files = (ListResponse) cmd;

                    Platform.runLater(() -> {
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
                                    if (filesTable.getSelectionModel().getSelectedItem().getType() == FileInfo.FileType.DIRECTORY) {
                                        net.sendCmd(new PathInRequest(filesTable.getSelectionModel().getSelectedItem().getFilename()));
                                        updateList(files);
                                    }
                                }
                            }
                        });

                        updateList(files);
                    });
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

    public String getSelectedFilename() {
        if (!filesTable.isFocused()) {
            return null;
        }
        return filesTable.getSelectionModel().getSelectedItem().getFilename();
    }

    public void copyBtnAction(ActionEvent actionEvent) {

        ClientPanelController cpc = (ClientPanelController) clientPanel.getProperties().get("ctrl");

        if (getSelectedFilename() == null && cpc.getSelectedFilename() == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Ни один файл не был выбран", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        try {
            if (cpc.getSelectedFilename() != null) {
                Path srcPath = Paths.get(cpc.pathField.getText(), cpc.getSelectedFilename());
                net.sendCmd(new FileMessage(srcPath));
            }

            if (getSelectedFilename() != null) {
                net.sendCmd(new FileRequest(getSelectedFilename()));
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

    public void deleteBtnAction(ActionEvent actionEvent) {

        ClientPanelController cpc = (ClientPanelController) clientPanel.getProperties().get("ctrl");

        if (getSelectedFilename() == null && cpc.getSelectedFilename() == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Ни один файл не был выбран", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        try {
            if (cpc.getSelectedFilename() != null) {
                Path srcPath = Paths.get(cpc.pathField.getText(), cpc.getSelectedFilename());
                System.out.println(srcPath.toString());
                Files.deleteIfExists(srcPath);
                cpc.updateList(cpc.getCurrentPath());
            }

            if (getSelectedFilename() != null) {
                net.sendCmd(new DeleteRequest(getSelectedFilename()));
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

    public void btnExitAction(ActionEvent actionEvent) {
        Platform.exit();
        net.closeChannel();
//        System.exit(0);
    }

    public void btnPathUpAction(ActionEvent actionEvent) {
        net.sendCmd(new PathUpRequest());
    }

    public void doConsole(ActionEvent actionEvent) {
    }

}