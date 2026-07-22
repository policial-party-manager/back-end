<template>
  <div>
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>党支部管理</span>
          <el-button type="primary" @click="showAddDialog">新增支部</el-button>
        </div>
      </template>
      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="branchName" label="支部名称" width="180" />
        <el-table-column prop="college" label="所属学院" width="180" />
        <el-table-column prop="secretary" label="支部书记" width="120" />
        <el-table-column prop="description" label="描述" />
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button text type="warning" size="small" @click="showEditDialog(row)">编辑</el-button>
            <el-button text type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="支部名称">
          <el-input v-model="form.branchName" />
        </el-form-item>
        <el-form-item label="所属学院">
          <el-input v-model="form.college" />
        </el-form-item>
        <el-form-item label="支部书记">
          <el-input v-model="form.secretary" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" />
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBranchList, addBranch, updateBranch, deleteBranch } from '@/api/branch'

const loading = ref(false)
const list = ref<any[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const form = reactive<any>({})

onMounted(fetchData)

async function fetchData() {
  loading.value = true
  try {
    const res = await getBranchList()
    list.value = res.data
  } finally {
    loading.value = false
  }
}

function showAddDialog() {
  Object.keys(form).forEach(k => delete form[k])
  dialogTitle.value = '新增党支部'
  dialogVisible.value = true
}

function showEditDialog(row: any) {
  Object.assign(form, row)
  dialogTitle.value = '编辑党支部'
  dialogVisible.value = true
}

async function handleSave() {
  if (form.id) {
    await updateBranch(form.id, form)
    ElMessage.success('修改成功')
  } else {
    await addBranch(form)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  fetchData()
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确定要删除该支部吗？', '确认', { type: 'warning' })
  await deleteBranch(id)
  ElMessage.success('删除成功')
  fetchData()
}
</script>
