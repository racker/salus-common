package com.rackspace.salus.common.errors;

/**
 * Thrown after catching either an InterruptedException or an ExecutionException when attempting to send data through to Kafka.
 */
public class RuntimeKafkaException extends RuntimeException {

  public RuntimeKafkaException(Exception e) {
    super(e);
  }

}
