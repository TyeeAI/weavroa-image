package com.eniac.weavroa.llm.factory.registry.handler;

import com.eniac.weavroa.configuration.http.WebHttpClient;
import com.eniac.weavroa.llm.config.ModelProperties;
import com.eniac.weavroa.llm.model.ModelCapability;
import com.eniac.weavroa.llm.model.image.gen.ImageGenModel;
import com.eniac.weavroa.llm.model.image.gen.SimpleImageGenModel;

public class ImageGenWanProModelHandler implements ModelClientHandler<ImageGenModel>{
    @Override
    public boolean support(ModelProperties properties, ModelCapability capability) {
        return capability == ModelCapability.IMAGE_GEN_WAN_RPO;
    }

    @Override
    public ImageGenModel create(ModelProperties properties, WebHttpClient httpClient) {
        return SimpleImageGenModel.builder()
                .id(properties.getProvider() + ":" + properties.getName())
                .name(properties.getName())
                .provider(properties.getProvider())
                .specialization(properties.getSpecialization())
                .capability(ModelCapability.IMAGE_GEN_WAN_RPO)
                .baseUrl(properties.getBaseUrl())
                .apiKey(properties.getApiKey())
                .httpClient(httpClient)
                .build();
    }
}
