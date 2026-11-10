const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''

async function request(path, options = {}) {
  const headers = { ...(options.headers || {}) }
  const isFormData = options.body instanceof FormData

  if (!isFormData && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json'
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: 'GET',
    ...options,
    headers,
  })

  if (!response.ok) {
    throw new Error(`接口请求失败：${response.status}`)
  }

  const result = await response.json()

  if (result?.code && result.code !== '200') {
    throw new Error(result?.message || '接口返回异常')
  }

  return result
}

export function createImageTranslateTask(file, sourceLanguage, targetLanguage) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('sourceLanguage', sourceLanguage)
  formData.append('targetLanguage', targetLanguage)

  return request('/api/v1/image/translate/task', {
    method: 'POST',
    body: formData,
  })
}

export function getImageTranslateResult(taskId) {
  const searchParams = new URLSearchParams({ taskId })
  return request(`/api/v1/image/translate/result?${searchParams.toString()}`)
}

export function isImageTaskFinished(taskStatus) {
  return taskStatus === 'SUCCEEDED'
}

