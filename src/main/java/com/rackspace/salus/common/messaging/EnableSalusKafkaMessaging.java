package com.rackspace.salus.common.messaging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * This annotation can be applied to a Spring Boot application class or any other {@link org.springframework.context.annotation.Configuration}
 * component where the application is participating in Kafka messaging between or out of a Salus service.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@EnableConfigurationProperties(KafkaTopicProperties.class)
public @interface EnableSalusKafkaMessaging {

}
