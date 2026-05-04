package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record WhoaResponse(
        String movie,
        Integer year,
        String release_date,
        String director,
        String character,
        String timestamp,
        String full_line,
        String poster,
        Map<String, String> video,
        String audio
) {
    public String preferredVideoUrl() {
        List<String> urls = playableMediaUrls();
        return urls.isEmpty() ? null : urls.get(0);
    }

    public List<String> playableMediaUrls() {
        List<String> urls = new ArrayList<>();
        if (video == null || video.isEmpty()) {
            addIfPresent(urls, audio);
            return urls;
        }
        urls.addAll(playableVideoUrls());
        addIfPresent(urls, audio);
        return urls;
    }

    public List<String> playableVideoUrls() {
        List<String> urls = new ArrayList<>();
        if (video == null || video.isEmpty()) {
            return urls;
        }
        for (String quality : new String[]{"720p", "1080p", "480p", "360p"}) {
            addIfPresent(urls, video.get(quality));
        }
        video.values().forEach(url -> addIfPresent(urls, url));
        return urls;
    }

    private void addIfPresent(List<String> urls, String url) {
        if (url != null && !url.isBlank() && !urls.contains(url)) {
            urls.add(url);
        }
    }

    public boolean isAudioUrl(String url) {
        if (url == null || audio == null) {
            return false;
        }
        return audio.equals(url);
    }

    public boolean hasPoster() {
        return poster != null && !poster.isBlank();
    }

    public String displayTitle() {
        if (year == null) {
            return movie;
        }
        return movie + " (" + year + ")";
    }

    public boolean hasVideoUrl() {
        if (video == null || video.isEmpty()) {
            return false;
        }
        return video.values().stream().anyMatch(url -> url != null && !url.isBlank());
    }

    public boolean hasPlayableMedia() {
        return !playableMediaUrls().isEmpty();
    }

    public String mediaFallbackMessage(String url) {
        if (isAudioUrl(url)) {
            return "Video non supportato: riproduco l'audio della scena.";
        }
        return "Caricamento video...";
    }

    public String unsupportedMediaMessage() {
        if (hasPlayableMedia()) {
            return "Questo spezzone non e' riproducibile da JavaFX.";
        }
        return "La risposta Whoa non contiene media riproducibili.";
    }

    public boolean hasAudioUrl() {
        return audio != null && !audio.isBlank();
    }

    public String bestStillImage() {
        if (hasPoster()) {
            return poster;
        }
        return null;
    }

    public boolean hasOnlyAudio() {
        return !hasVideoUrl() && hasAudioUrl();
    }

    public String firstMediaUrl() {
        List<String> urls = playableMediaUrls();
        return urls.isEmpty() ? null : urls.get(0);
    }

    public List<String> fallbackMediaUrlsAfter(String failedUrl) {
        List<String> urls = playableMediaUrls();
        int failedIndex = urls.indexOf(failedUrl);
        if (failedIndex < 0 || failedIndex + 1 >= urls.size()) {
            return List.of();
        }
        return urls.subList(failedIndex + 1, urls.size());
    }

    public boolean isVideoUrl(String url) {
        return url != null && video != null && video.containsValue(url);
    }

    public String playbackKind(String url) {
        return isAudioUrl(url) ? "audio" : "video";
    }

    public boolean canFallbackAfter(String url) {
        return !fallbackMediaUrlsAfter(url).isEmpty();
    }

    public String firstFallbackAfter(String url) {
        List<String> fallbacks = fallbackMediaUrlsAfter(url);
        return fallbacks.isEmpty() ? null : fallbacks.get(0);
    }

    public String firstPlayableVideoUrl() {
        if (video == null || video.isEmpty()) {
            return null;
        }
        for (String quality : new String[]{"720p", "1080p", "480p", "360p"}) {
            String url = video.get(quality);
            if (url != null && !url.isBlank()) {
                return url;
            }
        }
        return video.values().stream()
                .filter(url -> url != null && !url.isBlank())
                .findFirst()
                .orElse(null);
    }
}
