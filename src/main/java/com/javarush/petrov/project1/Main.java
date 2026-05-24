package com.javarush.petrov.project1;

import java.util.ResourceBundle;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import com.javarush.petrov.project1.services.CaesarCipherService;
import com.javarush.petrov.project1.services.FileLoader;
import com.javarush.petrov.project1.services.FileSaver;
import com.javarush.petrov.project1.services.FileService;
import com.javarush.petrov.project1.services.TextFileLoader;
import com.javarush.petrov.project1.services.TextFileSaver;

public class Main extends Application {
	// Инициализируем логгер для UI
	private static final Logger logger = Logger.getLogger(Main.class.getName());

	private FileService fileService;
	private CaesarCipherService cipherService; // Поле для сервиса шифрования
	private ResourceBundle bundle; // Поле для хранения ресурсов
	private TextArea textArea;
	private Button openButton;
	private Button saveButton;
	private Button encryptButton;
	private Button decryptButton;
	private Button bruteforceButton;
	private Button clearButton;
	private Spinner<Integer> shiftSpinner;
	private VBox rootLayout;
	// Компоненты для статистического анализа
	private ComboBox<String> methodSelector;
	private Button loadSampleButton;
	private String sampleText = ""; // Хранилище для текста-образца

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void init() {
		configureLogging(); // 1. Настраиваем логирование перед стартом приложения
		logger.info("Инициализация компонентов приложения...");
		// Загрузка ресурсов из файла messages.properties
		this.bundle = ResourceBundle.getBundle("messages");
		// Внедрение зависимостей (DI)
		FileLoader loader = new TextFileLoader();
		FileSaver saver = new TextFileSaver();
		this.fileService = new FileService(loader, saver);
		this.cipherService = new CaesarCipherService();
	}

	/**
	 * Конфигурация логирования: вывод в консоль и запись в файл app.log
	 */
	private void configureLogging() {
		try {
			// Создаем обработчик для записи в файл app.log (дозапись включена)
			FileHandler fileHandler = new FileHandler("app.log", true);
			fileHandler.setFormatter(new SimpleFormatter()); // Стандартный читаемый формат
			fileHandler.setLevel(Level.ALL);

			Logger rootLogger = Logger.getLogger("");
			rootLogger.addHandler(fileHandler);

			logger.info("Логирование успешно сконфигурировано. Файл логов: app.log");
		} catch (Exception e) {
			// IOException | SecurityException e
			System.err.println("Не удалось настроить запись логов в файл: " + e.getMessage());
		}
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		logger.info("Запуск главного графического интерфейса JavaFX...");
		// Установка заголовка из ресурсов
		primaryStage.setTitle(bundle.getString("window.title"));
		// UI Components с текстом из ресурсов
		openButton = new Button(bundle.getString("component.open_button"));
		saveButton = new Button(bundle.getString("component.save_button"));
		clearButton = new Button(bundle.getString("component.clear_button"));
		encryptButton = new Button(bundle.getString("component.encrypt_button"));
		decryptButton = new Button(bundle.getString("component.decrypt_button"));
		bruteforceButton = new Button(bundle.getString("component.bruteforce_button")); // Инициализация

		// Настройка Spinner на основе данных из сервиса
		int maxShift = cipherService.getMaxShift();
		shiftSpinner = new Spinner<>();
		SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, maxShift, 3);
		shiftSpinner.setValueFactory(valueFactory);
		shiftSpinner.setPrefWidth(70); // Ограничиваем ширину для аккуратного вида
		// Инициализация ComboBox для выбора метода криптоанализа
		String methodBruteforce = bundle.getString("method.bruteforce");
		String methodStats = bundle.getString("method.stats");
		methodSelector = new ComboBox<>(FXCollections.observableArrayList(methodBruteforce, methodStats));
		methodSelector.setValue(methodBruteforce); // По умолчанию брутфорс

