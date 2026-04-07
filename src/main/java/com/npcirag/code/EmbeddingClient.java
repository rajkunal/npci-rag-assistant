package com.npcirag.code;


import okhttp3.*;
import org.json.JSONObject;

/**
 * Calls Ollama embedding API to convert text → vector
 */
public class EmbeddingClient {

    private static final String URL = "http://localhost:11434/api/embeddings";

    public static float[] embed(String text) throws Exception {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS) // 🔥 important
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();

        JSONObject json = new JSONObject();
        json.put("model", "nomic-embed-text"); // You can switch to embedding model later
        json.put("prompt", text);

        RequestBody body = RequestBody.create(
                json.toString(),
                MediaType.get("application/json")
        );

        Request request = new Request.Builder()
                .url(URL)
                .post(body)
                .build();

        Response response = client.newCall(request).execute();

        JSONObject res = new JSONObject(response.body().string());

        // Convert JSON array → float[]
        var arr = res.getJSONArray("embedding");
        float[] vector = new float[arr.length()];

        for (int i = 0; i < arr.length(); i++) {
            vector[i] = arr.getFloat(i);
        }

        return vector;
    }
}