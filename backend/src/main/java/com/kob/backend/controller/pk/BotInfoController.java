package com.kob.backend.controller.pk;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/pk/")
public class BotInfoController {
    @RequestMapping("get_bot_info/")
    public Map getBotInfo(){
        Map<String,String> bot1 = new HashMap<>();
        bot1.put("name","Dragon");
        bot1.put("rating","1500");
        return bot1;
    }
}
