package service;

import model.JokeResult;
import model.UselessFactResponse;
import util.HttpUtil;

public class UselessFactService {
    private static final String URL = "https://uselessfacts.jsph.pl/api/v2/facts/random?language=en";

    private final HttpUtil httpUtil;

    public UselessFactService() {
        this(new HttpUtil());
    }

    UselessFactService(HttpUtil httpUtil) {
        this.httpUtil = httpUtil;
    }

    public JokeResult getRandomFact() {
        // L'endpoint e' fissato in inglese: la traduzione avviene dopo, nel flusso GUI.
        UselessFactResponse response = httpUtil.getJson(URL, UselessFactResponse.class);
        if (response == null || response.text() == null || response.text().isBlank()) {
            throw new IllegalStateException("risposta Useless Facts non valida.");
        }
        return new JokeResult(response.text(), "Fatto inutile", "uselessfacts.jsph.pl");
    }
}
