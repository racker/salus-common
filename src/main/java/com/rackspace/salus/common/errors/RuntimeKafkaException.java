package com.rackspace.salus.common.errors;

public class RuntimeKafkaException extends RuntimeException {

  public RuntimeKafkaException(Exception e) {
    super(e);
  }

}
