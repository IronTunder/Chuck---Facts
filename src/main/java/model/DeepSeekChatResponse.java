package model;

import java.util.List;

public class DeepSeekChatResponse {
    // Mappatura minima della risposta DeepSeek: ci serve solo choices[].message.content.
    private List<Choice> choices;

    public List<Choice> choices() {
        return choices;
    }

    public static class Choice {
        private Message message;

        public Message message() {
            return message;
        }
    }

    public static class Message {
        private String content;

        public String content() {
            return content;
        }
    }
}
