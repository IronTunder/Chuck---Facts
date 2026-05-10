package service;

import model.DadJokeResponse;
import model.JokeResult;
import util.HttpUtil;

import java.util.Map;

public class DadJokeService {
    private static final String URL = "https://icanhazdadjoke.com/";

    private final HttpUtil httpUtil;

    public DadJokeService() {
        this(new HttpUtil());
    }

    DadJokeService(HttpUtil httpUtil) {
        this.httpUtil = httpUtil;
    }

    public JokeResult getRandomJoke() {
        
        DadJokeResponse response = httpUtil.getJson(URL, DadJokeResponse.class, Map.of(
                "Accept", "application/json",
                "User-Agent", "Chuck & Facts"
        ));
        if (response == null || response.joke() == null || response.joke().isBlank()) {
            throw new IllegalStateException("risposta Dad Joke non valida.");
        }
        return new JokeResult(response.joke(), "Dad joke", "icanhazdadjoke.com");
    }
}
