package com.kob.backend.service.impl.user.account;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kob.backend.mapper.UserMapper;
import com.kob.backend.pojo.User;
import com.kob.backend.service.user.account.RegisterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RegisterServiceImpl implements RegisterService {

    @Autowired
    UserMapper userMapper;
    @Override
    public Map<String, String> register(String username, String password, String confirmedPassword) {
        Map<String,String> map = new HashMap<>();
        if(username == null){
            map.put("error_message","用户名不能为空！");
            return map;
        }
        if(password == null || confirmedPassword == null){
            map.put("error_message","密码不能为空！");
            return map;
        }
        username = username.trim();
        if(username.length() == 0){
            map.put("error_message","用户名不能为空!");
            return map;
        }
        if(password.length() == 0 || confirmedPassword.length() == 0){
            map.put("error_message","密码长度不能为零!");
            return map;
        }
        if(username.length() > 100){
            map.put("error_message","用户名长度不能大于一百!");
            return map;
        }
        if(password.length() > 100 || confirmedPassword.length() > 100){
            map.put("error_message","密码长度不能大于一百!");
            return map;
        }
        if(!password.equals(confirmedPassword)){
            map.put("error_message","两次密码不一样!");
            return map;
        }

        QueryWrapper<User> queryWrapper = new QueryWrapper<User>();
        queryWrapper.eq("username",username);
        List<User> list = userMapper.selectList(queryWrapper);
        if(list.size() != 0){
            map.put("error_message","用户已存在");
            return map;
        }

        String photo = "https://cdn.acwing.com/media/user/profile/photo/179903_lg_df6583e8cd.png";
        User user = new User(null,username,new BCryptPasswordEncoder().encode(password),photo);
        userMapper.insert(user);

        map.put("error_message","success");
        return map;
    }
}
