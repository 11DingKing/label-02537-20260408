import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import ElementPlus from 'element-plus'
import Logs from '../../views/Logs.vue'

vi.mock('../../api', () => ({
  getLogs: vi.fn().mockResolvedValue({
    data: {
      total: 3,
      records: [
        { id: 1, username: 'admin', module: '用户管理', operation: '新增用户', ip: '127.0.0.1', status: 1, duration: 50, createdAt: '2025-01-01', errorMsg: null },
        { id: 2, username: 'admin', module: '知识库', operation: '创建知识库', ip: '127.0.0.1', status: 1, duration: 30, createdAt: '2025-01-01', errorMsg: null },
        { id: 3, username: 'admin', module: '文档管理', operation: '上传文档', ip: '127.0.0.1', status: 0, duration: 1500, createdAt: '2025-01-01', errorMsg: '文件解析失败' }
      ]
    }
  })
}))

describe('Logs.vue', () => {
  let router

  beforeEach(() => {
    setActivePinia(createPinia())
    router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/logs', component: Logs }]
    })
  })

  const mountLogs = async () => {
    const wrapper = mount(Logs, {
      global: { plugins: [router, ElementPlus, createPinia()] }
    })
    await flushPromises()
    return wrapper
  }

  it('渲染操作日志页面', async () => {
    const wrapper = await mountLogs()
    expect(wrapper.text()).toContain('操作日志')
  })

  it('显示日志列表', async () => {
    const wrapper = await mountLogs()
    expect(wrapper.text()).toContain('admin')
    expect(wrapper.text()).toContain('用户管理')
    expect(wrapper.text()).toContain('新增用户')
  })

  it('显示搜索框', async () => {
    const wrapper = await mountLogs()
    const searchInput = wrapper.find('.el-input')
    expect(searchInput.exists()).toBe(true)
  })

  it('显示成功和失败状态', async () => {
    const wrapper = await mountLogs()
    expect(wrapper.text()).toContain('成功')
    expect(wrapper.text()).toContain('失败')
  })

  it('显示耗时信息', async () => {
    const wrapper = await mountLogs()
    expect(wrapper.text()).toContain('50ms')
    expect(wrapper.text()).toContain('1500ms')
  })

  it('显示错误信息', async () => {
    const wrapper = await mountLogs()
    expect(wrapper.text()).toContain('文件解析失败')
  })

  it('显示IP地址', async () => {
    const wrapper = await mountLogs()
    expect(wrapper.text()).toContain('127.0.0.1')
  })

  it('包含分页组件', async () => {
    const wrapper = await mountLogs()
    expect(wrapper.find('.el-pagination').exists()).toBe(true)
  })
})
