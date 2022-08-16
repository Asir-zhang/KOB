package com.kob.backend.consumer.utils;

import com.kob.backend.utils.JwtUtil;
import io.jsonwebtoken.Claims;

//工具类，根据token返回userId
public class JwtAuthentication {
    public static Integer getUserId(String token){
        String userId = "-1";
        //这里是直接调用了JwtAuthenticationTokenFilter中的代码段
        try {
            Claims claims = JwtUtil.parseJWT(token);
            userId = claims.getSubject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return Integer.parseInt(userId);
    }
}
