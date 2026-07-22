import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/utils/request'

export interface UserInfo {
  userId: number
  username: string
  realName: string
  role: string
  branchId: number | null
  branchName: string | null
  menus: MenuItem[]
}

export interface MenuItem {
  name: string
  path: string
  icon: string
  children?: MenuItem[]
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref<UserInfo | null>(null)

  async function login(username: string, password: string) {
    const res = await request.post('/auth/login', { username, password })
    const data = res.data
    token.value = data.token
    localStorage.setItem('token', data.token)
    userInfo.value = data
    return data
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.clear()
  }

  return { token, userInfo, login, logout }
})
