package util;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public class HttpUtil {
    private final HttpClient client;
    private final Gson gson;

    public HttpUtil() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(), new Gson());
    }

    HttpUtil(HttpClient client, Gson gson) {
        this.client = client;
        this.gson = gson;
    }

    public <T> T getJson(String url, Class<T> responseType) {
        return getJson(url, responseType, Map.of());
    }

    public <T> T getJson(String url, Type responseType) {
        return getJson(url, responseType, Map.of());
    }

    public <T> T getJson(String url, Class<T> responseType, Map<String, String> headers) {
        return getJson(url, (Type) responseType, headers);
    }

    public <T> T getJson(String url, Type responseType, Map<String, String> headers) {
        // Costruzione comune per tutti gli endpoint GET che restituiscono JSON.
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .GET();
        headers.forEach(builder::header);
        return send(builder.build(), responseType);
    }

    public <T> T postForm(String url, String body, Class<T> responseType, Map<String, String> headers) {
        // Supporto tenuto per API che richiedono form-urlencoded.
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        headers.forEach(builder::header);
        return send(builder.build(), responseType);
    }

    public <T> T postJson(String url, Object body, Class<T> responseType, Map<String, String> headers) {
        // Usato dai servizi AI: serializza il body Java in JSON prima dell'invio.
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)));
        headers.forEach(builder::header);
        return send(builder.build(), responseType);
    }

    private <T> T send(HttpRequest request, Type responseType) {
        try {
            // Punto unico di invio/parsing: mantiene coerenti timeout, errori e messaggi UI.
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            ensureSuccess(response.statusCode(), response.body());
            return gson.fromJson(response.body(), responseType);
        } catch (JsonSyntaxException ex) {
            throw new IllegalStateException("JSON non valido ricevuto dal servizio.", ex);
        } catch (IOException ex) {
            throw new IllegalStateException("impossibile contattare il servizio.", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("operazione interrotta.", ex);
        }
    }

    private void ensureSuccess(int statusCode, String responseBody) {
        if (statusCode >= 200 && statusCode < 300) {
            return;
        }
        // Gli errori piu' comuni diventano messaggi comprensibili per l'utente.
        if (statusCode == 401 || statusCode == 403) {
            throw new IllegalStateException("API key non valida o non autorizzata.");
        }
        if (statusCode == 429) {
            throw new IllegalStateException("troppe richieste, riprova piu tardi.");
        }
        String detail = responseBody == null || responseBody.isBlank() ? "" : " Dettaglio: " + abbreviate(responseBody);
        throw new IllegalStateException("risposta HTTP " + statusCode + " dal servizio." + detail);
    }

    private String abbreviate(String text) {
        // Evita popup enormi quando un servizio restituisce un body di errore molto lungo.
        String singleLine = text.replaceAll("\\s+", " ").trim();
        return singleLine.length() <= 160 ? singleLine : singleLine.substring(0, 157) + "...";
    }
}
