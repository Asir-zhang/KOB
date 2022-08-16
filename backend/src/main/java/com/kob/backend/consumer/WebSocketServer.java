package com.kob.backend.consumer;

import com.alibaba.fastjson.JSONObject;
import com.kob.backend.consumer.utils.Game;
import com.kob.backend.consumer.utils.JwtAuthentication;
import com.kob.backend.mapper.RecordMapper;
import com.kob.backend.mapper.UserMapper;
import com.kob.backend.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
@ServerEndpoint("/websocket/{token}")  // 注意不要以'/'结尾
public class WebSocketServer {
    //因为Game.sendResult等方法需要广播i信息，所以这里需要设置为public
    public static final ConcurrentHashMap<Integer,WebSocketServer> users = new ConcurrentHashMap<>();      //当前连接信息
    private static final CopyOnWriteArraySet<User> matchPool = new CopyOnWriteArraySet<>();       //匹配池
    private User user;
    private Session session = null;         //每个链接用这个维护
    private static UserMapper userMapper;
    private Game game = null;
    public static RecordMapper recordMapper;

    @Autowired
    public void setUserMapper(UserMapper userMapper){       //这一步是为了解决spring容器的单例模式
        WebSocketServer.userMapper = userMapper;
    }

    @Autowired
    public void setUserMapper(RecordMapper recordMapper){       //这一步是为了解决spring容器的单例模式
        WebSocketServer.recordMapper = recordMapper;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("token") String token) throws IOException {
        // 建立连接时调用
        this.session = session;
        System.out.println("connected");
        //假设token是userId
        int userId = JwtAuthentication.getUserId(token);
        this.user = userMapper.selectById(userId);

        if(user != null){
            users.put(userId,this);
            System.out.println(user);
        } else {
            this.session.close();
        }
    }

    @OnClose
    public void onClose() {
        // 关闭链接时调用
        System.out.println("disconnected");
        if(this.user != null){
            users.remove(this.user.getId());
            matchPool.remove(this.user);
        }
    }

    private void startMatching(){
        System.out.println("Start Matching");
        matchPool.add(this.user);

        while(matchPool.size() >= 2){
            Iterator<User> it = matchPool.iterator();
            User a = it.next();
            User b = it.next();
            matchPool.remove(a);
            matchPool.remove(b);

            Game game = new Game(13,14,20,a.getId(),b.getId());
            game.createMap();
            users.get(a.getId()).game = game;       //获取对应的WebSocket连接
            users.get(b.getId()).game = game;
            game.start();

            JSONObject respGame = new JSONObject();
            respGame.put("a_id",game.getPlayerA().getId());
            respGame.put("b_id",game.getPlayerB().getId());
            respGame.put("a_sx",game.getPlayerA().getSx());
            respGame.put("a_sy",game.getPlayerB().getSy());
            respGame.put("b_sx",game.getPlayerB().getSx());
            respGame.put("b_xy",game.getPlayerB().getSy());
            respGame.put("map",game.getG());

            JSONObject respA = new JSONObject();
            respA.put("event","start-matching");
            respA.put("opponent_username",b.getUsername());
            respA.put("opponent_photo",b.getPhoto());
            respA.put("game",respGame);
            users.get(a.getId()).sendMessage(respA.toJSONString());

            JSONObject respB = new JSONObject();
            respB.put("event","start-matching");
            respB.put("opponent_username",a.getUsername());
            respB.put("opponent_photo",a.getPhoto());
            respB.put("game",respGame);
            users.get(b.getId()).sendMessage(respB.toJSONString());
        }
    }

    private void stopMatching(){
        System.out.println("Stop Matching");
        matchPool.remove(this.user);
    }

    private void move(int direction){
        if(game.getPlayerA().getId().equals(user.getId())){     //A玩家移动
            game.setNextStepA(direction);
        } else if(game.getPlayerB().getId().equals(user.getId())){
            game.setNextStepB(direction);
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {        //一般将其当作一个路由
        // 从Client接收消息时调用
        System.out.println("receive message");
        JSONObject data = JSONObject.parseObject(message);      //通过对象来得到JSON
        String event = data.getString("event");
        if("start-matching".equals(event)){
            startMatching();
        } else if("stop-matching".equals(event)){
            stopMatching();
        } else if("move".equals(event)){
            move(data.getInteger("direction"));
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        error.printStackTrace();
    }

    public void sendMessage(String message){
        synchronized (this.session){
            try{
                this.session.getBasicRemote().sendText(message);
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
    }
}
