package com.kob.botrunningsystem.service.impl;

import com.kob.botrunningsystem.service.BotRunningService;
import com.kob.botrunningsystem.service.utils.BotPool;
import org.springframework.stereotype.Service;

@Service
public class BotRunningServiceImpl implements BotRunningService {
    public static final BotPool botPool = new BotPool();


    @Override
    public String addBot(Integer userId, String botCode, String input,Integer status) {
//        System.out.println("add bot:"+userId+ "  " +botCode+ "  " +input);
        botPool.addBot(userId,botCode,input,status);
        return "add bot success";
    }
}
