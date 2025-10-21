package com.mycompany.bidverse;

import java.net.*;
import java.io.*;

public class ApiClient {
    public static final String BASE_URL = "http://localhost:8080";

    /**
     * POST JSON to endpoint (endpoint must start with /).
     * Returns response body as String if response code is 2xx, otherwise returns
     * null.
     */
    public static String postJson(String endpoint, String jsonBody) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(BASE_URL + endpoint);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(8000);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] bytes = jsonBody.getBytes("UTF-8");
                os.write(bytes);
                os.flush();
            }

            int code = conn.getResponseCode();
            InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
            if (is == null)
                return null;

            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null)
                    sb.append(line);
                return (code >= 200 && code < 300) ? sb.toString() : null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (conn != null)
                conn.disconnect();
        }
    }
}
