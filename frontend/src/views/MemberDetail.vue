<template>
  <div>
    <el-page-header @back="$router.push('/members')" title="返回成员列表" />
    <el-card v-loading="loading" style="margin-top:16px">
      <template #header>成员详情</template>
      <el-descriptions :column="2" border v-if="member">
        <el-descriptions-item label="学号">{{ member.studentNo }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ member.name }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ member.gender }}</el-descriptions-item>
        <el-descriptions-item label="学院">{{ member.college }}</el-descriptions-item>
        <el-descriptions-item label="年级">{{ member.grade }}</el-descriptions-item>
        <el-descriptions-item label="专业">{{ member.major }}</el-descriptions-item>
        <el-descriptions-item label="班级">{{ member.className }}</el-descriptions-item>
        <el-descriptions-item label="党员身份">
          <el-tag>{{ identityLabel(member.identity) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="手机号">{{ member.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ member.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ member.createTime }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getMemberById } from '@/api/member'

const route = useRoute()
const loading = ref(false)
const member = ref<any>(null)

function identityLabel(v: string) {
  const map: Record<string, string> = { ordinary: '普通学生', applicant: '入党申请人', activist: '积极分子', development: '发展对象', probationary: '预备党员', full: '正式党员' }
  return map[v] || v
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await getMemberById(Number(route.params.id))
    member.value = res.data
  } finally {
    loading.value = false
  }
})
</script>
