package service;

import model.DeepSeekChatResponse;
import util.Config;
import util.HttpUtil;

import java.util.List;
import java.util.Map;

public class DeepSeekService {
    private static final String URL = "https://api.deepseek.com/chat/completions";
    private static final String DEFAULT_MODEL = "deepseek-v4-flash";

    private final Config config;
    private final HttpUtil httpUtil;

    public DeepSeekService(Config config) {
        this(config, new HttpUtil());
    }

    DeepSeekService(Config config, HttpUtil httpUtil) {
        this.config = config;
        this.httpUtil = httpUtil;
    }

    public String translateToItalian(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalStateException("nessun testo da tradurre.");
        }

        // La chiave puo' arrivare da variabile d'ambiente o config.properties.
        String apiKey = config.get("DEEPSEEK_API_KEY")
                .orElseThrow(() -> new IllegalStateException("API key DeepSeek mancante. Crea config.properties o imposta DEEPSEEK_API_KEY."));
        // Il modello resta configurabile per passare rapidamente a versioni piu' veloci o economiche.
        String model = config.get("DEEPSEEK_MODEL").orElse(DEFAULT_MODEL);

        DeepSeekChatResponse response = httpUtil.postJson(URL, translationRequest(text, model), DeepSeekChatResponse.class, Map.of(
                "Authorization", "Bearer " + apiKey
        ));

        // DeepSeek segue il formato OpenAI-compatible: la traduzione e' nel primo content valido.
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new IllegalStateException("risposta DeepSeek non valida.");
        }
        return response.choices().stream()
                .map(DeepSeekChatResponse.Choice::message)
                .filter(message -> message != null && message.content() != null && !message.content().isBlank())
                .map(message -> message.content().trim())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("traduzione DeepSeek vuota."));
    }

    private Map<String, Object> translationRequest(String text, String model) {
        // Prompt molto vincolato: riduce token, latenza e rischio di spiegazioni extra.
        return Map.of(
                "model", model,
                "temperature", 0.1,
                "stream", false,
                "messages", List.of(
                        Map.of(
                                "role", "system",
                                "content", "Traduci in italiano il testo dell'utente. Rispondi solo con la traduzione, senza note o virgolette."
                        ),
                        Map.of(
                                "role", "user",
                                "content", text
                        )
                )
        );
    }
}
