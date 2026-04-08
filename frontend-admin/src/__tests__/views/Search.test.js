import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import ElementPlus from 'element-plus'
import Search from '../../views/Search.vue'

vi.mock('../../api', () => ({
  searchKnowledge: vi.fn().mockResolvedValue({
    data: [
      { chunkId: 1, content: 'Java是面向对象的编程语言', sourceInfo: '段落', chunkIndex: 0, documentName: 'Java基础.docx', knowledgeBaseName: '技术知识库' },
      { chunkId: 2, content: 'Spring Boot是Java的微服务框架', sourceInfo: '段落', chunkIndex: 1, documentName: 'Java基础.docx', knowledgeBaseName: '技术知识库' }
    ]
  }),
  getKbList: vi.fn().mockResolvedValue({
    data: { records: [{ id: 1, name: '技术知识库' }, { id: 2, name: '产品知识库' }] }
  })
}))

describe('Search.vue', () => {
  let router

  beforeEach(() => {
    setActivePinia(createPinia())
    router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/search', component: Search }]
    })
  })

  const mountSearch = async () => {
    const wrapper = mount(Search, {
      global: { plugins: [router, ElementPlus, createPinia()] }
    })
    await flushPromises()
    return wrapper
  }

  it('渲染搜索页面', async () => {
    const wrapper = await mountSearch()
    expect(wrapper.text()).toContain('知识检索')
  })

  it('显示搜索输入框', async () => {
    const wrapper = await mountSearch()
    const inputs = wrapper.findAll('.el-input')
    expect(inputs.length).toBeGreaterThanOrEqual(1)
  })

  it('显示搜索按钮', async () => {
    const wrapper = await mountSearch()
    expect(wrapper.text()).toContain('搜索')
  })

  it('显示知识库选择器', async () => {
    const wrapper = await mountSearch()
    const select = wrapper.find('.el-select')
    expect(select.exists()).toBe(true)
  })

  it('初始状态显示提示文字', async () => {
    const wrapper = await mountSearch()
    expect(wrapper.text()).toContain('输入关键词，搜索知识库中的内容')
  })

  it('加载知识库选项', async () => {
    const { getKbList } = await import('../../api')
    await mountSearch()
    expect(getKbList).toHaveBeenCalled()
  })
})
