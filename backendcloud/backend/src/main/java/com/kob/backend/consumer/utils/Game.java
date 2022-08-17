package com.kob.backend.consumer.utils;

import com.alibaba.fastjson.JSONObject;
import com.kob.backend.consumer.WebSocketServer;
import com.kob.backend.pojo.Record;

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

    public Game(Integer rows, Integer cols, Integer inner_wall_count,Integer idA,Integer idB) {
        this.rows = rows;
        this.cols = cols;
        this.inner_wall_count = inner_wall_count;
        this.g = new int[rows][cols];
        playerA = new Player(idA,rows-2,1,new ArrayList<>());
        playerB = new Player(idB,1,cols-2,new ArrayList<>());
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

    public void setNextStepA(Integer nextStepA){        //外面需要写入这个值，内部也会调用这个值，所以涉及到了线程同步
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

    private boolean nextStep(){     //等待两名玩家的下一步操作
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

    private void saveToDatabase(){
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
        if(WebSocketServer.users.get(playerA.getId()) != null)
            WebSocketServer.users.get(playerA.getId()).sendMessage(message);
        if(WebSocketServer.users.get(playerB.getId()) != null)
            WebSocketServer.users.get(playerB.getId()).sendMessage(message);
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
                    if(nextStepA == null && nextStepB == null){     //这里可能会有问题就是在nextStep()函数返回false的情况下可能恰好输入，那么我们可以v不处理这个特殊情况
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
