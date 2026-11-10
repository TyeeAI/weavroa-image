package com.eniac.weavroa.llm.entity.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ImageTranslateResponse {

    private String requestId;

    private Output output;

    private Usage usage;

    @Data
    public static class Output {

        @JsonProperty("task_id")
        private String taskId;

        @JsonProperty("task_status")
        private String taskStatus;

        @JsonProperty("submit_time")
        private String submitTime;

        @JsonProperty("scheduled_time")
        private String scheduledTime;

        @JsonProperty("end_time")
        private String endTime;

        @JsonProperty("image_url")
        private String imageUrl;

        private String code;
        private String message;
    }

    @Data
    public static class Usage {
        private Integer imageCount;
    }
}