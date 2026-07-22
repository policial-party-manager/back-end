<template>
  <el-container style="height:100vh">
    <el-aside width="220px" style="background:#304156">
      <div style="color:#fff;text-align:center;padding:16px;font-size:18px;font-weight:bold">
        🏛️ 党建云平台
      </div>
      <el-menu
        :default-active="route.path"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        router
        style="border-right:none"
      >
        <template v-for="menu in menus" :key="menu.path">
          <el-menu-item v-if="!menu.children" :index="menu.path">
            <el-icon><component :is="menu.icon" /></el-icon>
            <span>{{ menu.name }}</span>
          </el-menu-item>
          <el-sub-menu v-else :index="menu.path">
            <template #title>
              <el-icon><component :is="menu.icon" /></el-icon>
              <span>{{ menu.name }}</span>
            </template>
            <el-menu-item v-for="child in menu.children" :key="child.path" :index="child.path">
              {{ child.name }}
            </el-menu-item>
          </el-sub-menu>
        </template>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header style="background:#fff;display:flex;justify-content:space-between;align-items:center;border-bottom:1px solid #e6e6e6">
        <span style="font-size:16px">{{ currentTitle }}</span>
        <div>
          <span style="margin-right:12px;color:#666">{{ authStore.userInfo?.realName }}</span>
          <el-tag :type="roleTag" size="small">{{ roleLabel }}</el-tag>
          <el-button text style="margin-left:12px" @click="handleLogout">退出</el-button>
        </div>
      </el-header>
      <el-main style="background:#f0f2f5">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore, type MenuItem } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const menus = computed<MenuItem[]>(() => authStore.userInfo?.menus || [])

const currentTitle = computed(() => {
  const menu = menus.value.find(m => m.path === route.path || m.children?.some(c => c.path === route.path))
  return menu?.name || '党建云平台'
})

const roleLabel = computed(() => {
  const map: Record<string, string> = { super_admin: '超级管理员', branch_admin: '支部管理员', student: '普通成员' }
  return map[authStore.userInfo?.role || ''] || ''
})

const roleTag = computed(() => {
  const map: Record<string, string> = { super_admin: 'danger', branch_admin: 'warning', student: 'info' }
  return map[authStore.userInfo?.role || ''] || 'info'
})

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>
