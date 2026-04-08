<template>
  <div>
    <div class="card">
      <div class="card-header">
        <h3>用户管理</h3>
        <el-button type="primary" @click="openDialog()" class="create-btn">
          <el-icon><Plus /></el-icon> 新增用户
        </el-button>
      </div>

      <!-- 用户统计 -->
      <transition name="fade-slide">
        <div v-if="list.length > 0" class="user-stats">
          <div class="user-stat-item">
            <div class="stat-mini-icon active"><el-icon><UserFilled /></el-icon></div>
            <div class="stat-mini-info">
              <span class="stat-mini-value">{{ activeCount }}</span>
              <span class="stat-mini-label">活跃用户</span>
            </div>
          </div>
          <div class="stat-divider"></div>
          <div class="user-stat-item">
            <div class="stat-mini-icon disabled"><el-icon><UserFilled /></el-icon></div>
            <div class="stat-mini-info">
              <span class="stat-mini-value">{{ disabledCount }}</span>
              <span class="stat-mini-label">已禁用</span>
            </div>
          </div>
          <div class="stat-divider"></div>
          <div class="user-stat-item">
            <div class="stat-mini-icon total"><el-icon><User /></el-icon></div>
            <div class="stat-mini-info">
              <span class="stat-mini-value">{{ total }}</span>
              <span class="stat-mini-label">总用户数</span>
            </div>
          </div>
        </div>
      </transition>

      <div class="search-bar">
        <el-input v-model="keyword" placeholder="搜索用户名/昵称" clearable style="width:300px" @keyup.enter="loadData" @clear="loadData">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button @click="loadData" :loading="loading">搜索</el-button>
        <transition name="fade-slide">
          <el-tag v-if="keyword" closable @close="keyword = ''; loadData()" effect="light" round>
            搜索: {{ keyword }}
          </el-tag>
        </transition>
      </div>

      <!-- 骨架屏 -->
      <div v-if="loading && list.length === 0" class="skeleton-table">
        <div class="skeleton-row" v-for="i in 4" :key="i"></div>
      </div>

      <el-table v-else :data="list" v-loading="loading" :row-class-name="tableRowClass" table-layout="auto">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" min-width="180">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="36" class="user-avatar-sm" :style="{ background: avatarGradient(row.username) }">
                {{ (row.nickname || row.username || 'U').charAt(0) }}
              </el-avatar>
              <div class="user-name-group">
                <div class="user-name-row">
                  <span class="user-name">{{ row.username }}</span>
                  <span v-if="row.id === 1" class="admin-badge">
                    <el-icon :size="10"><Star /></el-icon> 超管
                  </span>
                </div>
                <span class="user-nickname">{{ row.nickname || '未设置昵称' }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="nickname" label="昵称" min-width="100">
          <template #default="{ row }">
            {{ row.nickname || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              @change="val => handleStatus(row.id, val ? 1 : 0)"
              :disabled="row.id === 1"
              active-text="启用"
              inactive-text="禁用"
              inline-prompt
              style="--el-switch-on-color: #10B981; --el-switch-off-color: #EF4444"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170">
          <template #default="{ row }">
            <span class="time-text">{{ row.createdAt }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row)" class="action-btn">
              <el-icon><Edit /></el-icon> 编辑
            </el-button>
            <el-popconfirm title="确定删除该用户？此操作不可恢复" @confirm="handleDelete(row.id)" confirm-button-type="danger" :icon="WarningFilled" icon-color="#EF4444">
              <template #reference>
                <el-button link type="danger" :disabled="row.id === 1" class="action-btn">
                  <el-icon><Delete /></el-icon> 删除
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!loading && list.length === 0" class="empty-state">
        <div class="empty-illustration">
          <el-icon class="empty-icon"><UserFilled /></el-icon>
          <div class="empty-circle"></div>
        </div>
        <p class="empty-text">{{ keyword ? '未找到匹配的用户' : '暂无用户数据' }}</p>
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

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑用户' : '新增用户'" width="480px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="!!editId" placeholder="请输入用户名">
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item label="密码" :prop="editId ? '' : 'password'">
          <el-input v-model="form.password" type="password" :placeholder="editId ? '留空则不修改' : '请输入密码'" show-password>
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" placeholder="请输入昵称">
            <template #prefix><el-icon><EditPen /></el-icon></template>
          </el-input>
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
import { ref, computed, onMounted } from 'vue'
import { getUsers, createUser, updateUser, deleteUser, updateUserStatus } from '../api'
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
const form = ref({ username: '', password: '', nickname: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const activeCount = computed(() => list.value.filter(u => u.status === 1).length)
const disabledCount = computed(() => list.value.filter(u => u.status !== 1).length)

const gradients = [
  'linear-gradient(135deg, #4A6CF7, #6B8AFF)',
  'linear-gradient(135deg, #10B981, #34D399)',
  'linear-gradient(135deg, #F59E0B, #FBBF24)',
  'linear-gradient(135deg, #8B5CF6, #A78BFA)',
  'linear-gradient(135deg, #EF4444, #F87171)',
  'linear-gradient(135deg, #06B6D4, #22D3EE)',
]
const avatarGradient = (name) => {
  const hash = (name || '').split('').reduce((a, c) => a + c.charCodeAt(0), 0)
  return gradients[hash % gradients.length]
}

const tableRowClass = ({ rowIndex }) => `animate-row row-${rowIndex}`

const loadData = async () => {
  loading.value = true
  try {
    const res = await getUsers({ pageNum: pageNum.value, pageSize: pageSize.value, keyword: keyword.value })
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}

const openDialog = row => {
  editId.value = row?.id || null
  form.value = row ? { username: row.username, password: '', nickname: row.nickname } : { username: '', password: '', nickname: '' }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (editId.value) {
      await updateUser(editId.value, form.value)
      ElMessage.success('用户更新成功')
    } else {
      await createUser(form.value)
      ElMessage.success('用户创建成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.error('操作失败，请重试')
  } finally { submitting.value = false }
}

const handleDelete = async id => {
  try {
    await deleteUser(id)
    ElMessage.success('用户已删除')
    loadData()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

const handleStatus = async (id, status) => {
  try {
    await updateUserStatus(id, status)
    ElMessage.success(status === 1 ? '用户已启用' : '用户已禁用')
    loadData()
  } catch (e) {
    ElMessage.error('状态更新失败')
  }
}

onMounted(loadData)
</script>

<style lang="scss" scoped>
.create-btn {
  &:hover .el-icon { transform: rotate(90deg); }
  .el-icon { transition: transform var(--spring); }
}

.user-stats {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 16px;
  padding: 14px 20px;
  background: var(--bg-secondary);
  border-radius: var(--radius);
}

.user-stat-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.stat-mini-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  color: #fff;
  &.active { background: linear-gradient(135deg, #10B981, #34D399); }
  &.disabled { background: linear-gradient(135deg, #EF4444, #F87171); }
  &.total { background: linear-gradient(135deg, #4A6CF7, #6B8AFF); }
}

.stat-mini-info {
  display: flex;
  flex-direction: column;
  .stat-mini-value { font-size: 18px; font-weight: 700; color: var(--text-primary); line-height: 1.2; }
  .stat-mini-label { font-size: 12px; color: var(--text-secondary); }
}

.stat-divider {
  width: 1px;
  height: 32px;
  background: var(--border-color);
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-avatar-sm {
  color: #fff;
  font-weight: 600;
  font-size: 14px;
  flex-shrink: 0;
  transition: all var(--spring);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.user-name-group {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.user-name-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.user-name {
  font-weight: 600;
  font-size: 14px;
  color: var(--text-primary);
}

.admin-badge {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: 10px;
  color: #D97706;
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.12), rgba(251, 191, 36, 0.12));
  padding: 1px 7px;
  border-radius: 4px;
  font-weight: 600;
  line-height: 16px;
  white-space: nowrap;
}

.user-nickname {
  font-size: 12px;
  color: var(--text-placeholder);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.time-text {
  font-size: 13px;
  color: var(--text-secondary);
}

.action-btn {
  &:hover { transform: none !important; background: transparent !important; }
}

.animate-row {
  animation: slideUp 0.3s ease-out both;
  @for $i from 0 through 20 {
    &.row-#{$i} { animation-delay: #{$i * 0.03}s; }
  }
}

.skeleton-table { padding: 16px 0; }

.empty-illustration {
  position: relative;
  width: 80px;
  height: 80px;
  margin: 0 auto 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  .empty-icon {
    font-size: 40px;
    color: var(--text-placeholder);
    position: relative;
    z-index: 1;
  }
  .empty-circle {
    position: absolute;
    width: 80px;
    height: 80px;
    border-radius: 50%;
    background: var(--bg-secondary);
    animation: breathe 3s ease-in-out infinite;
  }
}
</style>
