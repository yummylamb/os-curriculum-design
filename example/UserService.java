package com.lj.bigEvent.service;

import com.lj.bigEvent.pojo.User;

/**
 * @author: lj
 * @desc
 * @create: 2024.01.19
 **/
public interface UserService {
    // 根据用户名查询用户
    User findByUsername(String username);

    // 注册
    void register(String username, String password);

    // 更新基本信息
    void update(User user);

    // 更新头像
    void updateAvatar(String avatar);

    void updatePwd(String newPwd);

}
