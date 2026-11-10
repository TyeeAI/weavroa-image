<script setup>
/**
 * AI 插画生成页：对话输入 + 流式展示（说明/链接等由后端流式返回）。
 */
import { computed, nextTick, onBeforeUnmount, ref } from 'vue'
import backgroundImage from '../../assets/image/18347150586858880.jpg'
import { postIllustrationStream } from './api/streamIllustration.js'
import { splitTextAndImageUrls } from '../../utils/splitTextAndImageUrls.js'

/** 后端非 2xx、流中断、网络异常等统一提示（不暴露原始错误栈或接口细节） */
const BACKEND_ERROR_HINT = '生成失败，可能原因是生成时间过长或内容敏感'

const inputText = ref('')
const messages = ref([])
const isStreaming = ref(false)
const errorMessage = ref('')
const scrollRef = ref(null)

let abortController = null

const pageStyle = computed(() => ({
  '--page-background': `url("${backgroundImage}")`,
}))

const canSend = computed(() => {
  return Boolean(inputText.value.trim()) && !isStreaming.value
})

function scrollToBottom() {
  nextTick(() => {
    const el = scrollRef.value
    if (el) {
      el.scrollTop = el.scrollHeight
    }
  })
}

function stopStream() {
  abortController?.abort()
  abortController = null
  isStreaming.value = false
}

async function handleSend() {
  const text = inputText.value.trim()
  if (!text || isStreaming.value) {
    return
  }

  errorMessage.value = ''
  messages.value.push({ role: 'user', content: text })
  messages.value.push({ role: 'assistant', content: '' })
  const assistantIndex = messages.value.length - 1

  inputText.value = ''
  isStreaming.value = true
  scrollToBottom()

  abortController = new AbortController()

  try {
    await postIllustrationStream(text, {
      signal: abortController.signal,
      onDelta(chunk) {
        const current = messages.value[assistantIndex]
        if (current && current.role === 'assistant') {
          current.content += chunk
        }
        scrollToBottom()
      },
    })
  } catch (err) {
    if (err instanceof Error && err.name === 'AbortError') {
      const current = messages.value[assistantIndex]
      if (current?.role === 'assistant' && !current.content) {
        current.content = '（已停止生成）'
      }
    } else {
      errorMessage.value = ''
      const current = messages.value[assistantIndex]
      if (current?.role === 'assistant') {
        const prev = current.content.trim()
        current.content = prev ? `${prev}\n\n${BACKEND_ERROR_HINT}` : BACKEND_ERROR_HINT
      }
    }
  } finally {
    isStreaming.value = false
    abortController = null
    scrollToBottom()
  }
}

function handleComposerKeydown(event) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    handleSend()
  }
}

function clearChat() {
  if (isStreaming.value) {
    stopStream()
  }
  messages.value = []
  errorMessage.value = ''
}

onBeforeUnmount(() => {
  stopStream()
})

function messageSegments(content) {
  return splitTextAndImageUrls(content ?? '')
}

/** 图片加载失败：隐藏 <img>，在卡片内展示可点的原始链接 */
function onStreamImageError(event) {
  const img = event.target
  if (!(img instanceof HTMLImageElement)) {
    return
  }
  const wrap = img.closest('.chat-seg-img-wrap')
  wrap?.classList.add('has-img-error')
}
</script>

