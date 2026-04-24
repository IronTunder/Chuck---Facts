package service;

import model.TranslationResponse;
import util.Config;
import util.HttpUtil;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class DeepLService {
    private static final String URL = "https://api-free.deepl.com/v2/translate";

    private final Config config;
    private final HttpUtil httpUtil;

    public DeepLService(Config config) {
        this(config, new HttpUtil());
    }

    DeepLService(Config config, HttpUtil httpUtil) {
        this.config = config;
        this.httpUtil = httpUtil;
    }

    public String translateToItalian(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalStateException("nessun testo da tradurre.");
        }
        String apiKey = config.get("DEEPL_API_KEY")
                .filter(value -> !value.isBlank())
                .orElseThrow(() -> new IllegalStateException("API key DeepL mancante. Crea config.properties o imposta DEEPL_API_KEY."));

        String body = formField("text", text)
                + "&" + formField("target_lang", "IT")
                + "&" + formField("source_lang", "EN");

        TranslationResponse response = httpUtil.postForm(URL, body, TranslationResponse.class, Map.of(
                "Authorization", "DeepL-Auth-Key " + apiKey
        ));

        if (response == null || response.translations() == null || response.translations().isEmpty()) {
            throw new IllegalStateException("risposta DeepL non valida.");
        }
        return response.translations().stream()
                .map(TranslationResponse.TranslationItem::text)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("traduzione DeepL vuota."));
    }

    private String formField(String key, String value) {
        return URLEncoder.encode(key, StandardCharsets.UTF_8) + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
