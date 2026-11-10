package com.eniac.weavroa.llm.model;

import lombok.Getter;

/**
 * 模型能力类型
 */
@Getter
public enum ModelCapability {

    /**
     * 文本生成/对话
     */
    TEXT("文本生成与对话"),

    /**
     * 图像生成
     */
    IMAGE_GEN("图像生成"),

    /**
     * 图片生成（万相Pro）
     */
    IMAGE_GEN_WAN_RPO("图片生成"),

    /**
     * 图片翻译
     */
    IMAGE_TRANSLATE("图片翻译");


    /**
     * 能力描述
     */
    private final String description;

    ModelCapability(String description) {
        this.description = description;
    }
}
