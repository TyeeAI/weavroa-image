package com.eniac.weavroa.llm.model.image.gen.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImageGenRequest {

    /**
     * 模型名称（必填）
     *
     * 示例：
     * - wanx-v1
     * - wanx-v1-t2i
     *
     * 说明：
     * 指定使用的图像生成模型版本
     */
    private String model;

    /**
     * 输入参数（必填）
     * 包含 prompt / negative_prompt / 参考图等
     */
    private ImageGenInput input;

    /**
     * 图像生成控制参数（可选）
     * 用于控制风格、分辨率、随机性等
     */
    private ImageGenParameters parameters;

    public ImageGenRequest(ImageGenInput input, ImageGenParameters parameters) {
        this.input = input;
        this.parameters = parameters;
    }

    /**
     * 输入内容结构
     * 用于描述“生成什么样的图”
     */
    @Getter
    @Setter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ImageGenInput {

        /**
         * 正向提示词（必填）
         *
         * 用于描述期望生成的画面内容
         *
         * 示例：
         * 一只坐着的橘黄色的猫，表情愉悦，光影柔和
         *
         * 说明：
         * - 支持中英文
         * - 最大800字符
         * - 越具体效果越稳定
         */
        private String prompt;

        /**
         * 反向提示词（可选）
         *
         * 用于排除不希望出现的内容
         *
         * 示例：
         * 低质量、模糊、多手指、畸形、噪点
         *
         * 说明：
         * - 最大500字符
         * - 用于“约束模型错误生成”
         */
        private String negative_prompt;

        /**
         * 参考图（垫图）URL（可选）
         *
         * 用于图生图 / 风格迁移 / 结构参考
         *
         * 要求：
         * - JPG / PNG / WEBP 等
         * - ≤10MB
         * - 256~4096像素
         * - URL不能包含中文
         */
        private String ref_img;

    }

    @Getter
    @Setter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ImageGenParameters {

        /**
         * 风格控制（可选）
         * 可选值：
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
         */
        private String style;

        /**
         * 输出分辨率（可选）
         * 支持：
         * - 1024*1024（默认）
         * - 720*1280
         * - 768*1152
         * - 1280*720
         */
        private String size;

        /**
         * 生成图片数量（可选）
         * 范围：
         * 1 ~ 4
         * 默认：
         * 4
         */
        private Integer n;

        /**
         * 随机种子（可选）
         * 用于控制生成稳定性
         * 范围：
         * 0 ~ 2147483647
         * 说明：
         * - 相同 seed → 可复现结果
         * - 不同 seed → 生成多样性结果
         */
        private Integer seed;


        /**
         * 参考图相似度控制（可选）
         * 范围：
         * 0.0 ~ 1.0
         * 说明：
         * - 越大 → 越接近参考图
         * - 越小 → 创造性越强
         */
        private Float ref_strength;

        /**
         * 参考图模式（可选）
         * 可选值：
         * - repaint：基于内容重绘（默认）
         * - refonly：仅风格迁移
         */
        private String ref_mode;
    }
}
