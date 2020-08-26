/*
 * Copyright 2020 Rackspace US, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rackspace.salus.common.messaging;

import javax.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Provides configurability of the Kafka topics used between and out of Salus services.
 */
@ConfigurationProperties("salus.kafka.topics")
@Data
public class KafkaTopicProperties {

  @NotEmpty
  String logs = "telemetry.logs.json";

  @NotEmpty
  String metrics = "telemetry.metrics.json";

  @NotEmpty
  String attaches = "telemetry.attaches.json";

  @NotEmpty
  String resources = "telemetry.resources.json";

  @NotEmpty
  String monitors = "telemetry.monitors.json";

  @NotEmpty
  String zones = "telemetry.zones.json";

  @NotEmpty
  String installs = "telemetry.installs.json";

  @NotEmpty
  String policies = "telemetry.policies.json";

  @NotEmpty
  String testMonitorRequests = "telemetry.test-monitor-requests.json";

  @NotEmpty
  String testMonitorResults = "telemetry.test-monitor-results.json";

  @NotEmpty
  String testEventTaskResults = "telemetry.test-event-task-results.json";

  @NotEmpty
  String kapacitorEvents = "salus.kapacitor-events.json";

  @NotEmpty
  String stateChangeEvents = "salus.state-change-events.json";
}
