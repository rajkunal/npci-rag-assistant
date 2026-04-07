package com.npcirag.code;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {

    public static List<String> loadDocs(String folderPath) {
        List<String> docs = new ArrayList<>();

        File folder = new File(folderPath);

        for (File file : Objects.requireNonNull(folder.listFiles())) {

            try {
                if (file.getName().endsWith(".txt")) {
                    docs.add(java.nio.file.Files.readString(file.toPath()));
                }

                else if (file.getName().endsWith(".pdf")) {
                    PDDocument document = PDDocument.load(file);
                    PDFTextStripper stripper = new PDFTextStripper();
                    docs.add(stripper.getText(document));
                    document.close();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return docs;
    }
    public static void main(String[] args) throws Exception {

        float[] vectorr = EmbeddingClient.embed("test");
        System.out.println("Embedding dimension: " + vectorr.length);

        // Load documents
        List<String> docs = loadDocs("docs/");

        // Initialize vector DB (dimension = embedding size, assume 768)
        VectorStore store = new VectorStore(768);

        int id = 0;

        // Index documents
        for (String doc : docs) {
            List<String> chunks = TextChunker.chunk(doc, 500);

            for (String chunk : chunks) {
                float[] vector = EmbeddingClient.embed(chunk);
                store.add("chunk-" + id++, vector, chunk);
            }
        }

        // Save index to disk
        store.save("vector.index","text.store");

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("Ask: ");
            String query = scanner.nextLine();

            // Convert query → embedding
            float[] queryVector = EmbeddingClient.embed(query);

            // Retrieve top chunks
            List<String> topChunks = store.search(queryVector, 3);

            String context = String.join("\n", topChunks);

            String prompt = "Answer only from context:\n" +
                    context +
                    "\n\nQuestion: " + query;

            String answer = OllamaClient.generate(prompt);

            System.out.println("\nAnswer:\n" + answer);
        }
    }
}