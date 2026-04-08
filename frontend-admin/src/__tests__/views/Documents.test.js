import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import ElementPlus from 'element-plus'
import Documents from '../../views/Documents.vue'

vi.mock('../../api', () => ({
  getDocuments: vi.fn().mockResolvedValue({
    data: {
      total: 2,
      records: [
        { id: 1, originalName: 'Java基础.docx', fileType: 'docx', fileSize: 102400, parseStatus: 2, chunkCount: 10, createdAt: '2025-01-01' },
        { id: 2, originalName: '数据分析.xlsx', fileType: 'xlsx', fileSize: 204800, parseStatus: 2, chunkCount: 20, createdAt: '2025-01-02' }
      ]
    }
  }),
  uploadDocument: vi.fn().mockResolvedValue({ code: 200 }),
  deleteDocument: vi.fn().mockResolvedValue({ code: 200 }),
  reparseDocument: vi.fn().mockResolvedValue({ code: 200 }),
  getDocChunks: vi.fn().mockResolvedValue({ data: [{ id: 1, content: '测试内容', chunkIndex: 0, sourceInfo: '段落' }] }),
  getKbList: vi.fn().mockResolvedValue({ data: { records: [{ id: 1, name: '测试知识库' }] } })
}))

describe('Documents.vue', () => {
  let router

  beforeEach(() => {
    setActivePinia(createPinia())
    router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/documents', component: Documents },
        { path: '/knowledge-base/:id/documents', component: Documents }
      ]
    })
  })

  const mountDocs = async () => {
    await router.push('/documents')
    await router.isReady()
    const wrapper = mount(Documents, {
      global: { plugins: [router, ElementPlus, createPinia()] }
    })
    await flushPromises()
    return wrapper
  }

  it('渲染文档管理页面', async () => {
    const wrapper = await mountDocs()
    expect(wrapper.text()).toContain('全部文档')
  })

  it('显示上传文档按钮', async () => {
    const wrapper = await mountDocs()
    expect(wrapper.text()).toContain('上传文档')
  })

  it('显示文档列表', async () => {
    const wrapper = await mountDocs()
    expect(wrapper.text()).toContain('Java基础.docx')
    expect(wrapper.text()).toContain('数据分析.xlsx')
  })

  it('显示文件类型标签', async () => {
    const wrapper = await mountDocs()
    expect(wrapper.text()).toContain('docx')
    expect(wrapper.text()).toContain('xlsx')
  })

  it('显示解析状态', async () => {
    const wrapper = await mountDocs()
    expect(wrapper.text()).toContain('已完成')
  })

  it('显示知识块数量', async () => {
    const wrapper = await mountDocs()
    expect(wrapper.text()).toContain('10')
    expect(wrapper.text()).toContain('20')
  })

  it('包含搜索框', async () => {
    const wrapper = await mountDocs()
    const searchInput = wrapper.find('.el-input')
    expect(searchInput.exists()).toBe(true)
  })

  it('包含分页组件', async () => {
    const wrapper = await mountDocs()
    expect(wrapper.find('.el-pagination').exists()).toBe(true)
  })

  it('包含操作按钮', async () => {
    const wrapper = await mountDocs()
    expect(wrapper.text()).toContain('知识块')
    expect(wrapper.text()).toContain('重新解析')
    expect(wrapper.text()).toContain('删除')
  })
})
