import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import ElementPlus from 'element-plus'
import KnowledgeBase from '../../views/KnowledgeBase.vue'

vi.mock('../../api', () => ({
  getKbList: vi.fn().mockResolvedValue({
    data: {
      total: 2,
      records: [
        { id: 1, name: 'Java知识库', description: 'Java相关', docCount: 5, chunkCount: 100, createdAt: '2025-01-01' },
        { id: 2, name: 'Python知识库', description: 'Python相关', docCount: 3, chunkCount: 50, createdAt: '2025-01-02' }
      ]
    }
  }),
  createKb: vi.fn().mockResolvedValue({ code: 200 }),
  updateKb: vi.fn().mockResolvedValue({ code: 200 }),
  deleteKb: vi.fn().mockResolvedValue({ code: 200 })
}))

describe('KnowledgeBase.vue', () => {
  let router

  beforeEach(() => {
    setActivePinia(createPinia())
    router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/knowledge-base', component: KnowledgeBase },
        { path: '/knowledge-base/:id/documents', component: { template: '<div>Docs</div>' } }
      ]
    })
  })

  const mountKb = async () => {
    const wrapper = mount(KnowledgeBase, {
      global: { plugins: [router, ElementPlus, createPinia()] }
    })
    await flushPromises()
    return wrapper
  }

  it('渲染知识库管理页面', async () => {
    const wrapper = await mountKb()
    expect(wrapper.text()).toContain('知识库管理')
  })

  it('显示新建知识库按钮', async () => {
    const wrapper = await mountKb()
    expect(wrapper.text()).toContain('新建知识库')
  })

  it('显示知识库列表', async () => {
    const wrapper = await mountKb()
    expect(wrapper.text()).toContain('Java知识库')
    expect(wrapper.text()).toContain('Python知识库')
  })

  it('显示文档数和知识块数', async () => {
    const wrapper = await mountKb()
    expect(wrapper.text()).toContain('5')
    expect(wrapper.text()).toContain('100')
  })

  it('显示搜索框', async () => {
    const wrapper = await mountKb()
    const searchInput = wrapper.find('.el-input')
    expect(searchInput.exists()).toBe(true)
  })

  it('包含编辑和删除操作', async () => {
    const wrapper = await mountKb()
    expect(wrapper.text()).toContain('编辑')
    expect(wrapper.text()).toContain('删除')
  })

  it('包含分页组件', async () => {
    const wrapper = await mountKb()
    expect(wrapper.find('.el-pagination').exists()).toBe(true)
  })
})
