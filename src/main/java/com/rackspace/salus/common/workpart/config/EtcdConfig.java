package com.rackspace.salus.common.workpart.config;

import com.coreos.jetcd.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class EtcdConfig {

  @Bean
  public ThreadPoolTaskScheduler workAllocatorTaskScheduler() {
    final ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
    taskScheduler.setPoolSize(Integer.MAX_VALUE);
    taskScheduler.setThreadNamePrefix("watchers-");
    taskScheduler.initialize();

    return taskScheduler;
  }
}