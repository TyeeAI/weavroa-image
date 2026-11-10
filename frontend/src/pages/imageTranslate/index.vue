<script setup>
import { computed, onBeforeUnmount, ref } from 'vue'
import {
  createImageTranslateTask,
  getImageTranslateResult,
  isImageTaskFinished,
} from './api/index.js'

const languages = [
  { label: '简体中文', englishName: 'Chinese', value: 'zh' },
  { label: '英文', englishName: 'English', value: 'en' },
  { label: '韩语', englishName: 'Korean', value: 'ko' },
  { label: '日语', englishName: 'Japanese', value: 'ja' },
  { label: '俄语', englishName: 'Russian', value: 'ru' },
  { label: '西班牙语', englishName: 'Spanish', value: 'es' },
  { label: '法语', englishName: 'French', value: 'fr' },
  { label: '葡萄牙语', englishName: 'Portuguese', value: 'pt' },
  { label: '意大利语', englishName: 'Italian', value: 'it' },
]

const selectedFile = ref(null)
const selectedFilePreviewUrl = ref('')
const sourceLanguage = ref('zh')
const targetLanguage = ref('en')
const isSubmitting = ref(false)
const resultImageUrl = ref('')
const taskId = ref('')
const statusMessage = ref('请选择图片并设置源语言和目标语言。')
const errorMessage = ref('')
const fileInput = ref(null)

let pollingTimer = null

const selectedFileName = computed(() => selectedFile.value?.name || '未选择文件')
const canSubmit = computed(() => {
  return Boolean(selectedFile.value) && Boolean(sourceLanguage.value) && Boolean(targetLanguage.value)
})
const displayImageUrl = computed(() => resultImageUrl.value || selectedFilePreviewUrl.value)
const previewTitle = computed(() => {
  if (resultImageUrl.value) {
    return '翻译结果'
  }
  if (selectedFilePreviewUrl.value) {
    return '上传预览'
  }
  return '等待预览'
})

function triggerFileSelect() {
  if (fileInput.value) {
    fileInput.value.value = ''
  }
  fileInput.value?.click()
}

function clearPreviewUrl() {
  if (selectedFilePreviewUrl.value) {
    URL.revokeObjectURL(selectedFilePreviewUrl.value)
    selectedFilePreviewUrl.value = ''
  }
}

function resetResultState() {
  errorMessage.value = ''
  resultImageUrl.value = ''
  taskId.value = ''
}

function applySelectedFile(file) {
  clearPreviewUrl()
  selectedFile.value = file
  selectedFilePreviewUrl.value = URL.createObjectURL(file)
  resetResultState()
}

function handleFileChange(event) {
  const [file] = event.target.files || []
  if (!file) {
    return
  }

  applySelectedFile(file)
  statusMessage.value = '图片已选择，右侧可预览原图，点击生成开始翻译。'
  event.target.value = ''
}

function handleDrop(event) {
  const [file] = event.dataTransfer?.files || []
  if (!file) {
    return
  }

  applySelectedFile(file)
  statusMessage.value = '图片已拖入，右侧可预览原图，点击生成开始翻译。'
}

function clearPolling() {
  if (pollingTimer) {
    clearTimeout(pollingTimer)
    pollingTimer = null
  }
}

function handleRequestError(error) {
  clearPolling()
  isSubmitting.value = false
  errorMessage.value = error instanceof Error ? error.message : '请求失败，请稍后重试。'
}

async function fetchImageUrl() {
  const payload = await getImageTranslateResult(taskId.value)
  const output = payload?.data?.output
  const taskStatus = output?.task_status

  if (isImageTaskFinished(taskStatus) && output?.image_url) {
    resultImageUrl.value = output.image_url
    isSubmitting.value = false
    statusMessage.value = '翻译完成，右侧已展示结果图片。'
    clearPolling()
    return
  }

  if (taskStatus === 'FAILED' || taskStatus === 'CANCELED') {
    throw new Error(`任务状态异常：${taskStatus}`)
  }

  statusMessage.value = `正在处理中，当前状态：${taskStatus || 'PENDING'}`
  pollingTimer = window.setTimeout(() => {
    fetchImageUrl().catch(handleRequestError)
  }, 1500)
}

async function submitTask() {
  if (!canSubmit.value || isSubmitting.value) {
    return
  }

  clearPolling()
  isSubmitting.value = true
  resetResultState()
  statusMessage.value = '正在提交翻译任务...'

  try {
    const payload = await createImageTranslateTask(
      selectedFile.value,
      sourceLanguage.value,
      targetLanguage.value,
    )
    const receivedTaskId = payload?.data

    if (!receivedTaskId) {
      throw new Error('接口未返回任务 ID。')
    }

    taskId.value = receivedTaskId
    statusMessage.value = '任务已创建，正在获取结果图片...'
    await fetchImageUrl()
  } catch (error) {
    handleRequestError(error)
  }
}

