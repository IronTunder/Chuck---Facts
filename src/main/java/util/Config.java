package util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

public class Config {
    private final Properties properties;

    public Config(Properties properties) {
        this.properties = properties;
    }

    public static Config load() {
        Properties properties = new Properties();
        Path configPath = Path.of("config.properties");
        if (Files.exists(configPath)) {
            try (InputStream inputStream = Files.newInputStream(configPath)) {
                properties.load(inputStream);
            } catch (IOException ex) {
                throw new IllegalStateException("impossibile leggere config.properties.", ex);
            }
        }
        return new Config(properties);
    }

    public Optional<String> get(String key) {
        String envValue = System.getenv(key);
        if (envValue != null && !envValue.isBlank()) {
            return Optional.of(envValue);
        }
        String propertyValue = properties.getProperty(key);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return Optional.of(propertyValue);
        }
        return Optional.empty();
    }
}
