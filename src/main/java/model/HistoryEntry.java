package model;

public record HistoryEntry(
        String dateTime,
        String source,
        String originalText,
        String translation
) {
}
