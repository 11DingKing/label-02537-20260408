import request from './request'

// Auth
export const login = data => request.post('/auth/login', data)
export const getUserInfo = () => request.get('/auth/info')

// Users
export const getUsers = params => request.get('/users', { params })
export const createUser = data => request.post('/users', data)
export const updateUser = (id, data) => request.put(`/users/${id}`, data)
export const deleteUser = id => request.delete(`/users/${id}`)
export const updateUserStatus = (id, status) => request.put(`/users/${id}/status`, null, { params: { status } })

// Knowledge Base
export const getKbList = params => request.get('/kb', { params })
export const getKbDetail = id => request.get(`/kb/${id}`)
export const createKb = data => request.post('/kb', data)
export const updateKb = (id, data) => request.put(`/kb/${id}`, data)
export const deleteKb = id => request.delete(`/kb/${id}`)

// Documents
export const getDocuments = params => request.get('/documents', { params })
export const uploadDocument = (file, kbId) => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('kbId', kbId)
  return request.post('/documents/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })
}
export const deleteDocument = id => request.delete(`/documents/${id}`)
export const reparseDocument = id => request.post(`/documents/${id}/reparse`)
export const getDocChunks = id => request.get(`/documents/${id}/chunks`)
export const getDocPreview = id => request.get(`/documents/${id}/preview`)

// Search
export const searchKnowledge = params => request.get('/search', { params })

// Logs
export const getLogs = params => request.get('/logs', { params })
