package com.javarush.petrov.project1.services;

import java.io.File;
import java.io.IOException;

public interface FileLoader {
	String loadContent(File file) throws IOException;
}
