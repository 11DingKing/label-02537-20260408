<template>
  <div>
    <div class="card">
      <div class="card-header">
        <h3>操作日志</h3>
        <div class="header-actions">
          <el-tag effect="plain" round class="total-tag">
            <el-icon><Notebook /></el-icon> 共 {{ total }} 条记录
          </el-tag>
        </div>
      </div>

      <!-- 日志统计 -->
      <transition name="fade-slide">
        <div v-if="list.length > 0" class="log-stats">
          <div class="log-stat-chip success">
            <el-icon><CircleCheckFilled /></el-icon>
            <span>成功 {{ successCount }}</span>
          </div>
          <div class="log-stat-chip fail">
            <el-icon><CircleCloseFilled /></el-icon>
            <span>失败 {{ failCount }}</span>
          </div>
          <div class="log-stat-chip" v-for="m in topModules" :key="m.name" :class="moduleClass(m.name)">
            <el-icon><component :is="moduleIcon(m.name)" /></el-icon>
            <span>{{ m.name }} {{ m.count }}</span>
          </div>
        </div>
      </transition>

      <div class="search-bar">
        <el-input v-model="keyword" placeholder="搜索模块/操作/用户" clearable style="width:300px" @keyup.enter="loadData" @clear="loadData">
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
        <div class="skeleton-row" v-for="i in 6" :key="i"></div>
      </div>

      <el-table v-else :data="list" v-loading="loading" style="width:100%" :row-class-name="tableRowClass">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="操作人" width="140">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="30" class="log-avatar" :style="{ background: avatarGradient(row.username) }">
                {{ (row.username || 'U').charAt(0) }}
              </el-avatar>
              <span class="log-username">{{ row.username }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="module" label="模块" width="130">
          <template #default="{ row }">
            <el-tag size="small" :type="moduleTagType(row.module)" effect="plain" round class="module-tag">
              {{ row.module }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operation" label="操作" width="150">
          <template #default="{ row }">
            <span class="operation-text">{{ row.operation }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="ip" label="IP" width="140">
          <template #default="{ row }">
            <span class="ip-text">
              <el-icon :size="12" class="ip-icon"><Monitor /></el-icon>
              {{ row.ip }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <div class="status-badge" :class="row.status === 1 ? 'success' : 'fail'">
              <span class="status-dot"></span>
              {{ row.status === 1 ? '成功' : '失败' }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="duration" label="耗时" width="100" align="center">
          <template #default="{ row }">
            <span class="duration-text" :class="durationClass(row.duration)">
              <el-icon :size="11" class="duration-icon"><Timer /></el-icon>
              {{ row.duration }}ms
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="操作时间" width="170">
          <template #default="{ row }">
            <span class="time-text">{{ row.createdAt }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="errorMsg" label="错误信息" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.errorMsg" class="error-text">
              <el-icon style="margin-right:4px"><CircleCloseFilled /></el-icon>
              {{ row.errorMsg }}
            </span>
            <span v-else class="no-error">-</span>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!loading && list.length === 0" class="empty-state">
        <div class="empty-illustration">
          <el-icon class="empty-icon"><Notebook /></el-icon>
          <div class="empty-circle"></div>
        </div>
        <p class="empty-text">{{ keyword ? '未找到匹配的日志' : '暂无操作日志' }}</p>
      </div>

      <el-pagination
        v-if="total > 0"
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        style="margin-top:16px;justify-content:flex-end"
        @change="loadData"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getLogs } from '../api'

const list = ref([])
const loading = ref(false)
const keyword = ref('')
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)

const successCount = computed(() => list.value.filter(l => l.status === 1).length)
const failCount = computed(() => list.value.filter(l => l.status !== 1).length)
const topModules = computed(() => {
  const map = {}
  list.value.forEach(l => { map[l.module] = (map[l.module] || 0) + 1 })
  return Object.entries(map).map(([name, count]) => ({ name, count })).sort((a, b) => b.count - a.count).slice(0, 4)
})

const tableRowClass = ({ rowIndex }) => `animate-row row-${rowIndex}`

const moduleTagType = (module) => {
  const map = { '知识库': '', '文档': 'success', '用户': 'warning', '认证': 'danger' }
  return map[module] || 'info'
}

const moduleIcon = (module) => {
  const map = { '知识库': 'FolderOpened', '文档': 'Document', '用户': 'UserFilled', '认证': 'Lock' }
  return map[module] || 'Notebook'
}

const moduleClass = (module) => {
  const map = { '知识库': 'primary', '文档': 'green', '用户': 'orange', '认证': 'red' }
  return map[module] || 'default'
}

const durationClass = (duration) => {
  if (duration > 1000) return 'slow'
  if (duration > 500) return 'medium'
  return 'fast'
}

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

const loadData = async () => {
  loading.value = true
  try {
    const res = await getLogs({ pageNum: pageNum.value, pageSize: pageSize.value, keyword: keyword.value })
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}

onMounted(loadData)
</script>

<style lang="scss" scoped>
.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.total-tag {
  font-weight: 500;
  .el-icon { margin-right: 4px; }
}

.log-stats {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.log-stat-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 5px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  transition: all var(--transition);
  cursor: default;
  &:hover { transform: translateY(-1px); }

  &.success { color: var(--success-color); background: var(--success-bg); }
  &.fail { color: var(--danger-color); background: var(--danger-bg); }
  &.primary { color: var(--primary-color); background: var(--primary-bg); }
  &.green { color: var(--success-color); background: var(--success-bg); }
  &.orange { color: var(--warning-color); background: var(--warning-bg); }
  &.red { color: var(--danger-color); background: var(--danger-bg); }
  &.default { color: var(--info-color); background: var(--info-bg); }
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
.log-avatar {
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  flex-shrink: 0;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
}
.log-username {
  font-weight: 500;
  font-size: 13px;
}

.module-tag {
  white-space: nowrap !important;
  display: inline-flex !important;
  align-items: center !important;
  flex-wrap: nowrap !important;
  transition: all var(--transition);
  :deep(.el-icon) {
    display: inline-flex;
    flex-shrink: 0;
  }
  &:hover { transform: scale(1.05); }
}

.operation-text {
  font-size: 13px;
  color: var(--text-regular);
}

.ip-text {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-family: 'SF Mono', 'Fira Code', monospace;
  font-size: 12px;
  color: var(--text-secondary);
  background: var(--bg-secondary);
  padding: 3px 8px;
  border-radius: 6px;
  transition: all var(--transition);
  &:hover { background: var(--primary-bg); color: var(--primary-color); }
  .ip-icon { opacity: 0.6; }
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  padding: 3px 12px;
  border-radius: 20px;
  transition: all var(--transition);

  .status-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    transition: all var(--transition);
  }

  &.success {
    color: var(--success-color);
    background: var(--success-bg);
    .status-dot { background: var(--success-color); box-shadow: 0 0 6px rgba(16, 185, 129, 0.4); }
  }
  &.fail {
    color: var(--danger-color);
    background: var(--danger-bg);
    .status-dot { background: var(--danger-color); box-shadow: 0 0 6px rgba(239, 68, 68, 0.4); }
  }
}

.duration-text {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-family: 'SF Mono', 'Fira Code', monospace;
  font-size: 12px;
  padding: 3px 8px;
  border-radius: 6px;
  transition: all var(--transition);
  .duration-icon { opacity: 0.7; }
  &.fast {
    color: var(--success-color);
    background: var(--success-bg);
  }
  &.medium {
    color: var(--warning-color);
    background: var(--warning-bg);
  }
  &.slow {
    color: var(--danger-color);
    background: var(--danger-bg);
    font-weight: 600;
  }
}

.time-text {
  font-size: 13px;
  color: var(--text-secondary);
}

.error-text {
  color: var(--danger-color);
  font-size: 13px;
  display: inline-flex;
  align-items: center;
}
.no-error {
  color: var(--text-placeholder);
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
