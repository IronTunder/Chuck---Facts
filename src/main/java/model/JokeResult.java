package model;

public record JokeResult(
        String originalText,
        String contentType,
        String source
) {
    public boolean supportsTranslation() {
        
        String normalizedSource = source == null ? "" : source.toLowerCase();
        return normalizedSource.contains("chucknorris") || normalizedSource.contains("uselessfacts");
    }
}
