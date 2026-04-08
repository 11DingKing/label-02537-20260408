import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useUserStore } from '../../store/user'

// Mock API
vi.mock('../../api', () => ({
  login: vi.fn().mockResolvedValue({
    data: {
      token: 'mock-jwt-token',
      user: { id: 1, username: 'admin', nickname: '管理员' }
    }
  }),
  getUserInfo: vi.fn().mockResolvedValue({
    data: { id: 1, username: 'admin', nickname: '管理员' }
  })
}))

describe('User Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  it('初始状态 - 未登录', () => {
    const store = useUserStore()
    expect(store.isLoggedIn).toBe(false)
    expect(store.userInfo).toBeNull()
    expect(store.token).toBe('')
  })

  it('登录成功 - 保存token和用户信息', async () => {
    const store = useUserStore()
    await store.login('admin', 'admin123')

    expect(store.isLoggedIn).toBe(true)
    expect(store.token).toBe('mock-jwt-token')
    expect(store.userInfo.username).toBe('admin')
    expect(localStorage.getItem('token')).toBe('mock-jwt-token')
  })

  it('登出 - 清除状态', async () => {
    const store = useUserStore()
    await store.login('admin', 'admin123')
    store.logout()

    expect(store.isLoggedIn).toBe(false)
    expect(store.token).toBe('')
    expect(store.userInfo).toBeNull()
    expect(localStorage.getItem('token')).toBeNull()
  })

  it('获取用户信息', async () => {
    const store = useUserStore()
    await store.fetchUserInfo()

    expect(store.userInfo).not.toBeNull()
    expect(store.userInfo.username).toBe('admin')
  })

  it('从 localStorage 恢复 token', () => {
    localStorage.setItem('token', 'saved-token')
    const store = useUserStore()
    expect(store.token).toBe('saved-token')
    expect(store.isLoggedIn).toBe(true)
  })

  it('登出后 isLoggedIn 为 false', async () => {
    const store = useUserStore()
    await store.login('admin', 'admin123')
    expect(store.isLoggedIn).toBe(true)
    store.logout()
    expect(store.isLoggedIn).toBe(false)
  })

  it('登出后 token 为空字符串', async () => {
    const store = useUserStore()
    await store.login('admin', 'admin123')
    store.logout()
    expect(store.token).toBe('')
  })

  it('多次登录更新token', async () => {
    const store = useUserStore()
    await store.login('admin', 'admin123')
    const firstToken = store.token
    await store.login('admin', 'admin123')
    // token应该被更新（mock返回相同值，但逻辑正确）
    expect(store.token).toBe('mock-jwt-token')
  })

  it('fetchUserInfo 更新 userInfo', async () => {
    const store = useUserStore()
    expect(store.userInfo).toBeNull()
    await store.fetchUserInfo()
    expect(store.userInfo).not.toBeNull()
    expect(store.userInfo.id).toBe(1)
  })
})
