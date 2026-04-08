<template>
  <div>
    <div class="card search-card">
      <div class="card-header"><h3>知识检索</h3></div>
      <div class="search-area">
        <el-select v-model="kbId" placeholder="全部知识库" clearable style="width:200px" size="large">
          <template #prefix><el-icon><FolderOpened /></el-icon></template>
          <el-option v-for="kb in kbOptions" :key="kb.id" :label="kb.name" :value="kb.id" />
        </el-select>
        <el-input
          v-model="keyword"
          placeholder="输入关键词搜索知识内容..."
          clearable
          size="large"
          style="flex:1;max-width:500px"
          @keyup.enter="handleSearch"
          class="search-input"
          @focus="inputFocused = true"
          @blur="inputFocused = false"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" size="large" :loading="loading" @click="handleSearch" class="search-btn">
          <el-icon><Search /></el-icon> 搜索
        </el-button>
      </div>

      <!-- 搜索历史 -->
      <transition name="fade-slide">
        <div v-if="!searched && searchHistory.length > 0" class="search-history">
          <div class="history-header">
            <span class="history-label"><el-icon><Clock /></el-icon> 最近搜索</span>
            <el-button link size="small" @click="clearHistory">清除</el-button>
          </div>
          <div class="history-tags">
            <el-tag
              v-for="(item, i) in searchHistory"
              :key="i"
              effect="plain"
              round
              class="history-tag"
              @click="keyword = item; handleSearch()"
            >{{ item }}</el-tag>
          </div>
        </div>
      </transition>

      <div v-if="!searched" class="empty-state">
        <div class="search-illustration" :class="{ 'pulse-active': inputFocused }">
          <el-icon :size="56" color="#CBD5E1"><Search /></el-icon>
          <div class="illustration-ring"></div>
          <div class="illustration-ring ring-2"></div>
        </div>
        <p class="empty-text">输入关键词，搜索知识库中的内容</p>
        <p class="empty-hint">支持模糊搜索，可指定知识库范围</p>
      </div>

      <!-- 搜索骨架屏 -->
      <div v-else-if="loading" class="search-skeleton">
        <div class="skeleton-result" v-for="i in 3" :key="i">
          <div class="skeleton-row" style="width:40%;height:12px;margin-bottom:16px"></div>
          <div class="skeleton-row" style="width:100%;height:14px"></div>
          <div class="skeleton-row" style="width:80%;height:14px"></div>
        </div>
      </div>

      <div v-else-if="results.length === 0 && !loading" class="empty-state">
        <div class="empty-illustration">
          <el-icon :size="48" color="#CBD5E1"><DocumentRemove /></el-icon>
          <div class="empty-circle"></div>
        </div>
        <p class="empty-text">未找到相关内容</p>
        <p class="empty-hint">换个关键词试试，或扩大搜索范围</p>
      </div>

      <div v-else class="search-results">
        <div class="result-count">
          <el-icon><Finished /></el-icon>
          找到 <strong>{{ results.length }}</strong> 条结果
          <span class="search-time" v-if="searchTime">耗时 {{ searchTime }}ms</span>
        </div>
        <transition-group name="result-list" tag="div">
          <div v-for="(item, index) in results" :key="item.chunkId" class="result-item" :style="{ animationDelay: index * 0.05 + 's' }">
            <div class="result-meta">
              <el-tag size="small" effect="light" round class="nowrap-tag">{{ item.knowledgeBaseName }}</el-tag>
              <el-tag size="small" type="info" effect="plain" round class="nowrap-tag">{{ item.documentName }}</el-tag>
              <span class="result-source">{{ item.sourceInfo }} #{{ item.chunkIndex }}</span>
            </div>
            <div class="result-content" v-html="highlightKeyword(item.content)"></div>
            <div class="result-footer">
              <span class="relevance-indicator">
                <span class="relevance-bar"><span class="relevance-fill" :style="{ width: Math.min(100, 60 + index * -5) + '%' }"></span></span>
                相关度
              </span>
            </div>
          </div>
        </transition-group>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { searchKnowledge, getKbList } from '../api'
import { ElMessage } from 'element-plus'

const keyword = ref('')
const kbId = ref(null)
const loading = ref(false)
const searched = ref(false)
const results = ref([])
const kbOptions = ref([])
const inputFocused = ref(false)
const searchTime = ref(0)
const searchHistory = ref(JSON.parse(localStorage.getItem('kb_search_history') || '[]'))

const handleSearch = async () => {
  if (!keyword.value.trim()) {
    ElMessage.warning('请输入搜索关键词')
    return
  }
  // 保存搜索历史
  const history = searchHistory.value.filter(h => h !== keyword.value.trim())
  history.unshift(keyword.value.trim())
  searchHistory.value = history.slice(0, 8)
  localStorage.setItem('kb_search_history', JSON.stringify(searchHistory.value))

  loading.value = true
  searched.value = true
  const startTime = Date.now()
  try {
    const res = await searchKnowledge({ keyword: keyword.value, kbId: kbId.value, limit: 30 })
    results.value = res.data || []
    searchTime.value = Date.now() - startTime
    if (results.value.length === 0) {
      ElMessage.info('未找到相关内容')
    }
  } catch (e) {
    ElMessage.error('搜索失败，请重试')
  } finally { loading.value = false }
}

