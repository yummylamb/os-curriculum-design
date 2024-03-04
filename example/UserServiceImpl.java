package com.lj.bigEvent.service.impl;

import com.lj.bigEvent.mapper.UserMapper;
import com.lj.bigEvent.pojo.User;
import com.lj.bigEvent.service.UserService;
import com.lj.bigEvent.utils.Md5Util;
import com.lj.bigEvent.utils.ThreadLocalUtil;
import org.apache.ibatis.annotations.Insert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * @author: lj
 * @desc
 * @create: 2024.01.19
 **/
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public User findByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    @Override
    public void register(String username, String password) {
        // 加密
        String md5String = Md5Util.getMD5String(password);
        // 注册
        userMapper.add(username, md5String);
    }

    @Override
    public void update(User user) {
        user.setUpdateTime(LocalDateTime.now());
        userMapper.update(user);
    }

    @Override
    public void updateAvatar(String avatar) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer id = (Integer) claims.get("id");
        userMapper.updateAvatar(avatar, id);
    }

    @Override
    public void updatePwd(String newPwd) {
        // 加密
        String md5String = Md5Util.getMD5String(newPwd);
        // 获取用户id
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer id = (Integer) claims.get("id");
        // 修改密码
        userMapper.updatePwd(md5String, id);
    }
}
