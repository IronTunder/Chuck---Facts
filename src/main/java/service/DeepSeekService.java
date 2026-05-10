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

        return chatCompletion(
                "Traduci in italiano il testo dell'utente. Rispondi solo con la traduzione, senza note o virgolette.",
                text,
                0.1
        );
    }

    public String makePlausibleFalseFact(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalStateException("nessun fatto da modificare.");
        }

        return chatCompletion(
                "Riceverai un fatto vero in inglese. Crea una singola frase in italiano che sembri plausibile ma sia falsa, modificando uno o due dettagli concreti. Non rendere la frase assurda. Rispondi solo con la frase falsa, senza spiegazioni o virgolette.",
                text,
                0.8
        );
    }

    private String chatCompletion(String systemPrompt, String userText, double temperature) {
        
        String apiKey = config.get("DEEPSEEK_API_KEY")
                .orElseThrow(() -> new IllegalStateException("API key DeepSeek mancante. Crea config.properties o imposta DEEPSEEK_API_KEY."));
        
        String model = config.get("DEEPSEEK_MODEL").orElse(DEFAULT_MODEL);

        DeepSeekChatResponse response = httpUtil.postJson(URL, chatRequest(systemPrompt, userText, model, temperature), DeepSeekChatResponse.class, Map.of(
                "Authorization", "Bearer " + apiKey
        ));

        
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

    private Map<String, Object> chatRequest(String systemPrompt, String text, String model, double temperature) {
        
        return Map.of(
                "model", model,
                "temperature", temperature,
                "stream", false,
                "messages", List.of(
                        Map.of(
                                "role", "system",
                                "content", systemPrompt
                        ),
                        Map.of(
                                "role", "user",
                                "content", text
                        )
                )
        );
    }
}
