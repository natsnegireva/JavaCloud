package common;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class FSWorker {

    //-- Общие для клиентов и сервера методы, в конструкторе задаем корневой каталог rootDir.
    private String rootDir;

    public FSWorker(String rootDir) {
        this.rootDir = rootDir;
    }

    public FSWorker() {
    }

    //-- Setter для установки значения поля - корневого каталога пользователя
    public void setRootDir(String rootDir) {
        this.rootDir = rootDir;
    }

    //-- Просмотр содержимого каталога
    public List<String> listDir(Path dirPath) {
        List<String> fileList = new ArrayList<>();

        try (DirectoryStream<Path> str = Files.newDirectoryStream(dirPath)) {

            str.forEach(path -> fileList.add(
                    path.getFileName().toString()));

        } catch (IOException e) {
            e.printStackTrace();
        }

        return fileList;
    }

    //-- Создаем каталог
    public void mkDir(Path newPath) {
        try {
            Files.createDirectories(newPath);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Ошибка создания директории!");
        }
    }

    //-- Создаем файл
    public void mkFile(Path newFilePath, byte[] data) {
        StandardOpenOption sOption;

        if (Files.exists(newFilePath)) {
            sOption = StandardOpenOption.TRUNCATE_EXISTING;
        } else {
            sOption = StandardOpenOption.CREATE;
        }

        try {
            Files.write(newFilePath, data, sOption);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //-- Удаляем файл или каталог с файлами
    public void delFsObject(String fsObjectName) {

        Path newPath = Paths.get(fsObjectName);
        Path pathToDelete = Paths.get(rootDir, "\\",
                newPath.subpath(2, newPath.getNameCount()).toString());

        if (Files.isDirectory(pathToDelete)) {
            deleteDirectory(pathToDelete);

        } else if (Files.isRegularFile(pathToDelete)) {
            deleteFileFromStorage(pathToDelete);

        }
    }

    //-- Удаляем файл из хранилища по пути
    public void deleteFileFromStorage(Path pathToDelete) {
        try {
            Files.delete(pathToDelete);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //-- Удаляем каталог с файлами
    private void deleteDirectory(Path pathToDelete) {
        try {
            Files.walkFileTree(pathToDelete, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    System.out.println("Удалить файл: " + file.toString());
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    System.out.println("Удалить директорию: " + dir.toString());
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
