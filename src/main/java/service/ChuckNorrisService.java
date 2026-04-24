package service;

import com.google.gson.reflect.TypeToken;
import model.ChuckNorrisResponse;
import model.JokeResult;
import util.HttpUtil;

import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ChuckNorrisService {
    private static final String RANDOM_URL = "https://api.chucknorris.io/jokes/random";
    private static final String CATEGORIES_URL = "https://api.chucknorris.io/jokes/categories";

    private final HttpUtil httpUtil;

    public ChuckNorrisService() {
        this(new HttpUtil());
    }

    ChuckNorrisService(HttpUtil httpUtil) {
        this.httpUtil = httpUtil;
    }

    public JokeResult getRandomJoke() {
        ChuckNorrisResponse response = httpUtil.getJson(RANDOM_URL, ChuckNorrisResponse.class);
        validateText(response == null ? null : response.value());
        return new JokeResult(response.value(), "Battuta Chuck Norris", "ChuckNorris.io");
    }

    public JokeResult getRandomJokeByCategory(String category) {
        if (category == null || category.isBlank()) {
            throw new IllegalStateException("seleziona una categoria Chuck Norris.");
        }
        String encoded = URLEncoder.encode(category, StandardCharsets.UTF_8);
        ChuckNorrisResponse response = httpUtil.getJson(RANDOM_URL + "?category=" + encoded, ChuckNorrisResponse.class);
        validateText(response == null ? null : response.value());
        return new JokeResult(response.value(), "Battuta Chuck Norris categoria " + category, "ChuckNorris.io");
    }

    public List<String> getCategories() {
        Type listType = new TypeToken<List<String>>() {
        }.getType();
        List<String> categories = httpUtil.getJson(CATEGORIES_URL, listType);
        if (categories == null) {
            return List.of();
        }
        return categories;
    }

    private void validateText(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalStateException("risposta Chuck Norris non valida.");
        }
    }
}
