/**
 * 文本生成插画 —— 流式请求封装（与页面 `illustration/index.vue` 配合使用）。
 *
 * 请求约定：
 * - 方法 POST，请求体 JSON：`{ message: string }`
 * - 本地开发经 vite.config.js 将 `/api` 代理到 `http://127.0.0.1:8080`，与直连 `http://localhost:8080/api/v1/image/generate` 等价
 *
 * 响应分流（由响应头 Content-Type 决定）：
 * - `text/event-stream`：按 SSE 解析 `data:` 行
 * - 其它：按 UTF-8 原始分块追加
 *
 * 环境变量 `VITE_ILLUSTRATION_API_URL`：未设置时使用 `/api/v1/image/generate`
 */
import { extractTextFromPayload } from '../../../utils/extractStreamPayloadText.js'

export const ILLUSTRATION_STREAM_URL =
  import.meta.env.VITE_ILLUSTRATION_API_URL ?? '/api/v1/image/generate'

/** 负载解析见 `extractStreamPayloadText.js`（含 `data` 字段时只展示其中内容）。 */
function emitSseLine(line, onDelta) {
  const trimmedEnd = line.trimEnd()
  if (!trimmedEnd) {
    return
  }
  if (trimmedEnd.startsWith('data:')) {
    const payload = trimmedEnd.slice(5).trimStart()
    const delta = extractTextFromPayload(payload)
    if (delta) {
      onDelta(delta)
    }
  } else {
    const delta = extractTextFromPayload(trimmedEnd)
    if (delta) {
      onDelta(delta)
    }
  }
}

/**
 * @param {string} message
 * @param {{ onDelta: (chunk: string) => void, signal?: AbortSignal }} options
 * @returns {Promise<void>}
 */
export async function postIllustrationStream(message, { onDelta, signal }) {
  const response = await fetch(ILLUSTRATION_STREAM_URL, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream, application/json, text/plain, */*',
    },
    body: JSON.stringify({ message }),
    signal,
  })

  if (!response.ok) {
    const errText = await response.text().catch(() => '')
    throw new Error(errText || `请求失败（${response.status}）`)
  }

  const reader = response.body?.getReader()
  if (!reader) {
    throw new Error('当前环境不支持 ReadableStream，无法读取流式响应')
  }

  const decoder = new TextDecoder()
  const contentType = (response.headers.get('content-type') || '').toLowerCase()
  const isEventStream = contentType.includes('text/event-stream')

  if (!isEventStream) {
    while (true) {
      const { done, value } = await reader.read()
      if (done) {
        const tail = decoder.decode()
        if (tail) {
          onDelta(tail)
        }
        break
      }
      onDelta(decoder.decode(value, { stream: true }))
    }
    return
  }

  let carry = ''
  while (true) {
    const { done, value } = await reader.read()
    if (value) {
      carry += decoder.decode(value, { stream: true })
    }
    if (done) {
      carry += decoder.decode()
    }

    let cut
    while ((cut = carry.indexOf('\n')) >= 0) {
      const line = carry.slice(0, cut)
      carry = carry.slice(cut + 1)
      emitSseLine(line, onDelta)
    }

    if (done) {
      if (carry) {
        emitSseLine(carry, onDelta)
      }
      break
    }
  }
}
