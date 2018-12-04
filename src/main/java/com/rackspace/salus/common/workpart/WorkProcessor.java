package com.rackspace.salus.common.workpart;

public interface WorkProcessor {

  void start(String id, String content);

  void update(String id, String content);

  void stop(String id, String content);
}
