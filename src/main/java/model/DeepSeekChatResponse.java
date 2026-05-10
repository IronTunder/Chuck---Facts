package model;

import java.util.List;

public class DeepSeekChatResponse {
    
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
