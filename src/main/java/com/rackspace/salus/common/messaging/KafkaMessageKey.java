package com.rackspace.salus.common.messaging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the properties of the annotated class that will be used to compose a Kafka message key
 * using {@link KafkaMessageKeyBuilder}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface KafkaMessageKey {

  /**
   * the property names of this class to compose into the Kafka message key
   */
  String[] properties();
}
