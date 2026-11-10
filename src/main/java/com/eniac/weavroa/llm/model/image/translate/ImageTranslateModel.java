package com.eniac.weavroa.llm.model.image.translate;

import com.eniac.weavroa.llm.entity.response.ImageTranslateResponse;
import com.eniac.weavroa.llm.model.CapabilityModel;
import com.eniac.weavroa.llm.model.image.gen.ImageGenModel;
import reactor.core.publisher.Mono;

public interface ImageTranslateModel extends CapabilityModel {

    public Mono<String> getImageTranslateId(String sourceLange, String targetLanguage, String imageUrl);

    public Mono<ImageTranslateResponse> getImageTranslateResult(String taskId);

}
