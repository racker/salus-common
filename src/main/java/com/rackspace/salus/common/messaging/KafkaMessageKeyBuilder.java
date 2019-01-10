package com.rackspace.salus.common.messaging;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.util.Assert;

/**
 * This utility class supports building Kafka message keys from objects annotated with
 * {@link KafkaMessageKey}.
 * <p>
 *   Note that this is quite similar in concept to the <code>message-key-expression</code>
 *   supported by <a href="https://docs.spring.io/spring-kafka/reference/html/_spring_integration.html#si-outbound">Spring Kafka's Outbound Channel Adapter</a>.
 *   In contrast, this processor is purposely simpler and more focused on building strings suitable
 *   as "primary keys" in Kafka Streams tables.
 * </p>
 */
public class KafkaMessageKeyBuilder {

  public static final String SEPARATOR = ":";

  /**
   * Builds a Kafka message key by extracting the properties declared in the annotated eventObject
   * and composing a string out of those.
   *
   * @param eventObject an object whose class is annotated with {@link KafkaMessageKey}
   * @return a string composed of the property values extracted from the given eventObject
   */
  public static String buildMessageKey(Object eventObject) {
    final Class<?> eventClass = eventObject.getClass();

    final KafkaMessageKey kafkaMessageKey = eventClass.getAnnotation(KafkaMessageKey.class);
    Assert.notNull(kafkaMessageKey, "Given object is missing KafkaMessageKey annotation");

    final String[] properties = kafkaMessageKey.properties();
    Assert.notEmpty(properties, "KafkaMessageKey annotation is missing properties");

    final BeanWrapper propertyAccessor = PropertyAccessorFactory.forBeanPropertyAccess(eventObject);

    final String[] parts = new String[properties.length];
    for (int i = 0; i < parts.length; i++) {
      final Object value;
      try {
        value = propertyAccessor.getPropertyValue(properties[i]);
      } catch (InvalidPropertyException e) {
        throw new IllegalArgumentException(
            String.format("Object is missing expected message key property: %s", properties[i]));
      }
      parts[i] = value != null ?
          value.toString() :
          // fallback to a "null" string but convey the field name to help us debug
          "null-"+properties[i];
    }

    return String.join(SEPARATOR, parts);
  }
}
