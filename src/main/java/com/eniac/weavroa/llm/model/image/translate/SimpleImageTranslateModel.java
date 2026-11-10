package com.eniac.weavroa.llm.model.image.translate;

import com.eniac.weavroa.llm.entity.response.ImageGenIdResponse;
import com.eniac.weavroa.llm.entity.response.ImageTranslateResponse;
import com.eniac.weavroa.llm.model.AbstractModel;
import com.eniac.weavroa.llm.model.image.gen.request.ImageGenRequest;
import com.eniac.weavroa.llm.model.image.gen.response.ImageGenResponse;
import lombok.experimental.SuperBuilder;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@SuperBuilder
public class SimpleImageTranslateModel extends AbstractModel<Object, Object> implements ImageTranslateModel{
    @Override
    public Mono<String> getImageTranslateId(String sourceLange,
                                            String targetLanguage,
                                            String imageUrl) {

        Map<String, Object> requestBody = Map.of(

                "model", this.getName(),

                "input", Map.of(
                        "image_url", imageUrl,
                        "source_lang", sourceLange,
                        "target_lang", targetLanguage,

                        "ext", Map.of(
                                "config", Map.of(
                                        "imageSegment", false
                                )
                        )
                )
        );

        return WebClient.builder()
                .baseUrl("https://dashscope.aliyuncs.com")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("X-DashScope-Async", "enable")
                .defaultHeader("X-DashScope-OssResourceResolve", "enable")
                .build()
                .post()
                .uri("/api/v1/services/aigc/image2image/image-synthesis")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(ImageGenIdResponse.class)
                .flatMap(resp -> {

                    if (resp == null || resp.getOutput() == null) {
                        return Mono.error(new RuntimeException("empty response"));
                    }

                    String taskId = resp.getOutput().getTaskId();

                    if (taskId == null || taskId.isBlank()) {
                        return Mono.error(new RuntimeException("taskId is null"));
                    }

                    return Mono.just(taskId);
                });
    }

    @Override
    public Mono<ImageTranslateResponse> getImageTranslateResult(String taskId) {
        String url = "https://dashscope.aliyuncs.com";

        WebClient client = WebClient.builder()
                .baseUrl(url)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();

        return client.get()
                .uri("/api/v1/tasks/{taskId}", taskId)
                .retrieve()
                .bodyToMono(ImageTranslateResponse.class);
    }
}
