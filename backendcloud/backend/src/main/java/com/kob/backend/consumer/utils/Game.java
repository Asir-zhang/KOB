package com.kob.backend.consumer.utils;

import com.alibaba.fastjson.JSONObject;
import com.kob.backend.consumer.WebSocketServer;
import com.kob.backend.pojo.Bot;
import com.kob.backend.pojo.Record;
import com.kob.backend.pojo.User;
import org.springframework.security.core.parameters.P;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

public class Game extends Thread{
    private final Integer rows;
    private final Integer cols;
    private final Integer inner_wall_count;
    private final int[][] g;        //地图
    private final  int[] dx = {-1,0,1,0},dy = {0,1,0,-1};
    private final Player playerA,playerB;       //规定A是左下角那个位置
    private Integer nextStepA = null;       //下一步操作，与dx、dy对应
    private Integer nextStepB = null;
    private ReentrantLock lock = new ReentrantLock();
    private String status = "playing";      //存储游戏的状态，一开始表示进行中 playing -> finished
    private String loser = "";      //all：平局 A：A输 B：B输
    private static final String addBotUrl = "http://127.0.0.1:34568/bot/add/";        //定义向BotRunningSystem服务发送的地址
    private boolean ifStart = false;

    public Game(Integer rows, Integer cols, Integer inner_wall_count,
                Integer idA, Bot botA,
                Integer idB, Bot botB) {
        this.rows = rows;
        this.cols = cols;
        this.inner_wall_count = inner_wall_count;
        this.g = new int[rows][cols];

        Integer botIdA = -1,botIdB = -1;
        String botCodeA = "",botCodeB = "";
        if(botA != null){       //根据传入的bot信息确定id和code
            botIdA = botA.getId();
            botCodeA = botA.getContent();
        }
        if(botB != null){
            botIdB = botB.getId();
            botCodeB = botB.getContent();
        }
        playerA = new Player(idA,botIdA,botCodeA,rows-2,1,new ArrayList<>());
        playerB = new Player(idB,botIdB,botCodeB,1,cols-2,new ArrayList<>());
    }

    public int[][] getG() {
        return g;
    }

    private boolean checkConnectivity(int sx,int sy,int tx,int ty){     //判断连通性
        if(sx == tx && sy == ty) return true;
        g[sx][sy] = 1;

        for(int i = 0;i < 4;i++){
            int x = sx+dx[i],y = sy+dy[i];
            if(x >= 0 && x < this.rows && y >= 0 && y < this.cols && g[x][y] == 0){
                if(checkConnectivity(x,y,tx,ty)){
                    g[sx][sy] = 0;
                    return true;
                }
            }
        }

        g[sx][sy] = 0;
        return false;
    }

    private boolean draw(){     //画地图
        for(int i = 0;i < this.rows;i++)
            for(int j = 0;j < this.cols;j++){
                g[i][j] = 0;
            }

        for(int i = 0;i < this.rows;i++) g[i][0] = g[i][this.cols-1] = 1;       //画四周
        for(int i = 0;i < this.cols;i++) g[0][i] = g[this.rows-1][i] = 1;       //画四周

        Random random = new Random();
        for(int i = 0;i < this.inner_wall_count>>1;i++){
            for(int j = 0;j < 1000;j++){
                int r = random.nextInt(this.rows);
                int c = random.nextInt(this.cols);

                if(g[r][c] == 1 || g[this.rows-1-r][this.cols-1-c] == 1){       //这个位置已经有了
                    continue;
                }
                if((r == this.rows-2 && c == 1) || (c == this.cols-2 && r == 1)){       //这个位置是蛇的出生地
                    continue;
                }

                g[r][c] = g[this.rows-r-1][this.cols-c-1] = 1;
                break;
            }
        }

        return checkConnectivity(this.rows-2,1,1,this.cols-2);
    }

    public void createMap(){
        for(int i = 0;i < 1000;i++){
            if(draw()){
                break;
            }
        }
    }

