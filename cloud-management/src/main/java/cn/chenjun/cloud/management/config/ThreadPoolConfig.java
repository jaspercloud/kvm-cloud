package cn.chenjun.cloud.management.config;


import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * @author chenjun
 */
@Configuration
public class ThreadPoolConfig {
    @Bean(destroyMethod = "shutdown")
    @Primary
    public ScheduledExecutorService workExecutorService(@Value("${app.work.thread.size:1}") int size) {
        return new ScheduledThreadPoolExecutor(Math.max(size, 1), new BasicThreadFactory.Builder().namingPattern("executor-pool-%d").daemon(true).build());
    }

}
