package com.javarush.petrov.project1.services;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger; // Импорт встроенного логгера

public class CaesarCipherService {
	// Создаем экземпляр логгера для текущего класса
	private static final Logger logger = Logger.getLogger(CaesarCipherService.class.getName());

	// Криптографический алфавит: русский (заглавные + строчные), знаки препинания и
	// пробел
	private static final String ALPHABET = "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ" + "абвгдеёжзийклмнопрстуфхцчшщъыьэюя"
			+ ".,\":-!? ";

	/**
	 * Возвращает максимальное значение сдвига на основе длины алфавита.
	 */
	public int getMaxShift() {
		return ALPHABET.length() - 1;
	}

	/**
	 * Шифрует текст методом Цезаря по заданному алфавиту.
	 */
	public String encrypt(String text, int shift) {
		logger.info("Запущено шифрование текста со сдвигом: " + shift);
		return transform(text, shift);
	}

	/**
	 * Расшифровывает текст методом Цезаря (сдвиг в обратную сторону).
	 */
	public String decrypt(String text, int shift) {
		logger.info("Запущено шифрование текста со сдвигом: " + shift);
		int decryptShift = ALPHABET.length() - (shift % ALPHABET.length());
		return transform(text, decryptShift);
	}

	/**
	 * Общий внутренний метод для перемещения по алфавиту.
	 */
	private String transform(String text, int shift) {
		if (text == null || text.isEmpty()) {
			return "";
		}

		StringBuilder result = new StringBuilder();
		int alphabetLength = ALPHABET.length();

		for (char character : text.toCharArray()) {
			int index = ALPHABET.indexOf(character);

			// Если символ найден в криптографическом алфавите — сдвигаем его
			if (index != -1) {
				int newIndex = (index + shift) % alphabetLength;
				result.append(ALPHABET.charAt(newIndex));
			} else {
				// Если не найден (например, латиница, цифры или переносы строк) — оставляем как
				// есть
				result.append(character);
			}
		}
		return result.toString();
	}

	/**
	 * Выполняет криптоанализ методом брутфорса. Автоматически подбирает на основе
	 * анализа правильного расположения пробелов и знаков препинания.
	 * 
	 * @param encryptedText зашифрованный текст
	 * @return объект с результатами взлома (ключ и расшифрованный текст)
	 */
	public BruteforceResult autoBruteforce(String encryptedText) {
		logger.info("Запущен криптоанализ методом Брутфорс (по знакам препинания)");
		if (encryptedText == null || encryptedText.isEmpty()) {
			return new BruteforceResult(0, "");
		}

		int maxShift = getMaxShift();
		int bestShift = 1;
		int maxScore = -1;
		String bestDecryptedText = "";

		// Перебираем все возможные ключи
		for (int shift = 1; shift <= maxShift; shift++) {
			String decryptedVariant = decrypt(encryptedText, shift);

			// Считаем качество текущего варианта текста
			int currentScore = calculateTextScore(decryptedVariant);

			// Запоминаем текст с максимальным качеством
			if (currentScore > maxScore) {
				maxScore = currentScore;
				bestShift = shift;
				bestDecryptedText = decryptedVariant;
			}
		}
		logger.info("Брутфорс успешно завершен. Подобран оптимальный ключ: " + bestShift);
		return new BruteforceResult(bestShift, bestDecryptedText);
	}

	/**
	 * Оценивает «осмысленность» текста на основе правил пунктуации. Проверяет
	 * паттерны: "слово, пробел", "слово. пробел", "слово пробел слово".
	 */
	private int calculateTextScore(String text) {
		int score = 0;

		// Паттерн 1: Буква, затем знак препинания (. , ! ? :), затем пробел
		// Пример: "привет, " или "да! "
		java.util.regex.Matcher punctuationMatcher = java.util.regex.Pattern.compile("[а-яА-ЯёЁ][.,!?:\\-][ ]")
				.matcher(text);
		while (punctuationMatcher.find()) {
			score += 5; // Высокий приоритет для правильных знаков препинания
		}

		// Паттерн 2: Обычные пробелы между русскими словами
		// Пример: "русское слово"
		java.util.regex.Matcher spaceMatcher = java.util.regex.Pattern.compile("[а-яА-ЯёЁ][ ][а-яА-ЯёЁ]").matcher(text);
		while (spaceMatcher.find()) {
			score += 2; // Стандартный приоритет для пробелов
		}

		// Штрафной паттерн: Неестественные стыки (например, пробел перед запятой или
		// два знака подряд)
		// Пример: " ," или ".,"
		java.util.regex.Matcher penaltyMatcher = java.util.regex.Pattern.compile("([ ][.,!?:])|([.,!?:]{2,})")
				.matcher(text);
		while (penaltyMatcher.find()) {
			score -= 3; // Снижаем рейтинг за аномалии
		}

		return score;
	}