    public Player getPlayerA() {
        return playerA;
    }

    public Player getPlayerB() {
        return playerB;
    }

    public void setNextStepA(Integer nextStepA){        //设置下一步操作，外面需要写入这个值，内部也会调用这个值，所以涉及到了线程同步
        lock.lock();
        try{
            this.nextStepA = nextStepA;
        } finally {
            lock.unlock();
        }
    }

    public void setNextStepB(Integer nextStepB){
        lock.lock();
        try{
            this.nextStepB = nextStepB;
        } finally {
            lock.unlock();
        }
    }

    private String getInput(Player player){      //辅助函数，将当前的局面编码成字符串传到BotRunningSystem
        //String格式:
        //地图信息#我的起始x坐标#我的起始y坐标#我的操作序列#对手的起始x坐标#对手的起始y坐标#对手的操作序列
        Player me,you;
        if(playerA.getId().equals(player.getId())){
            me = playerA;
            you = playerB;
        } else {
            me = playerB;
            you = playerA;
        }
        return getMapString()+"#"+
                me.getSx()+"#"+
                me.getSy()+"#A"+        //因为操作序列为空，所以在使用split函数的时候无法达到要求，故加上一个括号
                me.getStepsString()+"A#"+
                you.getSx()+"#"+
                you.getSy()+"#A"+
                you.getStepsString()+"A";
    }

    private void sendBotCode(Player player){        //是否是由bot来执行，是的话就向另一个服务发送执行代码
        if(player.getBotId().equals(-1)) return;        //人亲自操作
        MultiValueMap<String,String> data = new LinkedMultiValueMap<>();
        data.add("user_id",player.getId().toString());
        data.add("bot_code",player.getBotCode());
        data.add("input",getInput(player));
        if(player.getSteps() == null || player.getSteps().isEmpty()){       //如果是空的，表示刚开始
            data.add("status","0");
        } else {
            data.add("status","1");     //否则表示正在走
        }
        WebSocketServer.restTemplate.postForObject(addBotUrl,data,String.class);
    }

