<template>
  <el-container class="layout">
    <el-aside :width="isCollapse ? '72px' : '240px'" class="sidebar">
      <div class="logo" @click="$router.push('/')">
        <div class="logo-icon">
          <el-icon :size="22" color="#fff"><Collection /></el-icon>
        </div>
        <transition name="fade">
          <span v-show="!isCollapse" class="logo-text">知识库系统</span>
        </transition>
      </div>
      <el-menu
        :default-active="$route.path"
        router
        :collapse="isCollapse"
        background-color="transparent"
        text-color="rgba(255,255,255,0.6)"
        active-text-color="#fff"
        class="sidebar-menu"
      >
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
      </el-menu>
      <div class="sidebar-footer" v-show="!isCollapse">
        <div class="version-badge">
          <span class="dot"></span> v1.0.0
        </div>
      </div>
    </el-aside>
    <el-container class="main-container">
      <el-header class="header glass">
        <div class="header-left">
          <div class="collapse-btn" @click="isCollapse = !isCollapse">
            <el-icon :size="18">
              <Fold v-if="!isCollapse" /><Expand v-else />
            </el-icon>
          </div>
          <div class="breadcrumb">
            <el-icon :size="14" class="breadcrumb-icon"><HomeFilled /></el-icon>
            <span class="breadcrumb-sep">/</span>
            <span class="page-title">{{ $route.meta.title }}</span>
          </div>
        </div>
        <div class="header-right">
          <div class="header-time">{{ currentTime }}</div>
          <el-dropdown @command="handleCommand" trigger="click">
            <div class="user-info">
              <el-avatar :size="34" class="user-avatar">
                {{ (userStore.userInfo?.nickname || 'U').charAt(0) }}
              </el-avatar>
              <div class="user-meta" v-show="true">
                <span class="username">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
                <span class="user-role">管理员</span>
              </div>
              <el-icon :size="12"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">
                  <el-icon><SwitchButton /></el-icon> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const isCollapse = ref(false)
const currentTime = ref('')

const menuItems = [
  { path: '/dashboard', title: '仪表盘', icon: 'DataAnalysis' },
  { path: '/knowledge-base', title: '知识库管理', icon: 'FolderOpened' },
  { path: '/documents', title: '全部文档', icon: 'Document' },
  { path: '/search', title: '知识检索', icon: 'Search' },
  { path: '/users', title: '用户管理', icon: 'UserFilled' },
  { path: '/logs', title: '操作日志', icon: 'Notebook' }
]

let timer = null
const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

onMounted(async () => {
  updateTime()
  timer = setInterval(updateTime, 60000)
  if (userStore.isLoggedIn && !userStore.userInfo) {
    try { await userStore.fetchUserInfo() } catch (e) { /* handled */ }
  }
})

onUnmounted(() => { if (timer) clearInterval(timer) })

const handleCommand = cmd => {
  if (cmd === 'logout') {
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}
</script>

<style lang="scss" scoped>
.layout { min-height: 100vh; }

.sidebar {
  background: linear-gradient(180deg, #1a1f36 0%, #151929 100%);
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  position: relative;
  z-index: 10;
  box-shadow: 2px 0 12px rgba(0, 0, 0, 0.15);
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  cursor: pointer;
  padding: 0 16px;
  margin-bottom: 8px;
  transition: all var(--transition);
  &:hover {
    .logo-icon { transform: scale(1.05); box-shadow: 0 6px 20px rgba(74, 108, 247, 0.4); }
  }

  .logo-icon {
    width: 38px;
    height: 38px;
    border-radius: 10px;
    background: var(--primary-gradient);
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    box-shadow: 0 4px 12px rgba(74, 108, 247, 0.3);
    transition: all var(--spring);
  }
  .logo-text {
    color: #fff;
    font-size: 16px;
    font-weight: 700;
    white-space: nowrap;
    letter-spacing: 0.5px;
  }
}

.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

.sidebar-menu {
  border-right: none;
  padding: 0 12px;
  flex: 1;

  :deep(.el-menu-item) {
    height: 44px;
    line-height: 44px;
    margin-bottom: 4px;
    border-radius: 10px;
    font-size: 14px;
    font-weight: 500;
    transition: all var(--transition);

    &.is-active {
      background: rgba(74, 108, 247, 0.2) !important;
      color: #fff !important;
      position: relative;
      &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 50%;
        transform: translateY(-50%);
        width: 3px;
        height: 20px;
        background: var(--primary-color);
        border-radius: 0 3px 3px 0;
      }
    }
    &:hover:not(.is-active) {
      background: rgba(255, 255, 255, 0.06) !important;
      color: rgba(255, 255, 255, 0.9) !important;
      transform: translateX(4px);
    }
    .el-icon { font-size: 18px; }
  }
}

.sidebar-footer {
  padding: 16px;
  text-align: center;
  .version-badge {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    font-size: 11px;
    color: rgba(255, 255, 255, 0.3);
    background: rgba(255, 255, 255, 0.05);
    padding: 4px 12px;
    border-radius: 10px;
    .dot {
      width: 6px; height: 6px;
      border-radius: 50%;
      background: var(--success-color);
      animation: pulse 2s ease-in-out infinite;
    }
  }
}

.main-container { background: var(--bg-color); }

.header {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 64px;
  border-bottom: 1px solid var(--border-light);
  z-index: 5;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;

  .collapse-btn {
    width: 36px;
    height: 36px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 8px;
    cursor: pointer;
    color: var(--text-regular);
    transition: all var(--spring);
    &:hover {
      background: var(--bg-secondary);
      color: var(--primary-color);
      transform: scale(1.05);
    }
    &:active { transform: scale(0.95); }
  }

  .breadcrumb {
    display: flex;
    align-items: center;
    gap: 8px;
  }
  .breadcrumb-icon { color: var(--text-secondary); }
  .breadcrumb-sep { color: var(--text-placeholder); font-size: 12px; }
  .page-title {
    font-size: 16px;
    font-weight: 600;
    color: var(--text-primary);
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;

  .header-time {
    font-size: 13px;
    color: var(--text-secondary);
    font-weight: 500;
    font-variant-numeric: tabular-nums;
  }

  .user-info {
    display: flex;
    align-items: center;
    gap: 10px;
    cursor: pointer;
    padding: 6px 12px 6px 6px;
    border-radius: 24px;
    transition: all var(--transition);
    &:hover { background: var(--bg-secondary); }
  }
  .user-avatar {
    background: var(--primary-gradient);
    color: #fff;
    font-weight: 600;
    font-size: 14px;
  }
  .user-meta {
    display: flex;
    flex-direction: column;
    line-height: 1.2;
  }
  .username {
    font-size: 14px;
    font-weight: 500;
    color: var(--text-regular);
  }
  .user-role {
    font-size: 11px;
    color: var(--text-secondary);
  }
}

.main-content {
  background: var(--bg-color);
  padding: 24px;
  min-height: calc(100vh - 64px);
}
</style>
