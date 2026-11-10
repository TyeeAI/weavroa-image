Web frontend for the weavroa image capability, providing two visual pages: **text-to-image (illustration)** and **image translation**.

## About Us

Super Taiyi (https://tyeeai.com/) is a technology company providing AI application implementation and digital transformation services for enterprises.

### Recommended Scenarios

- Illustration & marketing material production: enter a description, the style is matched automatically, and illustrations, posters and artwork are produced quickly
- Cross-border content localization: upload an image with embedded text and translate it into a target language while keeping the original layout

## Features

| Page | Description |
|------|------|
| Text-to-image (illustration) | Enter a description; the style is inferred automatically and an illustration is generated, with streaming progress feedback |
| Image translation | Upload an image and pick source/target languages; the translation task is polled until the translated image is ready |

## Tech Stack

- Vue 3 + Vite
- vue-router 4

## Quick Start

### Requirements

- Node.js 20+
- Backend service [weavroa-image](../) (default `http://127.0.0.1:8080`)

### Install & Run

```bash
# Install dependencies
npm install

# Development mode (default port 5173, /api requests are proxied to 127.0.0.1:8080)
npm run dev

# Production build (output to dist/, router base is /tool/)
npm run build
```

## Environment Variables

All entries in `.env.development` are optional:

| Variable | Description |
|------|------|
| `VITE_API_BASE_URL` | Backend base URL, set it when all endpoints share one domain (backend CORS required) |
| `VITE_ILLUSTRATION_API_URL` | Full URL of the text-to-image endpoint; defaults to `/api/v1/image/generate` |

## Directory Structure

```
src/
├── assets/            # static assets such as page backgrounds
├── pages/
│   ├── illustration/  # text-to-image (illustration) page
│   └── imageTranslate/ # image translation page
├── utils/             # stream payload parsing, text/image URL splitting, etc.
└── router/            # router configuration
```

## License

This project is licensed under the [Apache License 2.0](./LICENSE).
