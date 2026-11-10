package com.eniac.weavroa.llm.model.image.gen;

import com.eniac.weavroa.llm.model.CapabilityModel;
import com.eniac.weavroa.llm.model.image.gen.request.ImageGenRequest;
import com.eniac.weavroa.llm.model.image.gen.response.ImageGenResponse;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * 图像生成模型接口
 */
public interface ImageGenModel extends CapabilityModel {

    Mono<String> imageGen(ImageGenRequest request);

    Mono<ImageGenResponse> getImageById(String taskId);

    Map<String,String> imageGenByWanPro(Map<String,String> contnetPromptMap, String referToImage);
}
