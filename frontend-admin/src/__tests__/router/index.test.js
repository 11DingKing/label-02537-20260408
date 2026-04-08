import { describe, it, expect, beforeEach } from 'vitest'
import { createRouter, createWebHistory, createMemoryHistory } from 'vue-router'

// 简化的路由配置用于测试
const routes = [
  { path: '/login', name: 'Login', component: { template: '<div>Login</div>' }, meta: { public: true } },
  {
    path: '/',
    component: { template: '<router-view />' },
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: { template: '<div>Dashboard</div>' }, meta: { title: '仪表盘' } },
      { path: 'knowledge-base', name: 'KnowledgeBase', component: { template: '<div>KB</div>' }, meta: { title: '知识库管理' } },
      { path: 'knowledge-base/:id/documents', name: 'KbDocuments', component: { template: '<div>Docs</div>' }, meta: { title: '文档管理' } },
      { path: 'documents', name: 'AllDocuments', component: { template: '<div>AllDocs</div>' }, meta: { title: '全部文档' } },
      { path: 'search', name: 'Search', component: { template: '<div>Search</div>' }, meta: { title: '知识检索' } },
      { path: 'users', name: 'Users', component: { template: '<div>Users</div>' }, meta: { title: '用户管理' } },
      { path: 'logs', name: 'Logs', component: { template: '<div>Logs</div>' }, meta: { title: '操作日志' } }
    ]
  }
]

describe('Router 路由配置', () => {
  let router

  beforeEach(() => {
    localStorage.clear()
    router = createRouter({
      history: createMemoryHistory(),
      routes
    })
    router.beforeEach((to, from, next) => {
      const token = localStorage.getItem('token')
      if (!to.meta.public && !token) {
        next('/login')
      } else {
        next()
      }
    })
  })

  it('未登录访问受保护页面 - 重定向到登录页', async () => {
    await router.push('/dashboard')
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/login')
  })

  it('未登录可以访问登录页', async () => {
    await router.push('/login')
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/login')
  })

  it('已登录可以访问仪表盘', async () => {
    localStorage.setItem('token', 'test-token')
    await router.push('/dashboard')
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/dashboard')
  })

  it('已登录访问根路径 - 重定向到仪表盘', async () => {
    localStorage.setItem('token', 'test-token')
    await router.push('/')
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/dashboard')
  })

  it('路由 meta.title 正确', async () => {
    localStorage.setItem('token', 'test-token')
    await router.push('/knowledge-base')
    await router.isReady()
    expect(router.currentRoute.value.meta.title).toBe('知识库管理')
  })

  it('知识库文档路由 - 带参数', async () => {
    localStorage.setItem('token', 'test-token')
    await router.push('/knowledge-base/5/documents')
    await router.isReady()
    expect(router.currentRoute.value.params.id).toBe('5')
    expect(router.currentRoute.value.name).toBe('KbDocuments')
  })

  it('所有子路由都可访问', async () => {
    localStorage.setItem('token', 'test-token')
    const paths = ['/dashboard', '/knowledge-base', '/documents', '/search', '/users', '/logs']
    for (const path of paths) {
      await router.push(path)
      await router.isReady()
      expect(router.currentRoute.value.path).toBe(path)
    }
  })

  it('登录页是公开路由', () => {
    const loginRoute = routes.find(r => r.path === '/login')
    expect(loginRoute.meta.public).toBe(true)
  })

  it('受保护路由没有 public meta', () => {
    const mainRoute = routes.find(r => r.path === '/')
    const protectedRoutes = mainRoute.children
    for (const route of protectedRoutes) {
      expect(route.meta.public).toBeUndefined()
    }
  })

  it('不存在的路由不会崩溃', async () => {
    localStorage.setItem('token', 'test-token')
    await router.push('/nonexistent')
    await router.isReady()
    // 不应该崩溃
    expect(router.currentRoute.value).toBeDefined()
  })

  it('各路由 meta.title 都已设置', async () => {
    localStorage.setItem('token', 'test-token')
    const mainRoute = routes.find(r => r.path === '/')
    for (const child of mainRoute.children) {
      expect(child.meta.title).toBeDefined()
      expect(child.meta.title.length).toBeGreaterThan(0)
    }
  })
})
