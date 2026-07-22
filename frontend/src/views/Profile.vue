<template>
  <div>
    <el-card v-loading="loading" style="max-width:600px">
      <template #header>个人中心</template>
      <el-descriptions v-if="member" :column="1" border>
        <el-descriptions-item label="学号">{{ member.studentNo }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ member.name }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ member.gender }}</el-descriptions-item>
        <el-descriptions-item label="学院">{{ member.college }}</el-descriptions-item>
        <el-descriptions-item label="年级">{{ member.grade }}</el-descriptions-item>
        <el-descriptions-item label="专业">{{ member.major }}</el-descriptions-item>
        <el-descriptions-item label="班级">{{ member.className }}</el-descriptions-item>
        <el-descriptions-item label="所属支部">{{ authStore.userInfo?.branchName || '未分配' }}</el-descriptions-item>
        <el-descriptions-item label="党员身份">
          <el-tag>{{ identityLabel(member.identity) }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <div style="margin-top:20px">
        <el-button type="primary" @click="showEditDialog">修改联系方式</el-button>
      </div>
    </el-card>

    <el-dialog title="修改联系方式" v-model="dialogVisible" width="400px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="手机号">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const loading = ref(false)
const member = ref<any>(null)
const dialogVisible = ref(false)
const form = reactive({ phone: '', email: '' })

function identityLabel(v: string) {
  const map: Record<string, string> = { ordinary: '普通学生', applicant: '入党申请人', activist: '积极分子', development: '发展对象', probationary: '预备党员', full: '正式党员' }
  return map[v] || v
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await request.get('/user/profile')
    member.value = res.data.member
  } finally {
    loading.value = false
  }
})

function showEditDialog() {
  form.phone = member.value?.phone || ''
  form.email = member.value?.email || ''
  dialogVisible.value = true
}

async function handleSave() {
  await request.put('/user/profile', { phone: form.phone, email: form.email })
  ElMessage.success('修改成功')
  dialogVisible.value = false
  if (member.value) {
    member.value.phone = form.phone
    member.value.email = form.email
  }
}
</script>
