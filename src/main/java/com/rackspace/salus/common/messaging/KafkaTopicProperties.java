package com.rackspace.salus.common.messaging;

import javax.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Provides configurability of the Kafka topics used between and out of Salus services.
 */
@ConfigurationProperties("kafka-topics")
@Data
public class KafkaTopicProperties {

  @NotEmpty
  String logs = "telemetry.logs.json";

  @NotEmpty
  String metrics = "telemetry.metrics.json";

  @NotEmpty
  String events = "telemetry.events.json";

  @NotEmpty
  String attaches = "telemetry.attaches.json";

  @NotEmpty
  String resources = "telemetry.resources.json";

}
