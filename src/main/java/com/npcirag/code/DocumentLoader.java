package com.npcirag.code;



import java.nio.file.*;
import java.io.IOException;
import java.util.*;

/**
 * Loads all text files from a folder.
 * You can later extend this for PDFs.
 */
public class DocumentLoader {

    public static List<String> loadDocs(String folderPath) throws IOException {
        List<String> docs = new ArrayList<>();

        Files.walk(Paths.get(folderPath))
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    try {
                        docs.add(Files.readString(path));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

        return docs;
    }
}