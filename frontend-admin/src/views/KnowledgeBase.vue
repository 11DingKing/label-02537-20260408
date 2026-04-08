<template>
  <div>
    <div class="card">
      <div class="card-header">
        <h3>知识库管理</h3>
        <el-button type="primary" @click="openDialog()" class="create-btn">
          <el-icon><Plus /></el-icon> 新建知识库
        </el-button>
      </div>
      <div class="search-bar">
        <el-input v-model="keyword" placeholder="搜索知识库名称" clearable style="width:300px" @keyup.enter="loadData" @clear="loadData">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button @click="loadData" :loading="loading">搜索</el-button>
        <transition name="fade-slide">
          <el-tag v-if="keyword" closable @close="keyword = ''; loadData()" effect="light" round>
            搜索: {{ keyword }}
          </el-tag>
        </transition>
      </div>
      <el-table :data="list" v-loading="loading" style="width:100%" :row-class-name="tableRowClass">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="知识库名称" min-width="180">
          <template #default="{ row }">
            <el-link type="primary" :underline="false" @click="$router.push(`/knowledge-base/${row.id}/documents`)" class="kb-link">
              <el-icon class="kb-icon"><FolderOpened /></el-icon>
              {{ row.name }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="desc-text">{{ row.description || '暂无描述' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="docCount" label="文档数" width="90" align="center">
          <template #default="{ row }">
            <span class="count-badge">{{ row.docCount || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="chunkCount" label="知识块" width="90" align="center">
          <template #default="{ row }">
            <span class="count-badge info">{{ row.chunkCount || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row)">
              <el-icon><Edit /></el-icon> 编辑
            </el-button>
            <el-popconfirm title="确定删除该知识库及其所有文档？" @confirm="handleDelete(row.id)" confirm-button-type="danger" :icon="WarningFilled" icon-color="#EF4444">
              <template #reference>
                <el-button link type="danger"><el-icon><Delete /></el-icon> 删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!loading && list.length === 0" class="empty-state">
        <el-icon class="empty-icon"><FolderOpened /></el-icon>
        <p class="empty-text">{{ keyword ? '未找到匹配的知识库' : '还没有知识库，点击上方按钮创建' }}</p>
      </div>
      <el-pagination
        v-if="total > 0"
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        style="margin-top:16px;justify-content:flex-end"
        @change="loadData"
      />
    </div>

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑知识库' : '新建知识库'" width="500px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入知识库名称" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入描述（可选）" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ submitting ? '提交中...' : '确定' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getKbList, createKb, updateKb, deleteKb } from '../api'
import { ElMessage } from 'element-plus'
import { WarningFilled } from '@element-plus/icons-vue'

const list = ref([])
const loading = ref(false)
const keyword = ref('')
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const dialogVisible = ref(false)
const editId = ref(null)
const submitting = ref(false)
const formRef = ref(null)
const form = ref({ name: '', description: '' })
const rules = { name: [{ required: true, message: '请输入名称', trigger: 'blur' }] }

const tableRowClass = ({ rowIndex }) => `animate-row row-${rowIndex}`

const loadData = async () => {
  loading.value = true
  try {
    const res = await getKbList({ pageNum: pageNum.value, pageSize: pageSize.value, keyword: keyword.value })
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}

const openDialog = (row) => {
  editId.value = row?.id || null
  form.value = row ? { name: row.name, description: row.description } : { name: '', description: '' }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (editId.value) {
      await updateKb(editId.value, form.value)
      ElMessage.success('知识库更新成功')
    } else {
      await createKb(form.value)
      ElMessage.success('知识库创建成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (e) { /* handled */ } finally { submitting.value = false }
}

const handleDelete = async id => {
  try {
    await deleteKb(id)
    ElMessage.success('知识库已删除')
    loadData()
  } catch (e) { /* handled */ }
}

onMounted(loadData)
</script>

<style lang="scss" scoped>
.create-btn {
  &:hover .el-icon { transform: rotate(90deg); }
  .el-icon { transition: transform var(--spring); }
}

:deep(.el-table) .el-button.is-link,
:deep(.el-table) .el-button--text {
  &:hover { transform: none !important; background: transparent !important; }
}

.kb-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
  transition: all var(--transition);
  .kb-icon { font-size: 16px; transition: transform var(--spring); }
  &:hover {
    transform: translateX(2px);
    .kb-icon { transform: scale(1.15); }
  }
}

.desc-text {
  color: var(--text-secondary);
  font-size: 13px;
}

.count-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 32px;
  height: 24px;
  padding: 0 8px;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 600;
  background: var(--primary-bg);
  color: var(--primary-color);
  transition: all var(--transition);
  &.info {
    background: var(--warning-bg);
    color: var(--warning-color);
  }
}

.animate-row {
  animation: slideUp 0.3s ease-out both;
  @for $i from 0 through 20 {
    &.row-#{$i} { animation-delay: #{$i * 0.03}s; }
  }
}
</style>
