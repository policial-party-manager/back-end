import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  response => {
    const data = response.data
    if (data.code === 200) {
      return data
    } else if (data.code === 401) {
      localStorage.clear()
      router.push('/login')
      return Promise.reject(new Error(data.message))
    } else {
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(new Error(data.message))
    }
  },
  error => {
    if (error.response?.status === 401 || error.response?.status === 403) {
      localStorage.clear()
      router.push('/login')
    }
    ElMessage.error('网络错误')
    return Promise.reject(error)
  }
)

export default request
