package com.kob.matchingsystem.service.impl.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class MatchingPool extends Thread{
    //因为run方法执行了一个死循环，所以需要新开一个线程
    private static List<Player> players = new ArrayList<>();
    private ReentrantLock lock = new ReentrantLock();
    private static RestTemplate restTemplate;
    private static final String resultUrl = "http://localhost:8081/pk/start/game/";

    @Autowired
    public void setRestTemplate(RestTemplate restTemplate){
        MatchingPool.restTemplate = restTemplate;
    }

    public void addPlayer(Integer userId,Integer rating){
        //players可以被多个线程读写
        lock.lock();
        try{
            players.add(new Player(userId,rating,0));
        } finally{
            lock.unlock();
        }
    }

    public void removePlayer(Integer userId){       //这个会被“取消等待“调用
        //players可以被多个线程读写
        lock.lock();
        try{
            List<Player> newPlayers = new ArrayList<>();
            for(Player player : players){
                if(!player.getUserId().equals(userId)){
                    newPlayers.add(player);
                }
            }
            players = newPlayers;
        } finally{
            lock.unlock();
        }
    }

    private void increaseWaitingTime(){      //所有玩家等待时间+1
        for(Player player : players){
            player.setWaitingTime(player.getWaitingTime()+1);
        }
    }

    private boolean checkMatched(Player a,Player b){        //判断两者是否匹配
        int ratingDelta = Math.abs(a.getRating()-b.getRating());
        int waitingTime = Math.min(a.getWaitingTime(),b.getWaitingTime());
        return ratingDelta <= waitingTime*10;       //每秒钟增加10的阈值
    }

    private void sendResult(Player a,Player b){      //返回结果
        //System.out.println("send result:"+ a.getUserId() + "   "+b.getUserId());
        MultiValueMap<String,String> data = new LinkedMultiValueMap<>();
        data.put("a_id", Collections.singletonList(a.getUserId().toString()));
        data.put("b_id", Collections.singletonList(b.getUserId().toString()));
        restTemplate.postForObject(resultUrl,data,String.class);
    }

    private void matchPlayers(){    //尝试匹配所有的玩家
        //System.out.println("match players:"+players.toString());
        boolean[] used = new boolean[players.size()];
        for(int i = 0;i < players.size();i++){
            if(used[i]) continue;
            for(int j = i+1;j < players.size();j++){
                Player a = players.get(i);
                Player b = players.get(j);
                if(checkMatched(a,b)){
                    used[i] = used[j] = true;
                    sendResult(a,b);
                    break;
                }
            }
        }
        List<Player> newPlayers = new ArrayList<>();
        for(int i = 0;i < players.size();i++){
            if(!used[i]){
                newPlayers.add(players.get(i));
            }
        }
        players = newPlayers;
    }

    @Override
    public void run() {
        while(true){
            try {
                Thread.sleep(1000);
                lock.lock();
                try{
                    //都涉及到players，所以需要加锁
                    increaseWaitingTime();
                    matchPlayers();
                } finally {
                    lock.unlock();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
