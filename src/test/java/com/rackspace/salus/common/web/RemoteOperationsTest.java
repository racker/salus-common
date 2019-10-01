/*
 * Copyright 2019 Rackspace US, Inc.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

public class RemoteOperationsTest {

  @Test
  public void testMapRestClientExceptions_returnValue() {
    final String result = RemoteOperations.mapRestClientExceptions("some-service", () -> {
      return "resulting value";
    });

    assertThat(result).isEqualTo("resulting value");
  }

  @Test
  public void testMapRestClientExceptions_illegalArg() {

    assertThatThrownBy(() -> {
      RemoteOperations.mapRestClientExceptions("some-service", () -> {
        throw new IllegalArgumentException("just a test");
      });

    })
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("some-service");
  }

  @Test
  public void testMapRestClientExceptions_httpStatusError() {
    assertThatThrownBy(() -> {
      RemoteOperations.mapRestClientExceptions("some-service", () -> {
        throw HttpClientErrorException
            .create(HttpStatus.BAD_REQUEST, "bad request", HttpHeaders.EMPTY, null, null);
      });

    })
        .isInstanceOf(RemoteServiceCallException.class)
        .hasMessageContaining("some-service")
        .hasCauseInstanceOf(RestClientException.class)
        .extracting("remoteServiceName", "statusCode")
        .containsExactly("some-service", HttpStatus.BAD_REQUEST);
  }

  @Test
  public void testMapRestClientExceptions_generalClientError() {
    assertThatThrownBy(() -> {
      RemoteOperations.mapRestClientExceptions("some-service", () -> {
        throw new RestClientException("mystery exception");
      });

    })
        .isInstanceOf(RemoteServiceCallException.class)
        .hasMessageContaining("some-service")
        .hasCauseInstanceOf(RestClientException.class)
        .extracting("remoteServiceName", "statusCode")
        .containsExactly("some-service", HttpStatus.BAD_GATEWAY);
  }
}