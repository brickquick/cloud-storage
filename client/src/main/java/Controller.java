import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import lombok.extern.slf4j.Slf4j;
import qbrick.*;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Optional;
import java.util.ResourceBundle;

@Slf4j
public class Controller implements Initializable {

    @FXML
    VBox clientPanel;

    @FXML
    public HBox hbox;
    @FXML
    TableView<FileInfo> serverFilesTable;
    @FXML
    TextField serverPathField;

    @FXML
    public VBox console;
    @FXML
    public ListView<String> listView;
    @FXML
    public TextField input;

    private Net net;
    private boolean authOk = false;

    private volatile boolean cancelUpload = false;
    private volatile long uploadStart = 0;
    private volatile long downloadStart = 0;
    private volatile int lastLength = 0;
    private int byteRead;
    private FileMessage fileUploadFile;

    private ProgressForm progressForm;
    private CreateDirForm createDirForm;
    private AuthForm authForm;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        connectToNet();
        hbox.getChildren().remove(console);
    }

    private void connectToNet() {
        Platform.runLater(() -> {
            authForm = new AuthForm();
            authForm.activateForm();
        });
        net = new Net(cmd -> {
            switch (cmd.getType()) {
                case AUTHENTICATION:
                    Authentication authentication = (Authentication) cmd;
                    authOk = authentication.isAuthOk();
                    Platform.runLater(() -> {
                        if (!authOk) {
                            if (!authForm.getLoginField().getText().equals("") || !authForm.getPassField().getText().equals("")) {
                                authForm.getTopLabel().setTextFill(Color.color(1, 0, 0));
                                authForm.getTopLabel().setText("Неверные логин или пароль:");
                            }
                            authForm.getLoginField().setOnAction(action -> {
                                if (!authForm.getLoginField().getText().equals("")) {
                                    authForm.getPassField().requestFocus();
                                    authForm.getLabelLogin().setTextFill(Color.color(0, 0, 0));
                                    authForm.getLabelLogin().setText("Логин:");
                                } else {
                                    authForm.getLabelLogin().setTextFill(Color.color(1, 0, 0));
                                    authForm.getLabelLogin().setText("Логин пуст:");
                                }
                            });
                            authForm.getPassField().setOnAction(action -> {
                                if (!authForm.getPassField().getText().equals("")) {
                                    net.sendCmd(new Authentication(authForm.getLoginField().getText(),
                                            authForm.getPassField().getText()));
                                    authForm.getLabelPass().setTextFill(Color.color(0, 0, 0));
                                    authForm.getLabelPass().setText("Пароль:");
                                    if (authForm.getLoginField().getText().equals("")){
                                        authForm.getLoginField().requestFocus();
                                        authForm.getLabelLogin().setTextFill(Color.color(1, 0, 0));
                                        authForm.getLabelLogin().setText("Логин пуст:");
                                    }
                                } else {
                                    authForm.getLabelPass().setTextFill(Color.color(1, 0, 0));
                                    authForm.getLabelPass().setText("Password is empty:");
                                }
                            });
                            authForm.activateForm();
                        } else {
                            net.sendCmd(new ListRequest());
                            authForm.closeForm();
                        }
                    });
                    break;
                case DOWNLOAD_STATUS:
                    DownloadStatus downloadStatus = (DownloadStatus) cmd;
                    uploadStart = downloadStatus.getStart();
                    try (RandomAccessFile randomAccessFile = new RandomAccessFile(fileUploadFile.getFile(), "r")) {
                        double pr = (double) uploadStart * 100L / randomAccessFile.length();
                        log.debug("start: " + uploadStart + "; % = " + pr);
                        progressForm.setProgress(pr / 100);
                        progressForm.getCancelBtn().setOnAction(action -> {
                            uploadStart = -1;
                            cancelUpload = true;
                            try {
                                Thread.sleep(1000);
                                net.sendCmd(new DeleteRequest(fileUploadFile.getName()));
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            progressForm.getDialogStage().close();
                        });

                        if (uploadStart != -1 && !cancelUpload) {
                            randomAccessFile.seek(uploadStart);
                            log.debug("(randomAccessFile.length() - start)：" + (randomAccessFile.length() - uploadStart));
                            long a = randomAccessFile.length() - uploadStart;
                            if (a < lastLength) {
                                lastLength = (int) a;
                            }
                            log.debug("randomAccessFile.length()：" + randomAccessFile.length() + ",start:" + uploadStart + ",a:" + a + ",lastLength:" + lastLength);
                            byte[] bytes = new byte[lastLength];
                            log.debug("bytes.length=" + bytes.length);
                            if ((byteRead = randomAccessFile.read(bytes)) != -1 && (randomAccessFile.length() - uploadStart) > 0) {
                                log.debug("byteRead = " + byteRead);
                                fileUploadFile.setEndPos(byteRead);
                                fileUploadFile.setBytes(bytes);
                                try {
                                    net.sendCmd(fileUploadFile);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            } else {
                                log.debug("byteRead channelRead()--------" + byteRead);
                                fileUploadFile.setEndPos(-1);
                                net.sendCmd(fileUploadFile);
                                fileUploadFile = null;
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case CREATE_DIR_REQUEST:
                    CreateDirRequest createDirRequest = (CreateDirRequest) cmd;
                    System.out.println("createDirRequest " + createDirRequest.isPossible());
                    if (createDirRequest.isPossible()) {
                        Platform.runLater(() -> {
                            createDirForm.closeForm();
                        });
                    } else {
                        Platform.runLater(() -> {
                            createDirForm.getLabel().setTextFill(Color.color(1, 0, 0));
                            createDirForm.getLabel().setText("Директория с именем '" + createDirRequest.getName()
                                    + "' уже существует");
                        });
                    }
                    break;
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
                    FileMessage fileMessage = (FileMessage) cmd;
                    ClientPanelController cpc = (ClientPanelController) clientPanel.getProperties().get("ctrl");
                    byte[] bytes = fileMessage.getBytes();
                    int endPos = fileMessage.getEndPos();
                    String fileName = fileMessage.getName();
                    String path = cpc.getCurrentPath() + File.separator + fileName;
                    File file = new File(path);
                    try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw")) {
                        if (endPos >= 0) {
                            randomAccessFile.seek(downloadStart);
                            randomAccessFile.write(bytes);
                            downloadStart = downloadStart + endPos;
                            log.debug("start: " + downloadStart);
                            net.sendCmd(new DownloadStatus(downloadStart));
                        } else {
                            downloadStart = 0;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
//                    Files.write(currentDir.resolve(message.getName()), message.getBytes());
                    cpc.updateList(cpc.getCurrentPath());
                    progressForm.setProgress(fileMessage.getProgress() / 100);
                    progressForm.getCancelBtn().setOnAction(action -> {
                        downloadStart = -1;
                        net.sendCmd(new DownloadStatus(downloadStart));
                        try {
                            Thread.sleep(1000);
                            Files.deleteIfExists(Paths.get(path));
                        } catch (IOException | InterruptedException e) {
                            e.printStackTrace();
                        }
                        progressForm.getDialogStage().close();
                    });
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
                        try {
                            fileDateColumn.setCellValueFactory(param ->
                                    new SimpleStringProperty(param.getValue().getLastModified().format(dtf)));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        fileDateColumn.setPrefWidth(120);

                        serverFilesTable.getColumns().clear();
                        serverFilesTable.getColumns().addAll(fileTypeColumn, filenameColumn, fileSizeColumn, fileDateColumn);
                        serverFilesTable.getSortOrder().add(fileTypeColumn);

                        serverFilesTable.setOnMouseClicked(new EventHandler<MouseEvent>() {
                            @Override
                            public void handle(MouseEvent event) {
                                if (event.getClickCount() == 2) {
                                    try {
                                        if (serverFilesTable.getSelectionModel().getSelectedItem().getType() == FileInfo.FileType.DIRECTORY) {
                                            net.sendCmd(new PathInRequest(serverFilesTable.getSelectionModel().getSelectedItem().getFilename()));
                                            updateList(files);
                                        }
                                    } catch (Exception ignored) {
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
        serverPathField.setText(path);
    }

    public void updateList(ListResponse files) {
        serverFilesTable.getItems().clear();
        serverFilesTable.getItems().addAll(new ArrayList<>(files.getFileInfos()));
        serverFilesTable.sort();
    }

    public String getSelectedFilename() {
        if (!serverFilesTable.isFocused()) {
            return null;
        }
        return serverFilesTable.getSelectionModel().getSelectedItem().getFilename();
    }

    public void copyBtnAction(ActionEvent actionEvent) {
        ClientPanelController clientPC = (ClientPanelController) clientPanel.getProperties().get("ctrl");

        try {
            if (getSelectedFilename() == null && clientPC.getSelectedFilename() == null) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Ни один файл не был выбран", ButtonType.OK);
                alert.showAndWait();
                return;
            }
        } catch (NullPointerException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Ни один файл не был выбран", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        try {
            if (clientPC.getSelectedFilename() != null) {
                Path srcFilePath = Paths.get(clientPC.pathField.getText(), clientPC.getSelectedFilename());
                fileUploadFile = new FileMessage(srcFilePath);

                try (RandomAccessFile randomAccessFile = new RandomAccessFile(fileUploadFile.getFile(), "r")) {
                    randomAccessFile.seek(fileUploadFile.getStarPos());
//                    lastLength = (int) randomAccessFile.length() / 10;
//                    lastLength = 1024 * 10;
                    lastLength = 1048576 / 2;
                    if (randomAccessFile.length() < lastLength) {
                        lastLength = (int) randomAccessFile.length();
                    }
                    byte[] bytes = new byte[lastLength];
                    if ((byteRead = randomAccessFile.read(bytes)) != -1) {
                        fileUploadFile.setEndPos(byteRead);
                        fileUploadFile.setBytes(bytes);
                        net.sendCmd(fileUploadFile);
                    } else {
                    }

                    uploadStart = 0;
                    cancelUpload = false;
                    progressForm = new ProgressForm(fileUploadFile.getName());
                    progressForm.setProgress(uploadStart);
                    progressForm.activateProgressBar();

                    log.debug("channelActive: " + byteRead);
                } catch (IOException e) {
                    e.printStackTrace();
                }
//                net.sendCmd(new FileMessage(srcPath));
            }

            if (getSelectedFilename() != null) {
                downloadStart = 0;
                net.sendCmd(new FileRequest(getSelectedFilename()));

                progressForm = new ProgressForm(getSelectedFilename());
                progressForm.setProgress(downloadStart);
                progressForm.activateProgressBar();
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

    public void createDirBtnAction(ActionEvent actionEvent) {
        ClientPanelController clientPC = (ClientPanelController) clientPanel.getProperties().get("ctrl");

        if (!clientPC.isFocusedTable() && !serverFilesTable.isFocused() && !serverPathField.isFocused()) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Ни одно расположение не было выбрано", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        try {
            if (clientPC.isFocusedTable()) {
                createDirForm = new CreateDirForm();
                createDirForm.activateForm();
                createDirForm.getTextField().setOnAction(action -> {
                    try {
                        Files.createDirectory(Paths.get(clientPC.getCurrentPath().toString(),
                                createDirForm.getTextField().getText()));
                        createDirForm.closeForm();
                    } catch (FileAlreadyExistsException ex) {
                        createDirForm.getLabel().setTextFill(Color.color(1, 0, 0));
                        if (createDirForm.getTextField().getText().equals("")) {
                            createDirForm.getLabel().setText("Задано пустое имя директории");
                        } else {
                            createDirForm.getLabel().setText("Директория с именем '" + createDirForm.getTextField().getText()
                                    + "' уже существует");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }

            if (serverFilesTable.isFocused() || serverPathField.isFocused()) {
                createDirForm = new CreateDirForm();
                createDirForm.activateForm();
                createDirForm.getTextField().setOnAction(action -> {
                    if (createDirForm.getTextField().getText().equals("")) {
                        createDirForm.getLabel().setTextFill(Color.color(1, 0, 0));
                        createDirForm.getLabel().setText("Задано пустое имя директории");
                    } else {
                        net.sendCmd(new CreateDirRequest(createDirForm.getTextField().getText()));
                    }
                });
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void deleteBtnAction(ActionEvent actionEvent) {

        ClientPanelController cpc = (ClientPanelController) clientPanel.getProperties().get("ctrl");

        try {
            if (getSelectedFilename() == null && cpc.getSelectedFilename() == null) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Ни один файл не был выбран", ButtonType.OK);
                alert.showAndWait();
                return;
            }
        } catch (NullPointerException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Ни один файл не был выбран", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Подтвердите удаление файла", ButtonType.OK, ButtonType.CANCEL);
        alert.setTitle("Delete File");

        if (cpc.getSelectedFilename() != null) {
            try {
                Path srcPath = Paths.get(cpc.pathField.getText(), cpc.getSelectedFilename());

                alert.setHeaderText(srcPath.normalize().toString());
                Optional<ButtonType> option = alert.showAndWait();
                if (option.isPresent() && option.get() == ButtonType.OK) {
                    File srcFile = new File(String.valueOf(Paths.get(cpc.pathField.getText(), cpc.getSelectedFilename())));
                    if (srcFile.isDirectory()) {
                        if (isDirectoryEmpty(srcFile)) {
                            Files.deleteIfExists(srcPath);
                            System.out.println(srcPath + " delete");
                        } else {
                            deleteDirRecursively(srcFile);
                            Thread.sleep(1000);
                            if (isDirectoryEmpty(srcFile)) {
                                Files.deleteIfExists(srcPath);
                            }
                        }
                    } else {
                        Files.deleteIfExists(srcPath);
                        cpc.updateList(cpc.getCurrentPath());
                    }
                }
//                Thread.sleep(500);
                cpc.updateList(cpc.getCurrentPath());

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (getSelectedFilename() != null) {
            alert.setHeaderText(serverPathField.getText() + "\\" + getSelectedFilename());
            Optional<ButtonType> option = alert.showAndWait();
            if (option.isPresent() && option.get() == ButtonType.OK) {
                net.sendCmd(new DeleteRequest(getSelectedFilename()));
            }
        }


    }

    public void deleteDirRecursively(File baseDirectory) throws IOException {
        File[] files = baseDirectory.listFiles();
        assert files != null;
        for (File file : files) {
            if (file.isFile()) {
                Files.deleteIfExists(Paths.get(file.getPath()));
                System.out.println(file.getName() + " файл удален");
            } else if (file.isDirectory()) {
                while (!isDirectoryEmpty(file)) {
                    log.debug(file.getName() + " Not empty");
                    System.out.println(file.getName() + " визит");
                    deleteDirRecursively(file);
                }
                log.debug(file.getName() + " empty");
                Files.deleteIfExists(Paths.get(file.getPath()));
                System.out.println(file.getName() + " каталог удален");
            }
        }
    }

    public boolean isDirectoryEmpty(File directory) {
        String[] files = directory.list();
        try {
            if (files != null) {
                return files.length == 0;
            } else {
                System.out.println("isDirectoryEmpty - true");
                return true;
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
            System.out.println("isDirectoryEmpty - truE");
            return true;
        }
    }

    public void btnExitAction(ActionEvent actionEvent) {
        Platform.exit();
        net.closeChannel();
    }

    public void btnPathUpAction(ActionEvent actionEvent) {
        net.sendCmd(new PathUpRequest());
    }

    public void showConsole(ActionEvent actionEvent) {
        console.setVisible(!console.isVisible());
        if (console.isVisible()) {
            console.setPrefWidth(300);
            hbox.getChildren().add(console);
        } else {
            console.setPrefWidth(0);
            hbox.getChildren().remove(console);
        }
    }

    public void send(ActionEvent actionEvent) {
        net.sendCmd(new ConsoleMessage(input.getText()));
        input.clear();
    }

    public void btnHomePathAction(ActionEvent actionEvent) {
        net.sendCmd(new ListRequest());
    }

    public void connectToServer(ActionEvent actionEvent) {
        if (!net.isConnected() || !authOk) {
            connectToNet();
        }
    }

}