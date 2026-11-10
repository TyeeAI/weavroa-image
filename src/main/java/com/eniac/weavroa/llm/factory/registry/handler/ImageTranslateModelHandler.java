package com.eniac.weavroa.llm.factory.registry.handler;

import com.eniac.weavroa.configuration.http.WebHttpClient;
import com.eniac.weavroa.llm.config.ModelProperties;
import com.eniac.weavroa.llm.model.ModelCapability;
import com.eniac.weavroa.llm.model.image.translate.ImageTranslateModel;
import com.eniac.weavroa.llm.model.image.translate.SimpleImageTranslateModel;

/**
 * 图片翻译模型处理器
 */
public class ImageTranslateModelHandler implements ModelClientHandler<ImageTranslateModel> {

    @Override
    public boolean support(ModelProperties properties, ModelCapability capability) {
        return capability == ModelCapability.IMAGE_TRANSLATE;
    }

    @Override
    public ImageTranslateModel create(ModelProperties properties, WebHttpClient httpClient) {
        return SimpleImageTranslateModel.builder()
                .id(properties.getProvider() + ":" + properties.getName())
                .name(properties.getName())
                .provider(properties.getProvider())
                .specialization(properties.getSpecialization())
                .capability(ModelCapability.IMAGE_TRANSLATE)
                .baseUrl(properties.getBaseUrl())
                .apiKey(properties.getApiKey())
                .httpClient(httpClient)
                .build();
    }
}
