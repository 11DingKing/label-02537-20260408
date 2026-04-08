<template>
  <div>
    <div class="card">
      <div class="card-header">
        <h3>
          <el-icon class="header-icon"><Document /></el-icon>
          {{ kbId ? '文档管理' : '全部文档' }}
        </h3>
        <div class="header-actions">
          <el-select v-if="!kbId" v-model="selectedKbId" placeholder="选择知识库上传" style="width:200px" clearable>
            <template #prefix><el-icon><FolderOpened /></el-icon></template>
            <el-option v-for="kb in kbOptions" :key="kb.id" :label="kb.name" :value="kb.id" />
          </el-select>
          <el-upload
            :show-file-list="false"
            :before-upload="beforeUpload"
            :http-request="handleUpload"
            accept=".doc,.docx,.xlsx,.xls"
            :disabled="!uploadKbId"
          >
            <el-button type="primary" :disabled="!uploadKbId" :loading="uploading" class="upload-btn">
              <el-icon><Upload /></el-icon> 上传文档
            </el-button>
          </el-upload>
        </div>
      </div>

      <!-- 文件类型统计 -->
      <transition name="fade-slide">
        <div v-if="list.length > 0" class="doc-stats">
          <div class="doc-stat-chip">
            <span class="chip-dot docx"></span>
            <span class="chip-label">Word</span>
            <span class="chip-count">{{ docxCount }}</span>
          </div>
          <div class="doc-stat-chip">
            <span class="chip-dot xlsx"></span>
            <span class="chip-label">Excel</span>
            <span class="chip-count">{{ xlsxCount }}</span>
          </div>
          <div class="doc-stat-chip">
            <span class="chip-dot success"></span>
            <span class="chip-label">已解析</span>
            <span class="chip-count">{{ parsedCount }}</span>
          </div>
          <div class="doc-stat-chip">
            <span class="chip-dot pending"></span>
            <span class="chip-label">待处理</span>
            <span class="chip-count">{{ pendingCount }}</span>
          </div>
        </div>
      </transition>

      <div class="search-bar">
        <el-input v-model="keyword" placeholder="搜索文件名" clearable style="width:300px" @keyup.enter="loadData" @clear="loadData">
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
        <div class="skeleton-row" v-for="i in 5" :key="i"></div>
      </div>

      <el-table v-else :data="list" v-loading="loading" style="width:100%" :row-class-name="tableRowClass">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="originalName" label="文件名" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="file-cell">
              <div class="file-type-icon" :class="row.fileType">
                <span class="file-type-letter">{{ ['doc', 'docx'].includes(row.fileType) ? 'W' : 'X' }}</span>
              </div>
              <div class="file-info">
                <span class="file-name">{{ row.originalName }}</span>
                <span class="file-size">{{ formatSize(row.fileSize) }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="fileType" label="类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.fileType === 'docx' ? '' : 'success'" effect="light" round>
              {{ row.fileType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="解析状态" width="130" align="center">
          <template #default="{ row }">
            <el-tooltip v-if="row.parseStatus === 3 && row.parseMessage" :content="row.parseMessage" placement="top">
              <span :class="['parse-status', 'failed']">
                <el-icon><CircleCloseFilled /></el-icon>
                解析失败
              </span>
            </el-tooltip>
            <span v-else :class="['parse-status', parseStatusClass(row.parseStatus)]">
              <el-icon v-if="row.parseStatus === 1" class="is-loading"><Loading /></el-icon>
              <el-icon v-else-if="row.parseStatus === 2"><CircleCheckFilled /></el-icon>
              <el-icon v-else-if="row.parseStatus === 3"><CircleCloseFilled /></el-icon>
              <span class="parse-dot" v-else></span>
              {{ parseStatusText(row.parseStatus) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="chunkCount" label="知识块" width="90" align="center">
          <template #default="{ row }">
            <span class="count-badge" :class="{ 'has-data': row.chunkCount > 0 }">
              <el-icon v-if="row.chunkCount > 0" class="count-icon"><Files /></el-icon>
              {{ row.chunkCount || 0 }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="上传时间" width="170">
          <template #default="{ row }">
            <span class="time-text">{{ row.createdAt }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right" class-name="action-col">
          <template #default="{ row }">
            <div class="action-row">
              <el-button link type="primary" @click="viewChunks(row)" class="action-btn" :disabled="row.parseStatus === 1">
                <el-icon><View /></el-icon> 知识块
              </el-button>
              <!-- 解析中：显示 loading -->
              <el-button v-if="row.parseStatus === 1" link type="info" class="action-btn" disabled>
                <el-icon class="is-loading"><Loading /></el-icon> 解析中
              </el-button>
              <!-- 解析失败：重试 -->
              <el-button v-else-if="row.parseStatus === 3" link type="danger" @click="handleReparse(row.id)" class="action-btn">
                <el-icon><RefreshRight /></el-icon> 重试
              </el-button>
              <!-- 待解析：解析 -->
              <el-button v-else-if="row.parseStatus === 0" link type="warning" @click="handleReparse(row.id)" class="action-btn">
                <el-icon><RefreshRight /></el-icon> 解析
              </el-button>
              <!-- 已完成：重新解析 -->
              <el-button v-else link type="info" @click="handleReparse(row.id)" class="action-btn">
                <el-icon><RefreshRight /></el-icon> 重解析
              </el-button>
              <el-popconfirm title="确定删除该文档及其知识块？" @confirm="handleDelete(row.id)" confirm-button-type="danger" :icon="WarningFilled" icon-color="#EF4444">
                <template #reference>
                  <el-button link type="danger" class="action-btn" :disabled="row.parseStatus === 1"><el-icon><Delete /></el-icon> 删除</el-button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!loading && list.length === 0" class="empty-state">
        <div class="empty-illustration">
          <el-icon class="empty-icon"><Document /></el-icon>
          <div class="empty-circle"></div>
        </div>
        <p class="empty-text">{{ keyword ? '未找到匹配的文档' : '还没有文档，点击上方按钮上传' }}</p>
        <p v-if="!keyword" class="empty-hint">支持 .doc .docx .xlsx .xls 格式，单文件最大 50MB</p>
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

    <!-- 上传进度遮罩 -->
    <transition name="fade-slide">
      <div v-if="uploading" class="upload-overlay">
        <div class="upload-progress-card">
          <div class="upload-spinner">
            <el-icon class="upload-spin"><Loading /></el-icon>
          </div>
          <div class="upload-info">
            <span class="upload-title">文档上传中</span>
            <span class="upload-desc">请稍候，正在处理文件...</span>
          </div>
        </div>
      </div>
    </transition>

    <!-- 拖拽上传提示 -->
    <transition name="fade-slide">
      <div v-if="isDragging" class="drag-overlay">
        <div class="drag-content">
          <el-icon :size="48" color="var(--primary-color)"><Upload /></el-icon>
          <p>释放文件以上传</p>
        </div>
      </div>
    </transition>

    <el-dialog v-model="chunkDialogVisible" title="知识块详情" width="720px" destroy-on-close>
      <div class="chunk-summary" v-if="chunks.length > 0">
        <el-icon><Files /></el-icon>
        共 <strong>{{ chunks.length }}</strong> 个知识块
      </div>
      <div v-if="chunks.length === 0" class="empty-state" style="padding:40px 0">
        <el-icon class="empty-icon"><Document /></el-icon>
        <p class="empty-text">暂无知识块</p>
      </div>
      <transition-group name="chunk-list" tag="div">
        <div v-for="(chunk, index) in chunks" :key="chunk.id" class="chunk-item" :style="{ animationDelay: index * 0.04 + 's' }">
          <div class="chunk-header">
            <span class="chunk-index">#{{ chunk.chunkIndex }}</span>
            <el-tag size="small" type="info" effect="plain" round>{{ chunk.sourceInfo }}</el-tag>
          </div>
          <div class="chunk-content">{{ chunk.content }}</div>
        </div>
      </transition-group>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { getDocuments, uploadDocument, deleteDocument, reparseDocument, getDocChunks, getKbList } from '../api'
import { ElMessage } from 'element-plus'
import { WarningFilled } from '@element-plus/icons-vue'

const route = useRoute()
const kbId = computed(() => route.params.id ? Number(route.params.id) : null)
const selectedKbId = ref(null)
const uploadKbId = computed(() => kbId.value || selectedKbId.value)
const kbOptions = ref([])

const list = ref([])
const loading = ref(false)
const uploading = ref(false)
const keyword = ref('')
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const isDragging = ref(false)

const chunkDialogVisible = ref(false)
const chunks = ref([])
let pollTimer = null

// 是否有正在解析的文档
const hasParsing = computed(() => list.value.some(d => d.parseStatus === 0 || d.parseStatus === 1))

// 统计
const docxCount = computed(() => list.value.filter(d => ['doc', 'docx'].includes(d.fileType)).length)
const xlsxCount = computed(() => list.value.filter(d => ['xlsx', 'xls'].includes(d.fileType)).length)
const parsedCount = computed(() => list.value.filter(d => d.parseStatus === 2).length)
const pendingCount = computed(() => list.value.filter(d => d.parseStatus !== 2).length)

const parseStatusText = s => ['待解析', '解析中', '已完成', '解析失败'][s] || '未知'
const parseStatusClass = s => ['pending', 'parsing', 'success', 'failed'][s] || ''
const formatSize = bytes => {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) { size /= 1024; i++ }
  return size.toFixed(1) + ' ' + units[i]
}

const tableRowClass = ({ rowIndex }) => `animate-row row-${rowIndex}`

const loadData = async () => {
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value, keyword: keyword.value }
    if (kbId.value) params.kbId = kbId.value
    const res = await getDocuments(params)
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
  // 有解析中的文档时启动轮询
  startPolling()
}

const startPolling = () => {
  stopPolling()
  if (hasParsing.value) {
    pollTimer = setInterval(async () => {
      try {
        const params = { pageNum: pageNum.value, pageSize: pageSize.value, keyword: keyword.value }
        if (kbId.value) params.kbId = kbId.value
        const res = await getDocuments(params)
        list.value = res.data?.records || []
        total.value = res.data?.total || 0
        // 全部解析完成，停止轮询
        if (!hasParsing.value) stopPolling()
      } catch (e) { /* ignore */ }
    }, 3000)
  }
}

const stopPolling = () => {
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
}

const loadKbOptions = async () => {
  const res = await getKbList({ pageNum: 1, pageSize: 1000 })
  kbOptions.value = res.data?.records || []
}

const beforeUpload = file => {
  const ext = file.name.split('.').pop().toLowerCase()
  if (!['doc', 'docx', 'xlsx', 'xls'].includes(ext)) {
    ElMessage.error('仅支持 doc、docx、xlsx、xls 格式')
    return false
  }
  if (file.size > 50 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过50MB')
    return false
  }
  return true
}

const handleUpload = async ({ file }) => {
  uploading.value = true
  try {
    await uploadDocument(file, uploadKbId.value)
    ElMessage.success('上传成功，正在解析...')
    await loadData()
    startPolling()
  } catch (e) {
    ElMessage.error('上传失败，请重试')
  } finally { uploading.value = false }
}

const handleReparse = async id => {
  // 立即更新本地状态为"解析中"
  const doc = list.value.find(d => d.id === id)
  if (doc) {
    doc.parseStatus = 1
    doc.chunkCount = 0
  }
  try {
    await reparseDocument(id)
    ElMessage.success('已提交重新解析')
    startPolling()
  } catch (e) {
    // 恢复状态
    if (doc) doc.parseStatus = 3
    ElMessage.error('操作失败')
  }
}

const handleDelete = async id => {
  try {
    await deleteDocument(id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

const viewChunks = async row => {
  try {
    const res = await getDocChunks(row.id)
    chunks.value = res.data || []
    chunkDialogVisible.value = true
  } catch (e) {
    ElMessage.error('获取知识块失败')
  }
}

// 拖拽上传
const onDragOver = (e) => { e.preventDefault(); isDragging.value = true }
const onDragLeave = () => { isDragging.value = false }
const onDrop = (e) => {
  e.preventDefault()
  isDragging.value = false
  if (!uploadKbId.value) {
    ElMessage.warning('请先选择知识库')
    return
  }
  const files = e.dataTransfer?.files
  if (files?.length) {
    const file = files[0]
    if (beforeUpload(file)) handleUpload({ file })
  }
}

onMounted(() => {
  loadData()
  if (!kbId.value) loadKbOptions()
  document.addEventListener('dragover', onDragOver)
  document.addEventListener('dragleave', onDragLeave)
  document.addEventListener('drop', onDrop)
})

onUnmounted(() => {
  stopPolling()
  document.removeEventListener('dragover', onDragOver)
  document.removeEventListener('dragleave', onDragLeave)
  document.removeEventListener('drop', onDrop)
})
</script>

<style lang="scss" scoped>
.header-icon {
  font-size: 18px;
  margin-right: 4px;
  vertical-align: middle;
}

.header-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.upload-btn {
  &:hover .el-icon { transform: translateY(-2px); }
  .el-icon { transition: transform var(--spring); }
}

.doc-stats {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.doc-stat-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 20px;
  background: var(--bg-secondary);
  font-size: 13px;
  transition: all var(--transition);
  cursor: default;
  &:hover { background: var(--primary-bg); transform: translateY(-1px); }

  .chip-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    &.docx { background: var(--primary-color); }
    &.xlsx { background: var(--success-color); }
    &.success { background: var(--success-color); }
    &.pending { background: var(--warning-color); }
  }
  .chip-label { color: var(--text-secondary); font-weight: 500; }
  .chip-count { color: var(--text-primary); font-weight: 700; }
}

.file-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.file-type-icon {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all var(--spring);
  position: relative;
  overflow: hidden;
  &.docx, &.doc { background: linear-gradient(135deg, #4A6CF7, #6B8AFF); }
  &.xlsx, &.xls { background: linear-gradient(135deg, #10B981, #34D399); }
  &::after {
    content: '';
    position: absolute;
    top: -50%;
    left: -50%;
    width: 200%;
    height: 200%;
    background: linear-gradient(45deg, transparent 40%, rgba(255,255,255,0.15) 50%, transparent 60%);
    transition: all 0.5s;
    transform: translateX(-100%);
  }
  &:hover::after { transform: translateX(100%); }
  .file-type-letter {
    font-size: 13px;
    font-weight: 800;
    color: #fff;
    position: relative;
    z-index: 1;
  }
}

.file-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.file-name {
  font-weight: 500;
  transition: color var(--transition);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-size {
  font-size: 12px;
  color: var(--text-placeholder);
  font-family: 'SF Mono', 'Fira Code', monospace;
}

.parse-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--text-placeholder);
  display: inline-block;
}

.count-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  min-width: 32px;
  height: 26px;
  padding: 0 10px;
  border-radius: 13px;
  font-size: 13px;
  font-weight: 600;
  background: var(--bg-secondary);
  color: var(--text-secondary);
  transition: all var(--transition);
  &.has-data {
    background: var(--primary-bg);
    color: var(--primary-color);
  }
  .count-icon { font-size: 12px; }
}

.time-text {
  font-size: 13px;
  color: var(--text-secondary);
}

.action-row {
  display: flex;
  align-items: center;
  flex-wrap: nowrap;
  white-space: nowrap;
  gap: 4px;
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

.empty-hint {
  font-size: 13px;
  color: var(--text-placeholder);
  margin-top: 6px;
}

.upload-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.2);
  backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.upload-progress-card {
  background: var(--card-bg);
  border-radius: var(--radius-xl);
  padding: 28px 40px;
  box-shadow: var(--shadow-xl);
  display: flex;
  align-items: center;
  gap: 16px;
  animation: scaleIn 0.3s ease-out;
}

.upload-spinner {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: var(--primary-bg);
  display: flex;
  align-items: center;
  justify-content: center;
}

.upload-spin {
  font-size: 22px;
  color: var(--primary-color);
  animation: spin 1s linear infinite;
}

.upload-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  .upload-title { font-size: 15px; font-weight: 600; color: var(--text-primary); }
  .upload-desc { font-size: 13px; color: var(--text-secondary); }
}

.drag-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(74, 108, 247, 0.08);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  .drag-content {
    text-align: center;
    padding: 48px;
    border: 3px dashed var(--primary-color);
    border-radius: var(--radius-xl);
    background: rgba(255, 255, 255, 0.9);
    animation: breathe 1.5s ease-in-out infinite;
    p {
      margin-top: 12px;
      font-size: 16px;
      font-weight: 600;
      color: var(--primary-color);
    }
  }
}

.chunk-summary {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-light);
  strong { color: var(--primary-color); }
}

.chunk-item {
  border: 1px solid var(--border-light);
  border-radius: var(--radius);
  padding: 16px 20px;
  margin-bottom: 12px;
  background: var(--bg-color);
  transition: all var(--transition);
  animation: slideUp 0.35s ease-out both;
  &:hover {
    border-color: var(--primary-color);
    box-shadow: 0 0 0 3px var(--primary-bg);
    transform: translateY(-1px);
  }
}
.chunk-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  .chunk-index {
    font-size: 13px;
    font-weight: 700;
    color: var(--primary-color);
  }
}
.chunk-content {
  font-size: 14px;
  line-height: 1.7;
  color: var(--text-regular);
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 200px;
  overflow-y: auto;
}

.chunk-list-enter-active { animation: slideUp 0.35s ease-out both; }

:deep(.action-col .cell) {
  display: flex !important;
  flex-wrap: nowrap !important;
  overflow: visible !important;
}
</style>
