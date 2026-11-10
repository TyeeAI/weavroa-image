package com.eniac.weavroa.llm.model.image.gen.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ImageGenResponse {

    private ImageGenOutput output;
    private ImageGenUsage usage;
    private String request_id;

    @Getter
    @Setter
    public static class ImageGenOutput {
        private String task_id;

        private String task_status;

        private String code;

        private String message;

        private List<ImageGenResults> results;
    }

    @Getter
    @Setter
    public static class ImageGenUsage {
        private Integer image_count;
    }

    @Getter
    @Setter
    public static class ImageGenResults {
        private String url;

        private String code;

        private String message;
    }

}
