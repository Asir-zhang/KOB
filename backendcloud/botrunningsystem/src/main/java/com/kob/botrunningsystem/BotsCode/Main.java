package com.kob.botrunningsystem.BotsCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

class Main {
    static class Cell{
        public int x,y;
        public Cell(int x,int y){
            this.x = x;
            this.y = y;
        }
    }

    public int nextStep(String input){
        String[] strs = input.split("#");
        int[][] g = new int[13][14];
        for(int i = 0,k = 0;i < 13;i++)
            for(int j = 0;j < 14;j++,k++){
                if(strs[0].charAt(k) == '1'){
                    g[i][j] = 1;
                }
            }
        int aSx = Integer.parseInt(strs[1]),aSy = Integer.parseInt(strs[2]);
        int bSx = Integer.parseInt(strs[4]),bSy = Integer.parseInt(strs[5]);
        String aSteps = strs[3].substring(1,strs[3].length()-1),bSteps = strs[6].substring(1,strs[6].length()-1);

        List<Main.Cell> aCells = getCells(aSx,aSy,aSteps);
        List<Main.Cell> bCells = getCells(bSx,bSy,bSteps);

        for(Main.Cell c:aCells) g[c.x][c.y] = 1;
        for(Main.Cell c:bCells) g[c.x][c.y] = 1;

        int[] dx = {-1,0,1,0},dy = {0,1,0,-1};

        for(int i:getI()){
            int x = aCells.get(aCells.size()-1).x+dx[i];
            int y = aCells.get(aCells.size()-1).y+dy[i];
            if(x >= 0 && x < 13 && y >= 0 && y < 14 && g[x][y] == 0){
                return i;
            }
        }
        return 0;
    }

    private boolean check_tail_increasing(int step){       //判断蛇是否伸长
        if(step <= 8) return true;
        return step%3 == 1;
    }

    public List<Main.Cell> getCells(int sx, int sy, String steps){
        int[] dx = {-1,0,1,0},dy = {0,1,0,-1};
        List<Main.Cell> res = new ArrayList<>();
        int x = sx,y = sy;
        res.add(new Main.Cell(x,y));
        int step = 0;
        for(int i = 0;i < steps.length();i++){
            int d = steps.charAt(i)-'0';
            x += dx[d];
            y += dy[d];
            res.add(new Main.Cell(x,y));
            if(!check_tail_increasing(++ step)){
                res.remove(0);
            }
        }
        return res;
    }

    private int[] getI(){
        int[] arr = {0,1,2,3};
        Random random = new Random();
        for(int i = 0;i < 10;i++){
            int x = random.nextInt(4);
            int y = random.nextInt(4);
            swap(arr,x,y);
        }
        return arr;
    }

    private void swap(int[] arr,int i,int j){
        int temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }
}