onBeforeUnmount(() => {
  clearPolling()
  clearPreviewUrl()
})
</script>

<template>
  <div class="image-page-shell">
    <section class="image-workspace-card">
      <div class="image-control-panel">
        <div class="image-panel-header">
          <p class="image-eyebrow">图片翻译</p>
          <h1>上传图片并生成翻译结果</h1>
          <p class="image-panel-copy">这里保留核心流程：选择语言、上传图片、生成结果。</p>
        </div>

        <div class="image-field-block">
          <label class="image-field-label" for="source-language">源语言</label>
          <select id="source-language" v-model="sourceLanguage" class="image-field-input">
            <option v-for="language in languages" :key="`source-${language.value}`" :value="language.value">
              {{ language.label }} / {{ language.englishName }} / {{ language.value }}
            </option>
          </select>
        </div>

        <div class="image-field-block">
          <label class="image-field-label" for="target-language">目标语言</label>
          <select id="target-language" v-model="targetLanguage" class="image-field-input">
            <option v-for="language in languages" :key="`target-${language.value}`" :value="language.value">
              {{ language.label }} / {{ language.englishName }} / {{ language.value }}
            </option>
          </select>
        </div>

        <div class="image-upload-block">
          <input
            ref="fileInput"
            class="image-hidden-input"
            type="file"
            accept="image/*"
            @change="handleFileChange"
          />
          <button
            class="image-upload-dropzone"
            type="button"
            @click="triggerFileSelect"
            @dragover.prevent
            @drop.prevent="handleDrop"
          >
            <span class="image-upload-icon">+</span>
            <span class="image-upload-title">点击或拖拽上传图片</span>
            <span class="image-upload-hint">支持 JPG、JPEG、PNG、WebP、BMP 等常见图片格式</span>
          </button>
          <p class="image-file-meta">当前文件：{{ selectedFileName }}</p>
        </div>

        <button
          class="image-submit-button"
          type="button"
          :disabled="!canSubmit || isSubmitting"
          @click="submitTask"
        >
          {{ isSubmitting ? '生成中...' : '生成' }}
        </button>

        <p class="image-status-text" :class="{ error: errorMessage }">
          {{ errorMessage || statusMessage }}
        </p>
      </div>

      <div class="image-preview-panel">
        <div class="image-preview-head">
          <p class="image-eyebrow">{{ previewTitle }}</p>
          <h2>右侧展示上传预览和翻译结果</h2>
        </div>

        <div class="image-preview-stage">
          <img v-if="displayImageUrl" class="image-preview-image" :src="displayImageUrl" alt="图片预览" />
          <div v-else class="image-empty-state">
            <div class="image-empty-icon">IMG</div>
            <p class="image-empty-title">上传后这里会先显示原图预览</p>
            <p class="image-empty-copy">生成成功后，预览图会自动切换为接口返回的翻译结果图片。</p>
          </div>

          <div v-if="isSubmitting" class="image-loading-mask">
            <div class="image-spinner"></div>
            <p>{{ statusMessage }}</p>
          </div>
        </div>

        <div class="image-preview-footer">
          <span class="image-task-pill">Task ID：{{ taskId || '等待生成' }}</span>
          <span class="image-task-pill">目标语言：{{ targetLanguage }}</span>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
:global(:root) {
  color-scheme: light;
}

