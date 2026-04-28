package model;

public record JokeResult(
        String originalText,
        String contentType,
        String source
) {
    public boolean supportsTranslation() {
        // Le Dad Joke vengono mostrate in originale per preservare il gioco di parole.
        String normalizedSource = source == null ? "" : source.toLowerCase();
        return normalizedSource.contains("chucknorris") || normalizedSource.contains("uselessfacts");
    }
}
