package com.javarush.petrov.project1.services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class TextFileSaver implements FileSaver {
	@Override
	public void saveContent(File file, String content) throws IOException {
		Files.writeString(file.toPath(), content);		
	}
}
