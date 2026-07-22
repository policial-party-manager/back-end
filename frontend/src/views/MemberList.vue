<template>
  <div>
    <!-- 搜索栏 -->
    <el-card style="margin-bottom:16px">
      <el-form :inline="true" :model="query">
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="姓名/学号" clearable />
        </el-form-item>
        <el-form-item label="学院">
          <el-input v-model="query.college" placeholder="学院" clearable />
        </el-form-item>
        <el-form-item label="身份">
          <el-select v-model="query.identity" placeholder="全部" clearable style="width:130px">
            <el-option label="普通学生" value="ordinary" />
            <el-option label="入党申请人" value="applicant" />
            <el-option label="积极分子" value="activist" />
            <el-option label="发展对象" value="development" />
            <el-option label="预备党员" value="probationary" />
            <el-option label="正式党员" value="full" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button v-if="canEdit" type="success" @click="showAddDialog">新增成员</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据表格 -->
    <el-card>
      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="studentNo" label="学号" width="120" />
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="gender" label="性别" width="70" />
        <el-table-column prop="college" label="学院" width="140" />
        <el-table-column prop="grade" label="年级" width="90" />
        <el-table-column prop="major" label="专业" width="150" />
        <el-table-column prop="className" label="班级" width="140" />
        <el-table-column label="党员身份" width="110">
          <template #default="{ row }">
            <el-tag :type="identityType(row.identity)">{{ identityLabel(row.identity) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="120" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="$router.push(`/members/${row.id}`)">详情</el-button>
            <template v-if="canEdit">
              <el-button text type="warning" size="small" @click="showEditDialog(row)">编辑</el-button>
              <el-button text type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        layout="total, prev, pager, next"
        style="margin-top:16px;justify-content:flex-end"
        @change="fetchData"
      />
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="学号">
          <el-input v-model="form.studentNo" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="性别">
          <el-select v-model="form.gender">
            <el-option label="男" value="男" />
            <el-option label="女" value="女" />
          </el-select>
        </el-form-item>
        <el-form-item label="学院">
          <el-input v-model="form.college" />
        </el-form-item>
        <el-form-item label="年级">
          <el-input v-model="form.grade" placeholder="如2024级" />
        </el-form-item>
        <el-form-item label="专业">
          <el-input v-model="form.major" />
        </el-form-item>
        <el-form-item label="班级">
          <el-input v-model="form.className" />
        </el-form-item>
        <el-form-item label="党员身份">
          <el-select v-model="form.identity">
            <el-option v-for="item in identityOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
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
import { reactive, ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMemberList, addMember, updateMember, deleteMember } from '@/api/member'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const canEdit = computed(() => authStore.userInfo?.role !== 'student')

const loading = ref(false)
const total = ref(0)
const tableData = ref<any[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const query = reactive({ page: 1, size: 10, keyword: '', college: '', identity: '' })
const form = reactive<any>({})

const identityOptions = [
  { value: 'ordinary', label: '普通学生' },
  { value: 'applicant', label: '入党申请人' },
  { value: 'activist', label: '积极分子' },
  { value: 'development', label: '发展对象' },
  { value: 'probationary', label: '预备党员' },
  { value: 'full', label: '正式党员' }
]

function identityLabel(v: string) {
  return identityOptions.find(o => o.value === v)?.label || v
}

function identityType(v: string) {
  const map: Record<string, string> = { ordinary: 'info', applicant: '', activist: 'warning', development: 'warning', probationary: 'success', full: 'danger' }
  return map[v] || 'info'
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getMemberList(query)
    const data = res.data
    total.value = data.total
    tableData.value = data.rows
  } finally {
    loading.value = false
  }
}

function showAddDialog() {
  Object.keys(form).forEach(k => delete form[k])
  form.identity = 'ordinary'
  dialogTitle.value = '新增成员'
  dialogVisible.value = true
}

function showEditDialog(row: any) {
  Object.assign(form, row)
  dialogTitle.value = '编辑成员'
  dialogVisible.value = true
}

async function handleSave() {
  if (form.id) {
    await updateMember(form.id, form)
    ElMessage.success('修改成功')
  } else {
    await addMember(form)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  fetchData()
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确定要删除该成员吗？', '确认', { type: 'warning' })
  await deleteMember(id)
  ElMessage.success('删除成功')
  fetchData()
}

fetchData()
</script>
