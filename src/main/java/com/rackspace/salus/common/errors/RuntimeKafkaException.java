package com.rackspace.salus.common.errors;

/**
 * We are creating this Runtime exception for Kafka so that we can track it and give a relevant message
 * to customers when Kafka is down
 */
public class RuntimeKafkaException extends RuntimeException {

  public RuntimeKafkaException(Exception e) {
    super(e);
  }

}