<template>
  <main class="page-shell illustration-page-shell" :style="pageStyle">
    <div class="page-mask illustration-mask"></div>

    <section class="illustration-layout">
      <header class="illustration-hero">
        <p class="hero-badge">AI Illustration</p>
        <h1 class="hero-title">用文字召唤插画</h1>
        <p class="hero-subtitle">
          描述画面、风格或角色，由后端完成生成；返回内容中的图片链接会在气泡内直接预览。
        </p>
      </header>

      <div class="chat-card">
        <div class="chat-toolbar">
          <span class="chat-toolbar-label">创作对话</span>
          <button type="button" class="ghost-button" :disabled="isStreaming" @click="clearChat">
            清空
          </button>
        </div>

        <div ref="scrollRef" class="chat-scroll">
          <div v-if="messages.length === 0" class="chat-empty">
            <p class="chat-empty-title">描述你想要的画面</p>
            <p class="chat-empty-desc">
              在底部输入创意文案并发送；流式文本中的图片链接会自动渲染为预览图（可点开原图）。
            </p>
          </div>

          <div
            v-for="(msg, index) in messages"
            :key="index"
            class="chat-row"
            :class="msg.role === 'user' ? 'is-user' : 'is-assistant'"
          >
            <div class="chat-bubble">
              <span class="chat-role">{{ msg.role === 'user' ? '你' : '插画' }}</span>

              <p v-if="msg.role === 'user'" class="chat-text">{{ msg.content }}</p>

              <div v-else class="chat-rich">
                <template v-for="(seg, si) in messageSegments(msg.content)" :key="si">
                  <span v-if="seg.type === 'text'" class="chat-seg-text">{{ seg.value }}</span>
                  <div v-else class="chat-seg-img-wrap">
                    <img
                      class="chat-seg-img"
                      :src="seg.src"
                      alt="生成插画预览"
                      loading="lazy"
                      referrerpolicy="no-referrer"
                      @error="onStreamImageError"
                    />
                    <p class="chat-seg-img-fallback">
                      <a :href="seg.src" target="_blank" rel="noopener noreferrer">{{ seg.src }}</a>
                    </p>
                    <a
                      class="chat-seg-img-open"
                      :href="seg.src"
                      target="_blank"
                      rel="noopener noreferrer"
                    >
                      下载原图
                    </a>
                  </div>
                </template>
                <span
                  v-if="isStreaming && index === messages.length - 1"
                  class="typing-cursor"
                />
              </div>
            </div>
          </div>
        </div>

        <p v-if="errorMessage" class="chat-error">{{ errorMessage }}</p>

        <div class="composer">
          <textarea
            v-model="inputText"
            class="composer-input"
            rows="3"
            placeholder="例如：赛博朋克雨夜街角的橘猫，霓虹反光，竖构图插画…（Enter 发送，Shift+Enter 换行）"
            :disabled="isStreaming"
            @keydown="handleComposerKeydown"
          />
          <div class="composer-actions">
            <button v-if="isStreaming" type="button" class="stop-button" @click="stopStream">
              停止
            </button>
            <button type="button" class="send-button" :disabled="!canSend" @click="handleSend">
              <span v-if="isStreaming" class="button-spinner" />
              <span v-else>生成</span>
            </button>
          </div>
        </div>
      </div>
    </section>
  </main>
</template>

<style scoped>
.illustration-page-shell {
  align-items: flex-start;
  padding-top: 48px;
  padding-bottom: 48px;
}

/* 轻微偏暖的蒙层，与摘要页的冷色区分 */
.illustration-mask {
  background:
    radial-gradient(circle at 20% 30%, rgba(255, 200, 160, 0.08), transparent 35%),
    radial-gradient(circle at 80% 70%, rgba(255, 160, 200, 0.06), transparent 32%),
    linear-gradient(180deg, rgba(0, 0, 0, 0.08), rgba(0, 0, 0, 0.58));
}

.illustration-layout {
  position: relative;
  z-index: 1;
  width: min(900px, 100%);
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 24px;
  animation: fade-up 0.75s ease-out;
}

.illustration-hero {
  text-align: center;
}

.illustration-hero .hero-badge {
  margin: 0 auto 12px;
  display: inline-block;
  padding: 8px 14px;
  border: 1px solid rgba(255, 220, 200, 0.22);
  border-radius: 999px;
  background: rgba(255, 200, 160, 0.1);
  backdrop-filter: blur(18px);
  font-size: 13px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: rgba(255, 235, 220, 0.95);
}

.illustration-hero .hero-title {
  margin: 0 0 10px;
  font-size: clamp(28px, 4.5vw, 44px);
  font-weight: 600;
  letter-spacing: -0.03em;
  line-height: 1.12;
  text-shadow: 0 16px 40px rgba(0, 0, 0, 0.5);
}

.illustration-hero .hero-subtitle {
  margin: 0 auto;
  max-width: 640px;
  font-size: 15px;
  line-height: 1.65;
  color: rgba(255, 255, 255, 0.72);
}

.chat-card {
  border-radius: 24px;
  border: 1px solid rgba(255, 210, 190, 0.14);
  background: rgba(14, 10, 8, 0.52);
  box-shadow:
    0 24px 80px rgba(0, 0, 0, 0.38),
    inset 0 1px 0 rgba(255, 220, 200, 0.06);
  backdrop-filter: blur(20px);
  display: flex;
  flex-direction: column;
  min-height: min(72vh, 640px);
  overflow: hidden;
}

.chat-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.chat-toolbar-label {
  font-size: 13px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: rgba(255, 220, 200, 0.5);
}

.ghost-button {
  padding: 8px 14px;
  font-size: 13px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(255, 255, 255, 0.06);
  color: rgba(255, 255, 255, 0.85);
  cursor: pointer;
  transition: background 0.2s ease;
}

.ghost-button:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.1);
}