.image-page-shell {
  min-height: 100vh;
  padding: 32px;
  background:
    radial-gradient(circle at top left, rgba(37, 99, 235, 0.1), transparent 28%),
    linear-gradient(180deg, #f6f9fd 0%, #edf3f9 100%);
}

.image-workspace-card {
  display: grid;
  grid-template-columns: minmax(320px, 420px) minmax(0, 1fr);
  gap: 24px;
  max-width: 1380px;
  margin: 0 auto;
}

.image-control-panel,
.image-preview-panel {
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(126, 148, 176, 0.18);
  border-radius: 28px;
  box-shadow: 0 24px 60px rgba(30, 41, 59, 0.12);
  backdrop-filter: blur(18px);
}

.image-control-panel {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 28px;
}

.image-preview-panel {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 28px;
}

.image-panel-header,
.image-preview-head {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.image-eyebrow {
  margin: 0;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: #2563eb;
  text-transform: uppercase;
}

h1,
h2 {
  margin: 0;
  color: #122033;
  font-size: clamp(28px, 3vw, 40px);
  line-height: 1.1;
  font-weight: 800;
}

.image-panel-copy {
  margin: 0;
  color: #5f7087;
  font-size: 15px;
}

.image-field-block {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.image-field-label {
  color: #122033;
  font-size: 15px;
  font-weight: 700;
}

.image-field-input {
  width: 100%;
  min-height: 52px;
  padding: 0 16px;
  border: 1px solid rgba(126, 148, 176, 0.35);
  border-radius: 16px;
  background: #fff;
  color: #122033;
  font-size: 15px;
  outline: none;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease;
}

.image-field-input:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.12);
}

.image-upload-block {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.image-hidden-input {
  display: none;
}

.image-upload-dropzone {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 30px 20px;
  border: 1px dashed rgba(37, 99, 235, 0.35);
  border-radius: 24px;
  background: linear-gradient(180deg, rgba(37, 99, 235, 0.05), rgba(37, 99, 235, 0.02));
  color: #122033;
  cursor: pointer;
  transition:
    transform 0.2s ease,
    border-color 0.2s ease,
    background 0.2s ease;
}

.image-upload-dropzone:hover {
  transform: translateY(-2px);
  border-color: #2563eb;
  background: linear-gradient(180deg, rgba(37, 99, 235, 0.08), rgba(37, 99, 235, 0.04));
}

.image-upload-icon {
  display: grid;
  place-items: center;
  width: 56px;
  height: 56px;
  border-radius: 18px;
  background: #fff;
  color: #2563eb;
  font-size: 36px;
  line-height: 1;
  box-shadow: 0 16px 32px rgba(37, 99, 235, 0.12);
}

.image-upload-title {
  font-size: 18px;
  font-weight: 700;
}

.image-upload-hint,
.image-file-meta,
.image-status-text {
  margin: 0;
  color: #5f7087;
  font-size: 14px;
}

.image-submit-button {
  min-height: 54px;
  border: none;
  border-radius: 18px;
  background: linear-gradient(135deg, #2563eb, #0f4ad7);
  color: #fff;
  font-size: 16px;
  font-weight: 800;
  cursor: pointer;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    opacity 0.2s ease;
  box-shadow: 0 18px 30px rgba(37, 99, 235, 0.22);
}

.image-submit-button:hover:enabled {
  transform: translateY(-2px);
}

.image-submit-button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  box-shadow: none;
}

.image-status-text {
  min-height: 22px;
}

.image-status-text.error {
  color: #d92d20;
}

.image-preview-stage {
  position: relative;
  flex: 1;
  min-height: 520px;
  overflow: hidden;
  border-radius: 28px;
  background:
    linear-gradient(135deg, rgba(37, 99, 235, 0.06), rgba(15, 74, 215, 0.12)),
    #dce8f6;
}

.image-preview-image,
.image-empty-state {
  width: 100%;
  height: 100%;
  min-height: 520px;
}

.image-preview-image {
  display: block;
  object-fit: contain;
  background: rgba(255, 255, 255, 0.78);
}

.image-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  padding: 24px;
  text-align: center;
}

.image-empty-icon {
  display: grid;
  place-items: center;
  width: 84px;
  height: 84px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.8);
  color: #2563eb;
  font-size: 24px;
  font-weight: 800;
  letter-spacing: 0.08em;
  box-shadow: 0 18px 36px rgba(37, 99, 235, 0.16);
}

.image-empty-title {
  margin: 0;
  color: #122033;
  font-size: 22px;
  font-weight: 800;
}

.image-empty-copy {
  margin: 0;
  max-width: 420px;
  color: #5f7087;
  font-size: 15px;
}

.image-loading-mask {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 18px;
  background: rgba(10, 19, 35, 0.42);
  color: #fff;
  text-align: center;
  padding: 24px;
}

.image-spinner {
  width: 54px;
  height: 54px;
  border: 4px solid rgba(255, 255, 255, 0.28);
  border-top-color: #fff;
  border-radius: 50%;
  animation: image-spin 1s linear infinite;
}

.image-preview-footer {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.image-task-pill {
  padding: 10px 14px;
  border-radius: 999px;
  background: rgba(18, 32, 51, 0.06);
  color: #122033;
  font-size: 13px;
  font-weight: 700;
}

@keyframes image-spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 960px) {
  .image-page-shell {
    padding: 16px;
  }

  .image-workspace-card {
    grid-template-columns: 1fr;
  }

  .image-control-panel,
  .image-preview-panel {
    padding: 22px;
    border-radius: 24px;
  }

  .image-preview-stage,
  .image-preview-image,
  .image-empty-state {
    min-height: 360px;
  }
}
</style>

