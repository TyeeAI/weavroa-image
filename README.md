# weavroa-image

图片能力服务，提供**文生图（插画）**与**图片翻译**两大能力，可独立运行、打包、部署。

## 关于我们

超级太乙（https://tyeeai.com/）是一家面向企业提供AI应用落地与数智化转型服务的科技公司。

### 场景推荐

- 插画与营销素材生产：输入一句描述自动推断风格（摄影、动漫、油画、水彩等 9 种），快速产出配图、海报与插画素材
- 跨境内容本地化：上传含文字的图片（截图、海报、商品图），将图中文字翻译为目标语言并回填原图版式

## 功能特性

| 能力 | 说明 |
|------|------|
| 文生图（插画） | 输入自然语言描述，对话模型自动推断画面风格，调用图像生成模型产出图片 |
| 图片翻译 | 上传图片，识别图中文字并翻译为目标语言，异步任务轮询返回翻译结果 |

内置对话模型接入层（`com.eniac.weavroa.llm`），基于 OpenAI 兼容协议，可通过配置注册任意兼容模型，用于风格推断等对话场景。

## 前端界面

配套 Web 前端位于 [`frontend/`](./frontend)，提供文生图与图片翻译的可视化操作页面，详见 [frontend/README.md](./frontend/README.md)。

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.6+（或直接使用项目自带的 Maven Wrapper）

### 启动

```bash
export DASHSCOPE_API_KEY=你的百炼APIKey

# 开发模式
./mvnw spring-boot:run

# 打包
./mvnw -DskipTests package

# 部署运行
java -jar target/weavroa-image-standalone-0.0.1-SNAPSHOT.jar
```

默认端口 `8080`（可通过 `server.port` 修改）。

## 配置

```yaml
weavroa:
  llm:
    model:
      register:
        TEXT:
          - name: qwen-plus             # 风格推断用的对话模型，可换成任意 OpenAI 兼容模型
            base-url: https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions
            api-key: ${DASHSCOPE_API_KEY:}
        IMAGE_GEN:
          - name: wanx-v1               # 文生图模型
            base-url: https://dashscope.aliyuncs.com/api/v1/
            api-key: ${DASHSCOPE_API_KEY:}
        IMAGE_TRANSLATE:
          - name: qwen-mt-image         # 图片翻译模型
            base-url: /api/v1/services/aigc/image2image/image-synthesis
            api-key: ${DASHSCOPE_API_KEY:}
```

密钥一律通过环境变量注入，代码与配置中不含任何硬编码密钥。

## REST 接口

### 文生图（插画）

| 方法 | 路径 | 功能 |
|------|------|------|
| POST | `/api/v1/image/generate` | 提交描述，风格推断 + 图像生成，返回图片地址 |

### 图片翻译

| 方法 | 路径 | 功能 |
|------|------|------|
| POST | `/api/v1/image/translate/task` | 上传图片创建翻译任务（multipart），返回任务 ID |
| GET | `/api/v1/image/translate/result` | 按任务 ID 查询翻译结果 |

## 技术栈

- Spring Boot 3.5.4（Web Starter + WebFlux，Java 21）
- httpclient 4.5.14、httpmime 4.5.14、org.json 20240303、gson 2.13.1、okhttp 4.12.0、lombok

## License

本项目基于 [Apache License 2.0](./LICENSE) 协议开源。
