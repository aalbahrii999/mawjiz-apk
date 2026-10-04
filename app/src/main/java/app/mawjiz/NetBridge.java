package app.mawjiz;

import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NetBridge {
    private final WebView web;
    private final OkHttpClient client = new OkHttpClient.Builder()
            .callTimeout(25, TimeUnit.SECONDS)
            .build();
    private final ExecutorService pool = Executors.newFixedThreadPool(6);

    public NetBridge(WebView web) {
        this.web = web;
    }

    @JavascriptInterface
    public void request(String id, String method, String url, String body, String contentType, String apiKey) {
        pool.execute(() -> {
            int status = 599;
            String text = "";
            try {
                if (url == null || !url.startsWith("https://")) {
                    throw new IllegalArgumentException("https only");
                }
                Request.Builder builder = new Request.Builder().url(url).header("user-agent", "Mawjiz/3.0");
                if (apiKey != null && !apiKey.isEmpty()) builder.header("x-goog-api-key", apiKey);
                if ("POST".equalsIgnoreCase(method)) {
                    String type = contentType == null || contentType.isEmpty() ? "application/json" : contentType;
                    builder.post(RequestBody.create(body == null ? "" : body, MediaType.parse(type)));
                }
                try (Response response = client.newCall(builder.build()).execute()) {
                    status = response.code();
                    text = response.body() == null ? "" : response.body().string();
                }
            } catch (Exception error) {
                text = error.getMessage() == null ? "" : error.getMessage();
            }
            String script = "window.__mawjizResult("
                    + JSONObject.quote(id)
                    + ","
                    + status
                    + ","
                    + JSONObject.quote(text)
                    + ")";
            web.post(() -> web.evaluateJavascript(script, null));
        });
    }
}
