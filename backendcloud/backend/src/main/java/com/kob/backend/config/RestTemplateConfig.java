package com.kob.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {           //返回一个工具：在我们的两个bot工程间通信
    //返回一个类，这个类是springframework.web.client.RestTemplate，具有发送http消息的功能
    @Bean
    public RestTemplate getRestTemplate(){
        return new RestTemplate();
    }
}
//当我们需要某个东西的时候我们只需要定一个它的类，然后按照上面的方式进行返回即可