	// --- МЕТОД: СТАТИСТИЧЕСКИЙ АНАЛИЗ (2-ГРАММЫ) ---

	/**
	 * Подбирает ключ на основе анализа частоты биграмм (2-грам) обучающего текста.
	 */
	public BruteforceResult statsAnalysis(String encryptedText, String sampleText) {
		logger.info("Запущен криптоанализ методом статистического анализа (2-граммы)");
		if (encryptedText == null || encryptedText.isEmpty() || sampleText == null || sampleText.isEmpty()) {
			return new BruteforceResult(0, "");
		}

		// 1. Считаем относительные частоты биграмм в эталонном тексте
		Map<String, Double> sampleBigrams = calculateBigramFrequencies(sampleText);
		logger.info("Расчет относительных частот биграмм в эталонном тексте завершен");

		int bestShift = 1;
		double minDistance = Double.MAX_VALUE; // Ищем минимальное различие частот
		String bestDecryptedText = "";
		int maxShift = getMaxShift();

		// 2. Перебираем сдвиги и ищем текст, чьи биграммы ближе всего к эталону
		for (int shift = 1; shift <= maxShift; shift++) {
			String decryptedVariant = decrypt(encryptedText, shift);
			Map<String, Double> variantBigrams = calculateBigramFrequencies(decryptedVariant);

			// Вычисляем расстояние между распределениями частот
			double distance = calculateFrequencyDistance(sampleBigrams, variantBigrams);
			logger.info("shift = " + shift+ " distance = " + distance);

			if (distance < minDistance) {
				minDistance = distance;
				bestShift = shift;
				bestDecryptedText = decryptedVariant;
			}
		}
		logger.info("Статистический анализ успешно завершен. Подобран оптимальный ключ: " + bestShift);
		return new BruteforceResult(bestShift, bestDecryptedText);
	}

	/**
	 * Считает относительную частоту появления каждой 2-граммы в тексте.
	 */
	private Map<String, Double> calculateBigramFrequencies(String text) {
		logger.info("Запущен метод расчета относительной частоты появления каждой 2-граммы в тексте.");
		Map<String, Double> frequencies = new HashMap<>();
		if (text.length() < 2)
			return frequencies;

		int totalBigrams = 0;
		for (int i = 0; i < text.length() - 1; i++) {
			char c1 = text.charAt(i);
			char c2 = text.charAt(i + 1);

			// Учитываем только символы из нашего алфавита
			if (ALPHABET.indexOf(c1) != -1 && ALPHABET.indexOf(c2) != -1) {
				String bigram = "" + c1 + c2;
				frequencies.put(bigram, frequencies.getOrDefault(bigram, 0.0) + 1.0);
				totalBigrams++;
			}
		}

		// Переводим абсолютное количество в относительную частоту (проценты)
		for (Map.Entry<String, Double> entry : frequencies.entrySet()) {
			entry.setValue(entry.getValue() / totalBigrams);
		}

		return frequencies;
	}

	/**
	 * Считает разность частот (критерий наименьших квадратов).
	 */
	private double calculateFrequencyDistance(Map<String, Double> sample, Map<String, Double> variant) {
		double distance = 0.0;

		// Объединяем уникальные биграммы из обоих списков для сравнения
		java.util.Set<String> allBigrams = new java.util.HashSet<>(sample.keySet());
		allBigrams.addAll(variant.keySet());

		for (String bigram : allBigrams) {
			double f1 = sample.getOrDefault(bigram, 0.0);
			double f2 = variant.getOrDefault(bigram, 0.0);
			distance += Math.pow(f1 - f2, 2); // Квадрат разности частот
		}

		return distance;
	}

	/**
	 * Вспомогательный POJO-класс для возврата нескольких значений из метода
	 * брутфорса.
	 */
	public static class BruteforceResult {
		private final int key;
		private final String decryptedText;

		public BruteforceResult(int key, String decryptedText) {
			this.key = key;
			this.decryptedText = decryptedText;
		}

		public int getKey() {
			return key;
		}

		public String getDecryptedText() {
			return decryptedText;
		}
	}

}

