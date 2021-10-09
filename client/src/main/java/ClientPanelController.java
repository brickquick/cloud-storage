import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import lombok.extern.slf4j.Slf4j;
import qbrick.FileInfo;

import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.StandardWatchEventKinds.*;

@Slf4j
public class ClientPanelController implements Initializable {

    @FXML
    TableView<FileInfo> filesTable;
    @FXML
    ComboBox<String> disksBox;
    @FXML
    TextField pathField;

    private final WatchService watchService = FileSystems.getDefault().newWatchService();

    private static final Path ROOT_DIR = Paths.get("client/root").toAbsolutePath();
    private static Path currentPath;

    public ClientPanelController() throws IOException {
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentPath = ROOT_DIR;
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
        fileDateColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getLastModified().format(dtf)));
        fileDateColumn.setPrefWidth(120);

        filesTable.getColumns().addAll(fileTypeColumn, filenameColumn, fileSizeColumn, fileDateColumn);
        filesTable.getSortOrder().add(fileTypeColumn);

        disksBox.getItems().clear();
        for (Path p : FileSystems.getDefault().getRootDirectories()) {
            disksBox.getItems().add(p.toString());
        }
        disksBox.getSelectionModel().select(0);

        filesTable.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    Path path = Paths.get(pathField.getText()).resolve(filesTable.getSelectionModel().getSelectedItem().getFilename());
                    if (Files.isDirectory(path)) {
                        updatePath(path);
                        updateList(path);
                    }
                }
            }
        });

        Thread watchThread = new Thread(() -> {
            while (true) {
                try {
                    WatchKey key = watchService.take();
                    if (key.isValid()) {
                        List<WatchEvent<?>> events = key.pollEvents();
                        for (WatchEvent<?> event: events) {
                            Path watchedPath = (Path) key.watchable();
                            System.out.println(watchedPath);
                            log.debug("kind {}, context {}", event.kind(), event.context());
                            Platform.runLater(() -> {
                                updateList(currentPath);
                            });
                            Thread.sleep(1000);
                        }
                        key.reset();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        watchThread.setDaemon(true);
        watchThread.start();

        updatePath(currentPath);
        updateList(currentPath);

    }

    public void updateList(Path path) {
        try {
            filesTable.getItems().clear();
            Stream<FileInfo> list = Files.list(path).map(FileInfo::new);
            filesTable.getItems().addAll(list.collect(Collectors.toList()));
            filesTable.sort();
            list.close();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "По какой-то причине не удалось обновить список файлов", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void clearList(Path path) {
        filesTable.getItems().clear();
    }

    public void updatePath(Path path) {
        try {
            pathField.setText(path.normalize().toAbsolutePath().toString());
            currentPath = path;
            currentPath.register(watchService, ENTRY_MODIFY, ENTRY_DELETE, ENTRY_CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getSelectedFilename() {
        if (!filesTable.isFocused()) {
            return null;
        }
        return filesTable.getSelectionModel().getSelectedItem().getFilename();
    }

    public Path getCurrentPath() {
        return Paths.get(pathField.getText());
    }

    public void selectDiskAction(ActionEvent actionEvent) {
        ComboBox<String> element = (ComboBox<String>) actionEvent.getSource();
        updatePath(Paths.get(element.getSelectionModel().getSelectedItem()));
        updateList(Paths.get(element.getSelectionModel().getSelectedItem()));
    }

    public void btnPathUpAction(ActionEvent actionEvent) {
        Path upperPath = getCurrentPath().getParent();
        if (upperPath != null) {
            updatePath(upperPath);
            updateList(upperPath);
        }
    }

    public void btnHomePathAction(ActionEvent actionEvent) {
        currentPath = ROOT_DIR;
        updatePath(currentPath);
        updateList(currentPath);
    }
}
