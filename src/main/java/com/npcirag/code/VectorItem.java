package com.npcirag.code;




import com.github.jelmerk.knn.Item;

/**
 * This is REQUIRED by your HNSW version.
 * It wraps:
 * - id
 * - vector
 */
public class VectorItem implements Item<String, float[]> {

    private final String id;
    private final float[] vector;

    public VectorItem(String id, float[] vector) {
        this.id = id;
        this.vector = vector;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public float[] vector() {
        return vector;
    }

    @Override
    public int dimensions() {
        return vector.length;
    }
}