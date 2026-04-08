import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import ElementPlus from 'element-plus'
import Login from '../../views/Login.vue'

vi.mock('../../api', () => ({
  login: vi.fn().mockResolvedValue({
    data: { token: 'mock-token', user: { id: 1, username: 'admin', nickname: '管理员' } }
  }),
  getUserInfo: vi.fn().mockResolvedValue({ data: { id: 1, username: 'admin' } })
}))

describe('Login.vue', () => {
  let router

  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/login', component: Login },
        { path: '/', component: { template: '<div>Home</div>' } }
      ]
    })
  })

  const mountLogin = async () => {
    const wrapper = mount(Login, {
      global: { plugins: [router, ElementPlus, createPinia()] }
    })
    await flushPromises()
    return wrapper
  }

  it('渲染登录页面', async () => {
    const wrapper = await mountLogin()
    expect(wrapper.text()).toContain('知识库管理系统')
  })

  it('显示用户名和密码输入框', async () => {
    const wrapper = await mountLogin()
    const inputs = wrapper.findAll('.el-input')
    expect(inputs.length).toBeGreaterThanOrEqual(2)
  })

  it('显示登录按钮', async () => {
    const wrapper = await mountLogin()
    expect(wrapper.text()).toContain('登 录')
  })

  it('显示默认账号提示', async () => {
    const wrapper = await mountLogin()
    expect(wrapper.text()).toContain('admin / admin123')
  })

  it('显示系统英文名称', async () => {
    const wrapper = await mountLogin()
    expect(wrapper.text()).toContain('Knowledge Base Management System')
  })

  it('登录卡片有动画效果', async () => {
    const wrapper = await mountLogin()
    // 等待动画触发
    await new Promise(r => setTimeout(r, 200))
    await flushPromises()
    const card = wrapper.find('.login-card')
    expect(card.exists()).toBe(true)
  })
})
