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

    @Test
    void parsesWhoaResponse() {
        WhoaResponse response = gson.fromJson(
                "{\"movie\":\"The Matrix\",\"year\":1999,\"video\":{\"480p\":\"https://example.test/480.mp4\",\"720p\":\"https://example.test/720.mp4\"},\"audio\":\"https://example.test/audio.mp3\"}",
                WhoaResponse.class
        );

        assertEquals("The Matrix", response.movie());
        assertEquals(1999, response.year());
        assertEquals("https://example.test/480.mp4", response.video().get("480p"));
        assertEquals("https://example.test/720.mp4", response.preferredVideoUrl());
        assertEquals("https://example.test/audio.mp3", response.playableMediaUrls().get(2));
    }
}
