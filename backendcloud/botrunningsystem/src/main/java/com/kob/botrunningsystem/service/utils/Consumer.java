package com.kob.botrunningsystem.service.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class Consumer extends Thread{       //一个Bot的执行对应一个这个线程
    private Bot bot;
    private static RestTemplate restTemplate;
    private static final String receiveBotMoveUrl = "http://127.0.0.1:34567/pk/receive/bot/move/";

    @Autowired
    public void setRestTemplate(RestTemplate restTemplate){ Consumer.restTemplate = restTemplate; }

    public void startTimeout(long timeout,Bot bot){
        this.bot = bot;
        this.start();

        try {
            Thread.sleep(200);
            this.join(timeout);     //让当前线程最多执行timeout时间，这也为什么不用sleep的意义
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            this.interrupt();       //直接中断
        }
    }

    private void copyCode(String id,String name){        //复制字节码文件到容器内部
        String[] cmd1 = new String[]{"sudo","docker","cp",name.substring(0,7)+".java",id+":/usr/src/myapp"};
        String[] cmd2 = new String[]{"sudo","docker","cp",name.substring(0,7)+".sh",id+":/usr/src/myapp"};
        String[] cmd3 = new String[]{"sudo","docker","exec",id,"javac",name};     //编译
        try {
            Runtime.getRuntime().exec(cmd1,null,new File("/home/lighthouse/docker/docker_botrunning/codes"));
            Runtime.getRuntime().exec(cmd2,null,new File("/home/lighthouse/docker/docker_botrunning/codes"));
            Process p = Runtime.getRuntime().exec(cmd3,null,new File("/home/lighthouse/docker/docker_botrunning/codes"));
            p.waitFor();
//            System.out.println("复制文件到容器");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private String startContainer(String name){        //启动容器,最后返回容器ID
        Process p;
        String id = null;
        String[] cmds0 = new String[6];
        cmds0[0] = "sudo";   cmds0[1] = "docker";   cmds0[2] = "run";           //启动容器
        cmds0[3] = "-dit";   cmds0[4] = "botrun:v1";   cmds0[5] = "/bin/bash";
        try {
            p = Runtime.getRuntime().exec(cmds0);
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            id = br.readLine();
//            System.out.println("启动容器："+id);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return id;
    }

    private String firstExcute(){             //一开始执行时，先创建字节码文件,返回类文件名字,创建运行脚本,脚本和类文件名相同
        UUID uuid = UUID.randomUUID();
        String uid = uuid.toString().substring(0,4);
        String name = "Bot"+uid+".java";        //定义类文件名字
        String head = "public class "+name.substring(0,7)+"{"+
                "    public static void main(String[] args) {" +
                "        Scanner in = new Scanner(System.in);" +
                "        Main m = new Main();"+
                "        String input = in.next();" +
                "        System.out.println(m.nextStep(input));" +
                "    }";
        String tail = "}";       //最后一行
        int k = bot.getBotCode().indexOf("class Main");
        String codeImport = bot.getBotCode().substring(0,k);        //获取用户代码的导入的包信息
        String codeSource = "static "+bot.getBotCode().substring(k);          //获取用户的源代码
        String code = "import java.util.Scanner;"+codeImport+head+codeSource+tail;       //最终的代码
        String shell = "echo $1 | java "+name.substring(0,7);
        String sourcePath = "/home/lighthouse/docker/docker_botrunning/codes/";

        try {
            OutputStream buildFile = new FileOutputStream(sourcePath+name);       //创建类文件
            buildFile.write(code.getBytes(StandardCharsets.UTF_8));
            buildFile.close();
            OutputStream buildShell = new FileOutputStream(sourcePath+name.substring(0,7)+".sh");       //创建脚本
            buildShell.write(shell.getBytes(StandardCharsets.UTF_8));
            buildShell.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return name;
    }

    private Integer getDirection(Player player){     //更新.sh文件，输入新的input
        String name = player.getJavaName();
        //botrun.sh
        String[] cmd = new String[]{"/bin/bash","-c","sudo bash botrun.sh "+player.containerId+" "+player.javaName.substring(0,7)+".sh"+" "+bot.getInput()};

        Integer direction = -1;
        try {
            Process p = Runtime.getRuntime().exec(cmd,null,new File("/home/lighthouse/docker/docker_botrunning/codes"));    //输入input获取方向
            p.waitFor();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
//            BufferedReader br2 = new BufferedReader(new InputStreamReader(p.getErrorStream()));
//            System.out.println("错误是:"+br2.readLine());
            String sd = br.readLine();
            if(sd == null) {
//                System.out.println("获取的方向为null，返回-1");
                return -1;
            } else{
                direction = Integer.parseInt(sd);
//                System.out.println("获取的方向为："+direction);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return direction;
    }

    private void end(Player player){     //关闭容器,删除本机相关文件
        String id = player.getContainerId();
        String name = player.getJavaName();

        File file1 = new File("/home/lighthouse/docker/docker_botrunning/codes/"+name);
        File file2 = new File("/home/lighthouse/docker/docker_botrunning/codes/"+name.substring(0,7)+".sh");
        if(file1.exists()) file1.delete();
        if(file2.exists()) file2.delete();      //删除本机文件

        String[] cmd2 = new String[]{"sudo","docker","stop",id};      //停止容器

        try {
            Process p2 = Runtime.getRuntime().exec(cmd2);
            p2.waitFor();
            this.join(2000);
            System.out.println("end执行");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        PlayerPool.players.remove(player);      //将这个player移除
    }

    @Override
    public void run() {
        Integer direction = 0;
        if(bot.getStatus() == 0){       //刚刚开始
            String name = firstExcute();        //一开始执行时，先创建类文件,返回类文件名字
            String id = startContainer(name);   //启动容器,最后返回容器ID
            copyCode(id,name);      //复制 .java 到容器里面
            Player player = new Player(bot.getUserId(),id,name);
            PlayerPool.players.put(bot.getUserId(),player);
            direction = getDirection(player);     //获取下一步
        } else if(bot.getStatus() == 1){
            Player player = PlayerPool.players.get(bot.getUserId());
            direction = getDirection(player);     //获取下一步
        } else if(bot.getStatus() == 2){        //结束了
            Player player = PlayerPool.players.get(bot.getUserId());
            end(player);        //关闭容器,删除本机java和字节码文件
        }
        if(bot.getStatus() != 2){
            MultiValueMap<String,String> data = new LinkedMultiValueMap<>();
            data.add("user_id",bot.getUserId().toString());
            data.add("direction",direction.toString());
            restTemplate.postForObject(receiveBotMoveUrl,data,String.class);
        }
    }
}
