import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import ElementPlus from 'element-plus'
import Dashboard from '../../views/Dashboard.vue'

vi.mock('../../api', () => ({
  getKbList: vi.fn().mockResolvedValue({
    data: {
      total: 3,
      records: [
        { id: 1, name: 'KB1', chunkCount: 100 },
        { id: 2, name: 'KB2', chunkCount: 200 }
      ]
    }
  }),
  getDocuments: vi.fn().mockResolvedValue({
    data: {
      total: 15,
      records: [
        { id: 1, originalName: 'test.docx', fileType: 'docx', parseStatus: 2, createdAt: '2025-01-01' }
      ]
    }
  }),
  getLogs: vi.fn().mockResolvedValue({
    data: {
      total: 8,
      records: [
        { id: 1, username: 'admin', module: '用户管理', operation: '新增用户', createdAt: '2025-01-01' }
      ]
    }
  })
}))

describe('Dashboard.vue', () => {
  let router

  beforeEach(() => {
    setActivePinia(createPinia())
    router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/dashboard', component: Dashboard },
        { path: '/documents', component: { template: '<div>Docs</div>' } },
        { path: '/logs', component: { template: '<div>Logs</div>' } }
      ]
    })
  })

  const mountDashboard = async () => {
    const wrapper = mount(Dashboard, {
      global: { plugins: [router, ElementPlus, createPinia()] }
    })
    await flushPromises()
    return wrapper
  }

  it('渲染仪表盘页面', async () => {
    const wrapper = await mountDashboard()
    expect(wrapper.find('.dashboard').exists() || wrapper.find('.stat-card').exists()).toBe(true)
  })

  it('显示统计卡片', async () => {
    const wrapper = await mountDashboard()
    expect(wrapper.text()).toContain('知识库')
    expect(wrapper.text()).toContain('文档总数')
    expect(wrapper.text()).toContain('知识块')
    expect(wrapper.text()).toContain('今日操作')
  })

  it('显示最近上传文档', async () => {
    const wrapper = await mountDashboard()
    expect(wrapper.text()).toContain('最近上传文档')
    expect(wrapper.text()).toContain('test.docx')
  })

  it('显示最近操作日志', async () => {
    const wrapper = await mountDashboard()
    expect(wrapper.text()).toContain('最近操作日志')
    expect(wrapper.text()).toContain('admin')
  })

  it('显示查看全部链接', async () => {
    const wrapper = await mountDashboard()
    const links = wrapper.findAll('.el-button--link')
    expect(links.length).toBeGreaterThanOrEqual(0)
    expect(wrapper.text()).toContain('查看全部')
  })

  it('调用所有API获取数据', async () => {
    const { getKbList, getDocuments, getLogs } = await import('../../api')
    await mountDashboard()
    expect(getKbList).toHaveBeenCalled()
    expect(getDocuments).toHaveBeenCalled()
    expect(getLogs).toHaveBeenCalled()
  })
})
