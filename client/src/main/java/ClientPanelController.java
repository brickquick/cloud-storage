import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;
import qbrick.FileInfo;

import java.io.File;
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
    public TableView<FileInfo> filesTable;
    @FXML
    public Button btnHome, btnUp;
    @FXML
    ComboBox<String> disksBox;
    @FXML
    TextField pathField;

    private final Media soundClick = new Media(new File("client/src/main/resources/sounds/ClickH3.mp3").toURI().toString());
    private MediaPlayer mediaPlayer;

    private WatchService watchService;

    private static final Path ROOT_DIR = Paths.get("client/root").toAbsolutePath();
    private static Path currentPath;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        mediaPlayer = new MediaPlayer(soundClick);
        currentPath = ROOT_DIR;
        TableColumn<FileInfo, String> fileTypeColumn = new TableColumn<>();
        fileTypeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getType().getName()));
        fileTypeColumn.setPrefWidth(24);

        TableColumn<FileInfo, String> filenameColumn = new TableColumn<>("Имя");
        filenameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFilename()));
        filenameColumn.setPrefWidth(190);

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

        try {
            watchService = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Thread watchThread = new Thread(() -> {
            while (true) {
                try {
                    WatchKey key = watchService.take();
                    if (key.isValid()) {
                        List<WatchEvent<?>> events = key.pollEvents();
                        for (WatchEvent<?> event : events) {
                            Path watchedPath = (Path) key.watchable();
                            System.out.println(watchedPath);
                            log.debug("kind {}, context {}", event.kind(), event.context());
                            Platform.runLater(() -> updateList(currentPath));
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

    public void updatePath(Path path) {
        try {
            pathField.setText(path.normalize().toAbsolutePath().toString());
            currentPath = path;
            currentPath.register(watchService, ENTRY_MODIFY, ENTRY_DELETE, ENTRY_CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void selectDiskAction(ActionEvent actionEvent) {
        mediaPlayer.seek(new Duration(0));
        mediaPlayer.play();
        ComboBox<String> element = (ComboBox<String>) actionEvent.getSource();
        updatePath(Paths.get(element.getSelectionModel().getSelectedItem()));
        updateList(Paths.get(element.getSelectionModel().getSelectedItem()));
    }

    public void btnPathUpAction() {
        mediaPlayer.seek(new Duration(0));
        mediaPlayer.play();
        Path upperPath = getCurrentPath().getParent();
        if (upperPath != null) {
            updatePath(upperPath);
            updateList(upperPath);
        }
    }

    public void btnHomePathAction(ActionEvent actionEvent) {
        mediaPlayer.seek(new Duration(0));
        mediaPlayer.play();
        currentPath = ROOT_DIR;
        updatePath(currentPath);
        updateList(currentPath);
    }

    public void pathIn() {
        mediaPlayer.seek(new Duration(0));
        mediaPlayer.play();
        try {
            Path path = Paths.get(pathField.getText()).resolve(filesTable.getSelectionModel().getSelectedItem().getFilename());
            if (Files.isDirectory(path)) {
                updatePath(path);
                updateList(path);
            }
        } catch (Exception ignored) {
        }
    }

    public String getSelectedFilename() {
        if (!filesTable.isFocused()) {
            return null;
        }
        return filesTable.getSelectionModel().getSelectedItem().getFilename();
    }

    public String getSelectedType() {
        if (!filesTable.isFocused()) {
            return null;
        }
        return filesTable.getSelectionModel().getSelectedItem().getType().getName();
    }

    public boolean isFocusedTable() {
        return filesTable.isFocused() || pathField.isFocused();
    }

    public Path getCurrentPath() {
        return Paths.get(pathField.getText());
    }

    public TableView<FileInfo> getFilesTable() {
        return filesTable;
    }
}
