package model;

public record HistoryEntry(
        // Valori gia' pronti per la tabella: la GUI non deve riformattarli ogni volta.
        String dateTime,
        String source,
        String originalText,
        String translation
) {
}
