package com.npcirag.code;



import com.github.jelmerk.knn.hnsw.HnswIndex;
import com.github.jelmerk.knn.DistanceFunctions;

import java.io.*;
import java.util.*;

public class VectorStore {

    private HnswIndex<String, float[], VectorItem, Float> index;

    // Store actual text separately
    private Map<String, String> textStore = new HashMap<>();

    public VectorStore(int dimension) {

        this.index = HnswIndex
                .newBuilder(
                        dimension,
                        DistanceFunctions.FLOAT_COSINE_DISTANCE, // ✅ correct
                        10000
                )
                .withM(16)
                .withEf(100)
                .build();
    }

    /**
     * Add embedding + text
     */
    public void add(String id, float[] vector, String text) {
        index.add(new VectorItem(id, vector)); // ✅ REQUIRED
        textStore.put(id, text);
    }

    /**
     * Search similar chunks
     */
    public List<String> search(float[] queryVector, int k) {

        var results = index.findNearest(queryVector, k);

        List<String> texts = new ArrayList<>();

        for (var result : results) {
            String id = result.item().id(); // ✅ important fix
            texts.add(textStore.get(id));
        }

        return texts;
    }

    /**
     * Save index + text
     */
    public void save(String indexPath, String textPath) throws IOException {

        index.save(new File(indexPath));

        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(textPath))) {
            oos.writeObject(textStore);
        }
    }

    /**
     * Load index + text
     */
    @SuppressWarnings("unchecked")
    public void load(String indexPath, String textPath) throws IOException, ClassNotFoundException {

        index = HnswIndex.load(new File(indexPath));

        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(textPath))) {
            textStore = (Map<String, String>) ois.readObject();
        }
    }
}