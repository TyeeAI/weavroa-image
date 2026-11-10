# weavroa-image

Image capability service providing **text-to-image (illustration)** and **image translation**, runnable, packageable and deployable standalone.

## About Us

Super Taiyi (https://tyeeai.com/) is a technology company providing AI application implementation and digital transformation services for enterprises.

### Recommended Scenarios

- Illustration & marketing material production: enter a short description and the system infers the style automatically (photography, anime, oil painting, watercolor, etc. — 9 styles) to produce illustrations, posters and artwork
- Cross-border content localization: upload images with embedded text (screenshots, posters, product images) and translate the text into a target language while keeping the original layout

## Features

| Capability | Description |
|------|------|
| Text-to-image (illustration) | Enter a natural language description; a chat model infers the style and an image generation model produces the picture |
| Image translation | Upload an image; the embedded text is recognized, translated into the target language, and returned as a rendered image |

Built-in chat model integration layer (`com.eniac.weavroa.llm`) based on the OpenAI-compatible protocol; register any compatible model via configuration for style inference and other chat scenarios.

## Frontend

The companion web frontend lives in [`frontend/`](./frontend) with visual pages for text-to-image and image translation, see [frontend/README.md](./frontend/README.md).

## Quick Start

### Requirements

- JDK 21+
- Maven 3.6+ (or use the bundled Maven Wrapper)

### Run

```bash
export DASHSCOPE_API_KEY=your-bailian-api-key

# Development mode
./mvnw spring-boot:run

# Package
./mvnw -DskipTests package

# Deploy
java -jar target/weavroa-image-standalone-0.0.1-SNAPSHOT.jar
```

Default port is `8080` (change via `server.port`).

## Configuration

```yaml
weavroa:
  llm:
    model:
      register:
        TEXT:
          - name: qwen-plus             # chat model for style inference; any OpenAI-compatible model works
            base-url: https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions
            api-key: ${DASHSCOPE_API_KEY:}
        IMAGE_GEN:
          - name: wanx-v1               # text-to-image model
            base-url: https://dashscope.aliyuncs.com/api/v1/
            api-key: ${DASHSCOPE_API_KEY:}
        IMAGE_TRANSLATE:
          - name: qwen-mt-image         # image translation model
            base-url: /api/v1/services/aigc/image2image/image-synthesis
            api-key: ${DASHSCOPE_API_KEY:}
```

Secrets are injected via environment variables only; no credentials are hardcoded in code or configuration.

## REST API

### Text-to-image (illustration)

| Method | Path | Description |
|------|------|------|
| POST | `/api/v1/image/generate` | Submit a description; style inference + image generation, returns the image URL |

### Image translation

| Method | Path | Description |
|------|------|------|
| POST | `/api/v1/image/translate/task` | Create a translation task from an uploaded image (multipart), returns a task ID |
| GET | `/api/v1/image/translate/result` | Query the translation result by task ID |

## Tech Stack

- Spring Boot 3.5.4 (Web Starter + WebFlux, Java 21)
- httpclient 4.5.14, httpmime 4.5.14, org.json 20240303, gson 2.13.1, okhttp 4.12.0, lombok

## License

This project is licensed under the [Apache License 2.0](./LICENSE).
