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

package com.rackspace.salus.common.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Customizes the application's {@link MeterRegistry} by adding tags to qualify this application
 * instance.
 * <p>
 *   <em>NOTE</em> it does require that <code>spring.application.name</code> has been declared
 *   in the application's environment, typically in the <code>application.yml</code>.
 * </p>
 */
@Configuration
public class MetricsConfig {
  private final String ourHostname;
  private final String appName;

  public MetricsConfig(@Value("${spring.application.name}") String appName,
                       @Value("${localhost.name}") String ourHostName) {
    this.appName = appName;
    this.ourHostname = ourHostName;
  }

  @Bean
  public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
    return registry ->
        registry.config().commonTags(
            "app", appName,
            "host", ourHostname);
  }

}
