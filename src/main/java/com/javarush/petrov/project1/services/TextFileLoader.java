package com.javarush.petrov.project1.services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class TextFileLoader implements FileLoader {

	@Override
	public String loadContent(File file) throws IOException {
		return Files.readString(file.toPath());
	}
}
