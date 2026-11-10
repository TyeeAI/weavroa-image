package com.eniac.weavroa.llm.entity.request;

import lombok.Data;
import java.util.List;

@Data
public class WanImageRequest {

    private String model;
    private Input input;
    private Parameters parameters;

    @Data
    public static class Input {
        private List<Message> messages;
    }

    @Data
    public static class Message {
        private String role; // 固定 user
        private List<Content> content;
    }

    @Data
    public static class Content {

        /**
         * 类型：
         * text / image
         */
        private String type;

        /**
         * 文本内容
         */
        private String text;

        /**
         * 图片 URL 或 base64
         */
        private String image;
    }

    @Data
    public static class Parameters {

        private List<List<List<Integer>>> bbox_list;

        private Boolean enable_sequential;

        private String size;

        private Integer n;

        private Boolean thinking_mode;

        private List<Color> color_palette;

        private Boolean watermark;

        private Integer seed;
    }

    @Data
    public static class Color {
        private String hex;
        private String ratio;
    }
}