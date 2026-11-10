package com.eniac.weavroa.image.api;

import com.eniac.weavroa.image.entity.Result;
import com.eniac.weavroa.llm.factory.ModelFactory;
import com.eniac.weavroa.llm.model.chat.Message;
import com.eniac.weavroa.llm.model.image.gen.ImageGenModel;
import com.eniac.weavroa.llm.model.image.gen.request.ImageGenRequest;
import com.eniac.weavroa.llm.model.image.translate.ImageTranslateModel;
import com.eniac.weavroa.util.string.PublicUrlUtil;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 图片能力接口：文生图（插画）与图片翻译
 */
@RestController
@RequestMapping("/api/v1/image")
@AllArgsConstructor
public class ImageController {

    private final ModelFactory modelFactory;

    /**
     * 文生图：对话模型推断风格标签后调用图像生成模型，轮询直至任务完成并返回图片地址
     */
    @PostMapping("/generate")
    public Mono<Result> generate(@RequestBody Map<String, Object> json) {

        String message = json.get("message").toString();

        List<Message> build = Message.builder()
                .user(String.format("""
                        从以下内容中猜测出风格意图。

                        可选风格仅有如下:
                        <auto>：默认值，由模型随机输出图像风格。
                        <photography>：摄影。
                        <portrait>：人像写真。
                        <3d cartoon>：3D卡通。
                        <anime>：动画。
                        <oil painting>：油画。
                        <watercolor>：水彩。
                        <sketch>：素描。
                        <chinese painting>：中国画。
                        <flat illustration>：扁平插画。

                        仅输出对应的标签即可,当不符合其他标签时才选择输出默认值的标签

                        内容: %s
                        """, message)
                ).build();

        ImageGenModel imageGenModel = this.modelFactory.imageGenModel();

        return this.modelFactory.chatModel()
                .generate(build)
                .map(chatResponse -> chatResponse.getChoices().getFirst().getMessage().getContent())
                .flatMap(style -> {
                    ImageGenRequest request = new ImageGenRequest(
                            ImageGenRequest.ImageGenInput.builder().prompt(message).build(),
                            ImageGenRequest.ImageGenParameters.builder().style(style).n(1).build()
                    );

                    return imageGenModel.imageGen(request);
                })
                .flatMap(taskId -> {
                    return Flux.interval(Duration.ZERO, Duration.ofSeconds(1))
                            // 每次触发去请求
                            .flatMap(i -> imageGenModel.getImageById(taskId))
                            // 直到成功
                            .filter(r -> "SUCCEEDED".equals(r.getOutput().getTask_status()))
                            // 只取第一个成功结果
                            .next()
                            // 最长等待时间
                            .timeout(Duration.ofSeconds(60));
                })
                .map(d -> d.getOutput().getResults().getFirst().getUrl())
                .map(Result::success);
    }

    /**
     * 图片翻译：上传图片（multipart），先转存公网 URL 再创建异步翻译任务，返回任务 ID
     */
    @PostMapping(value = "/translate/task", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<Result> translateTask(@RequestPart("file") FilePart filePart,
                                      @RequestPart("sourceLanguage") String sourceLanguage,
                                      @RequestPart("targetLanguage") String targetLanguage) {

        return Mono.usingWhen(

                Mono.fromCallable(() -> Files.createTempDirectory("image_translate_"))
                        .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic()),

                tempDir -> {
                    String fileName = Optional.of(filePart.filename())
                            .orElse("temp_image.png");

                    Path tempFile = tempDir.resolve(fileName);

                    return filePart.transferTo(tempFile)
                            .then(Mono.defer(() -> {
                                ImageTranslateModel imageTranslateModel = this.modelFactory.imageTranslateModel();
                                String modelName = imageTranslateModel.getName();

                                String imageUrl = PublicUrlUtil.uploadFileAndGetTempUrl(
                                        tempFile.toString(),
                                        modelName
                                );

                                return imageTranslateModel
                                        .getImageTranslateId(sourceLanguage, targetLanguage, imageUrl)
                                        .map(Result::success);
                            }))
                            .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
                },

                tempDir -> Mono.fromRunnable(() -> {
                    try {
                        Files.walk(tempDir)
                                .sorted(Comparator.reverseOrder())
                                .forEach(path -> {
                                    try {
                                        Files.deleteIfExists(path);
                                    } catch (Exception ignored) {
                                    }
                                });
                    } catch (Exception ignored) {
                    }
                }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
        );
    }

    /**
     * 图片翻译结果查询
     */
    @GetMapping("/translate/result")
    public Mono<Result> translateResult(@RequestParam String taskId) {

        ImageTranslateModel imageTranslateModel = this.modelFactory.imageTranslateModel();

        return imageTranslateModel.getImageTranslateResult(taskId)
                .map(Result::success);
    }
}