const clearHistory = () => {
  searchHistory.value = []
  localStorage.removeItem('kb_search_history')
}

const highlightKeyword = text => {
  if (!keyword.value || !text) return text
  const escaped = keyword.value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return text.replace(new RegExp(`(${escaped})`, 'gi'), '<mark>$1</mark>')
}

onMounted(async () => {
  const res = await getKbList({ pageNum: 1, pageSize: 1000 })
  kbOptions.value = res.data?.records || []
})
</script>

<style lang="scss" scoped>
.search-area {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  align-items: center;
}

.search-input {
  :deep(.el-input__wrapper) {
    transition: all var(--transition), box-shadow var(--transition);
    &.is-focus {
      box-shadow: 0 0 0 1px var(--primary-color) inset, 0 0 0 4px rgba(74, 108, 247, 0.12);
    }
  }
}

.search-btn {
  padding: 0 28px;
  font-weight: 600;
  &:hover .el-icon { transform: scale(1.15); }
  .el-icon { transition: transform var(--spring); }
}

.search-history {
  margin-bottom: 24px;
  padding: 16px;
  background: var(--bg-secondary);
  border-radius: var(--radius);
  .history-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 10px;
  }
  .history-label {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    color: var(--text-secondary);
    font-weight: 500;
  }
  .history-tags { display: flex; gap: 8px; flex-wrap: wrap; }
  .history-tag {
    cursor: pointer;
    transition: all var(--transition);
    &:hover {
      color: var(--primary-color);
      border-color: var(--primary-color);
      background: var(--primary-bg);
      transform: translateY(-1px);
    }
  }
}

.search-illustration {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: var(--bg-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
  transition: all var(--transition-slow);
  position: relative;

  .illustration-ring {
    position: absolute;
    width: 100%;
    height: 100%;
    border-radius: 50%;
    border: 2px solid var(--border-light);
    opacity: 0;
    transition: all var(--transition-slow);
  }
  .ring-2 { width: 140%; height: 140%; }

  &.pulse-active {
    background: var(--primary-bg);
    .illustration-ring {
      opacity: 1;
      border-color: rgba(74, 108, 247, 0.15);
      animation: searchPulse 2s ease-in-out infinite;
    }
    .ring-2 { animation-delay: 0.5s; }
  }
}

@keyframes searchPulse {
  0% { transform: scale(1); opacity: 0.6; }
  50% { transform: scale(1.15); opacity: 0; }
  100% { transform: scale(1); opacity: 0; }
}

.empty-illustration {
  position: relative;
  width: 80px;
  height: 80px;
  margin: 0 auto 16px;
  display: flex;
  align-items: center;
  justify-content: center;
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

.search-skeleton {
  padding: 16px 0;
  .skeleton-result {
    border: 1px solid var(--border-light);
    border-radius: var(--radius-lg);
    padding: 20px 24px;
    margin-bottom: 12px;
  }
}

.result-count {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-light);
  strong { color: var(--primary-color); }
  .search-time {
    margin-left: auto;
    font-size: 12px;
    color: var(--text-placeholder);
    font-family: 'SF Mono', 'Fira Code', monospace;
  }
}

.result-list-enter-active { animation: slideUp 0.4s ease-out both; }

.result-item {
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 20px 24px;
  margin-bottom: 12px;
  background: var(--card-bg);
  transition: all var(--transition);
  animation: slideUp 0.4s ease-out both;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 3px;
    background: var(--primary-gradient);
    opacity: 0;
    transition: opacity var(--transition);
  }

  &:hover {
    border-color: var(--primary-light);
    box-shadow: var(--shadow-md);
    transform: translateY(-2px);
    &::before { opacity: 1; }
  }
}

.result-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
  .nowrap-tag {
    display: inline-flex;
    align-items: center;
    white-space: nowrap;
  }
  .result-source {
    font-size: 12px;
    color: var(--text-secondary);
    margin-left: auto;
    font-family: 'SF Mono', 'Fira Code', monospace;
  }
}

.result-content {
  font-size: 14px;
  line-height: 1.8;
  color: var(--text-regular);
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 160px;
  overflow-y: auto;

  :deep(mark) {
    background: linear-gradient(120deg, #FDE68A 0%, #FCD34D 100%);
    padding: 1px 4px;
    border-radius: 3px;
    color: #92400E;
    font-weight: 500;
  }
}

.result-footer {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid var(--border-light);
}

.relevance-indicator {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--text-placeholder);
}

.relevance-bar {
  width: 60px;
  height: 4px;
  border-radius: 2px;
  background: var(--bg-secondary);
  overflow: hidden;
  .relevance-fill {
    display: block;
    height: 100%;
    border-radius: 2px;
    background: var(--primary-gradient);
    transition: width 0.6s ease-out;
  }
}
</style>
