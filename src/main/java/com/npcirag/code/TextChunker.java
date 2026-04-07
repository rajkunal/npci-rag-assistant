package com.npcirag.code;


import java.util.*;

/**
 * Splits large documents into smaller chunks.
 * This improves retrieval accuracy.
 */
public class TextChunker {

    public static List<String> chunk(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();

        for (int i = 0; i < text.length(); i += chunkSize) {
            chunks.add(text.substring(i, Math.min(text.length(), i + chunkSize)));
        }

        return chunks;
    }
}
