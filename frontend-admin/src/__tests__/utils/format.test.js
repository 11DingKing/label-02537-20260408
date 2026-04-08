import { describe, it, expect } from 'vitest'

// 从 Documents.vue 提取的工具函数进行独立测试
const formatSize = bytes => {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) { size /= 1024; i++ }
  return size.toFixed(1) + ' ' + units[i]
}

const parseStatusText = s => ['待解析', '解析中', '已完成', '解析失败'][s] || '未知'
const parseStatusClass = s => ['pending', 'parsing', 'success', 'failed'][s] || ''

const highlightKeyword = (text, keyword) => {
  if (!keyword || !text) return text
  const escaped = keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return text.replace(new RegExp(`(${escaped})`, 'gi'), '<mark>$1</mark>')
}

describe('格式化工具函数', () => {
  describe('formatSize - 文件大小格式化', () => {
    it('0 字节', () => expect(formatSize(0)).toBe('0 B'))
    it('null 值', () => expect(formatSize(null)).toBe('0 B'))
    it('字节', () => expect(formatSize(500)).toBe('500.0 B'))
    it('KB', () => expect(formatSize(1024)).toBe('1.0 KB'))
    it('MB', () => expect(formatSize(1048576)).toBe('1.0 MB'))
    it('GB', () => expect(formatSize(1073741824)).toBe('1.0 GB'))
    it('1.5 MB', () => expect(formatSize(1572864)).toBe('1.5 MB'))
  })

  describe('parseStatusText - 解析状态文本', () => {
    it('待解析', () => expect(parseStatusText(0)).toBe('待解析'))
    it('解析中', () => expect(parseStatusText(1)).toBe('解析中'))
    it('已完成', () => expect(parseStatusText(2)).toBe('已完成'))
    it('解析失败', () => expect(parseStatusText(3)).toBe('解析失败'))
    it('未知状态', () => expect(parseStatusText(99)).toBe('未知'))
    it('undefined', () => expect(parseStatusText(undefined)).toBe('未知'))
  })

  describe('parseStatusClass - 解析状态CSS类', () => {
    it('pending', () => expect(parseStatusClass(0)).toBe('pending'))
    it('parsing', () => expect(parseStatusClass(1)).toBe('parsing'))
    it('success', () => expect(parseStatusClass(2)).toBe('success'))
    it('failed', () => expect(parseStatusClass(3)).toBe('failed'))
    it('未知', () => expect(parseStatusClass(99)).toBe(''))
  })

  describe('highlightKeyword - 关键词高亮', () => {
    it('正常高亮', () => {
      expect(highlightKeyword('Hello World', 'World')).toBe('Hello <mark>World</mark>')
    })
    it('大小写不敏感', () => {
      expect(highlightKeyword('Hello WORLD', 'world')).toBe('Hello <mark>WORLD</mark>')
    })
    it('空关键词返回原文', () => {
      expect(highlightKeyword('Hello', '')).toBe('Hello')
    })
    it('空文本返回原文', () => {
      expect(highlightKeyword('', 'test')).toBe('')
    })
    it('null 安全', () => {
      expect(highlightKeyword(null, 'test')).toBeNull()
    })
    it('特殊正则字符转义', () => {
      expect(highlightKeyword('price is $100', '$100')).toBe('price is <mark>$100</mark>')
    })
    it('多次匹配', () => {
      expect(highlightKeyword('ab ab ab', 'ab')).toBe('<mark>ab</mark> <mark>ab</mark> <mark>ab</mark>')
    })
  })
})