.ghost-button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.chat-scroll {
  flex: 1;
  min-height: 200px;
  max-height: min(52vh, 480px);
  overflow-y: auto;
  padding: 18px 18px 8px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.chat-scroll::-webkit-scrollbar {
  width: 8px;
}

.chat-scroll::-webkit-scrollbar-thumb {
  background: rgba(255, 200, 170, 0.18);
  border-radius: 999px;
}

.chat-empty {
  margin: auto;
  text-align: center;
  padding: 32px 20px;
  color: rgba(255, 255, 255, 0.5);
}

.chat-empty-title {
  margin: 0 0 8px;
  font-size: 17px;
  color: rgba(255, 235, 220, 0.85);
}

.chat-empty-desc {
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
}

.chat-row {
  display: flex;
}

.chat-row.is-user {
  justify-content: flex-end;
}

.chat-row.is-assistant {
  justify-content: flex-start;
}

.chat-bubble {
  max-width: 92%;
  padding: 12px 16px 14px;
  border-radius: 18px;
  position: relative;
}

.is-user .chat-bubble {
  background: linear-gradient(135deg, rgba(255, 170, 120, 0.38), rgba(255, 120, 160, 0.22));
  border: 1px solid rgba(255, 200, 180, 0.35);
  border-bottom-right-radius: 6px;
}

.is-assistant .chat-bubble {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-bottom-left-radius: 6px;
}

.chat-role {
  display: block;
  font-size: 11px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: rgba(255, 220, 200, 0.45);
  margin-bottom: 6px;
}

.chat-text {
  margin: 0;
  font-size: 15px;
  line-height: 1.65;
  color: rgba(255, 255, 255, 0.92);
  white-space: pre-wrap;
  word-break: break-word;
}

.chat-rich {
  margin: 0;
  font-size: 15px;
  line-height: 1.65;
  color: rgba(255, 255, 255, 0.92);
}

.chat-seg-text {
  display: inline;
  white-space: pre-wrap;
  word-break: break-word;
}

.chat-seg-img-wrap {
  display: block;
  margin: 12px 0 4px;
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(0, 0, 0, 0.28);
}

.chat-seg-img-wrap.has-img-error .chat-seg-img {
  display: none;
}

.chat-seg-img {
  display: block;
  width: 100%;
  max-height: min(420px, 55vh);
  height: auto;
  object-fit: contain;
  background: rgba(0, 0, 0, 0.2);
}

.chat-seg-img-open {
  display: block;
  padding: 8px 12px;
  font-size: 13px;
  text-align: center;
  color: rgba(255, 220, 200, 0.9);
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.04);
}

.chat-seg-img-open:hover {
  background: rgba(255, 255, 255, 0.08);
}

.chat-seg-img-fallback {
  display: none;
  margin: 0;
  padding: 10px 12px;
  font-size: 13px;
  word-break: break-all;
}

.chat-seg-img-wrap.has-img-error .chat-seg-img-fallback {
  display: block;
}

.chat-seg-img-fallback a {
  color: rgba(160, 200, 255, 0.95);
  text-decoration: underline;
}

.typing-cursor {
  display: inline-block;
  width: 7px;
  height: 1.1em;
  margin-left: 2px;
  vertical-align: text-bottom;
  border-radius: 2px;
  background: rgba(255, 200, 160, 0.9);
  animation: blink 1s step-end infinite;
}

.chat-error {
  margin: 0;
  padding: 0 18px 8px;
  font-size: 13px;
  color: #ff9b9b;
}

.composer {
  padding: 14px 16px 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.composer-input {
  width: 100%;
  resize: vertical;
  min-height: 88px;
  max-height: 220px;
  padding: 14px 16px;
  border-radius: 16px;
  border: 1px solid rgba(255, 200, 180, 0.18);
  background: rgba(0, 0, 0, 0.35);
  color: #fff;
  font-size: 15px;
  line-height: 1.5;
  outline: none;
}

.composer-input:focus {
  border-color: rgba(255, 190, 140, 0.55);
  box-shadow: 0 0 0 1px rgba(255, 160, 120, 0.22);
}

.composer-input:disabled {
  opacity: 0.65;
}

.composer-input::placeholder {
  color: rgba(255, 255, 255, 0.38);
}

.composer-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 10px;
}

.stop-button {
  padding: 10px 18px;
  border-radius: 999px;
  border: 1px solid rgba(255, 180, 160, 0.35);
  background: rgba(255, 120, 100, 0.12);
  color: #ffc9be;
  font-size: 14px;
  cursor: pointer;
}

.stop-button:hover {
  background: rgba(255, 120, 100, 0.2);
}

.send-button {
  min-width: 96px;
  padding: 10px 22px;
  border-radius: 999px;
  border: 1px solid rgba(255, 200, 170, 0.28);
  background: linear-gradient(180deg, rgba(255, 200, 160, 0.28), rgba(255, 140, 120, 0.12));
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.send-button:hover:not(:disabled) {
  background: linear-gradient(180deg, rgba(255, 210, 170, 0.34), rgba(255, 150, 130, 0.18));
}

.send-button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.button-spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.25);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.85s linear infinite;
}

@keyframes blink {
  50% {
    opacity: 0;
  }
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@keyframes fade-up {
  from {
    opacity: 0;
    transform: translateY(14px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 768px) {
  .illustration-page-shell {
    padding-top: 28px;
  }

  .chat-scroll {
    max-height: 42vh;
  }
}
</style>
