package service;

import org.junit.jupiter.api.Test;
import util.Config;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeepLServiceTest {
    @Test
    void failsClearlyWhenApiKeyIsMissing() {
        DeepLService service = new DeepLService(new Config(new Properties()));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.translateToItalian("A test sentence.")
        );

        assertTrue(exception.getMessage().contains("API key DeepL mancante"));
    }
}
