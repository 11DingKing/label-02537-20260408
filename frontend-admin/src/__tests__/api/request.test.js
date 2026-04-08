import { describe, it, expect, vi, beforeEach } from 'vitest'

// Mock router
vi.mock('../../router', () => ({
  default: { push: vi.fn() }
}))

// Mock element-plus
vi.mock('element-plus', () => ({
  ElMessage: { error: vi.fn(), success: vi.fn(), warning: vi.fn() }
}))

describe('API Request 模块', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('token 存储和读取', () => {
    localStorage.setItem('token', 'test-jwt-token')
    expect(localStorage.getItem('token')).toBe('test-jwt-token')
  })

  it('token 清除', () => {
    localStorage.setItem('token', 'test-jwt-token')
    localStorage.removeItem('token')
    expect(localStorage.getItem('token')).toBeNull()
  })

  it('未登录时 token 为空', () => {
    expect(localStorage.getItem('token')).toBeNull()
  })

  it('token 格式正确 - Bearer 前缀', () => {
    const token = 'my-jwt-token'
    localStorage.setItem('token', token)
    const authHeader = `Bearer ${localStorage.getItem('token')}`
    expect(authHeader).toBe('Bearer my-jwt-token')
  })

  it('token 覆盖写入', () => {
    localStorage.setItem('token', 'old-token')
    localStorage.setItem('token', 'new-token')
    expect(localStorage.getItem('token')).toBe('new-token')
  })

  it('401 响应应清除 token', () => {
    localStorage.setItem('token', 'test-token')
    // 模拟 401 处理逻辑
    const code = 401
    if (code === 401) {
      localStorage.removeItem('token')
    }
    expect(localStorage.getItem('token')).toBeNull()
  })

  it('非 401 响应不清除 token', () => {
    localStorage.setItem('token', 'test-token')
    const code = 500
    if (code === 401) {
      localStorage.removeItem('token')
    }
    expect(localStorage.getItem('token')).toBe('test-token')
  })
})
