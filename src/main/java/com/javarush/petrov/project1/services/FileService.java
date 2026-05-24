package com.javarush.petrov.project1.services;

import java.io.File;
import java.io.IOException;

import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class FileService {
	private final FileLoader fileLoader;
	private final FileSaver fileSaver;

	// Зависимости внедряются через конструктор
    public FileService(FileLoader fileLoader, FileSaver fileSaver) {
        this.fileLoader = fileLoader;
        this.fileSaver = fileSaver;
    }

	public String openAndReadFile(Stage stage) {
		FileChooser fileChooser = createTextFileChooser("Выберите текстовый файл");
        File selectedFile = fileChooser.showOpenDialog(stage);
        
        if (selectedFile != null) {
            try {
                return fileLoader.loadContent(selectedFile);
            } catch (IOException e) {
                return "Ошибка при чтении файла: " + e.getMessage();
            }
        }
        return null;
	}
	
	public boolean saveFileAs(Stage stage, String content) {
        FileChooser fileChooser = createTextFileChooser("Сохранить файл как...");
        File selectedFile = fileChooser.showSaveDialog(stage);
        
        if (selectedFile != null) {
            try {
                fileSaver.saveContent(selectedFile, content);
                return true;
            } catch (IOException e) {
                System.err.println("Ошибка при сохранении файла: " + e.getMessage());
                return false;
            }
        }
        return false;
    }
	
	private FileChooser createTextFileChooser(String title) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Text Files (*.txt)", "*.txt")
        );
        return fileChooser;
    }
	
//	private void showError(String title, String message) {
//        Alert alert = new Alert(AlertType.ERROR);
//        alert.setTitle(title);
//        alert.setHeaderText(null);
//        alert.setContentText(message);
//        alert.showAndWait();
//    }
}

