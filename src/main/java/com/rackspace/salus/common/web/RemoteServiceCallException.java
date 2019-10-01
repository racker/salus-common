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

import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Indicates that a remote inter-service call, typically via REST, has failed. There is a companion
 * handler in {@link AbstractRestExceptionHandler} that will intercept this exception type.
 */
public class RemoteServiceCallException extends RuntimeException {

  private final String remoteServiceName;

  /**
   * When a {@link RestClientException} is caught from an invocation of a {@link RestTemplate}
   * method, then this constructor can wrap that cause with further details about the attempted
   * service interaction.
   * @param remoteServiceName the name of the remote service
   * @param cause the {@link RestClientException} thrown by a {@link RestTemplate} call
   */
  public RemoteServiceCallException(String remoteServiceName, RestClientException cause) {
    super(String.format("Remote call to service %s failed", remoteServiceName), cause);
    this.remoteServiceName = remoteServiceName;
  }

  public String getRemoteServiceName() {
    return remoteServiceName;
  }

  public HttpStatus getStatusCode() {
    if (getCause() instanceof HttpStatusCodeException) {
      return ((HttpStatusCodeException) getCause()).getStatusCode();
    }
    else {
      return HttpStatus.BAD_GATEWAY;
    }
  }
}
