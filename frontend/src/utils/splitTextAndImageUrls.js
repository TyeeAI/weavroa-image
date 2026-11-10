/**
 * 将一段文本拆成「纯文本」与「图片 URL」交替片段，用于气泡内用 <img> 渲染预览。
 *
 * 默认识别 http(s) 且路径上以常见图片后缀结尾的 URL（含查询串，便于签名链接）。
 * 若你的 CDN 不带后缀，可在下方 IMAGE_URL_REGEX 中按需扩展。
 */

// http(s) + 非空白 + 图片后缀 + 可选 query（避免把后面的中文标点吃进 URL）
const IMAGE_URL_REGEX =
  /https?:\/\/[^\s<>"']+\.(?:png|jpe?g|gif|webp|svg|bmp)(?:\?[^\s<>"']*)?/gi

/**
 * @param {string} text
 * @returns {Array<{ type: 'text', value: string } | { type: 'image', src: string }>}
 */
export function splitTextAndImageUrls(text) {
  if (!text) {
    return [{ type: 'text', value: '' }]
  }

  const parts = []
  let lastIndex = 0
  const re = new RegExp(IMAGE_URL_REGEX.source, IMAGE_URL_REGEX.flags)
  let match = re.exec(text)

  while (match !== null) {
    if (match.index > lastIndex) {
      parts.push({ type: 'text', value: text.slice(lastIndex, match.index) })
    }
    parts.push({ type: 'image', src: match[0] })
    lastIndex = match.index + match[0].length
    match = re.exec(text)
  }

  if (lastIndex < text.length) {
    parts.push({ type: 'text', value: text.slice(lastIndex) })
  }

  if (parts.length === 0) {
    return [{ type: 'text', value: text }]
  }

  return parts
}
