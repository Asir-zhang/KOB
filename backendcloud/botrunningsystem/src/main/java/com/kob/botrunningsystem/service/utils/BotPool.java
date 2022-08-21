package com.kob.botrunningsystem.service.utils;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class BotPool extends Thread{
    //这里有两个可能的想法：用消息队列或者阻塞队列？
    //使用较多的消息队列有ActiveMQ，RabbitMQ，ZeroMQ，Kafka，MetaMQ，RocketMQ等
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();      //用来阻塞和唤醒
    private final Queue<Bot> bots = new LinkedList<>();       //我们会自己管理成线程安全

    private void consume(Bot bot){      //消费这个任务
        //以后想扩展支持更多语言，或者想要更加安全的话（放进一个沙箱），可以将这里的代码改为Docker的执行
        //这里实现的是简单版，用joor动态编译执行java代码，另外为了让代码的运行时间可控，需要新开一个线程
        Consumer consumer = new Consumer();
        consumer.startTimeout(2000,bot);        //一个bot只让他执行2s
    }

    public void addBot(Integer userId,String botCode,String input){
        lock.lock();
        try{
            bots.add(new Bot(userId,botCode,input));
            condition.signalAll();      //加入了一个，就要唤醒了
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void run() {
        while(true){
            lock.lock();
            if(bots.isEmpty()){
                try {
                    condition.await();      //这个方法内部包含了一个释放锁的操作
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    lock.unlock();
                    break;
                }
            } else {
                Bot bot = bots.remove();
                lock.unlock();
                consume(bot);       //比较耗时，所以放在解锁后面，一旦用完bots之后，就与它没关系了，所以要及时解锁
            }
        }
    }
}
