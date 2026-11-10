/**
 * 将流式 JSON 负载（SSE `data:` 后一段、NDJSON 一行、或一行内 JSON 数组）转为可拼接到界面的纯文本。
 *
 * 常见约定：
 * - `{ message, data, code }`：`message` 常为 `"success"`；`data` 可为 **字符串增量**（摘要常见），或 **对象**（如 `{ content, role, partial }`）。
 * - 同时含 `data` 与 `code` 时：`code` 非 `200` 的帧返回空串；`data` 为字符串时原样返回（含 `""`）；`data` 为对象时优先取 `content` 字段（允许 `""`）。
 */

/**
 * @param {unknown} value 接口里的 `data` 字段，任意类型
 * @returns {string}
 */
function textFromDataField(value) {
  if (value == null) {
    return ''
  }
  if (typeof value === 'string') {
    return value
  }
  if (typeof value === 'number' || typeof value === 'boolean') {
    return String(value)
  }
  if (typeof value !== 'object') {
    return ''
  }

  const v = /** @type {Record<string, unknown>} */ (value)

  // Summary / Agent：{ role, content, partial, toolCalls, ... } —— `content` 含空串也必须原样返回，不能用 ?? 链误落到 JSON.stringify
  if (Object.prototype.hasOwnProperty.call(v, 'content')) {
    const c = v.content
    if (c == null) {
      return ''
    }
    return typeof c === 'string' ? c : String(c)
  }

  if (Object.prototype.hasOwnProperty.call(v, 'partial') && v.partial != null) {
    if (typeof v.partial === 'string') {
      return v.partial
    }
  }

  const nested =
    v.text ??
    v.message ??
    v.delta?.content ??
    (typeof v.delta === 'string' ? v.delta : '') ??
    (typeof v.image_url === 'string' ? v.image_url : '') ??
    (typeof v.imageUrl === 'string' ? v.imageUrl : '') ??
    (typeof v.url === 'string' ? v.url : '') ??
    ''

  if (nested) {
    return typeof nested === 'string' ? nested : String(nested)
  }

  try {
    return JSON.stringify(value)
  } catch {
    return ''
  }
}

/**
 * @param {unknown} parsed `JSON.parse` 后的对象或其它类型
 * @returns {string}
 */
export function extractTextFromStreamJsonPayload(parsed) {
  if (parsed == null) {
    return ''
  }
  if (typeof parsed === 'string') {
    return parsed
  }
  if (typeof parsed === 'number' || typeof parsed === 'boolean') {
    return String(parsed)
  }
  if (typeof parsed !== 'object') {
    return ''
  }

  // 一次负载内多条事件：[{ data: {...}, code }, ...]
  if (Array.isArray(parsed)) {
    return parsed.map((item) => extractTextFromStreamJsonPayload(item)).join('')
  }

  const envelope = /** @type {Record<string, unknown>} */ (parsed)

  // 摘要流：{ data: "增量" | { content }, message, code } —— 非 200 的帧不拼正文
  if (Object.prototype.hasOwnProperty.call(envelope, 'data') && Object.prototype.hasOwnProperty.call(envelope, 'code')) {
    const code = envelope.code
    if (code !== undefined && code !== null && String(code) !== '200') {
      return ''
    }
  }

  // 业务常见：{ message: "success", data: ... } —— 有 `data` 键时只解析 `data`，不回退到根级 `message`
  if (Object.prototype.hasOwnProperty.call(parsed, 'data')) {
    return textFromDataField(parsed.data)
  }

  return (
    parsed.content ??
    parsed.text ??
    parsed.message ??
    parsed.delta?.content ??
    (typeof parsed.delta === 'string' ? parsed.delta : '') ??
    (typeof parsed.image_url === 'string' ? parsed.image_url : '') ??
    (typeof parsed.imageUrl === 'string' ? parsed.imageUrl : '') ??
    (typeof parsed.url === 'string' ? parsed.url : '') ??
    ''
  )
}

/**
 * @param {string} payload 单行负载（可能为 JSON 或纯文本）
 * @returns {string}
 */
export function extractTextFromPayload(payload) {
  const trimmed = payload.trim()
  if (!trimmed || trimmed === '[DONE]') {
    return ''
  }
  try {
    const parsed = JSON.parse(trimmed)
    return extractTextFromStreamJsonPayload(parsed)
  } catch {
    return trimmed
  }
}
