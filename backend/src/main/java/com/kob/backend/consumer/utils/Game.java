package com.kob.backend.consumer.utils;

import java.util.Random;

public class Game {
    private final Integer rows;
    private final Integer cols;
    private final Integer inner_wall_count;
    private final int[][] g;

    final private int[] dx = {-1,0,1,0},dy = {0,1,0,-1};

    public Game(Integer rows, Integer cols, Integer inner_wall_count) {
        this.rows = rows;
        this.cols = cols;
        this.inner_wall_count = inner_wall_count;
        this.g = new int[rows][cols];
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

}
