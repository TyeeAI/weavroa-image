package com.eniac.weavroa.llm.model.image.gen;

import com.eniac.weavroa.llm.entity.request.WanImageRequest;
import com.eniac.weavroa.llm.entity.response.ImageGenIdResponse;
import com.eniac.weavroa.llm.entity.response.WanImageResponse;
import com.eniac.weavroa.llm.model.AbstractModel;
import com.eniac.weavroa.llm.model.image.gen.request.ImageGenRequest;
import com.eniac.weavroa.llm.model.image.gen.response.ImageGenResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.experimental.SuperBuilder;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import java.util.*;

/**
 * 简单图像生成模型实现
 */
@SuperBuilder
public class SimpleImageGenModel extends AbstractModel<Object, Object> implements ImageGenModel {

    private static String Base_API_URL = "https://dashscope.aliyuncs.com";

    private static ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<String> imageGen(ImageGenRequest request) {

        request.setModel(this.getName());

        return WebClient.builder()
                .baseUrl(this.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("X-DashScope-Async", "enable")
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create()
                                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000) // 连接超时30秒
                                .responseTimeout(Duration.ofSeconds(30)) // 响应超时30秒
                                .doOnConnected(conn ->
                                        conn.addHandlerLast(new ReadTimeoutHandler(30))    // 读超时30秒
                                                .addHandlerLast(new WriteTimeoutHandler(30))  // 写超时30秒
                                )
                ))
                .build()
                .post()
                .uri("services/aigc/text2image/image-synthesis")
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, resp -> {
                    // 打印阿里云返回的具体错误原因
                    return resp.bodyToMono(String.class)
                            .doOnNext(body -> System.err.println("阿里云400错误详情：" + body))
                            .flatMap(body -> Mono.error(new RuntimeException("接口请求失败：" + body)));
                })
                .bodyToMono(ImageGenIdResponse.class)
                .flatMap(d -> {
                    if (d == null || d.getOutput() == null) {
                        return Mono.error(new RuntimeException("empty response"));
                    }
                    String taskId = d.getOutput().getTaskId();

                    if (taskId == null || taskId.isBlank()) {
                        return Mono.error(new RuntimeException("taskId is null"));
                    }

                    return Mono.just(taskId);
                });
    }

    public Mono<ImageGenResponse> getImageById(String taskId) {
        return WebClient.builder()
                .baseUrl(this.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build()
                .get()
                .uri("tasks/" + taskId)
                .retrieve()
                .bodyToMono(ImageGenResponse.class);
    }

    @Override
    public Map<String,String> imageGenByWanPro(Map<String,String> contnetPromptMap,String referToImage) {

        Map<String,String> expressionImageUrls = new HashMap<>();

        for (Map.Entry<String, String> entry : contnetPromptMap.entrySet()) {

            String expression = entry.getKey();

            String contentPrompt = entry.getValue();

            WanImageRequest requestBody = new WanImageRequest();
            requestBody.setModel(this.getName());

            WanImageRequest.Input input = new WanImageRequest.Input();

            WanImageRequest.Message message = new WanImageRequest.Message();
            message.setRole("user");

            ArrayList<WanImageRequest.Content> contents = new ArrayList<>();

            WanImageRequest.Content textContent = new WanImageRequest.Content();
            textContent.setType("text");
            textContent.setText(contentPrompt);
            contents.add(textContent);

            if (referToImage != null){
                WanImageRequest.Content referImageContent = new WanImageRequest.Content();
                referImageContent.setType("image");
                referImageContent.setImage(referToImage);
                contents.add(referImageContent);
            }

            message.setContent(contents);
            input.setMessages(List.of(message));

            requestBody.setInput(input);

            WanImageRequest.Parameters parameters = new WanImageRequest.Parameters();
            parameters.setSize("2K");
            parameters.setN(1);
            parameters.setWatermark(false);
            parameters.setSeed(4);

            requestBody.setParameters(parameters);

            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
            String jsonBody = null;
            try {
                jsonBody = mapper.writeValueAsString(requestBody);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            // 4. 创建 HttpClient 并发送请求
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Base_API_URL + this.getBaseUrl()))
                    .header("Authorization", "Bearer " + this.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            try {

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    throw new RuntimeException("图片生成失败，HTTP错误: " + response.body());
                }

                // 3. 解析 JSON
                WanImageResponse result = objectMapper.readValue(response.body(), WanImageResponse.class);

                if (result == null
                        || result.getOutput() == null
                        || result.getOutput().getChoices() == null
                        || result.getOutput().getChoices().isEmpty()) {
                    throw new RuntimeException("图片生成失败：返回结构异常");
                }

                Optional<String> url = result.getOutput()
                        .getChoices()
                        .getFirst()
                        .getMessage()
                        .getContent()
                        .stream()
                        .map(WanImageResponse.Content::getImage)
                        .filter(img -> img != null && !img.isEmpty())
                        .findFirst();

                url.ifPresent(s -> expressionImageUrls.put(expression, s));

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return expressionImageUrls;
    }
}
