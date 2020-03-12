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

package com.rackspace.salus.common.web;

import java.net.URL;
import javax.net.ssl.SSLContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * This component conditionally enables SSL/TLS customization of {@link RestTemplate} instances
 * built via {@link RestTemplateBuilder}.
 * It obtains the trust store, store password, and protocol configuration via the {@link SecureRestTemplateProperties}
 * component and only activates when the trust store location is configured.
 */
@ConditionalOnProperty("salus.restclient.ssl.trust-store")
@EnableConfigurationProperties(SecureRestTemplateProperties.class)
@Slf4j
public class SecureRestTemplateCustomizer implements RestTemplateCustomizer {

  private final SecureRestTemplateProperties properties;

  @Autowired
  public SecureRestTemplateCustomizer(SecureRestTemplateProperties properties) {
    this.properties = properties;
  }

  @Override
  public void customize(RestTemplate restTemplate) {

    final SSLContext sslContext;
    try {
      sslContext = SSLContextBuilder.create()
          .loadTrustMaterial(new URL(properties.getTrustStore()),
              properties.getTrustStorePassword())
          .setProtocol(properties.getProtocol())
          .build();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to setup client SSL context", e);
    }

    final HttpClient httpClient = HttpClientBuilder.create()
        .setSSLContext(sslContext)
        .build();

    final ClientHttpRequestFactory requestFactory =
        new HttpComponentsClientHttpRequestFactory(httpClient);

    log.info("Registered SSL truststore {} for client requests",
        properties.getTrustStore());
    restTemplate.setRequestFactory(requestFactory);
  }
}
