package com.kob.backend.service.impl.user.bot;

import com.kob.backend.mapper.BotMapper;
import com.kob.backend.pojo.Bot;
import com.kob.backend.pojo.User;
import com.kob.backend.service.impl.utils.UserDetailsImpl;
import com.kob.backend.service.user.bot.RemoveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class RemoveServiceImpl implements RemoveService {
    @Autowired
    BotMapper botMapper;
    @Override
    public Map<String, String> remove(Map<String, String> data) {
        int botId = Integer.parseInt(data.get("bot_id"));

        UsernamePasswordAuthenticationToken authenticationToken = (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

        UserDetailsImpl loginUser = (UserDetailsImpl) authenticationToken.getPrincipal();
        User user = loginUser.getUser();

        Bot bot = new Bot();
        bot = botMapper.selectById(botId);
        Map<String,String> map = new HashMap<>();
        if(bot == null){
            map.put("error_message","bot不存在或已经被删除");
            return map;
        }
        if(!bot.getUserId().equals(user.getId())){
            map.put("error_message","你没有权限删除该bot");
            return map;
        }
        botMapper.deleteById(botId);
        map.put("error_message","success");
        return map;
    }
}
