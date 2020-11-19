import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;


public class ControllerFX implements Initializable {

    @FXML
    public TextField usernameField;

    @FXML
    public PasswordField passwordField;

    @FXML
    public ListView localList;

    @FXML
    public ListView cloudList;

    @FXML
    public HBox authPanel;

    @FXML
    public HBox actionPanel1;

    @FXML
    public HBox actionPanel2;

    public CloudClient client;
    private String command;
    private ObservableList<String> localFilesList;
    private ObservableList<String> cloudFilesList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        client = new CloudClient("localhost", 31337);
        localFilesList = FXCollections.observableArrayList();
        cloudFilesList = FXCollections.observableArrayList();

        localList.setItems(localFilesList);
        cloudList.setItems(cloudFilesList);

        //-- Устанавливаем реакцию на двойной клик но локалке
        localList.setOnMouseClicked(mouseEvent -> {
            if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
                if (mouseEvent.getClickCount() == 2) {
                    System.out.println("Двойной клик на список");
                    goDeeper((ListView) mouseEvent.getSource());
                }
            }
        });

        //-- Устанавливаем реакцию на двойной клик в облаке
        cloudList.setOnMouseClicked(mouseEvent -> {
            if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
                if (mouseEvent.getClickCount() == 2) {
                    System.out.println("Двойной клик на список");
                    goDeeper((ListView) mouseEvent.getSource());
                }
            }
        });
    }

    public void setCloudFilesList(List<String> list) {
        if (list != null) {
            Platform.runLater(() -> {
                cloudFilesList.clear();
                if (list.size() > 0) {
                    cloudFilesList.addAll(list);
                    cloudFilesList.add(0, "..");
                } else
                    cloudFilesList.add("На сервере нет файлов");
            });
        }

    }

    public void login() {
        System.out.println("Попытка соединения...");
        client.connect();

        String login = usernameField.getText().trim();
        String pass = passwordField.getText().trim();
        client.startReadingThread(this);
        client.doAuth(login, pass);

    }

    private void sendFile(String fileName) {
        client.sendFileToStorage(fileName);
    }

    public void refreshFolderList() {
        client.listCloudFiles(null);
    }


    //-- Отправляем команды на отправку файла или директории из локального хранилища в облачное
    public void uploadFileOrFolder(ActionEvent event) {
        String itemName = localList.getItems()
                .get(localList
                        .getFocusModel()
                        .getFocusedIndex())
                .toString();

        client.uploadFileOrFolder(itemName);
    }

    //--  Отправляет на сервер запрос на закачку файла в локальное хранилище
    public void downloadFile(ActionEvent event) {
        String fileName = cloudList.getItems().get(
                cloudList.getFocusModel().getFocusedIndex()).toString();
        System.out.println(fileName);

        client.downloadFile(fileName);
        updateLocalFilesList();
    }

    //-- Отправляет команды на удаление файла или папки в облачном хранилище
    public void deleteCloudFsObj(ActionEvent event) {
        String fileName = cloudList.getItems().get(
                cloudList.getFocusModel().getFocusedIndex()).toString();
        System.out.println("Удалить из облачного хранилища " + fileName);

        client.deleteCloudFsObj(fileName);
    }

    //-- Удаляет локальные файлы
    public void deleteLocalFile() {
        String fileName = localList.getItems().get(
                localList.getFocusModel().getFocusedIndex()).toString();
        client.deleteLocalFile(fileName);
        updateLocalFilesList();
    }

    public void dragDropFile(DragEvent dragEvent) {
        System.out.println("Перетащи нужный файл");
        dragEvent.acceptTransferModes(TransferMode.LINK);
        Dragboard dragboard = dragEvent.getDragboard();
        List<File> files;

        if (dragboard.hasFiles()) {
            files = dragboard.getFiles();
            for (int i = 0; i < files.size(); i++) {
                System.out.println("Отправить файл " + files.get(i).getName());
                sendFile(files.get(i).getName());
            }
        }
    }

    //-- Активирует панели с кнопками, в случае успешного логина на сервер
    public void loginOk() {
        authPanel.setVisible(false);
        authPanel.setManaged(false);

        actionPanel1.setVisible(true);
        actionPanel2.setVisible(true);

        actionPanel1.setManaged(true);
        actionPanel2.setManaged(true);

        updateLocalFilesList();
        client.listCloudFiles(null);
    }

    //-- Обновляет список файлов, находящийся в локальном хранилище
    public void updateLocalFilesList() {
        localList.getItems().clear();

        List<String> list = client.getLocalFilesList();

        if (list.size() > 0) {
            localFilesList.addAll(list);
        } else
            localFilesList.add("В локальной папке нет файлов!");

        localFilesList.add(0, "..");
    }

    //-- Устанавливает функцию двойного клика для перехода по путям
    //-- при двойном нажатии на ".." - переход на уровень выше
    private void goDeeper(ListView listView) {
        String itemName = listView.getItems()
                .get(listView
                        .getFocusModel()
                        .getFocusedIndex())
                .toString();

        if (itemName.equals("..")) {
            //-- переход на уровень выше
            if (listView.equals(localList)) {
                client.setRootDir(Paths.get(client.getRootDir()).getParent().toString());
                updateLocalFilesList();
            } else if (listView.equals(cloudList)) {
                command = "..";
                client.listCloudFiles(command);
                command = "";
            }
        } else {
            //-- переход в каталог с именем itemName
            if (listView.equals(localList)) {
                Path path = Paths.get(client.getRootDir(), itemName);
                if (Files.isDirectory(path)) {
                    client.setRootDir(client.getRootDir() + "\\" + itemName + "\\");
                    updateLocalFilesList();
                }
            } else if (listView.equals(cloudList)) {
                command = itemName;
                client.listCloudFiles(itemName);
                command = "";
            }
        }
    }
}
