package com.kob.botrunningsystem.service.utils;

import com.kob.botrunningsystem.utils.BotInterface;
import org.joor.Reflect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

//@Configuration
@Component
public class Consumer extends Thread{       //一个Bot的执行对应一个这个线程
    private Bot bot;
    private static RestTemplate restTemplate;
    private static final String receiveBotMoveUrl = "http://127.0.0.1:8081/pk/receive/bot/move/";

    @Autowired
    public void setRestTemplate(RestTemplate restTemplate){ Consumer.restTemplate = restTemplate; }

    public void startTimeout(long timeout,Bot bot){
        this.bot = bot;
        this.start();

        try {
            Thread.sleep(200);
            this.join(timeout);     //让当前线程最多执行timeout时间，这也为什么不用sleep的意义
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            this.interrupt();       //直接中断
        }
    }

    private String addUid(String code,String uid){      //在类名前面加上uid
        int k = code.indexOf(" implements com.kob.botrunningsystem.utils.BotInterface");
        return code.substring(0,k)+uid+code.substring(k);
    }

    @Override
    public void run() {
        UUID uuid = UUID.randomUUID();      //因为joor遇到重名的只会编译一次，所以需要一个不同的字符串拼接到后面
        String uid = uuid.toString().substring(0,8);
        BotInterface botInterface = Reflect.compile(
                "com.kob.botrunningsystem.BotsCode.Bot"+uid,
                addUid(bot.getBotCode(),uid)
        ).create().get();

        Integer direction = botInterface.nextMove(bot.getInput());
        //System.out.println("move"+bot.getUserId() + " ^ "+botInterface.nextMove(bot.getInput()));

        MultiValueMap<String,String> data = new LinkedMultiValueMap<>();
        data.add("user_id",bot.getUserId().toString());
        data.add("direction",direction.toString());
        restTemplate.postForObject(receiveBotMoveUrl,data,String.class);
    }
}
