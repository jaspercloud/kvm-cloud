package cn.roamblue.cloud.management.servcie;

import cn.roamblue.cloud.common.bean.NotifyInfo;
import cn.roamblue.cloud.management.util.RedisKeyUtil;
import org.redisson.api.RLock;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.MessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * @author chenjun
 */
@Service
public class NotifyService implements CommandLineRunner, MessageListener<NotifyInfo> {
    @Autowired
    private RedissonClient redissonClient;

    private RTopic topic;

    @Override
    public void run(String... args) throws Exception {
        topic= redissonClient.getTopic(RedisKeyUtil.GLOBAL_NOTIFY_KET);
        topic.addListener(NotifyInfo.class,this);
    }

    @Override
    public void onMessage(CharSequence channel, NotifyInfo msg) {
        RLock rLock=redissonClient.getReadWriteLock(RedisKeyUtil.GLOBAL_LOCK_KEY).readLock();
        try{
            if(rLock.tryLock(1, TimeUnit.MINUTES)){
                System.out.println(msg.toString());
            }
        }catch (Exception err){

        }finally {
            try {
                if (rLock.isHeldByCurrentThread()) {
                    rLock.unlock();
                }
            }catch (Exception err){

            }
        }
    }
    public void publish(NotifyInfo notify){
        this.topic.publish(notify);
    }
}