		// Кнопка загрузки файла-образца (по умолчанию скрыта/отключена)
		loadSampleButton = new Button(bundle.getString("component.load_sample_button"));
		loadSampleButton.setDisable(true);
		textArea = new TextArea();
		textArea.setEditable(false);
		textArea.setPromptText(bundle.getString("component.prompt_text"));
		// Слушатель смены метода криптоанализа
		methodSelector.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
			logger.info("Пользователь изменил метод криптоанализа на: " + newValue);
			// Активируем кнопку загрузки образца, только если выбран Стат. анализ
			loadSampleButton.setDisable(!newValue.equals(methodStats));
		});
		// Button Actions
		// Обработка клика (Вызов сервиса вместо написания логики внутри UI)
		openButton.setOnAction(event -> {
			logger.info("Нажата кнопка 'Открыть файл'");
			String content = fileService.openAndReadFile(primaryStage);
			if (content != null) {
				textArea.setText(content);
			}
		});
		// Обработка клика: Сохранение файла с валидацией
		saveButton.setOnAction(event -> {
			logger.info("Нажата кнопка 'Сохранить файл'");
			String content = textArea.getText();
			if (validateNotEmpty(content, bundle.getString("error.empty_save"))) {
				fileService.saveFileAs(primaryStage, content);
			}
		});
		// Обработка клика: Шифрование с валидацией
		encryptButton.setOnAction(event -> {
			logger.info("Нажата кнопка 'Зашифровать'");
			String currentText = textArea.getText();
			if (validateNotEmpty(currentText, bundle.getString("error.empty_encrypt"))) {
				int shift = shiftSpinner.getValue();
				String encrypted = cipherService.encrypt(currentText, shift);
				textArea.setText(encrypted);
			}
		});
		// Обработка клика: Расшифрование с валидацией
		decryptButton.setOnAction(event -> {
			logger.info("Нажата кнопка 'Расшифровать'");
			String currentText = textArea.getText();
			if (validateNotEmpty(currentText, bundle.getString("error.empty_decrypt"))) {
				int shift = shiftSpinner.getValue();
				String decrypted = cipherService.decrypt(currentText, shift);
				textArea.setText(decrypted);
			}
		});
		// Обработка клика: Очистка текстового поля
		clearButton.setOnAction(event -> {
			logger.info("Нажата кнопка 'Очистить'. Сброс текстовых полей.");
			textArea.clear();
		});
		// Обработка клика: Автоматический Брутфорс/ Стат. Анализ
		bruteforceButton.setOnAction(event -> {
			logger.info("Нажата кнопка запуска криптоанализа");
			String currentText = textArea.getText();
			if (!validateNotEmpty(currentText, bundle.getString("error.empty_bruteforce"))) {
				return;
			}

			CaesarCipherService.BruteforceResult result;

			// Проверяем, какой метод выбран в ComboBox
			if (methodSelector.getValue().equals(methodStats)) {
				// Если выбран статистический метод — проверяем наличие загруженного образца
				if (sampleText == null || sampleText.trim().isEmpty()) {
					showErrorAlert(bundle.getString("error.empty_sample"));
					return;
				}
				result = cipherService.statsAnalysis(currentText, sampleText);
			} else {
				// Иначе стандартный брутфорс по знакам препинания
				result = cipherService.autoBruteforce(currentText);
			}

			String successTemplate = bundle.getString("bruteforce.output_success");
			String formattedResult = String.format(successTemplate, result.getKey(), result.getDecryptedText());
			textArea.setText(formattedResult);
		});
		// Загрузка файла-образца
		loadSampleButton.setOnAction(event -> {
			logger.info("Нажата кнопка 'Загрузить образец'");
			String content = fileService.openAndReadFile(primaryStage);
			if (content != null) {
				this.sampleText = content;
				logger.info("Файл-образец успешно считан в память приложения.");
				// Показываем сообщение, что данные успешно усвоены
				Alert info = new Alert(AlertType.INFORMATION);
				info.setTitle("Успех");
				info.setHeaderText(null);
				info.setContentText(bundle.getString("info.sample_loaded"));
				info.showAndWait();
			} else {
				logger.warning("Загрузка файла-образца была отменена пользователем.");
			}
		});
		// --- ГРУППИРОВКА ПАНЕЛЕЙ ---
		// Панель 1: Управление файлами и текстом
		HBox filePanel = new HBox(10, openButton, saveButton, clearButton);
		filePanel.setAlignment(Pos.CENTER_LEFT); // Выравнивание по левому краю

		// Панель 2: Инструменты шифрования с выбором метода криптоанализа
		Label methodLabel = new Label(bundle.getString("component.analysis_method_label"));
		HBox cryptoPanel = new HBox(10, encryptButton, shiftSpinner, decryptButton,
				new Separator(javafx.geometry.Orientation.VERTICAL), methodLabel, methodSelector, loadSampleButton,
				bruteforceButton);
		cryptoPanel.setAlignment(Pos.CENTER_LEFT);

		// Тонкая горизонтальная линия-разделитель между панелями
		Separator separator = new Separator();
		separator.setPadding(new Insets(5, 0, 5, 0));

		// Контейнер управления (объединяет обе панели и разделитель)
		VBox controlsContainer = new VBox(5, filePanel, separator, cryptoPanel);
		controlsContainer.setPadding(new Insets(0, 0, 5, 0));

		// Главный контейнер всего окна
		rootLayout = new VBox(10, controlsContainer, textArea);
		rootLayout.setPadding(new Insets(15));
		// TextArea grow priority setup
		VBox.setVgrow(textArea, javafx.scene.layout.Priority.ALWAYS);
		primaryStage.setScene(new Scene(rootLayout, 1050, 550));
		primaryStage.show();
	}

	/**
	 * Универсальная валидация текста на пустоту.
	 * 
	 * @param text         проверяемый текст
	 * @param errorMessage текст ошибки для отображения пользователю
	 * @return true, если текст не пустой; false в противном случае
	 */
	private boolean validateNotEmpty(String text, String errorMessage) {
		if (text == null || text.trim().isEmpty()) {
			logger.warning("Сработала валидация: обнаружено пустое текстовое поле.");
            showErrorAlert(errorMessage);
			return false;
		}
		return true;
	}

	/**
	 * Вспомогательный метод для отображения диалогового окна с ошибкой (ERROR).
	 */
	private void showErrorAlert(String message) {
		Alert alert = new Alert(AlertType.ERROR);
		alert.setTitle(bundle.getString("error.title"));
		alert.setHeaderText(null);
		alert.setContentText(message);
		alert.showAndWait();
	}
}
