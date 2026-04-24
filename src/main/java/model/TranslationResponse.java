package model;

import java.util.List;

public class TranslationResponse {
    private List<TranslationItem> translations;

    public List<TranslationItem> translations() {
        return translations;
    }

    public static class TranslationItem {
        private String text;

        public String text() {
            return text;
        }
    }
}
