package me.itzg.tryetcdworkpart;

public interface WorkProcessor {

  void start(String id, String content);

  void update(String id, String content);

  void stop(String id, String content);
}
