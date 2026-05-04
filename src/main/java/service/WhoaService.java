package service;

import com.google.gson.reflect.TypeToken;
import model.WhoaResponse;
import util.HttpUtil;

import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.List;

public class WhoaService {
    private static final String RANDOM_URL = "https://whoa.onrender.com/whoas/random";
    private static final String MOVIES_URL = "https://whoa.onrender.com/whoas/movies";
    private static final Type WHOA_LIST_TYPE = new TypeToken<List<WhoaResponse>>() {
    }.getType();
    private static final Type MOVIE_LIST_TYPE = new TypeToken<List<String>>() {
    }.getType();

    private final HttpUtil httpUtil;

    public WhoaService() {
        this(new HttpUtil());
    }

    WhoaService(HttpUtil httpUtil) {
        this.httpUtil = httpUtil;
    }

    public WhoaResponse getRandomWhoa() {
        List<WhoaResponse> responses = httpUtil.getJson(RANDOM_URL, WHOA_LIST_TYPE);
        if (responses == null || responses.isEmpty()) {
            throw new IllegalStateException("risposta Whoa non valida.");
        }
        WhoaResponse response = responses.get(0);
        validateWhoa(response);
        return response;
    }

    public List<String> getMovies() {
        List<String> movies = httpUtil.getJson(MOVIES_URL, MOVIE_LIST_TYPE);
        if (movies == null) {
            return List.of();
        }
        return new LinkedHashSet<>(movies).stream()
                .filter(movie -> movie != null && !movie.isBlank())
                .toList();
    }

    private void validateWhoa(WhoaResponse response) {
        if (response == null || response.movie() == null || response.movie().isBlank()) {
            throw new IllegalStateException("risposta Whoa senza film.");
        }
        if (!response.hasPlayableMedia()) {
            throw new IllegalStateException("risposta Whoa senza media riproducibili.");
        }
    }
}
