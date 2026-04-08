package com.knowledgebase.service;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledgebase.common.BusinessException;
import com.knowledgebase.dto.UserDTO;
import com.knowledgebase.entity.SysUser;
import com.knowledgebase.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper userMapper;

    public Page<SysUser> page(int pageNum, int pageSize, String keyword) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getNickname, keyword);
        }
        wrapper.orderByDesc(SysUser::getCreatedAt);
        return userMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    public void create(UserDTO dto) {
        SysUser exists = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername()));
        if (exists != null) {
            log.warn("创建用户失败-用户名已存在: username={}", dto.getUsername());
            throw new BusinessException("用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(BCrypt.hashpw(dto.getPassword() != null ? dto.getPassword() : "123456"));
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        user.setStatus(1);
        userMapper.insert(user);
        log.info("创建用户: username={}, userId={}", dto.getUsername(), user.getId());
    }

    public void update(Long id, UserDTO dto) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            log.warn("更新用户失败-用户不存在: id={}", id);
            throw new BusinessException("用户不存在");
        }
        if (StringUtils.hasText(dto.getNickname())) user.setNickname(dto.getNickname());
        if (StringUtils.hasText(dto.getPassword())) {
            user.setPassword(BCrypt.hashpw(dto.getPassword()));
        }
        userMapper.updateById(user);
        log.info("更新用户: id={}, username={}", id, user.getUsername());
    }

    public void delete(Long id) {
        if (id == 1L) throw new BusinessException("不能删除超级管理员");
        userMapper.deleteById(id);
        log.info("删除用户: id={}", id);
    }

    public void updateStatus(Long id, Integer status) {
        if (id == 1L) throw new BusinessException("不能禁用超级管理员");
        SysUser user = new SysUser();
        user.setId(id);
        user.setStatus(status);
        userMapper.updateById(user);
        log.info("更新用户状态: id={}, status={}", id, status);
    }
}
