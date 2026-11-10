package com.eniac.weavroa.llm.entity.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ImageGenIdResponse {

    @JsonProperty("request_id")
    private String requestId;

    private Output output;

    @Data
    public static class Output {

        @JsonProperty("task_status")
        private String taskStatus;

        @JsonProperty("task_id")
        private String taskId;
    }
}