package com.lj.bigEvent.controller;

import com.lj.bigEvent.pojo.Result;
import com.lj.bigEvent.pojo.User;
import com.lj.bigEvent.service.UserService;
import com.lj.bigEvent.utils.JwtUtil;
import com.lj.bigEvent.utils.Md5Util;
import com.lj.bigEvent.utils.ThreadLocalUtil;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.URL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author: lj
 * @desc
 * @create: 2024.01.19
 **/
@RestController
@RequestMapping("/user")
@Validated
public class UserController {

    @Autowired
    private UserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // 注册用户
    @PostMapping("/register")
    public Result register(@Pattern(regexp = "^\\S{5,16}$") String username, @Pattern(regexp = "^\\S{5,16}$")String password) {
        // 查询用户
        User u = userService.findByUsername(username);
        if (u == null) {
            // 没有占用
            // 注册
            userService.register(username, password);
            return Result.success();
        } else return Result.error("用户名已被占用");
    }

    // 用户登录
    @PostMapping("/login")
    public Result<String> login(@Pattern(regexp = "^\\S{5,16}$") String username, @Pattern(regexp = "^\\S{5,16}$") String password){
        // 根据用户名查询用户
        User user = userService.findByUsername(username);
        // 判断该用户是否存在
        if(user == null){
            // 用户不存在
            return Result.error("用户名错误");
        }
        // 判断密码是否相同，数据库中存储的密码是密文的
        String md5String = Md5Util.getMD5String(password);
        if(md5String.equals(user.getPassword())){
            // 登录成功
            Map<String, Object> claims = new HashMap<>();
            claims.put("id", user.getId());
            claims.put("username", user.getUsername());
            String token = JwtUtil.genToken(claims);
            // 把token存储到redis中
            ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
            operations.set(token, token, 1, TimeUnit.HOURS);
            return Result.success(token);
        }
        else return Result.error("密码错误");
    }

    // 根据用户名查询用户
    @GetMapping("/userInfo")
    public Result<User> userInfo(){
        Map<String, Object> claims = ThreadLocalUtil.get();
        String username = (String) claims.get("username");
        User user = userService.findByUsername(username);
        return Result.success(user);
    }

    // 修改用户信息
    @PutMapping("/update")
    public Result update(@RequestBody @Validated User user){
        userService.update(user);
        return Result.success("更新成功");
    }

    // 修改用户头像
    @PatchMapping("/updateAvatar")
    public Result updateAvatar(@RequestParam @URL String avatarUrl){
        System.out.println(avatarUrl);
        userService.updateAvatar(avatarUrl);
        return Result.success();
    }

    // 修改用户密码
    @PatchMapping("/updatePwd")
    public Result updatePwd(@RequestBody Map<String, String> params, @RequestHeader("Authorization") String token){
        // 校验参数
        String oldPwd = params.get("oldPwd");
        String newPwd = params.get("newPwd");
        String rePwd = params.get("rePwd");
        if(!StringUtils.hasLength(oldPwd) || !StringUtils.hasLength(newPwd) || !StringUtils.hasLength(rePwd))
            return Result.error("缺少必要的参数");
        // 原密码是否正确
        Map<String, Object> map = ThreadLocalUtil.get();
        User loginUser = userService.findByUsername((String) map.get("username"));
        String password = loginUser.getPassword();
        if(!password.equals(Md5Util.getMD5String(oldPwd)))
            return Result.error("原密码不正确");
        // 两次填写的新密码是否正确
        if(!newPwd.equals(rePwd))
            return Result.error("两次填写的新密码不一致");
        // 调用service完成密码更新
        userService.updatePwd(newPwd);
        // 删除redis中的token
        ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
        RedisOperations<String, String> redisOperations = operations.getOperations();
        redisOperations.delete(token);
        return Result.success();
    }
}
