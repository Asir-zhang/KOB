package com.kob.backend.consumer.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Player {
    private Integer id;
    private Integer botId;      //为-1的话表示自己玩
    private String botCode;
    private Integer sx;     //起始坐标
    private Integer sy;
    private List<Integer> steps;        //每一步的方向

    private boolean check_tail_increasing(int step){       //判断蛇是否伸长
        if(step <= 8) return true;
        return step%3 == 1;
    }

    public List<Cell> getCells(){
        int[] dx = {-1,0,1,0},dy = {0,1,0,-1};
        List<Cell> res = new ArrayList<>();
        int x = sx,y = sy;
        res.add(new Cell(x,y));
        int step = 0;
        for(int d : steps){
            x += dx[d];
            y += dy[d];
            res.add(new Cell(x,y));
            if(!check_tail_increasing(++ step)){
                res.remove(0);
            }
        }
        return res;
    }

    public String getStepsString(){
        StringBuilder res = new StringBuilder();
        for(int t : steps){
            res.append(t);
        }
        return res.toString();
    }
}
