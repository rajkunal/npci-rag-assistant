package com.npcirag.code;

import okhttp3.*;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

/**
 * Calls local LLM (Mistral via Ollama)
 */
public class OllamaClient {

    private static final String URL = "http://localhost:11434/api/generate";

    public static String generate(String prompt) throws Exception {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build();

        JSONObject json = new JSONObject();
        json.put("model", "mistral");
        json.put("prompt", prompt);
        json.put("stream", false);

        RequestBody body = RequestBody.create(
                json.toString(),
                MediaType.get("application/json")
        );

        Request request = new Request.Builder()
                .url(URL)
                .post(body)
                .build();

        Response response = client.newCall(request).execute();
        String responseBody = response.body().string();

        JSONObject res = new JSONObject(responseBody);

        if (res.has("response")) {
            return res.getString("response");
        } else {
            System.out.println("Full response: " + responseBody);
            throw new RuntimeException("Invalid response from Ollama");
        }

      //  return new JSONObject(response.body().string()).getString("response");
    }
}