    private boolean nextStep(){     //等待两名玩家的下一步操作
//这一步是非常必须的，要不然可能会造成前端接受不了前几步的情况
/*原因详解：前端有TimeOut函数，有2s时间
* */
        if(!ifStart){       //刚刚开始
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            ifStart = true;
        } else {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        sendBotCode(playerA);
        sendBotCode(playerB);

        for(int i = 0;i < 50;i++){
            try{
                Thread.sleep(100);
                lock.lock();
                try{
                    if(nextStepA != null && nextStepB != null){
                        playerA.getSteps().add(nextStepA);
                        playerB.getSteps().add(nextStepB);
                        return true;
                    }
                } finally {
                    lock.unlock();
                }
            } catch (InterruptedException e){
                e.printStackTrace();
            }
        }
        return false;
    }

    private void sendResult(){      //公布结果
        JSONObject resp = new JSONObject();
        resp.put("event","result");
        resp.put("loser",loser);
        sendAllMessage(resp.toJSONString());
        saveToDatabase();
        sendRmContainer(playerA,playerB);
    }

    private void sendRmContainer(Player playerA, Player playerB){       //游戏结束，关闭代码运行容器
        MultiValueMap<String,String> data = new LinkedMultiValueMap<>();
        data.add("user_id",playerA.getId().toString());
        data.add("bot_code",playerA.getBotCode());
        data.add("input",getInput(playerA));
        data.add("status","2");         //2表示结束啦
        WebSocketServer.restTemplate.postForObject(addBotUrl,data,String.class);
        MultiValueMap<String,String> data2 = new LinkedMultiValueMap<>();
        data2.add("user_id",playerB.getId().toString());
        data2.add("bot_code",playerB.getBotCode());
        data2.add("input",getInput(playerB));
        data2.add("status","2");
        WebSocketServer.restTemplate.postForObject(addBotUrl,data2,String.class);
    }

    //判断A是否撞墙，是否撞自己，是否撞B
    private boolean check_valid(List<Cell> cellsA,List<Cell> cellsB){
        int n = cellsA.size();
        Cell cell = cellsA.get(n-1);
        if(g[cell.x][cell.y] == 1) return false;

        for(int i = 0;i < n-1;i++){
            if(cellsA.get(i).x == cell.x && cellsA.get(i).y == cell.y) return false;
        }

        for(int i = 0;i < cellsB.size();i++)
            if(cellsB.get(i).x == cell.x && cellsB.get(i).y == cell.y) return false;

        return true;
    }

    private void judge(){       //判断合法性
        List<Cell> cellsA = playerA.getCells();
        List<Cell> cellsB = playerB.getCells();

        boolean validA = check_valid(cellsA,cellsB);
        boolean validB = check_valid(cellsB,cellsA);

        if(!validA || !validB){
            status = "finished";

            if(!validA && !validB){
                loser = "all";
            } else if(!validA){
                loser = "A";
            } else {
                loser = "B";
            }
        }
    }

    private void sendMove(){        //返回移动信息
        lock.lock();
        try{
            JSONObject resp = new JSONObject();
            resp.put("event","move");
            resp.put("a_direction",nextStepA);
            resp.put("b_direction",nextStepB);
            sendAllMessage(resp.toJSONString());
            nextStepA = nextStepB = null;
        } finally {
             lock.unlock();
        }
    }

    private String getMapString(){      // 将map展开成String保存数据库
        StringBuilder res = new StringBuilder();
        for(int i = 0;i < rows;i++)
            for(int j = 0;j < cols;j++){
                res.append(g[i][j]);
            }
        return res.toString();
    }

    private void updateUserRating(Player player,Integer rating){        //更新天梯积分
        User user = WebSocketServer.userMapper.selectById(player.getId());
        user.setRating(rating);
        WebSocketServer.userMapper.updateById(user);
    }

    private void saveToDatabase(){      //保存对局信息
        //先更新天梯积分
        Integer ratingA = WebSocketServer.userMapper.selectById(playerA.getId()).getRating();
        Integer ratingB = WebSocketServer.userMapper.selectById(playerB.getId()).getRating();

        if("A".equals(loser)){
            ratingA -= 3;
            ratingB += 6;
        } else if("B".equals(loser)){
            ratingA += 6;
            ratingB -= 3;
        }
        updateUserRating(playerA,ratingA);
        updateUserRating(playerB,ratingB);

        Record record = new Record(
                null,
                playerA.getId(),
                playerA.getSx(),
                playerA.getSy(),
                playerB.getId(),
                playerB.getSx(),
                playerB.getSy(),
                playerA.getStepsString(),
                playerB.getStepsString(),
                getMapString(),
                loser,
                new Date()
        );
        WebSocketServer.recordMapper.insert(record);
    }

    private void sendAllMessage(String message){
        if(WebSocketServer.users.get(playerA.getId()) != null){
            WebSocketServer.users.get(playerA.getId()).sendMessage(message);
        }
        if(WebSocketServer.users.get(playerB.getId()) != null){
            WebSocketServer.users.get(playerB.getId()).sendMessage(message);
        }
    }

    @Override
    public void run() {
        for(int i = 0;i < 1000;i ++){       //总的操作步数不会超过一千步
            if(nextStep()){     //下一步操作是否获取到
                judge();
                if(status.equals("playing")){
                    sendMove();
                } else {
                    sendResult();
                    break;
                }
            } else {
                status = "finished";
                //凡是要读或写nextStepA\nextStepB都需要加锁
                lock.lock();
                try{
                    if(nextStepA == null && nextStepB == null){     //这里可能会有问题就是在nextStep()函数返回false的情况下可能恰好输入，那么我们可以不处理这个特殊情况
                        loser = "all";
                    } else if(nextStepA == null){
                        loser = "A";
                    } else {
                        loser = "B";
                    }
                } finally {
                    lock.unlock();
                }
                sendResult();
                break;
            }
        }
    }
}
