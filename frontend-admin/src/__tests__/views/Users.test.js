import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import ElementPlus from 'element-plus'
import Users from '../../views/Users.vue'

vi.mock('../../api', () => ({
  getUsers: vi.fn().mockResolvedValue({
    data: {
      total: 2,
      records: [
        { id: 1, username: 'admin', nickname: '管理员', status: 1, createdAt: '2025-01-01' },
        { id: 2, username: 'user1', nickname: '用户1', status: 1, createdAt: '2025-01-02' }
      ]
    }
  }),
  createUser: vi.fn().mockResolvedValue({ code: 200 }),
  updateUser: vi.fn().mockResolvedValue({ code: 200 }),
  deleteUser: vi.fn().mockResolvedValue({ code: 200 }),
  updateUserStatus: vi.fn().mockResolvedValue({ code: 200 })
}))

describe('Users.vue', () => {
  let router

  beforeEach(() => {
    setActivePinia(createPinia())
    router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/users', component: Users }]
    })
  })

  const mountUsers = async () => {
    const wrapper = mount(Users, {
      global: { plugins: [router, ElementPlus, createPinia()] }
    })
    await flushPromises()
    return wrapper
  }

  it('渲染用户管理页面', async () => {
    const wrapper = await mountUsers()
    expect(wrapper.text()).toContain('用户管理')
  })

  it('显示新增用户按钮', async () => {
    const wrapper = await mountUsers()
    expect(wrapper.text()).toContain('新增用户')
  })

  it('显示用户列表', async () => {
    const wrapper = await mountUsers()
    expect(wrapper.text()).toContain('admin')
    expect(wrapper.text()).toContain('管理员')
  })

  it('显示搜索框', async () => {
    const wrapper = await mountUsers()
    const searchInput = wrapper.find('.el-input')
    expect(searchInput.exists()).toBe(true)
  })

  it('包含编辑和删除操作', async () => {
    const wrapper = await mountUsers()
    expect(wrapper.text()).toContain('编辑')
    expect(wrapper.text()).toContain('删除')
  })

  it('包含分页组件', async () => {
    const wrapper = await mountUsers()
    expect(wrapper.find('.el-pagination').exists()).toBe(true)
  })

  it('包含状态切换开关', async () => {
    const wrapper = await mountUsers()
    const switches = wrapper.findAll('.el-switch')
    expect(switches.length).toBeGreaterThanOrEqual(1)
  })
})
