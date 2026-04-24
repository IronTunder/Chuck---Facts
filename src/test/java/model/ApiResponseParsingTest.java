package model;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiResponseParsingTest {
    private final Gson gson = new Gson();

    @Test
    void parsesChuckNorrisResponse() {
        ChuckNorrisResponse response = gson.fromJson("{\"value\":\"Chuck test\"}", ChuckNorrisResponse.class);

        assertEquals("Chuck test", response.value());
    }

    @Test
    void parsesDadJokeResponse() {
        DadJokeResponse response = gson.fromJson("{\"joke\":\"Dad test\"}", DadJokeResponse.class);

        assertEquals("Dad test", response.joke());
    }

    @Test
    void parsesUselessFactResponse() {
        UselessFactResponse response = gson.fromJson("{\"text\":\"Fact test\"}", UselessFactResponse.class);

        assertEquals("Fact test", response.text());
    }
}
