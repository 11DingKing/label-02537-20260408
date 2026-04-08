package com.knowledgebase.service;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledgebase.BaseTest;
import com.knowledgebase.common.BusinessException;
import com.knowledgebase.dto.UserDTO;
import com.knowledgebase.entity.SysUser;
import com.knowledgebase.mapper.SysUserMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserService 用户服务测试")
class UserServiceTest extends BaseTest {

    @Autowired
    private UserService userService;
    @Autowired
    private SysUserMapper userMapper;

    @BeforeEach
    void setUp() {
        userMapper.delete(null);
    }

    private SysUser insertUser(String username, String nickname) {
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(BCrypt.hashpw("123456"));
        user.setNickname(nickname);
        user.setStatus(1);
        userMapper.insert(user);
        return user;
    }

    @Test
    @DisplayName("分页查询-无关键词")
    void testPage() {
        insertUser("user1", "用户1");
        insertUser("user2", "用户2");
        Page<SysUser> page = userService.page(1, 10, null);
        assertEquals(2, page.getTotal());
    }

    @Test
    @DisplayName("分页查询-关键词搜索")
    void testPageWithKeyword() {
        insertUser("admin", "管理员");
        insertUser("test", "测试用户");
        Page<SysUser> page = userService.page(1, 10, "admin");
        assertEquals(1, page.getTotal());
    }

    @Test
    @DisplayName("分页查询-按昵称搜索")
    void testPageByNickname() {
        insertUser("user1", "管理员");
        insertUser("user2", "测试");
        Page<SysUser> page = userService.page(1, 10, "管理");
        assertEquals(1, page.getTotal());
    }

    @Test
    @DisplayName("创建用户-成功")
    void testCreate() {
        UserDTO dto = new UserDTO();
        dto.setUsername("newuser");
        dto.setPassword("pass123");
        dto.setNickname("新用户");
        userService.create(dto);

        Page<SysUser> page = userService.page(1, 10, null);
        assertEquals(1, page.getTotal());
    }

    @Test
    @DisplayName("创建用户-默认密码")
    void testCreateDefaultPassword() {
        UserDTO dto = new UserDTO();
        dto.setUsername("newuser");
        userService.create(dto);

        SysUser user = userMapper.selectList(null).get(0);
        assertTrue(BCrypt.checkpw("123456", user.getPassword()));
    }

    @Test
    @DisplayName("创建用户-默认昵称为用户名")
    void testCreateDefaultNickname() {
        UserDTO dto = new UserDTO();
        dto.setUsername("newuser");
        userService.create(dto);

        SysUser user = userMapper.selectList(null).get(0);
        assertEquals("newuser", user.getNickname());
    }

    @Test
    @DisplayName("创建用户-用户名重复")
    void testCreateDuplicate() {
        insertUser("admin", "管理员");
        UserDTO dto = new UserDTO();
        dto.setUsername("admin");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.create(dto));
        assertEquals("用户名已存在", ex.getMessage());
    }

    @Test
    @DisplayName("更新用户-修改昵称")
    void testUpdate() {
        SysUser user = insertUser("admin", "管理员");
        UserDTO dto = new UserDTO();
        dto.setNickname("超级管理员");
        userService.update(user.getId(), dto);

        SysUser updated = userMapper.selectById(user.getId());
        assertEquals("超级管理员", updated.getNickname());
    }

    @Test
    @DisplayName("更新用户-修改密码")
    void testUpdatePassword() {
        SysUser user = insertUser("admin", "管理员");
        UserDTO dto = new UserDTO();
        dto.setPassword("newpass");
        userService.update(user.getId(), dto);

        SysUser updated = userMapper.selectById(user.getId());
        assertTrue(BCrypt.checkpw("newpass", updated.getPassword()));
    }

    @Test
    @DisplayName("更新用户-用户不存在")
    void testUpdateNotFound() {
        UserDTO dto = new UserDTO();
        dto.setNickname("xxx");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.update(99999L, dto));
        assertEquals("用户不存在", ex.getMessage());
    }

    @Test
    @DisplayName("删除用户-成功")
    void testDelete() {
        SysUser user = insertUser("todelete", "待删除");
        userService.delete(user.getId());
        // 逻辑删除后查不到
        Page<SysUser> page = userService.page(1, 10, null);
        assertEquals(0, page.getTotal());
    }

    @Test
    @DisplayName("删除用户-不能删除超级管理员")
    void testDeleteSuperAdmin() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.delete(1L));
        assertEquals("不能删除超级管理员", ex.getMessage());
    }

    @Test
    @DisplayName("更新用户状态-成功")
    void testUpdateStatus() {
        SysUser user = insertUser("user1", "用户1");
        userService.updateStatus(user.getId(), 0);
        SysUser updated = userMapper.selectById(user.getId());
        assertEquals(0, updated.getStatus());
    }

    @Test
    @DisplayName("更新用户状态-不能禁用超级管理员")
    void testUpdateStatusSuperAdmin() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.updateStatus(1L, 0));
        assertEquals("不能禁用超级管理员", ex.getMessage());
    }

    @Test
    @DisplayName("分页查询-分页参数正确")
    void testPagePagination() {
        for (int i = 0; i < 15; i++) {
            insertUser("user" + i, "用户" + i);
        }
        Page<SysUser> page1 = userService.page(1, 10, null);
        assertEquals(15, page1.getTotal());
        assertEquals(10, page1.getRecords().size());

        Page<SysUser> page2 = userService.page(2, 10, null);
        assertEquals(5, page2.getRecords().size());
    }

    @Test
    @DisplayName("更新用户-不修改空字段")
    void testUpdateEmptyFields() {
        SysUser user = insertUser("testuser", "原昵称");
        UserDTO dto = new UserDTO();
        // 不设置nickname和password
        userService.update(user.getId(), dto);

        SysUser updated = userMapper.selectById(user.getId());
        assertEquals("原昵称", updated.getNickname());
        assertTrue(BCrypt.checkpw("123456", updated.getPassword()));
    }

    @Test
    @DisplayName("更新用户状态-启用用户")
    void testEnableUser() {
        SysUser user = insertUser("disabled", "禁用用户");
        user.setStatus(0);
        userMapper.updateById(user);

        userService.updateStatus(user.getId(), 1);
        SysUser updated = userMapper.selectById(user.getId());
        assertEquals(1, updated.getStatus());
    }

    @Test
    @DisplayName("创建用户-自定义密码")
    void testCreateWithCustomPassword() {
        UserDTO dto = new UserDTO();
        dto.setUsername("custompass");
        dto.setPassword("mypassword");
        dto.setNickname("自定义密码用户");
        userService.create(dto);

        SysUser user = userMapper.selectList(null).get(0);
        assertTrue(BCrypt.checkpw("mypassword", user.getPassword()));
    }
}
