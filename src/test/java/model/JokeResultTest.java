package model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JokeResultTest {
    @Test
    void chuckNorrisContentSupportsTranslation() {
        JokeResult result = new JokeResult("Chuck test", "Battuta Chuck Norris", "ChuckNorris.io");

        assertTrue(result.supportsTranslation());
    }

    @Test
    void uselessFactContentSupportsTranslation() {
        JokeResult result = new JokeResult("Fact test", "Fatto inutile", "uselessfacts.jsph.pl");

        assertTrue(result.supportsTranslation());
    }

    @Test
    void dadJokeContentDoesNotSupportTranslation() {
        JokeResult result = new JokeResult("Dad test", "Dad joke", "icanhazdadjoke.com");

        assertFalse(result.supportsTranslation());
    }
}
