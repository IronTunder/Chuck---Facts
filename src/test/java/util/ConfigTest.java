package util;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigTest {
    @Test
    void returnsEmptyWhenKeyIsMissing() {
        Config config = new Config(new Properties());

        assertTrue(config.get("MISSING_KEY_FOR_TEST").isEmpty());
    }

    @Test
    void readsPropertyValue() {
        Properties properties = new Properties();
        properties.setProperty("DEEPSEEK_API_KEY", "test-key");

        Config config = new Config(properties);

        assertEquals("test-key", config.get("DEEPSEEK_API_KEY").orElseThrow());
    }
}
