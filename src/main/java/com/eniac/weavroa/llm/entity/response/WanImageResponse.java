package com.eniac.weavroa.llm.entity.response;

import lombok.Data;
import java.util.List;

@Data
public class WanImageResponse {

    private Output output;
    private Usage usage;
    private String request_id;

    @Data
    public static class Output {
        private List<Choice> choices;
        private boolean finished;
    }

    @Data
    public static class Choice {
        private String finish_reason;
        private Message message;
    }

    @Data
    public static class Message {
        private List<Content> content;
        private String role;
    }

    @Data
    public static class Content {
        private String image;
        private String type;
    }

    @Data
    public static class Usage {
        private int image_count;
        private int input_tokens;
        private int output_tokens;
        private String size;
        private int total_tokens;
    }
}