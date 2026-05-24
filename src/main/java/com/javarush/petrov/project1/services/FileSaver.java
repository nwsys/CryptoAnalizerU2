package com.javarush.petrov.project1.services;

import java.io.File;
import java.io.IOException;

public interface FileSaver {
	void saveContent(File file, String content) throws IOException;
}
