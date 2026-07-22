<template>
  <div>
    <h3>欢迎回来，{{ authStore.userInfo?.realName }}</h3>
    <el-row :gutter="16" style="margin-top:20px">
      <el-col :span="8">
        <el-card shadow="hover" style="text-align:center">
          <div style="color:#999">当前角色</div>
          <div style="font-size:28px;color:#409EFF;margin-top:8px">{{ roleLabel }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" style="text-align:center">
          <div style="color:#999">所属支部</div>
          <div style="font-size:28px;color:#67C23A;margin-top:8px">{{ authStore.userInfo?.branchName || '未分配' }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" style="text-align:center">
          <div style="color:#999">用户名</div>
          <div style="font-size:28px;color:#E6A23C;margin-top:8px">{{ authStore.userInfo?.username }}</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

const roleLabel = computed(() => {
  const map: Record<string, string> = { super_admin: '超级管理员', branch_admin: '支部管理员', student: '普通成员' }
  return map[authStore.userInfo?.role || ''] || ''
})
</script>
