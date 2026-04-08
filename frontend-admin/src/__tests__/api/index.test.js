import { describe, it, expect, vi, beforeEach } from 'vitest'

// Mock axios
vi.mock('../../api/request', () => {
  const mockRequest = {
    get: vi.fn().mockResolvedValue({ code: 200, data: {} }),
    post: vi.fn().mockResolvedValue({ code: 200, data: {} }),
    put: vi.fn().mockResolvedValue({ code: 200, data: {} }),
    delete: vi.fn().mockResolvedValue({ code: 200, data: {} })
  }
  return { default: mockRequest }
})

import {
  login, getUserInfo,
  getUsers, createUser, updateUser, deleteUser, updateUserStatus,
  getKbList, getKbDetail, createKb, updateKb, deleteKb,
  getDocuments, uploadDocument, deleteDocument, reparseDocument, getDocChunks,
  searchKnowledge, getLogs
} from '../../api'
import request from '../../api/request'

describe('API 接口模块', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('Auth API', () => {
    it('login 调用正确的接口', async () => {
      await login({ username: 'admin', password: '123' })
      expect(request.post).toHaveBeenCalledWith('/auth/login', { username: 'admin', password: '123' })
    })

    it('getUserInfo 调用正确的接口', async () => {
      await getUserInfo()
      expect(request.get).toHaveBeenCalledWith('/auth/info')
    })
  })

  describe('User API', () => {
    it('getUsers 带分页参数', async () => {
      await getUsers({ pageNum: 1, pageSize: 10 })
      expect(request.get).toHaveBeenCalledWith('/users', { params: { pageNum: 1, pageSize: 10 } })
    })

    it('createUser', async () => {
      const data = { username: 'test', password: '123' }
      await createUser(data)
      expect(request.post).toHaveBeenCalledWith('/users', data)
    })

    it('updateUser', async () => {
      await updateUser(1, { nickname: 'new' })
      expect(request.put).toHaveBeenCalledWith('/users/1', { nickname: 'new' })
    })

    it('deleteUser', async () => {
      await deleteUser(5)
      expect(request.delete).toHaveBeenCalledWith('/users/5')
    })

    it('updateUserStatus', async () => {
      await updateUserStatus(3, 0)
      expect(request.put).toHaveBeenCalledWith('/users/3/status', null, { params: { status: 0 } })
    })
  })

  describe('Knowledge Base API', () => {
    it('getKbList', async () => {
      await getKbList({ pageNum: 1, pageSize: 10 })
      expect(request.get).toHaveBeenCalledWith('/kb', { params: { pageNum: 1, pageSize: 10 } })
    })

    it('getKbDetail', async () => {
      await getKbDetail(1)
      expect(request.get).toHaveBeenCalledWith('/kb/1')
    })

    it('createKb', async () => {
      await createKb({ name: 'test', description: 'desc' })
      expect(request.post).toHaveBeenCalledWith('/kb', { name: 'test', description: 'desc' })
    })

    it('updateKb', async () => {
      await updateKb(1, { name: 'updated' })
      expect(request.put).toHaveBeenCalledWith('/kb/1', { name: 'updated' })
    })

    it('deleteKb', async () => {
      await deleteKb(1)
      expect(request.delete).toHaveBeenCalledWith('/kb/1')
    })
  })

  describe('Document API', () => {
    it('getDocuments', async () => {
      await getDocuments({ kbId: 1 })
      expect(request.get).toHaveBeenCalledWith('/documents', { params: { kbId: 1 } })
    })

    it('deleteDocument', async () => {
      await deleteDocument(5)
      expect(request.delete).toHaveBeenCalledWith('/documents/5')
    })

    it('reparseDocument', async () => {
      await reparseDocument(3)
      expect(request.post).toHaveBeenCalledWith('/documents/3/reparse')
    })

    it('getDocChunks', async () => {
      await getDocChunks(2)
      expect(request.get).toHaveBeenCalledWith('/documents/2/chunks')
    })

    it('uploadDocument 使用 multipart/form-data', async () => {
      const file = new File(['content'], 'test.docx', { type: 'application/octet-stream' })
      await uploadDocument(file, 1)
      expect(request.post).toHaveBeenCalled()
      const call = request.post.mock.calls[0]
      expect(call[0]).toBe('/documents/upload')
      expect(call[1]).toBeInstanceOf(FormData)
      expect(call[2].headers['Content-Type']).toBe('multipart/form-data')
      expect(call[2].timeout).toBe(120000)
    })
  })

  describe('Search API', () => {
    it('searchKnowledge', async () => {
      await searchKnowledge({ keyword: 'Spring', kbId: 1, limit: 20 })
      expect(request.get).toHaveBeenCalledWith('/search', { params: { keyword: 'Spring', kbId: 1, limit: 20 } })
    })
  })

  describe('Log API', () => {
    it('getLogs', async () => {
      await getLogs({ pageNum: 1, pageSize: 20 })
      expect(request.get).toHaveBeenCalledWith('/logs', { params: { pageNum: 1, pageSize: 20 } })
    })

    it('getLogs 带关键词', async () => {
      await getLogs({ pageNum: 1, pageSize: 20, keyword: '用户' })
      expect(request.get).toHaveBeenCalledWith('/logs', { params: { pageNum: 1, pageSize: 20, keyword: '用户' } })
    })
  })

  describe('边界情况', () => {
    it('getUsers 无参数', async () => {
      await getUsers({})
      expect(request.get).toHaveBeenCalledWith('/users', { params: {} })
    })

    it('getKbList 无参数', async () => {
      await getKbList({})
      expect(request.get).toHaveBeenCalledWith('/kb', { params: {} })
    })

    it('searchKnowledge 无kbId', async () => {
      await searchKnowledge({ keyword: 'test', limit: 10 })
      expect(request.get).toHaveBeenCalledWith('/search', { params: { keyword: 'test', limit: 10 } })
    })

    it('updateUserStatus 启用', async () => {
      await updateUserStatus(3, 1)
      expect(request.put).toHaveBeenCalledWith('/users/3/status', null, { params: { status: 1 } })
    })
  })
})
