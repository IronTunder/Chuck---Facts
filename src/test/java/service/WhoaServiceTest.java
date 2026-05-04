package service;

import model.WhoaResponse;
import org.junit.jupiter.api.Test;
import util.HttpUtil;

import java.lang.reflect.Type;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WhoaServiceTest {
    @Test
    void fetchesRandomWhoa() {
        WhoaService service = new WhoaService(new FakeHttpUtil());

        WhoaResponse response = service.getRandomWhoa();

        assertEquals("The Matrix", response.movie());
        assertEquals("https://example.test/matrix.mp4", response.preferredVideoUrl());
    }

    @Test
    void fetchesMovies() {
        WhoaService service = new WhoaService(new FakeHttpUtil());

        List<String> movies = service.getMovies();

        assertEquals(List.of("The Matrix", "Speed"), movies);
    }

    @Test
    void failsWhenRandomWhoaIsEmpty() {
        WhoaService service = new WhoaService(new EmptyRandomHttpUtil());

        IllegalStateException exception = assertThrows(IllegalStateException.class, service::getRandomWhoa);

        assertTrue(exception.getMessage().contains("risposta Whoa non valida"));
    }

    @Test
    void failsWhenRandomWhoaHasNoVideo() {
        WhoaService service = new WhoaService(new NoVideoHttpUtil());

        IllegalStateException exception = assertThrows(IllegalStateException.class, service::getRandomWhoa);

        assertTrue(exception.getMessage().contains("senza media"));
    }

    private static class FakeHttpUtil extends HttpUtil {
        @Override
        public <T> T getJson(String url, Type responseType) {
            if (url.endsWith("/whoas/random")) {
                assertEquals("https://whoa.onrender.com/whoas/random", url);
                return castJson("[{\"movie\":\"The Matrix\",\"year\":1999,\"video\":{\"480p\":\"https://example.test/matrix.mp4\"}}]", responseType);
            }
            if (url.endsWith("/whoas/movies")) {
                assertEquals("https://whoa.onrender.com/whoas/movies", url);
                return castJson("[\"The Matrix\",\"Speed\",\"The Matrix\"]", responseType);
            }
            throw new AssertionError("Unexpected URL: " + url);
        }

        protected <T> T castJson(String json, Type responseType) {
            return new com.google.gson.Gson().fromJson(json, responseType);
        }
    }

    private static class EmptyRandomHttpUtil extends FakeHttpUtil {
        @Override
        public <T> T getJson(String url, Type responseType) {
            return castJson("[]", responseType);
        }
    }

    private static class NoVideoHttpUtil extends FakeHttpUtil {
        @Override
        public <T> T getJson(String url, Type responseType) {
            return castJson("[{\"movie\":\"The Matrix\",\"year\":1999,\"video\":{}}]", responseType);
        }
    }
}
