<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-row">
      <el-col :span="6" v-for="(stat, index) in stats" :key="stat.label">
        <div v-if="dataLoaded" class="stat-card animate-slide-up" :style="{ '--accent': stat.color }">
          <div class="stat-icon" :style="{ background: stat.gradient }">
            <el-icon :size="24"><component :is="stat.icon" /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ animatedValues[index] }}</div>
            <div class="stat-label">{{ stat.label }}</div>
          </div>
          <div class="stat-decoration" :style="{ background: stat.gradient }"></div>
          <div class="stat-trend" v-if="stat.trend">
            <el-icon :size="12"><Top /></el-icon> {{ stat.trend }}
          </div>
        </div>
        <div v-else class="skeleton-card animate-slide-up"></div>
      </el-col>
    </el-row>

    <!-- 快捷操作 -->
    <div class="quick-actions animate-slide-up" style="animation-delay: 0.3s">
      <div class="action-item" v-for="action in quickActions" :key="action.label" @click="$router.push(action.path)">
        <div class="action-icon" :style="{ background: action.gradient }">
          <el-icon :size="18"><component :is="action.icon" /></el-icon>
        </div>
        <span>{{ action.label }}</span>
      </div>
    </div>

    <!-- 数据表格 -->
    <el-row :gutter="20" style="margin-top:20px" class="table-row">
      <el-col :span="12">
        <div class="card animate-slide-up" style="animation-delay: 0.35s;height:100%">
          <div class="card-header">
            <h3>最近上传文档</h3>
            <el-button link type="primary" @click="$router.push('/documents')" class="view-all-btn">
              查看全部 <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>
          <div v-if="!dataLoaded" class="skeleton-table">
            <div class="skeleton-row" v-for="i in 4" :key="i"></div>
          </div>
          <el-table v-else :data="recentDocs" size="small" style="width:100%">
            <el-table-column prop="originalName" label="文件名" show-overflow-tooltip>
              <template #default="{ row }">
                <div class="file-name-cell">
                  <div class="file-icon-mini" :class="row.fileType">
                    {{ row.fileType === 'docx' ? 'W' : 'X' }}
                  </div>
                  <span>{{ row.originalName }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="fileType" label="类型" width="80" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="row.fileType === 'docx' ? '' : 'success'" effect="light" round>
                  {{ row.fileType }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="110" align="center">
              <template #default="{ row }">
                <span :class="['parse-status', parseStatusClass(row.parseStatus)]">
                  <el-icon v-if="row.parseStatus === 1" class="is-loading"><Loading /></el-icon>
                  {{ parseStatusText(row.parseStatus) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="上传时间" width="160" />
          </el-table>
          <div v-if="dataLoaded && recentDocs.length === 0" class="empty-mini">
            <el-icon><Document /></el-icon> 暂无文档
          </div>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="card animate-slide-up" style="animation-delay: 0.4s;height:100%">
          <div class="card-header">
            <h3>最近操作日志</h3>
            <el-button link type="primary" @click="$router.push('/logs')" class="view-all-btn">
              查看全部 <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>
          <div v-if="!dataLoaded" class="skeleton-table">
            <div class="skeleton-row" v-for="i in 4" :key="i"></div>
          </div>
          <el-table v-else :data="recentLogs" size="small" style="width:100%">
            <el-table-column prop="username" label="操作人" width="90">
              <template #default="{ row }">
                <div class="user-cell">
                  <el-avatar :size="24" class="mini-avatar">{{ (row.username || 'U').charAt(0) }}</el-avatar>
                  <span>{{ row.username }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="module" label="模块" width="100">
              <template #default="{ row }">
                <el-tag size="small" effect="plain" round>{{ row.module }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="operation" label="操作" show-overflow-tooltip />
            <el-table-column prop="createdAt" label="时间" width="160" />
          </el-table>
          <div v-if="dataLoaded && recentLogs.length === 0" class="empty-mini">
            <el-icon><Notebook /></el-icon> 暂无日志
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getKbList, getDocuments, getLogs } from '../api'

const stats = ref([
  { label: '知识库', value: 0, icon: 'FolderOpened', color: '#4A6CF7', gradient: 'linear-gradient(135deg, #4A6CF7, #6B8AFF)', trend: null },
  { label: '文档总数', value: 0, icon: 'Document', color: '#10B981', gradient: 'linear-gradient(135deg, #10B981, #34D399)', trend: null },
  { label: '知识块', value: 0, icon: 'Files', color: '#F59E0B', gradient: 'linear-gradient(135deg, #F59E0B, #FBBF24)', trend: null },
  { label: '今日操作', value: 0, icon: 'Notebook', color: '#EF4444', gradient: 'linear-gradient(135deg, #EF4444, #F87171)', trend: null }
])
const animatedValues = ref([0, 0, 0, 0])
const recentDocs = ref([])
const recentLogs = ref([])
const dataLoaded = ref(false)

const quickActions = [
  { label: '新建知识库', icon: 'FolderAdd', path: '/knowledge-base', gradient: 'linear-gradient(135deg, #4A6CF7, #6B8AFF)' },
  { label: '上传文档', icon: 'Upload', path: '/documents', gradient: 'linear-gradient(135deg, #10B981, #34D399)' },
  { label: '知识检索', icon: 'Search', path: '/search', gradient: 'linear-gradient(135deg, #8B5CF6, #A78BFA)' },
  { label: '用户管理', icon: 'UserFilled', path: '/users', gradient: 'linear-gradient(135deg, #F59E0B, #FBBF24)' }
]

const parseStatusText = s => ['待解析', '解析中', '已完成', '解析失败'][s] || '未知'
const parseStatusClass = s => ['pending', 'parsing', 'success', 'failed'][s] || ''

const animateNumber = (index, target) => {
  const duration = 1000
  const startTime = Date.now()
  const step = () => {
    const elapsed = Date.now() - startTime
    const progress = Math.min(elapsed / duration, 1)
    const eased = 1 - Math.pow(1 - progress, 3)
    animatedValues.value[index] = Math.round(target * eased)
    if (progress < 1) requestAnimationFrame(step)
  }
  requestAnimationFrame(step)
}

onMounted(async () => {
  try {
    const [kbRes, docRes, logRes] = await Promise.all([
      getKbList({ pageNum: 1, pageSize: 1 }),
      getDocuments({ pageNum: 1, pageSize: 5 }),
      getLogs({ pageNum: 1, pageSize: 5 })
    ])
    stats.value[0].value = kbRes.data?.total || 0
    stats.value[1].value = docRes.data?.total || 0
    recentDocs.value = docRes.data?.records || []
    recentLogs.value = logRes.data?.records || []

    const kbAll = await getKbList({ pageNum: 1, pageSize: 1000 })
    const totalChunks = (kbAll.data?.records || []).reduce((sum, kb) => sum + (kb.chunkCount || 0), 0)
    stats.value[2].value = totalChunks
    stats.value[3].value = logRes.data?.total || 0

    dataLoaded.value = true
    setTimeout(() => {
      stats.value.forEach((s, i) => animateNumber(i, s.value))
    }, 100)
  } catch (e) {
    dataLoaded.value = true
  }
})
</script>

<style lang="scss" scoped>
.stat-card {
  position: relative;
  .stat-decoration {
    position: absolute;
    top: 0;
    right: 0;
    width: 120px;
    height: 120px;
    border-radius: 50%;
    opacity: 0.06;
    transform: translate(30%, -30%);
    transition: all var(--spring);
    pointer-events: none;
  }
  &:hover .stat-decoration {
    opacity: 0.12;
    transform: translate(20%, -20%) scale(1.1);
  }
  .stat-trend {
    position: absolute;
    top: 12px;
    right: 16px;
    font-size: 11px;
    color: var(--success-color);
    display: flex;
    align-items: center;
    gap: 2px;
    font-weight: 600;
  }
}

.quick-actions {
  display: flex;
  gap: 16px;
  margin-top: 20px;

  .action-item {
    flex: 1;
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 16px 20px;
    background: var(--card-bg);
    border: 1px solid var(--border-light);
    border-radius: var(--radius-lg);
    cursor: pointer;
    transition: all var(--spring);
    box-shadow: var(--shadow-sm);

    &:hover {
      transform: translateY(-3px);
      box-shadow: var(--shadow-md);
      border-color: var(--primary-light);
      .action-icon { transform: scale(1.1) rotate(-5deg); }
    }
    &:active { transform: translateY(-1px); }

    .action-icon {
      width: 40px;
      height: 40px;
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      flex-shrink: 0;
      transition: all var(--spring);
    }

    span {
      font-size: 14px;
      font-weight: 600;
      color: var(--text-primary);
    }
  }
}

.skeleton-table {
  padding: 16px 0;
}

.file-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-icon-mini {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: 800;
  color: #fff;
  flex-shrink: 0;
  &.docx { background: linear-gradient(135deg, #4A6CF7, #6B8AFF); }
  &.xlsx, &.xls { background: linear-gradient(135deg, #10B981, #34D399); }
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  .mini-avatar {
    background: var(--primary-gradient);
    color: #fff;
    font-size: 11px;
    font-weight: 600;
    flex-shrink: 0;
  }
}

.view-all-btn {
  transition: all var(--transition);
  &:hover { transform: translateX(3px); }
}

.table-row {
  :deep(.el-col) {
    display: flex;
    flex-direction: column;
  }
}

.empty-mini {
  text-align: center;
  padding: 32px 0;
  color: var(--text-placeholder);
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

:deep(.el-table) {
  .el-table__header th { background: transparent !important; }
}

.table-row {
  :deep(.el-col) {
    display: flex;
    flex-direction: column;
  }
  .card {
    flex: 1;
    display: flex;
    flex-direction: column;
  }
  :deep(.el-table) {
    flex: 1;
  }
}
</style>
