package com.rackspace.salus.common.workpart.services;

import lombok.extern.slf4j.Slf4j;
import com.rackspace.salus.common.workpart.WorkProcessor;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DefaultWorkProcessor implements WorkProcessor {

  @Override
  public void start(String id, String content) {
    log.info("Starting work on id={}, content={}", id, content);
  }

  @Override
  public void update(String id, String content) {
    log.info("Updating work on id={}, content={}", id, content);
  }

  @Override
  public void stop(String id, String content) {
    log.info("Stopping work on id={}, content={}", id, content);
  }
}
