package com.kob.backend.consumer;

import com.alibaba.fastjson.JSONObject;
import com.kob.backend.consumer.utils.Game;
import com.kob.backend.consumer.utils.JwtAuthentication;
import com.kob.backend.mapper.BotMapper;
import com.kob.backend.mapper.RecordMapper;
import com.kob.backend.mapper.UserMapper;
import com.kob.backend.pojo.Bot;
import com.kob.backend.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ServerEndpoint("/websocket/{token}")  // 注意不要以'/'结尾
public class WebSocketServer {
    //因为Game.sendResult等方法需要广播i信息，所以这里需要设置为public
    public static final ConcurrentHashMap<Integer,WebSocketServer> users = new ConcurrentHashMap<>();      //当前连接信息
    private User user;
    private Session session = null;         //每个链接用这个维护
    private static UserMapper userMapper;
    public Game game = null;
    public static RecordMapper recordMapper;
    private static BotMapper botMapper;
    public static RestTemplate restTemplate;
    private static final String addPlayerUrl = "http://localhost:8082/player/add/";
    private static final String removePlayerUrl = "http://localhost:8082/player/remove/";

    @Autowired
    public void setUserMapper(UserMapper userMapper){       //这一步是为了解决spring容器的单例模式
        WebSocketServer.userMapper = userMapper;
    }

    @Autowired
    public void setBotMapper(BotMapper botMapper){       //这一步是为了解决spring容器的单例模式
        WebSocketServer.botMapper = botMapper;
    }

    @Autowired
    public void setRecordMapper(RecordMapper recordMapper){       //这一步是为了解决spring容器的单例模式
        WebSocketServer.recordMapper = recordMapper;
    }

    @Autowired
    public void setRestTemplate(RestTemplate restTemplate){       //这一步是为了解决spring容器的单例模式
        WebSocketServer.restTemplate = restTemplate;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("token") String token) throws IOException {
        // 建立连接时调用
        this.session = session;
        //System.out.println("connected");
        //假设token是userId
        int userId = JwtAuthentication.getUserId(token);
        this.user = userMapper.selectById(userId);

        if(user != null){
            users.put(userId,this);
            //System.out.println(user);
        } else {
            this.session.close();
        }
    }

    @OnClose
    public void onClose() {
        // 关闭链接时调用
        //System.out.println("disconnected");
        if(this.user != null){
            users.remove(this.user.getId());
        }
    }

    public static void startGame(Integer aId,Integer aBotId,Integer bId,Integer bBotId){
        User a = userMapper.selectById(aId);
        User b = userMapper.selectById(bId);

        //取出两个bot
        Bot botA = botMapper.selectById(aBotId);
        Bot botB = botMapper.selectById(bBotId);

        Game game = new Game(13,14,20,
                a.getId(),botA,
                b.getId(),botB);
        game.createMap();
        if(users.get(a.getId()) != null)
            users.get(a.getId()).game = game;       //获取对应的WebSocket连接
        if(users.get(b.getId()) != null)
            users.get(b.getId()).game = game;       //获取对应的WebSocket连接
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
        if(users.get(a.getId()) != null)
            users.get(a.getId()).sendMessage(respA.toJSONString());

        JSONObject respB = new JSONObject();
        respB.put("event","start-matching");
        respB.put("opponent_username",a.getUsername());
        respB.put("opponent_photo",a.getPhoto());
        respB.put("game",respGame);
        if(users.get(b.getId()) != null)
            users.get(b.getId()).sendMessage(respB.toJSONString());
    }

    private void startMatching(Integer botId){
        //System.out.println("Start Matching");
        MultiValueMap<String,String> data = new LinkedMultiValueMap<>();
        data.add("user_id",this.user.getId().toString());
        data.add("rating",this.user.getRating().toString());
        data.add("bot_id",botId.toString());
        restTemplate.postForObject(addPlayerUrl,data,String.class);
    }

    private void stopMatching(){
        //System.out.println("Stop Matching");
        MultiValueMap<String,String> data = new LinkedMultiValueMap<>();
        data.add("user_id",this.user.getId().toString());
        restTemplate.postForObject(removePlayerUrl,data,String.class);
    }

    private void move(int direction){
        if(game.getPlayerA().getId().equals(user.getId())){     //A玩家移动
            if(game.getPlayerA().getBotId().equals(-1))     //表示人工操作
                game.setNextStepA(direction);       //才接受前端的输入
        } else if(game.getPlayerB().getId().equals(user.getId())){
            if(game.getPlayerB().getBotId().equals(-1))
                game.setNextStepB(direction);
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {        //一般将其当作一个路由
        // 从Client接收消息时调用
        //System.out.println("receive message");
        JSONObject data = JSONObject.parseObject(message);      //通过对象来得到JSON
        String event = data.getString("event");
        if("start-matching".equals(event)){
            startMatching(data.getInteger("bot_id"));
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
