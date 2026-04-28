package service;

import model.DeepSeekChatResponse;
import org.junit.jupiter.api.Test;
import util.Config;
import util.HttpUtil;

import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeepSeekServiceTest {
    @Test
    void failsClearlyWhenApiKeyIsMissing() {
        DeepSeekService service = new DeepSeekService(new Config(new Properties()));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.translateToItalian("A test sentence.")
        );

        assertTrue(exception.getMessage().contains("API key DeepSeek mancante"));
    }

    @Test
    void sendsTranslationRequestToDeepSeek() {
        Properties properties = new Properties();
        properties.setProperty("DEEPSEEK_API_KEY", "test-key");
        DeepSeekService service = new DeepSeekService(new Config(properties), new FakeHttpUtil());

        String translation = service.translateToItalian("A test sentence.");

        assertEquals("Una frase di test.", translation);
    }

    private static class FakeHttpUtil extends HttpUtil {
        @Override
        public <T> T postJson(String url, Object body, Class<T> responseType, Map<String, String> headers) {
            assertEquals("https://api.deepseek.com/chat/completions", url);
            assertEquals("Bearer test-key", headers.get("Authorization"));
            assertTrue(body.toString().contains("deepseek-v4-flash"));
            assertTrue(body.toString().contains("A test sentence."));
            return responseType.cast(response());
        }

        private DeepSeekChatResponse response() {
            return new com.google.gson.Gson().fromJson(
                    "{\"choices\":[{\"message\":{\"content\":\"Una frase di test.\"}}]}",
                    DeepSeekChatResponse.class
            );
        }
    }
}
