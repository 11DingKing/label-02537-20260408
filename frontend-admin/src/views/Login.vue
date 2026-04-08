<template>
  <div class="login-page">
    <canvas ref="canvasRef" class="particle-canvas"></canvas>
    <div class="login-bg-shapes">
      <div class="shape shape-1"></div>
      <div class="shape shape-2"></div>
      <div class="shape shape-3"></div>
    </div>
    <div class="login-card" :class="{ 'card-visible': cardVisible }">
      <div class="login-header">
        <div class="logo-icon">
          <el-icon :size="32" color="#fff"><Collection /></el-icon>
        </div>
        <h1>知识库管理系统</h1>
        <p class="subtitle">
          <span v-for="(char, i) in subtitleChars" :key="i"
            class="typewriter-char"
            :style="{ animationDelay: (i * 40 + 600) + 'ms' }">{{ char }}</span>
        </p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" @keyup.enter="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" :prefix-icon="Lock" size="large" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="loading" @click="handleLogin" class="login-btn">
            <transition name="btn-text" mode="out-in">
              <span v-if="!loading" key="text">登 录</span>
              <span v-else key="loading" class="loading-text">
                <span class="dot-pulse"></span> 验证中
              </span>
            </transition>
          </el-button>
        </el-form-item>
      </el-form>

    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import { ElMessage } from 'element-plus'
import { User, Lock, Collection } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)
const cardVisible = ref(false)
const canvasRef = ref(null)
const form = ref({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const subtitleChars = 'Knowledge Base Management System'.split('')

// 粒子动画
let animationId = null
onMounted(() => {
  setTimeout(() => { cardVisible.value = true }, 100)

  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  let w = canvas.width = window.innerWidth
  let h = canvas.height = window.innerHeight
  const particles = []
  const count = 60

  for (let i = 0; i < count; i++) {
    particles.push({
      x: Math.random() * w, y: Math.random() * h,
      vx: (Math.random() - 0.5) * 0.5, vy: (Math.random() - 0.5) * 0.5,
      r: Math.random() * 2 + 1, alpha: Math.random() * 0.5 + 0.1
    })
  }

  const draw = () => {
    ctx.clearRect(0, 0, w, h)
    particles.forEach((p, i) => {
      p.x += p.vx; p.y += p.vy
      if (p.x < 0 || p.x > w) p.vx *= -1
      if (p.y < 0 || p.y > h) p.vy *= -1
      ctx.beginPath()
      ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
      ctx.fillStyle = `rgba(74, 108, 247, ${p.alpha})`
      ctx.fill()
      // 连线
      for (let j = i + 1; j < particles.length; j++) {
        const dx = p.x - particles[j].x
        const dy = p.y - particles[j].y
        const dist = Math.sqrt(dx * dx + dy * dy)
        if (dist < 150) {
          ctx.beginPath()
          ctx.moveTo(p.x, p.y)
          ctx.lineTo(particles[j].x, particles[j].y)
          ctx.strokeStyle = `rgba(74, 108, 247, ${0.08 * (1 - dist / 150)})`
          ctx.stroke()
        }
      }
    })
    animationId = requestAnimationFrame(draw)
  }
  draw()

  const resize = () => { w = canvas.width = window.innerWidth; h = canvas.height = window.innerHeight }
  window.addEventListener('resize', resize)
})

onUnmounted(() => { if (animationId) cancelAnimationFrame(animationId) })

const handleLogin = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await userStore.login(form.value.username, form.value.password)
    ElMessage.success('登录成功，欢迎回来')
    router.push('/')
  } catch (e) {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0F172A 0%, #1E293B 50%, #0F172A 100%);
  position: relative;
  overflow: hidden;
}

.particle-canvas {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.login-bg-shapes {
  position: absolute;
  inset: 0;
  overflow: hidden;
  .shape {
    position: absolute;
    border-radius: 50%;
    filter: blur(80px);
    opacity: 0.35;
    animation: float 20s ease-in-out infinite;
  }
  .shape-1 {
    width: 500px; height: 500px;
    background: linear-gradient(135deg, #4A6CF7, #6B8AFF);
    top: -15%; left: -10%;
  }
  .shape-2 {
    width: 400px; height: 400px;
    background: linear-gradient(135deg, #8B5CF6, #A78BFA);
    bottom: -10%; right: -5%;
    animation-delay: -7s;
  }
  .shape-3 {
    width: 300px; height: 300px;
    background: linear-gradient(135deg, #06B6D4, #22D3EE);
    top: 50%; left: 60%;
    animation-delay: -14s;
  }
}

.login-card {
  width: 420px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border-radius: 24px;
  padding: 48px 40px 36px;
  box-shadow: 0 25px 60px rgba(0, 0, 0, 0.3), 0 0 0 1px rgba(255, 255, 255, 0.1) inset;
  position: relative;
  z-index: 1;
  opacity: 0;
  transform: translateY(30px) scale(0.96);
  transition: all 0.7s cubic-bezier(0.16, 1, 0.3, 1);

  &.card-visible {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.login-header {
  text-align: center;
  margin-bottom: 36px;

  .logo-icon {
    width: 68px;
    height: 68px;
    border-radius: 20px;
    background: var(--primary-gradient);
    display: inline-flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 18px;
    box-shadow: 0 8px 24px rgba(74, 108, 247, 0.35);
    animation: breathe 3s ease-in-out infinite;
  }

  h1 {
    font-size: 24px;
    font-weight: 700;
    color: var(--text-primary);
    margin-bottom: 8px;
    letter-spacing: -0.3px;
  }
  .subtitle {
    font-size: 13px;
    color: var(--text-secondary);
    font-weight: 500;
    height: 20px;
  }
}

.typewriter-char {
  display: inline-block;
  opacity: 0;
  animation: typeIn 0.1s ease forwards;
}
@keyframes typeIn {
  from { opacity: 0; transform: translateY(4px); }
  to { opacity: 1; transform: translateY(0); }
}

.login-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  border-radius: 12px;
  font-weight: 600;
  letter-spacing: 2px;
  position: relative;
  overflow: hidden;

  &::after {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(90deg, transparent, rgba(255,255,255,0.2), transparent);
    transform: translateX(-100%);
    transition: transform 0.6s;
  }
  &:hover::after { transform: translateX(100%); }
}

.btn-text-enter-active, .btn-text-leave-active { transition: all 0.2s; }
.btn-text-enter-from { opacity: 0; transform: translateY(8px); }
.btn-text-leave-to { opacity: 0; transform: translateY(-8px); }

.loading-text {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.dot-pulse {
  width: 6px; height: 6px;
  border-radius: 50%;
  background: #fff;
  animation: pulse 0.8s ease-in-out infinite;
}

:deep(.el-input__wrapper) {
  border-radius: 12px;
  padding: 4px 14px;
  transition: all var(--transition);
}
:deep(.el-form-item) { margin-bottom: 22px; }
</style>
